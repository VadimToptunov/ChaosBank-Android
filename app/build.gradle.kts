plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    jacoco
}

android {
    namespace = "com.vadimtoptunov.chaosbank_android"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.vadimtoptunov.chaosbank_android"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // The "views build": render ported screens with the XML/View system
        // (RecyclerView) instead of Compose. Off by default; enabled at runtime via
        // the CHAOSBANK_VIEWS intent extra, or baked true by a distributable flavor.
        buildConfigField("boolean", "CHAOSBANK_VIEWS_BUILD", "false")
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            optimization {
                enable = false
            }
        }
    }

    // Per-defect distributable builds (Android analogue of iOS Build Configurations).
    // Each flavor bakes a bug profile in via BuildConfig + a distinct id and display name.
    flavorDimensions += "profile"
    // One baked build per bug profile — kept at parity with the iOS build
    // configurations (BuildConfig.bakedDefaultProfile). "everything" bakes the "all"
    // profile; "standard" is clean. Every id here must exist in BugProfiles.
    val chaosFlavors = listOf(
        // name, display label, baked profile id ("" = clean/standard)
        Triple("standard", "ChaosBank", ""),
        Triple("ui", "ChaosBank UI", "ui"),
        Triple("validation", "ChaosBank Validation", "validation"),
        Triple("accessibility", "ChaosBank A11y", "accessibility"),
        Triple("state", "ChaosBank State", "state"),
        Triple("localization", "ChaosBank L10n", "localization"),
        Triple("security", "ChaosBank Security", "security"),
        Triple("network", "ChaosBank Network", "network"),
        Triple("flaky", "ChaosBank Flaky", "flaky"),
        Triple("beginner", "ChaosBank Beginner", "beginner"),
        Triple("middle", "ChaosBank Middle", "middle"),
        Triple("senior", "ChaosBank Senior", "senior"),
        Triple("everything", "ChaosBank Chaos", "all"),
    )
    productFlavors {
        chaosFlavors.forEach { (flavorName, label, profile) ->
            create(flavorName) {
                dimension = "profile"
                if (flavorName != "standard") applicationIdSuffix = ".$flavorName"
                resValue("string", "app_name", label)
                buildConfigField("String", "CHAOSBANK_BAKED_PROFILE", "\"$profile\"")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // XML "views build": RecyclerView for the View-system rendering of ported screens.
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// Coverage is measured on the LOGIC layer only. Compose UI (@Composable), the
// Android-bound infra (Activity, SharedPreferences token store), the live network
// service and UI timing/feed are excluded — they are exercised by instrumented/manual
// tests, not host JVM unit tests.
// Classes NOT unit-testable on the host JVM (Compose UI, Android-bound infra, live
// network, UI timing) are excluded so the gate measures the LOGIC layer only.
val coverageExcludes = listOf(
    "**/ui/**",
    "**/MainActivity*",
    "**/*ComposableSingletons*",
    "**/BuildConfig*",
    "**/core/TokenStore*",
    "**/core/A11y*",
    "**/core/feed/LivePriceService*",
    "**/core/feed/MarketStore*",
    "**/app/AuthFlow*",
    "**/app/AppServices*",
    // Compose screens live under features/*; their view models do not.
    "**/features/**/*Screen*",
    "**/features/**/*ScreenKt*",
    // XML "views build" rendering (RecyclerView adapter/holder) — view layer.
    "**/features/**/ViewsTransactions*",
    "**/features/**/*Adapter*",
    "**/features/**/*ViewHolder*",
    "**/features/dev/**",
    "**/features/home/HomeScreenKt*",
)

fun classDirs() = fileTree(layout.buildDirectory.dir("intermediates/built_in_kotlinc/standardDebug/compileStandardDebugKotlin/classes")) {
    exclude(coverageExcludes)
}

val coverageSources = files("src/main/java")
val coverageExec = layout.buildDirectory.file("outputs/unit_test_code_coverage/standardDebugUnitTest/testStandardDebugUnitTest.exec")

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testStandardDebugUnitTest")
    classDirectories.setFrom(classDirs())
    sourceDirectories.setFrom(coverageSources)
    executionData.setFrom(coverageExec)
    reports {
        html.required.set(true)
        xml.required.set(true)
    }
}

tasks.register<JacocoCoverageVerification>("jacocoCoverageVerification") {
    dependsOn("testStandardDebugUnitTest")
    classDirectories.setFrom(classDirs())
    sourceDirectories.setFrom(coverageSources)
    executionData.setFrom(coverageExec)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.90".toBigDecimal()
            }
        }
    }
}

tasks.named("check") { dependsOn("jacocoCoverageVerification") }

// Let `-DupdateExercises=1` reach the test JVM so the catalog drift-guard can rewrite
// exercises.json (source of truth: core/exercises/Exercise.kt).
tasks.withType<Test>().configureEach {
    systemProperty("updateExercises", providers.systemProperty("updateExercises").getOrElse(""))
}
