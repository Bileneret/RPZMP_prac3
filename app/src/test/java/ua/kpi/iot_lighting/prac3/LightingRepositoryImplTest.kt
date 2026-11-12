package ua.kpi.iot_lighting.prac3

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ua.kpi.iot_lighting.prac3.data.error.ErrorSink
import ua.kpi.iot_lighting.prac3.data.error.NetworkAvailability
import ua.kpi.iot_lighting.prac3.data.local.CacheMetadata
import ua.kpi.iot_lighting.prac3.data.repository.LightingRepositoryImpl
import ua.kpi.iot_lighting.prac3.data.repository.withRetry
import ua.kpi.iot_lighting.prac3.data.source.LightingLocalDataSource
import ua.kpi.iot_lighting.prac3.data.source.LightingRemoteDataSource
import ua.kpi.iot_lighting.prac3.data.source.AuthUserRecord
import ua.kpi.iot_lighting.prac3.domain.model.LightDevice
import ua.kpi.iot_lighting.prac3.domain.model.Role
import ua.kpi.iot_lighting.prac3.domain.model.UiError
import ua.kpi.iot_lighting.prac3.domain.model.UiException
import java.io.IOException

/**
 * Тести репозиторію (ПЗ9): агрегація локального кешу та віддаленого джерела,
 * правила «спочатку сервер — потім кеш», обробка помилок.
 * Джерела підмінені fake-обʼєктами — саме це демонструє тестованість архітектури.
 */
class LightingRepositoryImplTest {

    private val sampleDevices = listOf(
        LightDevice(1, "Люстра зал", "Зал", 180, true),
        LightDevice(2, "Точковий кухонний", "Кухня", 60, false),
        LightDevice(3, "Тамбур", "Коридор", 40, true),
    )

    /** In-memory кеш замість Room. */
    private class FakeLocal : LightingLocalDataSource {
        val store = MutableStateFlow<List<LightDevice>>(emptyList())
        override fun observe(): Flow<List<LightDevice>> = store
        override suspend fun replaceAll(devices: List<LightDevice>) { store.value = devices }
        override suspend fun setPower(id: Long, poweredOn: Boolean) {
            store.value = store.value.map { if (it.id == id) it.copy(isPoweredOn = poweredOn) else it }
        }
        override suspend fun add(device: LightDevice) { store.value = store.value + device }
        override suspend fun delete(id: Long) { store.value = store.value.filter { it.id != id } }
    }

    /** Fake віддаленого джерела: тримає стан, як справжній сервер. */
    private class FakeRemote(
        seed: List<LightDevice>,
        var failure: Exception? = null,
    ) : LightingRemoteDataSource {
        private val store = seed.toMutableList()
        var putCalls = 0
        var postCalls = 0
        var deleteCalls = 0
        override suspend fun fetchLights(): List<LightDevice> {
            failure?.let { throw it }
            return store.toList()
        }
        override suspend fun setPower(device: LightDevice, poweredOn: Boolean): LightDevice {
            failure?.let { throw it }
            putCalls++
            val updated = device.copy(isPoweredOn = poweredOn)
            store.replaceAll { if (it.id == device.id) updated else it }
            return updated
        }
        override suspend fun addLight(name: String, zone: String, powerW: Int): LightDevice {
            failure?.let { throw it }
            postCalls++
            val created = LightDevice(id = 99, name = name, zone = zone, powerW = powerW, isPoweredOn = false)
            store.add(created)
            return created
        }
        override suspend fun deleteLight(id: Long) {
            failure?.let { throw it }
            deleteCalls++
            store.removeAll { it.id == id }
        }
        override suspend fun fetchUsers(): List<AuthUserRecord> = emptyList()
    }

    private class FakeNetwork(var online: Boolean = true) : NetworkAvailability {
        override fun isOnline(): Boolean = online
    }

    /** Fake журналу: збирає рядки для перевірки реєстрації помилок (ПЗ12). */
    private class FakeErrorSink : ErrorSink {
        val entries = mutableListOf<String>()
        override fun log(message: String) { entries.add(message) }
    }

    /** Fake метаданих кешу замість SharedPreferences. */
    private class FakeCacheMetadata : CacheMetadata {
        var lastSync: Long? = null
        override fun lastSyncAt(): Long? = lastSync
        override fun markSynced(atEpochMs: Long) { lastSync = atEpochMs }
    }

    private lateinit var local: FakeLocal
    private lateinit var remote: FakeRemote
    private lateinit var network: FakeNetwork
    private lateinit var errorSink: FakeErrorSink
    private lateinit var repository: LightingRepositoryImpl

    @Before
    fun setUp() {
        local = FakeLocal()
        remote = FakeRemote(sampleDevices)
        network = FakeNetwork()
        errorSink = FakeErrorSink()
        repository = LightingRepositoryImpl(
            local = local,
            remote = remote,
            networkChecker = network,
            cacheMetadata = FakeCacheMetadata(),
            errorLogger = errorSink,
        )
    }

    @Test
    fun `refresh заповнює кеш з сервера`() = runTest {
        repository.refreshFromRemote()
        assertEquals(sampleDevices, local.store.value)
    }

    @Test
    fun `без мережі refresh падає NoConnection і кеш не змінюється`() = runTest {
        local.replaceAll(sampleDevices)
        network.online = false
        val error = runCatching { repository.refreshFromRemote() }
        assertTrue(error.exceptionOrNull() is UiException)
        assertEquals(UiError.NoConnection, (error.exceptionOrNull() as UiException).error)
        assertEquals(sampleDevices, local.store.value)
    }

    @Test
    fun `setPower спочатку сервер потім кеш`() = runTest {
        repository.refreshFromRemote()
        repository.setPower(2, poweredOn = true)
        assertEquals(1, remote.putCalls)
        assertTrue(local.store.value.first { it.id == 2L }.isPoweredOn)
    }

    @Test
    fun `setPower при помилці сервера не змінює кеш`() = runTest {
        repository.refreshFromRemote()
        remote.failure = IOException("server down")
        val error = runCatching { repository.setPower(1, poweredOn = false) }
        assertTrue(error.exceptionOrNull() is UiException)
        assertTrue(local.store.value.first { it.id == 1L }.isPoweredOn)
    }

    @Test
    fun `addDevice додає на сервері і оновлює кеш`() = runTest {
        repository.refreshFromRemote()
        repository.addDevice("Новий", "Зал", 75)
        assertEquals(1, remote.postCalls)
        assertEquals(4, local.store.value.size)
    }

    @Test
    fun `deleteDevice видаляє з сервера і з кешу`() = runTest {
        repository.refreshFromRemote()
        repository.deleteDevice(3)
        assertEquals(1, remote.deleteCalls)
        assertNull(local.store.value.find { it.id == 3L })
    }

    @Test
    fun `setPower невідомого id падає NotFound`() = runTest {
        repository.refreshFromRemote()
        val error = runCatching { repository.setPower(777, poweredOn = true) }
        assertEquals(UiError.NotFound, (error.exceptionOrNull() as UiException).error)
    }

    @Test
    fun `помилки реєструються у журнал (ПЗ12)`() = runTest {
        network.online = false
        runCatching { repository.refreshFromRemote() }
        assertTrue("Журнал порожній", errorSink.entries.isNotEmpty())
        assertTrue(errorSink.entries.first().contains("NoConnection"))
    }
}
