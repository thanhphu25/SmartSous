package com.example.smartsous.feature.chatbot

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.smartsous.domain.model.ChatConversation
import com.example.smartsous.domain.model.ChatMessage
import com.example.smartsous.domain.model.MessageRole
import com.example.smartsous.domain.model.MessageType
import com.example.smartsous.domain.model.SuggestedRecipe
import com.example.smartsous.ui.theme.Amber400
import com.example.smartsous.ui.theme.Purple400
import com.example.smartsous.ui.theme.Teal400
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.smartsous.core.ui.components.MarkdownText
import com.example.smartsous.core.ui.components.parseMarkdown

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    onRecipeClick: (String) -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    var showHistoryDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size, uiState.streamingText) {
        val count = uiState.messages.size +
                (if (uiState.streamingText.isNotEmpty()) 1 else 0) +
                (if (uiState.isTyping && uiState.streamingText.isEmpty()) 1 else 0)
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack, // Icon mũi tên quay lại
                    contentDescription = "Quay lại",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Trợ lý AI SmartSous",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp)
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showHistoryDialog = true }) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Lịch sử chat",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = { viewModel.createNewChat() }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tạo chat mới",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        if (showHistoryDialog) {
            ChatHistoryDialog(
                conversations = uiState.conversations,
                currentConversationId = uiState.currentConversationId,
                onRename = viewModel::renameConversation,
                onDelete = viewModel::deleteConversation,
                onSelect = { conversationId ->
                    viewModel.switchConversation(conversationId)
                    showHistoryDialog = false
                },
                onDismiss = { showHistoryDialog = false }
            )
        }
        // ── Message list ──────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            items(uiState.messages, key = { it.id }) { message ->
                when (message.type) {
                    MessageType.RECIPE_SUGGESTION ->
                        RecipeSuggestionMessage(
                            message = message,
                            onRecipeClick = onRecipeClick
                        )
                    MessageType.TEXT ->
                        MessageBubble(message = message)
                }
            }

            // Streaming bubble
            if (uiState.streamingText.isNotEmpty()) {
                item {
                    StreamingBubble(text = uiState.streamingText)
                }
            }

            // Typing indicator
            if (uiState.isTyping && uiState.streamingText.isEmpty()) {
                item { TypingIndicator() }
            }

            // Error
            if (uiState.errorMessage != null) {
                item {
                    Text(
                        uiState.errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }
            }

            item { Spacer(Modifier.height(4.dp)) }
        }

        // ── Quick reply chips ──────────────────────────────────
        if (uiState.quickReplies.isNotEmpty() && !uiState.isTyping) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                uiState.quickReplies.forEach { reply ->
                    SuggestionChip(
                        onClick = {
                            viewModel.sendMessage(reply)
                        },
                        label = {
                            Text(reply, style = MaterialTheme.typography.labelSmall)
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Purple400.copy(alpha = 0.1f)
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = Purple400.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }

        // ── Input ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Hỏi SmartSous...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Purple400
                )
            )
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                },
                enabled = inputText.isNotBlank() && !uiState.isTyping,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Purple400,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Gửi")
            }
        }
    }
}

