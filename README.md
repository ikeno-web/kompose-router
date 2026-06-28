# kompose-router

Type-safe, multiplatform navigation for Compose Multiplatform — no string paths, no reflection.

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![](https://jitpack.io/v/ikeno-web/kompose-router.svg)](https://jitpack.io/#ikeno-web/kompose-router)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.7.3-4285F4.svg)](https://www.jetbrains.com/compose-multiplatform/)
[![KMP](https://img.shields.io/badge/KMP-JVM%20%7C%20iOS%20%7C%20JS-brightgreen.svg)](https://kotlinlang.org/docs/multiplatform.html)

---

## Why kompose-router?

Existing navigation libraries for Compose Multiplatform carry trade-offs that become friction in real projects:

- **Navigation Compose** is Android-only and relies on string-based route URIs, which break at runtime rather than compile time.
- **Voyager** requires wrapping every screen in a `Screen` interface and carries its own lifecycle model that can conflict with Compose's own state management.
- **Decompose** is powerful but brings significant boilerplate (Components, ValueClasses, custom lifecycle) that is disproportionate for small-to-medium apps.

kompose-router takes a different approach: your routes are plain sealed classes, your screens are plain composables, and the Navigator is a lightweight StateFlow-backed state holder. There is no generated code, no annotation processing, and no runtime string parsing for navigation calls.

---

## Features

- **Type-safe routes** via sealed classes or interfaces — compile-time safety, no stringly-typed paths
- **StateFlow-backed back stack** — atomic state with no manual synchronization required
- **DSL route graph** — register screens with `screen<T>` in a declarative builder
- **Built-in transitions** — None, Fade, SlideHorizontal, SlideVertical, or Custom via `AnimatedContent`
- **Deep link resolver** — URI pattern matching with `{param}` placeholders, query parameters, and wildcards
- **CompositionLocal access** — retrieve the current `Navigator` anywhere in the composition tree
- **Minimal dependencies** — only `compose.runtime`, `compose.foundation`, and `compose.animation`

---

## Installation

kompose-router is distributed via [JitPack](https://jitpack.io).

**Step 1.** Add the JitPack repository to your root `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        maven("https://jitpack.io")
    }
}
```

**Step 2.** Add the dependency in your shared module's `build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.github.ikeno-web:kompose-router:v0.1.0")
        }
    }
}
```

---

## Quick Start

### 1. Define Routes

Declare a sealed interface (or sealed class) that implements `Route`. Each entry can carry typed parameters.

```kotlin
sealed interface AppRoute {
    data object Home : AppRoute
    data class Detail(val id: String) : AppRoute
    data class Settings(val tab: Int = 0) : AppRoute
}
```

### 2. Create the Router

Use `rememberNavigator` to create a `Navigator`, then pass it to `Router` along with a route graph:

```kotlin
@Composable
fun App() {
    val navigator = rememberNavigator(startRoute = AppRoute.Home)

    Router(navigator = navigator) {
        screen<AppRoute.Home> {
            HomeScreen()
        }
        screen<AppRoute.Detail>(transition = RouteTransition.SlideHorizontal) { route ->
            DetailScreen(id = route.id)
        }
        screen<AppRoute.Settings> { route ->
            SettingsScreen(tab = route.tab)
        }
    }
}
```

### 3. Navigate

Access the `Navigator` via `LocalNavigator.current` inside any composable within the `Router`:

```kotlin
@Composable
fun HomeScreen() {
    val navigator = LocalNavigator.current

    Column {
        Button(onClick = { navigator.navigate(AppRoute.Detail("abc")) }) {
            Text("Open Detail")
        }
        Button(onClick = { navigator.navigate(AppRoute.Settings(tab = 1)) }) {
            Text("Open Settings")
        }
    }
}
```

---

## Routes

Routes are plain Kotlin data classes or objects. No marker interface, annotation, or generated code required.

```kotlin
sealed interface AppRoute {
    // No parameters
    data object Home : AppRoute

    // Typed parameters — checked at compile time
    data class Detail(val id: String) : AppRoute

    // Default parameter values work as expected
    data class Settings(val tab: Int = 0) : AppRoute
}
```

---

## Navigation Operations

`Navigator` exposes a StateFlow-backed back stack and four navigation primitives:

```kotlin
val navigator = LocalNavigator.current

// Push a new route onto the back stack
navigator.navigate(AppRoute.Detail("xyz"))

// Pop the top entry
navigator.pop()

// Replace the current top entry (no back stack growth)
navigator.replace(AppRoute.Settings(tab = 2))

// Pop entries until the predicate matches
navigator.popToRoute { it is AppRoute.Home }
```

The current back stack is available as a `StateFlow<List<Route>>` for observation:

```kotlin
val stack by navigator.backStack.collectAsState()
```

---

## Transitions

Each `screen<T>` registration accepts an optional `transition` parameter. The built-in options are:

| Value | Behavior |
|---|---|
| `RouteTransition.None` | Instant switch, no animation |
| `RouteTransition.Fade` | Cross-fade between screens |
| `RouteTransition.SlideHorizontal` | Slide in from the right, out to the left |
| `RouteTransition.SlideVertical` | Slide in from the bottom, out upward |
| `RouteTransition.Custom(...)` | Provide your own `AnimatedContentTransitionScope` spec |

```kotlin
screen<AppRoute.Detail>(transition = RouteTransition.Fade) { route ->
    DetailScreen(id = route.id)
}
```

---

## Deep Links

`DeepLinkResolver` matches URI strings against registered patterns and produces typed route instances.

Patterns support `{param}` placeholders and query parameters. Wildcards (`*`) are also accepted.

```kotlin
val resolver = DeepLinkResolver<AppRoute>()

resolver.register("app://detail/{id}") { params ->
    AppRoute.Detail(id = params["id"]!!)
}

resolver.register("app://settings") { params ->
    AppRoute.Settings(tab = params["tab"]?.toIntOrNull() ?: 0)
}

// Resolves to AppRoute.Detail(id = "abc"), with "ref" available via params
val route = resolver.resolve("app://detail/abc?ref=push")

// Use in your platform entry point
route?.let { navigator.navigate(it) }
```

---

## Comparison

| Feature | kompose-router | Voyager | Decompose | Navigation Compose |
|---|---|---|---|---|
| Type-safe routes | Yes | Partial (Screen interface) | Yes | No (string URIs) |
| Multiplatform | Yes | Yes | Yes | Android only |
| Sealed class routes | Yes | No | Yes | No |
| Plain composable screens | Yes | No (Screen wrapper) | No (Component) | Yes |
| Annotation processing | No | No | No | No |
| Back stack as StateFlow | Yes | Internal | Internal | No |
| Built-in transitions | Yes | Yes | Yes | Yes |
| Deep link resolver | Yes | Via Voyager-Navigator | Via deep-link plugin | Yes (manifest-based) |
| Lifecycle integration | Compose-only | Custom lifecycle | Custom lifecycle | Jetpack ViewModel |
| Dependency footprint | Minimal | Moderate | Large | Android Jetpack |

---

## Supported Targets

| Target | Status |
|---|---|
| JVM (Android) | Supported |
| JVM (Desktop) | Supported |
| iOS (iosArm64, iosX64) | Supported |
| iOS Simulator (iosSimulatorArm64) | Supported |
| JS (IR) | Supported |

---

## Development

Clone the repository and open the project in Android Studio or IntelliJ IDEA with the Kotlin Multiplatform plugin installed.

```bash
git clone https://github.com/ikeno-web/kompose-router.git
cd kompose-router
```

Run tests for all targets:

```bash
./gradlew allTests
```

Build all targets:

```bash
./gradlew assemble
```

Publishing a new version is handled via JitPack on tag push — no manual publication step is required.

---

## License

Copyright 2026 ikeno-web

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for the full text.
