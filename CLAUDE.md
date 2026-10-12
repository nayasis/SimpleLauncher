# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Simple Launcher** is a Swing desktop application for organizing and launching applications with advanced search capabilities. It features a database-backed link catalog with Boolean search operators, keyboard shortcuts, drag-and-drop support, and embedded terminal execution.

**Tech Stack**: Kotlin 2.2.20, Swing with FlatLaf (IntelliJ theme), kotlinx-coroutines (`Dispatchers.Swing`), Exposed ORM with KSP code generation, H2 database, JediTerm (Swing) terminal emulator with pty4j. Java toolchain 25.

The UI is plain Swing built in code (no form files), with the `basica-swing` library supplying the `SwingApp`/`SwingView` base classes, dialogs, controls and coroutine helpers. JavaFX, TornadoFX and `basicafx-kt` were removed — do not reintroduce them.

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

# Manual demos (assert nothing)
./gradlew runTerminalDemo
./gradlew runShortcutEditorTest

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
view/         # Swing UI
├── Main.kt              # Primary window (SwingView, built in code)
├── ShortcutEditor.kt    # Shortcut settings dialog
├── Terminal.kt          # JFrame wrapping a JediTerm widget over a pty4j process
├── SearchTokenField.kt  # search input with token chips (extends basica-swing TokenInputPane)
├── SearchSubmitTracker.kt
├── Splash.kt            # Preloader splash
└── theme/BlackTerminalTheme.kt

service/      # Business logic
├── LinkService.kt       # CRUD operations for links (suspend functions), file pickers
├── LinkExecutor.kt      # Command execution engine (coroutines)
├── LinkCommand.kt       # Command template parameter binding
├── TextMatcher.kt       # Search with Boolean operators
├── KeywordParser.kt     # Converts search to postfix notation (RPN)
├── SearchToken.kt       # Token-based search terms
├── ShortcutSettings.kt  # Shortcut actions and user bindings (KeyStroke)
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

### What belongs in basica-swing

Generic, app-independent code goes into `basica-swing` (`C:\project_nayasis\basica-swing`), not here: token input controls (`TokenInputPane`, `TokenField`, `TokenChip`), `AutoCompletion`, `PopOver`, `HistorySet`, `KeyStroke` helpers (`toKeyStroke()`, `displayText`, `matches`), file drop (`onFileDrop`), image helpers, table helpers (`installColumnVisibilityMenu`). Only launcher-specific logic stays in this project.

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

### 2. Coroutines
Coroutines are the concurrency model; do not add raw threads or `SwingWorker`.

- UI code runs on `Dispatchers.Swing`. Helpers from `basica-swing`: `launchUi`, `launchAsync`, `withUi`, `withBackground`, `runUiAndWait`, `runLater`.
- `LinkService` exposes `suspend` functions: database work runs in the background, mutation of the shared link list happens on the EDT. `launchSave(link)` is the fire-and-forget variant.
- `LinkExecutor` owns a `CoroutineScope(SupervisorJob() + Dispatchers.Swing + CoroutineExceptionHandler)`. `run(link, files)` returns a `Job`; blocking waits on processes go through `runInterruptible(Dispatchers.IO)`.
- The search filter in `Main` is a cancellable job: a new search cancels the previous one, and the evaluation runs on `Dispatchers.Default` with `ensureActive()` checks.

### 3. Boolean Search Engine
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

### 4. Relative Path Support
Links store both absolute and relative paths with fallback resolution:
1. Try absolute path
2. Try relative to application root
3. Use stored relative path

This enables portable link catalogs across machines.

### 5. Command Parameter Binding
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
- **Location**: `./db/simplelauncher` (set in `application.yml`; in-memory for tests)
- **Initialization**: `ExposedHelper.connectDatabase()`, called from `Simplelauncher.afterInitialization()`
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

### Look and feel
`Simplelauncher.setupLookAndFeel()` calls `FlatIntelliJLaf.setup()`. Use FlatLaf client properties for styling (for example `"JComponent.outline"` for error outlines, `"JComponent.focusOwner"` for composite focus borders) instead of custom painting.

### Main Window (Main.kt)
`Main` extends `SwingView` and builds its whole component tree in code. Components that tests or menus look up carry a `name` (`tableMain`, `menuViewDesc`, `menuShowInputGroup`, `menuAlwaysOnTop`, ...).