// ── Bubble tin nhắn thường ────────────────────────────────
@Composable
private fun ChatHistoryDialog(
    conversations: List<ChatConversation>,
    currentConversationId: String?,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var editingConversation by remember { mutableStateOf<ChatConversation?>(null) }
    var editingTitle by remember { mutableStateOf("") }
    var deletingConversation by remember { mutableStateOf<ChatConversation?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lịch sử chat") },
        text = {
            if (conversations.isEmpty()) {
                Text(
                    "Chưa có đoạn chat nào.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(conversations, key = { it.id }) { conversation ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (conversation.id == currentConversationId) {
                                    "• ${conversation.title}"
                                } else {
                                    conversation.title
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSelect(conversation.id) }
                                    .padding(start = 12.dp, end = 12.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (conversation.id == currentConversationId) {
                                    Purple400
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                            IconButton(
                                onClick = {
                                    editingConversation = conversation
                                    editingTitle = conversation.title
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    modifier = Modifier.size(18.dp),
                                    tint = Purple400,
                                    contentDescription = "Đổi tên chat"
                                )
                            }
                            IconButton(
                                onClick = {
                                    deletingConversation = conversation
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    modifier = Modifier.size(18.dp),
                                    contentDescription = "Xóa chat",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )

    val conversation = editingConversation
    if (conversation != null) {
        AlertDialog(
            onDismissRequest = { editingConversation = null },
            title = { Text("Đổi tên chat") },
            text = {
                OutlinedTextField(
                    value = editingTitle,
                    onValueChange = { editingTitle = it },
                    singleLine = true,
                    label = { Text("Tên đoạn chat") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRename(conversation.id, editingTitle)
                        editingConversation = null
                    },
                    enabled = editingTitle.isNotBlank()
                ) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingConversation = null }) {
                    Text("Hủy")
                }
            }
        )
    }

    val conversationToDelete = deletingConversation
    if (conversationToDelete != null) {
        AlertDialog(
            onDismissRequest = { deletingConversation = null },
            title = { Text("Xóa đoạn chat") },
            text = {
                Text("Bạn có chắc muốn xóa \"${conversationToDelete.title}\" không?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(conversationToDelete.id)
                        deletingConversation = null
                    }
                ) {
                    Text(
                        "Xóa",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingConversation = null }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(
                    topStart = 16.dp, topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd   = if (isUser) 4.dp  else 16.dp
                ))
                .background(
                    if (isUser) Purple400
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (isUser) {
                // User message: text thường, không cần markdown
                Text(
                    text  = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            } else {
                // AI message: render markdown
                MarkdownText(
                    text  = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// Trong StreamingBubble() — cũng dùng MarkdownText
@Composable
private fun StreamingBubble(text: String) {
    val alpha by rememberInfiniteTransition(label = "cursor")
        .animateFloat(
            initialValue  = 1f,
            targetValue   = 0f,
            animationSpec = infiniteRepeatable(
                animation  = tween(500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "cursor_alpha"
        )

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(
                    topStart = 16.dp, topEnd = 16.dp,
                    bottomStart = 4.dp, bottomEnd = 16.dp
                ))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Dùng AnnotatedString từ parseMarkdown + append cursor
            val annotated = buildAnnotatedString {
                append(text.parseMarkdown())
                withStyle(SpanStyle(color = Purple400.copy(alpha = alpha))) {
                    append("▌")
                }
            }
            Text(
                text  = annotated,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ── 3 chấm nhảy ──────────────────────────────────────────
@Composable
private fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val offsetY by transition.animateFloat(
                initialValue = 0f,
                targetValue  = -6f,
                animationSpec = infiniteRepeatable(
                    animation  = tween(400, delayMillis = index * 120),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_$index"
            )
            Box(
                modifier = Modifier
                    .offset(y = offsetY.dp)  // ← thêm dòng này
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

// ── Recipe suggestion message — unique feature ───────────
@Composable
private fun RecipeSuggestionMessage(
    message: ChatMessage,
    onRecipeClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Label "SmartSous đề xuất"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Purple400, CircleShape)
            )
            Text(
                "SmartSous",
                style = MaterialTheme.typography.labelSmall,
                color = Purple400,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Intro text
        if (message.content.isNotBlank()) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = 4.dp, bottomEnd = 16.dp
                    ))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // Recipe cards
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            message.suggestedRecipes.forEach { suggested ->
                ChatRecipeCard(
                    suggested = suggested,
                    onClick   = { onRecipeClick(suggested.recipe.id) }
                )
            }
        }
    }
}

// Card món ăn compact trong chat
@Composable
private fun ChatRecipeCard(
    suggested: SuggestedRecipe,
    onClick: () -> Unit
) {
    val recipe = suggested.recipe

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Ảnh nhỏ
            AsyncImage(
                model = recipe.imageUrl,
                contentDescription = recipe.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            // Thông tin
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    recipe.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "⏱ ${recipe.cookingTimeMinutes}p",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "🔥 ${recipe.nutrition.calories}kcal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                // Match percent badge
                if (suggested.matchPercent > 0) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (suggested.matchPercent == 100)
                                    Teal400.copy(alpha = 0.15f)
                                else Purple400.copy(alpha = 0.12f),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "${suggested.matchPercent}% nguyên liệu khớp",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (suggested.matchPercent == 100)
                                Teal400 else Purple400,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Arrow indicator
            Text(
                "›",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
