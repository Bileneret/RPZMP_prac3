package ua.kpi.iot_lighting.prac3.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ua.kpi.iot_lighting.prac3.ui.theme.AmberLightPrimary
import ua.kpi.iot_lighting.prac3.ui.theme.TextMuted
import ua.kpi.iot_lighting.prac3.ui.viewmodel.LoginViewModel

/**
 * Екран входу (ПЗ11): логін/пароль, валідація полів, помилка авторизації
 * та повідомлення «сесія завершена» після протермінованого токена.
 * Успішний вхід підмінює екран автоматично — через потік currentUser.
 */
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val emailError = state.emailError
    val passwordError = state.passwordError
    val authError = state.authError

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "IoT Lighting",
                style = MaterialTheme.typography.headlineMedium,
                color = AmberLightPrimary,
            )
            Text(
                text = "Система освітлення з IoT · Вхід",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )

            // протермінований токен → користувач розуміє, чому він тут
            if (state.showSessionExpiredNotice) {
                Text(
                    text = "Сесія завершена — увійдіть знову",
                    color = AmberLightPrimary,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                )
            }

            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text("Логін (email)") },
                isError = emailError != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            if (emailError != null) {
                Text(
                    text = emailError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("Пароль") },
                isError = passwordError != null,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            if (passwordError != null) {
                Text(
                    text = passwordError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }
            if (authError != null) {
                Text(
                    text = authError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }

            Button(
                onClick = viewModel::submit,
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = AmberLightPrimary,
                    )
                    Spacer(Modifier.widthIn(min = 8.dp))
                }
                Text(if (state.isLoading) "Входимо…" else "Вхід")
            }

            Text(
                text = "Демо-акаунти: admin@light.io / user@light.io\nпаролі — у даних mock-сервера",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                modifier = Modifier.padding(top = 24.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}
