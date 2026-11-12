package ua.kpi.iot_lighting.prac3

import com.google.gson.JsonParseException
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import ua.kpi.iot_lighting.prac3.data.error.ErrorHandler
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Централізований обробник помилок (ПЗ12): HTTP-коди → типізовані помилки.
 */
class ErrorHandlerTest {

    private fun httpException(code: Int): HttpException =
        HttpException(Response.error<Any>(code, ResponseBody.create(null, "err")))

    @Test
    fun `http 401 — сесія завершена`() {
        assertEquals(ErrorHandler.httpError(401), ua.kpi.iot_lighting.prac3.domain.model.UiError.SessionExpired)
    }

    @Test
    fun `http 403 — доступ заборонено`() {
        assertEquals(
            ua.kpi.iot_lighting.prac3.domain.model.UiError.Forbidden,
            ErrorHandler.toUiError(httpException(403)),
        )
    }

    @Test
    fun `http 404 — не знайдено`() {
        assertEquals(
            ua.kpi.iot_lighting.prac3.domain.model.UiError.NotFound,
            ErrorHandler.toUiError(httpException(404)),
        )
    }

    @Test
    fun `http 5xx — помилка сервера`() {
        assertEquals(
            ua.kpi.iot_lighting.prac3.domain.model.UiError.ServerError,
            ErrorHandler.toUiError(httpException(503)),
        )
    }

    @Test
    fun `таймаут — немає зєднання`() {
        assertEquals(
            ua.kpi.iot_lighting.prac3.domain.model.UiError.NoConnection,
            ErrorHandler.toUiError(SocketTimeoutException("t")),
        )
    }

    @Test
    fun `загальний IOException — немає зєднання`() {
        assertEquals(
            ua.kpi.iot_lighting.prac3.domain.model.UiError.NoConnection,
            ErrorHandler.toUiError(IOException("offline")),
        )
    }

    @Test
    fun `збій парсингу — некоректна відповідь`() {
        assertEquals(
            ua.kpi.iot_lighting.prac3.domain.model.UiError.BadResponse,
            ErrorHandler.toUiError(JsonParseException("bad json")),
        )
    }

    @Test
    fun `транзитивні помилки — таймаут і 5xx, нетранзитивні — 4xx`() {
        assertTrue(ErrorHandler.isTransient(SocketTimeoutException("t")))
        assertTrue(ErrorHandler.isTransient(httpException(500)))
        assertFalse(ErrorHandler.isTransient(httpException(404)))
        assertFalse(ErrorHandler.isTransient(IllegalStateException("x")))
    }
}
