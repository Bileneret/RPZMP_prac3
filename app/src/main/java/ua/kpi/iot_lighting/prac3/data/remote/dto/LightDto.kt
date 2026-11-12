package ua.kpi.iot_lighting.prac3.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO світильника — структура JSON mock-сервера.
 *
 * Імена полів Gson-конвертер поєднує з Kotlin-властивостями; `@SerializedName`
 * застосовано там, де JSON-ключ відрізняється від ідіоми Kotlin.
 */
data class LightDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("zone") val zone: String,
    @SerializedName("powerW") val powerW: Int,
    @SerializedName("isPoweredOn") val isPoweredOn: Boolean,
)
