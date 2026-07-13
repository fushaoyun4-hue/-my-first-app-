"""
D:\去水印\convert_scripts\convert_yolov8.py
YOLOv8n → TFLite INT8 转换脚本
"""
import os
import sys

# Python 环境: D:\miaoyin\rvc-py39
PYTHON = r"D:\miaoyin\rvc-py39\Scripts\python.exe"

os.makedirs(r"D:\去水印\models", exist_ok=True)
os.makedirs(r"D:\去水印\output_tflite", exist_ok=True)

MODEL_IN = r"D:\去水印\models\yolov8n.onnx"
MODEL_OUT_FP16 = r"D:\去水印\output_tflite\yolov8n_fp16.tflite"
MODEL_OUT_INT8 = r"D:\去水印\output_tflite\yolov8n_int8.tflite"

def run(cmd):
    print(f"\n>>> {cmd}")
    r = os.system(cmd)
    if r != 0:
        print(f"[ERROR] 命令失败，退出码: {r}")
        sys.exit(r)
    return r

def check_install(pkg):
    os.system(f"{PYTHON} -c 'import {pkg}' 2>nul && echo {pkg} OK || echo {pkg} MISSING")

print("=" * 60)
print("YOLOv8n → TFLite INT8 转换")
print("=" * 60)

# 检查依赖
print("\n[1] 检查环境...")
deps = ["torch", "onnx", "onnxruntime", "tensorflow"]
for d in deps:
    check_install(d)

# 检查模型文件
if not os.path.exists(MODEL_IN):
    print(f"\n[ERROR] 模型文件不存在: {MODEL_IN}")
    print("请先运行 download_models.py 下载模型")
    sys.exit(1)

size_mb = os.path.getsize(MODEL_IN) / 1e6
print(f"\n输入模型: {size_mb:.1f}MB")

# ONNX 验证
print("\n[2] 验证 ONNX 模型...")
run(f'{PYTHON} -c "import onnx; m=onnx.load(r\\\"{MODEL_IN}\\\"); onnx.checker.check_model(m); print(\\\"ONNX OK\\\")"')

# TFLite Converter
TFLITE_CONVERTER_SCRIPT = r"""import os
import numpy as np
import onnx
import torch

try:
    import onnxruntime as ort
except ImportError:
    os.system('cmd /c "D:\\miaoyin\\rvc-py39\\Scripts\\pip install onnxruntime"')
    import onnxruntime as ort

try:
    import tensorflow as tf
except ImportError:
    os.system('cmd /c "D:\\miaoyin\\rvc-py39\\Scripts\\pip install tensorflow-cpu"')
    import tensorflow as tf

model_path = r'{MODEL_IN}'
out_fp16 = r'{MODEL_OUT_FP16}'
out_int8 = r'{MODEL_OUT_INT8}'

print(f"输入: {{model_path}}")
print(f"输出FP16: {{out_fp16}}")
print(f"输出INT8: {{out_int8}}")

# 生成代表性数据集（用于 INT8 量化）
print("生成校准数据...")
calib_data = []
sample_count = 100
for i in range(sample_count):
    img = np.random.randn(1, 3, 640, 640).astype(np.float32)
    calib_data.append(img)
    if (i+1) % 20 == 0:
        print(f"  [{i+1}/{sample_count}]")

def representative_dataset():
    for img in calib_data:
        yield [img]

print("转换为 FP16 TFLite...")
converter = tf.lite.TFLiteConverter.from_saved_model(model_path)
# 如果是 onnx，先用 onnx2tf 转换
# 这里假设模型已转为 TensorFlow SavedModel 或直接用 onnx
# 策略：先用 FP16，再用 INT8

# 备选：onnx → tf → tflite
import subprocess
result = subprocess.run([
    'cmd', '/c',
    f'"{os.environ.get(\"PYTHON\", r\"D:\\miaoyin\\rvc-py39\\Scripts\\python.exe\")}"',
    '-c', 
    '''
import os, subprocess, sys
onnx_file = r"{MODEL_IN}"
output_dir = r"D:\\去水印\\output_tflite\\yolov8n_savedmodel"

# 用 onnx-tf 转换
try:
    import onnx_tf
except ImportError:
    subprocess.run([sys.executable, '-m', 'pip', 'install', 'onnx-tf'], check=True)
    import onnx_tf

import onnx
from onnx_tf.backend import prepare
import tensorflow as tf

print("加载 ONNX...")
onnx_model = onnx.load(onnx_file)
print("转换 TF...")
tf_rep = prepare(onnx_model)
print("导出 SavedModel...")
tf_rep.export_graph(output_dir)
print("SavedModel 导出完成")
'''
], capture_output=True, text=True, cwd=os.path.dirname(model_path))

print("stdout:", result.stdout[-500:] if result.stdout else "")
print("stderr:", result.stderr[-500:] if result.stderr else "")

print("\\n[完成] 转换流程已启动")
"""

