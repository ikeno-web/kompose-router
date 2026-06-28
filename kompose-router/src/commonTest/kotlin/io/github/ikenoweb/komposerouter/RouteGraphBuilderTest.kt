package io.github.ikenoweb.komposerouter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RouteGraphBuilderTest {

    @Test
    fun screen_registersRouteClass() {
        val builder = RouteGraphBuilder<TestRoute>()
        builder.screen<TestRoute.Home> { }

        assertEquals(1, builder.screens.size)
        assertEquals(TestRoute.Home::class, builder.screens[0].routeClass)
    }

    @Test
    fun screen_registersMultipleRoutes() {
        val builder = RouteGraphBuilder<TestRoute>()
        builder.screen<TestRoute.Home> { }
        builder.screen<TestRoute.Detail> { }
        builder.screen<TestRoute.Settings> { }

        assertEquals(3, builder.screens.size)
    }

    @Test
    fun screen_storesTransitionOverride() {
        val builder = RouteGraphBuilder<TestRoute>()
        builder.screen<TestRoute.Home>(transition = RouteTransition.SlideHorizontal) { }

        assertEquals(RouteTransition.SlideHorizontal, builder.screens[0].transition)
    }

    @Test
    fun screen_defaultTransitionIsNull() {
        val builder = RouteGraphBuilder<TestRoute>()
        builder.screen<TestRoute.Home> { }

        assertNull(builder.screens[0].transition)
    }

    @Test
    fun findScreen_findsRegisteredRoute() {
        val builder = RouteGraphBuilder<TestRoute>()
        builder.screen<TestRoute.Home> { }
        builder.screen<TestRoute.Detail> { }

        val found = builder.findScreen(TestRoute.Detail("x"))
        assertNotNull(found)
        assertEquals(TestRoute.Detail::class, found.routeClass)
    }

    @Test
    fun findScreen_returnsNullForUnregisteredRoute() {
        val builder = RouteGraphBuilder<TestRoute>()
        builder.screen<TestRoute.Home> { }

        val found = builder.findScreen(TestRoute.Profile)
        assertNull(found)
    }

    @Test
    fun screen_preservesRegistrationOrder() {
        val builder = RouteGraphBuilder<TestRoute>()
        builder.screen<TestRoute.Settings> { }
        builder.screen<TestRoute.Home> { }
        builder.screen<TestRoute.Detail> { }

        assertEquals(TestRoute.Settings::class, builder.screens[0].routeClass)
        assertEquals(TestRoute.Home::class, builder.screens[1].routeClass)
        assertEquals(TestRoute.Detail::class, builder.screens[2].routeClass)
    }

    @Test
    fun screen_throwsOnDuplicateRegistration() {
        val builder = RouteGraphBuilder<TestRoute>()
        builder.screen<TestRoute.Home> { }

        assertFailsWith<IllegalArgumentException> {
            builder.screen<TestRoute.Home> { }
        }
    }
}
