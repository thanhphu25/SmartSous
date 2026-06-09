package com.example.smartsous.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartsous.core.common.Spacing
import com.example.smartsous.ui.theme.Amber400
import com.example.smartsous.ui.theme.Coral400
import com.example.smartsous.ui.theme.Purple400
import com.example.smartsous.ui.theme.Teal400
import kotlinx.coroutines.launch

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

        // ── Header ────────────────────────────────────────────
        Column(
            modifier = Modifier.padding(
                horizontal = Spacing.md,
                vertical = Spacing.md
            )
        ) {
            Text(
                "Cài đặt ⚙️",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // ── Thông tin pantry ──────────────────────────────────
        SettingsCard(title = "Tủ lạnh của bạn") {
            SettingsInfoRow(
                label = "Tổng nguyên liệu",
                value = "${uiState.totalIngredients} món"
            )
            SettingsInfoRow(
                label = "Sắp hết hạn (3 ngày)",
                value = "${uiState.expiringCount} món",
                valueColor = if (uiState.expiringCount > 0) Coral400 else Teal400
            )
        }

        Spacer(Modifier.height(Spacing.md))

        // ── Debug: Test Notifications ─────────────────────────
        SettingsCard(
            title = "🧪 Test thông báo",
            subtitle = "Trigger thủ công để kiểm tra — chỉ dùng khi dev"
        ) {
            Spacer(Modifier.height(Spacing.sm))

            // Test expiry notification
            NotificationTestButton(
                icon = Icons.Default.Warning,
                title = "Test: Cảnh báo hết hạn",
                description = "Trigger ExpiryCheckWorker ngay lập tức.\n" +
                        "Cần có nguyên liệu với expiry ≤ 3 ngày.",
                buttonText = if (uiState.isTestingExpiry)
                    "Đang chạy..." else "Trigger ngay",
                buttonColor = Coral400,
                enabled = !uiState.isTestingExpiry,
                onClick = {
                    viewModel.testExpiryNotification()
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "ExpiryCheckWorker đã trigger — đợi vài giây"
                        )
                    }
                }
            )

            Spacer(Modifier.height(Spacing.sm))
            HorizontalDivider()
            Spacer(Modifier.height(Spacing.sm))

            // Test meal reminder
            NotificationTestButton(
                icon = Icons.Default.Notifications,
                title = "Test: Nhắc kế hoạch ăn",
                description = "Trigger MealReminderWorker ngay lập tức.",
                buttonText = if (uiState.isTestingMeal)
                    "Đang chạy..." else "Trigger ngay",
                buttonColor = Purple400,
                enabled = !uiState.isTestingMeal,
                onClick = {
                    viewModel.addTestMealPlanForToday()
                    viewModel.testMealReminder()
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "MealReminderWorker đã trigger — đợi vài giây"
                        )
                    }
                }
            )

            Spacer(Modifier.height(Spacing.sm))
            HorizontalDivider()
            Spacer(Modifier.height(Spacing.sm))

            // Test với ingredient giả (nếu pantry trống)
            NotificationTestButton(
                icon = Icons.Default.BugReport,
                title = "Thêm nguyên liệu sắp hết hạn (test)",
                description = "Tự động thêm 'Sữa tươi' hết hạn ngày mai\nđể test notification.",
                buttonText = "Thêm nguyên liệu test",
                buttonColor = Amber400,
                enabled = true,
                onClick = {
                    viewModel.addTestExpiringIngredient()
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "Đã thêm 'Sữa tươi' hết hạn ngày mai"
                        )
                    }
                }
            )
        }

        Spacer(Modifier.height(Spacing.md))

        // ── Trạng thái Workers ────────────────────────────────
        SettingsCard(title = "Trạng thái Workers") {
            uiState.workerStatuses.forEach { (name, status) ->
                SettingsInfoRow(
                    label = name,
                    value = status,
                    valueColor = when {
                        status.contains("RUNNING")   -> Teal400
                        status.contains("ENQUEUED")  -> Purple400
                        status.contains("SUCCEEDED") -> Teal400
                        status.contains("FAILED")    -> Coral400
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        Spacer(Modifier.height(80.dp))
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.padding(Spacing.md)
    )
}

// ── UI Components ─────────────────────────────────────────

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
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color =
        MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
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
private fun NotificationTestButton(
    icon: ImageVector,
    title: String,
    description: String,
    buttonText: String,
    buttonColor: androidx.compose.ui.graphics.Color,
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
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, buttonColor.copy(alpha = if (enabled) 1f else 0.4f)
                )
            ) {
                Text(buttonText, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