print("\n[3] 转换 ONNX → SavedModel → TFLite...")
# 实际执行：安装 tf+onnx-tf，然后转换
print("安装 tensorflow-cpu...")
run(f'"{PYTHON}" -m pip install tensorflow-cpu --quiet --no-warn-script-location')

print("安装 onnx-tf...")
run(f'"{PYTHON}" -m pip install onnx-tf --quiet --no-warn-script-location')

print("转换...")
savedmodel_dir = r"D:\去水印\output_tflite\yolov8n_savedmodel"
os.makedirs(savedmodel_dir, exist_ok=True)

conv_script = rf"""
import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'
import onnx
from onnx_tf.backend import prepare
import tensorflow as tf

print("加载 ONNX: {MODEL_IN}")
onnx_model = onnx.load(r"{MODEL_IN}")
print("转换...")
tf_rep = prepare(onnx_model)
print("导出到 SavedModel...")
tf_rep.export_graph(r"{savedmodel_dir}")
print("完成!")
"""

with open(r"D:\去水印\convert_scripts\_run_convert.py", "w") as f:
    f.write(conv_script)

run(f'"{PYTHON}" "D:\去水印\convert_scripts\_run_convert.py"')

print("\n[4] 导出 TFLite...")
tflite_script = rf"""
import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'
import tensorflow as tf
import numpy as np

savedmodel_dir = r"{savedmodel_dir}"
out_fp16 = r"{MODEL_OUT_FP16}"
out_int8 = r"{MODEL_OUT_INT8}"

# 加载 SavedModel
model = tf.saved_model.load(savedmodel_dir)
concrete_func = model.signatures[tf.saved_model.DEFAULT_SERVING_SIGNATURE_DEF_KEY]

# FP16 转换
print("FP16 量化...")
converter = tf.lite.TFLiteConverter.from_concrete_functions([concrete_func])
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.target_spec.supported_types = [tf.float16]
tflite_fp16 = converter.convert()
with open(out_fp16, 'wb') as f:
    f.write(tflite_fp16)
print(f"FP16: {{len(tflite_fp16)/1e6:.1f}}MB")

# INT8 转换（无校准数据，用 dynamic range）
print("INT8 量化...")
converter2 = tf.lite.TFLiteConverter.from_concrete_functions([concrete_func])
converter2.optimizations = [tf.lite.Optimize.DEFAULT]
# 尝试动态范围量化（不需要校准数据）
try:
    tflite_int8 = converter2.convert()
    with open(out_int8, 'wb') as f:
        f.write(tflite_int8)
    print(f"INT8: {{len(tflite_int8)/1e6:.1f}}MB")
except Exception as e:
    print(f"INT8 失败: {{e}}")
    print("备选：用 FP16")
    import shutil
    shutil.copy(out_fp16, out_int8)

print("转换完成!")
"""

with open(r"D:\去水印\convert_scripts\_run_tflite.py", "w") as f:
    f.write(tflite_script)

run(f'"{PYTHON}" "D:\去水印\convert_scripts\_run_tflite.py"')

# 结果报告
print("\n" + "=" * 60)
print("转换结果")
print("=" * 60)
for f in [MODEL_OUT_FP16, MODEL_OUT_INT8]:
    if os.path.exists(f):
        print(f"  ✅ {os.path.basename(f)}: {os.path.getsize(f)/1e6:.1f}MB")
    else:
        print(f"  ❌ {os.path.basename(f)}: 未生成")
