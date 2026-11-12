package ua.kpi.iot_lighting.prac3.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.kpi.iot_lighting.prac3.domain.model.UiException
import ua.kpi.iot_lighting.prac3.domain.repository.AuthRepository
import ua.kpi.iot_lighting.prac3.domain.validation.CredentialsValidator
import ua.kpi.iot_lighting.prac3.ui.util.userMessage

/**
 * Стан екрану входу (ПЗ11): значення полів, помилки кожного поля,
 * помилка автентифікації, повідомлення про завершену сесію та прогрес запиту.
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val authError: String? = null,
    /** true — показати «Сесія завершена — увійдіть знову» (протермінований токен). */
    val showSessionExpiredNotice: Boolean = false,
    val isLoading: Boolean = false,
)

/**
 * ViewModel логіну. Успіх нічого не робить вручну: AuthRepository
 * оновлює потік поточного користувача, і MainActivity сам показує список.
 *
 * Після успішного входу форма очищується (щоб пароль не залишався
 * в памʼяті екрана при наступному показі), а прапорець протермінованої
 * сесії скидається.
 */
class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    init {
        // протермінована сесія (при старті або після 401) — показати пояснення
        viewModelScope.launch {
            authRepository.sessionExpired.collect { expired ->
                _state.update { it.copy(showSessionExpiredNotice = expired) }
            }
        }
        // успішний вхід — форма більше не потрібна, поля очищуємо
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                if (user != null) _state.value = LoginUiState()
            }
        }
    }

    /** Редагування поля скидає помилки попередньої спроби для цього поля. */
    fun onEmailChange(value: String) = _state.update {
        it.copy(email = value, emailError = null, authError = null)
    }

    fun onPasswordChange(value: String) = _state.update {
        it.copy(password = value, passwordError = null, authError = null)
    }

    /** Натискання «Вхід»: валідація обох полів → запит авторизації. */
    fun submit() {
        val current = _state.value
        if (current.isLoading) return
        val errors = CredentialsValidator.validate(current.email, current.password)
        if (errors.hasErrors) {
            _state.update {
                it.copy(emailError = errors.email, passwordError = errors.password, authError = null)
            }
            return
        }
        _state.update {
            it.copy(isLoading = true, emailError = null, passwordError = null, authError = null)
        }
        viewModelScope.launch {
            try {
                authRepository.login(current.email.trim(), current.password)
                // успіх: currentUser-потік змінить екран; форма очиститься в init-колекторі
                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                val message = (e as? UiException)?.error?.userMessage()
                    ?: "Не вдалося виконати вхід"
                _state.update { it.copy(isLoading = false, authError = message) }
            }
        }
    }

    companion object {
        /** Фабрика з ручним DI — ViewModel не знає про Android-контекст. */
        val Factory = viewModelFactory {
            initializer {
                LoginViewModel(appContainer().authRepository)
            }
        }
    }
}
