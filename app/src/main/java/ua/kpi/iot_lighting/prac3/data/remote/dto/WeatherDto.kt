package ua.kpi.iot_lighting.prac3.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO відповіді Open-Meteo (погода) — приклад складної JSON-структури
 * з вкладеними обʼєктами та масивами (критерій ПЗ10).
 *
 * Повна відповідь API ширша; описуємо лише потрібні поля — Gson ігнорує
 * невідомі ключі, тому схема не ламається при розширенні API.
 */
data class WeatherDto(
    @SerializedName("current") val current: Current?,
    @SerializedName("hourly") val hourly: Hourly?,
) {
    /** Поточні умови в Києві. */
    data class Current(
        @SerializedName("time") val time: String?,
        @SerializedName("temperature_2m") val temperatureC: Double?,
        /** 1 — світлова доба, 0 — ніч. */
        @SerializedName("is_day") val isDay: Int?,
        /** Хмарність у %, 0..100. */
        @SerializedName("cloud_cover") val cloudCoverPercent: Int?,
    )

    /** Погодинні масиви — демонструють обробку вкладених списків. */
    data class Hourly(
        @SerializedName("time") val time: List<String>?,
        @SerializedName("cloud_cover") val cloudCover: List<Int>?,
    )
}
