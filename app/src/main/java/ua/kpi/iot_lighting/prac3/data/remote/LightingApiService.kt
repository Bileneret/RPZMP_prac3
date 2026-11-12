package ua.kpi.iot_lighting.prac3.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import ua.kpi.iot_lighting.prac3.data.remote.dto.LightDto
import ua.kpi.iot_lighting.prac3.data.remote.dto.UserDto

/**
 * REST-контракт mock-сервера освітлення (Retrofit, ПЗ10).
 *
 * Сuspend-функції виконуються в корутинах; кожен запит — реальний HTTP-виклик,
 * помилки HTTP приходять як `retrofit2.HttpException`.
 *
 * `/lights*` — захищені ресурси: запити йдуть з Bearer-токеном
 * (див. [ua.kpi.iot_lighting.prac3.data.auth.AuthInterceptor]).
 */
interface LightingApiService {
    /** Список світильників з сервера. */
    @GET("lights")
    suspend fun getLights(): List<LightDto>

    /** Додати світильник (доступно лише адміністратору). */
    @POST("lights")
    suspend fun addLight(@Body light: LightDto): LightDto

    /** Оновити стан світильника — використовується для перемикача живлення. */
    @PUT("lights/{id}")
    suspend fun updateLight(@Path("id") id: Long, @Body light: LightDto): LightDto

    /** Видалити світильник (доступно лише адміністратору). */
    @DELETE("lights/{id}")
    suspend fun deleteLight(@Path("id") id: Long)

    /** Облікові записи для mocked-автентифікації (перевірка логіна/пароля). */
    @GET("users")
    suspend fun getUsers(): List<UserDto>
}
