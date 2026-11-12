package ua.kpi.iot_lighting.prac3.domain.logic

import ua.kpi.iot_lighting.prac3.domain.model.LightDevice
import ua.kpi.iot_lighting.prac3.domain.model.LightingTotals

/**
 * Бізнес-логіка системи освітлення: перетворення сирих даних на показники.
 *
 * Винесена окремим класом, щоб репозиторій лишався «провідником» між
 * джерелами, а обчислення можна було тестувати без Android/мережі.
 */
object LightingAdvisor {

    /** Діючий тариф на електроенергію, ₴ за кВт·год (демо-значення). */
    const val TARIFF_UAH_PER_KWH: Double = 4.5

    /** Хвилин у добі — для перерахунку потужності в енергію. */
    private const val MINUTES_PER_DAY = 24 * 60

    /**
     * Оцінка природного освітлення, % (0..100) за добою/хмарністю.
     *
     * Правило: вночі 0; вдень базовий рівень знижується хмарністю.
     * Формула проста і детермінована — важлива зрозумілість, а не метео-точність.
     */
    fun daylightPercent(isDay: Boolean, cloudCoverPercent: Int): Int {
        if (!isDay) return 0
        val clamped = cloudCoverPercent.coerceIn(0, 100)
        return (100 - clamped * 0.8).toInt().coerceIn(0, 100)
    }

    /**
     * Текстова порада користувачу на основі рівня природного світла.
     */
    fun recommendation(daylightPercent: Int): String = when {
        daylightPercent >= 70 ->
            "Природного світла достатньо — вимкніть освітлення у зонах з вікнами"
        daylightPercent >= 40 ->
            "Рекомендується часткове освітлення у глибоких зонах"
        else ->
            "Природного світла мало — увімкніть штучне освітлення"
    }

    /**
     * Агрегати для головного екрана: сумарна потужність увімкнених
     * світильників, прогноз енергії та вартості за добу.
     */
    fun totals(devices: List<LightDevice>): LightingTotals {
        val powered = devices.filter { it.isPoweredOn }
        val activePowerW = powered.sumOf { it.powerW }
        // енергія за добу = потужність (кВт) × 24 год
        val energyKwhPerDay = activePowerW / 1000.0 * 24
        val costPerDay = energyKwhPerDay * TARIFF_UAH_PER_KWH
        return LightingTotals(
            totalDevices = devices.size,
            poweredDevices = powered.size,
            activePowerW = activePowerW,
            energyKwhPerDay = energyKwhPerDay,
            costPerDay = costPerDay,
        )
    }
}
