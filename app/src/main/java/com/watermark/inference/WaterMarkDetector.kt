package com.watermark.inference

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * 水印自动检测器（规则 + 轻量模型混合策略）
 *
 * 检测策略：
 * 1. 边缘检测（Sobel）- 找出明显的边界线
 * 2. 颜色分析 - 检测大面积均匀色块（常见水印：半透明白/灰文字）
 * 3. 连通域分析 - 过滤小噪点，保留文字区域
 */
@Singleton
class WaterMarkDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * 检测图片中的水印/文字区域
     * @return List of RectF（归一化坐标 0~1）
     */
    suspend fun detect(bitmap: Bitmap): List<RectF> = withContext(Dispatchers.Default) {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        // 策略1：边缘强度图
        val edges = SobelEdgeDetector.detect(pixels, w, h)

        // 策略2：颜色均匀度分析（水印常在半透明均匀色块上）
        val colorBlocks = detectColorBlocks(pixels, w, h)

        // 合并结果，过滤小区域
        val candidates = (edges + colorBlocks)
            .filter { rect ->
                val rw = rect.right - rect.left
                val rh = rect.bottom - rect.top
                rw > 0.05f && rh > 0.01f && rw < 0.98f && rh < 0.98f
            }
            .distinctBy { "${(it.left*100).toInt()}-${(it.top*100).toInt()}" }

        candidates.take(10)  // 最多返回 10 个候选区域
    }

    /** 边缘检测：Sobel 算子，提取高边缘密度区域（水印文字边缘）*/
    private fun detectColorBlocks(pixels: IntArray, w: Int, h: Int): List<RectF> {
        // 简化为灰度 + 动态阈值找出大面积接近白色的区域
        val blocks = mutableListOf<RectF>()
        val gray = FloatArray(w * h)
        for (i in pixels.indices) {
            gray[i] = (0.299f * ((pixels[i] shr 16) and 0xFF) +
                    0.587f * ((pixels[i] shr 8) and 0xFF) +
                    0.114f * (pixels[i] and 0xFF))
        }

        // 扫描顶部/底部 15% 区域（常见水印位置）
        val scanBands = listOf(
            0f to 0.15f,    // 顶部
            0.85f to 1f,    // 底部
            0f to 1f        // 全图（中央大面积区域）
        )

        for ((topP, bottomP) in scanBands) {
            val top = (topP * h).toInt()
            val bottom = (bottomP * h).toInt()
            var inBlock = false
            var blockStart = 0

            for (y in top until bottom) {
                // 检查这一行是否接近白色（亮度>220）
                var brightCount = 0
                for (x in 0 until w step 4) {
                    if (gray[y * w + x] > 220) brightCount++
                }
                val brightRatio = brightCount.toFloat() / (w / 4)

                if (brightRatio > 0.3f && !inBlock) {
                    inBlock = true
                    blockStart = y
                } else if (brightRatio < 0.1f && inBlock) {
                    inBlock = false
                    val height = y - blockStart
                    if (height > h * 0.01f) {
                        blocks.add(RectF(0f, blockStart.toFloat() / h,
                            1f, y.toFloat() / h))
                    }
                }
            }
        }
        return blocks
    }
}

/** Sobel 边缘检测 */
object SobelEdgeDetector {
    fun detect(pixels: IntArray, w: Int, h: Int): List<RectF> {
        val gray = FloatArray(w * h)
        for (i in pixels.indices) {
            gray[i] = (0.299f * ((pixels[i] shr 16) and 0xFF) +
                    0.587f * ((pixels[i] shr 8) and 0xFF) +
                    0.114f * (pixels[i] and 0xFF))
        }

        val edge = FloatArray(w * h)
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val gx = -gray[(y - 1) * w + x - 1] + gray[(y - 1) * w + x + 1]
                        - 2 * gray[y * w + x - 1] + 2 * gray[y * w + x + 1]
                        - gray[(y + 1) * w + x - 1] + gray[(y + 1) * w + x + 1]
                val gy = -gray[(y - 1) * w + x - 1] - 2 * gray[(y - 1) * w + x]
                        - gray[(y - 1) * w + x + 1] + gray[(y + 1) * w + x - 1]
                        + 2 * gray[(y + 1) * w + x] + gray[(y + 1) * w + x + 1]
                edge[y * w + x] = kotlin.math.sqrt(gx * gx + gy * gy)
            }
        }

        // 聚合高边缘密度区域 → RectF
        val threshold = edge.average().toFloat() * 2
        val blocks = mutableListOf<RectF>()

        // 简化：取边缘最强的几个连通区域
        val grid = 8
        val gw = w / grid
        val gh = h / grid
        val scores = Array(grid) { FloatArray(grid) }

        for (gy in 0 until grid) {
            for (gx in 0 until grid) {
                var sum = 0f
                for (dy in 0 until gh) {
                    for (dx in 0 until gw) {
                        val px = gx * gw + dx
                        val py = gy * gh + dy
                        if (py < h && px < w) sum += edge[py * w + px]
                    }
                }
                scores[gy][gx] = sum / (gw * gh)
            }
        }

        // 取分数 > threshold 的网格块
        for (gy in 0 until grid) {
            for (gx in 0 until grid) {
                if (scores[gy][gx] > threshold) {
                    blocks.add(RectF(
                        gx.toFloat() / grid,
                        gy.toFloat() / grid,
                        (gx + 1).toFloat() / grid,
                        (gy + 1).toFloat() / grid
                    ))
                }
            }
        }
        return blocks
    }
}

data class RectF(
    var left: Float,
    var top: Float,
    var right: Float,
    var bottom: Float
)
