package com.watermark.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.watermark.inference.BatteryMonitor
import com.watermark.inference.BatteryInfo
import com.watermark.inference.MaskRect
import com.watermark.inference.VideoProgress
import com.watermark.inference.WatermarkRepository
import com.watermark.video.VideoProcessor
import com.watermark.video.VideoProcessException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

// ── Image State ───────────────────────────────────────────────────────────────

data class ImageState(
    val imageUri: Uri? = null,
    val resultUri: Uri? = null,
    val masks: List<MaskRect> = emptyList(),
    val isProcessing: Boolean = false,
    val isAutoDetecting: Boolean = false,
    val progress: Int = 0,
    val error: String? = null,
)

data class MaskRect(
    val id: Int,
    val left: Float, val top: Float,
    val right: Float, val bottom: Float  // 0~1 比例
)

// ── Video State ───────────────────────────────────────────────────────────────

data class VideoState(
    val videoUri: Uri? = null,
    val frameUris: List<Uri> = emptyList(),
    val resultVideoUri: Uri? = null,
    val isExtracting: Boolean = false,
    val isProcessing: Boolean = false,
    val isEncoding: Boolean = false,
    val phase: String = "",
    val progress: Int = 0,
    val error: String? = null,
    val resultFile: File? = null,
)

// ── Battery Warning ─────────────────────────────────────────────────────────

data class BatteryWarning(
    val show: Boolean = false,
    val message: String = "",
    val isCharging: Boolean = false
)

// ── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val repository: WatermarkRepository,
    private val batteryMonitor: BatteryMonitor,
    private val videoProcessor: VideoProcessor
) : ViewModel() {

    private val _imageState = MutableStateFlow(ImageState())
    val imageState: StateFlow<ImageState> = _imageState.asStateFlow()

    private val _videoState = MutableStateFlow(VideoState())
    val videoState: StateFlow<VideoState> = _videoState.asStateFlow()

    private val _activeTab = MutableStateFlow(0)
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    private val _batteryWarning = MutableStateFlow(BatteryWarning())
    val batteryWarning: StateFlow<BatteryWarning> = _batteryWarning.asStateFlow()

    // ── Image ────────────────────────────────────────────────────────────────

    fun setImage(uri: Uri) {
        _imageState.value = _imageState.value.copy(
            imageUri = uri, resultUri = null, masks = emptyList(), error = null
        )
        detectWatermarks(uri)
    }

    private fun detectWatermarks(uri: Uri) {
        viewModelScope.launch {
            _imageState.value = _imageState.value.copy(isAutoDetecting = true)
            repository.detectWatermarks(uri)
                .onSuccess { masks ->
                    _imageState.value = _imageState.value.copy(
                        isAutoDetecting = false,
                        masks = masks.mapIndexed { i, rectF ->
                            MaskRect(i, rectF.left, rectF.top, rectF.right, rectF.bottom)
                        }
                    )
                }
                .onFailure {
                    _imageState.value = _imageState.value.copy(
                        isAutoDetecting = false,
                        // 检测失败不影响使用，用户可手动画区域
                    )
                }
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

            // ── 电池检查 ────────────────────────────────────────────
            val battery = batteryMonitor.getBatteryInfo()
            if (!battery.canProceed) {
                _imageState.value = _imageState.value.copy(
                    isProcessing = false,
                    error = "电量不足（${battery.level}%）请连接充电器后重试"
                )
                return@launch
            }

            val rectFList = state.masks.map {
                com.watermark.inference.RectF(it.left, it.top, it.right, it.bottom)
            }

            repository.processImage(state.imageUri, rectFList)
                .onSuccess { file ->
                    _imageState.value = _imageState.value.copy(
                        isProcessing = false,
                        progress = 100,
                        resultUri = Uri.fromFile(file)
                    )
                }
                .onFailure { e ->
                    _imageState.value = _imageState.value.copy(
                        isProcessing = false,
                        error = e.message
                    )
                }
        }
    }

    // ── Video ────────────────────────────────────────────────────────────────

    fun setVideo(uri: Uri) {
        _videoState.value = _videoState.value.copy(
            videoUri = uri, resultVideoUri = null, error = null,
            resultFile = null, frameUris = emptyList()
        )
        // 电池检查 + 抽帧预览
        viewModelScope.launch {
            _videoState.value = _videoState.value.copy(isExtracting = true, phase = "准备中...")
            try {
                // 先检查电池
                val battery = batteryMonitor.getBatteryInfo()
                if (!battery.canProceed) {
                    _videoState.value = _videoState.value.copy(
                        isExtracting = false,
                        error = "电量不足（${battery.level}%）请连接充电器"
                    )
                    return@launch
                }

                val cacheDir = File(context.cacheDir, "preview_${System.currentTimeMillis()}").apply {
                    mkdirs()
                }
                val frames = videoProcessor.extractFrames(uri, cacheDir)
                val frameUriList = frames.take(10).map { Uri.fromFile(it) }

                _videoState.value = _videoState.value.copy(
                    isExtracting = false,
                    frameUris = frameUriList,
                    phase = "已提取 ${frames.size} 帧"
                )
            } catch (e: Exception) {
                _videoState.value = _videoState.value.copy(
                    isExtracting = false,
                    error = "抽帧失败: ${e.message}"
                )
            }
        }
    }

    fun processVideo(mask: MaskRect? = null) {
        val state = _videoState.value
        if (state.videoUri == null) return

        viewModelScope.launch {
            _videoState.value = state.copy(
                isProcessing = true, error = null, progress = 0
            )

            val rectFList = if (mask != null) {
                listOf(com.watermark.inference.RectF(
                    mask.left, mask.top, mask.right, mask.bottom
                ))
            } else {
                emptyList()
            }

            repository.processVideo(state.videoUri, rectFList)
                .collect { prog ->
                    when (prog) {
                        is VideoProgress.Phase -> {
                            _videoState.value = _videoState.value.copy(
                                phase = prog.message,
                                progress = prog.percent
                            )
                        }
                        is VideoProgress.Done -> {
                            _videoState.value = _videoState.value.copy(
                                isProcessing = false,
                                isEncoding = false,
                                progress = 100,
                                phase = "完成！",
                                resultFile = prog.outputFile,
                                resultVideoUri = Uri.fromFile(prog.outputFile)
                            )
                        }
                        is VideoProgress.Error -> {
                            _videoState.value = _videoState.value.copy(
                                isProcessing = false,
                                isEncoding = false,
                                error = prog.message
                            )
                        }
                    }
                }
        }
    }

    // ── Tab ──────────────────────────────────────────────────────────────────

    fun setTab(index: Int) {
        _activeTab.value = index
    }

    fun dismissBatteryWarning() {
        _batteryWarning.value = BatteryWarning(show = false)
    }
}

