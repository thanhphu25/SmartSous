package com.example.smartsous.core.common

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Extension property — tạo DataStore instance gắn với Context
private val Context.dataStore: DataStore<Preferences>
        by preferencesDataStore(name = "smartsous_prefs")

@Singleton
class DataStoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_RECIPES_SEEDED = booleanPreferencesKey("recipes_seeded")
    }

    // Đọc flag đã seed chưa
    suspend fun isRecipesSeeded(): Boolean =
        context.dataStore.data
            .map { it[KEY_RECIPES_SEEDED] ?: false }
            .first()

    // Đánh dấu đã seed xong
    suspend fun markRecipesSeeded() {
        context.dataStore.edit { prefs ->
            prefs[KEY_RECIPES_SEEDED] = true
        }
    }
}