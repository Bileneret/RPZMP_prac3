package ua.kpi.iot_lighting.prac3.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ua.kpi.iot_lighting.prac3.data.error.ErrorHandler
import ua.kpi.iot_lighting.prac3.data.error.ErrorSink
import ua.kpi.iot_lighting.prac3.data.error.NetworkAvailability
import ua.kpi.iot_lighting.prac3.data.local.CacheMetadata
import ua.kpi.iot_lighting.prac3.data.source.LightingLocalDataSource
import ua.kpi.iot_lighting.prac3.data.source.LightingRemoteDataSource
import ua.kpi.iot_lighting.prac3.domain.logic.LightingAdvisor
import ua.kpi.iot_lighting.prac3.domain.model.LightDevice
import ua.kpi.iot_lighting.prac3.domain.model.LightingTotals
import ua.kpi.iot_lighting.prac3.domain.model.UiError
import ua.kpi.iot_lighting.prac3.domain.model.UiException
import ua.kpi.iot_lighting.prac3.domain.repository.LightingRepository

/**
 * Реалізація [LightingRepository] — агрегатор локального кешу та
 * mock-сервера (просунутий рівень ПЗ9: кілька джерел, кешування,
 * обробка помилок, асинхронна робота через корутини).
 *
 * Правила поєднання джерел:
 *  - читання — завжди з кешу (реактивний Room-Flow), сервер оновлює його;
 *  - кожен запис — спочатку сервер, потім кеш (кеш ніколи не «випереджає» факт);
 *  - при помилці запиту кеш не змінюється, помилка йде типізованою наверх.
 */
class LightingRepositoryImpl(
    private val local: LightingLocalDataSource,
    private val remote: LightingRemoteDataSource,
    private val networkChecker: NetworkAvailability,
    private val cacheMetadata: CacheMetadata,
    private val errorLogger: ErrorSink,
) : LightingRepository {

    override fun observeDevices(): Flow<List<LightDevice>> = local.observe()

    override fun observeTotals(): Flow<LightingTotals> =
        local.observe().map { LightingAdvisor.totals(it) }

    override suspend fun lastSyncAt(): Long? = cacheMetadata.lastSyncAt()

    override suspend fun refreshFromRemote() = guard {
        guardNetwork()
        val lights = withRetry { remote.fetchLights() }
        local.replaceAll(lights)
        cacheMetadata.markSynced(System.currentTimeMillis())
    }

    override suspend fun setPower(id: Long, poweredOn: Boolean) = guard {
        guardNetwork()
        val current = currentDevice(id)
        val updated = withRetry { remote.setPower(current, poweredOn) }
        local.setPower(id, updated.isPoweredOn)
    }

    override suspend fun addDevice(name: String, zone: String, powerW: Int) = guard {
        guardNetwork()
        // POST не ретраїться: повтор після таймауту створив би дубль на сервері
        remote.addLight(name, zone, powerW)
        // узгоджуємо кеш з сервером одразу після зміни складу
        refreshFromRemote()
    }

    override suspend fun deleteDevice(id: Long) = guard {
        guardNetwork()
        withRetry { remote.deleteLight(id) }
        local.delete(id)
    }

    /**
     * Централізоване перетворення помилок (ПЗ12): усе, що долетіло з
     * Data Source/Retrofit/мережі, стає [UiException] з [UiError] —
     * ViewModel працює лише з типізованими значеннями. Кожна помилка
     * дописується у файл-журнал [ErrorLogger] для подальшого аналізу.
     */
    private suspend fun <T> guard(body: suspend () -> T): T =
        try {
            body()
        } catch (e: UiException) {
            errorLogger.log("repository: ${e.error}")
            throw e
        } catch (t: Throwable) {
            val error = ErrorHandler.toUiError(t)
            errorLogger.log("repository: $error (${t.javaClass.simpleName}: ${t.message ?: "-"})")
            throw UiException(error, t)
        }

    /** Пристрій для PUT має бути відомий: беремо з кешу або падаємо NotFound. */
    private suspend fun currentDevice(id: Long): LightDevice {
        val list = local.observe().first()
        return list.find { it.id == id } ?: throw UiException(UiError.NotFound)
    }

    /** Без мережі одразу типізована помилка — без марних таймаутів (ПЗ12). */
    private fun guardNetwork() {
        if (!networkChecker.isOnline()) throw UiException(UiError.NoConnection)
    }
}
