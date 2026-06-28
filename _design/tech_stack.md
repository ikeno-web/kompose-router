# Tech Stack — kompose-router

## Language & Framework
| Component | Version | Notes |
|-----------|---------|-------|
| Kotlin | 2.0.21 | K2 compiler |
| Compose Multiplatform | 1.7.3 | JetBrains |
| Compose Compiler | Bundled with Kotlin 2.0 | K2 compose plugin |

## Build
| Component | Version | Notes |
|-----------|---------|-------|
| Gradle | 8.12 | Kotlin DSL |
| AGP | Not used | Pure KMP library, no Android module |
| Version Catalog | libs.versions.toml | Single source of truth |

## KMP Targets
| Target | Platform |
|--------|----------|
| jvm | JVM (Android runtime, Desktop) |
| iosArm64 | iOS device |
| iosX64 | iOS simulator (Intel) |
| iosSimulatorArm64 | iOS simulator (Apple Silicon) |
| js(IR) | Browser |

## Testing
| Component | Version | Notes |
|-----------|---------|-------|
| kotlin-test | Bundled | Multiplatform assertions |
| JUnit 5 | JVM only | Test runner |

## CI/CD
| Component | Notes |
|-----------|-------|
| GitHub Actions | JVM tests + JS tests |
| Gradle wrapper | Committed to repo |

## Dependencies (runtime)
| Dependency | Scope | Rationale |
|------------|-------|-----------|
| compose.runtime | api | Core Compose runtime |
| compose.foundation | api | AnimatedContent, layout |
| compose.animation | api | Transition APIs |

Zero additional runtime dependencies.
