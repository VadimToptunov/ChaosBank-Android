# Contributing to ChaosBank-Android

Thanks for improving the fixture. ChaosBank exists to train and benchmark mobile test
automation, so the bar is: **the clean build stays correct, defects stay isolated and
switchable, and the two platforms stay identical.**

Read [docs/DEVELOPERS_GUIDE.md](docs/DEVELOPERS_GUIDE.md) for the build/test workflow and
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the design.

---

## The 1:1 parity contract

ChaosBank-Android and [ChaosBank-iOS](https://github.com/VadimToptunov/ChaosBank-iOS) are
**strict mirrors**. Any change to the defect surface must land on **both** apps in
lockstep:

- **Same `DefectId` names**, same behavior, same category, same difficulty.
- **Same exercise** content (only the id prefix `AND-`/`IOS-` and the `launchArgument`
  syntax differ; Android emits `"profile": null`, iOS omits the key).
- **Same locator names** in `A11y` (the `testTag`/accessibility-identifier string is
  identical across platforms).

CI enforces the defect-name half of this automatically via
[`scripts/check_exercises.py`](scripts/check_exercises.py), which compares this repo's
`exercises.json` against the iOS repo's `main`. A drift fails the build.

---

## Golden rules

1. **Production code is correct.** Never bake a bug into the reference path. Inject it
   behind `if (Defects.isActive(DefectId.x)) { … }`, with the correct implementation as
   the `else`.
2. **Locators never move.** New test tags only in `core/A11y.kt`; a defect changes
   behavior/values, never a tag.
3. **Don't hand-edit `exercises.json`.** Regenerate it
   (`./gradlew :app:testStandardDebugUnitTest -DupdateExercises=1`) — ids are per-category
   and hand-editing causes duplicates. The `CatalogJsonTest` drift-guard fails CI if the
   file and catalog disagree.
4. **Smallest possible change.** No unrelated refactors, no fat comment blocks.
5. **Determinism.** Randomness derives from the seed via `SeededRng`.

---

## Adding a defect

Follow the full recipe in
[docs/DEVELOPERS_GUIDE.md → Adding a new defect](docs/DEVELOPERS_GUIDE.md#4-adding-a-new-defect-the-full-recipe),
then mirror it on iOS in the same PR.

---

## Before you push — checklist

- [ ] `./gradlew :app:jacocoCoverageVerification` is green locally (logic-layer floor).
- [ ] `python3 scripts/check_exercises.py exercises.json AND` passes (structure).
- [ ] `exercises.json` was **regenerated**, not hand-edited (drift-guard green).
- [ ] The matching iOS change is in the same change set (parity).
- [ ] New locators live only in `A11y.kt`.
- [ ] A clean-pass + defect-fail test pair exists for any new defect.
- [ ] README defect table updated if the defect is noteworthy.

---

## Pull requests

- Branch naming: `IssueNumber/ShortFeature`.
- Keep PRs code + tests + docs only.
- Describe the defect, the guard's location, and the clean vs buggy outcome.

## Commit messages

Conventional prefixes (`feat:`, `fix:`, `ci:`, `docs:`, `test:`). Explain the *why* of a
defect (what requirement it violates), not just the *what*.
