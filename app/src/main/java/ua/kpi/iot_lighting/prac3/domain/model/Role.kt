package ua.kpi.iot_lighting.prac3.domain.model

/**
 * Роль користувача, закодована у токені автентифікації.
 *
 * Керує доступом до UI: ADMIN може додавати й видаляти світильники,
 * USER — лише переглядати список і перемикати живлення.
 */
enum class Role {
    ADMIN,
    USER;

    companion object {
        /** Безпечне розпізнавання ролі з JSON-рядка токена. */
        fun from(raw: String?): Role =
            if (raw.equals(ADMIN.name, ignoreCase = true)) ADMIN else USER
    }
}

/**
 * Авторизований користувач, витягнутий з payload токена.
 */
data class AuthUser(
    val name: String,
    val email: String,
    val role: Role,
)
