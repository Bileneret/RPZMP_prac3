package ua.kpi.iot_lighting.prac3.data.source

import ua.kpi.iot_lighting.prac3.data.toDomain
import ua.kpi.iot_lighting.prac3.data.toDto
import ua.kpi.iot_lighting.prac3.data.auth.TokenStore
import ua.kpi.iot_lighting.prac3.data.remote.LightingApiService
import ua.kpi.iot_lighting.prac3.data.remote.dto.LightDto
import ua.kpi.iot_lighting.prac3.domain.model.LightDevice
import ua.kpi.iot_lighting.prac3.domain.model.Role
import ua.kpi.iot_lighting.prac3.domain.model.UiError
import ua.kpi.iot_lighting.prac3.domain.model.UiException

/**
 * Retrofit-реалізація віддаленого джерела освітлення.
 *
 * Саме тут знаходиться концепція «захищеного ресурсу» (ПЗ11): перед
 * кожним запитом перевіряється валідність токена. Немає/протермінований —
 * кидається [UiException] з [UiError.SessionExpired], і ViewModel
 * повертає користувача на екран входу.
 */
class RetrofitLightingRemoteDataSource(
    private val api: LightingApiService,
    private val tokenStore: TokenStore,
) : LightingRemoteDataSource {

    override suspend fun fetchLights(): List<LightDevice> {
        requireSession()
        return api.getLights().map { it.toDomain() }
    }

    override suspend fun setPower(device: LightDevice, poweredOn: Boolean): LightDevice {
        requireSession()
        val updated = device.copy(isPoweredOn = poweredOn)
        return api.updateLight(device.id, updated.toDto()).toDomain()
    }

    override suspend fun addLight(name: String, zone: String, powerW: Int): LightDevice {
        requireSession()
        return api.addLight(LightDto(id = 0, name = name, zone = zone, powerW = powerW, isPoweredOn = false)).toDomain()
    }

    override suspend fun deleteLight(id: Long) {
        requireSession()
        try {
            api.deleteLight(id)
        } catch (e: retrofit2.HttpException) {
            // DELETE — идемпотентна операція (стандарт REST): видалення запису,
            // якого вже немає (повторний клік/конкуруючий клієнт), вважається успіхом.
            if (e.code() != 404) throw e
        }
    }

    override suspend fun fetchUsers(): List<AuthUserRecord> =
        api.getUsers().map { user ->
            AuthUserRecord(
                email = user.email,
                password = user.password,
                name = user.name,
                role = Role.from(user.role),
            )
        }

    /** Захищений ресурс: без валідного (не протермінованого) токена входимо. */
    private fun requireSession() {
        if (!tokenStore.hasValidSession()) throw UiException(UiError.SessionExpired)
    }
}
