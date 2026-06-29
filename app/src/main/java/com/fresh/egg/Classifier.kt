package com.fresh.egg

import android.content.Context
import android.graphics.Bitmap
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class Classifier(private val context: Context) {
    private var module: Module? = null
    
    // Nama kelas berdasarkan model: indeks 0 adalah 'busuk', indeks 1 adalah 'segar'
    private val classNames = arrayOf("busuk", "segar")

    init {
        try {
            val modelPath = assetFilePath(context, "egg_classifier.pt")
            module = LiteModuleLoader.load(modelPath)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Menjalankan analisis klasifikasi telur pada bitmap yang diberikan.
     * Alur prapemrosesan:
     * 1. Ubah ukuran (Resize) ke 256x256.
     * 2. Potong bagian tengah (Center Crop) ke 224x224.
     * 3. Normalisasi: skala piksel ke [0.0, 1.0], lalu Mean [0.485, 0.456, 0.406] & Std [0.229, 0.224, 0.225].
     * Mengembalikan Pair berisi label kelas ("segar" atau "busuk") dan nilai tingkat kepercayaan (confidence).
     */
    fun classify(bitmap: Bitmap): Pair<String, Float>? {
        val localModule = module ?: return null
        
        try {
            // 1. Resize ke 256x256
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
            
            // 2. Center Crop ke 224x224 (ambil tengah dengan margin 16 piksel dari tiap sisi)
            val croppedBitmap = Bitmap.createBitmap(resizedBitmap, 16, 16, 224, 224)

            // 3. Konversi Bitmap ke PyTorch Tensor dengan normalisasi ImageNet
            val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
                croppedBitmap,
                floatArrayOf(0.485f, 0.456f, 0.406f), // Rata-rata (Mean)
                floatArrayOf(0.229f, 0.224f, 0.225f)  // Standar Deviasi (Std)
            )

            // 4. Jalankan inferensi model
            val outputTensor = localModule.forward(IValue.from(inputTensor)).toTensor()
            val scores = outputTensor.dataAsFloatArray

            // 5. Hitung probabilitas menggunakan softmax untuk tingkat kepercayaan
            val probabilities = softmax(scores)
            
            // Cari indeks kelas dengan probabilitas tertinggi
            var maxIdx = 0
            var maxScore = probabilities[0]
            for (i in 1 until probabilities.size) {
                if (probabilities[i] > maxScore) {
                    maxScore = probabilities[i]
                    maxIdx = i
                }
            }

            return Pair(classNames[maxIdx], maxScore)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun softmax(logits: FloatArray): FloatArray {
        var maxLogit = Float.NEGATIVE_INFINITY
        for (logit in logits) {
            if (logit > maxLogit) maxLogit = logit
        }
        
        var sum = 0.0f
        val expLogits = FloatArray(logits.size)
        for (i in logits.indices) {
            expLogits[i] = Math.exp((logits[i] - maxLogit).toDouble()).toFloat()
            sum += expLogits[i]
        }
        
        for (i in expLogits.indices) {
            expLogits[i] /= sum
        }
        return expLogits
    }

    companion object {
        @Throws(IOException::class)
        fun assetFilePath(context: Context, assetName: String): String {
            val file = File(context.filesDir, assetName)
            if (file.exists() && file.length() > 0) {
                return file.absolutePath
            }
            context.assets.open(assetName).use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    val buffer = ByteArray(4 * 1024)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                    }
                    outputStream.flush()
                }
                return file.absolutePath
            }
        }
    }
}
