package ua.kpi.iot_lighting.prac3

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.kpi.iot_lighting.prac3.domain.validation.CredentialsValidator

/**
 * Валідація форми входу (базова вимога ПЗ11: поля не порожні).
 * Помилки рахуються окремо для кожного поля.
 */
class CredentialsValidatorTest {

    @Test
    fun `коректні дані проходять`() {
        val errors = CredentialsValidator.validate("admin@light.io", "admin123")
        assertFalse(errors.hasErrors)
        assertNull(errors.email)
        assertNull(errors.password)
    }

    @Test
    fun `порожній логін відхиляється`() {
        val errors = CredentialsValidator.validate(" ", "1234")
        assertEquals("Введіть логін", errors.email)
        assertNull(errors.password)
    }

    @Test
    fun `логін без собакі відхиляється`() {
        val errors = CredentialsValidator.validate("admin", "1234")
        assertEquals("Логін має вигляд email (наприклад, admin@light.io)", errors.email)
    }

    @Test
    fun `порожній пароль відхиляється`() {
        val errors = CredentialsValidator.validate("a@b.io", "")
        assertEquals("Введіть пароль", errors.password)
    }

    @Test
    fun `короткий пароль відхиляється`() {
        val errors = CredentialsValidator.validate("a@b.io", "12")
        assertEquals("Пароль надто короткий (мінімум 4 символи)", errors.password)
    }

    @Test
    fun `обидва поля можуть мати помилки одночасно`() {
        val errors = CredentialsValidator.validate("", "")
        assertTrue(errors.hasErrors)
        assertEquals("Введіть логін", errors.email)
        assertEquals("Введіть пароль", errors.password)
    }
}
