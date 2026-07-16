# ChaosBank — Roadmap

ChaosBank is a deliberately-buggy neobank + broker used as a proving ground for mobile
test automation. The core principle never changes:

> **Behaviour changes, locators do not.** Every defect is a small, guarded override
> behind `Defects.isActive(_)`. Accessibility identifiers / `testTag`s are stable so a
> test suite can be measured on how well it survives *behavioural* churn — not on how
> well it chases moved locators.

This file tracks planned expansion. It is intentionally ambitious: the app is meant to
grow large enough to be a realistic banking surface, not a toy. Both platforms
(ChaosBank-iOS and ChaosBank-Android) should stay at **1:1 parity** — each item lands on
both, with the same defects, ids and behaviour.

## Status

- **Baseline (in progress):** 1:1 port of the current iOS app to Android
  (Kotlin + Jetpack Compose). Finish this before layering the clusters below.
- Everything under "Planned clusters" is **captured, not started.**

## Guiding idea (why this exists)

The open question this project probes: *what should the next stage of mobile automation
look like?* The bet is that it is less about new locator DSLs and more about:

- **Resilience to UI change** — semantic / intent-based locators over brittle trees.
- **Self-stabilising waits** — zero `sleep`; the harness converges on state.
- **Readability & maintenance cost** as first-class metrics (LOC, diff churn per UI change).

ChaosBank is the fixture: because defects move *behaviour* and keep *locators* fixed, you
can honestly compare strategies (XCUITest vs KassiOS vs Espresso/Compose vs Appium vs
Maestro) on the same screens. A dedicated "locator-stress" defect family will *also* move
the tree on purpose, to show where naive locators break and semantic ones don't.

## Planned clusters (prioritised)

### 1. Reliability stressors  ★ priority
Surfaces that stress a framework's synchronisation / flakiness handling.
- Unstable animations (transitions/`LiveTicker` that complete early or never settle) →
  `flakyAnimation` defect family.
- Infinite / paginated lists (Transactions) with jitter → extends `transactionsHeavyList`.
- Offline mode + explicit network-state selector in the dev menu:
  online / offline / slow / flaky / 401 / 403 / 409 / 500 / malformed-JSON / out-of-order.
- Race screens (concurrent refresh clobbering, double-submit) → Concurrency category.

### 2. Platform integrations  ★ priority
- Deep links / App Links → `deepLinkSkipsAuth` (route that bypasses the auth gate).
- Push notifications → Notifications screen + local/stub push; defects like
  "push opens wrong screen", "stale badge count".
- Real biometric (`BiometricPrompt` / `LocalAuthentication`) alongside the current mock.
- Complex navigation (nested stacks, tabs-within-tabs, modal-over-modal).

### 3. Localization & RTL  ★ priority
- Multiple locales; RTL (Arabic / Hebrew).
- Extends existing `localeParse` / `dateTimezoneShift`; adds `rtlBreaksLayout`,
  number/date/currency formatting defects per locale.

### 4. Banking breadth  ★ priority
New screens (some full, some stubs — even stubs add navigation/variety):
- Virtual card issuance, card block/freeze, limit changes.
- Payment templates / saved payees.
- Investments (extends trading), loans.
- KYC flow, 2FA (extends OTP), transaction-history filters.
- Notifications centre.
Each new screen ships with 2–4 guarded defects and stable ids.

### 5. Reference test suites (the actual deliverable)
Parallel implementations of the same scenarios so they can be compared head-to-head:
XCUITest / KassiOS / Swift Testing (iOS); Espresso+Compose / Appium / Maestro (Android).
Plus a comparison doc: LOC, readability, locator-stability after a UI change, maintenance
diff size. This is the article/talk material.

## Non-negotiables (apply to every item)

- Correct code is the default; a defect is a small override behind `Defects.isActive`.
- Never change an accessibility id / `testTag` to express a defect.
- Determinism: all randomness flows through the seeded RNG (SplitMix64).
- Keep iOS and Android in lockstep — same defect ids, same behaviour, same ids.
