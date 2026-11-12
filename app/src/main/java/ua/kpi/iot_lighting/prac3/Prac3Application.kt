package ua.kpi.iot_lighting.prac3

import android.app.Application
import ua.kpi.iot_lighting.prac3.di.AppContainer

/**
 * Точка створення DI-графа: контейнер живе стільки ж, скільки застосунок,
 * і передається ViewModel через фабрики (див. ui/viewmodel).
 */
class Prac3Application : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
