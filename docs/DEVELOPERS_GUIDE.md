# ChaosBank-Android — Developer's Guide

Everything you need to **build, run, test, and extend** the app. For the *why* behind the
structure, read [ARCHITECTURE.md](ARCHITECTURE.md) first; to use the app as a QA fixture,
read [USER_GUIDE.md](USER_GUIDE.md).

---

## 1. Prerequisites

| Tool | Version |
|---|---|
| JDK | 21 |
| Android SDK | compileSdk 37, minSdk 29 (Android 10) |
| Python 3 | for the catalog checker (stdlib only) |

No extra package manager — Gradle resolves AndroidX / Compose / Kotlin coroutines only.

---

## 2. Build & run

```bash
./gradlew :app:assembleStandardDebug          # build the clean APK
./gradlew :app:installStandardDebug           # install to a connected device/emulator
```

Launch a specific scenario via an Intent extra:

```bash
adb shell am start -n com.vadimtoptunov.chaosbank_android/.MainActivity \
  -e CHAOSBANK_PROFILE flaky -e CHAOSBANK_START_UNLOCKED 1
```

### Product flavors (distributable per-defect builds)

Beyond runtime extras, Gradle **product flavors** bake a profile into a standalone APK
(the Android analogue of iOS build configurations). Each flavor sets a distinct
`applicationId` suffix, a display name, and `BuildConfig.CHAOSBANK_BAKED_PROFILE`, read
in one place by `ConfigResolver.bakedDefaultProfile`.

| Flavor | Baked profile | Assemble task |
|---|---|---|
| `standard` | clean | `:app:assembleStandardDebug` |
| `flaky` | flaky | `:app:assembleFlakyDebug` |
| `security` | security | `:app:assembleSecurityDebug` |
| `senior` | senior | `:app:assembleSeniorDebug` |
| `everything` | all | `:app:assembleEverythingDebug` |

Adding another baked build is one entry in the `chaosFlavors` list in
[`app/build.gradle.kts`](../app/build.gradle.kts).

---

## 3. Tests & coverage

Run the unit suite (host JVM, `standard` flavor):

```bash
./gradlew :app:testStandardDebugUnitTest
```

Run the **coverage gate** (this is what CI runs — always run it locally before pushing):

```bash
./gradlew :app:jacocoCoverageVerification      # LINE ≥ 90% on the logic layer
./gradlew :app:jacocoTestReport                # HTML/XML report
```

Coverage is measured on the **logic layer** only. The `coverageExcludes` list in
`app/build.gradle.kts` drops Compose UI (`**/ui/**`, `**/features/**/*Screen*`),
Android-bound infra (`MainActivity`, `TokenStore`, `AppServices`, `AuthFlow`), the live
network service, and the dev screens — none of which host JVM unit tests execute. The
report/verification tasks read the `standardDebug` classes from
`intermediates/built_in_kotlinc/...` and the exec data from
`unit_test_code_coverage/standardDebugUnitTest`.

> **Standing rule:** never push Android without a green `jacocoCoverageVerification`
> locally — the CI gate is identical.

### The test pattern

Every regression test asserts the same thing on `clean` and under the defect:

```kotlin
@Test fun staleBalanceRefreshesOnClean() = runTest {
    Defects.configure(ChaosConfig(0, emptySet(), "clean", PriceSourceKind.Simulated))
    // … assert balance refreshes after a transfer (passes)
}

@Test fun staleBalanceKeepsOldValueWhenActive() = runTest {
    Defects.configure(ChaosConfig(0, setOf(DefectId.staleBalance), "t", PriceSourceKind.Simulated))
    // … assert Home keeps the pre-transfer balance (this is the bug)
}
```

Reset to `clean` between tests (see `support/TestSupport.kt`) so config doesn't leak.

---

## 4. Adding a new defect (the full recipe)

