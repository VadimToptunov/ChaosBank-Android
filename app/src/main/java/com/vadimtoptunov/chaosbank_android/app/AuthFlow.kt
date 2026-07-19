package com.vadimtoptunov.chaosbank_android.app

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vadimtoptunov.chaosbank_android.core.TokenStore
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AuthStage { login, otp, passcodeSetup, passcodeEntry, unlocked }

/**
 * The full mock auth ladder: login → OTP → passcode setup → unlocked, with fast
 * passcode/biometric re-entry after background or idle timeout. Every step carries
 * tester-hostile mechanics and hosts several guarded defects.
 */
class AuthFlow(startUnlocked: Boolean = false) {

    val mockOtp = "424242"
    private val otpValiditySec = 20L
    private val resendCooldownSeconds = 30
    private val maxOtpAttempts = 3
    val passcodeLength = 6
    private val sessionTimeoutSec = 90L

    var stage by mutableStateOf(AuthStage.login); private set

    // Login
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var loginError by mutableStateOf<String?>(null)

    // OTP
    var otpEntry by mutableStateOf("")
    var otpError by mutableStateOf<String?>(null)
    var otpAttempts by mutableStateOf(0); private set
    var resendCooldown by mutableStateOf(0); private set
    var otpSecondsLeft by mutableStateOf(0); private set
    private var otpGeneratedAt: Long? = null
    private var otpLocked = false

    // Passcode
    var storedPasscode by mutableStateOf<String?>(null); private set
    var passcodeEntry by mutableStateOf("")
    var passcodeError by mutableStateOf<String?>(null)

    private var lastActiveAt = now()
    private val scope = CoroutineScope(Dispatchers.Main)
    private var ticker: Job? = null

    init {
        if (startUnlocked) {
            storedPasscode = "000000"
            stage = AuthStage.unlocked
            startTicker()
        }
    }

    val isUnlocked: Boolean get() = stage == AuthStage.unlocked

    val requiredPasscodeLength: Int
        get() = if (active(DefectId.passcodeWeakAccepted)) 4 else passcodeLength

    // Login ------------------------------------------------------------------

    fun submitLogin() {
        if (!active(DefectId.loginAcceptsEmptyCreds)) {
            if (username.trim().isEmpty() || password.isEmpty()) {
                loginError = "Enter a username and password"; return
            }
        }
        if (active(DefectId.credentialsInLog)) Log.d("ChaosBank", "login user=$username pass=$password")
        loginError = null
        startOtp()
    }

    fun completeWebLogin(username: String, password: String) {
        this.username = username; this.password = password; submitLogin()
    }

    // OTP --------------------------------------------------------------------

    private fun startOtp() {
        otpEntry = if (active(DefectId.otpAutoFillsCode)) mockOtp else ""
        otpError = null; otpAttempts = 0; otpLocked = false
        otpGeneratedAt = now()
        resendCooldown = resendCooldownSeconds
        otpSecondsLeft = otpValiditySec.toInt()
        if (active(DefectId.otpCodeInLog)) Log.d("ChaosBank", "OTP code: $mockOtp")
        stage = AuthStage.otp
        startTicker()
    }

    private val otpExpired: Boolean
        get() = otpGeneratedAt?.let { now() - it > otpValiditySec } ?: true

    fun verifyOtp() {
        if (otpLocked && !active(DefectId.otpNoLockout)) {
            otpError = "Too many attempts — locked. Resend a new code."; return
        }
        if (otpExpired && !active(DefectId.otpAcceptsExpired)) {
            otpError = "Code expired — request a new one."; return
        }
        if (otpEntry != mockOtp && !active(DefectId.otpAcceptsAnyCode)) {
            otpAttempts += 1
            if (otpAttempts >= maxOtpAttempts) otpLocked = true
            otpError = "Incorrect code ($otpAttempts/$maxOtpAttempts)"; return
        }
        otpError = null
        stopTicker()
        stage = if (storedPasscode == null) AuthStage.passcodeSetup else AuthStage.unlocked
        if (stage == AuthStage.unlocked) markUnlocked()
    }

    fun resendOtp() {
        if (resendCooldown > 0 && !active(DefectId.otpResendNoCooldown)) return
        otpGeneratedAt = now()
        otpEntry = ""; otpError = null; otpAttempts = 0; otpLocked = false
        resendCooldown = resendCooldownSeconds
        otpSecondsLeft = otpValiditySec.toInt()
    }

    val canResend: Boolean get() = resendCooldown == 0 || active(DefectId.otpResendNoCooldown)

    // Passcode ---------------------------------------------------------------

    fun setPasscode() {
        if (passcodeEntry.length < requiredPasscodeLength) {
            passcodeError = "Passcode must be $requiredPasscodeLength digits"; return
        }
        storedPasscode = passcodeEntry.take(requiredPasscodeLength)
        if (active(DefectId.passcodeStoredPlaintext)) {
            // Deliberately insecure: mirror the token defect via the same prefs.
            TokenStore.saveSessionToken("passcode:${storedPasscode}")
        }
        passcodeEntry = ""; passcodeError = null
        markUnlocked(); stage = AuthStage.unlocked
    }

    fun submitPasscode() {
        if (passcodeEntry != storedPasscode && !active(DefectId.passcodeAnyAccepted)) {
            passcodeError = "Wrong passcode"; passcodeEntry = ""; return
        }
        passcodeError = null; passcodeEntry = ""
        markUnlocked(); stage = AuthStage.unlocked
    }

    fun unlockWithBiometrics() {
        // Biometrics are a fast RE-ENTRY only (after a session exists). From a fresh
        // login they must not bypass the ladder — unless `biometricUnlocksFromAnyStage`.
        if (stage != AuthStage.passcodeEntry && !Defects.isActive(DefectId.biometricUnlocksFromAnyStage)) return
        markUnlocked(); stage = AuthStage.unlocked
    }

    // Session / lifecycle ----------------------------------------------------

    fun keepAlive() { lastActiveAt = now() }

    private fun markUnlocked() {
        lastActiveAt = now()
        runCatching { TokenStore.saveSessionToken("sess-${username.ifEmpty { "user" }}-${now()}") }
        startTicker()
    }

    fun handleBackground() {
        if (stage != AuthStage.unlocked) return
        if (active(DefectId.authBypass)) return
        stage = if (storedPasscode == null) AuthStage.login else AuthStage.passcodeEntry
    }

    private fun checkIdleTimeout() {
        if (stage != AuthStage.unlocked) return
        if (active(DefectId.sessionTimeoutDisabled)) return
        if (now() - lastActiveAt > sessionTimeoutSec) {
            stage = if (storedPasscode == null) AuthStage.login else AuthStage.passcodeEntry
        }
    }

    private fun startTicker() {
        if (ticker != null) return
        ticker = scope.launch {
            while (true) {
                delay(1000)
                if (resendCooldown > 0) resendCooldown -= 1
                if (otpSecondsLeft > 0) otpSecondsLeft -= 1
                checkIdleTimeout()
            }
        }
    }

    private fun stopTicker() { ticker?.cancel(); ticker = null }

    private fun now(): Long = System.currentTimeMillis() / 1000
    private fun active(id: DefectId) = Defects.isActive(id)
}
