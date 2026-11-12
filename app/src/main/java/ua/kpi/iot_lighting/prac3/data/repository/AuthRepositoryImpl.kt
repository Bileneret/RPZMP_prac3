package ua.kpi.iot_lighting.prac3.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ua.kpi.iot_lighting.prac3.data.auth.MockTokenFactory
import ua.kpi.iot_lighting.prac3.data.auth.TokenStorage
import ua.kpi.iot_lighting.prac3.data.error.ErrorHandler
import ua.kpi.iot_lighting.prac3.data.error.ErrorSink
import ua.kpi.iot_lighting.prac3.data.error.NetworkAvailability
import ua.kpi.iot_lighting.prac3.data.source.LightingRemoteDataSource
import ua.kpi.iot_lighting.prac3.domain.model.AuthUser
import ua.kpi.iot_lighting.prac3.domain.model.UiError
import ua.kpi.iot_lighting.prac3.domain.model.UiException
import ua.kpi.iot_lighting.prac3.domain.repository.AuthRepository
import ua.kpi.iot_lighting.prac3.domain.repository.LogoutReason
import java.security.MessageDigest

/**
 * Реалізація автентифікації (ПЗ11).
 *
 * Сценарій входу:
 *  1. з mock-сервера зчитуються облікові записи (GET /users);
 *  2. логін/пароль перевіряються клієнтом (mockeded auth за завданням);
 *  3. клієнт генерує тестовий JWT-подібний токен ([MockTokenFactory])
 *     і зберігає його в [TokenStorage];
 *  4. подальші запити до захищених ресурсів отримують Bearer-токен
 *     автоматично через AuthInterceptor.
 */
class AuthRepositoryImpl(
    private val remote: LightingRemoteDataSource,
    private val tokenStorage: TokenStorage,
    private val networkChecker: NetworkAvailability,
    private val errorSink: ErrorSink,
) : AuthRepository {

    /**
     * Протермінований токен при старті: сесію не відновлюємо, але
     * повідомляємо екран входу, що потрібно увійти знову.
     */
    private val _currentUser = MutableStateFlow(tokenStorage.currentUser())

    override val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    private val _sessionExpired = MutableStateFlow(false)

    override val sessionExpired: StateFlow<Boolean> = _sessionExpired.asStateFlow()

    init {
        if (tokenStorage.token() != null && !tokenStorage.hasValidSession()) {
            // токен є, але прострочений — чистимо і фіксуємо причину
            tokenStorage.clear()
            _currentUser.value = null
            _sessionExpired.value = true
            errorSink.log("auth: протермінований токен при старті — потрібен повторний вхід")
        }
    }

    override suspend fun login(email: String, password: String): AuthUser {
        if (!networkChecker.isOnline()) throw UiException(UiError.NoConnection)
        val users = try {
            remote.fetchUsers()
        } catch (t: Throwable) {
            // Типізуємо справжню причину: 500 → ServerError, обрив мережі → NoConnection
            val error = ErrorHandler.toUiError(t)
            errorSink.log("auth: вхід не вдався: $error (${t.javaClass.simpleName})")
            throw UiException(error, t)
        }
        val account = users.find { it.email.equals(email.trim(), ignoreCase = true) }
            ?: throw UiException(UiError.InvalidCredentials)
        // Порівняння сталого часу — захист від timing-атак навіть у демо
        if (!MessageDigest.isEqual(
            account.password.toByteArray(Charsets.UTF_8),
            password.toByteArray(Charsets.UTF_8),
        )
        ) {
            throw UiException(UiError.InvalidCredentials)
        }

        val user = AuthUser(name = account.name, email = account.email, role = account.role)
        tokenStorage.save(MockTokenFactory.create(user))
        _currentUser.value = user
        _sessionExpired.value = false
        return user
    }

    override suspend fun logout(reason: LogoutReason) {
        tokenStorage.clear()
        _currentUser.value = null
        _sessionExpired.value = reason == LogoutReason.SESSION_EXPIRED
    }

    override fun hasValidSession(): Boolean = tokenStorage.hasValidSession()
}
