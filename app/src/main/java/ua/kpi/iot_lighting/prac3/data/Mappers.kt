package ua.kpi.iot_lighting.prac3.data

import ua.kpi.iot_lighting.prac3.data.local.LightDeviceEntity
import ua.kpi.iot_lighting.prac3.data.remote.dto.LightDto
import ua.kpi.iot_lighting.prac3.domain.model.LightDevice

/**
 * Мапери між трьома поданнями світильника: DTO сервера, Room-запис,
 * доменна модель. Домен ніколи не бачить ані JSON, ані SQLite.
 */
fun LightDto.toDomain() = LightDevice(id = id, name = name, zone = zone, powerW = powerW, isPoweredOn = isPoweredOn)

fun LightDevice.toDto() = LightDto(id = id, name = name, zone = zone, powerW = powerW, isPoweredOn = isPoweredOn)

fun LightDeviceEntity.toDomain() =
    LightDevice(id = id, name = name, zone = zone, powerW = powerW, isPoweredOn = isPoweredOn)

fun LightDevice.toEntity() =
    LightDeviceEntity(id = id, name = name, zone = zone, powerW = powerW, isPoweredOn = isPoweredOn)
