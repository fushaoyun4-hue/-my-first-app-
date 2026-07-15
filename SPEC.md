# 去水印 - 项目规格说明书

> 纯离线 Android App，无痕去除图片/视频中的水印和文字封装

---

## 1. 项目概述

- **项目名称**: 去水印 (WatermarkRemover)
- **类型**: 纯离线 Android App（APK），无需联网
- **核心功能**: 导入本地图片/视频，自动检测并去除水印/文字，导出无痕成品
- **目标用户**: 普通用户，几步完成操作，无需 AI/图像处理知识

---

## 2. 技术方案

### 2.1 图像去水印
- **模型**: LaMa-Fourier（基于 LaMa + 傅里叶修复思路的轻量模型）
- **推理引擎**: ONNX Runtime（Android）
- **模型量化**: INT8 量化版，目标 < 100MB
- **内存策略**: 单帧处理，释放 GPU 显存，避免 OOM

### 2.2 视频去封装
- **处理流程**:
  1. FFmpeg 抽帧（每秒 1~2 帧，关键帧优先）
  2. 逐帧 AI 去水印
  3. FFmpeg 合成回视频（保持原始分辨率、帧率、编码）
- **内存优化**: 流式处理，帧级 batch，8GB RAM 一体机可正常运行

### 2.3 端侧优化（手机不卡）
- 量化模型（FP16 / INT8）
- TensorFlow Lite / ONNX Mobile 加速
- 后台 Worker Thread 处理，进度实时同步 UI
- 不在主线程做 IO 和推理

### 2.4 构建方式
- **本地**: Gradle + Android Studio（可调试）
- **正式打包**: GitHub Actions CI/CD（解决 8GB 机器内存不足问题）
- **产物**: 独立 APK（包含模型 + FFmpeg），无需 Root

---

## 3. 功能列表

### 3.1 核心功能
- [ ] **图片导入**: 从相册/文件选择图片（JPG/PNG/WebP）
- [ ] **视频导入**: 从相册/文件选择视频（MP4/MOV/AVI），自动抽帧预览
- [ ] **自动检测**: 扫描水印/文字区域（基于边缘+颜色+语义多策略）
- [ ] **手动标注**: 用户可框选/涂抹遗漏的水印区域（画笔工具）
- [ ] **AI 去水印**: LaMa-Fourier 端侧推理，输出修复图
- [ ] **导出图片**: 保存至相册/指定路径
- [ ] **导出视频**: 合成后视频保存至相册/指定路径
- [ ] **处理进度**: 视频逐帧处理进度条，可取消

### 3.2 辅助功能
- [ ] 原图/效果图对比滑动预览
- [ ] 批量处理（多张图片排队）
- [ ] 处理历史记录

---

## 4. UI/UX 设计

### 4.1 视觉风格
- **风格**: 极简现代，参考"开拍"（轻量感、专业感）
- **色调**: 白色/浅灰背景 + 品牌蓝点缀
- **字体**: 系统默认无衬线字体
- **动画**: 轻量过渡，避免复杂动效（性能优先）

### 4.2 界面结构（Tab 导航）

```
┌─────────────────────────────────┐
│         去水印                    │
├─────────┬─────────┬─────────────┤
│  🖼 图片  │  🎬 视频  │   ⚙️ 设置   │
├─────────┴─────────┴─────────────┤
│                                 │
│      [导入区域 / 预览画面]         │
│                                 │
│      [去水印按钮]                 │
│      [导出按钮]                  │
│                                 │
│      [处理进度条]                 │
└─────────────────────────────────┘
```

### 4.3 操作流程

**图片流程**:
```
导入图片 → 自动检测水印 → 确认/手动调整 → 去水印处理 → 导出
```

**视频流程**:
```
导入视频 → 选择片段(可选) → 自动检测 → 逐帧处理 → 合成 → 导出
```

---

## 5. 项目结构

```
D:\去水印\
├── app/                          # Android 主工程
│   ├── app/src/main/
│   │   ├── java/com/watermark/  # Kotlin 源码
│   │   │   ├── MainActivity.kt
│   │   │   ├── ui/               # UI 层
│   │   │   ├── model/            # 数据模型
│   │   │   ├── inference/        # ONNX 推理 + LaMa
│   │   │   ├── video/            # FFmpeg 视频处理
│   │   │   └── util/             # 工具类
│   │   ├── res/                  # 资源文件
│   │   ├── assets/
│   │   │   ├── models/           # ONNX 模型文件（下载）
│   │   │   └── ffmpeg/           # FFmpeg Android 二进制
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── app.apk (输出)
│
├── docs/                         # 项目文档
│   ├── SPEC.md
│   ├── MODEL_GUIDE.md           # 模型转换/下载说明
│   └── ONBOARDING.md            # 新手入门
│
├── .github/
│   └── workflows/
│       └── build-apk.yml         # GitHub Actions 云端打包
│
├── models/                       # 模型文件（git-lfs 管理）
│   └── lama-fourier.onnx
│
└── README.md
```

---

## 6. GitHub Actions CI/CD

- **触发**: push tag 或 manual dispatch
- **环境**: ubuntu-latest（2核7GB RAM，可编译 APK）
- **流程**: Checkout → Setup Java → Build Debug APK → Upload Artifact
- **产物**: `app/app/build/outputs/apk/debug/app-debug.apk`
- **分发**: GitHub Release 下载 + 直链

---

## 7. 性能目标

| 指标 | 目标 |
|------|------|
| 图片去水印 | < 3 秒（1080p） |
| 视频去水印 | < 2x 实时（15秒视频 < 30秒） |
| 内存占用 | < 1.5GB RAM |
| APK 大小 | < 200MB（含模型 + FFmpeg） |
| 支持系统 | Android 8.0+（API 26） |

---

## 8. 依赖清单

| 组件 | 用途 | 版本 |
|------|------|------|
| Kotlin | 开发语言 | 1.9.x |
| ONNX Runtime | AI 推理引擎 | 1.16.x |
| FFmpeg | 视频抽帧/合成 | 6.0-android |
| Jetpack Compose | UI 框架 | 1.5.x |
| Coil | 图片加载 | 2.5.x |
| CameraX / SAF | 文件选择 | latest |
| Coroutines | 异步处理 | 1.7.x |
| Hilt | 依赖注入 | 2.48 |

---

*最后更新: 2026-07-14*
