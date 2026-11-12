package ua.kpi.iot_lighting.prac3.data.source

import ua.kpi.iot_lighting.prac3.domain.model.WeatherInsight

/**
 * Друге віддалене джерело — API погоди.
 *
 * Інтерфейс дозволяє в юніт-тестах підставити fake-погоду замість HTTP.
 */
interface WeatherRemoteDataSource {
    /** Поточні умови в Києві (Open-Meteo) без рекомендації — вона обчислюється у репозиторії. */
    suspend fun fetchConditions(): WeatherConditions
}

/**
 * Сирі погодні дані до обробки бізнес-логікою.
 */
data class WeatherConditions(
    val isDay: Boolean,
    val cloudCoverPercent: Int,
    val temperatureC: Double,
)
