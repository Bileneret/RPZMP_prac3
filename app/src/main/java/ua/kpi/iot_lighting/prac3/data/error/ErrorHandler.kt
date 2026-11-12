package ua.kpi.iot_lighting.prac3.data.error

import com.google.gson.JsonParseException
import retrofit2.HttpException
import ua.kpi.iot_lighting.prac3.domain.model.UiError
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Централізований обробник помилок мережевих запитів (ПЗ12).
 *
 * Єдине місце, де «сирі» винятки Retrofit/Gson/мережі перетворюються
 * на типізований [UiError]. UI ніколи не бачить винятків — тільки
 * готові значення для повідомлень.
 */
object ErrorHandler {

    /**
     * @param throwable виняток, що долетів з Data Source/репозиторію
     */
    fun toUiError(throwable: Throwable): UiError = when (throwable) {
        is HttpException -> httpError(throwable.code())
        is SocketTimeoutException,
        is UnknownHostException -> UiError.NoConnection
        is IOException -> UiError.NoConnection
        is JsonParseException -> UiError.BadResponse
        // Усі інші збої (у т.ч. неочікувані) — серверна сторона з погляду користувача
        else -> UiError.ServerError
    }

    /** HTTP-код → стратегія повідомлення. */
    fun httpError(code: Int): UiError = when (code) {
        401 -> UiError.SessionExpired
        403 -> UiError.Forbidden
        404 -> UiError.NotFound
        in 500..599 -> UiError.ServerError
        else -> UiError.Http(code)
    }

    /** Чи є помилка «тимчасовою» — має сенс автоматичний повтор запиту. */
    fun isTransient(throwable: Throwable): Boolean = when (throwable) {
        is SocketTimeoutException, is UnknownHostException -> true
        is IOException -> true
        is HttpException -> throwable.code() in 500..599
        else -> false
    }
}