- **Search field**: `SearchTokenField` plus group/hashtag token fields, with `AutoCompletion` popups for history, groups and hashtags
- **Link table**: `FilterSortList` bound to a `JTable` through `ListTableModel`; `predicate` filters, `items` is the full list. Titles are drawn with a renderer that caches icons per link.
- **Editor panel**: lives in a split pane that is swapped in and out to show/hide it (Alt+E)
- **Window state**: saved in `Config.windowMain` / `Config.windowTerminal` as `WindowProperty`

Key methods:
- `setSearchFilter()`: cancels the previous search job, evaluates links off the EDT, then applies the predicate on the EDT. Callers that need the result use its `onApplied` callback.
- `submitSearch()`: re-reads the inputs and runs `setSearchFilter()`
- `saveDetail()`: persists the edited link via `LinkService`
- `drawDetail(link)`: fills the editor panel from a link
- Execution goes through `Context.linkExecutor.run(link)`

### Terminal (Terminal.kt)
`Terminal` is a `JFrame` holding a JediTerm `JediTermWidget`:
- `suspend fun run(command)` starts the command through pty4j (`PtyProcessTtyConnector`, UTF-8) and resumes when it exits
- `BlackTerminalTheme` sets colors and the font. The font comes from `StyleContext` so that it is a composite font with glyph fallback; a plain `Font("Consolas")` renders Hangul as boxes.
- JediTerm is pinned to 3.72 from the JetBrains repository: 3.73+ needs kotlin-stdlib 2.4.

## Testing

### Test Structure
- **Unit tests**: `KeywordParserTest`, `TextMatcherTest`, `SearchTokenTest`, `ShortcutSettingsTest`, `SearchSubmitTrackerTest`, `MainShortcutRoutingTest`, `MainTableContextMenuTest`, `SearchTokenFieldTest`, `ShortcutEditorTest`, `LauncherChildWindowsTest`, `TerminalStagePropertyTest`
- **Integration tests**: `LinksTest` (database), `LinkServiceSuggestionTest`, `LinkExecutorTest` (disabled)
- **Framework**: JUnit 5 + Kotest assertions

Swing tests run their body on the EDT with `runUiAndWait<Unit> { ... }`. The explicit `<Unit>` matters: a test written as `fun x() = runUiAndWait { ... }` whose block ends in an assertion returns a non-`Unit` value, and JUnit silently skips non-void test methods.

Not everything under `src/test` is an automated test. `TerminalDemo` and `ShortcutEditorManualTest` are manual demos with a `fun main()` that assert nothing; run them with `./gradlew runTerminalDemo` / `./gradlew runShortcutEditorTest`. Don't assume a `*Test` name means there is coverage.

### Running Tests
```bash
# All tests
./gradlew test

# Specific test class
./gradlew test --tests "KeywordParserTest"

# With debug output
./gradlew test --info
```

## JVM arguments

`build.gradle.kts:appJvmArgs` holds `--enable-native-access=ALL-UNNAMED` (JNA for pty4j, FlatLaf native libraries). It is wired into `application.applicationDefaultJvmArgs`, every `JavaExec` task, and passed to `createNativeExe` as `--java-options`.

### jlink module list

