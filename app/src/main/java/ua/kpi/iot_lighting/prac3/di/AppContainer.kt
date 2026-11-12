package ua.kpi.iot_lighting.prac3.di

import android.content.Context
import ua.kpi.iot_lighting.prac3.data.auth.AuthInterceptor
import ua.kpi.iot_lighting.prac3.data.auth.TokenStore
import ua.kpi.iot_lighting.prac3.data.error.ErrorLogger
import ua.kpi.iot_lighting.prac3.data.error.NetworkChecker
import ua.kpi.iot_lighting.prac3.data.local.AppDatabase
import ua.kpi.iot_lighting.prac3.data.local.SharedPreferencesCacheMetadata
import ua.kpi.iot_lighting.prac3.data.remote.RetrofitFactory
import ua.kpi.iot_lighting.prac3.data.remote.ServerConfig
import ua.kpi.iot_lighting.prac3.data.repository.AuthRepositoryImpl
import ua.kpi.iot_lighting.prac3.data.repository.LightingRepositoryImpl
import ua.kpi.iot_lighting.prac3.data.repository.WeatherRepositoryImpl
import ua.kpi.iot_lighting.prac3.data.source.LightingLocalDataSource
import ua.kpi.iot_lighting.prac3.data.source.LightingRemoteDataSource
import ua.kpi.iot_lighting.prac3.data.source.OpenMeteoWeatherDataSource
import ua.kpi.iot_lighting.prac3.data.source.RetrofitLightingRemoteDataSource
import ua.kpi.iot_lighting.prac3.data.source.RoomLightingLocalDataSource
import ua.kpi.iot_lighting.prac3.data.source.WeatherRemoteDataSource
import ua.kpi.iot_lighting.prac3.domain.repository.AuthRepository
import ua.kpi.iot_lighting.prac3.domain.repository.LightingRepository
import ua.kpi.iot_lighting.prac3.domain.repository.WeatherRepository

/**
 * Ручний DI-контейнер (без Hilt — як у prac2): один граф залежностей
 * для всього застосунку, створюється в [Prac3Application].
 *
 * Порядок побудови: сховище → мережа → Data Sources → репозиторії.
 * ViewModel отримує лише інтерфейси репозиторіїв — підміна джерел
 * не зачіпає UI.
 */
class AppContainer(context: Context) {

    // — сховище та метадані —
    val tokenStore = TokenStore(context)
    val cacheMetadata = SharedPreferencesCacheMetadata(context)
    val errorLogger = ErrorLogger(context)
    val networkChecker = NetworkChecker(context)

    // — локальний кеш Room —
    private val database = AppDatabase.build(context)
    private val deviceDao = database.lightDeviceDao()
    private val localDataSource: LightingLocalDataSource =
        RoomLightingLocalDataSource(deviceDao)

    // — віддалені джерела —
    private val lightingApi = RetrofitFactory.createLightingApi(
        baseUrl = ServerConfig.LIGHTING_BASE_URL,
        authInterceptor = AuthInterceptor(tokenStore),
    )
    private val weatherApi = RetrofitFactory.createWeatherApi(ServerConfig.WEATHER_BASE_URL)
    private val lightingRemoteDataSource: LightingRemoteDataSource =
        RetrofitLightingRemoteDataSource(lightingApi, tokenStore)
    private val weatherRemoteDataSource: WeatherRemoteDataSource =
        OpenMeteoWeatherDataSource(weatherApi)

    // — репозиторії (інтерфейси для UI) —
    val lightingRepository: LightingRepository = LightingRepositoryImpl(
        local = localDataSource,
        remote = lightingRemoteDataSource,
        networkChecker = networkChecker,
        cacheMetadata = cacheMetadata,
        errorLogger = errorLogger,
    )
    val weatherRepository: WeatherRepository = WeatherRepositoryImpl(
        remote = weatherRemoteDataSource,
        networkChecker = networkChecker,
    )
    val authRepository: AuthRepository = AuthRepositoryImpl(
        remote = lightingRemoteDataSource,
        tokenStorage = tokenStore,
        networkChecker = networkChecker,
        errorSink = errorLogger,
    )
}
