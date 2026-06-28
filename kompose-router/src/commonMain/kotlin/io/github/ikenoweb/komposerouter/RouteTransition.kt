package io.github.ikenoweb.komposerouter

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically

/**
 * Sealed hierarchy of built-in and custom route transitions.
 *
 * Forward transitions are used when pushing; backward transitions when popping.
 * Use one of the built-in transitions ([None], [Fade], [SlideHorizontal], [SlideVertical])
 * or provide a [Custom] transition with your own enter/exit pairs.
 */
public sealed interface RouteTransition {

    /** No animation; instant switch. */
    public data object None : RouteTransition

    /** Crossfade between screens. */
    public data object Fade : RouteTransition

    /** Slide horizontally (enter from right, exit to left on push; reverse on pop). */
    public data object SlideHorizontal : RouteTransition

    /** Slide vertically (enter from bottom, exit to top on push; reverse on pop). */
    public data object SlideVertical : RouteTransition

    /**
     * Custom transition with user-supplied enter/exit pairs for forward and backward.
     *
     * @property enterForward Enter animation when pushing forward.
     * @property exitForward Exit animation for the outgoing screen when pushing forward.
     * @property enterBackward Enter animation when popping backward.
     * @property exitBackward Exit animation for the outgoing screen when popping backward.
     */
    public data class Custom(
        val enterForward: EnterTransition,
        val exitForward: ExitTransition,
        val enterBackward: EnterTransition,
        val exitBackward: ExitTransition,
    ) : RouteTransition
}

/**
 * Resolved enter/exit pair for a single direction.
 */
public data class TransitionPair(
    val enter: EnterTransition,
    val exit: ExitTransition,
)

/**
 * Resolves a [RouteTransition] to a [TransitionPair] for the forward direction.
 */
public fun RouteTransition.forwardPair(): TransitionPair = when (this) {
    RouteTransition.None -> TransitionPair(
        enter = EnterTransition.None,
        exit = ExitTransition.None,
    )
    RouteTransition.Fade -> TransitionPair(
        enter = fadeIn(),
        exit = fadeOut(),
    )
    RouteTransition.SlideHorizontal -> TransitionPair(
        enter = slideInHorizontally { it },
        exit = slideOutHorizontally { -it },
    )
    RouteTransition.SlideVertical -> TransitionPair(
        enter = slideInVertically { it },
        exit = slideOutVertically { -it },
    )
    is RouteTransition.Custom -> TransitionPair(
        enter = enterForward,
        exit = exitForward,
    )
}

/**
 * Resolves a [RouteTransition] to a [TransitionPair] for the backward direction.
 */
public fun RouteTransition.backwardPair(): TransitionPair = when (this) {
    RouteTransition.None -> TransitionPair(
        enter = EnterTransition.None,
        exit = ExitTransition.None,
    )
    RouteTransition.Fade -> TransitionPair(
        enter = fadeIn(),
        exit = fadeOut(),
    )
    RouteTransition.SlideHorizontal -> TransitionPair(
        enter = slideInHorizontally { -it },
        exit = slideOutHorizontally { it },
    )
    RouteTransition.SlideVertical -> TransitionPair(
        enter = slideInVertically { -it },
        exit = slideOutVertically { it },
    )
    is RouteTransition.Custom -> TransitionPair(
        enter = enterBackward,
        exit = exitBackward,
    )
}
