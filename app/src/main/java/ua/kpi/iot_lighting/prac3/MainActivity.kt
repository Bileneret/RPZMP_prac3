package ua.kpi.iot_lighting.prac3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.kpi.iot_lighting.prac3.di.AppContainer
import ua.kpi.iot_lighting.prac3.ui.screens.about.AboutScreen
import ua.kpi.iot_lighting.prac3.ui.screens.devices.DeviceListScreen
import ua.kpi.iot_lighting.prac3.ui.screens.login.LoginScreen
import ua.kpi.iot_lighting.prac3.ui.theme.IoTLightingTheme

/**
 * Єдина Activity застосунку. Навігація умовна, без Navigation Compose:
 * три екрани перемикаються за станом авторизації та локальним станом —
 * це найпростіше рішення для даного обсягу (спадкоємність з prac1).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as Prac3Application).container
        setContent {
            IoTLightingTheme {
                RootApp(container)
            }
        }
    }
}

@Composable
private fun RootApp(container: AppContainer) {
    val authRepository = container.authRepository
    val user by authRepository.currentUser.collectAsStateWithLifecycle()
    // rememberSaveable: екран «Про застосунок» переживає поворот екрана
    var showAbout by rememberSaveable { mutableStateOf(false) }

    when {
        // не авторизований — екран входу (також після виходу/протермінування)
        user == null -> LoginScreen()
        showAbout -> AboutScreen(onBack = { showAbout = false })
        else -> DeviceListScreen(onOpenAbout = { showAbout = true })
    }
}
