# Code Review: kompose-router v1.0

**Reviewer:** Reviewer Agent (Workflow v2.0)
**Date:** 2026-06-28
**Scope:** All source files in `commonMain` + `commonTest`, design doc `screen_flow.md`, constraints `constraints.md`

---

## Checklist

| # | Item | Status |
|---|------|--------|
| 1 | Route type safety (no marker interface, generics `<R : Any>`) | PASS |
| 2 | Navigator operations (navigate, pop, popToRoute, replace) | PASS with issues |
| 3 | BackStack integrity (unique IDs, ordering, no empty stack) | PASS with issues |
| 4 | Thread safety (StateFlow mutations) | FAIL |
| 5 | DeepLinkResolver pattern matching | PASS with issues |
| 6 | RouteGraphBuilder type safety (reified generics, KClass lookup) | PASS with issues |
| 7 | RouteTransition sealed hierarchy + forward/backward resolution | PASS |
| 8 | Router composable (AnimatedContent, CompositionLocal) | PASS with issues |
| 9 | Memory leak risk (StateFlow collectors, CompositionLocal) | PASS |
| 10 | API conformance with screen_flow.md | PASS with issues |
| 11 | Constraint conformance (constraints.md) | PASS |
| 12 | Test coverage | PASS with issues |

---

## Issues

### CRITICAL

#### CR-01: Navigator is NOT thread-safe -- race conditions on `nextId` and `syncCurrentRoute`

**Files:** `Navigator.kt:19-22, 112-116`

`nextId` is a plain `Long` incremented in `generateId()` without synchronization. When `navigate()` / `replace()` are called from multiple coroutines or threads concurrently, two entries can receive the same ID.

More critically, `syncCurrentRoute()` reads `_backStack.value` and writes to `_currentRoute.value` and `_canPop.value` as three separate non-atomic operations. Between `_backStack.update {}` returning and `syncCurrentRoute()` executing, another thread can mutate the back stack, causing `_currentRoute` to reflect the wrong entry.

`MutableStateFlow.update {}` is atomic for its own state, but the multi-flow synchronization pattern used here is not.

**Fix:** Either:
(a) Use a single `MutableStateFlow<NavigatorState<R>>` data class holding backStack + currentRoute + canPop, updated atomically in one `.update {}` call; or
(b) Protect all mutations with a `Mutex` or `synchronized` block.

Option (a) is strongly recommended as it is idiomatic for StateFlow.

---

### HIGH

#### H-01: `popToRoute` with `inclusive = true` on start route silently keeps the entry

**File:** `Navigator.kt:90`

When `popToRoute({ it is StartRoute }, inclusive = true)` matches the start route at index 0, `endIndex = 0`, then `coerceAtLeast(1)` makes it keep the entry. This is the correct safety behavior (never empty the stack), but the method returns `true` (found = true) even though nothing was actually removed. The caller receives a misleading success signal.

**Fix:** When the result list equals the original stack, set `found = false` or return a more descriptive result.

#### H-02: `splitUri` produces empty-string segments for double slashes and trailing slashes

**File:** `DeepLinkResolver.kt:118-120`

`filter { it.isNotEmpty() }` handles empty segments from double slashes, but this means `app://detail//abc` matches pattern `app://detail/{id}` with `id = "abc"`, silently swallowing the double slash. Depending on intent this may mask malformed URIs.

More importantly, there are no tests for: empty path (`""`), trailing slash (`"app://home/"`), double slash in path (`"app://detail//abc"`), or special characters in segments (`%20`, `+`, `#`).

**Fix:** Add explicit edge-case tests. Consider whether empty segments should cause a match failure rather than being silently dropped.

#### H-03: No URL decoding in DeepLinkResolver

**File:** `DeepLinkResolver.kt`

Path parameters and query values are not URL-decoded. A URI like `app://detail/hello%20world` will match with the literal value `"hello%20world"` rather than `"hello world"`. For a library claiming deep link support, this will surprise consumers.

