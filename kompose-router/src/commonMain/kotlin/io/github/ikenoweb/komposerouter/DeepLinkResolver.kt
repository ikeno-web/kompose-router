package io.github.ikenoweb.komposerouter

/**
 * Resolves URI strings to route instances using registered patterns.
 *
 * Patterns use `{paramName}` placeholders that match any non-`/` segment.
 * Query parameters are automatically extracted and merged into the params map.
 *
 * ```kotlin
 * val resolver = DeepLinkResolver<AppRoute>()
 * resolver.register("myapp://detail/{id}") { params ->
 *     AppRoute.Detail(id = params["id"]!!)
 * }
 * val route = resolver.resolve("myapp://detail/abc") // AppRoute.Detail("abc")
 * val withQuery = resolver.resolve("myapp://detail/abc?ref=push")
 * // params = {id: "abc", ref: "push"}
 * ```
 *
 * Patterns are matched in registration order; the first match wins.
 *
 * @param R The route type hierarchy.
 */
public class DeepLinkResolver<R : Any> {

    private val registrations = mutableListOf<Registration<R>>()

    /**
     * Register a URI pattern with a factory that builds a route from extracted parameters.
     *
     * @param pattern URI pattern with `{paramName}` placeholders (e.g., `"myapp://detail/{id}"`).
     * @param factory Function that receives a map of parameter names to matched values
     *                and returns the corresponding route.
     */
    public fun register(pattern: String, factory: (params: Map<String, String>) -> R) {
        registrations.add(Registration(pattern, factory))
    }

    /**
     * Attempt to resolve a [uri] to a route.
     *
     * @param uri The URI string to match against registered patterns.
     * @return The first matching route, or `null` if no pattern matches.
     */
    public fun resolve(uri: String): R? {
        val (path, queryParams) = splitPathAndQuery(uri)
        for (registration in registrations) {
            val pathParams = matchPattern(registration.pattern, path)
            if (pathParams != null) {
                val allParams = pathParams + queryParams
                return registration.factory(allParams)
            }
        }
        return null
    }

    private data class Registration<R : Any>(
        val pattern: String,
        val factory: (Map<String, String>) -> R,
    )
}

/**
 * Split a URI into path and query parameter parts.
 *
 * @return Pair of (path without query, map of query parameters).
 */
internal fun splitPathAndQuery(uri: String): Pair<String, Map<String, String>> {
    val queryIndex = uri.indexOf('?')
    if (queryIndex < 0) return uri to emptyMap()

    val path = uri.substring(0, queryIndex)
    val queryString = uri.substring(queryIndex + 1)
    val params = mutableMapOf<String, String>()
    for (pair in queryString.split('&')) {
        val eqIndex = pair.indexOf('=')
        if (eqIndex > 0) {
            params[pair.substring(0, eqIndex)] = pair.substring(eqIndex + 1)
        }
    }
    return path to params
}

/**
 * Match a URI path against a pattern, extracting `{paramName}` segments.
 *
 * @return A map of parameter names to values, or `null` if the pattern doesn't match.
 */
internal fun matchPattern(pattern: String, uri: String): Map<String, String>? {
    val patternParts = splitUri(pattern)
    val uriParts = splitUri(uri)

    if (patternParts.size != uriParts.size) return null

    val params = mutableMapOf<String, String>()
    for (i in patternParts.indices) {
        val pp = patternParts[i]
        val up = uriParts[i]
        when {
            pp.startsWith("{") && pp.endsWith("}") -> {
                val paramName = pp.substring(1, pp.length - 1)
                params[paramName] = up
            }
            pp == "*" -> { /* wildcard, matches anything */ }
            pp != up -> return null
        }
    }
    return params
}

/**
 * Split a URI into segments by `/` and `://`, treating scheme as a segment.
 *
 * Examples:
 * - `"app://detail/abc"` -> `["app:", "detail", "abc"]`
 * - `"https://example.com/path"` -> `["https:", "example.com", "path"]`
 */
internal fun splitUri(uri: String): List<String> {
    val normalized = uri.replace("://", ":/")
    return normalized.split("/").filter { it.isNotEmpty() }
}
