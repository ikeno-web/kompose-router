package io.github.ikenoweb.komposerouter

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages the navigation back stack and provides reactive access to the current route.
 *
 * [Navigator] is a plain class (not a Composable) so it can be used from ViewModels,
 * services, or any non-Composable code.
 *
 * All navigation state is derived from a single [_backStack] [MutableStateFlow].
 * Mutations use [MutableStateFlow.update] (an atomic compare-and-set loop),
 * making concurrent calls to [navigate], [pop], [popToRoute], and [replace] thread-safe.
 *
 * @param R The route type hierarchy.
 * @param startRoute The initial route to display.
 */
public class Navigator<R : Any>(startRoute: R) {

    private var nextId: Long = 1L

    private fun generateId(): String = (nextId++).toString()

    private val _backStack = MutableStateFlow(
        listOf(BackStackEntry(id = generateId(), route = startRoute))
    )

    /** The full back stack as a [StateFlow] of route entries. */
    public val backStack: StateFlow<List<BackStackEntry<R>>> = _backStack.asStateFlow()

    /** The currently displayed route, derived from the back stack. */
    public val currentRoute: R get() = _backStack.value.last().route

    /** Whether the back stack has more than one entry (i.e., [pop] would succeed). */
    public val canPop: Boolean get() = _backStack.value.size > 1

    /**
     * Push [route] onto the back stack and navigate to it.
     */
    public fun navigate(route: R) {
        _backStack.update { it + BackStackEntry(id = generateId(), route = route) }
    }

    /**
     * Pop the top entry from the back stack.
     *
     * @return `true` if an entry was popped, `false` if the back stack has only one entry
     *         (the start route is never popped).
     */
    public fun pop(): Boolean {
        var popped = false
        _backStack.update { stack ->
            if (stack.size > 1) {
                popped = true
                stack.dropLast(1)
            } else {
                stack
            }
        }
        return popped
    }

    /**
     * Pop entries until [predicate] matches an entry's route in the back stack.
     *
     * Uses predicate matching so data class instances can be matched by type:
     * ```kotlin
     * navigator.popToRoute { it is AppRoute.Home }
     * ```
     *
     * @param predicate Function to test each route in the back stack.
     * @param inclusive If `true`, the matching entry is also removed.
     * @return `true` if a matching route was found and the stack was unwound.
     */
    public fun popToRoute(predicate: (R) -> Boolean, inclusive: Boolean = false): Boolean {
        var found = false
        _backStack.update { stack ->
            val index = stack.indexOfLast { predicate(it.route) }
            if (index < 0) {
                stack
            } else {
                found = true
                val endIndex = if (inclusive) index else index + 1
                // Never pop below 1 entry
                val result = stack.subList(0, endIndex.coerceAtLeast(1))
                result.toList()
            }
        }
        return found
    }

    /**
     * Replace the current (top) back stack entry with [route].
     */
    public fun replace(route: R) {
        _backStack.update { stack ->
            if (stack.isEmpty()) {
                listOf(BackStackEntry(id = generateId(), route = route))
            } else {
                stack.dropLast(1) + BackStackEntry(id = generateId(), route = route)
            }
        }
    }
}
