# API Surface Design — kompose-router

## 1. モジュール構成

```
kompose-router/          ← 単一モジュール
  commonMain/            ← 全ターゲット共通
  commonTest/            ← 共通テスト
```

## 2. ルート定義（消費者側）

```kotlin
// 消費者が自分のアプリで定義する
sealed interface AppRoute {
    data object Home : AppRoute
    data class Detail(val id: String) : AppRoute
    data class Settings(val tab: Int = 0) : AppRoute
}
```

ルートは **任意の sealed interface/class** でよい。ライブラリは `Route` 基底型を強制しない（ジェネリクス `<R>` で受ける）。

## 3. Navigator

```kotlin
class Navigator<R : Any>(startRoute: R) {

    // ── ナビゲーション操作 ──
    fun navigate(route: R)
    fun pop(): Boolean
    fun popToRoute(predicate: (R) -> Boolean, inclusive: Boolean = false): Boolean
    fun replace(route: R)

    // ── リアクティブ状態 ──
    val currentRoute: StateFlow<R>
    val backStack: StateFlow<List<BackStackEntry<R>>>
    val canPop: StateFlow<Boolean>
}
```

### 設計判断
- **`popToRoute` は predicate ベース**: Route が data class の場合、引数違いの同クラスを一致させたいケースがある（例: 任意の `Detail` まで戻る）。`is` チェックが自然: `popToRoute { it is AppRoute.Home }`
- **ジェネリクス `<R : Any>`**: 消費者のルート型を強制しない。`Any` 下限で KMP common に留まる
- **StateFlow**: Compose の `collectAsState()` と直接接続。ViewModel からも購読可能

## 4. BackStackEntry

```kotlin
data class BackStackEntry<R : Any>(
    val id: String,       // 一意ID（UUID相当。KMP互換のため自前生成）
    val route: R,
)
```

### 設計判断
- **savedState を v1.0 では持たない**: KMP common で `Any?` のシリアライゼーションは不可能。US-06（状態保存）は SHOULD であり、v1.x で kotlinx.serialization ベースのソリューションを検討する
- **id は String**: `kotlin.uuid.Uuid` は Kotlin 2.0 で experimental。安全のため String

## 5. Router Composable

```kotlin
@Composable
fun <R : Any> Router(
    navigator: Navigator<R>,
    modifier: Modifier = Modifier,
    defaultTransition: RouteTransition = RouteTransition.Fade,
    builder: RouteGraphBuilder<R>.() -> Unit,
)
```

### 設計判断
- **Navigator を外から受ける**: Router 内部で生成すると ViewModel から到達できない。`rememberNavigator()` でラップは提供するが、Router は注入を前提とする
- **`defaultTransition`**: 個別 screen 指定がなければこれを使う

## 6. RouteGraphBuilder DSL

```kotlin
class RouteGraphBuilder<R : Any> {
    inline fun <reified T : R> screen(
        transition: RouteTransition? = null,
        noinline content: @Composable (route: T) -> Unit,
    )
}
```

### 使用例

```kotlin
val navigator = rememberNavigator(startRoute = AppRoute.Home)

Router(navigator = navigator) {
    screen<AppRoute.Home> { HomeScreen() }
    screen<AppRoute.Detail>(transition = RouteTransition.SlideHorizontal) { route ->
        DetailScreen(id = route.id)
    }
    screen<AppRoute.Settings> { route ->
        SettingsScreen(tab = route.tab)
    }
}
```

### 内部動作
1. `screen<T>` は `KClass<T>` を reified で取得し、`Map<KClass<*>, ScreenEntry>` に登録
2. Router は `navigator.currentRoute.collectAsState()` で再コンポジションをトリガー
3. 現在のルートの `KClass` でマップを引き、`content()` を呼ぶ
4. 未登録のルートは `IllegalStateException` をスロー（コンパイル時は sealed exhaustive、実行時はフォールバック）

## 7. RouteTransition

```kotlin
sealed interface RouteTransition {
    data object None : RouteTransition
    data object Fade : RouteTransition
    data object SlideHorizontal : RouteTransition
    data object SlideVertical : RouteTransition
    data class Custom(
        val enterForward: EnterTransition,
        val exitForward: ExitTransition,
        val enterBackward: EnterTransition,
        val exitBackward: ExitTransition,
    ) : RouteTransition
}
```

