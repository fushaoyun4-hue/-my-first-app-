# 去水印 APP — 开发进度记录

> 最后更新：2026-07-15 14:36 GMT+8

---

## 一、项目基本信息

- **项目路径**：D:\去水印\
- **项目类型**：纯 Kotlin 原生 Android（Jetpack Compose）
- **包名**：com.watermark.remover
- **最低 SDK**：API 26（Android 8.0）
- **目标 SDK**：API 34

---

## 二、当前已完成的文件

| 文件 | 状态 | 说明 |
|------|------|------|
| `app/src/main/java/com/watermark/inference/LaMaEngine.kt` | ✅ 完成 | ONNX LaMa 推理引擎，支持 NNAPI 加速 |
| `app/src/main/java/com/watermark/ui/MainScreen.kt` | ✅ 完成 | Compose UI，图片 Tab + 视频 Tab + 设置 Tab |
| `app/src/main/java/com/watermark/ui/theme/Theme.kt` | ✅ 完成 | Material3 主题，品牌蓝 BrandBlue |
| `app/build.gradle.kts` | ✅ 完成 | Gradle 配置（Compose + Hilt + ONNX + SAF） |
| `.github/workflows/android.yml` | ✅ 完成 | GitHub Actions 云端构建配置 |
| `SPEC.md` | ✅ 完成 | 项目规格说明 |
| `gradlew` / `gradlew.bat` | ✅ 完成 | Gradle Wrapper |

---

## 三、已知 Bug（已修复等待重新编译）

1. **ONNX Runtime 包名错误** — `org.microsoft.onnxruntime` → `ai.onnxruntime`（已修复）
2. **BrandBlue 访问权限** — Theme.kt 中 BrandBlue 已改为 internal（已修复）

---

## 四、待开发功能（按优先级）

### 🔴 P0 — 阻塞项
1. **接入 SAF 视频选择器** — 从相册导入本地视频（MP4/MOV）
2. **手动框选工具** — 拖动框选水印区域，归一化坐标传入 AI
3. **AI 去水印流程** — 框选 → 调用 LaMaEngine → 生成结果 Bitmap
4. **视频处理流程** — 抽帧 → 逐帧 AI → FFmpeg 合成 → 输出 MP4
5. **保存至相册** — 处理完成后 MediaStore 写入

### 🟡 P1 — 重要
6. **进度条** — 视频处理实时进度反馈
7. **预览播放** — 处理前后视频对比

### 🟢 P2 — 优化
8. **自动检测水印**（可选，后续再做）

---

## 五、缺失的依赖资源

| 资源 | 状态 | 说明 |
|------|------|------|
| LaMa ONNX 模型 | ❌ 缺失 | `models/` 目录为空，需放入 `lama-fourier.onnx` |
| FFmpeg | ❌ 未集成 | 需 Android FFmpeg 二进制，放在 `assets/ffmpeg/` |

---

## 六、云端构建方案

**方案**：GitHub Actions（`.github/workflows/android.yml` 已配置）

**你的下一步**：
1. 把 `D:\去水印` 整个文件夹上传到 GitHub 私有仓库
2. 推送后 GitHub 自动编译 APK（无需本地编译）
3. 下载 `app/build/outputs/apk/debug/app-debug.apk`

**本地调试方式**：
- ❌ 禁止使用 `gradlew`、`Android Studio`、EAS 等本地编译
- ✅ 使用 VS Code / Notepad++ 编辑代码
- ✅ 代码改好后 push → GitHub Actions 自动编译

---

## 七、构建环境约束

- **绝对禁止**：gradlew、assembleDebug/Release、eas build（均会导致 OOM）
- **唯一合法构建**：GitHub Actions
- **本地调试**：编辑代码 → push → 下载 APK 安装测试

---

## 八、MainScreen.kt 当前代码状态

- 图片 Tab：已完成 UI 布局、拖动框选逻辑、状态管理
- 视频 Tab：仅占位，SAF 接入后需重新实现
- 设置 Tab：仅占位

**关键 TODO 占位**：
- `setVideo(uri)` — SAF 接入后实现
- `extractFrames()` — FFmpeg 接入后实现
- `encodeVideo()` — FFmpeg 接入后实现
- `saveToGallery()` — MediaStore API 实现
- 自动检测水印 — 暂不实现（P2）

---

## 九、技术栈

```
UI：        Jetpack Compose + Material3
架构：      MVVM + Hilt 依赖注入
AI 推理：   ONNX Runtime 1.16.3（ai.onnxruntime）
加速：      NNAPI（GPU/NPU）
视频处理：  FFmpeg（待集成）
文件选择：  SAF（待接入）
协程：      Kotlin Coroutines
最低版本：  Android 8.0 (API 26)
```

---

*下次继续时，先读取本文件再开始工作*
