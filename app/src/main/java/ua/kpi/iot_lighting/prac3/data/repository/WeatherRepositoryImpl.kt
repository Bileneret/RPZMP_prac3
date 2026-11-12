package ua.kpi.iot_lighting.prac3.data.repository

import ua.kpi.iot_lighting.prac3.data.error.ErrorHandler
import ua.kpi.iot_lighting.prac3.data.error.NetworkAvailability
import ua.kpi.iot_lighting.prac3.data.source.WeatherRemoteDataSource
import ua.kpi.iot_lighting.prac3.domain.logic.LightingAdvisor
import ua.kpi.iot_lighting.prac3.domain.model.UiError
import ua.kpi.iot_lighting.prac3.domain.model.UiException
import ua.kpi.iot_lighting.prac3.domain.model.WeatherInsight
import ua.kpi.iot_lighting.prac3.domain.repository.WeatherRepository

/**
 * Реалізація погодного джерела: сирі умови → рекомендація освітлення.
 *
 * Бізнес-обчислення (daylightPercent, текстова порада) виконуються тут —
 * Weather Data Source повертає лише сирі дані.
 */
class WeatherRepositoryImpl(
    private val remote: WeatherRemoteDataSource,
    private val networkChecker: NetworkAvailability,
) : WeatherRepository {

    override suspend fun insight(): WeatherInsight {
        if (!networkChecker.isOnline()) throw UiException(UiError.NoConnection)
        val conditions = try {
            remote.fetchConditions()
        } catch (t: UiException) {
            throw t
        } catch (t: Throwable) {
            // таймаут Open-Meteo — це відсутність зʼєднання, а не «погана відповідь»
            throw UiException(ErrorHandler.toUiError(t), t)
        }
        val daylight = LightingAdvisor.daylightPercent(conditions.isDay, conditions.cloudCoverPercent)
        return WeatherInsight(
            isDay = conditions.isDay,
            cloudCoverPercent = conditions.cloudCoverPercent,
            temperatureC = conditions.temperatureC,
            daylightPercent = daylight,
            recommendation = LightingAdvisor.recommendation(daylight),
        )
    }
}
