package com.example.smartsous.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartsous.BuildConfig
import com.example.smartsous.core.common.Spacing
import com.example.smartsous.domain.model.NotificationPreference
import com.example.smartsous.domain.model.UserPreference
import com.example.smartsous.ui.theme.Amber400
import com.example.smartsous.ui.theme.Coral400
import com.example.smartsous.ui.theme.Purple400
import com.example.smartsous.ui.theme.Teal400
import kotlinx.coroutines.launch

private val cuisineOptions = listOf("Việt", "Nhật", "Hàn", "Ý", "Thái", "Âu")
private val allergyOptions = listOf("Hải sản", "Sữa", "Đậu phộng", "Trứng", "Gluten")
private val dislikedOptions = listOf("Hành", "Tỏi", "Ớt", "Nấm", "Rau mùi")

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = Spacing.md,
                vertical = Spacing.md
            )
        ) {
            Text(
                "Hồ sơ & cài đặt",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Cá nhân hóa SmartSous theo cách bạn nấu ăn.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        ProfileCard(uiState)

        Spacer(Modifier.height(Spacing.md))

        PreferencesCard(
            preferences = uiState.preferences,
            onCuisineToggle = viewModel::toggleFavoriteCuisine,
            onAllergyToggle = viewModel::toggleAllergy,
            onDislikedToggle = viewModel::toggleDislikedIngredient,
            onLowFatChange = viewModel::setLowFat,
            onHighProteinChange = viewModel::setHighProtein,
            onVegetarianChange = viewModel::setVegetarian,
            onTargetCaloriesChange = viewModel::setTargetCalories,
            onMaxCookingTimeChange = viewModel::setMaxCookingTime
        )

        Spacer(Modifier.height(Spacing.md))

        CookingStatsCard(uiState)

        Spacer(Modifier.height(Spacing.md))

        NotificationSettingsCard(
            notifications = uiState.notifications,
            onExpiryEnabledChange = viewModel::setExpiryRemindersEnabled,
            onMealEnabledChange = viewModel::setMealRemindersEnabled,
            onHourChange = viewModel::setMealReminderHour,
            onMinuteChange = viewModel::setMealReminderMinute
        )

        if (BuildConfig.DEBUG) {
            Spacer(Modifier.height(Spacing.md))

            DeveloperToolsCard(
                uiState = uiState,
                onTestExpiry = {
                    viewModel.testExpiryNotification()
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "ExpiryCheckWorker đã trigger, đợi vài giây"
                        )
                    }
                },
                onTestMeal = {
                    viewModel.testMealReminder()
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "MealReminderWorker đã trigger, đợi vài giây"
                        )
                    }
                },
                onAddTestIngredient = {
                    viewModel.addTestExpiringIngredient()
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "Đã thêm nguyên liệu test hết hạn ngày mai"
                        )
                    }
                }
            )
        }

        Spacer(Modifier.height(80.dp))
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.padding(Spacing.md)
    )
}

@Composable
private fun ProfileCard(uiState: SettingsUiState) {
    SettingsCard(
        title = "Hồ sơ",
        subtitle = "Thông tin cơ bản của tài khoản hiện tại."
    ) {
        SettingsInfoRow(
            icon = Icons.Default.Person,
            label = "Người dùng",
            value = uiState.profileName
        )
        SettingsInfoRow(
            icon = Icons.Default.Sync,
            label = "Trạng thái đồng bộ",
            value = uiState.syncStatus,
            valueColor = Teal400
        )
    }
}

