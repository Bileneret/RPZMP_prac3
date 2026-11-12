package ua.kpi.iot_lighting.prac3

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.kpi.iot_lighting.prac3.domain.logic.LightingAdvisor
import ua.kpi.iot_lighting.prac3.domain.model.LightDevice

/**
 * Юніт-тести бізнес-логіки освітлення (ПЗ9: бізнес-логіка в шарі даних).
 * Працюють без Android і мережі — чиста функція від вхідних даних.
 */
class LightingAdvisorTest {

    @Test
    fun `вночі рівень природного світла нульовий`() {
        assertEquals(0, LightingAdvisor.daylightPercent(isDay = false, cloudCoverPercent = 0))
    }

    @Test
    fun `ясний день дає високий рівень`() {
        val percent = LightingAdvisor.daylightPercent(isDay = true, cloudCoverPercent = 10)
        assertTrue("Очікувано >70%, отримано $percent", percent >= 70)
    }

    @Test
    fun `суцільна хмара суттєво знижує рівень`() {
        val percent = LightingAdvisor.daylightPercent(isDay = true, cloudCoverPercent = 100)
        assertTrue("Очікувано <30%, отримано $percent", percent < 30)
    }

    @Test
    fun `рекомендація високого рівня радить вимкнути штучне освітлення`() {
        val text = LightingAdvisor.recommendation(80)
        assertTrue(text.contains("вимкніть", ignoreCase = true))
    }

    @Test
    fun `рекомендація низького рівня радить увімкнути`() {
        val text = LightingAdvisor.recommendation(10)
        assertTrue(text.contains("увімкніть", ignoreCase = true))
    }

    @Test
    fun `агрегати рахують лише увімкнені світильники`() {
        val devices = listOf(
            LightDevice(1, "A", "Зал", 100, isPoweredOn = true),
            LightDevice(2, "B", "Зал", 60, isPoweredOn = false),
            LightDevice(3, "C", "Кухня", 40, isPoweredOn = true),
        )
        val totals = LightingAdvisor.totals(devices)
        assertEquals(3, totals.totalDevices)
        assertEquals(2, totals.poweredDevices)
        assertEquals(140, totals.activePowerW)
    }

    @Test
    fun `прогноз енергії та вартості відповідає потужності за добу`() {
        val devices = listOf(
            LightDevice(1, "A", "Зал", 1000, isPoweredOn = true), // 1 кВт
        )
        val totals = LightingAdvisor.totals(devices)
        assertEquals(24.0, totals.energyKwhPerDay, 0.001)
        assertEquals(24.0 * LightingAdvisor.TARIFF_UAH_PER_KWH, totals.costPerDay, 0.001)
    }
}
