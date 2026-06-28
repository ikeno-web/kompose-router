package io.github.ikenoweb.komposerouter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigatorTest {

    private fun createNavigator(start: TestRoute = TestRoute.Home): Navigator<TestRoute> =
        Navigator(start)

    // --- navigate ---

    @Test
    fun navigate_pushesRouteOntoBackStack() {
        val nav = createNavigator()
        nav.navigate(TestRoute.Detail("1"))

        assertEquals(TestRoute.Detail("1"), nav.currentRoute)
        assertEquals(2, nav.backStack.value.size)
    }

    @Test
    fun navigate_multiplePushes() {
        val nav = createNavigator()
        nav.navigate(TestRoute.Detail("1"))
        nav.navigate(TestRoute.Settings(2))

        assertEquals(TestRoute.Settings(2), nav.currentRoute)
        assertEquals(3, nav.backStack.value.size)
    }

    @Test
    fun navigate_updatesCanPop() {
        val nav = createNavigator()
        assertFalse(nav.canPop)

        nav.navigate(TestRoute.Detail("1"))
        assertTrue(nav.canPop)
    }

    // --- pop ---

    @Test
    fun pop_removesTopEntry() {
        val nav = createNavigator()
        nav.navigate(TestRoute.Detail("1"))

        val result = nav.pop()

        assertTrue(result)
        assertEquals(TestRoute.Home, nav.currentRoute)
        assertEquals(1, nav.backStack.value.size)
    }

    @Test
    fun pop_returnsFalseWhenOnlyOneEntry() {
        val nav = createNavigator()

        val result = nav.pop()

        assertFalse(result)
        assertEquals(TestRoute.Home, nav.currentRoute)
        assertEquals(1, nav.backStack.value.size)
    }

    @Test
    fun pop_updatesCanPop() {
        val nav = createNavigator()
        nav.navigate(TestRoute.Detail("1"))
        assertTrue(nav.canPop)

        nav.pop()
        assertFalse(nav.canPop)
    }

    @Test
    fun pop_multipleTimesStopsAtStart() {
        val nav = createNavigator()
        nav.navigate(TestRoute.Detail("1"))
        nav.navigate(TestRoute.Settings())

        assertTrue(nav.pop())
        assertTrue(nav.pop())
        assertFalse(nav.pop()) // Can't pop start route

        assertEquals(TestRoute.Home, nav.currentRoute)
    }

    // --- popToRoute ---

    @Test
    fun popToRoute_unwindsToMatchingRoute() {
        val nav = createNavigator()
        nav.navigate(TestRoute.Detail("1"))
        nav.navigate(TestRoute.Settings())
        nav.navigate(TestRoute.Profile)

        val result = nav.popToRoute(predicate = { it is TestRoute.Detail })

        assertTrue(result)
        assertEquals(TestRoute.Detail("1"), nav.currentRoute)
        assertEquals(2, nav.backStack.value.size)
    }

    @Test
    fun popToRoute_exactEquality() {
        val nav = createNavigator()
        nav.navigate(TestRoute.Detail("1"))
        nav.navigate(TestRoute.Settings())

        val result = nav.popToRoute(predicate = { it == TestRoute.Detail("1") })

        assertTrue(result)
        assertEquals(TestRoute.Detail("1"), nav.currentRoute)
        assertEquals(2, nav.backStack.value.size)
    }

    @Test
    fun popToRoute_inclusive_removesTarget() {
        val nav = createNavigator()
        nav.navigate(TestRoute.Detail("1"))
        nav.navigate(TestRoute.Settings())

        val result = nav.popToRoute({ it is TestRoute.Detail }, inclusive = true)

        assertTrue(result)
        assertEquals(TestRoute.Home, nav.currentRoute)
        assertEquals(1, nav.backStack.value.size)
    }

    @Test
    fun popToRoute_returnsFalseWhenNotFound() {
        val nav = createNavigator()
        nav.navigate(TestRoute.Detail("1"))

        val result = nav.popToRoute(predicate = { it is TestRoute.Settings })

        assertFalse(result)
        assertEquals(TestRoute.Detail("1"), nav.currentRoute)
        assertEquals(2, nav.backStack.value.size)
    }

    @Test
    fun popToRoute_inclusive_neverPopsLastEntry() {
        val nav = createNavigator()

        val result = nav.popToRoute(predicate = { it is TestRoute.Home }, inclusive = true)

        assertTrue(result)
        assertEquals(1, nav.backStack.value.size)
    }

    @Test
    fun popToRoute_findsLastOccurrence() {
        val nav = createNavigator()
        nav.navigate(TestRoute.Detail("1"))
        nav.navigate(TestRoute.Home) // Second Home
        nav.navigate(TestRoute.Detail("2"))

        val result = nav.popToRoute(predicate = { it is TestRoute.Home })

        assertTrue(result)
        assertEquals(TestRoute.Home, nav.currentRoute)
        // Should pop to the LAST Home (index 2), so 3 entries remain
        assertEquals(3, nav.backStack.value.size)
    }

    // --- replace ---

    @Test
    fun replace_swapsTopEntry() {
        val nav = createNavigator()
        nav.navigate(TestRoute.Detail("1"))

        nav.replace(TestRoute.Settings(5))

        assertEquals(TestRoute.Settings(5), nav.currentRoute)
        assertEquals(2, nav.backStack.value.size)
        assertEquals(TestRoute.Home, nav.backStack.value[0].route)
    }

    @Test
    fun replace_onSingleEntry() {
        val nav = createNavigator()

        nav.replace(TestRoute.Detail("x"))

        assertEquals(TestRoute.Detail("x"), nav.currentRoute)
        assertEquals(1, nav.backStack.value.size)
        assertFalse(nav.canPop)
    }

    // --- currentRoute StateFlow ---

    @Test
    fun currentRoute_reflectsLatestNavigation() {
        val nav = createNavigator()

        assertEquals(TestRoute.Home, nav.currentRoute)

        nav.navigate(TestRoute.Detail("a"))
        assertEquals(TestRoute.Detail("a"), nav.currentRoute)

        nav.replace(TestRoute.Detail("b"))
        assertEquals(TestRoute.Detail("b"), nav.currentRoute)

        nav.pop()
        assertEquals(TestRoute.Home, nav.currentRoute)
    }

    // --- backStack entries have unique IDs ---

    @Test
    fun backStackEntries_haveUniqueIds() {
        val nav = createNavigator()
        nav.navigate(TestRoute.Detail("1"))
        nav.navigate(TestRoute.Settings())

        val ids = nav.backStack.value.map { it.id }
        assertEquals(ids.distinct(), ids)
    }

    @Test
    fun backStackEntries_idsAreStrings() {
        val nav = createNavigator()
        val id = nav.backStack.value[0].id
        assertTrue(id.isNotEmpty())
    }
}
