package com.example.smartsous.core.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.example.smartsous.domain.model.IngredientCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

@Singleton
class YoloIngredientDetector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val labelMapper: IngredientLabelMapper
) {
    private val interpreter: Interpreter by lazy {
        Interpreter(loadModel(), Interpreter.Options().setNumThreads(4))
    }
    private val labels: List<String> by lazy { loadLabels() }

    fun detect(
        bitmap: Bitmap,
        confidenceThreshold: Float = 0.45f,
        iouThreshold: Float = 0.45f
    ): List<DetectedIngredient> {
        return try {
            val inputShape = interpreter.getInputTensor(0).shape()
            val inputType = interpreter.getInputTensor(0).dataType()
            val outputShape = interpreter.getOutputTensor(0).shape()
            val outputType = interpreter.getOutputTensor(0).dataType()

            if (inputShape.size != 4 || outputShape.size != 3 || outputType != DataType.FLOAT32) {
                Log.w(TAG, "Unsupported model shape input=${inputShape.contentToString()} output=${outputShape.contentToString()}")
                return emptyList()
            }

            val inputHeight = inputShape[1]
            val inputWidth = inputShape[2]
            val inputBuffer = bitmap.toInputBuffer(inputWidth, inputHeight, inputType)
            val output = Array(outputShape[0]) {
                Array(outputShape[1]) {
                    FloatArray(outputShape[2])
                }
            }

            interpreter.run(inputBuffer, output)

            parseYoloOutput(
                raw = output[0],
                outputShape = outputShape,
                imageWidth = bitmap.width,
                imageHeight = bitmap.height,
                inputWidth = inputWidth,
                inputHeight = inputHeight,
                confidenceThreshold = confidenceThreshold,
                iouThreshold = iouThreshold
            )
        } catch (e: Exception) {
            Log.e(TAG, "Detect failed: ${e.message}", e)
            emptyList()
        }
    }

    private fun parseYoloOutput(
        raw: Array<FloatArray>,
        outputShape: IntArray,
        imageWidth: Int,
        imageHeight: Int,
        inputWidth: Int,
        inputHeight: Int,
        confidenceThreshold: Float,
        iouThreshold: Float
    ): List<DetectedIngredient> {
        val dim1 = outputShape[1]
        val dim2 = outputShape[2]
        val rows = if (dim1 < dim2) {
            List(dim2) { boxIndex -> FloatArray(dim1) { attrIndex -> raw[attrIndex][boxIndex] } }
        } else {
            raw.toList()
        }

        val detections = rows.mapNotNull { values ->
            if (values.size < 6) return@mapNotNull null
            val classStart = classStartIndex(values.size)
            if (classStart >= values.size) return@mapNotNull null

            val classScores = values.copyOfRange(classStart, values.size)
            val bestClass = classScores.indices.maxByOrNull { classScores[it] } ?: return@mapNotNull null
            val classScore = classScores[bestClass]
            val objectness = if (classStart == 5) values[4].coerceIn(0f, 1f) else 1f
            val confidence = (classScore * objectness).coerceIn(0f, 1f)
            if (confidence < confidenceThreshold) return@mapNotNull null

            val rawLabel = labels.getOrNull(bestClass) ?: "class_$bestClass"
            val rect = yoloBoxToRect(
                x = values[0],
                y = values[1],
                w = values[2],
                h = values[3],
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                inputWidth = inputWidth,
                inputHeight = inputHeight
            )

            DetectedIngredient(
                rawLabel = rawLabel,
                displayName = labelMapper.displayName(rawLabel),
                category = labelMapper.category(rawLabel),
                confidence = confidence,
                boundingBox = rect
            )
        }

        return nonMaxSuppression(detections, iouThreshold)
            .sortedByDescending { it.confidence }
            .take(MAX_RESULTS)
    }

    private fun classStartIndex(attributeCount: Int): Int {
        val labelCount = labels.size.takeIf { it > 0 }
        return when {
            labelCount != null && attributeCount == labelCount + 5 -> 5
            labelCount != null && attributeCount == labelCount + 4 -> 4
            attributeCount > 20 -> 4
            else -> 5
        }
    }

    private fun yoloBoxToRect(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        imageWidth: Int,
        imageHeight: Int,
        inputWidth: Int,
        inputHeight: Int
    ): RectF {
        val normalized = max(max(x, y), max(w, h)) <= 2f
        val scaleX = if (normalized) imageWidth.toFloat() else imageWidth.toFloat() / inputWidth
        val scaleY = if (normalized) imageHeight.toFloat() else imageHeight.toFloat() / inputHeight

        val cx = x * scaleX
        val cy = y * scaleY
        val bw = w * scaleX
        val bh = h * scaleY

        return RectF(
            (cx - bw / 2f).coerceIn(0f, imageWidth.toFloat()),
            (cy - bh / 2f).coerceIn(0f, imageHeight.toFloat()),
            (cx + bw / 2f).coerceIn(0f, imageWidth.toFloat()),
            (cy + bh / 2f).coerceIn(0f, imageHeight.toFloat())
        )
    }

    private fun nonMaxSuppression(
        detections: List<DetectedIngredient>,
        iouThreshold: Float
    ): List<DetectedIngredient> {
        val selected = mutableListOf<DetectedIngredient>()
        detections.sortedByDescending { it.confidence }.forEach { candidate ->
            val overlaps = selected.any { existing ->
                existing.rawLabel == candidate.rawLabel &&
                        iou(existing.boundingBox, candidate.boundingBox) > iouThreshold
            }
            if (!overlaps) selected += candidate
        }
        return selected
    }

    private fun iou(a: RectF, b: RectF): Float {
        val left = max(a.left, b.left)
        val top = max(a.top, b.top)
        val right = min(a.right, b.right)
        val bottom = min(a.bottom, b.bottom)
        val intersection = max(0f, right - left) * max(0f, bottom - top)
        val union = a.width() * a.height() + b.width() * b.height() - intersection
        return if (union <= 0f) 0f else intersection / union
    }

    private fun Bitmap.toInputBuffer(
        width: Int,
        height: Int,
        inputType: DataType
    ): ByteBuffer {
        val resized = Bitmap.createScaledBitmap(this, width, height, true)
        val bytesPerChannel = if (inputType == DataType.FLOAT32) 4 else 1
        val buffer = ByteBuffer
            .allocateDirect(width * height * 3 * bytesPerChannel)
            .order(ByteOrder.nativeOrder())
        val pixels = IntArray(width * height)
        resized.getPixels(pixels, 0, width, 0, 0, width, height)
        pixels.forEach { pixel ->
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            if (inputType == DataType.FLOAT32) {
                buffer.putFloat(r / 255f)
                buffer.putFloat(g / 255f)
                buffer.putFloat(b / 255f)
            } else {
                buffer.put(r.toByte())
                buffer.put(g.toByte())
                buffer.put(b.toByte())
            }
        }
        buffer.rewind()
        return buffer
    }

    private fun loadModel(): MappedByteBuffer {
        val descriptor = context.assets.openFd(MODEL_NAME)
        FileInputStream(descriptor.fileDescriptor).use { input ->
            return input.channel.map(
                FileChannel.MapMode.READ_ONLY,
                descriptor.startOffset,
                descriptor.declaredLength
            )
        }
    }

    private fun loadLabels(): List<String> {
        val candidates = listOf("labels.txt", "ingredient_labels.txt")
        return candidates.firstNotNullOfOrNull { fileName ->
            runCatching {
                context.assets.open(fileName).bufferedReader().useLines { lines ->
                    lines.map { it.trim() }
                        .filter { it.isNotBlank() }
                        .toList()
                }
            }.getOrNull()
        }.orEmpty()
    }

    companion object {
        private const val TAG = "YoloIngredientDetector"
        private const val MODEL_NAME = "best_copy_float32.tflite"
        private const val MAX_RESULTS = 5
    }
}
