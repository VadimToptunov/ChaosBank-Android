# ChaosBank-Android — User Guide (for QA / SDET engineers)

This guide is for the person **using ChaosBank to practise or benchmark test
automation** — not for someone modifying the app (that's the
[Developer's Guide](DEVELOPERS_GUIDE.md)). It explains how to launch the app, turn
specific defects on and off, read the exercise catalog, and write a stable test that
proves it catches a regression.

---

## 1. The mental model

ChaosBank is a fully-working neobank + broker with a library of **named, switchable
defects** planted across every layer (UI, state, validation, localization, concurrency,
networking, security, accessibility, performance).

- **`clean` = a correct app.** With no defects active, everything behaves per spec and
  the full reference test suite passes.
- **A defect is a switch.** Turning one on changes *behavior or values* on a specific
  screen — but **never** the `Modifier.testTag` your tests locate. So a test you write
  against `clean` keeps finding its elements when a defect is active; it just starts
  *failing on the assertion*, which is exactly what a good regression test should do.
- **119 defects, each with an exercise.** Every defect has a matching, self-contained
  task in [`exercises.json`](../exercises.json).

This app is byte-for-behavior identical to
[ChaosBank-iOS](https://github.com/VadimToptunov/ChaosBank-iOS): same defect names, same
locator strings — so a cross-platform test suite can share expectations.

---

## 2. Launching the app in a chosen state

There are three ways to control which defects are active — pick per situation.

### A. Intent extras (best for CI and ad-hoc runs)

Pass string extras via `adb`:

```bash
# Turn on exactly the defects you want (highest precedence)
adb shell am start -n com.vadimtoptunov.chaosbank_android/.MainActivity \
  -e CHAOSBANK_DEFECTS doubleCharge,roundingDrift

# …or a whole profile
adb shell am start -n com.vadimtoptunov.chaosbank_android/.MainActivity \
  -e CHAOSBANK_PROFILE security

# …or a numeric seed (maps to a defect bundle)
adb shell am start -n com.vadimtoptunov.chaosbank_android/.MainActivity \
  -e CHAOSBANK_SEED 7
```

Precedence (highest first): explicit `CHAOSBANK_DEFECTS` → `CHAOSBANK_PROFILE` →
`CHAOSBANK_SEED` → the profile baked into the flavor → `clean`.

**Profiles:** `clean`; the category profiles `ui`, `validation`, `accessibility`,
`state`, `localization`, `security`, `network`; `flaky` (concurrency/races, seed-pinned);
difficulty bundles `beginner`, `middle`, `senior`; and `all`.

**Market data:** `CHAOSBANK_PRICE_SOURCE=live` uses real Yahoo Finance quotes (no key);
default `simulated` keeps runs reproducible.

**Test/demo affordances** (these never change product behavior — they just position you):

```bash
-e CHAOSBANK_START_UNLOCKED 1     # skip the auth ladder
-e CHAOSBANK_TAB markets          # start on a tab: home|markets|portfolio|card
-e CHAOSBANK_SHOW_DEV 1           # auto-open the developer menu
-e CHAOSBANK_SHOW_WEB_LOGIN 1     # auto-open the web login
```

### B. In-app developer menu (best for exploring interactively)

**Long-press the build badge** (it always shows the active profile/seed). From there you
can switch profile/seed live, toggle individual defects, open the **Exercises** browser,
switch the network condition (normal/offline/slow/flaky), toggle RTL/locale, and flip
KYC — no reinstall.

### C. Baked flavors (best for a fixed, shareable per-defect app)

Install a flavor such as **flaky** to get a standalone APK that defaults to that profile
and installs alongside the clean build with its own `applicationId` suffix and label
(**ChaosBank Flaky**):

```bash
./gradlew :app:installFlakyDebug
```

See [DEVELOPERS_GUIDE.md](DEVELOPERS_GUIDE.md#product-flavors-distributable-per-defect-builds).

---

## 3. The exercise catalog — your task list

[`exercises.json`](../exercises.json) has one entry per defect. Each is a complete brief:

```json
{
  "id": "AND-CON-01",
  "title": "Rapid double-tap sends twice",
  "difficulty": "senior",
  "category": "concurrency",
  "feature": "Transfer",
  "defects": ["doubleCharge"],
  "launchArgument": "CHAOSBANK_DEFECTS=doubleCharge",
  "condition": "Idempotency of a submit action",
  "expectedClean": "Idempotent — one transaction.",
  "expectedBuggy": "Two transactions (double charge).",
  "task": "Rapidly double-tap Confirm; assert exactly one transaction / one debit.",
  "keyLocators": ["transfer.confirmButton", "home.totalBalance"]
}
```

| Field | Use it for |
|---|---|
| `launchArgument` | The extra to activate exactly this defect (pass as `-e CHAOSBANK_DEFECTS doubleCharge`). |
| `expectedClean` / `expectedBuggy` | The assertion boundary — clean must pass, buggy must fail. |
| `task` | What to automate. |
| `keyLocators` | The `testTag`s to target (stable across clean/buggy). |
| `difficulty` | `junior` / `middle` / `senior` — pick your level. |

The in-app **Developer → Exercises** browser lists the same catalog by difficulty and can
apply any exercise's defect(s) live.

---

## 4. Writing your first regression test (worked example)

Take exercise `AND-STA-…` `staleBalance` ("dashboard shows the pre-transfer balance").

1. **Establish the clean baseline.** Launch with no defects, note the Home total
   (`home.totalBalance`), make a transfer, return Home — the balance should decrease.
   Write that as your assertion. It **passes**.
2. **Activate the defect.** Relaunch with `-e CHAOSBANK_DEFECTS staleBalance` (from the
   exercise's `launchArgument`).
3. **Re-run the same test.** The `testTag`s still resolve; the assertion now **fails**
   because Home keeps the old balance. Your test just proved it catches the regression.
4. **Keep it stable.** Target only the `keyLocators`; don't assert on text a defect might
   legitimately change elsewhere.

This clean-pass → buggy-fail loop is the whole point of the app; repeat it per exercise.

---

## 5. Feature tour (what you can automate)

- **Bank** — Home dashboard (currency-switchable balance, notifications bell), Transfer
  (confirmation sheet, idempotency, retry, saved templates, KYC gate), Exchange (live FX,
  fees), Transactions (search / filter / pagination).
- **Broker** — Markets (live ticking prices, sparklines), Asset detail, Order ticket
  (market/limit lifecycle), Portfolio (live P&L, allocation).
- **Card** — freeze / online-payments toggles, limits, PIN, virtual card.
- **Loans** — a loan calculator (advertised vs effective APR).
- **Auth ladder** — **web login** (a `WebView`, reached via a web context, not native
  locators) → **OTP** (resend cooldown, expiry, auto-submit, lockout) → **6-digit
  passcode** → **biometric** fallback, plus background re-lock and idle session timeout.
- **Sync playground** (dev menu) — a concurrent-increment counter for race exercises.

---

## 6. Guarantees you can rely on

- **Stable locators.** Every identifier is a constant in `core/A11y.kt`; defects never
  move them.
- **Determinism.** With the default `simulated` price source, a given seed reproduces the
  same run (price walk, race coin-flips). Only `live` mode is intentionally
  non-deterministic.
- **Correct baseline.** `clean` passes the full reference suite; any failure there is a
  real bug, not a planted one.

---

## 7. FAQ

**Q: A `testTag` disappeared when I turned on a defect.**
It shouldn't — defects change behavior/values, not tags. Check you're using the tag from
`keyLocators`, and file it if a tag truly moved (that would be a defect in the fixture
itself).

**Q: My "flaky" test is flaky even on clean.**
Concurrency/timing exercises (`flakyAnimation`, races) are `senior` for a reason — prefer
Compose's idling/synchronization over `Thread.sleep`. The `flaky` profile is seed-pinned
so the *app* is reproducible; your waits must be robust.

**Q: Where's the authoritative defect list?**
[`exercises.json`](../exercises.json) (generated from the in-app catalog and drift-guarded
by a unit test). The README has a curated highlights table.
