package ua.kpi.iot_lighting.prac3.data.auth

import com.google.gson.Gson
import ua.kpi.iot_lighting.prac3.domain.model.AuthUser
import ua.kpi.iot_lighting.prac3.domain.model.Role
import ua.kpi.iot_lighting.prac3.domain.model.TokenPayload
import java.util.Base64

/**
 * Генерація та розбір тестового (mocked) токена у форматі JWT.
 *
 * Токен складається з трьох base64url-частин через крапку — так само,
 * як справжній JWT, але підпис фейковий: справжній сервер підписував би
 * payload секретним ключем (HS256), а в демо підпис лише імітується.
 * Роль користувача закодована в payload і читається клієнтом —
 * саме на ній будується адаптація UI (просунутий рівень ПЗ11).
 */
object MockTokenFactory {
    /** Термін дії токена: 8 годин (епоха-секунди). */
    private const val LIFETIME_SEC = 8 * 60 * 60

    private val gson = Gson()

    /** Fallback-модель для розбір_payload: Gson не знає enum Role напряму. */
    private data class PayloadJson(
        val sub: String,
        val name: String,
        val role: String,
        val iat: Long,
        val exp: Long,
    )

    /**
     * Згенерувати тестовий токен для користувача. Викликається після
     * успішної перевірки логіна/пароля за даними mock-сервера.
     */
    fun create(user: AuthUser, nowEpochSec: Long = System.currentTimeMillis() / 1000): String {
        val header = mapOf("alg" to "mock", "typ" to "JWT")
        val payload = PayloadJson(
            sub = user.email,
            name = user.name,
            role = user.role.name,
            iat = nowEpochSec,
            exp = nowEpochSec + LIFETIME_SEC,
        )
        val signature = base64Url("mock-signature:${user.email}")
        return listOf(
            base64Url(gson.toJson(header)),
            base64Url(gson.toJson(payload)),
            signature,
        ).joinToString(".")
    }

    /**
     * Розібрати payload токена; null — токен пошкоджений або не JWT-формат.
     * Підпис не перевіряється (немає секрету) — валідність визначає лише `exp`.
     */
    fun decode(token: String?): TokenPayload? {
        if (token.isNullOrBlank()) return null
        val parts = token.split('.')
        if (parts.size != 3) return null
        return runCatching {
            val payload = gson.fromJson(
                String(base64Decode(parts[1]), Charsets.UTF_8),
                PayloadJson::class.java,
            )
            TokenPayload(
                subject = payload.sub,
                name = payload.name,
                role = Role.from(payload.role),
                issuedAtEpochSec = payload.iat,
                expiresAtEpochSec = payload.exp,
            )
        }.getOrNull()
    }

    /** base64url без перенесень — так кодуються частини JWT (java.util.Base64
     *  працює і на Android (API 26+), і в юніт-тестах без емулятора). */
    private fun base64Url(text: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(text.toByteArray(Charsets.UTF_8))

    private fun base64Decode(encoded: String): ByteArray =
        Base64.getUrlDecoder().decode(encoded)
}
