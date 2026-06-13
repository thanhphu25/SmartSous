package com.example.smartsous.feature.settings

import android.app.Application
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.smartsous.core.common.BaseViewModel
import com.example.smartsous.core.common.DataStoreManager
import com.example.smartsous.core.notification.ExpiryCheckWorker
import com.example.smartsous.core.notification.MealReminderWorker
import com.example.smartsous.core.notification.WorkerScheduler
import com.example.smartsous.data.local.dao.MealPlanDao
import com.example.smartsous.data.local.entity.MealPlanEntity
import com.example.smartsous.domain.model.Ingredient
import com.example.smartsous.domain.model.IngredientCategory
import com.example.smartsous.domain.model.NotificationPreference
import com.example.smartsous.domain.model.UserPreference
import com.example.smartsous.domain.repository.IMealPlanRepository
import com.example.smartsous.domain.repository.IPantryRepository
import com.example.smartsous.domain.repository.IRecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val profileName: String = "SmartSous user",
    val syncStatus: String = "Đồng bộ sẵn sàng",
    val preferences: UserPreference = UserPreference(),
    val notifications: NotificationPreference = NotificationPreference(),
    val totalIngredients: Int = 0,
    val expiringCount: Int = 0,
    val favoriteRecipeCount: Int = 0,
    val weeklyMealPlanCount: Int = 0,
    val isTestingExpiry: Boolean = false,
    val isTestingMeal: Boolean = false,
    val workerStatuses: Map<String, String> = emptyMap()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val application: Application,
    private val dataStoreManager: DataStoreManager,
    private val pantryRepository: IPantryRepository,
    private val recipeRepository: IRecipeRepository,
    private val mealPlanRepository: IMealPlanRepository,
    private val mealPlanDao: MealPlanDao,
    private val workerScheduler: WorkerScheduler
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val workManager = WorkManager.getInstance(application)
    private val expiryStatusLiveData =
        workManager.getWorkInfosForUniqueWorkLiveData(ExpiryCheckWorker.WORK_NAME)
    private val mealStatusLiveData =
        workManager.getWorkInfosForUniqueWorkLiveData(MealReminderWorker.WORK_NAME)
    private val expiryStatusObserver = Observer<List<WorkInfo>> { workInfos ->
        val status = workInfos?.firstOrNull()?.state?.name ?: "NOT_SCHEDULED"
        _uiState.update { state ->
            state.copy(workerStatuses = state.workerStatuses + ("ExpiryCheck" to status))
        }
    }
    private val mealStatusObserver = Observer<List<WorkInfo>> { workInfos ->
        val status = workInfos?.firstOrNull()?.state?.name ?: "NOT_SCHEDULED"
        _uiState.update { state ->
            state.copy(workerStatuses = state.workerStatuses + ("MealReminder" to status))
        }
    }

    init {
        observeUserPreferences()
        observeNotificationPreferences()
        observePantryStats()
        observeFavoriteStats()
        observeMealPlanStats()
        observeWorkerStatus()
    }

    private fun observeUserPreferences() {
        dataStoreManager.userPreferencesFlow
            .onEach { preferences ->
                _uiState.update { state -> state.copy(preferences = preferences) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeNotificationPreferences() {
        dataStoreManager.notificationPreferenceFlow
            .onEach { notifications ->
                _uiState.update { state -> state.copy(notifications = notifications) }
            }
            .launchIn(viewModelScope)
    }

    private fun observePantryStats() {
        pantryRepository.getAllIngredients()
            .onEach { ingredients ->
                val expiring = ingredients.count { ingredient ->
                    ingredient.expiryDate?.let { date ->
                        val days = java.time.temporal.ChronoUnit.DAYS
                            .between(LocalDate.now(), date).toInt()
                        days in 0..3
                    } ?: false
                }
                _uiState.update { state ->
                    state.copy(
                        totalIngredients = ingredients.size,
                        expiringCount = expiring
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeFavoriteStats() {
        recipeRepository.getFavorites()
            .onEach { favorites ->
                _uiState.update { state ->
                    state.copy(favoriteRecipeCount = favorites.size)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeMealPlanStats() {
        mealPlanRepository.getMealPlanForWeek(startOfWeek(LocalDate.now()))
            .onEach { plans ->
                val count = plans.sumOf { plan ->
                    plan.meals.values.sumOf { recipeIds -> recipeIds.size }
                }
                _uiState.update { state ->
                    state.copy(weeklyMealPlanCount = count)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeWorkerStatus() {
        expiryStatusLiveData.observeForever(expiryStatusObserver)
        mealStatusLiveData.observeForever(mealStatusObserver)
    }

    fun toggleFavoriteCuisine(cuisine: String) {
        updatePreferences {
            it.copy(favoriteCuisines = it.favoriteCuisines.toggleValue(cuisine))
        }
    }

    fun toggleAllergy(allergy: String) {
        updatePreferences {
            it.copy(allergies = it.allergies.toggleValue(allergy))
        }
    }

    fun toggleDislikedIngredient(ingredient: String) {
        updatePreferences {
            it.copy(dislikedIngredients = it.dislikedIngredients.toggleValue(ingredient))
        }
    }

    fun setLowFat(enabled: Boolean) {
        updatePreferences { it.copy(preferLowFat = enabled) }
    }

    fun setHighProtein(enabled: Boolean) {
        updatePreferences { it.copy(preferHighProtein = enabled) }
    }

    fun setVegetarian(enabled: Boolean) {
        updatePreferences { it.copy(vegetarian = enabled) }
    }

    fun setTargetCalories(value: Int) {
        updatePreferences { it.copy(targetCaloriesPerMeal = value.coerceIn(200, 1000)) }
    }

    fun setMaxCookingTime(value: Int) {
        updatePreferences { it.copy(maxCookingTimeMinutes = value.coerceIn(10, 180)) }
    }

    fun setAiApiKey(key: String) {
        updatePreferences { it.copy(aiApiKey = key) }
    }

    fun setAiModel(model: String) {
        updatePreferences { it.copy(aiModel = model) }
    }

    fun setExpiryRemindersEnabled(enabled: Boolean) {
        safeLaunch {
            dataStoreManager.updateNotificationPreference {
                it.copy(expiryRemindersEnabled = enabled)
            }
            if (enabled) {
                workerScheduler.scheduleExpiryCheck()
            } else {
                workerScheduler.cancelExpiryCheck()
            }
        }
    }

    fun setMealRemindersEnabled(enabled: Boolean) {
        safeLaunch {
            val current = _uiState.value.notifications
            dataStoreManager.updateNotificationPreference {
                it.copy(mealRemindersEnabled = enabled)
            }
            if (enabled) {
                workerScheduler.scheduleMealReminder(
                    hour = current.mealReminderHour,
                    minute = current.mealReminderMinute
                )
            } else {
                workerScheduler.cancelMealReminder()
            }
        }
    }

    fun setMealReminderHour(hour: Int) {
        updateMealReminderTime(
            hour = hour.floorMod(24),
            minute = _uiState.value.notifications.mealReminderMinute
        )
    }

    fun setMealReminderMinute(minute: Int) {
        updateMealReminderTime(
            hour = _uiState.value.notifications.mealReminderHour,
            minute = minute.floorMod(60)
        )
    }

    fun testExpiryNotification() {
        _uiState.update { it.copy(isTestingExpiry = true) }

        val request = OneTimeWorkRequestBuilder<ExpiryCheckWorker>()
            .addTag("test_expiry")
            .build()

        workManager.enqueue(request)
        val liveData = workManager.getWorkInfoByIdLiveData(request.id)
        lateinit var observer: Observer<WorkInfo>
        observer = Observer { info ->
                if (info?.state?.isFinished == true) {
                    _uiState.update { it.copy(isTestingExpiry = false) }
                    liveData.removeObserver(observer)
                }
            }
        liveData.observeForever(observer)
    }

    fun testMealReminder() {
        _uiState.update { it.copy(isTestingMeal = true) }

        safeLaunch {
            insertTestMealPlanForToday()

            val request = OneTimeWorkRequestBuilder<MealReminderWorker>()
                .addTag("test_meal")
                .build()

            workManager.enqueue(request)
            val liveData = workManager.getWorkInfoByIdLiveData(request.id)
            lateinit var observer: Observer<WorkInfo>
            observer = Observer { info ->
                    if (info?.state?.isFinished == true) {
                        _uiState.update { it.copy(isTestingMeal = false) }
                        liveData.removeObserver(observer)
                    }
                }
            liveData.observeForever(observer)
        }
    }

    fun addTestExpiringIngredient() {
        safeLaunch {
            pantryRepository.upsert(
                Ingredient(
                    id = UUID.randomUUID().toString(),
                    name = "Sữa tươi (test)",
                    quantity = 1.0,
                    unit = "hộp",
                    category = IngredientCategory.DAIRY,
                    expiryDate = LocalDate.now().plusDays(1),
                    addedDate = LocalDate.now()
                )
            )
        }
    }

    fun addTestMealPlanForToday() {
        safeLaunch {
            insertTestMealPlanForToday()
        }
    }

    fun resetSeedFlag() {
        safeLaunch {
            dataStoreManager.resetRecipesSeededFlag()
        }
    }

    private fun updatePreferences(update: (UserPreference) -> UserPreference) {
        safeLaunch {
            dataStoreManager.updateUserPreferences(update)
        }
    }

    private fun updateMealReminderTime(hour: Int, minute: Int) {
        safeLaunch {
            dataStoreManager.updateNotificationPreference {
                it.copy(mealReminderHour = hour, mealReminderMinute = minute)
            }
            if (_uiState.value.notifications.mealRemindersEnabled) {
                workerScheduler.scheduleMealReminder(hour = hour, minute = minute)
            }
        }
    }

    private suspend fun insertTestMealPlanForToday() {
        val today = LocalDate.now().toString()
        mealPlanDao.upsert(
            MealPlanEntity(
                date = today,
                mealType = "BREAKFAST",
                recipeIdsJson = """["vn004"]"""
            )
        )
        mealPlanDao.upsert(
            MealPlanEntity(
                date = today,
                mealType = "DINNER",
                recipeIdsJson = """["vn002","vn012"]"""
            )
        )
    }

    private fun startOfWeek(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    private fun List<String>.toggleValue(value: String): List<String> =
        if (contains(value)) filterNot { it == value } else this + value

    private fun Int.floorMod(mod: Int): Int =
        ((this % mod) + mod) % mod

    override fun onCleared() {
        expiryStatusLiveData.removeObserver(expiryStatusObserver)
        mealStatusLiveData.removeObserver(mealStatusObserver)
        super.onCleared()
    }
}
