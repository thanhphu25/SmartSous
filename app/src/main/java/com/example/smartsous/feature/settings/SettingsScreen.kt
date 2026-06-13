package com.example.smartsous.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartsous.BuildConfig
import com.example.smartsous.core.common.Spacing
import com.example.smartsous.core.ui.components.AppTextField
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
    
    var devClickCount by remember { mutableIntStateOf(0) }
    var isDeveloperModeEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(devClickCount) {
        if (devClickCount >= 7) {
            isDeveloperModeEnabled = true
        }
    }

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

        CookingProfileCard(
            preferences = uiState.preferences,
            onLowFatChange = viewModel::setLowFat,
            onHighProteinChange = viewModel::setHighProtein,
            onVegetarianChange = viewModel::setVegetarian,
            onTargetCaloriesChange = viewModel::setTargetCalories,
            onMaxCookingTimeChange = viewModel::setMaxCookingTime
        )

        Spacer(Modifier.height(Spacing.md))

        TasteRestrictionCard(
            preferences = uiState.preferences,
            onCuisineToggle = viewModel::toggleFavoriteCuisine,
            onAllergyToggle = viewModel::toggleAllergy,
            onDislikedToggle = viewModel::toggleDislikedIngredient
        )

        Spacer(Modifier.height(Spacing.md))

        PantryAndNotificationCard(
            uiState = uiState,
            notifications = uiState.notifications,
            onExpiryEnabledChange = viewModel::setExpiryRemindersEnabled,
            onMealEnabledChange = viewModel::setMealRemindersEnabled,
            onHourChange = viewModel::setMealReminderHour,
            onMinuteChange = viewModel::setMealReminderMinute
        )

        Spacer(Modifier.height(Spacing.md))

        AiSettingsCard(
            preferences = uiState.preferences,
            onApiKeyChange = viewModel::setAiApiKey,
            onModelChange = viewModel::setAiModel
        )

        if (isDeveloperModeEnabled) {
            Spacer(Modifier.height(Spacing.md))

            DeveloperToolsCard(
                uiState = uiState,
                onTestExpiry = {
                    viewModel.testExpiryNotification()
                    scope.launch {
                        snackbarHostState.showSnackbar("ExpiryCheckWorker đã trigger")
                    }
                },
                onTestMeal = {
                    viewModel.testMealReminder()
                    scope.launch {
                        snackbarHostState.showSnackbar("MealReminderWorker đã trigger")
                    }
                },
                onAddTestIngredient = {
                    viewModel.addTestExpiringIngredient()
                    scope.launch {
                        snackbarHostState.showSnackbar("Đã thêm nguyên liệu test hết hạn")
                    }
                },
                onAddTestMealPlan = {
                    viewModel.addTestMealPlanForToday()
                    scope.launch {
                        snackbarHostState.showSnackbar("Đã thêm test meal plan hôm nay")
                    }
                },
                onResetSeed = {
                    viewModel.resetSeedFlag()
                    scope.launch {
                        snackbarHostState.showSnackbar("Đã reset seed flag")
                    }
                }
            )
        }

        Spacer(Modifier.height(Spacing.xl))

        Text(
            text = "Phiên bản ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.md)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { devClickCount++ },
                        onLongPress = { isDeveloperModeEnabled = true }
                    )
                }
                .padding(Spacing.sm),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(80.dp))
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.padding(Spacing.md)
    )
}

