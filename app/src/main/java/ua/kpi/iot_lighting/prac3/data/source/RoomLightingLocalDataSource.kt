package ua.kpi.iot_lighting.prac3.data.source

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ua.kpi.iot_lighting.prac3.data.toDomain
import ua.kpi.iot_lighting.prac3.data.toEntity
import ua.kpi.iot_lighting.prac3.data.local.LightDeviceDao
import ua.kpi.iot_lighting.prac3.domain.model.LightDevice

/**
 * Room-реалізація локального джерела.
 *
 * Суто перетворення Entity⇄Domain: будь-яка бізнес-логіка лежить
 * у репозиторії/Advisor, а не тут.
 */
class RoomLightingLocalDataSource(private val dao: LightDeviceDao) : LightingLocalDataSource {

    override fun observe(): Flow<List<LightDevice>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun replaceAll(devices: List<LightDevice>) =
        dao.replaceAll(devices.map { it.toEntity() })

    override suspend fun setPower(id: Long, poweredOn: Boolean) =
        dao.setPower(id, poweredOn)

    override suspend fun add(device: LightDevice) =
        dao.insert(device.toEntity())

    override suspend fun delete(id: Long) =
        dao.delete(id)
}
