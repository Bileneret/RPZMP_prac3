package ua.kpi.iot_lighting.prac3.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
import ua.kpi.iot_lighting.prac3.ui.util.userMessage

/** Фільтр списку світильників (критерій «фільтрація у різних компонентах UI»). */
enum class DeviceFilter(val label: String) { ALL("Усі"), ON("Увімкнені"), OFF("Вимкнені") }

/** Сортування списку (критерій «сортування»). */
enum class DeviceSort(val label: String) { BY_NAME("Назва"), BY_POWER("Потужність") }

/**
 * Стан головного екрана: список, агрегати, погода, стан завантаження,
 * помилки та роль користувача для адмін-функцій.
 */
data class DeviceListUiState(
    val devices: List<LightDevice> = emptyList(),
    val totals: LightingTotals? = null,
    val weather: WeatherInsight? = null,
    val lastSyncAt: Long? = null,
    val user: AuthUser? = null,
    val filter: DeviceFilter = DeviceFilter.ALL,
    val sort: DeviceSort = DeviceSort.BY_NAME,
    val isLoading: Boolean = false,
    /** Ручне оновлення триває — TopAppBar показує спінер замість іконки. */
    val isRefreshing: Boolean = false,
    /** Повноекранна помилка — лише коли кеш порожній і список показати нічого. */
    val loadError: String? = null,
    /** Разове повідомлення про помилку дії — для Snackbar. */
    val actionError: String? = null,
    /** true — показано кеш без звʼязку з сервером (позначка офлайн). */
    val isOfflineCache: Boolean = false,
)

/**
 * ViewModel списку світильників: поєднує реактивний кеш (Room),
 * синхронізацію з сервером (Retrofit), погоду та адмін-дії.
 *
 * Помилки ПЗ12: повноекранний стан — коли даних немає взагалі,
 * Snackbar — коли дія не вдалась, а дані видно.
 */
