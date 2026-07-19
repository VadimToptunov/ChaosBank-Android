package com.vadimtoptunov.chaosbank_android.app

import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.support.CoroutineTest
import com.vadimtoptunov.chaosbank_android.support.configureDefects
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthBiometricTest : CoroutineTest() {

    @Test fun clean_biometricFromLoginDoesNotUnlock() {
        configureDefects()
        val auth = AuthFlow(startUnlocked = false)
        auth.unlockWithBiometrics()
        assertFalse(auth.isUnlocked)
    }

    @Test fun biometricUnlocksFromAnyStage_bypassesLadder() {
        configureDefects(DefectId.biometricUnlocksFromAnyStage)
        val auth = AuthFlow(startUnlocked = false)
        auth.unlockWithBiometrics()
        assertTrue(auth.isUnlocked)
    }
}
