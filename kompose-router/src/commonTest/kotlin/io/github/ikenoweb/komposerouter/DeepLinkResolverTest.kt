package io.github.ikenoweb.komposerouter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeepLinkResolverTest {

    @Test
    fun resolve_matchesSimplePattern() {
        val resolver = DeepLinkResolver<TestRoute>()
        resolver.register("app://home") { TestRoute.Home }

        val result = resolver.resolve("app://home")
        assertEquals(TestRoute.Home, result)
    }

    @Test
    fun resolve_extractsSingleParameter() {
        val resolver = DeepLinkResolver<TestRoute>()
        resolver.register("app://detail/{id}") { params ->
            TestRoute.Detail(id = params["id"]!!)
        }

        val result = resolver.resolve("app://detail/abc")
        assertEquals(TestRoute.Detail("abc"), result)
    }

    @Test
    fun resolve_extractsMultipleParameters() {
        val resolver = DeepLinkResolver<TestRoute>()
        resolver.register("app://settings/{tab}") { params ->
            TestRoute.Settings(tab = params["tab"]!!.toInt())
        }

        val result = resolver.resolve("app://settings/3")
        assertEquals(TestRoute.Settings(3), result)
    }

    @Test
    fun resolve_returnsNullForNoMatch() {
        val resolver = DeepLinkResolver<TestRoute>()
        resolver.register("app://home") { TestRoute.Home }

        val result = resolver.resolve("app://unknown")
        assertNull(result)
    }

    @Test
    fun resolve_returnsNullForDifferentSegmentCount() {
        val resolver = DeepLinkResolver<TestRoute>()
        resolver.register("app://detail/{id}") { params ->
            TestRoute.Detail(id = params["id"]!!)
        }

        assertNull(resolver.resolve("app://detail/abc/extra"))
        assertNull(resolver.resolve("app://detail"))
    }

    @Test
    fun resolve_firstMatchWins() {
        val resolver = DeepLinkResolver<TestRoute>()
        resolver.register("app://page/{id}") { TestRoute.Detail(it["id"]!!) }
        resolver.register("app://page/{id}") { TestRoute.Settings(it["id"]!!.toInt()) }

        val result = resolver.resolve("app://page/5")
        assertEquals(TestRoute.Detail("5"), result)
    }

    @Test
    fun resolve_returnsNullForEmptyRegistrations() {
        val resolver = DeepLinkResolver<TestRoute>()
        assertNull(resolver.resolve("app://anything"))
    }

    @Test
    fun resolve_handlesHttpsScheme() {
        val resolver = DeepLinkResolver<TestRoute>()
        resolver.register("https://example.com/detail/{id}") { params ->
            TestRoute.Detail(id = params["id"]!!)
        }

        val result = resolver.resolve("https://example.com/detail/xyz")
        assertEquals(TestRoute.Detail("xyz"), result)
    }

    @Test
    fun resolve_noMatchWhenLiteralsDiffer() {
        val resolver = DeepLinkResolver<TestRoute>()
        resolver.register("app://detail/{id}") { params ->
            TestRoute.Detail(id = params["id"]!!)
        }

        assertNull(resolver.resolve("app://other/abc"))
    }

    @Test
    fun resolve_extractsQueryParameters() {
        val resolver = DeepLinkResolver<TestRoute>()
        resolver.register("app://detail/{id}") { params ->
            TestRoute.Detail(id = params["id"]!! + "-" + (params["ref"] ?: "none"))
        }

        val result = resolver.resolve("app://detail/abc?ref=push")
        assertEquals(TestRoute.Detail("abc-push"), result)
    }

    @Test
    fun resolve_queryParamsWithoutPathParams() {
        val resolver = DeepLinkResolver<TestRoute>()
        resolver.register("app://home") { params ->
            if (params.containsKey("tab")) {
                TestRoute.Settings(params["tab"]!!.toInt())
            } else {
                TestRoute.Home
            }
        }

        val result = resolver.resolve("app://home?tab=2")
        assertEquals(TestRoute.Settings(2), result)
    }

    @Test
    fun resolve_multipleQueryParams() {
        val resolver = DeepLinkResolver<TestRoute>()
        var capturedParams: Map<String, String> = emptyMap()
        resolver.register("app://search") { params ->
            capturedParams = params
            TestRoute.Home
        }

        resolver.resolve("app://search?q=hello&page=2&sort=asc")
        assertEquals("hello", capturedParams["q"])
        assertEquals("2", capturedParams["page"])
        assertEquals("asc", capturedParams["sort"])
    }

    @Test
    fun resolve_wildcardMatchesAnySegment() {
        val resolver = DeepLinkResolver<TestRoute>()
        resolver.register("app://*/detail/{id}") { params ->
            TestRoute.Detail(id = params["id"]!!)
        }

        val result = resolver.resolve("app://v2/detail/abc")
        assertEquals(TestRoute.Detail("abc"), result)
    }

    // --- splitUri tests ---

    @Test
    fun splitUri_handlesAppScheme() {
        val parts = splitUri("app://detail/abc")
        assertEquals(listOf("app:", "detail", "abc"), parts)
    }

    @Test
    fun splitUri_handlesHttpsScheme() {
        val parts = splitUri("https://example.com/path")
        assertEquals(listOf("https:", "example.com", "path"), parts)
    }

    @Test
    fun splitUri_handlesNoPathAfterHost() {
        val parts = splitUri("app://home")
        assertEquals(listOf("app:", "home"), parts)
    }

    // --- matchPattern tests ---

    @Test
    fun matchPattern_returnsEmptyMapForExactMatch() {
        val result = matchPattern("app://home", "app://home")
        assertEquals(emptyMap(), result)
    }

    @Test
    fun matchPattern_returnsNullForMismatch() {
        val result = matchPattern("app://home", "app://other")
        assertNull(result)
    }

    @Test
    fun matchPattern_extractsParam() {
        val result = matchPattern("app://detail/{id}", "app://detail/123")
        assertEquals(mapOf("id" to "123"), result)
    }

    @Test
    fun matchPattern_returnsNullForDifferentLengths() {
        assertNull(matchPattern("app://a/b", "app://a"))
        assertNull(matchPattern("app://a", "app://a/b"))
    }

    // --- splitPathAndQuery tests ---

    @Test
    fun splitPathAndQuery_noQuery() {
        val (path, params) = splitPathAndQuery("app://home")
        assertEquals("app://home", path)
        assertEquals(emptyMap(), params)
    }

    @Test
    fun splitPathAndQuery_withQuery() {
        val (path, params) = splitPathAndQuery("app://home?key=value")
        assertEquals("app://home", path)
        assertEquals(mapOf("key" to "value"), params)
    }

    @Test
    fun splitPathAndQuery_multipleQueryParams() {
        val (path, params) = splitPathAndQuery("app://home?a=1&b=2")
        assertEquals("app://home", path)
        assertEquals(mapOf("a" to "1", "b" to "2"), params)
    }
}