@Composable
private fun PreferencesCard(
    preferences: UserPreference,
    onCuisineToggle: (String) -> Unit,
    onAllergyToggle: (String) -> Unit,
    onDislikedToggle: (String) -> Unit,
    onLowFatChange: (Boolean) -> Unit,
    onHighProteinChange: (Boolean) -> Unit,
    onVegetarianChange: (Boolean) -> Unit,
    onTargetCaloriesChange: (Int) -> Unit,
    onMaxCookingTimeChange: (Int) -> Unit
) {
    SettingsCard(
        title = "Sở thích ăn uống",
        subtitle = "Các lựa chọn này sẽ dùng cho gợi ý món, search và chatbot."
    ) {
        SectionLabel("Ẩm thực yêu thích")
        ChipRow(
            options = cuisineOptions,
            selectedOptions = preferences.favoriteCuisines,
            onToggle = onCuisineToggle
        )

        Spacer(Modifier.height(Spacing.sm))

        SectionLabel("Dị ứng")
        ChipRow(
            options = allergyOptions,
            selectedOptions = preferences.allergies,
            onToggle = onAllergyToggle
        )

        Spacer(Modifier.height(Spacing.sm))

        SectionLabel("Không thích")
        ChipRow(
            options = dislikedOptions,
            selectedOptions = preferences.dislikedIngredients,
            onToggle = onDislikedToggle
        )

        Spacer(Modifier.height(Spacing.sm))
        HorizontalDivider()
        Spacer(Modifier.height(Spacing.sm))

        SwitchRow(
            title = "Ăn chay",
            subtitle = "Ưu tiên món không dùng thịt/cá.",
            checked = preferences.vegetarian,
            onCheckedChange = onVegetarianChange
        )
        SwitchRow(
            title = "Ít béo",
            subtitle = "Ưu tiên món có lượng fat thấp hơn.",
            checked = preferences.preferLowFat,
            onCheckedChange = onLowFatChange
        )
        SwitchRow(
            title = "Giàu protein",
            subtitle = "Ưu tiên món nhiều protein.",
            checked = preferences.preferHighProtein,
            onCheckedChange = onHighProteinChange
        )

        Spacer(Modifier.height(Spacing.sm))

        StepperRow(
            title = "Calories mục tiêu mỗi bữa",
            value = "${preferences.targetCaloriesPerMeal} kcal",
            onDecrease = {
                onTargetCaloriesChange(preferences.targetCaloriesPerMeal - 50)
            },
            onIncrease = {
                onTargetCaloriesChange(preferences.targetCaloriesPerMeal + 50)
            }
        )
        StepperRow(
            title = "Thời gian nấu tối đa",
            value = "${preferences.maxCookingTimeMinutes} phút",
            onDecrease = {
                onMaxCookingTimeChange(preferences.maxCookingTimeMinutes - 5)
            },
            onIncrease = {
                onMaxCookingTimeChange(preferences.maxCookingTimeMinutes + 5)
            }
        )
    }
}

@Composable
private fun CookingStatsCard(uiState: SettingsUiState) {
    SettingsCard(
        title = "Thống kê nấu ăn",
        subtitle = "Tổng quan nhanh từ dữ liệu hiện tại."
    ) {
        SettingsInfoRow(
            icon = Icons.Default.Kitchen,
            label = "Nguyên liệu trong tủ",
            value = "${uiState.totalIngredients}"
        )
        SettingsInfoRow(
            icon = Icons.Default.Warning,
            label = "Sắp hết hạn",
            value = "${uiState.expiringCount}",
            valueColor = if (uiState.expiringCount > 0) Coral400 else Teal400
        )
        SettingsInfoRow(
            icon = Icons.Default.Favorite,
            label = "Món yêu thích",
            value = "${uiState.favoriteRecipeCount}"
        )
        SettingsInfoRow(
            icon = Icons.Default.Restaurant,
            label = "Món trong kế hoạch tuần",
            value = "${uiState.weeklyMealPlanCount}"
        )
    }
}

@Composable
private fun NotificationSettingsCard(
    notifications: NotificationPreference,
    onExpiryEnabledChange: (Boolean) -> Unit,
    onMealEnabledChange: (Boolean) -> Unit,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    SettingsCard(
        title = "Thông báo",
        subtitle = "Kiểm soát các nhắc nhở chính của SmartSous."
    ) {
        SwitchRow(
            title = "Nhắc nguyên liệu hết hạn",
            subtitle = "Gửi cảnh báo tại các mốc 3 ngày, 1 ngày và hôm nay.",
            checked = notifications.expiryRemindersEnabled,
            onCheckedChange = onExpiryEnabledChange
        )
        SwitchRow(
            title = "Nhắc kế hoạch ăn hôm nay",
            subtitle = "Gửi nhắc nhở dựa trên meal plan trong ngày.",
            checked = notifications.mealRemindersEnabled,
            onCheckedChange = onMealEnabledChange
        )

        Spacer(Modifier.height(Spacing.sm))

        StepperRow(
            title = "Giờ nhắc bữa ăn",
            value = notifications.formattedMealReminderTime(),
            enabled = notifications.mealRemindersEnabled,
            onDecrease = { onHourChange(notifications.mealReminderHour - 1) },
            onIncrease = { onHourChange(notifications.mealReminderHour + 1) }
        )
        StepperRow(
            title = "Phút nhắc",
            value = notifications.mealReminderMinute.toString().padStart(2, '0'),
            enabled = notifications.mealRemindersEnabled,
            onDecrease = { onMinuteChange(notifications.mealReminderMinute - 5) },
            onIncrease = { onMinuteChange(notifications.mealReminderMinute + 5) }
        )
    }
}

