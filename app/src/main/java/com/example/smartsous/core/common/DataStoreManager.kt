package com.example.smartsous.core.common

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.smartsous.domain.model.NotificationPreference
import com.example.smartsous.domain.model.UserPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences>
        by preferencesDataStore(name = "smartsous_prefs")

@Singleton
class DataStoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_RECIPES_SEEDED = booleanPreferencesKey("recipes_seeded")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")

        val KEY_TARGET_CALORIES = intPreferencesKey("target_calories")
        val KEY_FAVORITE_CUISINES = stringSetPreferencesKey("favorite_cuisines")
        val KEY_DISLIKED_INGREDIENTS = stringSetPreferencesKey("disliked_ingredients")
        val KEY_ALLERGIES = stringSetPreferencesKey("allergies")
        val KEY_PREFER_LOW_FAT = booleanPreferencesKey("prefer_low_fat")
        val KEY_PREFER_HIGH_PROTEIN = booleanPreferencesKey("prefer_high_protein")
        val KEY_VEGETARIAN = booleanPreferencesKey("vegetarian")
        val KEY_MAX_COOKING_TIME = intPreferencesKey("max_cooking_time")
        val KEY_AI_API_KEY = androidx.datastore.preferences.core.stringPreferencesKey("ai_api_key")
        val KEY_AI_MODEL = androidx.datastore.preferences.core.stringPreferencesKey("ai_model")

        val KEY_EXPIRY_REMINDERS_ENABLED = booleanPreferencesKey("expiry_reminders_enabled")
        val KEY_MEAL_REMINDERS_ENABLED = booleanPreferencesKey("meal_reminders_enabled")
        val KEY_MEAL_REMINDER_HOUR = intPreferencesKey("meal_reminder_hour")
        val KEY_MEAL_REMINDER_MINUTE = intPreferencesKey("meal_reminder_minute")
    }

    suspend fun isRecipesSeeded(): Boolean =
        context.dataStore.data
            .map { it[KEY_RECIPES_SEEDED] ?: false }
            .first()

    suspend fun markRecipesSeeded() {
        context.dataStore.edit { prefs ->
            prefs[KEY_RECIPES_SEEDED] = true
        }
    }

    suspend fun resetRecipesSeededFlag() {
        context.dataStore.edit { prefs ->
            prefs[KEY_RECIPES_SEEDED] = false
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

    suspend fun reset() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    val userPreferencesFlow: Flow<UserPreference> = context.dataStore.data
        .safePrefs()
        .map { prefs -> prefs.toUserPreference() }

    suspend fun updateUserPreferences(update: (UserPreference) -> UserPreference) {
        context.dataStore.edit { prefs ->
            val updated = update(prefs.toUserPreference())

            prefs[KEY_TARGET_CALORIES] = updated.targetCaloriesPerMeal
            prefs[KEY_FAVORITE_CUISINES] = updated.favoriteCuisines.toSet()
            prefs[KEY_DISLIKED_INGREDIENTS] = updated.dislikedIngredients.toSet()
            prefs[KEY_ALLERGIES] = updated.allergies.toSet()
            prefs[KEY_PREFER_LOW_FAT] = updated.preferLowFat
            prefs[KEY_PREFER_HIGH_PROTEIN] = updated.preferHighProtein
            prefs[KEY_VEGETARIAN] = updated.vegetarian
            prefs[KEY_MAX_COOKING_TIME] = updated.maxCookingTimeMinutes
            prefs[KEY_AI_API_KEY] = updated.aiApiKey
            prefs[KEY_AI_MODEL] = updated.aiModel
        }
    }

    val notificationPreferenceFlow: Flow<NotificationPreference> = context.dataStore.data
        .safePrefs()
        .map { prefs -> prefs.toNotificationPreference() }

    suspend fun updateNotificationPreference(
        update: (NotificationPreference) -> NotificationPreference
    ) {
        context.dataStore.edit { prefs ->
            val updated = update(prefs.toNotificationPreference()).sanitized()

            prefs[KEY_EXPIRY_REMINDERS_ENABLED] = updated.expiryRemindersEnabled
            prefs[KEY_MEAL_REMINDERS_ENABLED] = updated.mealRemindersEnabled
            prefs[KEY_MEAL_REMINDER_HOUR] = updated.mealReminderHour
            prefs[KEY_MEAL_REMINDER_MINUTE] = updated.mealReminderMinute
        }
    }

    private fun Flow<Preferences>.safePrefs(): Flow<Preferences> =
        catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    private fun Preferences.toUserPreference(): UserPreference =
        UserPreference(
            targetCaloriesPerMeal = this[KEY_TARGET_CALORIES] ?: 400,
            favoriteCuisines = this[KEY_FAVORITE_CUISINES]?.toList() ?: emptyList(),
            dislikedIngredients = this[KEY_DISLIKED_INGREDIENTS]?.toList() ?: emptyList(),
            allergies = this[KEY_ALLERGIES]?.toList() ?: emptyList(),
            preferLowFat = this[KEY_PREFER_LOW_FAT] ?: false,
            preferHighProtein = this[KEY_PREFER_HIGH_PROTEIN] ?: false,
            vegetarian = this[KEY_VEGETARIAN] ?: false,
            maxCookingTimeMinutes = this[KEY_MAX_COOKING_TIME] ?: 60,
            aiApiKey = this[KEY_AI_API_KEY] ?: "",
            aiModel = this[KEY_AI_MODEL] ?: "llama-3.3-70b-versatile"
        )

    private fun Preferences.toNotificationPreference(): NotificationPreference =
        NotificationPreference(
            expiryRemindersEnabled = this[KEY_EXPIRY_REMINDERS_ENABLED] ?: true,
            mealRemindersEnabled = this[KEY_MEAL_REMINDERS_ENABLED] ?: true,
            mealReminderHour = this[KEY_MEAL_REMINDER_HOUR] ?: 7,
            mealReminderMinute = this[KEY_MEAL_REMINDER_MINUTE] ?: 30
        ).sanitized()

    private fun NotificationPreference.sanitized(): NotificationPreference =
        copy(
            mealReminderHour = mealReminderHour.coerceIn(0, 23),
            mealReminderMinute = mealReminderMinute.coerceIn(0, 59)
        )
}
