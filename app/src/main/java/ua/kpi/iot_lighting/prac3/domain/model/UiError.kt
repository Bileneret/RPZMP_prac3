package ua.kpi.iot_lighting.prac3.domain.model

/**
 * Типізовані помилки мережевих запитів (ПЗ12).
 *
 * Репозиторій перетворює «сирі» винятки (IOException, HttpException, Gson)
 * на ці значення, а ViewModel — у зрозумілі користувачу повідомлення.
 * Один централізований тип ⇒ жодних перевірок HTTP-кодів у UI.
 */
sealed interface UiError {
    /** Відсутнє зʼєднання або таймаут мережі. */
    data object NoConnection : UiError

    /** Токен відсутній або протермінований — потрібен повторний вхід. */
    data object SessionExpired : UiError

    /** HTTP 403 — роль не дозволяє операцію. */
    data object Forbidden : UiError

    /** HTTP 404 — ресурс не знайдено. */
    data object NotFound : UiError

    /** HTTP 5xx — помилка на стороні сервера. */
    data object ServerError : UiError

    /** Відповідь отримано, але JSON некоректний або не збігається зі схемою. */
    data object BadResponse : UiError

    /** Некоректний логін/пароль (помилка автентифікації). */
    data object InvalidCredentials : UiError

    /** Інший HTTP-код, для якого немає окремої стратегії. */
    data class Http(val code: Int) : UiError
}

/**
 * Виняток, що несе типізовану помилку назовні з репозиторію.
 *
 * Уніфікований канал передачі помилок: Retrofit/Gson/Room кидають різні
 * типи, а ViewModel ловить лише [UiException] і мапить `error` у стан UI.
 */
class UiException(val error: UiError, cause: Throwable? = null) :
    RuntimeException(error.toString(), cause)
