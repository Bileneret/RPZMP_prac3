package ua.kpi.iot_lighting.prac3.data.remote

/**
 * Конфігурація мережевого шару: базові URL обох віддалених джерел.
 */
object ServerConfig {
    /**
     * Адреса mock-сервера освітлення (`tools/mock_server.py` на ПК).
     *
     * Застосунок звертається на `127.0.0.1:8080`, а Android Debug Bridge
     * перенаправляє цей порт на компʼютер:
     * `adb reverse tcp:8080 tcp:8080`.
     * Альтернатива без ADB — вказати LAN-адресу ПК (наприклад `http://192.168.1.50:8080/`).
     */
    const val LIGHTING_BASE_URL = "http://127.0.0.1:8080/"

    /** Публічний API погоди Open-Meteo — без ключа і без реєстрації. */
    const val WEATHER_BASE_URL = "https://api.open-meteo.com/"

    /** Координати Кривого Рогу — міста, де розташований обʼєкт системи освітлення. */
    const val KRYVYI_RIH_LATITUDE = 47.9105
    const val KRYVYI_RIH_LONGITUDE = 33.3918
}
