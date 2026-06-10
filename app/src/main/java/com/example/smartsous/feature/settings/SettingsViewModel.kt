package com.example.smartsous.feature.settings

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.smartsous.core.common.BaseViewModel
import com.example.smartsous.core.notification.ExpiryCheckWorker
import com.example.smartsous.core.notification.MealReminderWorker
import com.example.smartsous.data.local.dao.MealPlanDao
import com.example.smartsous.data.local.entity.MealPlanEntity
import com.example.smartsous.domain.model.Ingredient
import com.example.smartsous.domain.model.IngredientCategory
import com.example.smartsous.domain.repository.IPantryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class SettingsUiState(
    val totalIngredients: Int = 0,
    val expiringCount: Int = 0,
    val isTestingExpiry: Boolean = false,
    val isTestingMeal: Boolean = false,
    val workerStatuses: Map<String, String> = emptyMap()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val application: Application,
    private val pantryRepository: IPantryRepository,
    private val mealPlanDao: MealPlanDao
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val workManager = WorkManager.getInstance(application)

    init {
        observePantry()
        observeWorkerStatus()
    }

    private fun observePantry() {
        pantryRepository.getAllIngredients()
            .onEach { ingredients ->
                val expiring = ingredients.count { ing ->
                    ing.expiryDate?.let { date ->
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

    // Theo dõi trạng thái các Workers
    private fun observeWorkerStatus() {
        workManager
            .getWorkInfosForUniqueWorkLiveData(ExpiryCheckWorker.WORK_NAME)
            .observeForever { workInfos ->
                val status = workInfos?.firstOrNull()?.state?.name ?: "NOT_SCHEDULED"
                _uiState.update { state ->
                    state.copy(
                        workerStatuses = state.workerStatuses +
                                ("ExpiryCheck" to status)
                    )
                }
            }

        workManager
            .getWorkInfosForUniqueWorkLiveData(MealReminderWorker.WORK_NAME)
            .observeForever { workInfos ->
                val status = workInfos?.firstOrNull()?.state?.name ?: "NOT_SCHEDULED"
                _uiState.update { state ->
                    state.copy(
                        workerStatuses = state.workerStatuses +
                                ("MealReminder" to status)
                    )
                }
            }
    }

    // Trigger ExpiryCheckWorker chạy ngay — OneTimeWorkRequest
    fun testExpiryNotification() {
        _uiState.update { it.copy(isTestingExpiry = true) }

        val request = OneTimeWorkRequestBuilder<ExpiryCheckWorker>()
            .addTag("test_expiry")
            .build()

        workManager.enqueue(request)

        // Monitor kết quả
        workManager.getWorkInfoByIdLiveData(request.id)
            .observeForever { info ->
                if (info?.state?.isFinished == true) {
                    _uiState.update { it.copy(isTestingExpiry = false) }
                }
            }
    }

    // Trigger MealReminderWorker chạy ngay
    fun testMealReminder() {
        _uiState.update { it.copy(isTestingMeal = true) }

        safeLaunch {
            insertTestMealPlanForToday()

            val request = OneTimeWorkRequestBuilder<MealReminderWorker>()
                .addTag("test_meal")
                .build()

            workManager.enqueue(request)

            workManager.getWorkInfoByIdLiveData(request.id)
                .observeForever { info ->
                    if (info?.state?.isFinished == true) {
                        _uiState.update { it.copy(isTestingMeal = false) }
                    }
                }
        }
    }

    // Thêm nguyên liệu test với expiry ngày mai
    fun addTestExpiringIngredient() {
        safeLaunch {
            val testIngredient = Ingredient(
                id = UUID.randomUUID().toString(),
                name = "Sữa tươi (test)",
                quantity = 1.0,
                unit = "hộp",
                category = IngredientCategory.DAIRY,
                expiryDate = LocalDate.now().plusDays(1), // hết hạn ngày mai
                addedDate = LocalDate.now()
            )
            pantryRepository.upsert(testIngredient)
        }
    }

    fun addTestMealPlanForToday() {
        safeLaunch {
            insertTestMealPlanForToday()
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
}
