package io.github.ikenoweb.komposerouter

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class RouteTransitionTest {

    @Test
    fun none_forwardPair_returnsNoTransitions() {
        val pair = RouteTransition.None.forwardPair()
        assertEquals(EnterTransition.None, pair.enter)
        assertEquals(ExitTransition.None, pair.exit)
    }

    @Test
    fun none_backwardPair_returnsNoTransitions() {
        val pair = RouteTransition.None.backwardPair()
        assertEquals(EnterTransition.None, pair.enter)
        assertEquals(ExitTransition.None, pair.exit)
    }

    @Test
    fun fade_forwardPair_returnsNonNoneTransitions() {
        val pair = RouteTransition.Fade.forwardPair()
        assertNotEquals(EnterTransition.None, pair.enter)
        assertNotEquals(ExitTransition.None, pair.exit)
    }

    @Test
    fun fade_backwardPair_returnsNonNoneTransitions() {
        val pair = RouteTransition.Fade.backwardPair()
        assertNotEquals(EnterTransition.None, pair.enter)
        assertNotEquals(ExitTransition.None, pair.exit)
    }

    @Test
    fun slideHorizontal_forwardPair_returnsNonNoneTransitions() {
        val pair = RouteTransition.SlideHorizontal.forwardPair()
        assertNotEquals(EnterTransition.None, pair.enter)
        assertNotEquals(ExitTransition.None, pair.exit)
    }

    @Test
    fun slideHorizontal_backwardPair_returnsNonNoneTransitions() {
        val pair = RouteTransition.SlideHorizontal.backwardPair()
        assertNotEquals(EnterTransition.None, pair.enter)
        assertNotEquals(ExitTransition.None, pair.exit)
    }

    @Test
    fun slideVertical_forwardPair_returnsNonNoneTransitions() {
        val pair = RouteTransition.SlideVertical.forwardPair()
        assertNotEquals(EnterTransition.None, pair.enter)
        assertNotEquals(ExitTransition.None, pair.exit)
    }

    @Test
    fun slideVertical_backwardPair_returnsNonNoneTransitions() {
        val pair = RouteTransition.SlideVertical.backwardPair()
        assertNotEquals(EnterTransition.None, pair.enter)
        assertNotEquals(ExitTransition.None, pair.exit)
    }

    @Test
    fun custom_forwardPair_returnsProvidedTransitions() {
        val enterFwd = fadeIn()
        val exitFwd = fadeOut()
        val enterBwd = fadeIn()
        val exitBwd = fadeOut()
        val transition = RouteTransition.Custom(enterFwd, exitFwd, enterBwd, exitBwd)

        val pair = transition.forwardPair()
        assertEquals(enterFwd, pair.enter)
        assertEquals(exitFwd, pair.exit)
    }

    @Test
    fun custom_backwardPair_returnsProvidedTransitions() {
        val enterFwd = fadeIn()
        val exitFwd = fadeOut()
        val enterBwd = fadeIn()
        val exitBwd = fadeOut()
        val transition = RouteTransition.Custom(enterFwd, exitFwd, enterBwd, exitBwd)

        val pair = transition.backwardPair()
        assertEquals(enterBwd, pair.enter)
        assertEquals(exitBwd, pair.exit)
    }

    @Test
    fun sealedHierarchy_allVariantsResolve() {
        val transitions: List<RouteTransition> = listOf(
            RouteTransition.None,
            RouteTransition.Fade,
            RouteTransition.SlideHorizontal,
            RouteTransition.SlideVertical,
            RouteTransition.Custom(
                EnterTransition.None, ExitTransition.None,
                EnterTransition.None, ExitTransition.None,
            ),
        )

        transitions.forEach {
            it.forwardPair()
            it.backwardPair()
        }
        assertEquals(5, transitions.size)
    }

    @Test
    fun transitionPair_dataClassEquality() {
        val pair1 = TransitionPair(EnterTransition.None, ExitTransition.None)
        val pair2 = TransitionPair(EnterTransition.None, ExitTransition.None)
        assertEquals(pair1, pair2)
    }
}
