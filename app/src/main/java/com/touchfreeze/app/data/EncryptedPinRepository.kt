package com.touchfreeze.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Repository for PIN storage using EncryptedSharedPreferences.
 *
 * Security features:
 * - PIN hashed with SHA-256 before storage
 * - EncryptedSharedPreferences using Android Keystore master key
 * - Hardware-backed encryption on supported devices
 * - Random salt per installation
 */
@Singleton
class EncryptedPinRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        const val PIN_KEY = "pin_hash"
        const val SALT_KEY = "pin_salt"
        private const val MIN_PIN_LENGTH = 4
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Check if a PIN is set.
     */
    suspend fun isPinSet(): Boolean {
        return encryptedPrefs.contains(PIN_KEY)
    }

    /**
     * Set a new PIN. Generates a random salt and stores the hashed PIN.
     */
    suspend fun setPin(rawPin: String): Result<Unit> {
        return runCatching {
            if (rawPin.length < MIN_PIN_LENGTH) {
                throw IllegalArgumentException("PIN must be at least $MIN_PIN_LENGTH digits")
            }

            val salt = Random.nextBytes(32).encodeToHexString()
            val hashedPin = hashPinWithSalt(rawPin, salt)

            with(encryptedPrefs.edit()) {
                putString(PIN_KEY, hashedPin)
                putString(SALT_KEY, salt)
                apply()
            }
        }
    }

    /**
     * Verify a PIN against the stored hash.
     */
    suspend fun verifyPin(rawPin: String): Boolean {
        val storedHash = encryptedPrefs.getString(PIN_KEY, null) ?: return false
        val salt = encryptedPrefs.getString(SALT_KEY, null) ?: return false

        val computedHash = hashPinWithSalt(rawPin, salt)
        return computedHash == storedHash
    }

    /**
     * Clear the stored PIN.
     */
    suspend fun clearPin(): Result<Unit> {
        return runCatching {
            with(encryptedPrefs.edit()) {
                remove(PIN_KEY)
                remove(SALT_KEY)
                apply()
            }
        }
    }

    /**
     * Change PIN (requires old PIN verification first).
     */
    suspend fun changePin(oldPin: String, newPin: String): Result<Unit> {
        if (!verifyPin(oldPin)) {
            return Result.failure(Exception("Old PIN verification failed"))
        }
        return setPin(newPin)
    }

    /**
     * Hash a PIN with salt using SHA-256.
     */
    private fun hashPinWithSalt(rawPin: String, salt: String): String {
        val saltedPin = rawPin + salt
        val bytes = saltedPin.toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.encodeToHexString()
    }

    /**
     * Extension function to convert ByteArray to hex string.
     */
    private fun ByteArray.encodeToHexString(): String {
        return joinToString("") { "%02x".format(it) }
    }
}