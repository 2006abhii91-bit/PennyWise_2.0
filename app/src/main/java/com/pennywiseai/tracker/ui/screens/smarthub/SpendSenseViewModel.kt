package com.pennywiseai.tracker.ui.screens.smarthub

import android.app.Application
import android.util.Log


import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.repository.SpendSenseRepository
import com.pennywiseai.tracker.domain.model.SpendSenseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for SpendSense dashboard
 * Handles UI state and data processing for financial insights
 */
@HiltViewModel
class SpendSenseViewModel @Inject constructor(
    application: Application,
    private val spendSenseRepository: SpendSenseRepository
) : AndroidViewModel(application) {
    
    private val TAG = "SpendSenseViewModel"
    


    // UI State
    private val _uiState = MutableStateFlow(SpendSenseUiState())
    val uiState: StateFlow<SpendSenseUiState> = _uiState.asStateFlow()
    
    init {
        // Start observing SpendSense data when ViewModel is created
        observeSpendSenseData()
        // Initial data load
        refreshData()
    }
    
    /**
     * Observe SpendSense data from repository
     */
    private fun observeSpendSenseData() {
        viewModelScope.launch {
            try {
                spendSenseRepository.getSpendSenseResultFlow()
                    .collect { result ->
                        _uiState.value = _uiState.value.copy(
                            spendSenseResult = result,
                            isLoading = false,
                            error = null
                        )
                        Log.d(TAG, "SpendSense data updated: ${result.thisMonth.income}")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing SpendSense data", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load financial data"
                )
            }
        }
    }
    
    /**
     * Refresh SpendSense data by processing new SMS messages
     */
    fun refreshData() {
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Process new SMS messages
                val processedCount = spendSenseRepository.processNewSmsMessages()
                Log.d(TAG, "Refresh completed. Processed $processedCount transactions")
                
                // Repository will automatically update the flow with new data
                
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing SpendSense data", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to refresh data: ${e.message}"
                )
            } finally {
                _isRefreshing.value = false
            }
        }
    }
    
    /**
     * Manual refresh trigger for UI
     */
    fun onRefreshTriggered() {
        refreshData()
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}


/**
 * UI State for SpendSense dashboard
 */
data class SpendSenseUiState(
    val spendSenseResult: SpendSenseResult? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val hasError: Boolean
        get() = error != null
    
    val isDataEmpty: Boolean
        get() = spendSenseResult == null
}