### 設計判断
- **forward/backward を分離**: push 時と pop 時で逆方向のアニメーションを適用するため
- **Custom は Compose Animation API に依存**: これは意図的。Router 自体が `@Composable` であり Compose 依存は不可避
- **テスト**: Navigator のロジックテストは Compose 不要（純 Kotlin）。アニメーション適用は UI テストの領域

## 8. DeepLinkResolver

```kotlin
class DeepLinkResolver<R : Any> {
    fun register(pattern: String, factory: (params: Map<String, String>) -> R)
    fun resolve(uri: String): R?
}
```

### パターン構文
- `{name}` — パスパラメータ（`app://detail/{id}` → `{id: "abc"}`）
- `*` — ワイルドカード（1セグメント）
- クエリパラメータは自動抽出（`?key=value` → params に追加）

### 使用例

```kotlin
val resolver = DeepLinkResolver<AppRoute>()
resolver.register("myapp://home") { AppRoute.Home }
resolver.register("myapp://detail/{id}") { params ->
    AppRoute.Detail(id = params["id"]!!)
}

val route = resolver.resolve("myapp://detail/abc")
// → AppRoute.Detail(id = "abc")

val withQuery = resolver.resolve("myapp://detail/abc?ref=push")
// → params = {id: "abc", ref: "push"}
```

## 9. Composition Local

```kotlin
val LocalNavigator: ProvidableCompositionLocal<Navigator<*>>

@Composable
fun <R : Any> rememberNavigator(startRoute: R): Navigator<R>
```

### 設計判断
- `LocalNavigator` は `Navigator<*>` 型（型パラメータを消去）。キャストは消費者責任だが、通常は `rememberNavigator` + Router 経由で型安全に使う
- Router 内部で `CompositionLocalProvider(LocalNavigator provides navigator)` を設定

## 10. ナビゲーションフロー図

### Forward Navigation
```
navigate(Detail("x"))
  → backStack に BackStackEntry(route=Detail("x")) を追加
  → _currentRoute.emit(Detail("x"))
  → canPop.emit(backStack.size > 1)
  → Router が recompose
    → AnimatedContent(targetState, forward transition)
      → screen<Detail> の content(Detail("x")) を描画
```

### Pop Back
```
pop()
  → backStack.removeLast()
  → _currentRoute.emit(backStack.last().route)
  → canPop.emit(backStack.size > 1)
  → Router が recompose
    → AnimatedContent(targetState, backward transition)
```

### Deep Link
```
resolver.resolve("myapp://detail/abc")
  → パターンマッチ → factory({id: "abc"}) → Detail("abc")
  → navigator.navigate(Detail("abc"))
  → (Forward Navigation フロー)
```

## 11. トレーサビリティ

| ユーザーストーリー | API 要素 |
|-------------------|---------|
| US-01 (sealed routes) | ジェネリクス `<R : Any>`, `screen<reified T : R>` |
| US-02 (型安全引数) | `navigate(route: R)`, data class プロパティ |
| US-03 (バックスタック) | `navigate()`, `pop()`, `popToRoute()`, `replace()` |
| US-04 (アニメーション) | `RouteTransition`, `AnimatedContent`, forward/backward |
| US-05 (ディープリンク) | `DeepLinkResolver.register/resolve` |
| US-08 (VM ナビゲーション) | `Navigator` は plain class, DI 可能 |
| US-10 (リアクティブ) | `currentRoute: StateFlow<R>`, `canPop: StateFlow<Boolean>` |
| US-06 (状態保存) | v1.x で `BackStackEntry.savedState` + serialization 追加予定 |
| US-07 (ネストグラフ) | v1.x で `nested {}` DSL 追加予定 |
| US-09 (インターセプト) | v1.x で `NavigationInterceptor` 追加予定 |

## 12. セルフチェック

- [x] 全 MUST ストーリーが API 要素にマッピングされている
- [x] D-006（KSPなし）との整合性: アノテーション不使用、reified generics のみ
- [x] D-003（ゼロ依存）との整合性: Compose Runtime/Animation/Foundation のみ
- [x] popToRoute が predicate ベースで data class equality 問題を回避
- [x] BackStackEntry に v1.0 では savedState を持たない判断を明記
- [x] Navigator がジェネリクスで消費者のルート型を強制しない
- [x] Custom transition が forward/backward を分離
