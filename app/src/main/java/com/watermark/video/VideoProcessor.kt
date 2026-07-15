package com.watermark.video

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileDescriptor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 视频处理引擎（FFmpeg 封装）
 *
 * 处理流程：
 * 1. 抽帧（FFmpeg extract frames）
 * 2. 逐帧传入 LaMaEngine 去水印
 * 3. 合成回视频（FFmpeg encode）
 */
@Singleton
class VideoProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val ffmpegBin: String by lazy {
        // assets/ffmpeg/ffmpeg 二进制路径
        File(context.filesDir, "ffmpeg/ffmpeg").absolutePath
    }

    /**
     * 抽帧 - 从视频提取所有帧
     * @param videoUri 视频文件 Uri
     * @param outputDir 输出目录（每帧图片）
     * @param fps 每秒抽帧数（默认 1，保持水印覆盖足够帧）
     * @return 帧文件列表（顺序按文件名排序）
     */
    suspend fun extractFrames(
        videoUri: Uri,
        outputDir: File,
        fps: Int = 1
    ): List<File> = withContext(Dispatchers.IO) {
        outputDir.deleteRecursively()
        outputDir.mkdirs()

        val framePattern = File(outputDir, "frame_%04d.png").absolutePath

        // FFmpeg 抽帧命令
        val cmd = listOf(
            ffmpegBin,
            "-i", getFilePath(videoUri) ?: videoUri.toString(),
            "-vf", "fps=$fps",
            "-q:v", "2",          // PNG 质量
            framePattern,
            "-y"                 // 覆盖输出
        )

        val result = runFFmpeg(cmd)
        if (!result) throw VideoProcessException("抽帧失败")

        outputDir.listFiles()
            ?.filter { it.extension == "png" }
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    /**
     * 合成视频 - 将帧序列合成为 MP4
     * @param frames 帧图片列表（顺序）
     * @param outputFile 输出视频文件
     * @param originalVideoUri 参考原视频（用于复制编码参数）
     * @param fps 输出帧率
     */
    suspend fun encodeVideo(
        frames: List<File>,
        outputFile: File,
        originalVideoUri: Uri,
        fps: Int = 30
    ): File = withContext(Dispatchers.IO) {
        if (frames.isEmpty()) throw VideoProcessException("没有帧可合成")

        // 构造 concat list（FFmpeg concat demuxer 需要）
        val listFile = File(context.cacheDir, "frames.txt")
        listFile.writeText(frames.joinToString("\n") { "file '${it.absolutePath}'" })

        val cmd = listOf(
            ffmpegBin,
            "-f", "concat",
            "-safe", "0",
            "-i", listFile.absolutePath,
            "-i", getFilePath(originalVideoUri) ?: originalVideoUri.toString(),
            "-c:v", "libx264",
            "-preset", "fast",
            "-crf", "23",
            "-c:a", "aac",
            "-shortest",
            outputFile.absolutePath,
            "-y"
        )

        val result = runFFmpeg(cmd)
        if (!result) throw VideoProcessException("视频合成失败")

        outputFile
    }

    /**
     * 带进度的逐帧处理 Flow
     * @param frames 帧文件列表
     * @param processBlock  每帧处理 lambda，返回修复后帧 Bitmap
     */
    fun processFramesWithProgress(
        frames: List<File>,
        processBlock: suspend (File, Int, Int) -> Bitmap
    ): Flow<FrameProgress> = flow {
        val total = frames.size
        val outputDir = File(context.cacheDir, "processed_frames").apply { mkdirs() }

        frames.forEachIndexed { index, frameFile ->
            val processed = processBlock(frameFile, index, total)
            val outFile = File(outputDir, "out_${String.format("%04d", index)}.png")
            processed.compress(Bitmap.CompressFormat.PNG, 100, outFile.outputStream())
            processed.recycle()

            emit(FrameProgress(
                current = index + 1,
                total = total,
                percent = ((index + 1) * 100 / total).toInt(),
                frameFile = outFile
            ))
        }

        emit(FrameProgress(current = total, total = total, percent = 100,
            frameFile = null, done = true, outputDir = outputDir))
    }.flowOn(Dispatchers.Default)

    private fun runFFmpeg(cmd: List<String>): Boolean {
        return try {
            val process = ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start()

            // 消费输出（避免缓冲阻塞）
            process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            process.exitValue() == 0
        } catch (e: Exception) {
            false
        }
    }

    private fun getFilePath(uri: Uri): String? {
        // 通过 ContentResolver 获取真实文件路径
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                pfd.statSize
                null  // Content Uri，FFmpeg 可直接用 uri
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}

data class FrameProgress(
    val current: Int,
    val total: Int,
    val percent: Int,
    val frameFile: File? = null,
    val done: Boolean = false,
    val outputDir: File? = null
)

class VideoProcessException(msg: String) : Exception(msg)
