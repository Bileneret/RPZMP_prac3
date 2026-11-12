package ua.kpi.iot_lighting.prac3

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ua.kpi.iot_lighting.prac3.data.local.AppDatabase
import ua.kpi.iot_lighting.prac3.data.local.LightDeviceEntity

/**
 * Інструментальні тести Room-кешу (ПЗ9, локальний Data Source).
 * Використовується in-memory база — файл на диску не створюється.
 */
@RunWith(AndroidJUnit4::class)
class LightDeviceDaoTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun device(id: Long, name: String, poweredOn: Boolean = false) = LightDeviceEntity(
        id = id,
        name = name,
        zone = "Зал",
        powerW = 60,
        isPoweredOn = poweredOn,
    )

    @Test
    fun replaceAllПерезаписуєКеш() = runTest {
        val dao = database.lightDeviceDao()
        dao.replaceAll(listOf(device(1, "A"), device(2, "B")))
        dao.replaceAll(listOf(device(3, "C")))
        assertEquals(listOf("C"), dao.observeAll().first().map { it.name })
    }

    @Test
    fun setPowerОновлюєТількиВибранийЗапис() = runTest {
        val dao = database.lightDeviceDao()
        dao.replaceAll(listOf(device(1, "A"), device(2, "B", poweredOn = true)))
        dao.setPower(1, poweredOn = true)
        dao.setPower(2, poweredOn = false)
        val rows = dao.observeAll().first()
        assertTrue(rows.first { it.id == 1L }.isPoweredOn)
        assertFalse(rows.first { it.id == 2L }.isPoweredOn)
    }

    @Test
    fun deleteВидаляєЗапис() = runTest {
        val dao = database.lightDeviceDao()
        dao.replaceAll(listOf(device(1, "A"), device(2, "B")))
        dao.delete(2)
        assertNull(dao.observeAll().first().find { it.id == 2L })
        assertEquals(1, dao.observeAll().first().size)
    }
}
