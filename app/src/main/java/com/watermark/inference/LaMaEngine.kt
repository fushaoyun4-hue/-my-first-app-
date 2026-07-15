package com.watermark.inference

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LaMa-Fourier ONNX 鎺ㄧ悊寮曟搸
 *
 * 璐熻矗鍔犺浇妯″瀷銆佸崟甯у幓姘村嵃鎺ㄧ悊
 * 鏀鎸 INT8 閲忓寲妯″瀷锛屽唴瀛樺崰鐢?< 500MB
 */
@Singleton
class LaMaEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var session: ai.onnxruntime.OrtSession? = null
    private var env: ai.onnxruntime.OrtEnvironment? = null

    // 妯″瀷杈撳叆/杈撳嚭鍚嶇О锛堥渶涓庡疄闄呮ā鍨嬩竴鑷达級
    private val inputName: String = "input"
    private val maskName: String = "mask"
    private val outputName: String = "output"

    /**
     * 鍒濆嬪寲妯″瀷锛堥栨¤皟鐢锛?     * @param modelPath assets/models/lama-fourier.onnx
     */
    suspend fun init(modelPath: String = "models/lama-fourier.onnx") = withContext(Dispatchers.IO) {
        if (session != null) return@withContext  // 宸插垵濮嬪寲

        try {
            env = ai.onnxruntime.OrtEnvironment.getEnvironment()
            val modelAsset = loadModelFromAssets(modelPath)
            session = env!!.createSession(modelAsset, ai.onnxruntime.OrtSession.SessionOptions().apply {
                // 鍚鐢 NNAPI 鍔犻燂紙Android GPU/NPU锛?                graphOptimizationLevel = ai.onnxruntime.GraphOptimizationLevel.ORT_ENABLE_ALL
            })
        } catch (e: Exception) {
            throw LaMaInitException("妯″瀷鍒濆嬪寲澶辫? ${e.message}", e)
        }
    }

    /** 浠?assets 鍔犺浇妯″瀷鏂囦欢鍒扮紦瀛?*/
    private fun loadModelFromAssets(assetPath: String): ByteArray {
        return context.assets.open(assetPath).use { it.readBytes() }
    }

    /**
     * 瀵瑰崟甯у浘鐗囪繘琛屽幓姘村嵃
     *
     * @param image 杈撳叆鍘熷浘锛圔itmap锛?     * @param masks 姘村嵃/鏂囧瓧鍖哄煙鍒楄〃锛堝潗鏍囦负鐩稿逛?image 鐨勬瘮渚?0~1锛?     * @return 淇澶嶅悗鐨 Bitmap
     */
    suspend fun inpaint(image: Bitmap, masks: List<RectF>): Bitmap = withContext(Dispatchers.Default) {
        val session = session ?: throw LaMaInitException("妯″瀷鏈鍒濆嬪寲锛岃峰厛璋冪?init()")

        // 1. 灏?masks 鍚堝苟涓哄崟閫氶亾 mask 鍥撅紙鐧借壊=闇淇澶嶅尯鍩燂?        val maskBitmap = buildMask(image.width, image.height, masks)

        // 2. 璋冩暣灏哄稿埌妯″瀷杈撳叆灏哄革紙64 鐨勫嶆暟锛?        val (w, h) = alignToModelInput(image.width, image.height)
        val resizedImage = Bitmap.createScaledBitmap(image, w, h, true)
        val resizedMask = Bitmap.createScaledBitmap(maskBitmap, w, h, true)

        // 3. 杞鎹涓?float 鏁扮粍锛堝綊涓鍖栧埌 [0,1]锛?        val imageData = bitmapToFloatArray(resizedImage, w, h)
        val maskData = bitmapToFloatArray(resizedMask, w, h)

        // 4. 鏋勫缓杈撳叆 tensor
        val inputTensor = createTensor(imageData, longArrayOf(1, 3, h.toLong(), w.toLong()))
        val maskTensor = createTensor(maskData, longArrayOf(1, 1, h.toLong(), w.toLong()))

        // 5. 鎺ㄧ悊
        val output = session.run(mapOf(inputName to inputTensor, maskName to maskTensor))
        val outputTensor = output.get(0).value
        val outputBuffer = outputTensor.floatBuffer
        val outputData = FloatArray(outputBuffer.remaining())
        outputBuffer.get(outputData)

        // 6. 鍙嶅綊涓鍖?+ 杞鎹㈠?Bitmap
        val resultBitmap = floatArrayToBitmap(outputData, w, h)

        // 7. 缂╂斁鍥炲師灏哄
        val finalResult = Bitmap.createScaledBitmap(resultBitmap, image.width, image.height, true)

        // 娓呯悊涓存椂瀵硅薄
        resizedImage.recycle()
        resizedMask.recycle()
        resultBitmap.recycle()
        maskBitmap.recycle()

        finalResult
    }

    /** 灏嗗氫?mask 鍚堝苟涓轰竴涓浜屽?mask Bitmap */
    private fun buildMask(width: Int, height: Int, masks: List<RectF>): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            style = android.graphics.Paint.Style.FILL
        }
        for (m in masks) {
            val left = (m.left * width).toInt().coerceIn(0, width)
            val top = (m.top * height).toInt().coerceIn(0, height)
            val right = (m.right * width).toInt().coerceIn(left, width)
            val bottom = (m.bottom * height).toInt().coerceIn(top, height)
            canvas.drawRect(
                left.toFloat(), top.toFloat(),
                right.toFloat(), bottom.toFloat(),
                paint
            )
        }
        return bitmap
    }

    /** 灏?Bitmap 杞涓哄綊涓鍖?float 鏁扮粍 [BCHW 鏍煎紡] */
    private fun bitmapToFloatArray(bitmap: Bitmap, w: Int, h: Int): FloatArray {
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val data = FloatArray(w * h * 3)

        for (i in pixels.indices) {
            val r = (pixels[i] shr 16 and 0xFF) / 255f
            val g = (pixels[i] shr 8 and 0xFF) / 255f
            val b = (pixels[i] and 0xFF) / 255f
            data[i]             = r          // R
            data[w * h + i]     = g          // G
            data[w * h * 2 + i] = b          // B
        }
        return data
    }

    /** 灏嗘ā鍨嬭緭鍑?float 鏁扮粍杞鍥 Bitmap */
    private fun floatArrayToBitmap(data: FloatArray, w: Int, h: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        for (i in pixels.indices) {
            val r = (data[i].coerceIn(0f, 1f) * 255).toInt()
            val g = (data[w * h + i].coerceIn(0f, 1f) * 255).toInt()
            val b = (data[w * h * 2 + i].coerceIn(0f, 1f) * 255).toInt()
            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return bitmap
    }

    /** ONNX float tensor 鍒涘缓 */
    private fun createTensor(data: FloatArray, shape: LongArray): ai.onnxruntime.OnnxTensor {
        val envRef = env ?: throw IllegalStateException()
        val buffer = java.nio.FloatBuffer.wrap(data)
        return ai.onnxruntime.OnnxTensor.createTensor(envRef, buffer, shape)
    }

    /** 璋冩暣灏哄稿?64 鐨勫嶆暟锛圤NNX 妯″瀷瑕佹眰锛?*/
    private fun alignToModelInput(w: Int, h: Int): Pair<Int, Int> {
        val align = 64
        return Pair((w + align - 1) / align * align, (h + align - 1) / align * align)
    }

    fun release() {
        session?.close()
        env?.close()
        session = null
        env = null
    }
}

class LaMaInitException(msg: String, cause: Throwable? = null) : Exception(msg, cause)
