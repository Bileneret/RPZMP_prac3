package ua.kpi.iot_lighting.prac3

import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import ua.kpi.iot_lighting.prac3.data.auth.TokenStorage
import ua.kpi.iot_lighting.prac3.data.error.ErrorSink
import ua.kpi.iot_lighting.prac3.data.error.NetworkAvailability
import ua.kpi.iot_lighting.prac3.data.repository.AuthRepositoryImpl
import ua.kpi.iot_lighting.prac3.data.source.AuthUserRecord
import ua.kpi.iot_lighting.prac3.data.source.LightingRemoteDataSource
import ua.kpi.iot_lighting.prac3.domain.model.AuthUser
import ua.kpi.iot_lighting.prac3.domain.model.LightDevice
import ua.kpi.iot_lighting.prac3.domain.model.Role
import ua.kpi.iot_lighting.prac3.domain.model.TokenPayload
import ua.kpi.iot_lighting.prac3.domain.model.UiError
import ua.kpi.iot_lighting.prac3.domain.model.UiException
import ua.kpi.iot_lighting.prac3.domain.repository.LogoutReason
import java.io.IOException

/**
 * Тести авторизації (ПЗ11): успіх, невірні дані, мережеві помилки
 * (з типізацією причини), протермінована сесія та вихід.
 */
class AuthRepositoryImplTest {

    private val accounts = listOf(
        AuthUserRecord(email = "admin@light.io", password = "admin123", name = "Олексій", role = Role.ADMIN),
        AuthUserRecord(email = "user@light.io", password = "user123", name = "Користувач", role = Role.USER),
    )

    private class FakeRemote(
        val users: List<AuthUserRecord>,
        var failure: Throwable? = null,
    ) : LightingRemoteDataSource {
        override suspend fun fetchUsers(): List<AuthUserRecord> {
            failure?.let { throw it }
            return users
        }
        override suspend fun fetchLights(): List<LightDevice> = emptyList()
        override suspend fun setPower(device: LightDevice, poweredOn: Boolean): LightDevice = device
        override suspend fun addLight(name: String, zone: String, powerW: Int): LightDevice =
            LightDevice(0, name, zone, powerW, false)
        override suspend fun deleteLight(id: Long) = Unit
    }

    private class FakeNetwork(var online: Boolean = true) : NetworkAvailability {
        override fun isOnline(): Boolean = online
    }

    private class FakeSink : ErrorSink {
        val entries = mutableListOf<String>()
        override fun log(message: String) { entries.add(message) }
    }

    /** In-memory сховище токена; стартові значення задаються для expiry-кейсу. */
    private class FakeTokenStorage(
        initialToken: String? = null,
        private val valid: Boolean = false,
        private val user: AuthUser? = null,
    ) : TokenStorage {
        var token: String? = initialToken
        override fun save(token: String) { this.token = token }
        override fun token(): String? = token
        override fun payload(): TokenPayload? = null
        override fun hasValidSession(nowEpochSec: Long): Boolean = valid
        override fun currentUser(): AuthUser? = user
        override fun clear() { token = null }
    }

    private lateinit var remote: FakeRemote
    private lateinit var network: FakeNetwork
    private lateinit var sink: FakeSink

    @Before
    fun setUp() {
        remote = FakeRemote(accounts)
        network = FakeNetwork()
        sink = FakeSink()
    }

    private fun repo(storage: TokenStorage = FakeTokenStorage()) = AuthRepositoryImpl(
        remote = remote,
        tokenStorage = storage,
        networkChecker = network,
        errorSink = sink,
    )

    @Test
    fun `успішний вхід повертає користувача і зберігає токен`() = runTest {
        val storage = FakeTokenStorage()
        val repository = repo(storage)
        val user = repository.login("admin@light.io", "admin123")
        assertEquals(Role.ADMIN, user.role)
        assertNotNull("Токен має бути збережений", storage.token)
        assertEquals(user, repository.currentUser.value)
        assertFalse(repository.sessionExpired.value)
    }

    @Test
    fun `email не залежить від регістру`() = runTest {
        val user = repo().login("ADMIN@LIGHT.IO", "admin123")
        assertEquals("admin@light.io", user.email)
    }

    @Test
    fun `невірний пароль дає InvalidCredentials`() = runTest {
        val error = runCatching { repo().login("admin@light.io", "wrong") }
        assertEquals(UiError.InvalidCredentials, (error.exceptionOrNull() as UiException).error)
    }

    @Test
    fun `невідомий email дає InvalidCredentials`() = runTest {
        val error = runCatching { repo().login("nobody@light.io", "admin123") }
        assertEquals(UiError.InvalidCredentials, (error.exceptionOrNull() as UiException).error)
    }

    @Test
    fun `http 500 не маскується під відсутність мережі`() = runTest {
        remote.failure = HttpException(Response.error<Any>(500, ResponseBody.create(null, "err")))
        val error = runCatching { repo().login("admin@light.io", "admin123") }
        assertEquals(UiError.ServerError, (error.exceptionOrNull() as UiException).error)
        assertTrue("Помилка має потрапити в журнал", sink.entries.any { it.contains("ServerError") })
    }

    @Test
    fun `обрив мережі дає NoConnection`() = runTest {
        remote.failure = IOException("offline")
        val error = runCatching { repo().login("admin@light.io", "admin123") }
        assertEquals(UiError.NoConnection, (error.exceptionOrNull() as UiException).error)
    }

    @Test
    fun `без мережі запит не виконується`() = runTest {
        network.online = false
        val error = runCatching { repo().login("admin@light.io", "admin123") }
        assertEquals(UiError.NoConnection, (error.exceptionOrNull() as UiException).error)
    }

    @Test
    fun `вихід користувача не позначає сесію як протерміновану`() = runTest {
        val repository = repo()
        repository.logout(LogoutReason.USER)
        assertNull(repository.currentUser.value)
        assertFalse(repository.sessionExpired.value)
    }

    @Test
    fun `вихід через протермінований токен виставляє прапорець`() = runTest {
        val repository = repo()
        repository.logout(LogoutReason.SESSION_EXPIRED)
        assertTrue(repository.sessionExpired.value)
    }

    @Test
    fun `протермінований токен при старті очищується із позначкою сесії`() {
        val storage = FakeTokenStorage(initialToken = "stale.token.here", valid = false)
        val repository = repo(storage)
        assertNull(storage.token)
        assertTrue(repository.sessionExpired.value)
        assertNull(repository.currentUser.value)
    }
}
