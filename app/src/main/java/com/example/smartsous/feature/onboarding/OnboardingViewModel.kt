package com.example.smartsous.feature.onboarding

import com.example.smartsous.core.common.BaseViewModel
import com.example.smartsous.core.common.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : BaseViewModel() {

    suspend fun isOnboardingDone(): Boolean =
        dataStoreManager.isOnboardingDone()

    fun markOnboardingDone() {
        safeLaunch {
            dataStoreManager.markOnboardingDone()
        }
    }
}