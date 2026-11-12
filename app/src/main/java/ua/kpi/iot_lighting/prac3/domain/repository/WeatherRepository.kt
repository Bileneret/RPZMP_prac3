package ua.kpi.iot_lighting.prac3.domain.repository

import ua.kpi.iot_lighting.prac3.domain.model.WeatherInsight

/**
 * Контракт другого віддаленого джерела — публічного API погоди Open-Meteo.
 *
 * Показує роботу з кількома REST-ендпоінтами (критерій середнього рівня
 * ПЗ10) та вкладеними JSON-структурами (hourly-масиви з обʼєктами часу).
 */
interface WeatherRepository {
    /**
     * Отримати поточні умови та обчислену рекомендацію освітлення.
     * @throws UiException при відсутності мережі/некоректній відповіді.
     */
    suspend fun insight(): WeatherInsight
}
