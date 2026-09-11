# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Simple Launcher** is a JavaFX desktop application for organizing and launching applications with advanced search capabilities. It features a database-backed link catalog with Boolean search operators, keyboard shortcuts, drag-and-drop support, and embedded terminal execution.

**Tech Stack**: Kotlin 2.2.20, JavaFX 26, Exposed ORM with KSP code generation, H2 database, JediTermFx terminal emulator. Java toolchain 25.

The UI is plain JavaFX (FXML + code), with the `basicafx-kt` library supplying the `FxApp`/`FxView` base classes, dialogs and helpers. TornadoFX was removed — do not reintroduce its DSL. Neither is `kotlinx-coroutines` used directly: background work goes through `runAsync`/`runAwait` from `basicafx-kt`, or a plain thread plus `Platform.runLater`.

## Build Commands

```bash
# Build the project
./gradlew build

# Run the application
./gradlew run

# Run tests
./gradlew test

# Run a specific test class
./gradlew test --tests "io.github.nayasis.simplelauncher.service.KeywordParserTest"

# Create fat JAR
./gradlew shadowJar

# Bundle a JVM with the app (jpackage)
./gradlew createNativeExe
# Output: build/dist/simplelauncher/

# Compile ahead of time to a real native binary (GraalVM native-image)
native-build.bat
# Output: build/native/nativeCompile/simplelauncher.exe
```

## Architecture

### Layered Structure

```
view/         # JavaFX UI
├── Main.kt            # Primary window (FxView backed by main.fxml)
├── ShortcutEditor.kt  # Shortcut settings dialog (built in code)
├── Terminal.kt        # PTY terminal wrapper using JediTermFx
├── Splash.kt          # Preloader splash
└── TokenField.kt, SearchTokenField.kt, TokenInputPane.kt  # token input controls

service/      # Business logic
├── LinkService.kt       # CRUD operations for links, file pickers
├── LinkExecutor.kt      # Command execution engine
├── LinkCommand.kt       # Command template parameter binding
├── TextMatcher.kt       # Search with Boolean operators
├── KeywordParser.kt     # Converts search to postfix notation (RPN)
├── SearchToken.kt       # Token-based search terms
├── ShortcutSettings.kt  # Shortcut actions and user bindings
├── Config.kt            # Persisted app state
└── LauncherChildWindows.kt, ProgressQueuePopOver.kt

model/        # Domain entities
├── Link.kt        # Main entity with @Entity annotation for code gen
├── Hashtags.kt    # Token set helpers
└── vo/JsonLink.kt # Import/export DTO

common/       # Utilities
├── Context.kt        # Service locator pattern
├── ExposedHelper.kt  # Database connection and transactions
└── Commons.kt        # Extension functions
```

### Code Generation with KSP

The project uses Exposed-CRUD with KSP to generate database code:

- **Source**: `Link.kt` with `@Entity` annotation
- **Generated**: `build/generated/ksp/main/kotlin/io/github/nayasis/simplelauncher/model/LinkTable.kt`
- **Access**: Repository via `LinkTable.repo` property (lazy-initialized)

When modifying `Link.kt`, rebuild to regenerate:
```bash
./gradlew clean kspKotlin build
```

## Key Architectural Patterns

### 1. Service Locator
All services accessed via `Context` companion object:
```kotlin
Context.linkService
Context.linkExecutor
Context.main
```

### 2. Boolean Search Engine
Search syntax supports Boolean operators with precedence:
- **AND**: space (highest precedence after NOT)
- **OR**: comma
- **NOT**: dash prefix
- **Grouping**: parentheses

Example: `foo bar, -baz (qux quux)` → "foo AND bar OR NOT baz OR (qux AND quux)"

Implementation:
- `KeywordParser.kt`: Converts infix to postfix notation
- `TextMatcher.kt`: Evaluates postfix expression against keyword sets
- Uses LRU cache (20 entries) for parsed queries

### 3. Relative Path Support
Links store both absolute and relative paths with fallback resolution:
1. Try absolute path
2. Try relative to application root
3. Use stored relative path

