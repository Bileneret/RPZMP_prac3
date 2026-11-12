package ua.kpi.iot_lighting.prac3

import org.junit.Assert.assertEquals
import org.junit.Test
import ua.kpi.iot_lighting.prac3.data.local.LightDeviceEntity
import ua.kpi.iot_lighting.prac3.data.remote.dto.LightDto
import ua.kpi.iot_lighting.prac3.data.toDomain
import ua.kpi.iot_lighting.prac3.data.toDto
import ua.kpi.iot_lighting.prac3.data.toEntity
import ua.kpi.iot_lighting.prac3.domain.model.LightDevice

/**
 * Мапери між DTO сервера, Room-записом і доменною моделлю: жодне поле
 * не має губитися при перетвореннях.
 */
class MappersTest {

    private val dto = LightDto(id = 7, name = "Люстра", zone = "Зал", powerW = 72, isPoweredOn = true)

    @Test
    fun `dto to domain зберігає всі поля`() {
        assertEquals(
            LightDevice(7, "Люстра", "Зал", 72, true),
            dto.toDomain(),
        )
    }

    @Test
    fun `domain to dto і назад не втрачає даних`() {
        val device = dto.toDomain()
        assertEquals(device, device.toDto().toDomain())
    }

    @Test
    fun `entity to domain і назад не втрачає даних`() {
        val entity = LightDeviceEntity(3, "Нічник", "Спальня", 10, false)
        assertEquals(entity, entity.toDomain().toEntity())
    }
}
