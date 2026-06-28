package io.github.ikenoweb.komposerouter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember

/**
 * [CompositionLocal] providing the current [Navigator] instance.
 *
 * Provided automatically by [Router]. Access it in any descendant composable:
 * ```kotlin
 * val navigator = LocalNavigator.current
 * navigator.navigate(AppRoute.Detail("abc"))
 * ```
 *
 * The type is `Navigator<Any>` to erase the generic parameter.
 * In practice, the navigator is always scoped to the route type of the enclosing Router.
 *
 * Throws [IllegalStateException] if accessed outside a [Router].
 */
public val LocalNavigator: ProvidableCompositionLocal<Navigator<Any>> = compositionLocalOf {
    error("No Navigator provided. Wrap your content in a Router composable.")
}

/**
 * Create and remember a [Navigator] with the given [startRoute].
 *
 * This is the recommended way to create a Navigator for use with [Router]:
 * ```kotlin
 * val navigator = rememberNavigator(startRoute = AppRoute.Home)
 * Router(navigator = navigator) { ... }
 * ```
 *
 * @param R The route type hierarchy.
 * @param startRoute The initial route for the navigator.
 * @return A remembered [Navigator] instance.
 */
@Composable
public fun <R : Any> rememberNavigator(startRoute: R): Navigator<R> {
    return remember(startRoute) { Navigator(startRoute) }
}
