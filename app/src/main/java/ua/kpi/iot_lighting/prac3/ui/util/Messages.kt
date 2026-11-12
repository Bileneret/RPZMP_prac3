package ua.kpi.iot_lighting.prac3.ui.util

import ua.kpi.iot_lighting.prac3.domain.model.UiError

/**
 * Єдиний словник повідомлень для користувача (ПЗ12): різні причини помилки
 * → різні зрозумілі тексти. Ніде більше не будується текст помилки.
 */
fun UiError.userMessage(): String = when (this) {
    UiError.NoConnection -> "Немає з'єднання з інтернетом"
    UiError.SessionExpired -> "Сесія завершена — увійдіть знову"
    UiError.Forbidden -> "Доступ заборонено: операція лише для адміністратора"
    UiError.NotFound -> "Світильник не знайдено"
    UiError.ServerError -> "Сталася помилка сервера, спробуйте пізніше"
    UiError.BadResponse -> "Некоректна відповідь сервера"
    UiError.InvalidCredentials -> "Невірний логін або пароль"
    is UiError.Http -> "Помилка сервера: ${code}"
}
