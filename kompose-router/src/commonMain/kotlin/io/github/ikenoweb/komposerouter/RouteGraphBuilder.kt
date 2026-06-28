package io.github.ikenoweb.komposerouter

import androidx.compose.runtime.Composable
import kotlin.reflect.KClass

/**
 * Internal registration for a screen in the route graph.
 *
 * @property routeClass The [KClass] of the route type this screen handles.
 * @property transition Optional per-screen transition override.
 * @property content The composable content factory, accepting the route instance.
 */
@PublishedApi
internal data class ScreenRegistration<R : Any>(
    val routeClass: KClass<out R>,
    val transition: RouteTransition?,
    val content: @Composable (R) -> Unit,
)

/**
 * DSL builder for defining the route graph inside a [Router].
 *
 * ```kotlin
 * Router(navigator = navigator) {
 *     screen<AppRoute.Home> { HomeScreen() }
 *     screen<AppRoute.Detail> { route -> DetailScreen(route.id) }
 * }
 * ```
 *
 * @param R The route type hierarchy.
 */
public class RouteGraphBuilder<R : Any> {

    @PublishedApi
    internal val screens = mutableListOf<ScreenRegistration<R>>()

    /**
     * Register a screen for route type [T].
     *
     * @param T The concrete route type this screen handles.
     * @param transition Optional transition override for this screen.
     *                   If `null`, the [Router]-level default is used.
     * @param content Composable content that receives the route instance.
     */
    public inline fun <reified T : R> screen(
        transition: RouteTransition? = null,
        noinline content: @Composable (route: T) -> Unit,
    ) {
        require(screens.none { it.routeClass == T::class }) {
            "Screen for ${T::class} is already registered"
        }
        @Suppress("UNCHECKED_CAST")
        screens.add(
            ScreenRegistration(
                routeClass = T::class,
                transition = transition,
                content = { route -> content(route as T) },
            )
        )
    }

    /**
     * Find the [ScreenRegistration] matching a given route instance.
     *
     * @return The registration, or `null` if no screen is registered for the route's type.
     */
    internal fun findScreen(route: R): ScreenRegistration<R>? {
        return screens.find { it.routeClass.isInstance(route) }
    }
}
