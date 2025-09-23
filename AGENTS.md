# Repository Guidelines

## Project Structure & Module Organization
- Source lives in `src/main/kotlin`, organized by feature packages (`service`, `view`, etc.) supporting the JavaFX launcher. 
- Shared resources and FXML sit under `src/main/resources`. 
- Tests mirror this layout in `src/test/kotlin` with fixtures in `src/test/resources`. 
- Build logic resides in `build.gradle.kts`. 
- Runtime assets (`conf/`, `db/`, `work/`) hold default catalogs, the embedded H2 database, and sample launch scripts; document any custom paths in PRs.

## Build, Test, and Development Commands
- `./gradlew run` – start the JavaFX app from source and attach to the dev toolchain.
- `./gradlew build` – compile Kotlin, run tests, and assemble the distributable JAR under `build/libs/`.
- `./gradlew runtime` – produce the self-contained runtime image in `build/image/` for packaging.
- `./gradlew test` – execute the JUnit 5 / Kotest suite; run before sharing changes. Use `./gradlew clean` when caches misbehave.

## Coding Style & Naming Conventions
Stick to the Kotlin coding conventions: 4-space indentation, PascalCase for types, lowerCamelCase for functions/properties, and SCREAMING_SNAKE_CASE constants. Keep package names aligned with directory structure (`io.github.nayasis.simplelauncher`). Use Kotlin null-safety and logging utilities instead of raw exceptions or println for diagnostics. Place UI resources in `src/main/resources/view` and configuration YAML in `src/main/resources`; name files with kebab-case for readability.

## Testing Guidelines
Tests belong in `src/test/kotlin`, mirroring production packages. The suite mixes Kotest, JUnit 5, and TestFX for JavaFX interactions; follow the existing `*Test.kt` naming so Gradle discovers files. Prefer lightweight unit coverage for services and add TestFX scenarios when UI behaviour changes. Store fixtures alongside tests in `src/test/resources`. Always run `./gradlew test` (and document notable edge cases) before opening a PR.

## Commit & Pull Request Guidelines
Commit messages are concise and action-oriented (`update readme`, `bug fix: keep wait feature`). Use the imperative mood, optionally prefixing the affected area. Keep refactors and behavioural changes separate to simplify review. Pull requests should describe motivation, list major changes, link related issues, and mention any config or database implications. Attach UI screenshots when JavaFX views shift and note the verification commands you executed.
