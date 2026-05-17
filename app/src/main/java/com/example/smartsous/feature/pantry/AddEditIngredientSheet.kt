package com.example.smartsous.feature.pantry

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.smartsous.core.common.Spacing
import com.example.smartsous.core.ui.components.AppButton
import com.example.smartsous.domain.model.IngredientCategory
import com.example.smartsous.ui.theme.Coral400
import com.example.smartsous.ui.theme.Purple400
import java.time.LocalDate
import java.util.Calendar

private val commonUnits = listOf(
    "gram", "kg", "ml", "lít",
    "quả", "củ", "cái", "bó",
    "muỗng", "hộp", "gói", "lon"
)

private val categoryOptions = listOf(
    IngredientCategory.MEAT      to "🥩 Thịt",
    IngredientCategory.SEAFOOD   to "🦐 Hải sản",
    IngredientCategory.VEGETABLE to "🥦 Rau củ",
    IngredientCategory.DAIRY     to "🥛 Sữa/Trứng",
    IngredientCategory.GRAIN     to "🌾 Ngũ cốc",
    IngredientCategory.SPICE     to "🧄 Gia vị",
    IngredientCategory.FRUIT     to "🍎 Trái cây",
    IngredientCategory.BEVERAGE  to "🧃 Đồ uống",
    IngredientCategory.OTHER     to "📦 Khác",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditIngredientSheet(
    formState: IngredientFormState,
    onNameChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    onCategoryChange: (IngredientCategory) -> Unit,
    onExpiryDateChange: (LocalDate?) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md)
                .padding(bottom = Spacing.xl)
        ) {

            // ── Header ─────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (formState.isEditing) "Sửa nguyên liệu"
                    else "Thêm nguyên liệu",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Đóng")
                }
            }

            HorizontalDivider()
            Spacer(Modifier.height(Spacing.md))

            // ── Tên nguyên liệu ────────────────────────────
            Text(
                "Tên nguyên liệu *",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.xs))
            OutlinedTextField(
                value = formState.name,
                onValueChange = onNameChange,
                placeholder = { Text("VD: Cà chua, Thịt bò...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Purple400
                )
            )

            Spacer(Modifier.height(Spacing.md))

            // ── Số lượng + đơn vị ──────────────────────────
            Text(
                "Số lượng *",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.xs))
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                OutlinedTextField(
                    value = formState.quantity,
                    onValueChange = onQuantityChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    isError = formState.quantity.toDoubleOrNull() == null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Purple400
                    )
                )
                // Unit dropdown dạng chips
                Spacer(Modifier.width(0.dp))
            }

            // Unit chips
            Spacer(Modifier.height(Spacing.sm))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                commonUnits.forEach { unit ->
                    val selected = formState.unit == unit
                    FilterChip(
                        selected = selected,
                        onClick = { onUnitChange(unit) },
                        label = {
                            Text(unit, style = MaterialTheme.typography.labelSmall)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Purple400.copy(alpha = 0.15f),
                            selectedLabelColor = Purple400
                        )
                    )
                }
            }

            Spacer(Modifier.height(Spacing.md))

            // ── Danh mục ───────────────────────────────────
            Text(
                "Danh mục",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.xs))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                categoryOptions.forEach { (category, label) ->
                    val selected = formState.category == category
                    FilterChip(
                        selected = selected,
                        onClick = { onCategoryChange(category) },
                        label = {
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Purple400.copy(alpha = 0.15f),
                            selectedLabelColor = Purple400
                        )
                    )
                }
            }

            Spacer(Modifier.height(Spacing.md))

            // ── Ngày hết hạn ───────────────────────────────
            Text(
                "Ngày hết hạn (tuỳ chọn)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.xs))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                OutlinedButton(
                    onClick = {
                        // Mở DatePickerDialog
                        val today = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                onExpiryDateChange(LocalDate.of(year, month + 1, day))
                            },
                            today.get(Calendar.YEAR),
                            today.get(Calendar.MONTH),
                            today.get(Calendar.DAY_OF_MONTH)
                        ).also {
                            // Không cho chọn ngày trong quá khứ
                            it.datePicker.minDate = today.timeInMillis
                        }.show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = androidx.compose.ui.Modifier.padding(end = 4.dp)
                    )
                    Text(
                        formState.expiryDate?.toString() ?: "Chọn ngày",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Nút xoá ngày hết hạn
                if (formState.expiryDate != null) {
                    IconButton(onClick = { onExpiryDateChange(null) }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Xoá ngày",
                            tint = Coral400
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.lg))

            // ── Nút lưu ────────────────────────────────────
            AppButton(
                text = if (formState.isEditing) "Cập nhật" else "Thêm vào tủ lạnh",
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = formState.isValid
            )
        }
    }
}