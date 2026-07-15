"""
D:\去水印\convert_scripts\download_models.py
下载预训练模型权重，用于去水印APP
"""
import os
import urllib.request
import sys

# ========== 配置 ==========
OUT_DIR = r"D:\去水印\models"
os.makedirs(OUT_DIR, exist_ok=True)

# Tuna 镜像（国内可访问）
HUGGINGFACE_MIRROR = "https://hf-mirror.com"

def download_file(url: str, dest: str, desc: str = ""):
    """带重试的下载"""
    if os.path.exists(dest):
        size_mb = os.path.getsize(dest) / 1e6
        print(f"  [跳过] {desc} 已存在 ({size_mb:.1f}MB)")
        return True
    
    print(f"  [下载] {desc}")
    print(f"         → {dest}")
    try:
        urllib.request.urlretrieve(url, dest)
        size_mb = os.path.getsize(dest) / 1e6
        print(f"  [完成] {size_mb:.1f}MB")
        return True
    except Exception as e:
        print(f"  [失败] {e}")
        if os.path.exists(dest):
            os.remove(dest)
        return False

# ========== 模型下载 ==========
print("=" * 60)
print("去水印 APP - 模型下载脚本")
print("=" * 60)

models = []

# ----- 1. YOLOv8n (水印检测) -----
# Ultralytics 官方发布
yolo_url = "https://github.com/ultralytics/assets/releases/download/v8.2.0/yolov8n.onnx"
yolo_dest = os.path.join(OUT_DIR, "yolov8n.onnx")
download_file(yolo_url, yolo_dest, "YOLOv8n ONNX (水印检测主模型)")

# 备用：HuggingFace mirror
# yolo_hf = f"{HUGGINGFACE_MIRROR}/onnx-community/yolov8n/object-detection/resolve/main/yolov8n.onnx"

# ----- 2. LaMa 修复模型 -----
# 已验证可用的 LaMa ONNX 模型源
# Carve/LaMa-ONNX: 来自 Carve 团队转换的 big-lama ONNX (opset 17, 512x512, fp32)
# 参考: https://huggingface.co/Carve/LaMa-ONNX
lama_urls = [
    # 主选: Carve 团队转换的 big-lama ONNX (推荐, opset 17, 512x512)
    ("https://huggingface.co/Carve/LaMa-ONNX/resolve/main/lama_fp32.onnx", "lama_inpaint.onnx"),
    # 备选: 同一仓库的 lama.onnx (opset 18, 较慢, 不推荐)
    ("https://huggingface.co/Carve/LaMa-ONNX/resolve/main/lama.onnx", "lama_inpaint.onnx"),
]
lama_dest = os.path.join(OUT_DIR, "lama_inpaint.onnx")
for url, dest_name in lama_urls:
    ok = download_file(url, lama_dest, f"LaMa ONNX (图像修复) [{os.path.basename(dest_name)}]")
    if ok:
        break

# ----- 3. 水印检测数据集（校准用，可选）----
# MSCOCO 2017 子集（用于 INT8 量化校准，300张图）
coco_url = "https://images.cocodataset.org/zips/train2017.zip"
coco_dest = os.path.join(OUT_DIR, "calib_images.zip")
# download_file(coco_url, coco_dest, "COCO子集 (INT8校准)")

# ----- 4. STTN-lite 权重（自定义，暂无公开权重）----
# 提示：STTN 需要自己训练或找预训练权重
sttn_note = os.path.join(OUT_DIR, "STTN_LITE_README.txt")
with open(sttn_note, "w") as f:
    f.write("""STTN-lite 权重说明
====================

STTN（Shift-Symmetric Temporal Transformer Network）是用于视频
修复时序一致性的模型。当前目录暂无预训练权重。

备选方案：
1. 使用帧级 LaMa 修复 + 时序平滑后处理
2. 自己训练 STTN-lite（参考 RVC hubert 训练框架）
3. 寻找社区分享的预训练 ONNX 权重

STTN-lite 架构设计（建议）:
- 输入: 当前帧 [B,3,H,W] + 蒙版 [B,1,H,W] + 隐藏状态 [B,64,H//4,W//4]
- 主干: 4层因果卷积 (kernel=3, dilation=1,2,4,8) + ConvGRU
- 输出: 修复帧 + 更新隐藏状态
- 训练数据: YouTube-VOS / DAVIS 数据集
""")
print(f"  [完成] STTN说明文件已创建")

# ----- 5. 文本检测模型（CTPN/DBNet，用于文字水印）----
# 极小文字检测模型
db_url = f"{HUGGINGFACE_MIRROR}/PaddleOCR/duckdnn/resolve/main/db_mobilenetv3_fp32.onnx"
db_dest = os.path.join(OUT_DIR, "db_text_det.onnx")
download_file(db_url, db_dest, "DBNet文字检测 (文字水印识别)")

print("")
print("=" * 60)
print("下载完成！")
print(f"模型目录: {OUT_DIR}")
for f in os.listdir(OUT_DIR):
    fp = os.path.join(OUT_DIR, f)
    if os.path.isfile(fp):
        print(f"  {f}: {os.path.getsize(fp)/1e6:.1f}MB")
print("=" * 60)