@Composable
private fun ProfileCard(uiState: SettingsUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Purple400.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "S",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Purple400
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = uiState.profileName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Người dùng ẩn danh",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Sync,
                        contentDescription = null,
                        tint = Teal400,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = uiState.syncStatus,
                        style = MaterialTheme.typography.labelMedium,
                        color = Teal400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CookingProfileCard(
    preferences: UserPreference,
    onLowFatChange: (Boolean) -> Unit,
    onHighProteinChange: (Boolean) -> Unit,
    onVegetarianChange: (Boolean) -> Unit,
    onTargetCaloriesChange: (Int) -> Unit,
    onMaxCookingTimeChange: (Int) -> Unit
) {
    SettingsCard(
        title = "Hồ sơ nấu ăn",
        subtitle = "SmartSous dùng hồ sơ này để cá nhân hóa gợi ý món và chatbot.",
        initiallyExpanded = true,
        collapsible = false
    ) {
        StepperRow(
            title = "Mục tiêu calo mỗi bữa",
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

        Spacer(Modifier.height(Spacing.xs))
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
    }
}

@Composable
private fun TasteRestrictionCard(
    preferences: UserPreference,
    onCuisineToggle: (String) -> Unit,
    onAllergyToggle: (String) -> Unit,
    onDislikedToggle: (String) -> Unit
) {
    SettingsCard(
        title = "Khẩu vị & hạn chế",
        subtitle = "Các lựa chọn này dùng cho gợi ý món, search và chatbot."
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
    }
}

@Composable
private fun PantryAndNotificationCard(
    uiState: SettingsUiState,
    notifications: NotificationPreference,
    onExpiryEnabledChange: (Boolean) -> Unit,
    onMealEnabledChange: (Boolean) -> Unit,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    SettingsCard(
        title = "Tủ lạnh & Nhắc hạn",
        subtitle = "Cài đặt thông báo và quản lý nguyên liệu."
    ) {
        SectionLabel("Thông báo")
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

        Spacer(Modifier.height(Spacing.sm))
        HorizontalDivider()
        Spacer(Modifier.height(Spacing.sm))

        SectionLabel("Thống kê")
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
private fun DeveloperToolsCard(
    uiState: SettingsUiState,
    onTestExpiry: () -> Unit,
    onTestMeal: () -> Unit,
    onAddTestIngredient: () -> Unit,
    onAddTestMealPlan: () -> Unit,
    onResetSeed: () -> Unit
) {
    SettingsCard(
        title = "Chế độ nhà phát triển",
        subtitle = "Các công cụ ẩn để debug ứng dụng."
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
            icon = Icons.Default.Kitchen,
            title = "Thêm test Pantry",
            description = "Tạo dữ liệu nguyên liệu test.",
            buttonText = "Thêm",
            buttonColor = Amber400,
            enabled = true,
            onClick = onAddTestIngredient
        )

        Spacer(Modifier.height(Spacing.sm))
        HorizontalDivider()
        Spacer(Modifier.height(Spacing.sm))

        NotificationTestButton(
            icon = Icons.Default.Restaurant,
            title = "Thêm test Meal Plan",
            description = "Tạo meal plan test cho hôm nay.",
            buttonText = "Thêm",
            buttonColor = Teal400,
            enabled = true,
            onClick = onAddTestMealPlan
        )

        Spacer(Modifier.height(Spacing.sm))
        HorizontalDivider()
        Spacer(Modifier.height(Spacing.sm))

        NotificationTestButton(
            icon = Icons.Default.Sync,
            title = "Reset seed data",
            description = "Khôi phục trạng thái chưa seed dữ liệu.",
            buttonText = "Reset",
            buttonColor = Coral400,
            enabled = true,
            onClick = onResetSeed
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
private fun AiSettingsCard(
    preferences: UserPreference,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit
) {
    var showApiKey by rememberSaveable { mutableStateOf(false) }

    SettingsCard(
        title = "Cài đặt AI",
        subtitle = "Cấu hình mô hình AI và API Key cho SmartSous."
    ) {
        AppTextField(
            value = preferences.aiApiKey,
            onValueChange = onApiKeyChange,
            label = "Groq API Key",
            placeholder = "Nhập Groq API Key của bạn (tuỳ chọn)",
            keyboardType = KeyboardType.Password,
            visualTransformation = if (showApiKey) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { showApiKey = !showApiKey }) {
                    Icon(
                        imageVector = if (showApiKey) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                        contentDescription = if (showApiKey) "Ẩn API key" else "Hiện API key"
                    )
                }
            },
            singleLine = true
        )

        Spacer(Modifier.height(Spacing.sm))
        
        SectionLabel("Mô hình AI")
        val aiModels = listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant", "gemma2-9b-it")
        SingleChoiceChipRow(
            options = aiModels,
            selectedOption = preferences.aiModel,
            onSelect = onModelChange
        )
    }
}

@Composable
private fun SingleChoiceChipRow(
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        items(options) { option ->
            val selected = option == selectedOption
            FilterChip(
                selected = selected,
                onClick = { onSelect(option) },
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
private fun SettingsCard(
    title: String,
    subtitle: String? = null,
    initiallyExpanded: Boolean = false,
    collapsible: Boolean = true,
    content: @Composable () -> Unit
) {
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    val showContent = expanded || !collapsible

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (collapsible) {
                            Modifier.clickable { expanded = !expanded }
                        } else {
                            Modifier
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Column(modifier = Modifier.weight(1f)) {
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
                }
                if (collapsible) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Thu gọn" else "Mở rộng",
                        tint = Purple400
                    )
                }
            }

            if (showContent) {
                Spacer(Modifier.height(Spacing.sm))
                content()
            }
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
