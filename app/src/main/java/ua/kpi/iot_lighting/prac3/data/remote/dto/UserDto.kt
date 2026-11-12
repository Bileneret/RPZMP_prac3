package ua.kpi.iot_lighting.prac3.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO облікового запису користувача mock-сервера (ендпоінт `users`).
 *
 * Пароль у демо-даних відкритим текстом — так передбачено завданням
 * (mockeded auth); у реальній системі пароль передається лише в POST /login
 * і зберігається на сервері у хешованому вигляді.
 */
data class UserDto(
    @SerializedName("id") val id: Long,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("name") val name: String,
    @SerializedName("role") val role: String,
)
