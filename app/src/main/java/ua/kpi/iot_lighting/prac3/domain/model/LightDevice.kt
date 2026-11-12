package ua.kpi.iot_lighting.prac3.domain.model

/**
 * Доменна модель світильника системи освітлення з IoT (варіант 13).
 *
 * Ця модель не залежить від Room чи Retrofit: шар даних віддає UI саме її,
 * тому заміна локального/віддаленого джерела не впливає на інтерфейс.
 */
data class LightDevice(
    val id: Long,
    val name: String,
    /** Зона монтажу (наприклад, «Коридор», «Зал») — текст з mock-сервера. */
    val zone: String,
    /** Номінальна потужність світильника, Вт. */
    val powerW: Int,
    /** Єдиний стан живлення: увімкнено/вимкнено (спадкоємність з prac2). */
    val isPoweredOn: Boolean,
)
