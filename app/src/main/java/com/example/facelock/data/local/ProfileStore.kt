package com.example.facelock.data.local

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.facelock.data.security.SecureCipher
import com.example.facelock.domain.model.DisplayName
import com.example.facelock.domain.model.FaceEmbedding
import com.example.facelock.domain.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore(name = "face_lock_profile")

/**
 * The only place that knows how the profile is stored: JSON -> Tink AES-GCM -> Base64 -> DataStore.
 * Nothing readable (not even the name) is ever written to disk.
 */
@Singleton
class ProfileStore @Inject constructor(
    @ApplicationContext context: Context,
    private val cipher: SecureCipher,
) {
    private val store = context.profileDataStore

    fun observe(): Flow<UserProfile?> = store.data
        .map { prefs -> prefs[KEY_PROFILE]?.let(::decodeOrNull) }
        .flowOn(Dispatchers.Default)

    suspend fun current(): UserProfile? = observe().first()

    suspend fun save(profile: UserProfile) = withContext(Dispatchers.Default) {
        val encrypted = cipher.encrypt(encode(profile), ASSOCIATED_DATA)
        val text = Base64.encodeToString(encrypted, Base64.NO_WRAP)
        store.edit { it[KEY_PROFILE] = text }
    }

    suspend fun clear() {
        store.edit { it.remove(KEY_PROFILE) }
    }

    private fun encode(profile: UserProfile): ByteArray {
        val embedding = JSONArray()
        profile.faceTemplate.toFloatArray().forEach { embedding.put(it.toDouble()) }
        return JSONObject()
            .put("name", profile.displayName.value)
            .put("registeredAt", profile.registeredAtMillis)
            .put("embedding", embedding)
            .toString()
            .toByteArray(Charsets.UTF_8)
    }

    /** A blob that cannot be decrypted (key lost, data tampered) is treated as "not registered". */
    private fun decodeOrNull(text: String): UserProfile? = try {
        val plain = cipher.decrypt(Base64.decode(text, Base64.NO_WRAP), ASSOCIATED_DATA)
        val json = JSONObject(String(plain, Charsets.UTF_8))
        val array = json.getJSONArray("embedding")
        val values = FloatArray(array.length()) { array.getDouble(it).toFloat() }
        UserProfile(
            displayName = DisplayName.restore(json.getString("name")),
            faceTemplate = FaceEmbedding.fromRaw(values),
            registeredAtMillis = json.getLong("registeredAt"),
        )
    } catch (e: Exception) {
        Log.w(TAG, "Stored profile could not be read; treating the user as not registered", e)
        null
    }

    private companion object {
        const val TAG = "ProfileStore"
        val KEY_PROFILE = stringPreferencesKey("profile_blob")
        val ASSOCIATED_DATA = "face-lock/profile/v1".toByteArray(Charsets.UTF_8)
    }
}
