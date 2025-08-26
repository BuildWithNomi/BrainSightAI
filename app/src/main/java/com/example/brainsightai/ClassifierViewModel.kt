package com.example.brainsightai

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ClassifierUiState(
    val predictionText: String = "Upload an MRI image to classify",
    val selectedBitmap: Bitmap? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class ClassifierViewModel(
    private val modelHandler: ModelHandler
) : ViewModel() {

    var uiState by mutableStateOf(ClassifierUiState())

    fun onImageSelected(bitmap: Bitmap) {
        uiState = uiState.copy(
            selectedBitmap = bitmap,
            predictionText = "Analyzing...",
            isLoading = true,
            error = null
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prediction = modelHandler.predict(bitmap)
                withContext(Dispatchers.Main) {
                    uiState = if (prediction != null) {
                        uiState.copy(
                            predictionText = "Prediction: ${prediction.first} (${
                                "%.2f".format(
                                    prediction.second * 100
                                )
                            }%)",
                            isLoading = false
                        )
                    } else {
                        uiState.copy(
                            predictionText = "Error during prediction.",
                            isLoading = false,
                            error = "Prediction failed."
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("ClassifierViewModel", "Prediction error: ${e.message}")
                    uiState = uiState.copy(
                        predictionText = "Error: ${e.message}",
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    companion object {
        fun Factory(modelHandler: ModelHandler): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ClassifierViewModel(modelHandler) as T
                }
            }
    }
}