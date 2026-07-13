# 📄 去水印 App 项目规范 v1.1

> 最后更新: 2026-07-10

## 项目目标
构建一个可在 Android 离线运行的本地短视频去水印 App，支持抖音、快手、B站等平台的水印检测与移除。

---

## 技术选型

| 层级 | 技术 | 说明 |
|------|------|------|
| 前端框架 | React Native 0.85.3 (Expo SDK 56) | 跨平台，成熟生态 |
| 语言 | JavaScript / React 19 | 团队熟悉 |
| 视频处理 | FFmpegKit (ffmpeg-kit-react-native) | 帧提取、合成 |
| AI 检测 | YOLOv8n ONNX → TFLite | 水印目标检测 |
| AI 修复 | LaMa / OpenCV inpaint | 水印区域图像修复 |
| 构建 | EAS Build / 本地 expo prebuild | APK 输出 |

---

## 项目状态

### ✅ 已完成
- [x] 需求分析 & SPEC.md 编写
- [x] 项目创建 (Expo SDK 56, React 19, RN 0.85.3)
- [x] 基础依赖安装 (expo, expo-av, expo-document-picker, expo-file-system, expo-sharing)
- [x] 主 UI (App.js, ~20KB, 包含选择/处理/结果三个界面)
- [x] 水印检测模块 (src/wmDetector.js - JS API + 模板降级)
- [x] 原生模块 JS 接口 (src/WMDetectorNative.js)
- [x] 视频处理模块 (src/videoProcessor.js - FFmpegKit 封装)
- [x] 主流水线 (src/watermarkRemover.js - 整合检测+修复)
- [x] Android 原生模块 Java 代码 (WatermarkDetectorModule.java)
- [x] Android 包注册 (WatermarkPackage.java)
- [x] EAS Build 配置 (eas.json)
- [x] app.json 完善 (包名: com.aisinghelper.watermark)
- [x] BUILD.md 构建指南

### ⏳ 进行中
- [ ] 下载 YOLOv8n TFLite 模型 → `native/android/.../assets/yolov8n_float16.tflite`
- [ ] 下载 LaMa TFLite 模型
- [ ] 安装 `ffmpeg-kit-react-native` npm 包

### ❌ 阻塞中
- [ ] 本地 Android SDK 环境未就绪 (dl.google.com 不可达)
  - 已安装: OpenJDK 17.0.2 (D:\androiddev\tools\jdk-17.0.2)
  - 已安装: Android cmdlinetools (D:\androiddev\tools\android-sdk) - **但版本损坏**
  - 缺失: platform-tools, android-34, build-tools

### 🔲 待办
- [ ] 运行 `npx expo prebuild --platform android`
- [ ] 将模型文件放入 `android/app/src/main/assets/`
- [ ] 编译 debug APK
- [ ] 测试实际去水印效果
- [ ] 优化检测精度 (YOLOv8 自训练模型)
- [ ] 添加多平台支持 (iOS)

---

## 架构设计

```
用户选择视频 (DocumentPicker)
         ↓
┌──────────────────────────────────┐
│   watermarkRemover.process()      │
│                                    │
│  1. extractFrames()               │  FFmpegKit
│     提取关键帧 (~8帧)              │
│         ↓                          │
│  2. detectWatermarks()            │  TFLite ONNX
│     YOLOv8n 目标检测               │
│     → [{x,y,w,h,conf,label}]      │
│         ↓                          │
│  3. mergeRegions()                │  JS
│     多帧合并 → 稳定水印区域          │
│         ↓                          │
│  4. removeWatermarks()            │  FFmpegKit
│     逐帧蒙版修复 → 合成新视频       │
└──────────────────────────────────┘
         ↓
  输出: 无水印 MP4
         ↓
  分享/保存 (Expo Sharing)
```

---

## 网络状态

| 目标 | 状态 | 备注 |
|------|------|------|
| dl.google.com | ❌ 404/不可达 | Android SDK 下载失败 |
| github.com | ❌ 超时 | 模型权重无法下载 |
| registry.npmmirror.com | ✅ 可用 | npm 镜像正常 |
| Tuna 镜像 (pypi) | ✅ 可用 | Python 包正常 |
| 华为云镜像 | ✅ 部分可用 | cmdlinetools 损坏 |

---

## 关键路径

- **项目目录**: `D:\ai_sing_new\code`
- **Android SDK**: `D:\androiddev\tools\android-sdk` (损坏)
- **JDK 17**: `D:\androiddev\tools\jdk-17.0.2` (正常)
- **临时工具**: `D:\androiddev\tools\`
