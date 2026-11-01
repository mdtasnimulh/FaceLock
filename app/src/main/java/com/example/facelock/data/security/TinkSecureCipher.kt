package com.example.facelock.data.security

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AES-256-GCM through Tink. The keyset is kept in private SharedPreferences and is itself
 * encrypted with a master key that never leaves the Android Keystore.
 */
@Singleton
class TinkSecureCipher @Inject constructor(
    @ApplicationContext private val context: Context,
) : SecureCipher {

    // lazy: creating the keyset talks to the Keystore, so it happens on first use, off the main thread
    private val aead: Aead by lazy {
        AeadConfig.register()
        val keyset = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, KEYSET_PREFS)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
        keyset.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    override fun encrypt(plain: ByteArray, context: ByteArray): ByteArray = aead.encrypt(plain, context)

    override fun decrypt(encrypted: ByteArray, context: ByteArray): ByteArray = aead.decrypt(encrypted, context)

    private companion object {
        const val KEYSET_NAME = "face_lock_keyset"
        const val KEYSET_PREFS = "face_lock_keyset_prefs"
        const val MASTER_KEY_URI = "android-keystore://face_lock_master_key"
    }
}
