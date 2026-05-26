package com.example.smartsous.core.common

import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()
    private var isProcessing = false  // tránh xử lý nhiều frame cùng lúc

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        if (isProcessing) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        isProcessing = true
        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                barcodes.firstOrNull { barcode ->
                    // Chỉ lấy các loại barcode phổ biến trên sản phẩm thực phẩm
                    barcode.format in listOf(
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E,
                        Barcode.FORMAT_CODE_128,
                        Barcode.FORMAT_QR_CODE
                    )
                }?.rawValue?.let { barcodeValue ->
                    Log.d("BarcodeAnalyzer", "Detected: $barcodeValue")
                    onBarcodeDetected(barcodeValue)
                }
            }
            .addOnFailureListener { e ->
                Log.e("BarcodeAnalyzer", "Scan lỗi: ${e.message}")
            }
            .addOnCompleteListener {
                isProcessing = false
                imageProxy.close()
            }
    }
}