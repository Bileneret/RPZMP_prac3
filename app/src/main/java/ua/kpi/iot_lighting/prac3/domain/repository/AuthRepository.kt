package ua.kpi.iot_lighting.prac3.domain.repository

import kotlinx.coroutines.flow.StateFlow
import ua.kpi.iot_lighting.prac3.domain.model.AuthUser

/** Причина завершення сесії — впливає на повідомлення екрана входу. */
enum class LogoutReason {
    /** Користувач сам натиснув «Вийти». */
    USER,

    /** Токен протермінований або відхилений сервером (401). */
    SESSION_EXPIRED,
}

/**
 * Контракт автентифікації (ПЗ11): логін, поточний користувач, вихід.
 *
 * Облікові записи зчитуються з mock-сервера, токен генерується клієнтом —
 * саме такий «mockeded token» передбачений завданням просунутого рівня.
 */
interface AuthRepository {
    /**
     * Реактивний стан поточного користувача; null — не авторизований.
     * StateFlow з початковим значенням зі збереженого токена, тому при
     * старті застосунку екран не «мигає» логіном до першої емісії.
     */
    val currentUser: StateFlow<AuthUser?>

    /**
     * true — сесію завершено через протермінований/відхилений токен.
     * Екран входу показує з цього прапорця повідомлення
     * «Сесія завершена — увійдіть знову» (вимога критерію ПЗ11).
     */
    val sessionExpired: StateFlow<Boolean>

    /**
     * Перевірити облікові дані, згенерувати тестовий токен і зберегти його.
     * @throws ua.kpi.iot_lighting.prac3.domain.model.UiException
     *         з типізованою причиною (мережа/сервер/невірні дані).
     */
    suspend fun login(email: String, password: String): AuthUser

    /** Завершити сесію; [reason] визначає повідомлення на екрані входу. */
    suspend fun logout(reason: LogoutReason = LogoutReason.USER)

    /** Чи наявний валідний (не протермінований) токен. */
    fun hasValidSession(): Boolean
}