This enables portable link catalogs across machines.

### 4. Command Parameter Binding
`LinkCommand.kt` templates support variables:
- `${path}`: Full file path
- `${dir}`: Directory
- `${file}`: Filename
- `${name}`: Name without extension
- `${ext}`: Extension
- `${home}`: User home directory
- Unix variants: `${path-unix}`, `${dir-unix}`, `${home-unix}`

Used for drag-and-drop file handling and batch operations.

## Database Layer

### H2 Database
- **Location**: `~/.nayasis/simplelauncher/data.mv.db` (or in-memory for tests)
- **Initialization**: `ExposedHelper.connectDatabase()`, called from `Simplelauncher.onStart()`
- **Schema**: auto-created inside `connectDatabase()` via `SchemaUtils.create(LinkTable)`

### Transactions
Use `ExposedHelper.tx {}` for all database operations. Pass `readOnly = true` for queries:
```kotlin
ExposedHelper.tx {
    LinkTable.repo.create(link)
}

ExposedHelper.tx(readOnly = true) {
    LinkTable.repo.select().count()
}
```

### Testing with Database
`connectDatabase` creates the schema, so no separate `SchemaUtils` call is needed:
```kotlin
ExposedHelper.connectDatabase(
    url      = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
    user     = "user",
    password = "1234",
)
tx {
    // ...
}
```

## UI Components

### Main Window (Main.kt)
`Main` extends `FxView` and loads `/view/main/main.fxml`. Nodes are bound with the `by fxid()` delegate (the property name is the `fx:id`), and `onBeforeShow()`/`onUndock()` are the lifecycle hooks.

- **Search field**: live filtering with 300ms debounce
- **Link table**: `FilterSortList` (from `basicafx-kt`) bound via `bindTo(tableView)`; `predicate` filters, `items` is the full list
- **Editor panel**: toggle with Alt+E
- **Keyboard shortcuts**: see README.md for complete list

Key methods:
- `setSearchFilter()`: evaluates links on a background thread, then applies the predicate on the FX thread. A generation counter drops superseded searches, so callers that need the result use its `onApplied` callback.
- `submitSearch()`: re-reads the inputs and runs `setSearchFilter()`
- `saveDetail()`: persists the edited link via `LinkService`
- `drawDetail(link)`: fills the editor panel from a link
- Execution goes through `Context.linkExecutor.run(link)`

### Terminal (Terminal.kt)
Wraps commands in PTY-based terminal using JediTermFx:
- Executes command and captures output
- Handles ANSI color codes
- Configurable theme (`BlackTerminalTheme`)

## Testing

### Test Structure
- **Unit tests**: `KeywordParserTest`, `TextMatcherTest`, `SearchTokenTest`, `ShortcutSettingsTest`, `SearchSubmitTrackerTest`, `MainShortcutRoutingTest`
- **Integration tests**: `LinksTest` (database), `LinkExecutorTest` (disabled)
- **Framework**: JUnit 5 + Kotest assertions

Not everything under `src/test` is an automated test. Several files are manual demo apps — they have a `fun main()`, extend `Application`, and assert nothing (`ExecutorTest`, `TerminalBuilderTest`, `ChildWindowLifecycleTest`, `ProgressDialogQueueButtonTest`, `ShortcutEditorManualTest`, `TerminalFx*`). Gradle does not run them; launch them by hand. Don't assume a `*Test` name means there is coverage.

### Running Tests
```bash
# All tests
./gradlew test

# Specific test class
./gradlew test --tests "KeywordParserTest"

# With debug output
./gradlew test --info
```

### Test Database
Tests use H2 in-memory database with automatic cleanup:
```kotlin
ExposedHelper.init("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
```

## Module System Exports

JavaFX requires special JVM arguments for reflection access:
```kotlin
--enable-native-access=ALL-UNNAMED
--enable-native-access=javafx.graphics
--add-exports=javafx.base/com.sun.javafx.event=ALL-UNNAMED
--add-exports=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED
--add-exports=javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED
--add-opens=javafx.graphics/javafx.scene=ALL-UNNAMED
```

