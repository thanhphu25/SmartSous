package com.example.smartsous.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.smartsous.core.common.Spacing
import com.example.smartsous.core.ui.components.RecipeCard
import com.example.smartsous.core.ui.components.RecipeListSkeleton
import com.example.smartsous.data.local.entity.AppNotificationEntity
import com.example.smartsous.domain.model.SuggestedRecipe
import com.example.smartsous.domain.model.SuggestionReason
import com.example.smartsous.ui.theme.Purple400
import com.example.smartsous.ui.theme.Teal400
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onRecipeClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onNotificationNavigate: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    notificationViewModel: HomeNotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val notifications by notificationViewModel.notifications.collectAsStateWithLifecycle()
    val unreadCount by notificationViewModel.unreadCount.collectAsStateWithLifecycle()
    var showNotificationSheet by remember { mutableStateOf(false) }

    if (uiState.isLoading && uiState.allRecipes.isEmpty()) {
        RecipeListSkeleton()
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Spacing.lg)
    ) {

        // ── Greeting ──────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = Spacing.md,
                        vertical = Spacing.md
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Hôm nay nấu gì? 👨‍🍳",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Gợi ý từ nguyên liệu trong tủ lạnh",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                HomeSearchBar(onClick = onSearchClick)
                HomeNotificationButton(
                    unreadCount = unreadCount,
                    onClick = { showNotificationSheet = true }
                )
            }
        }

        // ── Hero Card — món gợi ý số 1 ───────────────────────
        if (uiState.suggestedRecipes.isNotEmpty()) {
            item {
                HeroRecipeCard(
                    suggested = uiState.suggestedRecipes.first(),
                    onClick = {
                        onRecipeClick(uiState.suggestedRecipes.first().recipe.id)
                    },
                    onFavoriteClick = {
                        val recipe = uiState.suggestedRecipes.first().recipe
                        viewModel.toggleFavorite(recipe.id, recipe.isFavorite)
                    },
                    modifier = Modifier.padding(horizontal = Spacing.md)
                )
                Spacer(Modifier.height(Spacing.md))
            }
        } else if (uiState.isRecommending) {
            item {
                RecommendationLoadingCard(
                    modifier = Modifier.padding(horizontal = Spacing.md)
                )
                Spacer(Modifier.height(Spacing.md))
            }
        }

        // ── Danh sách gợi ý ngang — món 2 đến 6 ─────────────
        if (uiState.suggestedRecipes.size > 1) {
            item {
                Text(
                    "Gợi ý cho bạn",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = Spacing.md)
                )
                Spacer(Modifier.height(Spacing.sm))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(
                        items = uiState.suggestedRecipes.drop(1).take(5),
                        key = { it.recipe.id }
                    ) { suggested ->
                        SuggestedMiniCard(
                            suggested = suggested,
                            onClick = { onRecipeClick(suggested.recipe.id) }
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.md))
            }
        }

        // ── Tất cả món ────────────────────────────────────────
        item {
            Text(
                "Tất cả món ăn",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = Spacing.md)
            )
            Spacer(Modifier.height(Spacing.sm))
        }

        items(
            items = uiState.allRecipes,
            key = { it.id }
        ) { recipe ->
            RecipeCard(
                recipe = recipe,
                onClick = { onRecipeClick(recipe.id) },
                onFavoriteClick = {
                    viewModel.toggleFavorite(recipe.id, recipe.isFavorite)
                },
                modifier = Modifier.padding(
                    horizontal = Spacing.md,
                    vertical = Spacing.xs
                )
            )
        }
    }

    if (showNotificationSheet) {
        NotificationInboxSheet(
            notifications = notifications,
            onDismiss = { showNotificationSheet = false },
            onNotificationClick = { notification ->
                notificationViewModel.markAsRead(notification.id)
                showNotificationSheet = false
                onNotificationNavigate(notification.route)
            }
        )
    }
}

