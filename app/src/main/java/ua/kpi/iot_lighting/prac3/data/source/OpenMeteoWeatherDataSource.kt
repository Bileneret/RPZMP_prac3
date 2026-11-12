package ua.kpi.iot_lighting.prac3.data.source

import ua.kpi.iot_lighting.prac3.data.remote.ServerConfig
import ua.kpi.iot_lighting.prac3.data.remote.WeatherApiService
import ua.kpi.iot_lighting.prac3.domain.model.UiError
import ua.kpi.iot_lighting.prac3.domain.model.UiException

/**
 * Open-Meteo реалізація погодного джерела.
 *
 * Розбір відповіді мінімальний: з вкладеного обʼєкта `current` беруться
 * три значення; null-поля з API трактуються консервативно (ніч, ясна погода).
 */
class OpenMeteoWeatherDataSource(private val api: WeatherApiService) : WeatherRemoteDataSource {

    override suspend fun fetchConditions(): WeatherConditions {
        val response = api.forecast(
            latitude = ServerConfig.KRYVYI_RIH_LATITUDE,
            longitude = ServerConfig.KRYVYI_RIH_LONGITUDE,
        )
        val current = response.current
            ?: throw UiException(UiError.BadResponse)
        return WeatherConditions(
            isDay = (current.isDay ?: 0) == 1,
            cloudCoverPercent = current.cloudCoverPercent ?: 0,
            temperatureC = current.temperatureC ?: 0.0,
        )
    }
}
