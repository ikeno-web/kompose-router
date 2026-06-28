package io.github.ikenoweb.komposerouter

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * The root navigation composable that hosts a route graph.
 *
 * Displays the screen matching the current route from the [navigator] back stack,
 * with animated transitions between screens.
 *
 * @param R The route type hierarchy.
 * @param navigator The [Navigator] instance managing the back stack.
 * @param modifier [Modifier] applied to the container.
 * @param defaultTransition Default [RouteTransition] for screens that don't specify their own.
 * @param builder DSL block to register screens via [RouteGraphBuilder.screen].
 */
@Composable
public fun <R : Any> Router(
    navigator: Navigator<R>,
    modifier: Modifier = Modifier,
    defaultTransition: RouteTransition = RouteTransition.Fade,
    builder: RouteGraphBuilder<R>.() -> Unit,
) {
    val graphBuilder = remember(builder) { RouteGraphBuilder<R>().apply(builder) }
    val backStackSnapshot by navigator.backStack.collectAsState()
    val currentRoute = backStackSnapshot.last().route

    // Track previous back stack size to determine forward/backward
    var previousSize by remember { mutableStateOf(backStackSnapshot.size) }
    val isForward = backStackSnapshot.size >= previousSize
    previousSize = backStackSnapshot.size

    @Suppress("UNCHECKED_CAST")
    CompositionLocalProvider(LocalNavigator provides (navigator as Navigator<Any>)) {
        AnimatedContent(
            targetState = currentRoute,
            modifier = modifier,
            transitionSpec = {
                val registration = graphBuilder.findScreen(targetState)
                val resolvedTransition = registration?.transition ?: defaultTransition
                val pair = if (isForward) {
                    resolvedTransition.forwardPair()
                } else {
                    resolvedTransition.backwardPair()
                }
                pair.enter togetherWith pair.exit
            },
            contentKey = { it::class },
        ) { route ->
            val registration = graphBuilder.findScreen(route)
            if (registration != null) {
                registration.content(route)
            } else {
                error("No screen registered for route: $route (${route::class})")
            }
        }
    }
}
