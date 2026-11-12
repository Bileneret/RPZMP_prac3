package ua.kpi.iot_lighting.prac3.domain.validation

/**
 * Помилки полів форми входу (окремо для кожного поля, щоб UI підсвітив обидва).
 */
data class CredentialsErrors(
    val email: String? = null,
    val password: String? = null,
) {
    val hasErrors: Boolean get() = email != null || password != null
}

/**
 * Валідація форми входу (базова перевірка введених значень — вимога
 * домашнього завдання ПЗ11: поля не мають бути порожніми).
 */
object CredentialsValidator {

    /** @return помилки для кожного поля; жодної — якщо дані коректні. */
    fun validate(email: String, password: String): CredentialsErrors = CredentialsErrors(
        email = when {
            email.isBlank() -> "Введіть логін"
            !email.contains('@') -> "Логін має вигляд email (наприклад, admin@light.io)"
            else -> null
        },
        password = when {
            password.isBlank() -> "Введіть пароль"
            password.length < 4 -> "Пароль надто короткий (мінімум 4 символи)"
            else -> null
        },
    )
}
