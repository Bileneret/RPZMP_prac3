package ua.kpi.iot_lighting.prac3.data.auth

import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp-Interceptor: додає заголовок `Authorization: Bearer <token>`
 * до кожного запиту до mock-сервера освітлення.
 *
 * Це стандартний механізм «управління авторизацією на стороні клієнта»
 * (ПЗ11): ViewModel жодного разу не торкається токена — все у мережевому шарі.
 * Погодний API іде окремим OkHttp-клієнтом без цього Interceptor.
 */
class AuthInterceptor(private val tokenStore: TokenStorage) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = tokenStore.token()
        val request = if (!token.isNullOrBlank()) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }
        return chain.proceed(request)
    }
}
