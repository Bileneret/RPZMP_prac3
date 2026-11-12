package ua.kpi.iot_lighting.prac3.data.local

import android.content.Context
import androidx.core.content.edit

/**
 * Метадані кешу: момент останньої успішної синхронізації з сервером.
 *
 * Потрібне для позначки «офлайн-дані від HH:MM» — користувач має бачити,
 * що бачить кешовані дані, а не живий сервер. Інтерфейс (а не клас)
 * дозволяє юніт-тестам репозиторію підставляти fake без Android.
 */
interface CacheMetadata {
    /** Момент останньої синхронізації (епоха-мс) або null, якщо її не було. */
    fun lastSyncAt(): Long?

    /** Записати момент успішного [ua.kpi.iot_lighting.prac3.domain.repository.LightingRepository.refreshFromRemote]. */
    fun markSynced(atEpochMs: Long)
}

/**
 * SharedPreferences-реалізація метаданих кешу.
 */
class SharedPreferencesCacheMetadata(context: Context) : CacheMetadata {
    private val prefs = context.getSharedPreferences("cache_meta", Context.MODE_PRIVATE)

    override fun lastSyncAt(): Long? =
        prefs.getLong(KEY_LAST_SYNC, 0L).takeIf { it > 0L }

    override fun markSynced(atEpochMs: Long) {
        prefs.edit { putLong(KEY_LAST_SYNC, atEpochMs) }
    }

    private companion object {
        const val KEY_LAST_SYNC = "last_sync"
    }
}
