package ua.kpi.iot_lighting.prac3.data.auth

import android.content.Context
import androidx.core.content.edit
import ua.kpi.iot_lighting.prac3.domain.model.AuthUser
import ua.kpi.iot_lighting.prac3.domain.model.TokenPayload

/**
 * Локальне сховище токена автентифікації (SharedPreferences).
 *
 * Токен зберігається між запусками: після перезапуску застосунок
 * одразу потрапляє у список, якщо токен ще дійсний. Демо-спрощення:
 * токен у SharedPreferences відкритим текстом (у продуктивних застосунках
 * — EncryptedSharedPreferences або DataStore).
 */
class TokenStore(context: Context) : TokenStorage {
    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    override fun save(token: String) {
        prefs.edit { putString(KEY_TOKEN, token) }
    }

    override fun token(): String? = prefs.getString(KEY_TOKEN, null)

    override fun payload(): TokenPayload? = MockTokenFactory.decode(token())

    override fun hasValidSession(nowEpochSec: Long): Boolean {
        val payload = payload() ?: return false
        return !payload.isExpired(nowEpochSec)
    }

    /**
     * Поточний користувач лише з дійсним токеном: протермінована сесія
     * не дає права показувати екран списку (краще одразу екран входу).
     */
    override fun currentUser(): AuthUser? {
        if (!hasValidSession()) return null
        val payload = payload() ?: return null
        return AuthUser(name = payload.name, email = payload.subject, role = payload.role)
    }

    override fun clear() {
        prefs.edit { remove(KEY_TOKEN) }
    }

    private companion object {
        const val KEY_TOKEN = "token"
    }
}