These live in `build.gradle.kts:appJvmArgs`, are wired into `application.applicationDefaultJvmArgs`, and are passed to `createNativeExe` as `--java-options`.

The packaged app puts the JavaFX jars on the classpath rather than the module path, so it logs `WARNING: Unknown module: javafx.graphics specified to --add-exports` on startup. This is expected noise, not a failure.

### jlink module list

`requiredJdkModules` in `build.gradle.kts` drives `createRuntimeImage`. Two entries are easy to get wrong:

- `java.scripting` — required by `javafx.fxml`: `FXMLLoader` references `javax.script.Bindings` for `fx:script` support. Removing it still passes `./gradlew run` (the full JDK is present) but makes the packaged app die with `NoClassDefFoundError: javax/script/Bindings` on its first FXML load.
- `jdk.unsupported` — required by JavaFX.

Always verify module changes with `./gradlew createNativeExe` and by running the produced executable, not just `./gradlew run`.

## Distribution

### Fat JAR
```bash
./gradlew shadowJar
# Output: build/libs/simplelauncher-<version>-all.jar
```

The version comes from the branch name: `release/x.y.z` yields `x.y.z`, anything else yields `0.1.0-SNAPSHOT`.

### GraalVM native-image (true AOT binary)

```bash
native-build.bat
# Output: build/native/nativeCompile/simplelauncher.exe  (~93MB, ~2m30s)
```

Do not call `./gradlew nativeCompile` directly — native-image drives `cl.exe`, and
without the MSVC environment it fails with `fatal error C1034: stdio.h`.
`native-build.bat` runs `vcvars64.bat` first, then Gradle.

Requirements:
- **Liberica NIK 25 "full"** at `%NATIVE_JAVA_HOME%` (default:
  `%USERPROFILE%\.sdkman\candidates\java\25.0.4.1-nikfull`). Download the *full*
  bundle from <https://bell-sw.com/pages/downloads/native-image-kit/>; sdkman's
  Windows NIK candidate is the plain bundle and will not work.
- Visual Studio 2022 Build Tools, C++ workload plus Windows SDK.
- A local copy of the shared reachability metadata (the plugin cannot fetch it
  from GitHub releases here); see the comment in `build.gradle.kts`.

Things that cost time to rediscover:

| Trap | Why |
|---|---|
| Requires the **full** NIK bundle | Only it ships JavaFX as JDK modules *and* the static libs native-image links (`glass.lib`, `prism_d3d.lib`, `javafx_font.lib`, `javafx_iio.lib`). With the plain bundle the link step fails with `LNK2001` on `JNI_OnLoad_glass` and every `Java_com_sun_glass_ui_win_*` symbol. |
| JavaFX comes from the JDK, not Maven | The `org.openjfx` jars are filtered out of the image classpath. The static libs match the JDK's JavaFX, so a second JavaFX from Maven would mismatch. Note this means the native binary runs JavaFX 25.0.4.1 while `gradlew run` uses 26.0.2. |
| No classpath to scan at runtime | Anything built on `Classes.findResources` glob patterns silently finds nothing. That is why message bundles are listed explicitly in `Simplelauncher.kt` and the `@Inject` services are registered in `beforeInitialization()` instead of relying on `FxApp`'s package scan. Adding a new `@Inject` service means registering it there too. |
| Concrete `Application`/`Preloader` subclasses need a no-arg constructor | native-image's `JavaFXFeature` registers every reachable subtype for reflective instantiation. This is why `DefaultSplash` in basicafx-kt is `abstract`. |
| `.bat` files must stay CRLF | An LF-only batch file makes `cmd.exe` produce no output at all. Enforced by `.gitattributes`. |

### Native Executable (jpackage — bundles a JVM)
Two-stage process:
1. **jlink**: Creates custom runtime with only required modules
   ```bash
   ./gradlew createRuntimeImage
   # Output: build/runtime-image/
   ```

2. **jpackage**: Creates platform-specific installer
   ```bash
   ./gradlew createNativeExe
   # Output: build/dist/simplelauncher/
   ```

