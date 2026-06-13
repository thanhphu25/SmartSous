package com.example.smartsous.feature.pantry

import com.example.smartsous.core.common.BaseViewModel
import com.example.smartsous.core.vision.YoloIngredientDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class IngredientScanViewModel @Inject constructor(
    val detector: YoloIngredientDetector
) : BaseViewModel()
