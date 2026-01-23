# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Simple Launcher** is a JavaFX desktop application for organizing and launching applications with advanced search capabilities. It features a database-backed link catalog with Boolean search operators, keyboard shortcuts, drag-and-drop support, and embedded terminal execution.

**Tech Stack**: Kotlin 2.2.0, JavaFX 21, TornadoFX, Exposed ORM with KSP code generation, H2 database, JediTermFx terminal emulator.

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

# Create native executable (requires Java 14+ with jpackage)
./gradlew createNativeExe
# Output: build/dist/simplelauncher/
```

## Architecture

### Layered Structure

```
view/         # JavaFX UI (Main, Terminal, Splash)
├── Main.kt       # Primary window with table, editor, search
└── Terminal.kt   # PTY terminal wrapper using JediTermFx

service/      # Business logic
├── LinkService.kt    # CRUD operations for links
├── LinkExecutor.kt   # Command execution engine
├── TextMatcher.kt    # Search with Boolean operators
└── KeywordParser.kt  # Converts search to postfix notation (RPN)

model/        # Domain entities
├── Link.kt       # Main entity with @Entity annotation for code gen
└── JsonLink.kt   # Import/export DTO

common/       # Utilities
├── Context.kt        # Service locator pattern
├── ExposedHelper.kt  # Database initialization
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
- **Initialization**: `ExposedHelper.kt:init()`
- **Schema**: Auto-created via `SchemaUtils.create(LinkTable)`

### Transactions
Use `ExposedHelper.runInTransaction {}` for all database operations:
```kotlin
ExposedHelper.runInTransaction {
    LinkTable.repo.create(link)
}
```

### Testing with Database
```kotlin
@BeforeTest
fun setup() {
    ExposedHelper.init("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
    transaction {
        SchemaUtils.create(LinkTable)
    }
}
```

## UI Components

### Main Window (Main.kt)
- **Search field**: Live filtering with 300ms debounce
- **Link table**: `SortedFilteredList` with reactive binding
- **Editor panel**: Toggle with Alt+E
- **Keyboard shortcuts**: See README.md for complete list

Key methods:
- `searchKeyword()`: Filters table using `TextMatcher`
- `save()`: Persists link via `LinkService`
- `execute()`: Runs selected link via `LinkExecutor`

### Terminal (Terminal.kt)
Wraps commands in PTY-based terminal using JediTermFx:
- Executes command and captures output
- Handles ANSI color codes
- Configurable theme (`BlackTerminalTheme`)

## Testing

### Test Structure
- **Unit tests**: `KeywordParserTest`, `TextMatcherTest`
- **Integration tests**: `LinksTest` (database), `LinkExecutorTest` (marked `@Ignored`)
- **Framework**: JUnit 5 + Kotest StringSpec

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
--add-exports=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED
--add-exports=javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED
--add-opens=javafx.graphics/javafx.scene=ALL-UNNAMED
```

These are configured in `build.gradle.kts:application.applicationDefaultJvmArgs` and automatically applied to `createNativeExe`.

## Distribution

### Fat JAR
```bash
./gradlew shadowJar
# Output: build/libs/simplelauncher-0.1.5-all.jar
```

### Native Executable
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
- **basicafx-kt**: Custom JavaFX utilities
- **Exposed 1.0.0-rc-4**: Database ORM
- **Exposed-CRUD**: KSP code generator for repositories
- **TornadoFX**: Kotlin DSL for JavaFX
- **JediTermFx**: Terminal emulator component
- **mslinks**: Windows .lnk file parser

## Common Development Patterns

### Adding a New Link Field
1. Add property to `Link.kt` entity
2. Run `./gradlew clean kspKotlin` to regenerate `LinkTable.kt`
3. Update UI bindings in `Main.kt`
4. Update JSON export/import in `JsonLink.kt`
5. Migration: Database recreates on first run (H2 in user directory)

### Adding a New Service
1. Create service in `service/` package
2. Add lazy property to `Context.kt`
3. Initialize in `Context.init()`

### Modifying Search Logic
- **Parser**: `KeywordParser.kt:toPostfix()` (infix to RPN)
- **Matcher**: `TextMatcher.kt:match()` (evaluates postfix)
- **Indexing**: `Link.kt:keywords` property (space-delimited)

### Adding Keyboard Shortcuts
- **Global**: Register in `Main.kt:init()` using `setOnKeyPressed`
- **Menu**: Use TornadoFX `item("Name", "Shortcut") { action }`
- **Context menu**: Add to `contextmenu { }` block

## Icon Handling

Icons are stored as PNG bytes in database:
- **Extraction**: From .exe, .ico, .lnk files via `Link.kt:extractIcon()`
- **Conversion**: All formats converted to PNG on import
- **Export**: Base64-encoded in JSON
- **Display**: Lazy-loaded from `iconBase64` property

When adding icon support for new file types, modify `Link.kt:extractIcon()`.

## Recent Changes

- Migrated from Komapper to Exposed ORM (commit: 4543400)
- Added jpackage support for native executables (commit: be3b3b3)
- Improved transaction handling in ExposedHelper (commit: da523b1)

## Contact

- Issues: https://github.com/nayasis/SimpleLauncher/issues
- Email: nayasis@gmail.com
