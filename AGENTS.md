# Repository Guidelines

This file provides guidance for coding agents working in this repository.

## Project Overview

**Simple Launcher** is a Swing desktop application for organizing and launching applications with advanced search capabilities. It features a database-backed link catalog with Boolean search operators, keyboard shortcuts, drag-and-drop support, and embedded terminal execution.

**Tech Stack**: Kotlin 2.4.21, Swing with FlatLaf (IntelliJ theme), kotlinx-coroutines (`Dispatchers.Swing`), Exposed ORM with KSP code generation, H2 database, JediTerm (Swing) terminal emulator with pty4j. Java toolchain 25.

The UI is plain Swing built in code (no form files), with the `basica-swing` library supplying the `SwingApp`/`SwingView` base classes, dialogs, controls and coroutine helpers. JavaFX, TornadoFX and `basicafx-kt` were removed — do not reintroduce them.

## Project Structure & Module Organization
- Source lives in `src/main/kotlin`, organized by feature packages (`service`, `view`, etc.) supporting the Swing launcher. 
- Shared resources (images, message bundles, application.yml) sit under `src/main/resources`. 
- Tests mirror this layout in `src/test/kotlin` with fixtures in `src/test/resources`. 
- Build logic resides in `build.gradle.kts`. 
- Runtime assets (`conf/`, `db/`, `work/`) hold default catalogs, the embedded H2 database, and sample launch scripts; document any custom paths in PRs.

## Coding Style & Naming Conventions
Stick to the Kotlin coding conventions: 4-space indentation, PascalCase for types, lowerCamelCase for functions/properties, and SCREAMING_SNAKE_CASE constants. Keep package names aligned with directory structure (`io.github.nayasis.simplelauncher`). Use Kotlin null-safety and logging utilities instead of raw exceptions or println for diagnostics. Build Swing UI components in code; place supporting UI resources in `src/main/resources/view` and configuration YAML in `src/main/resources`. Name resource files with kebab-case for readability.

## Build, Test, and Development Commands

```bash
# Build the project
./gradlew build

# Run the application
./gradlew run

# Run tests (JUnit 5 / Kotest)
./gradlew test

# Run tests with debug output
./gradlew test --info

# Run a specific test class
./gradlew test --tests "io.github.nayasis.simplelauncher.service.SearchTokenTest"

# Manual demos (assert nothing)
./gradlew runTerminalDemo
./gradlew runShortcutEditorTest

# Create fat JAR
./gradlew shadowJar

# Bundle a trimmed JVM with the app (jpackage)
./gradlew makeExecutable
# Output: build/dist/simplelauncher/

# Package and install to D:/app/SimpleLauncher (or C:/app/SimpleLauncher)
./gradlew deploy
```

Use `./gradlew clean` when build caches misbehave.

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
├── SearchToken.kt       # Search tokens and their Boolean evaluation
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
A search is a list of `SearchToken`s (chips in `SearchTokenField`): terms, `AND`, `OR`, `NOT` and parentheses.
There is no text syntax: typed text without chips is searched as one term, and Enter turns it into that chip.

- Precedence: parentheses > `NOT` (prefix) > `AND` > `OR`
- Adjacent terms, and a `NOT` after a term, are joined with an implicit `AND`
- Shift+Enter on a term joins it with `OR` (`insertTermWithOr`); Ctrl+Enter always adds a plain term
- A malformed expression falls back to matching any of its terms, minus what a `NOT` right before a term excludes
- The field does not let `AND` / `OR` in where an operand is missing: at the start, after `(` or before `)`
- Terms match by substring or by fuzzy subsequence (`matchesSearchTerm`); what a `NOT` excludes matches by substring only

`matchesSearchTokens()` in `SearchToken.kt` evaluates the tokens with a shunting-yard pass.

### 4. Relative Path Support
Links store both absolute and relative paths with fallback resolution:
1. Try absolute path
2. Try relative to application root
3. Use stored relative path (this corrects `path` and saves the link, if it is saved already)

The application root is `applicationRoot` in `Commons.kt`: the directory of the packaged executable on Windows, otherwise the working directory. The database and `conf/` are resolved against it too.

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

- `${home}` is bound always, the file variables when files are dropped; any other `${...}` is left as it is
- With no file variable in any field, the dropped files are appended to the argument
- Otherwise the argument repeats once per file, and so does each line of the previous / next commands that uses a file variable; the prefix takes the first file
- Outside Windows the quotes are taken off the arguments (`unquote()`), as no shell reads them

