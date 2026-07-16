package com.vadimtoptunov.chaosbank_android.core

import android.content.Context
import android.content.SharedPreferences
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects

/**
 * Where the session token lives. The correct path keeps it in a mock secure store
 * (in memory, standing in for the Android Keystore). The `tokenInUserDefaults`
 * defect also writes it to SharedPreferences, where it is trivially readable.
 */
object TokenStore {
    private const val KEY = "chaosbank.session.token"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("chaosbank", Context.MODE_PRIVATE)
    }

    var secureToken: String? = null
        private set

    fun saveSessionToken(token: String) {
        secureToken = token
        if (Defects.isActive(DefectId.tokenInUserDefaults)) {
            prefs.edit().putString(KEY, token).apply()
        } else {
            prefs.edit().remove(KEY).apply()
        }
    }

    fun clear() {
        secureToken = null
        if (::prefs.isInitialized) prefs.edit().remove(KEY).apply()
    }

    val isTokenInPrefs: Boolean
        get() = ::prefs.isInitialized && prefs.getString(KEY, null) != null

    val storageDescription: String
        get() = if (isTokenInPrefs) "SharedPreferences (INSECURE)" else "Keystore (secure)"
}
