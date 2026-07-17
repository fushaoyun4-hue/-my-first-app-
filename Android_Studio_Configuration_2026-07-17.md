# Android Studio 配置完成清单

**项目**: D:\去水印 (QuickWatermarkRemover)  
**日期**: 2026-07-17  
**目标**: 适配 8GB 内存 + 核显电脑

---

## ✅ 已完成的配置

### 1. gradle.properties（低内存优化）
```properties
org.gradle.jvmargs=-Xmx2048m        # 限制 Gradle 堆内存为 2GB
org.gradle.parallel=false            # 禁用并行构建
org.gradle.daemon=true               # 保持 Gradle 守护进程
android.enableParallelSync=false    # 禁用并行同步
```

### 2. .idea/gradle.xml（Gradle JDK 配置）
- 设置 `gradleJvm = #JAVA_HOME`
- 关联到系统已安装的 JDK 17: `D:\androiddev\tools\jdk-17.0.2`

### 3. .idea/misc.xml（项目 JDK 配置）
- `languageLevel = JDK_17`
- `project-jdk-name = 17`
- `project-jdk-type = JavaSDK`

### 4. local.properties（SDK 路径）
- `sdk.dir = D:\androiddev\tools\android-sdk`

---

## 🚀 启动 Android Studio

现在可以安全启动 Android Studio：

1. **关闭任何正在运行的 Android Studio 实例**

2. **启动 Android Studio**
   ```
   开始菜单 → Android Studio
   ```

3. **打开项目**: File → Open → 选择 `D:\去水印`

4. **等待索引完成**（首次打开会索引文件，可能需要几分钟）

5. **验证配置**:
   - File → Project Structure → Project → Project SDK 应显示 "Android API 34"
   - File → Settings → Build, Execution, Deployment → Build Tools → Gradle → 应显示 JDK 17 路径

---

## ⚠️ 低内存使用建议

由于你的电脑只有 8GB 内存：

| 操作 | 建议 |
|------|------|
| 启动 Android Studio | 先关闭其他程序 |
| 同步 Gradle | 等待进度条完成，不要频繁切换 |
| 编辑代码 | 可以正常使用 |
| 运行/调试 | 确保设备连接稳定 |
| 大型重构 | 考虑分步进行 |

---

## 🔧 如遇问题

### 问题：Gradle 同步卡住
```
解决：等待 3-5 分钟，如仍无响应，检查 Task Manager 中 java.exe 进程是否崩溃
```

### 问题：内存不足警告
```
解决：File → Settings → Appearance → 取消勾选 "Show memory indicator"
```

### 问题：JDK 未识别
```
解决：File → Project Structure → SDK Location → 检查 JDK location 指向：
D:\androiddev\tools\jdk-17.0.2
```

---

## 📋 后续任务

根据之前的调研，下一步可以：

1. **选项 A**: 继续使用现有 LaMa 模型（项目已有）
2. **选项 B**: 集成 MI-GAN ONNX 模型（更轻量 ~30MB）
3. **选项 C**: 两个都保留，通过 UI 切换

需要我继续帮你做哪个？