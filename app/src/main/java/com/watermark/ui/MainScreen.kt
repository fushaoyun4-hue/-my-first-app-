package com.watermark.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.watermark.ui.theme.BrandBlue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

// ── State ──────────────────────────────────────────────────────────────────

data class ImageState(
    val imageUri: Uri? = null,
    val resultUri: Uri? = null,
    val masks: List<MaskRect> = emptyList(),
    val isProcessing: Boolean = false,
    val isAutoDetecting: Boolean = false,
    val progress: Int = 0,
    val error: String? = null,
)

data class VideoState(
    val videoUri: Uri? = null,
    val frameUris: List<Uri> = emptyList(),
    val resultVideoUri: Uri? = null,
    val isProcessing: Boolean = false,
    val currentFrame: Int = 0,
    val totalFrames: Int = 0,
    val progress: Int = 0,
    val isExtracting: Boolean = false,
    val isEncoding: Boolean = false,
    val error: String? = null,
)

data class MaskRect(
    val id: Int,
    val left: Float, val top: Float,
    val right: Float, val bottom: Float  // 0~1 比例
)

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

    private val _imageState = MutableStateFlow(ImageState())
    val imageState: StateFlow<ImageState> = _imageState.asStateFlow()

    private val _videoState = MutableStateFlow(VideoState())
    val videoState: StateFlow<VideoState> = _videoState.asStateFlow()

    private val _activeTab = MutableStateFlow(0)
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    fun setImage(uri: Uri) {
        _imageState.value = _imageState.value.copy(imageUri = uri, resultUri = null, masks = emptyList(), error = null)
        // TODO: 触发自动检测水印
        detectWatermarks(uri)
    }

    fun setVideo(uri: Uri) {
        _videoState.value = _videoState.value.copy(videoUri = uri, resultVideoUri = null, error = null)
        // TODO: 抽帧预览
    }

    private fun detectWatermarks(uri: Uri) {
        viewModelScope.launch {
            _imageState.value = _imageState.value.copy(isAutoDetecting = true)
            // TODO: 调用检测模型，示例用占位坐标
            kotlinx.coroutines.delay(1500)
            _imageState.value = _imageState.value.copy(
                isAutoDetecting = false,
                masks = listOf(
                    MaskRect(0, 0.05f, 0.05f, 0.95f, 0.12f)  // 示例：水印在顶部
                )
            )
        }
    }

    fun addMask(mask: MaskRect) {
        _imageState.value = _imageState.value.copy(
            masks = _imageState.value.masks + mask
        )
    }

    fun removeMask(id: Int) {
        _imageState.value = _imageState.value.copy(
            masks = _imageState.value.masks.filter { it.id != id }
        )
    }

    fun processImage() {
        val state = _imageState.value
        if (state.imageUri == null || state.masks.isEmpty()) return
        viewModelScope.launch {
            _imageState.value = state.copy(isProcessing = true, progress = 0, error = null)
            try {
                // TODO: 调用 LaMaEngine.inpaint()
                kotlinx.coroutines.delay(3000)  // 模拟处理
                _imageState.value = _imageState.value.copy(
                    isProcessing = false,
                    progress = 100,
                    resultUri = state.imageUri  // 替换为实际结果
                )
            } catch (e: Exception) {
                _imageState.value = _imageState.value.copy(isProcessing = false, error = e.message)
            }
        }
    }

    fun setTab(index: Int) {
        _activeTab.value = index
    }
}

// ── Navigation ───────────────────────────────────────────────────────────────

@Composable
fun WatermarkNavHost() {
    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf("图片", "视频", "设置").forEachIndexed { index, title ->
                    NavigationBarItem(
                        icon = {
                            when (index) {
                                0 -> Icon(Icons.Default.Image, contentDescription = title)
                                1 -> Icon(Icons.Default.VideoLibrary, contentDescription = title)
                                else -> Icon(Icons.Default.Settings, contentDescription = title)
                            }
                        },
                        label = { Text(title) },
                        selected = false,
                        onClick = {}
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            ImageTab()
        }
    }
}

// ── 图片 Tab ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageTab(viewModel: MainViewModel = hiltViewModel()) {
    val state by viewModel.imageState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "图片去水印",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 预览区域
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    2.dp,
                    if (state.imageUri != null) BrandBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (state.imageUri != null) {
                var showMaskEditor by remember { mutableStateOf(true) }

                AsyncImage(
                    model = ImageRequest.Builder(context).data(state.imageUri).crossfade(true).build(),
                    contentDescription = "待处理图片",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // Mask 覆盖层（可选：简化为结果预览模式）
                if (showMaskEditor && state.masks.isNotEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .pointerInput(state.masks) {
                                // 简化：显示已检测区域边框
                            }
                    )
                }

                // 处理中遮罩
                if (state.isProcessing) {
                    Box(
                        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(Modifier.height(12.dp))
                            Text("去水印中...", color = Color.White)
                        }
                    }
                }

                // 拖动添加 mask
                var dragStart by remember { mutableStateOf<Offset?>(null) }
                Box(
                    Modifier.fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    dragStart = offset
                                },
                                onDragEnd = {
                                    dragStart = null
                                },
                                onDrag = { change, _ ->
                                    val start = dragStart ?: return@detectDragGestures
                                    val pos = change.position
                                    // 添加一个 mask（归一化到 0~1）
                                    viewModel.addMask(
                                        MaskRect(
                                            id = System.currentTimeMillis().toInt(),
                                            left = (minOf(start.x, pos.x) / size.width),
                                            top = (minOf(start.y, pos.y) / size.height),
                                            right = (maxOf(start.x, pos.x) / size.width),
                                            bottom = (maxOf(start.y, pos.y) / size.height)
                                        )
                                    )
                                    dragStart = pos
                                }
                            )
                        }
                )
            } else {
                // 导入提示
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddPhotoAlternate, null,
                        modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(8.dp))
                    Text("点击下方按钮导入图片", color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 已检测水印标签
        if (state.masks.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("已检测到 ${state.masks.size} 处水印/文字",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(Modifier.height(12.dp))

        // 操作按钮行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 导入按钮
            OutlinedButton(
                onClick = { /* TODO: 触发 SAF pick */ },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PhotoLibrary, null)
                Spacer(Modifier.width(4.dp))
                Text("导入图片")
            }

            // 去水印按钮
            Button(
                onClick = { viewModel.processImage() },
                enabled = state.imageUri != null && state.masks.isNotEmpty() && !state.isProcessing,
                modifier = Modifier.weight(1f)
            ) {
                if (state.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White, strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.AutoFixHigh, null)
                }
                Spacer(Modifier.width(4.dp))
                Text(if (state.isProcessing) "处理中" else "去水印")
            }
        }

        // 导出按钮
        if (state.resultUri != null) {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { /* TODO: 保存到相册 */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(Icons.Default.SaveAlt, null)
                Spacer(Modifier.width(4.dp))
                Text("导出图片")
            }
        }

        // 错误提示
        state.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }
    }
}
