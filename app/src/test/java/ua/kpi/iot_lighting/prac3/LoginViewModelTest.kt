package ua.kpi.iot_lighting.prac3

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ua.kpi.iot_lighting.prac3.domain.model.AuthUser
import ua.kpi.iot_lighting.prac3.domain.model.Role
import ua.kpi.iot_lighting.prac3.domain.model.UiError
import ua.kpi.iot_lighting.prac3.domain.model.UiException
import ua.kpi.iot_lighting.prac3.domain.repository.AuthRepository
import ua.kpi.iot_lighting.prac3.domain.repository.LogoutReason
import ua.kpi.iot_lighting.prac3.ui.viewmodel.LoginViewModel

/**
 * Тести ViewModel екрану входу (ПЗ11): валідація полів, очищення форми
 * після успіху, показ помилки сервера та повідомлення про завершену сесію.
 */
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeAuthRepository : AuthRepository {
        val userFlow = MutableStateFlow<AuthUser?>(null)
        val expiredFlow = MutableStateFlow(false)
        var loginFailure: UiException? = null
        var lastLogoutReason: LogoutReason? = null

        override val currentUser: StateFlow<AuthUser?> = userFlow
        override val sessionExpired: StateFlow<Boolean> = expiredFlow

        override suspend fun login(email: String, password: String): AuthUser {
            loginFailure?.let { throw it }
            val user = AuthUser("Тест", email, Role.USER)
            userFlow.value = user
            return user
        }

        override suspend fun logout(reason: LogoutReason) {
            lastLogoutReason = reason
            userFlow.value = null
            expiredFlow.value = reason == LogoutReason.SESSION_EXPIRED
        }

        override fun hasValidSession(): Boolean = userFlow.value != null
    }

    @Test
    fun `порожні поля дають дві помилки одразу`() = runTest {
        val vm = LoginViewModel(FakeAuthRepository())
        vm.submit()
        val state = vm.state.value
        assertEquals("Введіть логін", state.emailError)
        assertEquals("Введіть пароль", state.passwordError)
        assertFalse(state.isLoading)
    }

    @Test
    fun `невірний email підсвічує лише поле логіна`() = runTest {
        val vm = LoginViewModel(FakeAuthRepository())
        vm.onEmailChange("admin")
        vm.onPasswordChange("admin123")
        vm.submit()
        assertEquals("Логін має вигляд email (наприклад, admin@light.io)", vm.state.value.emailError)
        assertNull(vm.state.value.passwordError)
    }

    @Test
    fun `успішний вхід очищує форму`() = runTest {
        val vm = LoginViewModel(FakeAuthRepository())
        vm.onEmailChange("admin@light.io")
        vm.onPasswordChange("admin123")
        vm.submit()
        val state = vm.state.value
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertNull(state.authError)
    }

    @Test
    fun `помилка сервера показується типізовано`() = runTest {
        val repository = FakeAuthRepository().apply {
            loginFailure = UiException(UiError.ServerError)
        }
        val vm = LoginViewModel(repository)
        vm.onEmailChange("admin@light.io")
        vm.onPasswordChange("admin123")
        vm.submit()
        assertEquals("Сталася помилка сервера, спробуйте пізніше", vm.state.value.authError)
    }

    @Test
    fun `прапорець протермінованої сесії показує повідомлення`() = runTest {
        val repository = FakeAuthRepository()
        val vm = LoginViewModel(repository)
        repository.expiredFlow.value = true
        assertTrue(vm.state.value.showSessionExpiredNotice)
    }
}
