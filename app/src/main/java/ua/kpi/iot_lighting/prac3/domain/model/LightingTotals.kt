package ua.kpi.iot_lighting.prac3.domain.model

/**
 * Агреговані показники поточного стану освітлення.
 *
 * Обчислюються репозиторієм з [LightingDevice][LightDevice] — приклад
 * бізнес-логіки в шарі даних (критерій просунутого рівня ПЗ9/ПЗ10).
 */
data class LightingTotals(
    /** Кількість світильників у системі. */
    val totalDevices: Int,
    /** Скільки з них увімкнено. */
    val poweredDevices: Int,
    /** Сумарна потужність увімкнених світильників, Вт. */
    val activePowerW: Int,
    /** Прогноз споживання увімкнених світильників за добу, кВт·год. */
    val energyKwhPerDay: Double,
    /** Прогноз вартості за добу за поточним тарифом, ₴. */
    val costPerDay: Double,
)
