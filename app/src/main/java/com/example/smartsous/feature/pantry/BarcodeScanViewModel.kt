package com.example.smartsous.feature.pantry

import com.example.smartsous.core.common.BaseViewModel
import com.example.smartsous.core.network.FoodLookupService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BarcodeScanViewModel @Inject constructor(
    private val foodLookupService: FoodLookupService
) : BaseViewModel() {

    // Tra tên sản phẩm từ barcode — trả về tên hoặc barcode nếu không tìm thấy
    suspend fun lookupProduct(barcode: String): String =
        foodLookupService.lookupProductName(barcode)
}