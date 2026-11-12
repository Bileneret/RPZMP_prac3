package ua.kpi.iot_lighting.prac3

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.kpi.iot_lighting.prac3.data.auth.MockTokenFactory
import ua.kpi.iot_lighting.prac3.domain.model.AuthUser
import ua.kpi.iot_lighting.prac3.domain.model.Role

/**
 * Тести токена (ПЗ11): генерація → розбір, термін дії, ролі.
 * Токен має формат JWT (header.payload.signature) — перевіряємо структуру.
 */
class MockTokenFactoryTest {

    private val user = AuthUser(name = "Адмін", email = "admin@light.io", role = Role.ADMIN)

    @Test
    fun `токен складається з трьох частин`() {
        val token = MockTokenFactory.create(user)
        assertEquals(3, token.split('.').size)
    }

    @Test
    fun `roundtrip створення та розбору зберігає поля`() {
        val now = 1_700_000_000L
        val payload = MockTokenFactory.decode(MockTokenFactory.create(user, now))
        assertEquals("admin@light.io", payload?.subject)
        assertEquals("Адмін", payload?.name)
        assertEquals(Role.ADMIN, payload?.role)
        assertEquals(now, payload?.issuedAtEpochSec)
        assertEquals(now + 8 * 3600, payload?.expiresAtEpochSec)
    }

    @Test
    fun `свіжий токен не протермінований`() {
        val now = 1_700_000_000L
        val payload = MockTokenFactory.decode(MockTokenFactory.create(user, now))
        assertFalse(payload!!.isExpired(now + 60))
    }

    @Test
    fun `токен вважається протермінованим після exp`() {
        val now = 1_700_000_000L
        val payload = MockTokenFactory.decode(MockTokenFactory.create(user, now))
        assertTrue(payload!!.isExpired(now + 8 * 3600 + 1))
    }

    @Test
    fun `роль user розпізнається з токена`() {
        val payload = MockTokenFactory.decode(
            MockTokenFactory.create(user.copy(role = Role.USER)),
        )
        assertEquals(Role.USER, payload?.role)
    }

    @Test
    fun `пошкоджений токен дає null`() {
        assertNull(MockTokenFactory.decode("not-a-jwt"))
        assertNull(MockTokenFactory.decode("a.b"))
        assertNull(MockTokenFactory.decode(null))
    }
}
