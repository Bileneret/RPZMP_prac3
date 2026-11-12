package ua.kpi.iot_lighting.prac3.data.auth

import ua.kpi.iot_lighting.prac3.domain.model.AuthUser
import ua.kpi.iot_lighting.prac3.domain.model.TokenPayload

/**
 * Абстракція сховища токена: AuthRepository залежить від інтерфейсу,
 * тому юніт-тести підставляють in-memory fake без Android-контексту.
 */
interface TokenStorage {
    /** Зберегти токен після успішного входу. */
    fun save(token: String)

    /** Сирий токен або null. */
    fun token(): String?

    /** Розібраний payload поточного токена; null — токена немає/пошкоджений. */
    fun payload(): TokenPayload?

    /** Чи є дійсна (не протермінована) сесія. */
    fun hasValidSession(nowEpochSec: Long = System.currentTimeMillis() / 1000): Boolean

    /** Поточний користувач з payload; null — сесії немає або токен протермінований. */
    fun currentUser(): AuthUser?

    /** Видалити токен. */
    fun clear()
}
