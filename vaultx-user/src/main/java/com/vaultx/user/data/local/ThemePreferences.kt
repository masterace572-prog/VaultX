package com.vaultx.user.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class ThemePreferences(private val context: Context) {
    companion object {
        val THEME_KEY = booleanPreferencesKey("is_dark_mode")
    }

    val isDarkMode: Flow<Boolean?> = context.dataStore.data.map { preferences ->
        preferences[THEME_KEY]
    }

    suspend fun saveTheme(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = isDark
        }
    }
}
