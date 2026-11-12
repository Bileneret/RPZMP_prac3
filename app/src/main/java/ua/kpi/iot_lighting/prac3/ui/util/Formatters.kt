package ua.kpi.iot_lighting.prac3.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Форматування часу та чисел. Формати створюються на кожен виклик, щоб
 * враховувати поточну локаль пристрою (top-level SimpleDateFormat «залипає»
 * на локалі, що була при старті процесу).
 */

/** «11.11 14:07» — позначка часу останньої синхронізації/офлайн-даних. */
fun formatDateTime(epochMs: Long): String =
    SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(epochMs))

/** «24,50 кВт·год». */
fun formatKwh(kwh: Double): String =
    "${String.format(Locale.getDefault(), "%.2f", kwh)} кВт·год"

/** «110,25 ₴». */
fun formatMoney(uah: Double): String =
    "${String.format(Locale.getDefault(), "%.2f", uah)} ₴"