Platform-specific notes:
- **Windows**: Attempts to create .exe installer if WiX Toolset is installed (light.exe), otherwise creates app-image
- **Linux/Mac**: Creates app-image

## Dependencies of Note

- **basica-kt**: Custom utility library (path, string, reflection helpers)
- **basicafx-kt**: Custom JavaFX library. Supplies `FxApp`/`FxView` with the `fxml`/`fxid` delegates, `Dialog`, progress dialogs, `FilterSortList`, `runAsync`/`runAwait`/`runLater`, `StageProperty`, `Localizator`. Developed in parallel at `C:\project_nayasis\basicafx-kt` and consumed from mavenLocal as `0.1.0-SNAPSHOT`, so run `./gradlew publishToMavenLocal` there after changing it.
- **Exposed 1.0.0-rc-4**: Database ORM
- **Exposed-CRUD**: KSP code generator for repositories
- **JediTermFx**: Terminal emulator component
- **mslinks**: Windows .lnk file parser

`kotlinx-coroutines` appears on the classpath transitively through Exposed and is needed at runtime, but is not used by this project's own code.

## Common Development Patterns

### Adding a New Link Field
1. Add property to `Link.kt` entity
2. Run `./gradlew clean kspKotlin` to regenerate `LinkTable.kt`
3. Update UI bindings in `Main.kt`
4. Update JSON export/import in `JsonLink.kt`
5. Migration: Database recreates on first run (H2 in user directory)

### Adding a New Service
1. Create the service in the `service/` package and annotate it `@Inject` — `FxApp.init()` scans the application package and registers it in `SimpleDiContainer`
2. Add a lazy property to `Context.kt` that resolves it with `bean(YourService::class)`

Views are not DI beans and must not be annotated: `FxApp.find()` instantiates them through their no-arg constructor and caches them.

### Modifying Search Logic
- **Parser**: `KeywordParser.kt:toPostfix()` (infix to RPN)
- **Matcher**: `TextMatcher.kt:match()` (evaluates postfix)
- **Indexing**: `Link.kt:keywords` property (space-delimited)

### Adding Keyboard Shortcuts
Shortcuts are data, not hardcoded key handlers. They are user-rebindable through the shortcut settings dialog.

1. Add an entry to the `ShortcutAction` enum in `ShortcutSettings.kt` with its message key, `ShortcutGroup` and default `KeyCombination`
2. Add the message key to `src/main/resources/message/`
3. Handle it in `handleMainShortcutEvent()` in `Main.kt` and add the matching lambda to `MainShortcutHandlers`
4. For table context-menu entries, add the action to `TABLE_ITEM_SHORTCUT_ACTIONS` in `Main.kt`

Menu items live in `main.fxml`; wire their `setOnAction` in `Main.kt:initEvent()`.

## Icon Handling

Icons are stored as PNG bytes in database:
- **Extraction**: from .exe, .ico, .lnk files via `Link.kt:setIcon(file)`
- **Conversion**: all formats converted to PNG on import
- **Export**: Base64-encoded in JSON
- **Display**: lazy-loaded through the `iconImage` property

When adding icon support for new file types, modify `Link.kt:setIcon()`.

## Recent Changes

- Removed TornadoFX and direct coroutine use; the UI runs on plain JavaFX with `FxView`/`FxApp` from `basicafx-kt`. Design notes: `docs/superpowers/specs/2026-09-08-remove-tornadofx-design.md`
- Fixed native packaging: `java.scripting` must stay in the jlink module list (FXMLLoader needs it), and `toJpackageAppVersion()` now strips version qualifiers so `createNativeExe` works off `release/*` branches
- Table rendering uses a fixed cell size and reuses cell graphics; search filtering runs off the FX thread
- Migrated from Komapper to Exposed ORM (commit: 4543400)
- Added jpackage support for native executables (commit: be3b3b3)

## Contact

- Issues: https://github.com/nayasis/SimpleLauncher/issues
- Email: nayasis@gmail.com
