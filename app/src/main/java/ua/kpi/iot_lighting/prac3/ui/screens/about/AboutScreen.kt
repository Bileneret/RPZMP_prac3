package ua.kpi.iot_lighting.prac3.ui.screens.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.kpi.iot_lighting.prac3.ui.theme.AmberLightPrimary
import ua.kpi.iot_lighting.prac3.ui.theme.OnBackgroundDark
import ua.kpi.iot_lighting.prac3.ui.theme.OnPrimaryDark
import ua.kpi.iot_lighting.prac3.ui.theme.TextMuted

private const val GITHUB_URL = "https://github.com/Bileneret/RPZMP_prac3"

private data class Feature(
    val icon: ImageVector,
    val title: String,
    val description: String,
)

/**
 * Екран «Про застосунок» — стиль успадкований від prac2:
 * заголовок проєкту, можливості з іконками, технології та GitHub-посилання.
 * Вихід із сесії — окремою кнопкою на головному екрані (TopAppBar).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val features = listOf(
        Feature(
            icon = Icons.Default.Cloud,
            title = "Синхронізація з сервером",
            description = "REST API, кеш Room і оновлення списку",
        ),
        Feature(
            icon = Icons.Default.VerifiedUser,
            title = "Ролі й доступи",
            description = "Адміністратор і користувач — різні можливості UI",
        ),
        Feature(
            icon = Icons.Default.WbSunny,
            title = "Погода та рекомендації",
            description = "Open-Meteo: денне світло і поради для Кривого Рогу",
        ),
        Feature(
            icon = Icons.Default.CloudOff,
            title = "Офлайн і помилки",
            description = "Повтори запитів, журнал помилок, офлайн-кеш",
        ),
    )

    Scaffold(
        modifier = modifier.fillMaxHeight(),
        topBar = {
            TopAppBar(
                title = { Text("Про застосунок", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(padding)
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "IoT Lighting",
                style = MaterialTheme.typography.headlineMedium,
                color = AmberLightPrimary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Система освітлення з IoT",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )

            SectionHeader("Про проєкт")
            Text(
                text = "Практична робота №3 · Варіант 13",
                style = MaterialTheme.typography.bodyMedium,
                color = OnBackgroundDark,
            )
            Text(
                text = "Недяк Олександр Сергійович · група ТВ-32",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )

            SectionHeader("Можливості")
            features.forEachIndexed { index, feature ->
                FeatureRow(feature)
                if (index < features.lastIndex) ThinDivider()
            }

            SectionHeader("Технології")
            Column(modifier = Modifier.fillMaxWidth()) {
                TechRow("Мова", "Kotlin")
                ThinDivider()
                TechRow("UI", "Jetpack Compose, Material 3")
                ThinDivider()
                TechRow("Архітектура", "MVVM, Repository, Data Source")
                ThinDivider()
                TechRow("Мережа", "Retrofit, Gson")
                ThinDivider()
                TechRow("Сховище", "Room, KSP")
                ThinDivider()
                TechRow("Стан", "ViewModel, StateFlow")
                ThinDivider()
                TechRow("Авторизація", "JWT (mock), Interceptor")
            }

            SectionHeader("Дані")
            Text(
                text = "Світильники зберігаються на mock-сервері, локальний кеш — Room. " +
                    "Демо-акаунти: admin@light.io та user@light.io; паролі — у даних сервера.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )

            Button(
                onClick = { uriHandler.openUri(GITHUB_URL) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberLightPrimary,
                    contentColor = OnPrimaryDark,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                )
                Text(
                    text = "GitHub-репозиторій",
                    modifier = Modifier.padding(start = 6.dp),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = GITHUB_URL,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Секційний заголовок у стилі prac2. */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = AmberLightPrimary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

/** Тонкий розділювач між рядками. */
@Composable
private fun ThinDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
}

/** Рядок можливості: іконка, назва, опис. */
@Composable
private fun FeatureRow(feature: Feature) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = feature.icon,
            contentDescription = null,
            tint = AmberLightPrimary,
            modifier = Modifier.size(20.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = feature.title,
                style = MaterialTheme.typography.bodyMedium,
                color = OnBackgroundDark,
            )
            Text(
                text = feature.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        }
    }
}

/** Рядок технології: підпис ліворуч, значення праворуч. */
@Composable
private fun TechRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = OnBackgroundDark,
        )
    }
}
