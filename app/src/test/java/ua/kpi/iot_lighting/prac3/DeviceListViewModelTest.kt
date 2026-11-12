package ua.kpi.iot_lighting.prac3

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ua.kpi.iot_lighting.prac3.domain.logic.LightingAdvisor
import ua.kpi.iot_lighting.prac3.domain.model.AuthUser
import ua.kpi.iot_lighting.prac3.domain.model.LightDevice
import ua.kpi.iot_lighting.prac3.domain.model.LightingTotals
import ua.kpi.iot_lighting.prac3.domain.model.Role
import ua.kpi.iot_lighting.prac3.domain.model.UiError
import ua.kpi.iot_lighting.prac3.domain.model.UiException
import ua.kpi.iot_lighting.prac3.domain.model.WeatherInsight
import ua.kpi.iot_lighting.prac3.domain.repository.AuthRepository
import ua.kpi.iot_lighting.prac3.domain.repository.LightingRepository
import ua.kpi.iot_lighting.prac3.domain.repository.LogoutReason
import ua.kpi.iot_lighting.prac3.domain.repository.WeatherRepository
import ua.kpi.iot_lighting.prac3.ui.viewmodel.DeviceFilter
import ua.kpi.iot_lighting.prac3.ui.viewmodel.DeviceListViewModel
import ua.kpi.iot_lighting.prac3.ui.viewmodel.DeviceSort

/**
 * Тести ViewModel списку (ПЗ9/ПЗ12): реактивний кеш, фільтр/сортування,
 * типізовані помилки дій і реакція на протерміновану сесію.
 */
class DeviceListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val seed = listOf(
        LightDevice(1, "Люстра", "Зал", 72, true),
        LightDevice(2, "Нічник", "Спальня", 10, true),
        LightDevice(3, "Прожектор", "Фасад", 90, false),
    )

    private class FakeLightingRepository(initial: List<LightDevice>) : LightingRepository {
        val store = MutableStateFlow(initial)
        var refreshFailure: UiException? = null
        var setPowerFailure: UiException? = null

        override fun observeDevices(): Flow<List<LightDevice>> = store
        override fun observeTotals(): Flow<LightingTotals> =
            store.map { LightingAdvisor.totals(it) }
        override suspend fun lastSyncAt(): Long? = 123L
        override suspend fun refreshFromRemote() {
            refreshFailure?.let { throw it }
        }
        override suspend fun setPower(id: Long, poweredOn: Boolean) {
            setPowerFailure?.let { throw it }
            store.value = store.value.map { if (it.id == id) it.copy(isPoweredOn = poweredOn) else it }
        }
        override suspend fun addDevice(name: String, zone: String, powerW: Int) = Unit
        override suspend fun deleteDevice(id: Long) {
            store.value = store.value.filter { it.id != id }
        }
    }

    private class FakeWeatherRepository : WeatherRepository {
        override suspend fun insight(): WeatherInsight =
            WeatherInsight(isDay = true, cloudCoverPercent = 10, temperatureC = 20.0, daylightPercent = 90, recommendation = "Світла достатньо")
    }

    private class FakeAuthRepository : AuthRepository {
        val userFlow = MutableStateFlow<AuthUser?>(AuthUser("Адмін", "admin@light.io", Role.ADMIN))
        val expiredFlow = MutableStateFlow(false)
        var lastLogoutReason: LogoutReason? = null
        override val currentUser: StateFlow<AuthUser?> = userFlow
        override val sessionExpired: StateFlow<Boolean> = expiredFlow
        override suspend fun login(email: String, password: String): AuthUser =
            AuthUser("Адмін", email, Role.ADMIN)
        override suspend fun logout(reason: LogoutReason) {
            lastLogoutReason = reason
            userFlow.value = null
            expiredFlow.value = reason == LogoutReason.SESSION_EXPIRED
        }
        override fun hasValidSession(): Boolean = false
    }

    private fun vm(
        repository: FakeLightingRepository = FakeLightingRepository(seed),
        auth: FakeAuthRepository = FakeAuthRepository(),
    ) = DeviceListViewModel(repository, FakeWeatherRepository(), auth)

    @Test
    fun `старт показує кеш і агрегати`() = runTest {
        val viewModel = vm()
        val state = viewModel.state.value
        assertEquals(3, state.devices.size)
        assertEquals(2, state.totals?.poweredDevices)
        assertEquals(123L, state.lastSyncAt)
    }

    @Test
    fun `фільтр увімкнених залишає лише увімкнені`() = runTest {
        val viewModel = vm()
        viewModel.setFilter(DeviceFilter.ON)
        assertEquals(listOf(1L, 2L), viewModel.state.value.devices.map { it.id })
    }

    @Test
    fun `фільтр вимкнених залишає лише вимкнені`() = runTest {
        val viewModel = vm()
        viewModel.setFilter(DeviceFilter.OFF)
        assertEquals(listOf(3L), viewModel.state.value.devices.map { it.id })
    }

    @Test
    fun `сортування за потужністю спадає`() = runTest {
        val viewModel = vm()
        viewModel.setFilter(DeviceFilter.ALL)
        viewModel.setSort(DeviceSort.BY_POWER)
        assertEquals(listOf(90, 72, 10), viewModel.state.value.devices.map { it.powerW })
    }

    @Test
    fun `помилка перемикання показується через actionError`() = runTest {
        val repository = FakeLightingRepository(seed).apply {
            setPowerFailure = UiException(UiError.ServerError)
        }
        val viewModel = vm(repository)
        viewModel.toggle(seed[0], false)
        assertEquals("Сталася помилка сервера, спробуйте пізніше", viewModel.state.value.actionError)
    }

    @Test
    fun `протермінована сесія завершує її з позначкою`() = runTest {
        val repository = FakeLightingRepository(seed).apply {
            setPowerFailure = UiException(UiError.SessionExpired)
        }
        val auth = FakeAuthRepository()
        val viewModel = vm(repository, auth)
        viewModel.toggle(seed[0], false)
        assertEquals(LogoutReason.SESSION_EXPIRED, auth.lastLogoutReason)
        assertTrue(auth.expiredFlow.value)
    }

    @Test
    fun `явний вихід очищає сесію без позначки протермінування`() = runTest {
        val auth = FakeAuthRepository()
        val viewModel = vm(auth = auth)
        viewModel.logout()
        assertEquals(LogoutReason.USER, auth.lastLogoutReason)
        assertFalse(auth.expiredFlow.value)
    }

    @Test
    fun `офлайн-оновлення позначає кеш і не дублює помилку`() = runTest {
        val repository = FakeLightingRepository(seed).apply {
            refreshFailure = UiException(UiError.NoConnection)
        }
        val viewModel = vm(repository)
        viewModel.refresh()
        val state = viewModel.state.value
        assertTrue(state.isOfflineCache)
        assertEquals("Немає з'єднання з інтернетом", state.actionError)
        assertEquals(3, state.devices.size)
        assertNull(state.loadError)
        assertNotNull(state.lastSyncAt)
    }
}
