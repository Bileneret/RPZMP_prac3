package ua.kpi.iot_lighting.prac3.data.source

import kotlinx.coroutines.flow.Flow
import ua.kpi.iot_lighting.prac3.domain.model.LightDevice

/**
 * Локальний Data Source — кеш світильників (Room).
 *
 * Інтерфейс, а не клас: у юніт-тестах репозиторію кеш підміняється fake-обʼєктом
 * без бази (критерій просунутого рівня ПЗ9 — тестованість).
 */
interface LightingLocalDataSource {
    /** Реактивний потік усього кешу. */
    fun observe(): Flow<List<LightDevice>>

    /** Замінити кеш повним набором з сервера. */
    suspend fun replaceAll(devices: List<LightDevice>)

    /** Оновити живлення одного світильника. */
    suspend fun setPower(id: Long, poweredOn: Boolean)

    /** Додати світильник (результат POST). */
    suspend fun add(device: LightDevice)

    /** Видалити світильник (після успішного DELETE). */
    suspend fun delete(id: Long)
}

/**
 * Віддалений Data Source — mock-сервер освітлення (Retrofit).
 *
 * Перед кожним викликом перевіряє наявність валідного токена: запити
 * /lights* — захищений ресурс (ПЗ11), без токена кидає SessionExpired.
 */
interface LightingRemoteDataSource {
    /** Список світильників із сервера (GET /lights). */
    suspend fun fetchLights(): List<LightDevice>

    /**
     * Оновити стан живлення на сервері (PUT /lights/{id}).
     * REST-PUT очікує повний обʼєкт, тому джерело приймає поточний
     * [LightDevice] з кешу і новий стан живлення; сервер повертає оновлений запис.
     */
    suspend fun setPower(device: LightDevice, poweredOn: Boolean): LightDevice

    /** Додати світильник (POST /lights, лише адміністратор). */
    suspend fun addLight(name: String, zone: String, powerW: Int): LightDevice

    /** Видалити світильник (DELETE /lights/{id}, лише адміністратор). */
    suspend fun deleteLight(id: Long)

    /** Зчитати облікові записи для автентифікації (GET /users). */
    suspend fun fetchUsers(): List<AuthUserRecord>
}

/**
 * Обліковий запис у вигляді доменного обʼєкта (пароль потрібен лише
 * для перевірки при вході, далі не зберігається ніде).
 */
data class AuthUserRecord(
    val email: String,
    val password: String,
    val name: String,
    val role: ua.kpi.iot_lighting.prac3.domain.model.Role,
)
