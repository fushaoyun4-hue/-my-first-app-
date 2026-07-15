# 模型下载与转换指南

## 获取 LaMa-Fourier ONNX 模型

### 方案一：直接下载预编译 ONNX（推荐）

1. 访问 HuggingFace 模型仓库：
   ```
   https://huggingface.co/YOUR_USERNAME/lama-fourier
   ```
2. 下载 `lama-fourier.onnx`（或 INT8 量化版 `lama-fourier-int8.onnx`）
3. 放入项目目录：
   ```
   app/src/main/assets/models/lama-fourier.onnx
   ```

### 方案二：从 PyTorch 转换

如果你有 LaMa 或 LaMa-Fourier 的 PyTorch 权重：

```python
import torch
from torchvision import transforms

# 1. 加载 PyTorch 模型
model = YourLaMaModel.load_from_checkpoint("path/to/ckpt")
model.eval()

# 2. 准备输入（单张 512x512 图片 + mask）
dummy_image = torch.randn(1, 3, 512, 512)
dummy_mask = torch.randn(1, 1, 512, 512)

# 3. 导出 ONNX
torch.onnx.export(
    model,
    (dummy_image, dummy_mask),
    "lama-fourier.onnx",
    input_names=["input", "mask"],
    output_names=["output"],
    dynamic_axes={
        "input": {2: "h", 3: "w"},
        "mask": {2: "h", 3: "w"},
        "output": {2: "h", 3: "w"}
    },
    opset_version=14
)
print("ONNX 导出成功")
```

### 方案三：替代轻量模型

如果 LaMa 模型获取困难，可替换为以下任一模型：

| 模型 | 来源 | 尺寸 | 效果 |
|------|------|------|------|
| SD Inpainting ( pruned) | HuggingFace stable-diffusion-inpainting | ~1.5GB | ⭐⭐⭐⭐ |
| ZITS | GitHub: dwz92/ZITS | ~200MB | ⭐⭐⭐⭐ |
| LaMa (原版) | GitHub:ADVANCEorg/Lama | ~400MB | ⭐⭐⭐ |

替换步骤：
1. 下载模型 → 转为 ONNX → 放入 assets/models/
2. 修改 `LaMaEngine.kt` 中的 `inputName` / `outputName`
3. 测试推理输出尺寸是否与代码兼容

## FFmpeg Android 二进制

### 推荐预编译包

下载 `ffmpeg` Android 二进制（armeabi-v7a + arm64-v8a）：

```bash
# 从 BtbN 的 Android FFmpeg 构建
https://github.com/BtbN/FFmpeg-Builds/releases

# 选择：ffmpeg-master-latest-android-arm64-v8a.tar.xz
```

解压到 `app/src/main/assets/ffmpeg/`，文件结构：
```
app/src/main/assets/
├── ffmpeg/
│   └── ffmpeg          # 主程序（无后缀）
└── models/
    └── lama-fourier.onnx
```

> 注：APK 打包时 assets 会被压缩，首次运行时复制到 `context.filesDir`

## 验证模型

打包前测试（连接手机 ADB）：
```bash
# 安装 APK 后
adb push lama-fourier.onnx /data/data/com.watermark.remover/files/models/
adb logcat -s WatermarkRemover
```
