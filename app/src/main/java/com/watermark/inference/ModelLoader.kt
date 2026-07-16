package com.watermark.inference

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 模型加载器：支持从 assets 和远程 URL 加载 AI 模型
 *
 * 模型文件（.onnx / .tflite）通常 50~500MB，
 * 放在 assets 会导致 APK 过大，推荐策略：
 *
 * 1. 首次运行时从 HuggingFace Hub 下载到 filesDir
 * 2. 后续直接从本地加载（秒开）
 * 3. 如果 assets 内置了模型（开发阶段），优先读 assets
 *
 * 模型下载进度通过 Flow 暴露给 UI 层。
 */
@Singleton
class ModelLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val modelsDir: File by lazy {
        File(context.filesDir, "models").also { it.mkdirs() }
    }

    /**
     * 确保模型已下载，返回本地 File 路径
     * 如果模型已在本地则跳过下载，直接返回路径
     *
     * @param modelName  模型文件名，如 "lama-fourier.onnx"
     * @param remoteUrl  远程下载地址（支持 HuggingFace / 直链）
     * @param expectedBytes  预期文件大小（用于校验，可为 null）
     */
    suspend fun getModel(
        modelName: String,
        remoteUrl: String,
        expectedBytes: Long? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        val localFile = File(modelsDir, modelName)

        // 已存在则直接使用
        if (localFile.exists() && localFile.length() > 1024) {
            // 简单校验：文件非空
            if (expectedBytes == null || localFile.length() >= expectedBytes * 9 / 10) {
                return@withContext Result.success(localFile)
            }
            // 大小不符，重新下载
            localFile.delete()
        }

        // 下载模型（带进度）
        downloadWithProgress(remoteUrl, localFile, expectedBytes)
            .collect { /* 进度在 flow 中已暴露给 UI */ }

        if (!localFile.exists() || localFile.length() < 1024) {
            Result.failure(ModelDownloadException("模型下载失败: $modelName"))
        } else {
            Result.success(localFile)
        }
    }

    /**
     * 从 assets 目录加载模型（开发阶段 / 小模型）
     * @return ByteArray（内存中），大模型不适用
     */
    suspend fun loadFromAssets(assetPath: String): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            try {
                val bytes = context.assets.open(assetPath).use { it.readBytes() }
                Result.success(bytes)
            } catch (e: Exception) {
                Result.failure(ModelLoadException("assets 加载失败: ${e.message}", e))
            }
        }

    /**
     * 检查本地模型是否存在且完整
     */
    fun isModelReady(modelName: String): Boolean {
        val file = File(modelsDir, modelName)
        return file.exists() && file.length() > 1024
    }

    /**
     * 下载文件并实时推送进度（0~100）
     * 使用 Flow 暴露下载进度，不阻塞主线程
     */
    private fun downloadWithProgress(
        url: String,
        outputFile: File,
        expectedBytes: Long?
    ): Flow<DownloadProgress> = flow {
        emit(DownloadProgress(0, "正在连接..."))

        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 60_000
            connection.connect()

            val totalBytes = expectedBytes ?: connection.contentLengthLong.coerceAtLeast(1L)
            val input = connection.inputStream.buffered()
            val output = FileOutputStream(outputFile)

            var downloaded = 0L
            val buffer = ByteArray(8192)
            var bytesRead: Int

            emit(DownloadProgress(0, "开始下载模型..."))

            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                downloaded += bytesRead
                val percent = ((downloaded * 100) / totalBytes).toInt().coerceIn(0, 100)
                val mb = downloaded / 1_048_576
                val totalMb = totalBytes / 1_048_576
                emit(DownloadProgress(percent, "下载中 ${mb}/${totalMb} MB"))
            }

            output.close()
            emit(DownloadProgress(100, "下载完成"))
        } finally {
            connection.disconnect()
        }
    }

    /** 获取本地模型文件路径（模型已存在时）*/
    fun getLocalPath(modelName: String): File? {
        val file = File(modelsDir, modelName)
        return if (file.exists() && file.length() > 1024) file else null
    }

    /** 删除本地模型（节省空间 / 强制重新下载）*/
    fun deleteModel(modelName: String): Boolean {
        return File(modelsDir, modelName).delete()
    }

    /** 列出本地所有模型*/
    fun listLocalModels(): List<File> {
        return modelsDir.listFiles()?.filter { it.length() > 1024 } ?: emptyList()
    }
}

// ── 数据类 ──────────────────────────────────────────────────────────────────

data class DownloadProgress(
    val percent: Int,     // 0~100
    val message: String   // 状态描述
)

class ModelDownloadException(msg: String) : Exception(msg)
class ModelLoadException(msg: String, cause: Throwable? = null) : Exception(msg, cause)