@Composable
private fun DeveloperToolsCard(
    uiState: SettingsUiState,
    onTestExpiry: () -> Unit,
    onTestMeal: () -> Unit,
    onAddTestIngredient: () -> Unit
) {
    SettingsCard(
        title = "Developer tools",
        subtitle = "Chỉ hiển thị trong debug build."
    ) {
        NotificationTestButton(
            icon = Icons.Default.Warning,
            title = "Test: Cảnh báo hết hạn",
            description = "Trigger ExpiryCheckWorker ngay lập tức.",
            buttonText = if (uiState.isTestingExpiry) "Đang chạy..." else "Trigger ngay",
            buttonColor = Coral400,
            enabled = !uiState.isTestingExpiry,
            onClick = onTestExpiry
        )

        Spacer(Modifier.height(Spacing.sm))
        HorizontalDivider()
        Spacer(Modifier.height(Spacing.sm))

        NotificationTestButton(
            icon = Icons.Default.Notifications,
            title = "Test: Nhắc kế hoạch ăn",
            description = "Trigger MealReminderWorker ngay lập tức.",
            buttonText = if (uiState.isTestingMeal) "Đang chạy..." else "Trigger ngay",
            buttonColor = Purple400,
            enabled = !uiState.isTestingMeal,
            onClick = onTestMeal
        )

        Spacer(Modifier.height(Spacing.sm))
        HorizontalDivider()
        Spacer(Modifier.height(Spacing.sm))

        NotificationTestButton(
            icon = Icons.Default.BugReport,
            title = "Thêm nguyên liệu sắp hết hạn",
            description = "Tạo dữ liệu test để kiểm tra notification.",
            buttonText = "Thêm test",
            buttonColor = Amber400,
            enabled = true,
            onClick = onAddTestIngredient
        )

        Spacer(Modifier.height(Spacing.sm))
        HorizontalDivider()
        Spacer(Modifier.height(Spacing.sm))

        SectionLabel("Trạng thái Workers")
        uiState.workerStatuses.forEach { (name, status) ->
            SettingsInfoRow(
                icon = Icons.Default.Schedule,
                label = name,
                value = status,
                valueColor = when {
                    status.contains("RUNNING") -> Teal400
                    status.contains("ENQUEUED") -> Purple400
                    status.contains("SUCCEEDED") -> Teal400
                    status.contains("FAILED") -> Coral400
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            content()
        }
    }
}

@Composable
private fun SettingsInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Purple400,
            modifier = Modifier.size(18.dp)
        )
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun ChipRow(
    options: List<String>,
    selectedOptions: List<String>,
    onToggle: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        items(options) { option ->
            val selected = option in selectedOptions
            FilterChip(
                selected = selected,
                onClick = { onToggle(option) },
                label = { Text(option) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Purple400.copy(alpha = 0.15f),
                    selectedLabelColor = Purple400
                )
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun StepperRow(
    title: String,
    value: String,
    enabled: Boolean = true,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        StepperButton("-", enabled, onDecrease)
        Text(
            value,
            modifier = Modifier.padding(horizontal = 2.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) Purple400 else MaterialTheme.colorScheme.onSurfaceVariant
        )
        StepperButton("+", enabled, onIncrease)
    }
}

@Composable
private fun StepperButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(width = 40.dp, height = 36.dp),
        contentPadding = PaddingValues(0.dp),
        border = BorderStroke(
            1.dp,
            if (enabled) Purple400.copy(alpha = 0.65f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    ) {
        Text(label)
    }
}

@Composable
private fun NotificationTestButton(
    icon: ImageVector,
    title: String,
    description: String,
    buttonText: String,
    buttonColor: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = buttonColor,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.xs))
            OutlinedButton(
                onClick = onClick,
                enabled = enabled,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = buttonColor
                ),
                border = BorderStroke(
                    1.dp,
                    buttonColor.copy(alpha = if (enabled) 1f else 0.4f)
                )
            ) {
                Text(buttonText, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private fun NotificationPreference.formattedMealReminderTime(): String =
    "${mealReminderHour.toString().padStart(2, '0')}:" +
            mealReminderMinute.toString().padStart(2, '0')