**Fix:** Apply percent-decoding to extracted path params and query param values. At minimum, document the limitation.

#### H-04: Query parameter with empty key is silently accepted

**File:** `DeepLinkResolver.kt:75`

`eqIndex > 0` correctly rejects `=value` (key-less), but `key=` (empty value) passes with value `""`. Also, `&` alone or `&&` produces entries like `""` which are silently skipped by `eqIndex > 0`. This is acceptable but undocumented and untested.

---

### MEDIUM

#### M-01: `RouteGraphBuilder.findScreen` uses `isInstance` which matches superclass registrations

**File:** `RouteGraphBuilder.kt:65`

`isInstance` means if a consumer registers `screen<TestRoute>` (the sealed interface itself), it will match ALL routes. Combined with `find` (first-match), registration order becomes load-bearing but is not documented or enforced. The design doc says "Map<KClass<*>, ScreenEntry>" (exact match), but the implementation uses `isInstance` (assignability check).

**Fix:** Consider using `route::class == it.routeClass` for exact matching, consistent with the design doc. If `isInstance` is intentional for polymorphism, document it.

#### M-02: `RouteGraphBuilder` allows duplicate registrations for the same KClass

**File:** `RouteGraphBuilder.kt:35`

Multiple `screen<T>` calls with the same `T` silently add duplicate entries. Only the first is ever matched (via `find`). This is a foot-gun for consumers.

**Fix:** Either throw on duplicate registration or replace the existing entry.

#### M-03: Router direction detection is fragile

**File:** `Router.kt:38-40`

Forward/backward is determined by comparing back stack size. This fails for `replace()` which keeps size constant -- it will always be detected as "forward" (`>=`). Similarly, `popToRoute` popping multiple entries is correctly detected as backward, but `popToRoute` that finds the target at the current position (no entries removed) is detected as forward.

**Fix:** Track the operation type (push/pop/replace) in the Navigator state, or compare entry IDs rather than stack size.

#### M-04: `Router` casts `Navigator<R>` to `Navigator<Any>` unsafely

**File:** `Router.kt:43`

`@Suppress("UNCHECKED_CAST")` hides the generic erasure. While this works at runtime due to type erasure, it means any composable accessing `LocalNavigator.current` gets `Navigator<Any>` and can `navigate()` with any type, bypassing compile-time type safety.

The design doc acknowledges this (section 9: "cast is consumer responsibility"), but this is a known foot-gun. Consider a typed accessor helper:
```kotlin
@Composable
inline fun <reified R : Any> localNavigator(): Navigator<R> =
    LocalNavigator.current as Navigator<R>
```

#### M-05: `rememberNavigator` re-creates Navigator if `startRoute` reference changes

**File:** `LocalNavigator.kt:40-42`

`remember(startRoute)` uses `startRoute` as the key. If the consumer passes a data class instance reconstructed each recomposition (e.g., `rememberNavigator(AppRoute.Settings(viewModel.tab))`), the Navigator is recreated and the entire back stack is lost.

**Fix:** Document that `startRoute` should be a stable reference, or use `rememberSaveable` / drop the key.

---

### LOW

#### L-01: `BackStackEntry.id` uses monotonic Long counter -- not unique across Navigator instances

**File:** `Navigator.kt:19-21`

Two Navigator instances will both start IDs at "1". If entries from different navigators are ever compared or stored together (e.g., analytics), collisions occur.

**Fix:** Prefix with a per-instance random token, or use `kotlin.uuid.Uuid` (experimental in Kotlin 2.0 but stable by now in 2.0.21+). Low priority since cross-navigator comparison is unlikely in v1.0.

#### L-02: No test for `replace` on a truly empty back stack

**File:** `Navigator.kt:102-104`

The `if (stack.isEmpty())` branch in `replace` is dead code -- the constructor guarantees at least one entry, and no public API can empty the stack. The branch is harmless defensive code but is untested.

