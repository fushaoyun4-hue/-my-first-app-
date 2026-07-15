# 去水印 - 纯离线 Android App

> 一键去除图片/视频中的水印和文字封装，导出的成品无痕自然

## 功能特性

- 🖼 **图片去水印** - 导入 JPG/PNG/WebP，自动检测并去除水印/文字
- 🎬 **视频去封装** - 导入 MP4/MOV，AI 逐帧处理后合成回无水印视频
- 📱 **纯离线运行** - 无需联网，所有 AI 推理在本地完成
- ⚡ **手机流畅** - 量化模型 + ONNX 加速，内存占用 < 1.5GB
- 🔒 **隐私安全** - 图片/视频不离开设备

## 技术方案

- **去水印模型**: LaMa-Fourier（轻量 ONNX 模型，INT8 量化 < 100MB）
- **推理引擎**: ONNX Runtime for Android
- **视频处理**: FFmpeg（抽帧 + 合成）
- **构建方式**: GitHub Actions 云端打包（解决 8GB 机器编译难题）
- **最低系统**: Android 8.0（API 26）

## 快速开始

### 方式一：下载 APK（推荐）

前往 [Releases](https://github.com/YOUR_USERNAME/watermark-remover/releases) 下载最新 APK，直装使用。

### 方式二：本地编译

```bash
# 克隆项目
git clone https://github.com/YOUR_USERNAME/watermark-remover.git
cd watermark-remover

# 首次运行 Gradle Wrapper
./gradlew wrapper

# 编译 Debug APK
./gradlew assembleDebug

# 输出位置: app/build/outputs/apk/debug/app-debug.apk
```

### 方式三：GitHub Actions 云端编译

```bash
# 打 tag 触发 GitHub Actions 自动编译
git tag v0.1.0
git push origin v0.1.0
```

## 项目结构

```
去水印/
├── app/                    # Android 主工程
│   └── src/main/
│       ├── java/com/watermark/
│       │   ├── ui/         # Compose UI
│       │   ├── inference/  # ONNX + LaMa 推理
│       │   ├── video/      # FFmpeg 视频处理
│       │   └── util/       # 工具类
│       ├── res/            # 资源文件
│       └── assets/
│           ├── models/      # ONNX 模型
│           └── ffmpeg/      # FFmpeg 二进制
├── .github/workflows/       # CI/CD
└── docs/                   # 文档
```

## 操作流程

**图片**: 导入 → 自动检测 → 确认/调整 → 去水印 → 导出

**视频**: 导入 → 选择片段 → 自动检测 → 逐帧处理 → 合成 → 导出

## 性能数据

| 场景 | 处理时间 | 内存占用 |
|------|---------|---------|
| 1080p 图片 | < 3 秒 | < 500MB |
| 15 秒视频 | < 30 秒 | < 1.5GB |
| APK 大小 | - | < 200MB |

---
*完全离线，保护隐私*
