package ua.kpi.iot_lighting.prac3.data.error

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Абстракція доступності мережі: репозиторії залежать від інтерфейсу,
 * тому в юніт-тестах мережу підміняє fake без Android-контексту.
 */
interface NetworkAvailability {
    fun isOnline(): Boolean
}

/**
 * Перевірка наявності інтернет-зʼєднання перед мережевим запитом
 * (базова вимога ПЗ12: «перевірка наявності зʼєднання перед запитом»).
 *
 * Через NetworkCapabilities, а не `activeNetworkInfo` (deprecated):
 * вважаємо мережу придатною, якщо вона має NET_CAPABILITY_INTERNET.
 * Свідомо НЕ вимагаємо NET_CAPABILITY_VALIDATED: mock-сервер доступний
 * і в локальній мережі без виходу в інтернет (adb reverse / LAN).
 */
class NetworkChecker(context: Context) : NetworkAvailability {
    private val connectivity =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun isOnline(): Boolean {
        val network = connectivity.activeNetwork ?: return false
        val caps = connectivity.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
