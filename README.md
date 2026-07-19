# ChaosBank (Android)

> A deliberately-buggy Android neobank **+** broker, built as a controlled practice
> range for mobile QA / SDET automation.

ChaosBank looks and behaves like a real fintech product (a Revolut/Robinhood-style
hybrid of a bank and a stock/crypto broker), but it exists for one purpose: to give
test-automation engineers a realistic app to **write UI tests against** — with
known, switchable defects planted across every layer: UI, state, validation,
localization, concurrency, networking, security, accessibility and performance.

This is the **1:1 Android port** of [ChaosBank-iOS](https://github.com/VadimToptunov/ChaosBank-iOS)
— same design, same features, the same defects with identical ids and behaviour,
built with Kotlin + Jetpack Compose. The build additionally carries the first
**reliability-stressor** surfaces — a dev-menu network-state selector
(normal / offline / slow / flaky), unstable animations, and never-ending pagination —
from the shared [`ROADMAP.md`](ROADMAP.md).

![CI](https://github.com/VadimToptunov/ChaosBank-Android/actions/workflows/android.yml/badge.svg)
![minSdk 29](https://img.shields.io/badge/Android-10%2B%20(API%2029)-black)
![Kotlin 2.2](https://img.shields.io/badge/Kotlin-2.2-purple)
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue)
![Logic coverage](https://img.shields.io/badge/logic%20coverage-95%25-brightgreen)
![Dependencies](https://img.shields.io/badge/app%20dependencies-none-brightgreen)

---

## The core idea: correct code and bugs are separated

This is the most important rule in the project.

**Production logic is always written correctly.** Money math, order pricing,
balance updates — the reference implementation is the correct one. Defects are
never baked into the core; they are injected at explicit, guarded points behind a
single query:

```kotlin
if (Defects.isActive(DefectId.doubleCharge)) {
    // small, isolated buggy override
}
```

Why this matters:

- **Regression training works.** Write a test against the clean baseline (it
  passes), flip a profile that activates the defect, re-run (it fails). The test
  proved it catches the regression.
- **The clean build is a real, correct app.** Profile `clean` = zero defects.
- **Locators never move.** A defect changes *behaviour or values*, never the
  `Modifier.testTag` surface, so tests stay stable across builds. Every
  identifier lives in one file (`core/A11y.kt`).
- **Determinism.** All randomness (the price walk, race-condition coin flips)
  derives from the build seed, so runs reproduce — except the opt-in **live**
  price feed, which is intentionally non-deterministic.

---

## Bug configuration engine

Bugs are enabled by **configuration**, not by separate builds. A build activates a
set of defects via a named **profile**, a numeric **seed**, or an explicit list —
chosen at launch or switched live in the in-app **developer menu** (long-press the
build badge).

### Profiles

| Profile | Contents |
|---|---|
| `clean` | no defects (passes the full reference suite) |
| `ui`, `validation`, `accessibility`, `state`, `localization`, `security`, `network` | all defects in that category |
| `flaky` | concurrency / race-condition defects (seed-pinned) |
| `beginner`, `middle`, `senior` | curated difficulty bundles |
| `all` | every defect at once |

### Launch arguments

Passed as Intent string extras (`adb shell am start … -e KEY value`):

```bash
# Profiles / seeds / explicit defects
-e CHAOSBANK_PROFILE flaky
-e CHAOSBANK_SEED 7
-e CHAOSBANK_DEFECTS doubleCharge,roundingDrift

# Real vs simulated market data
-e CHAOSBANK_PRICE_SOURCE live       # or 'simulated' (default)

# Test / demo affordances (never change product behaviour)
-e CHAOSBANK_START_UNLOCKED 1        # skip the auth ladder
-e CHAOSBANK_TAB markets             # deep-link a tab: home|markets|portfolio|card
-e CHAOSBANK_SHOW_DEV 1              # auto-open the developer menu
-e CHAOSBANK_SHOW_WEB_LOGIN 1        # auto-open the web login
```

```bash
adb shell am start -n com.vadimtoptunov.chaosbank_android/.MainActivity \
  -e CHAOSBANK_PROFILE flaky -e CHAOSBANK_START_UNLOCKED 1
```

### Product flavors (distributable per-defect builds)

Launch arguments configure a build at *run time* (great for dev and CI). To ship a
**fixed, standalone build that already contains a bug set** — the way you'd ship
prod/debug/dev — use a Gradle **product flavor** (the Android analogue of iOS build
configurations), not a separate module:

- One module, one codebase. The `standard` flavor stays `clean`.
- A flavor (e.g. **flaky**) sets a distinct `applicationId` suffix
  (`…chaosbank_android.flaky`), a display name (**ChaosBank Flaky**), and a
  `BuildConfig.CHAOSBANK_BAKED_PROFILE` field.
- `ConfigResolver.bakedDefaultProfile` reads that field in **one place** and becomes
  the lowest-precedence default (launch args still override it).

```bash
# Build the flaky build — it defaults to the flaky profile on device
./gradlew :app:assembleFlakyDebug
```

Flavors ship for `standard`, `flaky`, `security`, `senior`, and `everything`;
adding another baked build is mechanical — one entry in the `chaosFlavors` list in
`app/build.gradle.kts`.

---

## Defect catalog (119 defects, 10 categories)

Every defect ships **OFF** in the `clean` profile. The **complete, machine-readable
list** is in [`exercises.json`](exercises.json) (one exercise per defect); the table
below is a representative selection.

| Category | Defect | What it breaks |
|---|---|---|
| **Money** | `roundingDrift` | stored amount drifts from displayed (Double vs BigDecimal) |
| | `loanAprUnderstated` ⭑ | loan payment uses a higher rate than the advertised APR |
| | `pnlSign` | a loss renders as a gain |
| | `exchangeFeeNotApplied` | credited amount ignores the displayed fee |
| | `homeTotalOmitsAccount` | total balance omits the GBP account |
| **Validation** | `limitValidation` | zero/negative qty & bad limit orders accepted |
| | `zeroAmountAccepted` | zero-amount transfer allowed |
| | `whitespaceRecipient` | recipient whitespace not trimmed |
| | `amountExceedsBalanceAllowed` | transfer over the balance allowed client-side |
| **Localization** | `localeParse` | `1,000.50` parsed as `1.0005` |
| | `dateTimezoneShift` | transaction dates shifted by timezone |
| | `rtlBreaksLayout` ⭑ | a transaction row does not mirror under RTL |
| | `numberGroupingIgnoresLocale` ⭑ | grouping/decimal separators ignore the selected locale |
| | `currencySymbolPlacementIgnoresLocale` ⭑ | currency symbol placed before the amount in every locale |
| **State** | `staleBalance` | dashboard shows the pre-transfer balance |
| | `paginationDup` | a transaction duplicated after Load more |
| | `paginationNeverEnds` ⭑ | "Load more" never terminates → scroll-to-end loops forever |
| | `cardToggleInvert` | freeze toggle reads back inverted |
| | `orderStuckPending` | a filled order still shows pending |
| | `notificationBadgeStale` ⭑ | unread badge stays after reading notifications |
| | `notificationOpensWrongScreen` ⭑ | tapping a notification opens the wrong screen |
| | `templatePrefillsWrongAmount` ⭑ | a saved payment template prefills the wrong amount |
| **Concurrency** | `doubleCharge` | rapid double-tap sends the transfer twice |
| | `livePriceRace` | order price ≠ the tapped price |
| | `orderDoubleSubmit` | rapid double-tap places two orders |
| | `syncLostUpdate` ⭑ | concurrent +1s race on a shared counter → updates lost |
| **UI** | `disabledButtonTappable` | a disabled-looking button still fires |
| | `successToastMissing` | no confirmation toast after a transfer |
| **Accessibility** | `duplicateAssetA11yId` | two market rows share one identifier |
| | `wrongA11yLabel` | Buy button is labelled "Sell" |
| **Security** | `authBypass` | gate skipped after backgrounding |
| | `noPrivacyBlur` | sensitive data visible in the app switcher |
| | `tokenInUserDefaults` | session token stored in SharedPreferences, not the Keystore |
| | `cardCvvVisible` | CVV shown on the card face |
| | `kycBypassAllowsTransfer` ⭑ | unverified KYC still sends transfers over €1,000 |
| | `virtualCardShowsRealPan` ⭑ | a virtual card leaks the real card number |
| | `deepLinkSkipsAuth` ⭑ | `chaosbank://` deep link opens a screen without the auth gate |
| | `biometricUnlocksFromAnyStage` ⭑ | biometrics skip login/OTP/passcode from a fresh launch |
| **Networking** | `retryDuplicate` | retry after a slow response double-posts |
| | `slowResponseRace` | a stale late response clobbers fresh state |
| | `timeoutAsSuccess` | a timeout is shown as a successful transfer |
| **Performance** | `transactionsHeavyList` | huge non-lazy, non-paginated list hitches |
| | `mainThreadStall` | Portfolio blocks the main thread on open |
| | `feedPollsTooOften` | live feed polls 10× too often |
| **Reliability** ⭑ | `flakyAnimation` | ticker flash settle-time jitters → wait-for-idle flakes |
| | `offlineBannerMissing` | offline, but no banner — cached data served silently |

---

## Machine-readable exercise catalog

Every defect has a matching **exercise**, exported to [`exercises.json`](exercises.json)
(source of truth: `core/exercises/Exercise.kt`). Each entry is a self-contained task
with everything a tester needs:

```json
{
  "id": "AND-CON-01",
  "title": "Rapid double-tap sends twice",
  "difficulty": "senior",
  "category": "concurrency",
  "feature": "Transfer",
  "defects": ["doubleCharge"],
  "launchArgument": "CHAOSBANK_DEFECTS=doubleCharge",
  "expectedClean": "Idempotent — one transaction.",
  "expectedBuggy": "Two transactions (double charge).",
  "task": "Rapidly double-tap Confirm; assert exactly one transaction / one debit.",
  "keyLocators": ["transfer.confirmButton", "home.totalBalance"]
}
```

The in-app **Developer → Exercises** browser lists them by difficulty and can apply
any exercise's defect(s) live.

---

## Features

- **Bank** — Home dashboard (currency-switchable balance), Transfer (+ confirmation
  sheet, idempotency, retry), Exchange (live FX, fees), Transactions (search /
  filter / pagination).
- **Broker** — Markets (live ticking prices, sparklines), Asset detail, Order
  ticket (market / limit lifecycle), Portfolio (live P&L, allocation).
- **Card** — freeze / online-payments toggles, limits, PIN.
- **Auth ladder** — **web login** (a `WebView` — deliberately reached via a web
  context, not native locators) → **OTP** (resend cooldown, expiry, auto-submit,
  lockout) → **6-digit passcode** → **biometric** fallback, plus background re-lock
  and idle session timeout.
- **Live market data** — real quotes from the public Yahoo Finance endpoint
  (no API key). Off by default so the reference defects stay reproducible.

---

## Money & determinism

- `BigDecimal` (HALF_EVEN) everywhere for money — the single exception is the
  `roundingDrift` defect, which deliberately routes one calculation through `Double`.
- Seeded RNG (`SeededRng`, SplitMix64) drives the price walk and race probabilities.
- The build badge always shows the real active profile/seed on screen.

---

## Project structure

```
app/src/main/java/com/vadimtoptunov/chaosbank_android/
├── MainActivity.kt        entry: resolves config, wires services, hosts RootScreen
├── app/                   AppServices, AuthFlow, ConfigResolver, LaunchOptions, Navigator
├── core/
│   ├── A11y.kt            ALL test tags (single source)
│   ├── defects/           DefectId, Defect, categories, registry, profiles, config
│   ├── money/             BigDecimal money, currency, FX, locale-aware parsing
│   ├── feed/              seeded PriceFeed + live Yahoo source + MarketStore
│   ├── backend/           in-memory MockBackend (Mutex) + network scenarios
│   ├── exercises/         machine-readable exercise catalog
│   └── TokenStore.kt      session-token storage (Keystore vs SharedPreferences defect)
├── models/                Account, Transaction, Asset, Quote, Holding, Order, seed data
├── features/              Home, Transfer, Exchange, Transactions, Markets, AssetDetail,
│                          Order, Portfolio, Card, Dev
└── ui/                    theme tokens, components (live ticker, sparkline…), RootScreen
exercises.json             generated machine-readable catalog
```

No third-party dependencies in the app (only AndroidX / Compose / Kotlin coroutines).

---

## Build & run

Requires JDK 21 and the Android SDK (compileSdk 37, minSdk 29).

```bash
./gradlew :app:assembleStandardDebug
./gradlew :app:installStandardDebug   # to a connected device / emulator
```

Run a specific scenario:

```bash
adb shell am start -n com.vadimtoptunov.chaosbank_android/.MainActivity \
  -e CHAOSBANK_PROFILE flaky
```

---

## Tests

Host JVM unit tests (JUnit4 + `kotlinx-coroutines-test`) cover the correct baseline
and the regression pattern — the same assertion passes on `clean` and fails when a
defect is active. **168 tests** across the catalog (integrity, profiles, exercises,
`exercises.json` drift-guard), money & rounding, locale parsing, the mock backend &
every network scenario/error path (including offline mode), the seeded price feed,
launch-option/navigation parsing, and every view model (Home, Transfer, Exchange,
Transactions, Order, Portfolio, Card).

```bash
./gradlew :app:testStandardDebugUnitTest

# …or the coverage gate (fails under the logic-layer threshold):
./gradlew :app:jacocoCoverageVerification
```

**Coverage.** The logic layer (core, models, view models, backend) is at **95%**
line coverage, enforced by `jacocoCoverageVerification` (floor 90%) and CI. Compose
`@Composable` UI, the Android-bound infra (Activity, SharedPreferences token store),
the live network service and UI timing/feed are excluded from the unit-coverage
budget — they belong to instrumented / reference suites (see Roadmap and
[`ROADMAP.md`](ROADMAP.md)).

## Roadmap

See [`ROADMAP.md`](ROADMAP.md). Highlights: parallel reference test suites
(Espresso+Compose / Appium / Maestro) for head-to-head comparison, plus expansion
surfaces (unstable animations, infinite lists, deep links, push, offline/network
states, RTL) layered in lockstep with the iOS app.

## License

[MIT](LICENSE) © 2026 Vadim Toptunov.
