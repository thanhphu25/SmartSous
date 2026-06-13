package com.example.smartsous.core.vision

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

class IngredientImageAnalyzer(
    private val detector: YoloIngredientDetector,
    private val onDetected: (List<DetectedIngredient>) -> Unit
) : ImageAnalysis.Analyzer {
    private var isProcessing = false
    private var lastAnalyzedAt = 0L

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val now = System.currentTimeMillis()
        if (isProcessing || now - lastAnalyzedAt < ANALYZE_INTERVAL_MS) {
            imageProxy.close()
            return
        }

        isProcessing = true
        lastAnalyzedAt = now

        try {
            val bitmap = imageProxy.toBitmapSafely()
            if (bitmap != null) {
                val detections = detector.detect(bitmap)
                onDetected(detections)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Analyze failed: ${e.message}", e)
        } finally {
            isProcessing = false
            imageProxy.close()
        }
    }

    private fun ImageProxy.toBitmapSafely(): Bitmap? {
        val image = image ?: return null
        if (image.format != ImageFormat.YUV_420_888) return null

        val nv21 = yuv420ToNv21()
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val output = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 85, output)
        val bytes = output.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

        val rotation = imageInfo.rotationDegrees.toFloat()
        if (rotation == 0f) return bitmap

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            Matrix().apply { postRotate(rotation) },
            true
        )
    }

    private fun ImageProxy.yuv420ToNv21(): ByteArray {
        val image = image ?: return ByteArray(0)
        val crop = image.cropRect
        val width = crop.width()
        val height = crop.height()
        val data = ByteArray(width * height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8)
        val rowData = ByteArray(planes.maxOf { it.rowStride })
        var channelOffset: Int
        var outputStride: Int

        planes.forEachIndexed { planeIndex, plane ->
            when (planeIndex) {
                0 -> {
                    channelOffset = 0
                    outputStride = 1
                }
                1 -> {
                    channelOffset = width * height + 1
                    outputStride = 2
                }
                else -> {
                    channelOffset = width * height
                    outputStride = 2
                }
            }

            val buffer = plane.buffer
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride
            val shift = if (planeIndex == 0) 0 else 1
            val planeWidth = width shr shift
            val planeHeight = height shr shift

            buffer.position(
                rowStride * (crop.top shr shift) + pixelStride * (crop.left shr shift)
            )

            for (row in 0 until planeHeight) {
                val length: Int
                if (pixelStride == 1 && outputStride == 1) {
                    length = planeWidth
                    buffer.get(data, channelOffset, length)
                    channelOffset += length
                } else {
                    length = (planeWidth - 1) * pixelStride + 1
                    buffer.get(rowData, 0, length)
                    for (col in 0 until planeWidth) {
                        data[channelOffset] = rowData[col * pixelStride]
                        channelOffset += outputStride
                    }
                }

                if (row < planeHeight - 1) {
                    buffer.position(buffer.position() + rowStride - length)
                }
            }
        }
        return data
    }

    companion object {
        private const val TAG = "IngredientImageAnalyzer"
        private const val ANALYZE_INTERVAL_MS = 700L
    }
}
