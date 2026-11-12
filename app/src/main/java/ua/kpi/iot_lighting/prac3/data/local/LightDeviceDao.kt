package ua.kpi.iot_lighting.prac3.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * DAO кешу світильників.
 *
 * Методи викликаються тільки з [ua.kpi.iot_lighting.prac3.data.source.LightingLocalDataSource],
 * ViewModel до DAO напряму не доходить (шар Repository на ПЗ9).
 */
@Dao
interface LightDeviceDao {
    /** Реактивний потік усіх світильників — UI оновлюється сам при зміні кешу. */
    @Query("SELECT * FROM light_devices ORDER BY zone, name")
    fun observeAll(): Flow<List<LightDeviceEntity>>

    /**
     * Замінити ВЕСЬ кеш результатом синхронізації з сервера.
     *
     * Транзакція «спершу очистити, потім вставити» обовʼязкова: звичайний
     * INSERT OR REPLACE оновив би наявні записи, але НЕ видалив би ті,
     * яких на сервері вже немає (видалені іншим клієнтом) — вони б назавжди
     * лишились «привидами» в кеші.
     */
    @Transaction
    suspend fun replaceAll(devices: List<LightDeviceEntity>) {
        clearAll()
        insertAll(devices)
    }

    @Query("DELETE FROM light_devices")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(devices: List<LightDeviceEntity>)

    /** Оновити стан живлення одного світильника після успішного PUT. */
    @Query("UPDATE light_devices SET isPoweredOn = :poweredOn WHERE id = :id")
    suspend fun setPower(id: Long, poweredOn: Boolean)

    /** Додати один світильник (результат POST). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: LightDeviceEntity)

    /** Видалити світильник після успішного DELETE на сервері. */
    @Query("DELETE FROM light_devices WHERE id = :id")
    suspend fun delete(id: Long)
}