#### L-03: `RouteTransitionTest` only checks `assertNotEquals(None)` for non-None transitions

**File:** `RouteTransitionTest.kt`

Tests for Fade/SlideHorizontal/SlideVertical only assert the result is not `None`. They don't verify directionality (e.g., that forward slide goes right-to-left and backward goes left-to-right). While animation specifics are hard to unit test, at minimum the enter/exit objects could be compared between forward and backward to assert they differ.

---

### INFO

#### I-01: Design doc says `Map<KClass<*>, ScreenEntry>` but implementation uses `List` + `find`

**File:** `screen_flow.md` section 6 vs `RouteGraphBuilder.kt:35`

The design doc states "Map<KClass<*>, ScreenEntry>" but the implementation uses `mutableListOf`. This gives O(n) lookup instead of O(1). For typical route counts (<20) this is negligible, but it is a design-implementation drift.

#### I-02: Design doc specifies `LocalNavigator: ProvidableCompositionLocal<Navigator<*>>` but implementation uses `Navigator<Any>`

**File:** `screen_flow.md` section 9 vs `LocalNavigator.kt:22`

`Navigator<*>` (star projection) and `Navigator<Any>` have different type semantics. `Navigator<*>` would prevent calling `navigate()` without a cast, which is arguably safer. The implementation chose `Navigator<Any>` which allows unchecked navigation.

#### I-03: No `NavigatorTest` for concurrent access

Tests are all single-threaded. Given CR-01, concurrent tests would expose the race condition.

---

## Test Coverage Assessment

| Source File | Test File | Coverage Notes |
|-------------|-----------|---------------|
| `Navigator.kt` | `NavigatorTest.kt` (19 tests) | Good functional coverage. Missing: concurrency, empty-stack edge in replace |
| `BackStackEntry.kt` | `BackStackEntryTest.kt` (5 tests) | Adequate for a data class |
| `RouteTransition.kt` | `RouteTransitionTest.kt` (11 tests) | Good. Missing: directionality assertions |
| `RouteGraphBuilder.kt` | `RouteGraphBuilderTest.kt` (7 tests) | Good. Missing: duplicate registration, superclass registration |
| `DeepLinkResolver.kt` | `DeepLinkResolverTest.kt` (17 tests) | Good base coverage. Missing: edge cases (empty URI, double slashes, special chars, URL encoding, fragment `#`) |
| `Router.kt` | None (Compose UI test) | No unit test. Acceptable for v1.0 as it requires Compose test harness |
| `LocalNavigator.kt` | None (Compose UI test) | No unit test. Acceptable for v1.0 |
| `Route.kt` | N/A (comment-only file) | N/A |

Estimated line coverage: ~85-90% for testable (non-Compose) code. Meets C-09 threshold for logic code; Compose UI tests would be needed for full 90%+.

---

## Verdict

### CONDITIONAL PASS

The library is well-structured, the API surface aligns with the design doc, and the core navigation logic is functionally correct for single-threaded use. Code is clean, well-documented, and idiomatic Kotlin.

However, **CR-01 (thread safety)** is a blocking issue. The Navigator advertises StateFlow-based reactivity (implying multi-threaded collection), but mutations are not atomic across the three StateFlows. This must be fixed before v1.0 release.

**Required for PASS:**
1. Fix CR-01: Consolidate Navigator state into a single atomic StateFlow or add synchronization.

**Strongly recommended before v1.0:**
2. Fix H-01: `popToRoute` inclusive return value accuracy.
3. Fix M-03: Direction detection for `replace()`.
4. Fix M-02: Prevent duplicate screen registrations.
5. Add DeepLinkResolver edge-case tests (H-02).

**Can ship as known limitations (document in README):**
- H-03: No URL decoding.
- M-04: `LocalNavigator` type erasure.
- M-05: `rememberNavigator` key sensitivity.
