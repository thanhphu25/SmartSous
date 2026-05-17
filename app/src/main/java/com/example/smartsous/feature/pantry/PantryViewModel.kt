package com.example.smartsous.feature.pantry

import androidx.lifecycle.viewModelScope
import com.example.smartsous.core.common.BaseViewModel
import com.example.smartsous.domain.model.Ingredient
import com.example.smartsous.domain.model.IngredientCategory
import com.example.smartsous.domain.repository.IPantryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

// State của dialog thêm/sửa
data class IngredientFormState(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val quantity: String = "1",
    val unit: String = "gram",
    val category: IngredientCategory = IngredientCategory.OTHER,
    val expiryDate: LocalDate? = null,
    val isEditing: Boolean = false  // true = sửa, false = thêm mới
) {
    val isValid: Boolean get() = name.isNotBlank()
            && quantity.toDoubleOrNull() != null
            && (quantity.toDoubleOrNull() ?: 0.0) > 0
}

data class PantryUiState(
    val allIngredients: List<Ingredient> = emptyList(),
    val filteredIngredients: List<Ingredient> = emptyList(),
    val selectedCategory: IngredientCategory? = null, // null = tất cả
    val isLoading: Boolean = true,
    val showAddEditSheet: Boolean = false,
    val formState: IngredientFormState = IngredientFormState(),
    val expiringCount: Int = 0
)

@HiltViewModel
class PantryViewModel @Inject constructor(
    private val pantryRepository: IPantryRepository
) : BaseViewModel() {

    private val _selectedCategory = MutableStateFlow<IngredientCategory?>(null)
    private val _showAddEditSheet = MutableStateFlow(false)
    private val _formState = MutableStateFlow(IngredientFormState())

    // Combine allIngredients + selectedCategory để filter
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = combine(
        pantryRepository.getAllIngredients(),
        _selectedCategory,
        _showAddEditSheet,
        _formState
    ) { ingredients, category, showSheet, form ->
        val filtered = if (category == null) ingredients
        else ingredients.filter { it.category == category }

        val expiringSoon = ingredients.count { ing ->
            ing.expiryDate?.let { date ->
                val days = java.time.temporal.ChronoUnit.DAYS
                    .between(LocalDate.now(), date).toInt()
                days in 0..3
            } ?: false
        }

        PantryUiState(
            allIngredients = ingredients,
            filteredIngredients = filtered.sortedWith(
                // Sort: hết hạn sớm nhất lên đầu, null expiry xuống cuối
                compareBy(nullsLast()) { it.expiryDate }
            ),
            selectedCategory = category,
            isLoading = false,
            showAddEditSheet = showSheet,
            formState = form,
            expiringCount = expiringSoon
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PantryUiState()
    )

    // ── Filter ────────────────────────────────────────────
    fun selectCategory(category: IngredientCategory?) {
        _selectedCategory.value = category
    }

    // ── Add Sheet ─────────────────────────────────────────
    fun openAddSheet() {
        _formState.value = IngredientFormState() // reset form
        _showAddEditSheet.value = true
    }

    // ── Edit Sheet ────────────────────────────────────────
    fun openEditSheet(ingredient: Ingredient) {
        _formState.value = IngredientFormState(
            id = ingredient.id,
            name = ingredient.name,
            quantity = ingredient.quantity.toString(),
            unit = ingredient.unit,
            category = ingredient.category,
            expiryDate = ingredient.expiryDate,
            isEditing = true
        )
        _showAddEditSheet.value = true
    }

    fun closeSheet() {
        _showAddEditSheet.value = false
    }

    // ── Form updates ──────────────────────────────────────
    fun onNameChange(name: String) {
        _formState.update { it.copy(name = name) }
    }

    fun onQuantityChange(qty: String) {
        _formState.update { it.copy(quantity = qty) }
    }

    fun onUnitChange(unit: String) {
        _formState.update { it.copy(unit = unit) }
    }

    fun onCategoryChange(category: IngredientCategory) {
        _formState.update { it.copy(category = category) }
    }

    fun onExpiryDateChange(date: LocalDate?) {
        _formState.update { it.copy(expiryDate = date) }
    }

    // ── Save (Add hoặc Edit) ──────────────────────────────
    fun saveIngredient() {
        val form = _formState.value
        if (!form.isValid) return

        safeLaunch {
            val ingredient = Ingredient(
                id = form.id,
                name = form.name.trim(),
                quantity = form.quantity.toDouble(),
                unit = form.unit,
                category = form.category,
                expiryDate = form.expiryDate,
                addedDate = LocalDate.now()
            )
            pantryRepository.upsert(ingredient)
            closeSheet()
        }
    }

    // ── Delete ────────────────────────────────────────────
    fun deleteIngredient(ingredient: Ingredient) {
        safeLaunch {
            pantryRepository.delete(ingredient)
        }
    }
}