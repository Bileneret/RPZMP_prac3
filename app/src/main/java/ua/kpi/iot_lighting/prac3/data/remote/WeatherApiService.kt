package ua.kpi.iot_lighting.prac3.data.remote

import retrofit2.http.GET
import retrofit2.http.Query
import ua.kpi.iot_lighting.prac3.data.remote.dto.WeatherDto

/**
 * REST-контракт другого віддаленого джерела — Open-Meteo.
 *
 * Параметри запиту передаються як @Query — Retrofit формує URL автоматично.
 */
interface WeatherApiService {
    /**
     * Прогноз для координат обʼєкта: поточні умови + погодинна хмарність.
     * `current`/`hourly` — списки полів через кому, за форматом Open-Meteo.
     */
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,is_day,cloud_cover",
        @Query("hourly") hourly: String = "cloud_cover",
        @Query("forecast_days") forecastDays: Int = 1,
        @Query("timezone") timezone: String = "Europe/Kyiv",
    ): WeatherDto
}