`requiredJdkModules` in `build.gradle.kts` drives `createRuntimeImage`. Removing a module still passes `./gradlew run` (the full JDK is present) but can break the packaged app at runtime, so always verify module changes with `./gradlew createNativeExe` and by running the produced executable, not just `./gradlew run`.

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
# Output: build/native/nativeCompile/simplelauncher.exe
```

Not re-verified since the move to Swing.

Do not call `./gradlew nativeCompile` directly — native-image drives `cl.exe`, and
without the MSVC environment it fails with `fatal error C1034: stdio.h`.
`native-build.bat` runs `vcvars64.bat` first, then Gradle.

Requirements:
- A GraalVM-based JDK 25 (Liberica NIK 25) at `%NATIVE_JAVA_HOME%`.
- Visual Studio 2022 Build Tools, C++ workload plus Windows SDK.
- A local copy of the shared reachability metadata (the plugin cannot fetch it
  from GitHub releases here); see the comment in `build.gradle.kts`.

Things that cost time to rediscover:

| Trap | Why |
|---|---|
| No classpath to scan at runtime | Anything built on `Classes.findResources` glob patterns silently finds nothing. That is why message bundles are listed explicitly in `Simplelauncher.kt` and the `@Inject` services are registered in `beforeInitialization()` instead of relying on `SwingApp`'s package scan. Adding a new `@Inject` service means registering it there too. |
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
- **basica-swing**: Custom Swing library. Supplies `SwingApp`/`SwingView`, `SimpleDiContainer`, `Dialog`, `ProgressDialog`, `DefaultPreloader`, `FilterSortList`/`ListTableModel`, `WindowProperty`, token controls, `AutoCompletion`, `PopOver`, `HistorySet`, `KeyStroke` helpers and the coroutine helpers. Developed in parallel at `C:\project_nayasis\basica-swing` and consumed from mavenLocal as `0.1.0-SNAPSHOT`, so run `./gradlew publishToMavenLocal` there after changing it.
- **FlatLaf**: look and feel (IntelliJ theme)
- **kotlinx-coroutines-swing**: `Dispatchers.Swing`
- **Exposed 1.0.0-rc-4**: Database ORM
- **Exposed-CRUD**: KSP code generator for repositories
- **JediTerm 3.72 + pty4j**: Terminal emulator component
- **mslinks**: Windows .lnk file parser

## Common Development Patterns

### Adding a New Link Field
1. Add property to `Link.kt` entity
2. Run `./gradlew clean kspKotlin` to regenerate `LinkTable.kt`
3. Update UI bindings in `Main.kt`
4. Update JSON export/import in `JsonLink.kt`
5. Migration: Database recreates on first run

### Adding a New Service
1. Create the service in the `service/` package and annotate it `@Inject`
2. Register it in `Simplelauncher.beforeInitialization()` (the package scan does not work in a native image)
3. Add a lazy property to `Context.kt` that resolves it

Views are not DI beans and must not be annotated: `SwingApp.find()` instantiates them through their no-arg constructor and caches them.

### Modifying Search Logic
- **Parser**: `KeywordParser.kt:toPostfix()` (infix to RPN)
- **Matcher**: `TextMatcher.kt:match()` (evaluates postfix)
- **Indexing**: `Link.kt:keywords` property (space-delimited)

### Adding Keyboard Shortcuts
Shortcuts are data, not hardcoded key handlers. They are user-rebindable through the shortcut settings dialog and stored as `KeyStroke`s; the config keeps them as text like `Ctrl+Shift+N` (strings saved by the JavaFX version still parse).

1. Add an entry to the `ShortcutAction` enum in `ShortcutSettings.kt` with its message key, `ShortcutGroup` and default key stroke
2. Add the message key to `src/main/resources/message/`
3. Handle it in `handleMainShortcutEvent()` in `Main.kt` and add the matching lambda to `MainShortcutHandlers`
4. For table context-menu entries, add the action to `TABLE_ITEM_SHORTCUT_ACTIONS` in `Main.kt`

Main-window shortcuts are routed by a global `KeyEventDispatcher`, which ignores key events while a menu is open. Menus are built in `Main.kt`.

## Icon Handling

Icons are stored as image bytes in the database:
- **Extraction**: `Link.readIcon(file)` / `Link.setIcon(file)` read images and .ico files directly; anything else (.exe, .lnk, ...) uses the system icon (`File.toSystemIconImage`)
- **Export**: Base64-encoded in JSON
- **Display**: decoded lazily through the `iconImage` property (`BufferedImage`)

When adding icon support for new file types, modify `Link.readIcon()`.

## Recent Changes

- Moved from JavaFX to Swing + FlatLaf IntelliJ, from `basicafx-kt` to `basica-swing`, and from JediTermFx to JediTerm Swing. Concurrency now uses coroutines. Generic controls moved into basica-swing. Window state saved by the JavaFX version (`stageMain`/`stageTerminal`) is not migrated.
- Table rendering caches icons; search filtering runs off the EDT
- Migrated from Komapper to Exposed ORM (commit: 4543400)
- Added jpackage support for native executables (commit: be3b3b3)

## Contact

- Issues: https://github.com/nayasis/SimpleLauncher/issues
- Email: nayasis@gmail.com