Commands without a console run with their output inherited, never piped: an unread pipe blocks the program once it is full.

## Database Layer

### H2 Database
- **Location**: `./db/simplelauncher` (set in `application.yml`; `./` means the application root, see Relative Path Support; in-memory for tests)
- **Connections**: pooled with H2's `JdbcConnectionPool`
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
- JediTerm 3.72 comes from the JetBrains repository (the Swing build is published only there).

## Testing Guidelines

Tests belong in `src/test/kotlin`, mirroring production packages. The suite mixes Kotest and JUnit 5; Swing tests run on the EDT via `runUiAndWait<Unit> { }`; follow the existing `*Test.kt` naming so Gradle discovers files. Prefer lightweight unit coverage for services and add component-level tests when UI behaviour changes. Store fixtures alongside tests in `src/test/resources`. Always run `./gradlew test` (and document notable edge cases) before opening a PR.

### Test Structure
- **Unit tests**: `SearchTokenTest`, `ShortcutSettingsTest`, `SearchSubmitTrackerTest`, `MainShortcutRoutingTest`, `MainTableContextMenuTest`, `SearchTokenFieldTest`, `ShortcutEditorTest`, `LauncherChildWindowsTest`, `TerminalStagePropertyTest`, `LinkCommandTest`, `LanguagesTest`, `LinkIconTest`
- **Integration tests**: `LinksTest` (database, JSON export/import, path resolution, `.lnk` on Windows), `LinkServiceSuggestionTest`, `JsonMappingTest` (exposed-crud with the test entities `Department` / `Person`)
- **Framework**: JUnit 5 + Kotest assertions

Swing tests run their body on the EDT with `runUiAndWait<Unit> { ... }`. The explicit `<Unit>` matters: a test written as `fun x() = runUiAndWait { ... }` whose block ends in an assertion returns a non-`Unit` value, and JUnit silently skips non-void test methods.

Not everything under `src/test` is an automated test. `TerminalDemo` and `ShortcutEditorManualTest` are manual demos with a `fun main()` that assert nothing; run them with `./gradlew runTerminalDemo` / `./gradlew runShortcutEditorTest`. Don't assume a `*Test` name means there is coverage.

## JVM arguments

`build.gradle.kts:appJvmArgs` holds `--enable-native-access=ALL-UNNAMED` (JNA for pty4j, FlatLaf native libraries) and the memory flags (Serial GC, `-Xms8m -Xmx128m`, C1 only, small code cache and stacks), which bring the private working set on Windows from about 164MB to 125MB. A bare Swing window already takes about 42MB, so going much lower would mean dropping libraries (kotlin-reflect, jackson, ...), which was decided against. It is wired into `application.applicationDefaultJvmArgs`, every `JavaExec` task, and passed to `makeExecutable` as `--java-options`.

### jlink module list

`requiredJdkModules` in `build.gradle.kts` is passed to jpackage as `--add-modules`. Removing a module still passes `./gradlew run` (the full JDK is present) but can break the packaged app at runtime, so always verify module changes with `./gradlew makeExecutable` and by running the produced executable, not just `./gradlew run`.

## Distribution

### Fat JAR
```bash
./gradlew shadowJar
# Output: build/libs/simplelauncher-<version>-all.jar
```

The version comes from the branch name: `release/x.y.z` yields `x.y.z`, anything else yields `0.1.0-SNAPSHOT`.

### Native Executable (jpackage — bundles a JVM)
```bash
./gradlew makeExecutable
# Output: build/dist/simplelauncher/
```

One `jpackage` call over the `installDist` jars; jpackage runs jlink itself with `requiredJdkModules`. The package type is `app-image` unless `SIMPLELAUNCHER_PACKAGE_TYPE` says otherwise (the release workflow passes `deb` / `dmg`).

`./gradlew deploy` copies `app/`, `runtime/` and the exe next to the install directory first (`.deploy/`), then kills a running `simplelauncher.exe` (with `/T`, so programs it started end too) and swaps them in, leaving `conf/` and `db/` alone. It needs an app image.

`@Inject` services are registered in `beforeInitialization()`, a leftover from the GraalVM native-image build (no classpath to scan there). Adding a new `@Inject` service means registering it there too.

## Dependencies of Note