// ── UI ───────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatermarkNavHost(viewModel: MainViewModel = hiltViewModel()) {
    val activeTab by viewModel.activeTab.collectAsState()
    val batteryWarning by viewModel.batteryWarning.collectAsState()

    // 电池警告弹窗
    if (batteryWarning.show) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissBatteryWarning() },
            icon = { Icon(Icons.Default.BatteryAlert, null, tint = Color(0xFFE65100)) },
            title = { Text("电量不足") },
            text = { Text(batteryWarning.message) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissBatteryWarning() }) {
                    Text("知道了")
                }
            },
            dismissButton = {
                if (!batteryWarning.isCharging) {
                    TextButton(onClick = { viewModel.dismissBatteryWarning() }) {
                        Text("连接充电器")
                    }
                }
            }
        )
    }

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
                        selected = activeTab == index,
                        onClick = { viewModel.setTab(index) }
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (activeTab) {
                0 -> ImageTab(viewModel = viewModel)
                1 -> VideoTab(viewModel = viewModel)
                2 -> SettingsTab(viewModel = viewModel)
            }
        }
    }
}

// ── 图片 Tab ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageTab(viewModel: MainViewModel) {
    val state by viewModel.imageState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // 图片选择器
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.setImage(it) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
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
                .weight(1f, fill = false)
                .fillMaxWidth()
                .heightIn(min = 240.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    2.dp,
                    if (state.imageUri != null) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (state.imageUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(state.imageUri).crossfade(true).build(),
                    contentDescription = "待处理图片",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // 自动检测中
                if (state.isAutoDetecting) {
                    Box(
                        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(Modifier.height(8.dp))
                            Text("正在检测水印...", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }

                // 处理中遮罩
                if (state.isProcessing) {
                    Box(
                        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = Color.White, strokeWidth = 3.dp
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("去水印中...", color = Color.White)
                            if (state.progress > 0) {
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { state.progress / 100f },
                                    modifier = Modifier.width(160.dp),
                                    color = Color.White, trackColor = Color.White.copy(alpha = 0.3f)
                                )
                            }
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
                                    viewModel.addMask(
                                        MaskRect(
                                            id = System.currentTimeMillis().toInt(),
                                            left = (minOf(start.x, pos.x) / size.width).coerceIn(0f, 1f),
                                            top = (minOf(start.y, pos.y) / size.height).coerceIn(0f, 1f),
                                            right = (maxOf(start.x, pos.x) / size.width).coerceIn(0f, 1f),
                                            bottom = (maxOf(start.y, pos.y) / size.height).coerceIn(0f, 1f)
                                        )
                                    )
                                    dragStart = pos
                                }
                            )
                        }
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.AddPhotoAlternate, null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "点击下方按钮导入图片",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 已检测水印标签
        if (state.masks.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Default.CheckCircle, null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "已 ${state.masks.size} 处水印/文字",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // 操作按钮行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PhotoLibrary, null)
                Spacer(Modifier.width(4.dp))
                Text("导入图片")
            }

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
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp
            )
        }
    }
}

