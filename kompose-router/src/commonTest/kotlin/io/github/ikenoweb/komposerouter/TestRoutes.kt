package io.github.ikenoweb.komposerouter

/** Shared test route hierarchy used across all test files. */
sealed interface TestRoute {
    data object Home : TestRoute
    data class Detail(val id: String) : TestRoute
    data class Settings(val tab: Int = 0) : TestRoute
    data object Profile : TestRoute
}
