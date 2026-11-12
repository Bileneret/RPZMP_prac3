package ua.kpi.iot_lighting.prac3.data.repository

import kotlinx.coroutines.delay
import ua.kpi.iot_lighting.prac3.data.error.ErrorHandler

/**
 * Автоматичний повтор запиту при тимчасових помилках (просунутий рівень ПЗ12).
 *
 * Ретраються лише «можливо минущі» збої: таймаут, відсутність DNS/мережі,
 * HTTP 5xx. Помилки авторизації/4xx повторювати безглуздо — вони детерміновані.
 *
 * ВАЖЛИВО: ретрай використовується тільки для ідемпотентних операцій
 * (GET/PUT/DELETE). POST (створення світильника) не ретраїться в
 * репозиторії, щоб таймаут після успішної обробки на сервері не створив дубль.
 *
 * @param times загальна кількість спроб (перша + повтори)
 * @param delayMs пауза між спробами — дає серверу шанс оговтатись
 */
suspend fun <T> withRetry(
    times: Int = RETRY_COUNT,
    delayMs: Long = RETRY_DELAY_MS,
    block: suspend () -> T,
): T {
    var lastError: Throwable? = null
    repeat(times) { attempt ->
        try {
            return block()
        } catch (t: Throwable) {
            if (!ErrorHandler.isTransient(t)) throw t
            lastError = t
            if (attempt < times - 1) delay(delayMs)
        }
    }
    throw lastError ?: IllegalStateException("withRetry завершився без результату")
}

private const val RETRY_COUNT = 2
private const val RETRY_DELAY_MS = 800L
