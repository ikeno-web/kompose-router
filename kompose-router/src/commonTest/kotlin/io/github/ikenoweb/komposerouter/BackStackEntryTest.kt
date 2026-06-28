package io.github.ikenoweb.komposerouter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BackStackEntryTest {

    @Test
    fun dataClassEquality_sameIdAndRoute() {
        val e1 = BackStackEntry<TestRoute>(id = "1", route = TestRoute.Detail("x"))
        val e2 = BackStackEntry<TestRoute>(id = "1", route = TestRoute.Detail("x"))

        assertEquals(e1, e2)
    }

    @Test
    fun differentIds_areNotEqual() {
        val e1 = BackStackEntry<TestRoute>(id = "1", route = TestRoute.Home)
        val e2 = BackStackEntry<TestRoute>(id = "2", route = TestRoute.Home)

        assertNotEquals(e1, e2)
    }

    @Test
    fun differentRoutes_areNotEqual() {
        val e1 = BackStackEntry<TestRoute>(id = "1", route = TestRoute.Home)
        val e2 = BackStackEntry<TestRoute>(id = "1", route = TestRoute.Profile)

        assertNotEquals(e1, e2)
    }

    @Test
    fun copy_preservesValues() {
        val original = BackStackEntry<TestRoute>(id = "1", route = TestRoute.Detail("a"))
        val copy = original.copy(route = TestRoute.Detail("b"))

        assertEquals("1", copy.id)
        assertEquals(TestRoute.Detail("b"), copy.route)
    }

    @Test
    fun toString_containsValues() {
        val entry = BackStackEntry<TestRoute>(id = "42", route = TestRoute.Home)
        val str = entry.toString()

        assertTrue(str.contains("42"))
        assertTrue(str.contains("Home"))
    }
}