Every increment must keep both platforms **1:1** — add the same defect, with the same
`DefectId` name and behavior, to [ChaosBank-iOS](https://github.com/VadimToptunov/ChaosBank-iOS)
in the same change set. See [CONTRIBUTING.md](../CONTRIBUTING.md#the-11-parity-contract).

1. **Declare the id** — add an entry to
   `core/defects/DefectId.kt`. The name is the public identifier (used in
   `exercises.json`, launch args, parity); keep it identical to the iOS `DefectID`.
2. **Register metadata** — add a `Defect(...)` entry in `core/defects/DefectRegistry.kt`
   (title, category, feature, `violates`).
3. **Add locators if needed** — any new test tag goes in `core/A11y.kt` (the single
   source). Reuse existing tags where possible; **never** change a tag to express a
   defect.
4. **Inject the guard** — at the correct code path, wrap the buggy override:
   ```kotlin
   if (Defects.isActive(DefectId.myNewDefect)) { /* buggy */ } else { /* correct (default) */ }
   ```
   Keep it small and isolated; the `else` branch is the reference implementation.
5. **Write the exercise guidance** — add the spec for the id in
   `core/exercises/Exercise.kt` (difficulty, task, expectedClean, expectedBuggy,
   locators).
6. **Add tests** — a clean-pass + defect-fail pair under `app/src/test/...`.
7. **Regenerate the catalog** — see [§5](#5-regenerating-exercisesjson). Do **not**
   hand-edit `exercises.json`.
8. **Add to a profile (optional)** — if it belongs in a category/difficulty bundle, add
   it in `BugProfiles`.
9. **Verify** — `./gradlew :app:jacocoCoverageVerification` and
   `python3 scripts/check_exercises.py exercises.json AND`.
10. **README** — add a row to the defect table if it's noteworthy.

---

## 5. Regenerating `exercises.json`

Ids are generated per-category from `DefectId.entries`, so **inserting a defect renumbers
its category** — never hand-edit the file. The `CatalogJsonTest` drift-guard both
regenerates and verifies it:

```bash
# Rewrite exercises.json from the catalog (source of truth: Exercise.kt)
./gradlew :app:testStandardDebugUnitTest -DupdateExercises=1

# Without the flag, the same test FAILS if the committed file drifts from the catalog.
./gradlew :app:testStandardDebugUnitTest
```

---

## 6. Catalog validation & cross-platform parity

```bash
python3 scripts/check_exercises.py exercises.json AND \
  https://raw.githubusercontent.com/VadimToptunov/ChaosBank-iOS/main/exercises.json
```

- **Structural/schema check (hard gate):** required keys, id pattern/prefix, unique ids,
  enum-valid category/difficulty, non-empty strings. Uses `jsonschema` if installed, else
  an equivalent stdlib check against [`exercises.schema.json`](../exercises.schema.json).
- **Parity check:** the set of all `defects` names here must equal the set in the iOS
  repo's `main`. Skipped (not failed) if the sibling can't be fetched, so a network blip
  never reds CI; fails loudly on a real drift.

---

## 7. Continuous integration

[`.github/workflows/android.yml`](../.github/workflows/android.yml) on `ubuntu-latest`:

1. **Validate catalog & parity** — `check_exercises.py` runs first (fails fast, pre-build).
2. JDK 21 + Gradle setup.
3. `:app:testStandardDebugUnitTest` — unit tests (includes the catalog drift-guard).
4. `:app:jacocoCoverageVerification` — logic-layer coverage floor.
5. `:app:jacocoTestReport` + upload the HTML report artifact.
6. `:app:assembleStandardDebug` — build the APK.

---

## 8. Conventions

- **Smallest possible change.** No unrelated refactors, no fat comment blocks.
- **Locators are sacred.** New tags only in `A11y.kt`; never repurpose one to express a
  bug.
- **Money is `BigDecimal` (HALF_EVEN).** The only `Double` money path is the
  `roundingDrift` defect.
- **Determinism.** Anything random must derive from the seed via `SeededRng`.
- **1:1 with iOS.** Same defect names and behavior; regenerate both catalogs; keep the
  parity checker green.
- **Do not hand-edit `exercises.json`.**
- **Kotlin gotcha:** a `var x …; private set` plus a `fun setX(...)` collides
  ("Platform declaration clash"); name mutators distinctly (`updateX`, `applyX`).

### Glossary

| Term | Meaning |
|---|---|
| **Profile** | A named defect bundle (`clean`, `flaky`, `security`, `all`, …). |
| **Seed** | Numeric RNG seed; also maps to a defect bundle via `DefectRegistry`. |
| **Flavor** | A Gradle product flavor that bakes a profile into a distributable APK. |
| **Exercise** | The tester-facing task for one defect (in `exercises.json`). |
| **Parity** | The invariant that iOS and Android expose the identical defect-name set. |
