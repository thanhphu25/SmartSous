package com.example.smartsous.feature.pantry

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartsous.core.common.BarcodeAnalyzer
import com.example.smartsous.core.common.Spacing
import com.example.smartsous.ui.theme.Purple400
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Composable
fun BarcodeScanScreen(
    onBarcodeDetected: (String) -> Unit,  // trả về tên sản phẩm
    onBack: () -> Unit,
    viewModel: BarcodeScanViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isLookingUp by remember { mutableStateOf(false) }
    var hasDetected by remember { mutableStateOf(false) }

    // Launcher xin quyền camera
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        if (hasCameraPermission) {
            // Camera preview
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    val executor = Executors.newSingleThreadExecutor()

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(
                                ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                            )
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(
                                    executor,
                                    BarcodeAnalyzer { barcodeValue ->
                                        // Chỉ xử lý lần đầu
                                        if (hasDetected) return@BarcodeAnalyzer
                                        hasDetected = true
                                        isLookingUp = true

                                        // Lookup tên sản phẩm trong coroutine
                                        kotlinx.coroutines.GlobalScope.launch(
                                            kotlinx.coroutines.Dispatchers.Main
                                        ) {
                                            val productName = viewModel
                                                .lookupProduct(barcodeValue)
                                            isLookingUp = false
                                            onBarcodeDetected(productName)
                                        }
                                    }
                                )
                            }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("BarcodeScreen", "Camera lỗi: ${e.message}")
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Overlay — khung quét ở giữa
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.weight(0.25f))

                // Khung quét
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Transparent)
                ) {
                    // 4 góc viền
                    ScannerCorner(Alignment.TopStart)
                    ScannerCorner(Alignment.TopEnd)
                    ScannerCorner(Alignment.BottomStart)
                    ScannerCorner(Alignment.BottomEnd)
                }

                Spacer(Modifier.height(Spacing.lg))

                // Hướng dẫn
                Text(
                    text = if (isLookingUp) "Đang tra cứu sản phẩm..."
                    else "Đặt barcode vào khung để quét",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = Spacing.lg)
                )

                if (isLookingUp) {
                    Spacer(Modifier.height(Spacing.md))
                    CircularProgressIndicator(
                        color = Purple400,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(Modifier.weight(0.55f))
            }

        } else {
            // Chưa có quyền camera
            Column(
                modifier = Modifier.fillMaxSize().padding(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Text(
                    "Cần quyền truy cập camera",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(Spacing.md))
                Text(
                    "Cho phép SmartSous dùng camera để quét barcode sản phẩm",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(Spacing.lg))
                androidx.compose.material3.Button(
                    onClick = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                ) {
                    Text("Cấp quyền camera")
                }
            }
        }

        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(Spacing.sm)
                .background(
                    Color.Black.copy(alpha = 0.4f),
                    androidx.compose.foundation.shape.CircleShape
                )
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Quay lại",
                tint = Color.White
            )
        }
    }
}

// 4 góc viền của khung quét
@Composable
private fun ScannerCorner(alignment: Alignment) {
    Box(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 4.dp
        val cornerSize = 30.dp

        Box(
            modifier = Modifier
                .align(alignment)
                .size(cornerSize)
                .background(Color.Transparent)
        ) {
            // Vẽ 2 đường thẳng tạo thành góc
            val isTop = alignment == Alignment.TopStart || alignment == Alignment.TopEnd
            val isLeft = alignment == Alignment.TopStart || alignment == Alignment.BottomStart

            // Đường ngang
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(strokeWidth)
                    .align(if (isTop) Alignment.TopCenter else Alignment.BottomCenter)
                    .background(Purple400)
            )
            // Đường dọc
            Box(
                modifier = Modifier
                    .width(strokeWidth)
                    .fillMaxSize()
                    .align(if (isLeft) Alignment.CenterStart else Alignment.CenterEnd)
                    .background(Purple400)
            )
        }
    }
}