# Repository Guidelines

## Project Structure & Module Organization

Gugu ABC is an offline English-learning Android tablet app using Kotlin and Jetpack Compose. Paths below are relative to this Gradle project, `app/`.

- `src/main/java/com/guguabc/kids/`: `screens/` contains UI, `design/` shared tokens and components, `state/` navigation and learning logic, `data/` vocabulary and copy, and `audio/` playback.
- `src/main/res/`: illustrations, fonts, vectors, and bundled OGG audio; `src/main/assets/` holds license notices.
- `src/test/` contains JVM tests; `src/androidTest/` contains Compose UI and navigation tests.
- `tools/` contains asset preparation and verification scripts. `word_packs.json` is the vocabulary source of truth.
- Consult `../design.md`, `../design_v2.md`, and `../gugu-abc-android-assets/` for specifications and visual baselines.

## Build, Test, and Development Commands

Use JDK 17 and Android SDK 34. Configure the SDK in untracked `local.properties`. Run from `app/`; on macOS/Linux, replace `.\gradlew.bat` with `./gradlew`.

- `.\gradlew.bat assembleDebug assembleRelease`: build debug and unsigned release APKs.
- `.\gradlew.bat installDebug`: install on a connected emulator or device; launch Gugu ABC there.
- `.\gradlew.bat testDebugUnitTest`: run JVM tests.
- `.\gradlew.bat lintDebug`: run Android Lint.
- `python tools/verify_project.py`: check design, resource, and vocabulary contracts.
- `.\gradlew.bat assembleDebug assembleDebugAndroidTest`: build app and instrumentation APKs. Follow `../README.md` to install both and run instrumentation; the documented Chinese-path UTP issue affects `connectedDebugAndroidTest`.
- `python tools/verify_screens.py all`: capture and compare five baseline screens on the configured Honor Pad X9 with `adb` available.

## Coding Style & Naming Conventions

Use official Kotlin style, four-space indentation, PascalCase classes/composables, and camelCase functions/properties. Match existing trailing-comma formatting. Name resources in lowercase snake_case, such as `word_cat.ogg`.

Reuse `GuguTokens.kt` and `data/Strings.kt` instead of embedding colors or Chinese UI copy. Keep dependency versions in `gradle/libs.versions.toml`; upgrade AndroidX test dependencies together. No standalone formatter is configured.

## Testing Guidelines

Use JUnit 4 for pure logic and Compose UI tests for interactions. Name classes `*Test` and methods descriptively, such as `sameInputsAreReproducible`. Preserve test tags and use `TestHarness.kt` for component tests. Cover changed behavior; no numeric coverage threshold is configured. Static, behavioral, and pixel checks are complementary.

## Commit & Pull Request Guidelines

History uses concise subjects such as `feat: …` and `feat(v2): …`, with Chinese summaries. Follow that pattern. Describe behavior changes, link relevant issues/specifications, list validation performed, and include screenshots for UI changes.

## Configuration & Product Constraints

Preserve offline operation, zero manifest permissions, and landscape layout. The approved V2 category home and lesson chooser in `docs/v2-ui/` supersede the old homepage baseline. Run `python tools/generate_catalog.py` after changing vocabulary and regenerate font subsets for new copy. Keep SDK paths, signing credentials, and build outputs out of Git; retain bundled font and audio notices.
