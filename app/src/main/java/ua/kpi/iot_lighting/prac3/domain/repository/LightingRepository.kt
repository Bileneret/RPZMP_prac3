package ua.kpi.iot_lighting.prac3.domain.repository

import kotlinx.coroutines.flow.Flow
import ua.kpi.iot_lighting.prac3.domain.model.LightDevice
import ua.kpi.iot_lighting.prac3.domain.model.LightingTotals

/**
 * Контракт доступу до даних освітлення (ПЗ9).
 *
 * Реалізація агрегує два джерела: локальний кеш Room і mock-сервер REST.
 * UI працює лише з цим інтерфейсом — джерела можна замінювати без змін
 * ViewModel (абстракція джерела даних).
 */
interface LightingRepository {
    /** Реактивний потік світильників з локального кешу для списку UI. */
    fun observeDevices(): Flow<List<LightDevice>>

    /** Потік агрегованих показників (сумарна потужність, прогноз вартості). */
    fun observeTotals(): Flow<LightingTotals>

    /** Момент останньої успішної синхронізації, епоха-мс; null — її не було. */
    suspend fun lastSyncAt(): Long?

    /**
     * Завантажити світильники з сервера та оновити кеш.
     * @throws ua.kpi.iot_lighting.prac3.domain.model.UiException при помилці
     *         мережі/HTTP/парсингу або відсутності валідного токена.
     */
    suspend fun refreshFromRemote()

    /**
     * Перемкнути живлення: спочатку PUT на сервер, потім оновлення кешу.
     * @throws UiException — з тими самими правилами, що й [refreshFromRemote].
     */
    suspend fun setPower(id: Long, poweredOn: Boolean)

    /**
     * Додати світильник (POST). Доступно лише користувачу з роллю ADMIN —
     * сервер приймає запит лише з токеном адміністратора (перевірка ролі
     * виконується на клієнті, бо мок-сервер не валідує підпис токена).
     */
    suspend fun addDevice(name: String, zone: String, powerW: Int)

    /** Видалити світильник (DELETE), також операція адміністратора. */
    suspend fun deleteDevice(id: Long)
}
