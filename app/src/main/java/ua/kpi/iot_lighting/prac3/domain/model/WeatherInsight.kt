package ua.kpi.iot_lighting.prac3.domain.model

/**
 * Погодне спостереження та обчислена рекомендація щодо освітлення.
 *
 * Дані приходять з публічного API Open-Meteo (друге віддалене джерело),
 * а `daylightPercent` і `recommendation` — результат бізнес-логіки
 * [ua.kpi.iot_lighting.prac3.domain.logic.LightingAdvisor].
 */
data class WeatherInsight(
    /** true — зараз світлова доба в Києві за даними API. */
    val isDay: Boolean,
    /** Хмарність на поточну годину, % (0 — ясно, 100 — суцільна хмара). */
    val cloudCoverPercent: Int,
    /** Температура повітря, °C (показує «живість» зовнішніх даних). */
    val temperatureC: Double,
    /** Оцінка рівня природного освітлення, % (0 — темно, 100 — яскраво). */
    val daylightPercent: Int,
    /** Текстова порада користувачу, збудована з daylightPercent. */
    val recommendation: String,
)
