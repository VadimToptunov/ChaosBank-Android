# ChaosBank-Android — Architecture

This document explains **how the app is built** and, above all, **how known defects are
injected without corrupting the correct baseline**. If you only read one design section,
read [The defect-injection model](#the-defect-injection-model).

> Companion docs: [DIAGRAMS.md](DIAGRAMS.md) (UML for the sections below),
> [DEVELOPERS_GUIDE.md](DEVELOPERS_GUIDE.md) (how to build, test and extend),
> [USER_GUIDE.md](USER_GUIDE.md) (how to drive the app as a QA fixture),
> [CONTRIBUTING.md](../CONTRIBUTING.md) (workflow + the 1:1 parity contract). This app is
> the **1:1 Android port** of
> [ChaosBank-iOS](https://github.com/VadimToptunov/ChaosBank-iOS).

---

## 1. What this app is

ChaosBank is a **deliberately-buggy neobank + broker** used as a controlled practice
range for mobile QA / SDET automation. It looks and behaves like a real fintech product
(a Revolut/Robinhood-style hybrid), but every "bug" is a **named, switchable defect**
that can be turned on or off at launch or live. The clean build is a real, correct app;
defects are opt-in overlays.

- **Platform:** Android 10+ (minSdk 29), compileSdk/targetSdk 37, Jetpack Compose,
  Kotlin 2.2, Kotlin coroutines.
- **Toolchain:** JDK 21 + Android SDK, Gradle.
- **Dependencies:** none beyond AndroidX / Compose / Kotlin coroutines.
- **Determinism:** all randomness derives from a build seed (see [§6](#6-determinism--seeding)).

---

## 2. Layered structure

The codebase splits into a **logic layer** (pure, host-testable, correctness-owning) and
a **UI layer** (Compose). Defects live in the logic layer behind guards; the UI layer
only renders and never decides correctness.

```
app/src/main/java/com/vadimtoptunov/chaosbank_android/
├── MainActivity.kt        entry: resolves config, wires services, hosts RootScreen
├── app/                   ← app-shell logic
│   ├── AppServices.kt      dependency container
│   ├── AuthFlow.kt         the login → OTP → passcode → biometric ladder
│   ├── ConfigResolver.kt   resolves the active build (precedence)
│   ├── LaunchOptions.kt    non-defect test/demo affordances
│   ├── Navigator.kt        tab + Route navigation
│   ├── DeepLink.kt         chaosbank:// routing
│   ├── LocaleSettings.kt   RTL / locale dev toggles
│   ├── KycStore / NotificationStore / TemplateStore   small stateful stores
├── core/                  ← the logic layer; correctness lives here
│   ├── A11y.kt            ALL test tags (single source)
│   ├── defects/           DefectId, Defect, categories, registry, profiles, ChaosConfig, Defects
│   ├── money/             BigDecimal money, Currency, FxRates, AmountParser, LocaleFormat, LoanCalc
│   ├── feed/              seeded PriceFeed + live Yahoo source + MarketStore + PriceSourceKind
│   ├── backend/           in-memory MockBackend (Mutex) + BackendScenario + NetworkCondition
│   ├── exercises/         machine-readable exercise catalog (source of exercises.json)
│   ├── SeededRng.kt       SplitMix64 deterministic RNG
│   └── TokenStore.kt      session-token storage (Keystore vs SharedPreferences defect)
├── models/                Models.kt (Account, Transaction, Asset, Quote, Holding, Order) + SeedData
├── features/              one folder per screen: *Screen (Composable) + *ViewModel
└── ui/                    theme tokens, components (LiveTicker, Sparkline…), RootScreen, auth screens
```

Each feature follows **MVVM**: a `*Screen.kt` (Composable, dumb) plus a `*ViewModel.kt`
(holds `mutableStateOf` state and calls the backend). View models are the main unit-test
surface; Composables are covered by instrumented/reference suites.

---

## 3. The defect-injection model

**The single most important rule: production logic is always written correctly.** Money
math, order pricing, balance updates — the reference implementation is the correct one. A
defect is never baked into the core; it is injected at an explicit, guarded point behind
one query:

```kotlin
if (Defects.isActive(DefectId.doubleCharge)) {
    // small, isolated buggy override
}
```

`Defects` is the only surface a guard touches (a process-wide holder set once at launch
from `ConfigResolver.resolve()`).

Design consequences that tests rely on:

| Property | Why it holds |
|---|---|
| **Clean build is correct** | `clean` profile = empty active-defect set; every guard's `else` path is the reference implementation. |
| **Regression training works** | A test passes on `clean`, then fails when its defect is active — proving it catches the regression. |
| **Locators never move** | A defect changes *behavior or values*, never the `Modifier.testTag` surface. Every identifier is a constant in [`core/A11y.kt`](../app/src/main/java/com/vadimtoptunov/chaosbank_android/core/A11y.kt). |
| **One switch, many builds** | The same APK becomes any bug set via config — no scattered conditionals in features. |

### Defect model types

- **`DefectId`** — an enum; every defect is one entry (e.g. `doubleCharge`). The name is
  the stable public identifier used in `exercises.json`, launch args, and cross-platform
  parity. `DefectId.from(string)` parses a name back to the enum.
- **`DefectCategory`** — money, validation, localization, state, concurrency, ui,
  accessibility, security, network, performance (10 categories).
- **`Defect`** — descriptive metadata for one id: `title`, `category`, `feature`,
  `violates`, severity.
- **`DefectRegistry`** — the id → `Defect` table, plus `defectsForSeed(n)` mapping a
  numeric seed to a defect bundle.
- **`BugProfile` / `BugProfiles`** — named bundles (`clean`, category profiles, `flaky`,
  difficulty bundles, `all`), each with a pinned seed.

---

## 4. Build/config resolution

A run is described by a **`ChaosConfig`** (`seed`, `activeDefects`, `label`,
`priceSource`). `ConfigResolver.resolve(...)` computes it once at launch (`MainActivity`)
from the Intent extras + the baked flavor default, in strict precedence (highest first):

1. `CHAOSBANK_DEFECTS=a,b,c` — explicit defect list → label `custom`.
2. `CHAOSBANK_PROFILE=<id>` — a named profile.
3. `CHAOSBANK_SEED=<n>` — numeric seed mapping.
4. `ConfigResolver.bakedDefaultProfile` — set from the Gradle flavor's
   `BuildConfig.CHAOSBANK_BAKED_PROFILE` field.
5. `clean`.

Runtime overrides arrive as Intent string extras
(`adb shell am start … -e CHAOSBANK_PROFILE flaky`). This is why the same catalog is
reachable three ways — runtime extras for CI/dev, and **product flavors** for
distributable per-defect APKs that install side-by-side with their own `applicationId`
suffix and display name.

**Launch affordances** (`LaunchOptions`) are separate and **never change product
behavior** — they only skip the auth ladder or deep-link a tab so a test/screenshot can
start where it needs to (`CHAOSBANK_START_UNLOCKED`, `CHAOSBANK_TAB`,
`CHAOSBANK_SHOW_DEV`, `CHAOSBANK_SHOW_WEB_LOGIN`).

---

## 5. The exercise catalog pipeline

Every defect has exactly one **exercise** — a self-contained task telling a tester what
to automate, what the clean vs buggy outcome is, and which locators to use.

```
DefectRegistry (metadata)  ┐
Exercise.kt specs (guidance)┼──▶ Exercises.all ──▶ toJson() ──▶ exercises.json
DefectId.entries (order)   ┘                                       ▲
                                                                   │
                             exercises.schema.json + check_exercises.py (CI gate)
```

- **Source of truth:** [`core/exercises/Exercise.kt`](../app/src/main/java/com/vadimtoptunov/chaosbank_android/core/exercises/Exercise.kt).
  The catalog walks `DefectId.entries`, pulls title/category/feature/`violates` from the
  registry, merges per-defect guidance, and assigns a stable id `AND-<CATCODE>-NN`
  (per-category counter).
- **Export & drift-guard:** the `CatalogJsonTest` unit test regenerates `exercises.json`
  when run with `-DupdateExercises=1`, and otherwise **fails if the committed file drifts**
  from the catalog. So the file is guaranteed to match the code.
- **Validation:** [`exercises.schema.json`](../exercises.schema.json) +
  [`scripts/check_exercises.py`](../scripts/check_exercises.py) validate structure and
  enforce **cross-platform parity** — the set of defect names here must equal the set in
  ChaosBank-iOS. This runs first in CI.

---

## 6. Determinism & seeding

- **`SeededRng`** (SplitMix64) drives the price walk and race-condition coin flips, so a
  given seed reproduces the same run. The build badge always shows the active
  profile/seed on screen.
- **Money is `BigDecimal` (HALF_EVEN) everywhere.** The single exception is the
  `roundingDrift` defect, which deliberately routes one calculation through `Double`.
- **The one intentional non-determinism** is the opt-in **live** price feed
  (`CHAOSBANK_PRICE_SOURCE=live`), which fetches real quotes from the public Yahoo
  Finance endpoint. It is off by default so reference defects stay reproducible.

---

## 7. Backend & networking model

`MockBackend` guards its in-memory bank/broker state with a `Mutex` (serialized access).
`BackendScenario` seeds it; `NetworkCondition` (normal / offline / slow / flaky) models
reliability stressors. Networking defects (`retryDuplicate`, `slowResponseRace`,
`timeoutAsSuccess`, `staleOfflineBalance`, `offlineBannerMissing`) are injected inside
the backend/scenario layer, not in Composables.

---

## 8. Testing & coverage philosophy

- **Unit tests target the logic layer** (core, models, view models, backend) with JUnit4
  + `kotlinx-coroutines-test`. The canonical pattern: one assertion that **passes on
  `clean` and fails when the defect is active**.
- **Coverage gate:** JaCoCo (`jacocoCoverageVerification`) enforces a **LINE 90% floor**
  on the logic layer. Compose UI, Android-bound infra (Activity, SharedPreferences token
  store), the live network service, and UI timing/feed are excluded (see the
  `coverageExcludes` list in [`app/build.gradle.kts`](../app/build.gradle.kts)) — they
  belong to instrumented/reference suites. The logic layer sits at ~95%.

See [DEVELOPERS_GUIDE.md](DEVELOPERS_GUIDE.md#tests--coverage) for commands and the
exclusion-list rationale.
