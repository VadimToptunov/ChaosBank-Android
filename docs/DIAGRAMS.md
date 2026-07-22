# ChaosBank-Android — UML Diagrams

Mermaid diagrams (render natively on GitHub). They complement
[ARCHITECTURE.md](ARCHITECTURE.md); the prose there is authoritative if the two ever
drift. This app is the 1:1 port of
[ChaosBank-iOS](https://github.com/VadimToptunov/ChaosBank-iOS), so these diagrams mirror
its structure with Kotlin/Compose types.

---

## 1. Package / layer diagram

The logic layer owns correctness; the UI layer only renders. Defects are injected in the
logic layer behind a single query surface.

```mermaid
flowchart TD
    subgraph AppShell["app/ (shell)"]
        MainActivity --> RootScreen
        RootScreen --> AuthFlow
        MainActivity --> ConfigResolver
        MainActivity -. intent .-> DeepLink
        Navigator
        LaunchOptions
    end

    subgraph Features["features/ (MVVM)"]
        Screen["*Screen (@Composable)"] --> ViewModel["*ViewModel (mutableStateOf)"]
    end

    subgraph Core["core/ (logic layer — correctness)"]
        Defects["Defects.isActive(DefectId.x)"]
        DefectsPkg["defects/ (Id, Registry, Profiles, ChaosConfig)"]
        Money["money/ (BigDecimal, FX, LocaleFormat, LoanCalc)"]
        Backend["backend/ (MockBackend + Mutex, Scenario, NetworkCondition)"]
        Feed["feed/ (seeded PriceFeed + live Yahoo)"]
        Exercises["exercises/ (catalog → exercises.json)"]
        A11y["A11y.kt (all testTags)"]
        RNG["SeededRng (SplitMix64)"]
    end

    Models[("models/ + SeedData")]

    ViewModel --> Backend
    ViewModel --> Defects
    ViewModel --> Money
    Screen --> A11y
    Defects --> DefectsPkg
    Backend --> Models
    Feed --> RNG
    DefectsPkg --> Exercises
    ConfigResolver --> DefectsPkg
    AppShell --> Features
    Features --> Core
```

---

## 2. Defect-system class diagram

How a defect is described, bundled, resolved, and surfaced to a guard.

```mermaid
classDiagram
    class Defects {
        <<object>>
        +config: ChaosConfig
        +configure(ChaosConfig)
        +isActive(DefectId) Boolean
        +seed: Int
    }
    class ChaosConfig {
        <<data class>>
        +seed: Int
        +activeDefects: Set~DefectId~
        +label: String
        +priceSource: PriceSourceKind
    }
    class ConfigResolver {
        <<object>>
        +bakedDefaultProfile: String?
        +resolve(profile, defects, seed, priceSource, baked) ChaosConfig
    }
    class DefectId {
        <<enum>>
        roundingDrift
        doubleCharge
        staleBalance
        … (119 entries)
        +from(String) DefectId?$
    }
    class DefectCategory {
        <<enum>>
        money, validation, localization
        state, concurrency, ui
        accessibility, security, network, performance
    }
    class Defect {
        <<data class>>
        +id: DefectId
        +title: String
        +category: DefectCategory
        +feature: String
        +violates: String
    }
    class DefectRegistry {
        <<object>>
        +defect(DefectId) Defect
        +defectsForSeed(Int) Set~DefectId~
    }
    class BugProfile {
        <<data class>>
        +id: String
        +seed: Int
        +defects: Set~DefectId~
    }
    class BugProfiles {
        <<object>>
        +profile(String) BugProfile?
    }
    class Exercise {
        <<data class>>
        +id: String
        +defects: List~String~
        +task, expectedClean, expectedBuggy
        +keyLocators: List~String~
    }
    class Exercises {
        <<object>>
        +all: List~Exercise~
        +toJson() String
    }

    Defects --> ChaosConfig
    ConfigResolver --> ChaosConfig
    ConfigResolver ..> BugProfiles : resolve()
    ChaosConfig --> DefectId
    Defect --> DefectId
    Defect --> DefectCategory
    DefectRegistry --> Defect
    BugProfile --> DefectId
    BugProfiles --> BugProfile
    Exercises --> Exercise
    Exercises ..> DefectRegistry : metadata
    Exercises ..> DefectId : entries
```

---

## 3. Launch — config resolution

`ConfigResolver.resolve()` runs once at launch (`MainActivity`). Precedence: explicit
defects → profile → seed → baked flavor default → clean.

```mermaid
sequenceDiagram
    participant OS as Android
    participant MA as MainActivity
    participant CR as ConfigResolver.resolve()
    participant BP as BugProfiles
    participant DR as DefectRegistry
    participant D as Defects

    OS->>MA: launch Intent (-e extras) + flavor BuildConfig
    MA->>CR: resolve(profile, defects, seed, baked)
    alt CHAOSBANK_DEFECTS=a,b,c
        CR->>CR: split → activeDefects (label "custom")
    else CHAOSBANK_PROFILE=id (or baked)
        CR->>BP: profile(id)
        BP-->>CR: BugProfile(defects, seed)
    else CHAOSBANK_SEED=n
        CR->>DR: defectsForSeed(n)
        DR-->>CR: Set~DefectId~
    else nothing
        CR->>CR: clean (empty set)
    end
    CR-->>MA: ChaosConfig(seed, activeDefects, label, priceSource)
    MA->>D: configure(config)
    Note over D: every guard now reads this config
```

---

## 4. A guarded defect at runtime (`doubleCharge`)

The reference path is the `else`; the defect is a small isolated override. Locators are
unchanged either way, so the same test finds the same elements.

```mermaid
sequenceDiagram
    actor Tester
    participant TS as TransferScreen
    participant VM as TransferViewModel
    participant D as Defects
    participant MB as MockBackend (Mutex)

    Tester->>TS: double-tap Confirm (transfer.confirmButton)
    TS->>VM: confirm()
    VM->>D: isActive(DefectId.doubleCharge)?
    alt clean (default / correct)
        D-->>VM: false
        VM->>MB: transfer(idempotencyKey = K)
        VM->>MB: transfer(idempotencyKey = K)
        MB-->>VM: one transaction (K deduped)
    else doubleCharge active
        D-->>VM: true
        VM->>MB: transfer(newKey1)
        VM->>MB: transfer(newKey2)
        MB-->>VM: two transactions (double charge)
    end
    VM-->>TS: update home.totalBalance
```

---

## 5. Auth ladder — state machine

The login → OTP → passcode → biometric ladder, plus background re-lock and idle timeout.
Several security defects short-circuit specific edges (annotated).

```mermaid
stateDiagram-v2
    [*] --> WebLogin
    WebLogin --> OTP : credentials (WebView)
    OTP --> Passcode : correct code
    OTP --> OTP : wrong code (lockout after 3)
    Passcode --> Unlocked : 6-digit passcode
    Unlocked --> Passcode : background / idle timeout
    Passcode --> Unlocked : biometric

    note right of Unlocked
        authBypass: skip re-lock on background
        sessionTimeoutDisabled: never idle-locks
        biometricUnlocksFromAnyStage: biometric from WebLogin
        deepLinkSkipsAuth: chaosbank:// bypasses the gate
    end note
```

---

## 6. Exercise catalog + cross-platform parity pipeline

One source of truth (`Exercise.kt`) → `exercises.json`, drift-guarded by a unit test and
parity-checked in CI against the iOS sibling.

```mermaid
flowchart LR
    DR[DefectRegistry] --> EX[Exercises.all]
    SPEC[Exercise.kt specs] --> EX
    IDS[DefectId.entries] --> EX
    EX -->|"-DupdateExercises=1"| JSON[(exercises.json)]
    EX -. CatalogJsonTest asserts no drift .-> JSON

    subgraph CI["CI (android.yml)"]
        JSON --> CHK[check_exercises.py]
        SCHEMA[(exercises.schema.json)] --> CHK
        SIB[("iOS exercises.json @ main")] -. defect-name set .-> CHK
        CHK -->|structure OK + parity OK| PASS([green])
        CHK -->|dup id / drift| FAIL([red])
    end
```