class DeviceListViewModel(
    private val lightingRepository: LightingRepository,
    private val weatherRepository: WeatherRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DeviceListUiState())
    val state: StateFlow<DeviceListUiState> = _state.asStateFlow()

    /** Повний набір з кешу — фільтр/сортування застосовуються поверх. */
    private var allDevices: List<LightDevice> = emptyList()

    /** Захист від подвійних натискань (HyperOS може продублювати подію тапа). */
    private val busyDeviceIds = mutableSetOf<Long>()
    private var addInFlight = false

    init {
        // реактивне оновлення списку при кожній зміні кешу Room
        viewModelScope.launch {
            lightingRepository.observeDevices().collect { devices ->
                allDevices = devices
                applyFilterSort()
            }
        }
        viewModelScope.launch {
            lightingRepository.observeTotals().collect { totals ->
                _state.update { it.copy(totals = totals) }
            }
        }
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _state.update { it.copy(user = user) }
            }
        }
        // старт: миттєво показуємо кеш (якщо є) і лише потім ідемо в мережу
        viewModelScope.launch {
            val cached = runCatching { lightingRepository.observeDevices().first() }
                .getOrDefault(emptyList())
            if (cached.isNotEmpty()) {
                allDevices = cached
                applyFilterSort()
            }
            _state.update { it.copy(lastSyncAt = lightingRepository.lastSyncAt()) }
            refresh(initial = true)
        }
    }

    /** Оновити дані з сервера + погоду. [initial] — перший запуск екрана. */
    fun refresh(initial: Boolean = false) {
        viewModelScope.launch {
            // повноекранний індикатор — лише коли показати справді нічого
            if (initial && _state.value.devices.isEmpty()) {
                _state.update { it.copy(isLoading = true, loadError = null) }
            } else {
                _state.update { it.copy(isRefreshing = true) }
            }
            try {
                lightingRepository.refreshFromRemote()
                _state.update { st ->
                    st.copy(
                        lastSyncAt = lightingRepository.lastSyncAt(),
                        isOfflineCache = false,
                        loadError = null,
                    )
                }
                // погода — незалежний запит: її збій не ламає список
                loadWeather()
            } catch (e: UiException) {
                if (e.error == UiError.SessionExpired) {
                    // токен протермінований — екран входу покаже пояснення
                    authRepository.logout(LogoutReason.SESSION_EXPIRED)
                } else if (allDevices.isEmpty()) {
                    _state.update { it.copy(lastSyncAt = lightingRepository.lastSyncAt(), loadError = e.error.userMessage()) }
                } else {
                    // дані видно з кешу — позначка офлайну з часом останньої синхронізації
                    _state.update { st ->
                        st.copy(
                            isOfflineCache = true,
                            lastSyncAt = lightingRepository.lastSyncAt(),
                            actionError = e.error.userMessage(),
                        )
                    }
                }
            } finally {
                _state.update { it.copy(isLoading = false, isRefreshing = false) }
            }
        }
    }

    /** Погодні умови та рекомендація — друге віддалене джерело (ПЗ10). */
    private suspend fun loadWeather() {
        val insight = runCatching { weatherRepository.insight() }.getOrNull()
        _state.update { it.copy(weather = insight) }
    }

    /** Перемикання живлення: PUT на сервер, потім кеш (порядок фікс у репозиторії). */
    fun toggle(device: LightDevice, poweredOn: Boolean) {
        viewModelScope.launch {
            if (!busyDeviceIds.add(device.id)) return@launch
            try {
                lightingRepository.setPower(device.id, poweredOn)
            } catch (e: UiException) {
                handleActionError(e)
            } finally {
                busyDeviceIds.remove(device.id)
            }
        }
    }

    /** Додати світильник — операція адміністратора (POST). */
    fun addDevice(name: String, zone: String, powerW: Int) {
        if (addInFlight) return
        addInFlight = true
        viewModelScope.launch {
            try {
                lightingRepository.addDevice(name, zone, powerW)
            } catch (e: UiException) {
                handleActionError(e)
            } finally {
                addInFlight = false
            }
        }
    }

    /** Видалити світильник — операція адміністратора (DELETE). */
    fun deleteDevice(id: Long) {
        viewModelScope.launch {
            if (!busyDeviceIds.add(id)) return@launch
            try {
                lightingRepository.deleteDevice(id)
            } catch (e: UiException) {
                handleActionError(e)
            } finally {
                busyDeviceIds.remove(id)
            }
        }
    }

    fun setFilter(filter: DeviceFilter) {
        _state.update { it.copy(filter = filter) }
        applyFilterSort()
    }

    fun setSort(sort: DeviceSort) {
        _state.update { it.copy(sort = sort) }
        applyFilterSort()
    }

    /** Закрити Snackbar вручну. */
    fun dismissActionError() {
        _state.update { it.copy(actionError = null) }
    }

    /** Явний вихід із акаунта (кнопка в TopAppBar): очищає токен і сесію. */
    fun logout() {
        viewModelScope.launch { authRepository.logout(LogoutReason.USER) }
    }

    /** Помилка дії: сесія → вихід на логін з поясненням, інше → Snackbar. */
    private fun handleActionError(e: UiException) {
        if (e.error == UiError.SessionExpired) {
            viewModelScope.launch { authRepository.logout(LogoutReason.SESSION_EXPIRED) }
        } else {
            _state.update { it.copy(actionError = e.error.userMessage()) }
        }
    }

    /** Застосувати фільтр і сортування до кешованого набору. */
    private fun applyFilterSort() {
        val filtered = when (_state.value.filter) {
            DeviceFilter.ALL -> allDevices
            DeviceFilter.ON -> allDevices.filter { it.isPoweredOn }
            DeviceFilter.OFF -> allDevices.filter { !it.isPoweredOn }
        }
        val sorted = when (_state.value.sort) {
            DeviceSort.BY_NAME -> filtered.sortedBy { it.name.lowercase() }
            DeviceSort.BY_POWER -> filtered.sortedByDescending { it.powerW }
        }
        _state.update { it.copy(devices = sorted) }
    }

    companion object {
        /** Фабрика з ручним DI (AppContainer). */
        val Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                DeviceListViewModel(
                    lightingRepository = container.lightingRepository,
                    weatherRepository = container.weatherRepository,
                    authRepository = container.authRepository,
                )
            }
        }
    }
}
