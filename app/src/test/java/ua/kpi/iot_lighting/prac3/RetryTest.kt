package ua.kpi.iot_lighting.prac3

import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import ua.kpi.iot_lighting.prac3.data.repository.withRetry
import java.io.IOException

/**
 * Retry-логіка (просунутий рівень ПЗ12): повторюються лише тимчасові збої.
 * runTest дає віртуальний час — delay не гальмує тести.
 */
class RetryTest {

    private fun httpException(code: Int): HttpException =
        HttpException(Response.error<Any>(code, ResponseBody.create(null, "err")))

    @Test
    fun `успіх з першої спроби не ретрається`() = runTest {
        var calls = 0
        val result = withRetry {
            calls++
            "ok"
        }
        assertEquals("ok", result)
        assertEquals(1, calls)
    }

    @Test
    fun `IOException з другої спроби завершується успіхом`() = runTest {
        var calls = 0
        val result = withRetry {
            calls++
            if (calls == 1) throw IOException("flaky") else "ok"
        }
        assertEquals("ok", result)
        assertEquals(2, calls)
    }

    @Test
    fun `HTTP 5xx ретрається`() = runTest {
        var calls = 0
        val result = withRetry {
            calls++
            if (calls == 1) throw httpException(503) else "ok"
        }
        assertEquals("ok", result)
        assertEquals(2, calls)
    }

    @Test
    fun `HTTP 4xx не ретрається — одразу помилка`() = runTest {
        var calls = 0
        assertThrows(HttpException::class.java) {
            kotlinx.coroutines.runBlocking {
                withRetry {
                    calls++
                    throw httpException(404)
                }
            }
        }
        assertEquals(1, calls)
    }

    @Test
    fun `стійкий збій повертає останній виняток після всіх спроб`() = runTest {
        var calls = 0
        assertThrows(IOException::class.java) {
            kotlinx.coroutines.runBlocking {
                withRetry(times = 3) {
                    calls++
                    throw IOException("always down")
                }
            }
        }
        assertEquals(3, calls)
    }
}
