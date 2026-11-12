package ua.kpi.iot_lighting.prac3.domain.model

/**
 * Декодований payload тестового (mocked) токена.
 *
 * Токен має формат JWT (header.payload.signature), але підпис — фейковий:
 * сервера, що перевіряє підпис, у демо немає, тому валідність на клієнті
 * обмежується наявністю токена і терміном дії [expiresAtEpochSec].
 */
data class TokenPayload(
    /** Email користувача (стандартне поле `sub` JWT). */
    val subject: String,
    /** Імʼя користувача для відображення у UI. */
    val name: String,
    /** Роль з payload — керує адаптацією UI (просунутий рівень ПЗ11). */
    val role: Role,
    /** Час видачі, епоха-секунди (`iat`). */
    val issuedAtEpochSec: Long,
    /** Час закінчення дії, епоха-секунди (`exp`). */
    val expiresAtEpochSec: Long,
) {
    /** Чи протермінований токен у вказаний момент часу. */
    fun isExpired(nowEpochSec: Long = System.currentTimeMillis() / 1000): Boolean =
        nowEpochSec >= expiresAtEpochSec
}
