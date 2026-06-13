package com.example.smartsous.feature.pantry

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartsous.core.common.Spacing
import com.example.smartsous.core.vision.DetectedIngredient
import com.example.smartsous.domain.model.IngredientCategory
import com.example.smartsous.ui.theme.Purple400
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.max

@Composable
fun IngredientScanScreen(
    onIngredientDetected: (String, IngredientCategory) -> Unit,
    onBack: () -> Unit,
    viewModel: IngredientScanViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var detections by remember { mutableStateOf<List<DetectedIngredient>>(emptyList()) }
    var cameraReady by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var captureError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    fun resetCapture() {
        capturedBitmap = null
        detections = emptyList()
        captureError = null
        isAnalyzing = false
    }

    fun captureAndDetect() {
        val capture = imageCapture ?: return
        val photoFile = File(context.cacheDir, "ingredient_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        isAnalyzing = true
        captureError = null
        detections = emptyList()

        capture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    if (bitmap == null) {
                        mainExecutor.execute {
                            isAnalyzing = false
                            captureError = "Không đọc được ảnh vừa chụp."
                        }
                        return
                    }

                    val result = viewModel.detector.detect(bitmap)
                    mainExecutor.execute {
                        capturedBitmap = bitmap
                        detections = result
                        isAnalyzing = false
                        captureError = null
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    mainExecutor.execute {
                        isAnalyzing = false
                        captureError = exception.message ?: "Không chụp được ảnh."
                    }
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val captureUseCase = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                captureUseCase
                            )
                            imageCapture = captureUseCase
                            cameraReady = true
                        } catch (e: Exception) {
                            android.util.Log.e("IngredientScan", "Camera error: ${e.message}", e)
                        }
                    }, mainExecutor)

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            capturedBitmap?.let { bitmap ->
                CapturedIngredientImage(
                    bitmap = bitmap,
                    detections = detections,
                    modifier = Modifier.fillMaxSize()
                )
            }

            IngredientCaptureOverlay(
                cameraReady = cameraReady,
                capturedBitmap = capturedBitmap,
                detections = detections,
                isAnalyzing = isAnalyzing,
                captureError = captureError,
                onCapture = ::captureAndDetect,
                onRetake = ::resetCapture,
                onUseIngredient = { detected ->
                    onIngredientDetected(detected.displayName, detected.category)
                },
                onManualInput = {
                    onIngredientDetected("", IngredientCategory.OTHER)
                }
            )
        } else {
            CameraPermissionContent(
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            )
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(Spacing.sm)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Quay lại",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun CapturedIngredientImage(
    bitmap: Bitmap,
    detections: List<DetectedIngredient>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Ảnh nguyên liệu đã chụp",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        DetectionBoxesOverlay(
            bitmap = bitmap,
            detections = detections,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun DetectionBoxesOverlay(
    bitmap: Bitmap,
    detections: List<DetectedIngredient>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (detections.isEmpty()) return@Canvas

        val imageWidth = bitmap.width.toFloat()
        val imageHeight = bitmap.height.toFloat()
        val scale = max(size.width / imageWidth, size.height / imageHeight)
        val dx = (size.width - imageWidth * scale) / 2f
        val dy = (size.height - imageHeight * scale) / 2f
        val boxColor = Color(0xFF7E6DE8)
        val secondaryBoxColor = Color(0xFF00A78E)
        val strokeWidth = 4.dp.toPx()
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 14.dp.toPx()
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT,
                android.graphics.Typeface.BOLD
            )
        }

        detections.take(5).forEachIndexed { index, detection ->
            val rect = detection.boundingBox
            val left = rect.left * scale + dx
            val top = rect.top * scale + dy
            val right = rect.right * scale + dx
            val bottom = rect.bottom * scale + dy
            val clampedLeft = left.coerceIn(0f, size.width)
            val clampedTop = top.coerceIn(0f, size.height)
            val clampedRight = right.coerceIn(0f, size.width)
            val clampedBottom = bottom.coerceIn(0f, size.height)
            if (clampedRight <= clampedLeft || clampedBottom <= clampedTop) return@forEachIndexed

            val color = if (index == 0) boxColor else secondaryBoxColor
            drawRoundRect(
                color = color,
                topLeft = Offset(clampedLeft, clampedTop),
                size = Size(clampedRight - clampedLeft, clampedBottom - clampedTop),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                style = Stroke(width = strokeWidth)
            )

            val label = "${detection.displayName} ${(detection.confidence * 100).toInt()}%"
            val labelPaddingX = 8.dp.toPx()
            val labelPaddingY = 5.dp.toPx()
            val labelWidth = labelPaint.measureText(label) + labelPaddingX * 2
            val labelHeight = labelPaint.textSize + labelPaddingY * 2
            val labelLeft = clampedLeft
            val labelTop = (clampedTop - labelHeight).coerceAtLeast(0f)

            drawRoundRect(
                color = color.copy(alpha = 0.92f),
                topLeft = Offset(labelLeft, labelTop),
                size = Size(labelWidth.coerceAtMost(size.width - labelLeft), labelHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
            )
            drawContext.canvas.nativeCanvas.drawText(
                label,
                labelLeft + labelPaddingX,
                labelTop + labelPaddingY + labelPaint.textSize * 0.82f,
                labelPaint
            )
        }
    }
}

