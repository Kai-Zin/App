package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mysales_prefs")

data class CloudSessionPrefs(
    val supabaseUrl: String = "",
    val supabaseKey: String = "",
    val accessToken: String = "",
    val refreshToken: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val lastSyncOkMs: Long = 0L
) {
    val isConfigured: Boolean get() = supabaseUrl.startsWith("https://") && supabaseKey.isNotBlank()
    val isSignedIn: Boolean get() = isConfigured && accessToken.isNotBlank() && userId.isNotBlank()
}

class AppPreferences(private val context: Context) {
    private object Keys {
        val SB_URL = stringPreferencesKey("sb_url")
        val SB_KEY = stringPreferencesKey("sb_key")
        val ACCESS_TOKEN = stringPreferencesKey("sb_access_token")
        val REFRESH_TOKEN = stringPreferencesKey("sb_refresh_token")
        val USER_ID = stringPreferencesKey("sb_user_id")
        val USER_EMAIL = stringPreferencesKey("sb_user_email")
        val LAST_OK = longPreferencesKey("sb_last_ok")
        val UI_SCALE = androidx.datastore.preferences.core.floatPreferencesKey("ui_scale")
    }

    val uiScaleFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        (prefs[Keys.UI_SCALE] ?: 0.90f).coerceIn(0.75f, 1.25f)
    }

    val cloudSessionFlow: Flow<CloudSessionPrefs> = context.dataStore.data.map { prefs ->
        CloudSessionPrefs(
            supabaseUrl = prefs[Keys.SB_URL] ?: "",
            supabaseKey = prefs[Keys.SB_KEY] ?: "",
            accessToken = prefs[Keys.ACCESS_TOKEN] ?: "",
            refreshToken = prefs[Keys.REFRESH_TOKEN] ?: "",
            userId = prefs[Keys.USER_ID] ?: "",
            userEmail = prefs[Keys.USER_EMAIL] ?: "",
            lastSyncOkMs = prefs[Keys.LAST_OK] ?: 0L
        )
    }

    suspend fun currentCloudSession(): CloudSessionPrefs = cloudSessionFlow.first()

    suspend fun saveUiScale(scale: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.UI_SCALE] = scale.coerceIn(0.75f, 1.25f)
        }
    }

    suspend fun saveConfig(url: String, key: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SB_URL] = url.trim().trimEnd('/')
            prefs[Keys.SB_KEY] = key.trim()
            prefs.remove(Keys.ACCESS_TOKEN)
            prefs.remove(Keys.REFRESH_TOKEN)
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.USER_EMAIL)
        }
    }

    suspend fun saveAuthSession(
        accessToken: String,
        refreshToken: String,
        userId: String,
        userEmail: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = accessToken
            prefs[Keys.REFRESH_TOKEN] = refreshToken
            prefs[Keys.USER_ID] = userId
            prefs[Keys.USER_EMAIL] = userEmail
        }
    }

    suspend fun clearAuthSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.ACCESS_TOKEN)
            prefs.remove(Keys.REFRESH_TOKEN)
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.USER_EMAIL)
        }
    }

    suspend fun markLastSyncOk(timestampMs: Long = System.currentTimeMillis()) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_OK] = timestampMs
        }
    }
}