- **basica-kt**: Custom utility library (path, string, reflection, `Messages` helpers). Like basica-swing it is developed in parallel (`C:\project_nayasis\basica-kt`); the build uses the release `0.4.0` from Maven Central. To try local changes, publish a snapshot there, add `mavenLocal()` to the repositories for the time being and point the dependency at it.
- **basica-swing**: Custom Swing library. Supplies `SwingApp`/`SwingView`, `SimpleDiContainer`, `Dialog`, `ProgressDialog`, `DefaultPreloader`, `FilterSortList`/`ListTableModel`, `WindowProperty`, token controls, `AutoCompletion`, `PopOver`, `HistorySet`, `KeyStroke` helpers and the coroutine helpers. Developed in parallel at `C:\project_nayasis\basica-swing`; the build uses the release `0.1.0` from Maven Central. To try local changes, run `./gradlew publishToMavenLocal` there, add `mavenLocal()` for the time being and point the dependency at the snapshot.
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
- **Evaluation**: `SearchToken.kt:matchesSearchTokens()`
- **Turning input into tokens**: `createCommandOrTermSearchToken()`, `createOperatorSearchToken()`, `insertTermWithOr()` in `SearchToken.kt`; key and mouse handling in `SearchTokenField.kt`
- **What is searched**: `Main.setSearchFilter()` (title and hashtags for the keyword, groups for the group field)

### Localization
Labels come from `src/main/resources/message/localization.<lang>[_<COUNTRY>].prop` (`zh_CN` is language `zh`, country `CN`), read with `"key".message()`.

- Every bundle must have the same keys. `language.name` is the bundle's own name, shown in 설정 > 언어 (Settings > Language).
- The language menu lists whatever bundles exist (`Languages.available`); `Languages.ORDER` puts the most spoken first, unlisted ones go last.
- The choice is saved as a language tag in `Config.language` and applied at startup in `Simplelauncher.afterInitialization()` through `Languages.apply()`, ahead of `simplelauncher.locale` in `application.yml` and the system locale. `Languages.apply()` sets both `Locale.setDefault()` and `Messages.defaultLocale` (Messages keeps the default it saw on first use).
- A switch in the menu applies at once: build texts with `localize { text = "key".message() }` (basica-swing `Localization`) so `Localization.refresh()` reads them again. Texts put together from several parts (table headers, tooltips with shortcuts, the status line) are redone in the `Localization.onRefresh` block in `Main.init`. Dialogs and popups built on demand need nothing.

### Adding Keyboard Shortcuts
Shortcuts are data, not hardcoded key handlers. They are user-rebindable through the shortcut settings dialog and stored as `KeyStroke`s; the config keeps them as text like `Ctrl+Shift+N` (strings saved by the JavaFX version still parse). An action can be left without a shortcut (`null`, stored as `None`).

Shortcuts outside the `TABLE` group are caught before a focused text input sees the key, so `shortcutRestriction()` requires Ctrl, Alt or Command (or a function key) and keeps text editing and search history keys free. Defaults must pass it (`ShortcutSettingsTest`).

1. Add an entry to the `ShortcutAction` enum in `ShortcutSettings.kt` with its message key, `ShortcutGroup` and default key stroke
2. Add the message key to every bundle in `src/main/resources/message/`
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

- Packaging is a single jpackage task; the GraalVM native-image build was removed
- Moved from JavaFX to Swing + FlatLaf IntelliJ, from `basicafx-kt` to `basica-swing`, and from JediTermFx to JediTerm Swing. Concurrency now uses coroutines. Generic controls moved into basica-swing. Window state saved by the JavaFX version (`stageMain`/`stageTerminal`) is not migrated.
- Table rendering caches icons; search filtering runs off the EDT
- Migrated from Komapper to Exposed ORM (commit: 4543400)
- Added jpackage support for native executables (commit: be3b3b3)

## Commit & Pull Request Guidelines
Commit messages are concise and action-oriented (`update readme`, `bug fix: keep wait feature`). Use the imperative mood, optionally prefixing the affected area. Keep refactors and behavioural changes separate to simplify review. Pull requests should describe motivation, list major changes, link related issues, and mention any config or database implications. Attach UI screenshots when views shift and note the verification commands you executed.

## Contact

- Issues: https://github.com/nayasis/SimpleLauncher/issues
- Email: nayasis@gmail.com