// ── 视频 Tab ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoTab(viewModel: MainViewModel) {
    val state by viewModel.videoState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.setVideo(it) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "视频去水印",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // ── 视频预览帧缩略图 ──────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    2.dp,
                    if (state.videoUri != null) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (state.frameUris.isNotEmpty()) {
                // 取第一帧作为封面
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(state.frameUris.first())
                        .crossfade(true).build(),
                    contentDescription = "视频首帧",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // 抽帧中
                if (state.isExtracting) {
                    Box(
                        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(Modifier.height(8.dp))
                            Text(state.phase, color = Color.White, fontSize = 13.sp)
                        }
                    }
                }

                // 处理中
                if (state.isProcessing) {
                    Box(
                        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = Color.White, strokeWidth = 3.dp
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(state.phase, color = Color.White, fontSize = 13.sp)
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { state.progress / 100f },
                                modifier = Modifier.width(200.dp),
                                color = Color.White, trackColor = Color.White.copy(alpha = 0.3f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${state.progress}%",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.VideoLibrary, null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "点击下方按钮导入视频",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // ── 帧缩略图列表 ──────────────────────────────────────────
        if (state.frameUris.isNotEmpty() && !state.isExtracting) {
            Spacer(Modifier.height(8.dp))
            Text(
                "预览帧（共 ${state.frameUris.size} 帧）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(state.frameUris) { uri ->
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(uri).build(),
                        contentDescription = "帧预览",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 状态文字
        if (state.phase.isNotEmpty() && !state.isProcessing) {
            Text(
                state.phase,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // ── 操作按钮 ──────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { videoPicker.launch("video/*") },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.VideoFile, null)
                Spacer(Modifier.width(4.dp))
                Text("导入视频")
            }

            Button(
                onClick = { viewModel.processVideo() },
                enabled = state.videoUri != null && !state.isExtracting && !state.isProcessing,
                modifier = Modifier.weight(1f)
            ) {
                if (state.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White, strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.MovieCreation, null)
                }
                Spacer(Modifier.width(4.dp))
                Text(if (state.isProcessing) "处理中" else "开始处理")
            }
        }

        // ── 完成提示 ──────────────────────────────────────────────
        if (state.resultVideoUri != null) {
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle, null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("处理完成！", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "视频已保存至缓存目录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.Share, null,
                        modifier = Modifier.clickable { /* TODO: 分享 */ })
                }
            }
        }

        // 错误提示
        state.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ── 提示信息 ──────────────────────────────────────────────
        if (state.videoUri != null && !state.isProcessing && !state.isExtracting) {
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info, null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "优化提示",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "• 视频帧自动缩放至 720P，节省 75% 算力\n" +
                                "• AI 推理线程上限 2，防止手机过热\n" +
                                "• 进度条实时更新，UI 保持流畅",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// ── 设置 Tab ────────────────────────────────────────────────────────────────

@Composable
fun SettingsTab(viewModel: MainViewModel) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "设置",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 电池信息
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "设备状态",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))

                val batteryInfo = remember {
                    BatteryMonitor(context).getBatteryInfo()
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when {
                            batteryInfo.isCharging -> Icons.Default.BatteryChargingFull
                            batteryInfo.level < 20 -> Icons.Default.BatteryAlert
                            else -> Icons.Default.BatteryFull
                        },
                        null,
                        tint = when {
                            batteryInfo.level < 20 -> Color(0xFFE65100)
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "电量 ${batteryInfo.level}%${if (batteryInfo.isCharging) "（充电中）" else ""}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (batteryInfo.temperature > 0) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Thermostat, null,
                            modifier = Modifier.size(18.dp),
                            tint = if (batteryInfo.isOverheated) Color(0xFFE65100)
                            else MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "温度 ${batteryInfo.temperature}°C" +
                                    if (batteryInfo.isOverheated) " ⚠️ 过热" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (batteryInfo.isOverheated) Color(0xFFE65100)
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "处理配置",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "• AI 线程数上限：2\n" +
                            "• 视频帧分辨率上限：720P（长边）\n" +
                            "• 低电量阈值：< 20%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}
