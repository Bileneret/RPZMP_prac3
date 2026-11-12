package ua.kpi.iot_lighting.prac3.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room-запис світильника — локальний кеш даних з mock-сервера (ПЗ9/ПЗ10).
 *
 * Кеш дає офлайн-доступ: при відсутності мережі список показується з нього
 * з позначкою часу останньої синхронізації.
 */
@Entity(tableName = "light_devices")
data class LightDeviceEntity(
    /** Ідентифікатор з сервера — і для GET/PUT/DELETE, тому PrimaryKey. */
    @PrimaryKey val id: Long,
    val name: String,
    val zone: String,
    val powerW: Int,
    val isPoweredOn: Boolean,
)