// Hero Card — card lớn hiện ở đầu màn hình
@Composable
private fun HomeSearchBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(112.dp)
            .height(40.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Tìm kiếm",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeNotificationButton(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .size(40.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            BadgedBox(
                badge = {
                    if (unreadCount > 0) {
                        Badge(
                            containerColor = Color(0xFFE53935),
                            contentColor = Color.White
                        ) {
                            Text(
                                text = unreadCount.coerceAtMost(99).toString(),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = if (unreadCount > 0) {
                        Icons.Default.Notifications
                    } else {
                        Icons.Default.NotificationsNone
                    },
                    contentDescription = "Thông báo",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationInboxSheet(
    notifications: List<AppNotificationEntity>,
    onDismiss: () -> Unit,
    onNotificationClick: (AppNotificationEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md)
                .padding(bottom = Spacing.lg)
        ) {
            Text(
                text = "Thông báo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(Spacing.sm))

            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chưa có thông báo nào",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    items(
                        items = notifications,
                        key = { it.id }
                    ) { notification ->
                        NotificationInboxItem(
                            notification = notification,
                            onClick = { onNotificationClick(notification) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationInboxItem(
    notification: AppNotificationEntity,
    onClick: () -> Unit
) {
    val isUnread = notification.readAt == null
    val accentColor = when (notification.type) {
        "EXPIRY" -> Color(0xFFE86A33)
        else -> Purple400
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnread) {
                accentColor.copy(alpha = 0.09f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (notification.type == "EXPIRY") {
                        Icons.Default.Kitchen
                    } else {
                        Icons.AutoMirrored.Filled.EventNote
                    },
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (isUnread) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(percent = 50))
                                .background(Color(0xFFE53935))
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatNotificationTime(notification.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }
        }
    }
}

private fun formatNotificationTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm dd/MM"))

@Composable
private fun RecommendationLoadingCard(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = Purple400
        )
        Text(
            "Đang gợi ý món phù hợp...",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Purple400
        )
    }
}

@Composable
private fun HeroRecipeCard(
    suggested: SuggestedRecipe,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recipe = suggested.recipe

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box {
            // Ảnh nền
            AsyncImage(
                model = recipe.imageUrl,
                contentDescription = recipe.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient overlay từ nửa dưới lên — để text đọc được nổi bật hơn
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // Badge lý do gợi ý — góc trên trái
            SuggestionBadge(
                reason = suggested.reason,
                matchPercent = suggested.matchPercent,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(Spacing.md)
            )

            // Thông tin món — góc dưới trái
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(Spacing.md)
            ) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "⏱ ${recipe.cookingTimeMinutes} phút",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Text(
                        "🔥 ${recipe.nutrition.calories} kcal",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    if (suggested.matchPercent > 0) {
                        Text(
                            "🥬 ${suggested.matchPercent}% nguyên liệu",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

// Mini card dạng đứng trong LazyRow
@Composable
private fun SuggestedMiniCard(
    suggested: SuggestedRecipe,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recipe = suggested.recipe

    Card(
        modifier = modifier
            .width(160.dp)
            .padding(bottom = Spacing.sm) // Khoảng trống cho shadow
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column {
            Box {
                AsyncImage(
                    model = recipe.imageUrl,
                    contentDescription = recipe.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp) // Tăng chiều cao để ảnh không lùn
                )
                if (suggested.matchPercent == 100) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(Teal400, RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("✓ Đủ NL", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
            }
            Column(modifier = Modifier.padding(Spacing.sm)) {
                Text(
                    recipe.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${recipe.cookingTimeMinutes} phút · ${recipe.nutrition.calories} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Badge "Đủ nguyên liệu", "Lành mạnh"...
@Composable
private fun SuggestionBadge(
    reason: SuggestionReason,
    matchPercent: Int,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (reason) {
        SuggestionReason.PERFECT_MATCH      -> "✅ Đủ nguyên liệu" to Teal400
        SuggestionReason.HIGH_MATCH         -> "🥬 $matchPercent% nguyên liệu" to Purple400
        SuggestionReason.USE_EXPIRING_SOON  -> "⏳ Sắp hết hạn" to Purple400
        SuggestionReason.FAVORITE_PICK      -> "❤️ Món yêu thích" to Purple400
        SuggestionReason.HEALTHY_CHOICE     -> "💚 Lành mạnh" to Teal400
        SuggestionReason.QUICK_COOK         -> "⚡ Nấu nhanh" to Purple400
        SuggestionReason.NOT_COOKED_RECENTLY -> "🆕 Món mới" to Purple400
    }

    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.8f), RoundedCornerShape(percent = 50)) // Bo tròn hẳn dạng pill
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}