@Composable
private fun IngredientCaptureOverlay(
    cameraReady: Boolean,
    capturedBitmap: Bitmap?,
    detections: List<DetectedIngredient>,
    isAnalyzing: Boolean,
    captureError: String?,
    onCapture: () -> Unit,
    onRetake: () -> Unit,
    onUseIngredient: (DetectedIngredient) -> Unit,
    onManualInput: () -> Unit
) {
    val bestDetection = detections.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Purple400,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Nhận diện nguyên liệu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                when {
                    !cameraReady -> LoadingCameraState("Đang mở camera...")
                    isAnalyzing -> LoadingCameraState("Đang nhận diện ảnh vừa chụp...")
                    captureError != null -> CaptureErrorState(
                        message = captureError,
                        onRetake = onRetake,
                        onManualInput = onManualInput
                    )
                    capturedBitmap == null -> ReadyToCaptureState(
                        onCapture = onCapture,
                        onManualInput = onManualInput
                    )
                    bestDetection == null -> NoIngredientDetectedState(
                        onRetake = onRetake,
                        onManualInput = onManualInput
                    )
                    else -> DetectedIngredientState(
                        detected = bestDetection,
                        onUseIngredient = onUseIngredient,
                        onRetake = onRetake,
                        onManualInput = onManualInput
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingCameraState(message: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = Purple400
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReadyToCaptureState(
    onCapture: () -> Unit,
    onManualInput: () -> Unit
) {
    Text(
        text = "Đưa nguyên liệu vào khung rồi chụp ảnh. SmartSous sẽ dự đoán sau khi ảnh được chụp.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Button(
        onClick = onCapture,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.size(8.dp))
        Text("Chụp ảnh")
    }
    OutlinedButton(
        onClick = onManualInput,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.size(8.dp))
        Text("Nhập thủ công")
    }
}

@Composable
private fun CaptureErrorState(
    message: String,
    onRetake: () -> Unit,
    onManualInput: () -> Unit
) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error
    )
    CaptureFallbackActions(
        onRetake = onRetake,
        onManualInput = onManualInput
    )
}

@Composable
private fun NoIngredientDetectedState(
    onRetake: () -> Unit,
    onManualInput: () -> Unit
) {
    Text(
        text = "Ảnh này chưa nhận diện được nguyên liệu. Bạn có thể chụp lại hoặc nhập thủ công.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    CaptureFallbackActions(
        onRetake = onRetake,
        onManualInput = onManualInput
    )
}

@Composable
private fun DetectedIngredientState(
    detected: DetectedIngredient,
    onUseIngredient: (DetectedIngredient) -> Unit,
    onRetake: () -> Unit,
    onManualInput: () -> Unit
) {
    Text(
        text = detected.displayName,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = Purple400,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
    Text(
        text = "Độ tin cậy ${(detected.confidence * 100).toInt()}% • ${categoryLabel(detected.category)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Button(
        onClick = { onUseIngredient(detected) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Restaurant,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.size(8.dp))
        Text("Dùng nguyên liệu này")
    }
    CaptureFallbackActions(
        onRetake = onRetake,
        onManualInput = onManualInput,
        manualText = "Nhận sai? Nhập thủ công"
    )
}

@Composable
private fun CaptureFallbackActions(
    onRetake: () -> Unit,
    onManualInput: () -> Unit,
    manualText: String = "Nhập thủ công"
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        OutlinedButton(
            onClick = onRetake,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.size(8.dp))
            Text("Chụp lại")
        }
        OutlinedButton(
            onClick = onManualInput,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(manualText)
        }
    }
}

@Composable
private fun CameraPermissionContent(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
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
            "Cho phép SmartSous dùng camera để chụp và nhận diện nguyên liệu.",
            color = Color.White.copy(alpha = 0.75f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Spacing.lg))
        Button(onClick = onRequestPermission) {
            Text("Cấp quyền camera")
        }
    }
}

private fun categoryLabel(category: IngredientCategory): String =
    when (category) {
        IngredientCategory.VEGETABLE -> "Rau củ"
        IngredientCategory.MEAT -> "Thịt"
        IngredientCategory.SEAFOOD -> "Hải sản"
        IngredientCategory.DAIRY -> "Sữa/Trứng"
        IngredientCategory.GRAIN -> "Ngũ cốc"
        IngredientCategory.SPICE -> "Gia vị"
        IngredientCategory.FRUIT -> "Trái cây"
        IngredientCategory.BEVERAGE -> "Đồ uống"
        IngredientCategory.OTHER -> "Khác"
    }
