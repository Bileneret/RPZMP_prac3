package ua.kpi.iot_lighting.prac3

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ua.kpi.iot_lighting.prac3.data.error.NetworkAvailability
import ua.kpi.iot_lighting.prac3.data.repository.WeatherRepositoryImpl
import ua.kpi.iot_lighting.prac3.data.source.WeatherConditions
import ua.kpi.iot_lighting.prac3.data.source.WeatherRemoteDataSource
import ua.kpi.iot_lighting.prac3.domain.model.UiError
import ua.kpi.iot_lighting.prac3.domain.model.UiException

/**
 * Тести погодного репозиторію: сирі умови → рекомендація освітлення.
 */
class WeatherRepositoryImplTest {

    private class FakeWeather(var conditions: WeatherConditions? = null, var error: Exception? = null) :
        WeatherRemoteDataSource {
        override suspend fun fetchConditions(): WeatherConditions {
            error?.let { throw it }
            return conditions!!
        }
    }

    private class FakeNetwork(var online: Boolean = true) : NetworkAvailability {
        override fun isOnline(): Boolean = online
    }

    private lateinit var weather: FakeWeather
    private lateinit var network: FakeNetwork
    private lateinit var repository: WeatherRepositoryImpl

    @Before
    fun setUp() {
        weather = FakeWeather()
        network = FakeNetwork()
        repository = WeatherRepositoryImpl(weather, network)
    }

    @Test
    fun `ясний день дає високий рівень та пораду вимкнути`() = runTest {
        weather.conditions = WeatherConditions(isDay = true, cloudCoverPercent = 5, temperatureC = 20.0)
        val insight = repository.insight()
        assertTrue(insight.isDay)
        assertTrue(insight.daylightPercent >= 70)
        assertTrue(insight.recommendation.contains("вимкніть", ignoreCase = true))
    }

    @Test
    fun `ніч дає нульовий рівень і пораду увімкнути`() = runTest {
        weather.conditions = WeatherConditions(isDay = false, cloudCoverPercent = 40, temperatureC = 12.0)
        val insight = repository.insight()
        assertFalse(insight.isDay)
        assertEquals(0, insight.daylightPercent)
        assertTrue(insight.recommendation.contains("увімкніть", ignoreCase = true))
    }

    @Test
    fun `без мережі — типізована помилка NoConnection`() = runTest {
        network.online = false
        val error = runCatching { repository.insight() }
        assertEquals(UiError.NoConnection, (error.exceptionOrNull() as UiException).error)
    }

    @Test
    fun `некоректний JSON — типізована помилка BadResponse`() = runTest {
        weather.error = com.google.gson.JsonParseException("malformed")
        val error = runCatching { repository.insight() }
        assertEquals(UiError.BadResponse, (error.exceptionOrNull() as UiException).error)
    }

    @Test
    fun `таймаут погодного API — NoConnection, а не BadResponse`() = runTest {
        weather.error = java.net.SocketTimeoutException("timeout")
        val error = runCatching { repository.insight() }
        assertEquals(UiError.NoConnection, (error.exceptionOrNull() as UiException).error)
    }
}
