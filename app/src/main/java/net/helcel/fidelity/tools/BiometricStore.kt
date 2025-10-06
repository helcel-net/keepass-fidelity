package net.helcel.fidelity.tools

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import javax.crypto.Cipher
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.FragmentActivity
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.utils.parseUri
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

val Context.securePrefs by preferencesDataStore("keepass_prefs")
object KeePassKeys {
    val DB_FILE_PATH = stringPreferencesKey("db_file_path")
    val PASSWORD = stringPreferencesKey("password_enc")
    val KEY_FILE_PATH = stringPreferencesKey("key_file_path")
    val IV = stringPreferencesKey("iv")
}

sealed class CredentialResult {
    data class Success(val db: Uri?, val password: String, val key: Uri?) : CredentialResult()
    object NoData : CredentialResult()
    object AuthFailed : CredentialResult()
}

private const val KEY_ALIAS = "keepass_bio_key"

fun getOrCreateBiometricKey(): SecretKey {
    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }
    val keyGenerator =
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
    val spec = KeyGenParameterSpec.Builder(
        KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    ).apply {
        setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        setUserAuthenticationRequired(true)
        setInvalidatedByBiometricEnrollment(true)
    }.build()

    keyGenerator.init(spec)
    return keyGenerator.generateKey()
}

fun getCipherForDecryption(key: SecretKey, iv: ByteArray?): Cipher {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    if(iv==null) cipher.init(Cipher.ENCRYPT_MODE, key)
    else cipher.init(Cipher.DECRYPT_MODE, key, javax.crypto.spec.GCMParameterSpec(128, iv))
    return cipher
}
object KeePassStore {
    suspend fun saveCredentials(
        context: Context, cred: CredentialResult.Success
    ): CredentialResult {
        val cipher = showBiometricPrompt(context as FragmentActivity, true)
            ?: return CredentialResult.AuthFailed
        val encPasswordB = cipher.doFinal(cred.password.toByteArray(Charsets.UTF_8))
        context.securePrefs.edit { prefs ->
            prefs[KeePassKeys.DB_FILE_PATH] = cred.db.toString()
            prefs[KeePassKeys.PASSWORD] = Base64.encodeToString(encPasswordB, Base64.DEFAULT)
            prefs[KeePassKeys.IV] = Base64.encodeToString(cipher.iv, Base64.DEFAULT)
            cred.key?.let { prefs[KeePassKeys.KEY_FILE_PATH] = it.toString() }
        }
        return cred
    }

    suspend fun hasCredentials(context: Context): Boolean {
        val prefs = context.securePrefs.data.first()
        return prefs[KeePassKeys.DB_FILE_PATH] != null &&
                prefs[KeePassKeys.PASSWORD] != null
    }

    fun packCredentials(dbFilePath:Uri?, password: String, keyFilePath: Uri?): CredentialResult.Success {
        return CredentialResult.Success(dbFilePath, password, keyFilePath)
    }

    suspend fun loadCredentials(context: Context): CredentialResult {
        val prefs = context.securePrefs.data.first { true }
        val dbFilePath = prefs[KeePassKeys.DB_FILE_PATH] ?: return CredentialResult.NoData
        val encryptedBase64 = prefs[KeePassKeys.PASSWORD] ?: return CredentialResult.NoData
        val keyFilePath = prefs[KeePassKeys.KEY_FILE_PATH]
        val cipher = showBiometricPrompt(context as FragmentActivity, false)
            ?: return CredentialResult.AuthFailed
        val decrypted = cipher.doFinal(Base64.decode(encryptedBase64, Base64.DEFAULT))
        return packCredentials(
            dbFilePath.parseUri(),
            String(decrypted, Charsets.UTF_8),
            keyFilePath?.parseUri()
        )
    }
}


@OptIn(ExperimentalCoroutinesApi::class)
suspend fun showBiometricPrompt(activity: FragmentActivity, enc: Boolean): Cipher? {
    val prefs = activity.securePrefs.data.first()
    return suspendCancellableCoroutine { cont ->
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { cont.resume(result.cryptoObject?.cipher) {} }
                override fun onAuthenticationError(code: Int, msg: CharSequence) { cont.resume(null) {} }
                override fun onAuthenticationFailed() { cont.resume(null) {} }
            }
        )
        val iv = if(enc) null else prefs[KeePassKeys.IV]?.let { Base64.decode(it, Base64.DEFAULT) }
        if (!enc && iv == null) { cont.resume(null) {} }
        val cipher = getCipherForDecryption(getOrCreateBiometricKey(), iv)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock KeePass")
            .setSubtitle("Authenticate to access your KeePass database")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }

}


fun retrieveResponseFromChallenge(
    hardwareKey: HardwareKey,
    seed: ByteArray?,
): ByteArray {
    val response: ByteArray = "".toByteArray()
    return response
}
