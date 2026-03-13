package com.college.sportsmeet.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Singleton object for managing JWT Access Tokens securely using AndroidX Security Crypto.
 * Never use standard SharedPreferences for storing sensitive tokens, as they are easily
 * accessible on rooted devices or via adb backup.
 */
object TokenManager {

    private const val PREFS_FILENAME = "secure_token_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"

    private lateinit var sharedPreferences: SharedPreferences
    private var isInitialized = false

    /**
     * Initializes the TokenManager with application context.
     * Must be called exactly once from the custom Application class.
     *
     * @param context Application context
     */
    fun init(context: Context) {
        if (isInitialized) return

        // Create a MasterKey for encryption/decryption using AES256-GCM
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        // Initialize EncryptedSharedPreferences
        // Keys are encrypted with Deterministic AEAD
        // Values are encrypted with AES256-GCM
        sharedPreferences = EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_FILENAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        isInitialized = true
    }

    /**
     * Securely stores the JWT Access Token.
     *
     * @param token The JWT string to store
     */
    fun saveAccessToken(token: String) {
        checkInitialization()
        sharedPreferences.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .apply()
    }

    /**
     * Retrieves the stored JWT Access Token.
     *
     * @return The token string if it exists, otherwise null
     */
    fun getAccessToken(): String? {
        checkInitialization()
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }

    /**
     * Clears the stored JWT Access Token securely (e.g., on logout).
     */
    fun clearAccessToken() {
        checkInitialization()
        sharedPreferences.edit()
            .remove(KEY_ACCESS_TOKEN)
            .apply()
    }

    /**
     * Internal helper to verify the TokenManager was properly initialized.
     */
    private fun checkInitialization() {
        check(isInitialized) {
            "TokenManager must be initialized before use. Call TokenManager.init(context) from your Application class."
        }
    }
}