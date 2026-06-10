package com.example.smartsous.core.common

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.smartsous.domain.model.UserPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import java.io.IOException
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
        val KEY_ONBOARDING_DONE   = booleanPreferencesKey("onboarding_done")
        
        // User Preferences Keys
        val KEY_TARGET_CALORIES = intPreferencesKey("target_calories")
        val KEY_FAVORITE_CUISINES = stringSetPreferencesKey("favorite_cuisines")
        val KEY_DISLIKED_INGREDIENTS = stringSetPreferencesKey("disliked_ingredients")
        val KEY_PREFER_LOW_FAT = booleanPreferencesKey("prefer_low_fat")
        val KEY_PREFER_HIGH_PROTEIN = booleanPreferencesKey("prefer_high_protein")
        val KEY_MAX_COOKING_TIME = intPreferencesKey("max_cooking_time")
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

    suspend fun isOnboardingDone(): Boolean =
        context.dataStore.data
            .map { it[KEY_ONBOARDING_DONE] ?: false }
            .first()

    suspend fun markOnboardingDone() {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_DONE] = true
        }
    }

    // Thêm vào DataStoreManager.kt
    suspend fun reset() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    // --- User Preferences Logic ---

    val userPreferencesFlow: Flow<UserPreference> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            UserPreference(
                targetCaloriesPerMeal = prefs[KEY_TARGET_CALORIES] ?: 400,
                favoriteCuisines = prefs[KEY_FAVORITE_CUISINES]?.toList() ?: emptyList(),
                dislikedIngredients = prefs[KEY_DISLIKED_INGREDIENTS]?.toList() ?: emptyList(),
                preferLowFat = prefs[KEY_PREFER_LOW_FAT] ?: false,
                preferHighProtein = prefs[KEY_PREFER_HIGH_PROTEIN] ?: false,
                maxCookingTimeMinutes = prefs[KEY_MAX_COOKING_TIME] ?: 60
            )
        }

    suspend fun updateUserPreferences(update: (UserPreference) -> UserPreference) {
        context.dataStore.edit { prefs ->
            // Lấy current state
            val current = UserPreference(
                targetCaloriesPerMeal = prefs[KEY_TARGET_CALORIES] ?: 400,
                favoriteCuisines = prefs[KEY_FAVORITE_CUISINES]?.toList() ?: emptyList(),
                dislikedIngredients = prefs[KEY_DISLIKED_INGREDIENTS]?.toList() ?: emptyList(),
                preferLowFat = prefs[KEY_PREFER_LOW_FAT] ?: false,
                preferHighProtein = prefs[KEY_PREFER_HIGH_PROTEIN] ?: false,
                maxCookingTimeMinutes = prefs[KEY_MAX_COOKING_TIME] ?: 60
            )
            // Lấy state mới từ hàm lamda truyền vào
            val updated = update(current)

            // Lưu lại vào DataStore
            prefs[KEY_TARGET_CALORIES] = updated.targetCaloriesPerMeal
            prefs[KEY_FAVORITE_CUISINES] = updated.favoriteCuisines.toSet()
            prefs[KEY_DISLIKED_INGREDIENTS] = updated.dislikedIngredients.toSet()
            prefs[KEY_PREFER_LOW_FAT] = updated.preferLowFat
            prefs[KEY_PREFER_HIGH_PROTEIN] = updated.preferHighProtein
            prefs[KEY_MAX_COOKING_TIME] = updated.maxCookingTimeMinutes
        }
    }
}