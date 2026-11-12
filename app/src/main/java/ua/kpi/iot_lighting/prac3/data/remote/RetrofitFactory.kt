package ua.kpi.iot_lighting.prac3.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Єдина точка побудови Retrofit-клієнтів.
 *
 * Два базові URL ⇒ два окремі клієнти: освітлення — з [AuthInterceptor]
 * (захищені запити), погода — без авторизації. Gson-конвертер спільний.
 */
object RetrofitFactory {
    /** Таймаут менший за дефолтний — повільний сервер швидше веде до retry. */
    private const val TIMEOUT_SEC = 6L

    /**
     * Клієнт для mock-сервера освітлення з підстановкою Bearer-токена.
     */
    fun createLightingApi(baseUrl: String, authInterceptor: okhttp3.Interceptor): LightingApiService =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(authInterceptor)
                    .connectTimeout(TIMEOUT_SEC, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(TIMEOUT_SEC, java.util.concurrent.TimeUnit.SECONDS)
                    .build(),
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LightingApiService::class.java)

    /**
     * Клієнт для публічного API погоди (без авторизації).
     */
    fun createWeatherApi(baseUrl: String): WeatherApiService =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(TIMEOUT_SEC, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(TIMEOUT_SEC, java.util.concurrent.TimeUnit.SECONDS)
                    .build(),
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApiService::class.java)
}
