package ua.kpi.iot_lighting.prac3.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ua.kpi.iot_lighting.prac3.ui.theme.AmberLightPrimary
import ua.kpi.iot_lighting.prac3.ui.theme.BorderSlate
import ua.kpi.iot_lighting.prac3.ui.theme.OnBackgroundDark
import ua.kpi.iot_lighting.prac3.ui.theme.OnPrimaryDark
import ua.kpi.iot_lighting.prac3.ui.theme.TextMuted

/**
 * Спільні composable-компоненти застосунку. Використовуються кількома
 * екранами — повторне використання UI-частин (критерій ПЗ9/ПЗ12).
 */

/**
 * Перемикач живлення — вигляд ідентичний prac2: amber-трек у ввімкненому
 * стані, контрастний сірий у вимкненому (вимога контрасту станів).
 */
@Composable
fun PowerSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = OnPrimaryDark,
            checkedTrackColor = AmberLightPrimary,
            uncheckedThumbColor = OnBackgroundDark,
            uncheckedTrackColor = BorderSlate,
            uncheckedBorderColor = TextMuted,
        ),
    )
}

/** Індикатор первинного завантаження списку. */
@Composable
fun LoadingState(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = AmberLightPrimary,
        )
        Text(message, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
    }
}

/**
 * Повноекранний стан помилки завантаження з кнопкою «Повторити» —
 * основна форма інформування про невдалий мережевий запит (ПЗ12).
 */
@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(40.dp),
        )
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = OnBackgroundDark,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(onClick = onRetry) {
            Text("Повторити")
        }
    }
}

/** Текстовий статус живлення — «Увімкнено/Вимкнено», як у prac2. */
@Composable
fun PowerStatusText(isPoweredOn: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = if (isPoweredOn) "Увімкнено" else "Вимкнено",
        style = MaterialTheme.typography.labelMedium,
        color = if (isPoweredOn) AmberLightPrimary else TextMuted,
        modifier = modifier,
    )
}

/**
 * Діалог підтвердження (видалення світильника).
 *
 * Захист від подвійного натискання: HyperOS інколи дублює подію тапа,
 * і без прапорця один клік давав два DELETE (204 + 404).
 */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmed by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(
                onClick = {
                    if (confirmed) return@TextButton
                    confirmed = true
                    onConfirm()
                    onDismiss()
                },
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Скасувати") }
        },
    )
}

/**
 * Діалог додавання світильника (операція адміністратора, ПЗ10 POST).
 * Валідація полів проста: назва і зона не порожні, потужність > 0.
 * Поля — rememberSaveable: введені дані переживають поворот екрана.
 */
@Composable
fun AddDeviceDialog(
    onAdd: (name: String, zone: String, powerW: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var zone by rememberSaveable { mutableStateOf("") }
    var power by rememberSaveable { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    val powerValue = power.toIntOrNull()
    val valid = !submitted && name.isNotBlank() && zone.isNotBlank() && (powerValue ?: 0) > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новий світильник", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Назва") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = zone,
                    onValueChange = { zone = it },
                    label = { Text("Зона") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = power,
                    onValueChange = { power = it.filter(Char::isDigit) },
                    label = { Text("Потужність, Вт") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    if (submitted) return@TextButton
                    submitted = true
                    onAdd(name.trim(), zone.trim(), powerValue!!)
                    onDismiss()
                },
            ) { Text("Додати") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } },
    )
}
