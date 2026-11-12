package ua.kpi.iot_lighting.prac3

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ua.kpi.iot_lighting.prac3.data.auth.MockTokenFactory
import ua.kpi.iot_lighting.prac3.data.auth.TokenStore
import ua.kpi.iot_lighting.prac3.domain.model.AuthUser
import ua.kpi.iot_lighting.prac3.domain.model.Role

/**
 * Тести локального сховища токена (ПЗ11): збереження між запусками,
 * визначення протермінованої сесії, очищення при виході.
 */
@RunWith(AndroidJUnit4::class)
class TokenStoreTest {

    private lateinit var store: TokenStore
    private val user = AuthUser(name = "Олексій", email = "admin@light.io", role = Role.ADMIN)

    @Before
    fun setUp() {
        store = TokenStore(ApplicationProvider.getApplicationContext())
        store.clear()
    }

    @Test
    fun збереженийТокенВідновлюєКористувача() {
        store.save(MockTokenFactory.create(user))
        assertTrue(store.hasValidSession())
        assertEquals("admin@light.io", store.currentUser()?.email)
        assertEquals(Role.ADMIN, store.currentUser()?.role)
    }

    @Test
    fun протермінованийТокенНеДаєСесії() {
        val now = System.currentTimeMillis() / 1000
        // токен створено 9 годин тому при терміні дії 8 годин
        store.save(MockTokenFactory.create(user, nowEpochSec = now - 9 * 3600))
        assertFalse(store.hasValidSession())
        assertNull(store.currentUser())
    }

    @Test
    fun очищенняВидаляєСесію() {
        store.save(MockTokenFactory.create(user))
        store.clear()
        assertNull(store.token())
        assertFalse(store.hasValidSession())
        assertNull(store.currentUser())
    }

    @Test
    fun пошкодженийТокенНеДаєСесії() {
        store.save("not-a-jwt")
        assertFalse(store.hasValidSession())
        assertNull(store.currentUser())
    }
}
