package com.example.smartsous.feature.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartsous.core.common.Spacing
import com.example.smartsous.ui.theme.Purple400
import com.example.smartsous.ui.theme.Purple50
import com.example.smartsous.ui.theme.Teal400
import kotlinx.coroutines.launch

// Data class cho mỗi slide
data class OnboardingPage(
    val emoji: String,
    val title: String,
    val description: String,
    val backgroundColor: Color
)

// Nội dung 3 slide
private val pages = listOf(
    OnboardingPage(
        emoji = "🤖",
        title = "Chef AI thông minh",
        description = "Hỏi SmartSous bất cứ điều gì về nấu ăn.\nChatbot gợi ý món từ nguyên liệu có sẵn trong tủ lạnh của bạn.",
        backgroundColor = Color(0xFFEEEDFE)
    ),
    OnboardingPage(
        emoji = "🧊",
        title = "Quản lý tủ lạnh",
        description = "Theo dõi nguyên liệu và ngày hết hạn.\nNhận thông báo khi thực phẩm sắp hết hạn để không lãng phí.",
        backgroundColor = Color(0xFFE1F5EE)
    ),
    OnboardingPage(
        emoji = "📅",
        title = "Lên kế hoạch ăn uống",
        description = "Lập thực đơn cả tuần dễ dàng.\nTìm kiếm và lọc món theo dinh dưỡng, khẩu vị và thời gian nấu.",
        backgroundColor = Color(0xFFFAECE7)
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.size - 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // Nút Bỏ qua — chỉ hiện ở slide 1 và 2
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.End
        ) {
            if (!isLastPage) {
                TextButton(
                    onClick = {
                        viewModel.markOnboardingDone()
                        onFinish()
                    }
                ) {
                    Text(
                        text = "Bỏ qua",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Pager — vuốt qua lại giữa 3 slide
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            OnboardingPageContent(page = pages[pageIndex])
        }

        // Dots indicator + nút điều hướng
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dots indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { index ->
                    val isSelected = index == pagerState.currentPage
                    val color by animateColorAsState(
                        targetValue = if (isSelected) Purple400
                        else MaterialTheme.colorScheme.outline,
                        animationSpec = tween(300),
                        label = "dot_color"
                    )
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 24.dp else 8.dp, 8.dp)
                            .clip(if (isSelected) RoundedCornerShape(4.dp) else CircleShape)
                            .background(color)
                    )
                }
            }

            Spacer(Modifier.height(Spacing.lg))

            // Nút chính
            Button(
                onClick = {
                    if (isLastPage) {
                        // Slide cuối → vào app
                        viewModel.markOnboardingDone()
                        onFinish()
                    } else {
                        // Chưa phải cuối → sang slide tiếp
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple400
                )
            ) {
                Text(
                    text = if (isLastPage) "Bắt đầu nấu ăn 🍳" else "Tiếp theo",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }
        }
    }
}

// Nội dung của 1 slide
@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Emoji trong box màu
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(page.backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = page.emoji,
                fontSize = 72.sp
            )
        }

        Spacer(Modifier.height(Spacing.xl))

        // Tiêu đề slide
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(Spacing.md))

        // Mô tả slide
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 26.sp
        )
    }
}