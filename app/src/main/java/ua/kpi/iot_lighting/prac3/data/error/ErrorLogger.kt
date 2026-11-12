package ua.kpi.iot_lighting.prac3.data.error

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Абстракція журналу помилок: репозиторій залежить від інтерфейсу,
 * юніт-тести підставляють fake без Android-контексту.
 */
interface ErrorSink {
    fun log(message: String)
}

/**
 * Журнал помилок у файл `error_log.txt` у внутрішній теці застосунку
 * (просунутий рівень ПЗ12: «реєстрація усіх помилок у файл»).
 *
 * Формат рядка: `<час епохи-мс> <повідомлення>`. Журнал не росте безмежно:
 * після 256 КБ він перейменовується в `error_log.1.txt` і починається новий
 * (одна ротація — демо-достатньо, історія останніх двох «поколінь» зберігається).
 */
class ErrorLogger(context: Context) : ErrorSink {
    private val file = File(context.filesDir, ERROR_LOG_NAME)
    private val rotatedFile = File(context.filesDir, ROTATED_LOG_NAME)

    /** Дописати помилку в журнал; збій логування не має ламати застосунок. */
    override fun log(message: String) {
        Log.e(TAG, message)
        runCatching {
            if (file.length() > MAX_LOG_BYTES) {
                // ротація: старий журнал замінює попередню архівну копію
                file.renameTo(rotatedFile)
            }
            file.appendText("${System.currentTimeMillis()} $message\n")
        }.onFailure {
            Log.e(TAG, "Не вдалося дописати журнал: ${it.localizedMessage}")
        }
    }

    /** Усі записи журналу (для майбутнього перегляду/аналізу). */
    fun readAll(): String =
        if (file.exists()) file.readText() else ""

    private companion object {
        const val ERROR_LOG_NAME = "error_log.txt"
        const val ROTATED_LOG_NAME = "error_log.1.txt"
        const val MAX_LOG_BYTES = 256 * 1024L
        const val TAG = "ErrorLogger"
    }
}
