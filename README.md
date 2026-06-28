# kompose-router

Type-safe navigation for Compose Multiplatform.

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

## Features

- **Type-safe routes** via sealed classes/interfaces -- no string-based paths
- **DSL-based route graph** definition
- **Back stack management** with push, pop, popTo, replace
- **Animated transitions** (fade, slide, custom) via Compose animation APIs
- **Deep link support** with URI pattern matching
- **Multiplatform** -- JVM (Android/Desktop), iOS, JS
- **Zero dependencies** beyond Compose Runtime/Foundation/Animation

## Quick Start

### Define Routes

```kotlin
sealed interface AppRoute : Route {
    data object Home : AppRoute
    data class Detail(val id: String) : AppRoute
    data class Settings(val tab: Int = 0) : AppRoute
}
```

### Set Up Router

```kotlin
@Composable
fun App() {
    Router(startRoute = AppRoute.Home) {
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

### Navigate

```kotlin
@Composable
fun HomeScreen() {
    val navigator = LocalNavigator.current
    Button(onClick = { navigator.navigate(AppRoute.Detail("abc")) }) {
        Text("Go to Detail")
    }
}
```

## Targets

| Platform | Status |
|----------|--------|
| JVM (Android/Desktop) | Supported |
| iOS | Supported |
| JS (Browser) | Supported |

## License

Apache License 2.0. See [LICENSE](LICENSE).
