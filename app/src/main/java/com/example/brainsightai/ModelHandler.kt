package com.example.brainsightai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class ModelHandler(private val context: Context) {

    private var module: Module? = null
    val classNames = arrayOf("glioma", "meningioma", "notumor", "pituitary")

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            val modelPath = assetFilePath("brain_tumor_model.pt")
            module = Module.load(modelPath)
            Log.d("ModelHandler", "Model loaded successfully!")
        } catch (e: Exception) {
            Log.e("ModelHandler", "Error loading model: ${e.message}")
        }
    }

    private fun assetFilePath(assetName: String): String {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }

        context.assets.open(assetName).use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return file.absolutePath
    }

    fun predict(bitmap: Bitmap): Pair<String, Float>? {
        if (module == null) {
            Log.e("ModelHandler", "Model not loaded, cannot predict.")
            return null
        }

        val floatBuffer = preprocessImage(bitmap)
        val inputTensor = Tensor.fromBlob(floatBuffer, longArrayOf(1, 1, 64, 64))
        val outputTensor = module?.forward(IValue.from(inputTensor))?.toTensor()
        val scores = outputTensor?.dataAsFloatArray

        if (scores != null) {
            val probabilities = softmax(scores)
            val predictedClassIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1
            val confidence = probabilities[predictedClassIndex]
            return classNames[predictedClassIndex] to confidence
        }
        return null
    }

    private fun preprocessImage(bitmap: Bitmap): FloatBuffer {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 64, 64, true)
        val byteBuffer = ByteBuffer.allocateDirect(64 * 64 * 4) // 4 bytes per float
        byteBuffer.order(ByteOrder.nativeOrder())
        val floatBuffer = byteBuffer.asFloatBuffer()
        val pixels = IntArray(64 * 64)
        resizedBitmap.getPixels(pixels, 0, 64, 0, 0, 64, 64)
        val mean = 0.5f
        val std = 0.5f

        for (pixel in pixels) {
            val grayValue = (pixel shr 16) and 0xFF
            floatBuffer.put((grayValue / 255.0f - mean) / std)
        }
        floatBuffer.rewind()
        return floatBuffer
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.maxOrNull() ?: 0f
        val expValues = logits.map { kotlin.math.exp(it - maxLogit) }.toFloatArray()
        val sumExp = expValues.sum()
        return expValues.map { it / sumExp }.toFloatArray()
    }
}