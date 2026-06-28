package io.github.ikenoweb.komposerouter

/**
 * Represents a single entry in the navigation back stack.
 *
 * Each entry has a unique [id] for identity and the [route] it represents.
 *
 * @param R The route type hierarchy.
 * @property id Unique identifier for this entry.
 * @property route The route this entry represents.
 */
public data class BackStackEntry<R : Any>(
    val id: String,
    val route: R,
)
