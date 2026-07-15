from PIL import Image, ImageDraw, ImageFont
import os

base = r"D:\去水印\app\src\main\res"

densities = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

os.makedirs(os.path.join(base, "mipmap-anydpi-v26"), exist_ok=True)
os.makedirs(os.path.join(base, "values"), exist_ok=True)

for folder, size in densities.items():
    folder_path = os.path.join(base, folder)
    os.makedirs(folder_path, exist_ok=True)

    img = Image.new("RGBA", (size, size), (33, 150, 243, 255))
    draw = ImageDraw.Draw(img)

    try:
        font = ImageFont.truetype("arial.ttf", int(size * 0.55))
    except:
        font = ImageFont.load_default()

    text = "W"
    bbox = draw.textbbox((0, 0), text, font=font)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    tx = (size - tw) // 2 - bbox[0]
    ty = (size - th) // 2 - bbox[1]
    draw.text((tx, ty), text, fill=(255, 255, 255, 255), font=font)

    img.save(os.path.join(folder_path, "ic_launcher.png"))
    print(f"Created {folder}/ic_launcher.png")

    # round icon
    mask = Image.new("L", (size, size), 0)
    mask_draw = ImageDraw.Draw(mask)
    r = size // 4
    mask_draw.rounded_rectangle([(0, 0), (size-1, size-1)], radius=r, fill=255)
    rounded = Image.new("RGBA", (size, size))
    rounded.paste(img, (0, 0))
    rounded.putalpha(mask)
    rounded.save(os.path.join(folder_path, "ic_launcher_round.png"))
    print(f"Created {folder}/ic_launcher_round.png")

# foreground for adaptive icon
fg_img = Image.new("RGBA", (108, 108), (0, 0, 0, 0))
draw = ImageDraw.Draw(fg_img)
try:
    font2 = ImageFont.truetype("arial.ttf", 72)
except:
    font2 = ImageFont.load_default()
text = "W"
bbox = draw.textbbox((0, 0), text, font=font2)
tw = bbox[2] - bbox[0]
th = bbox[3] - bbox[1]
tx = (108 - tw) // 2 - bbox[0]
ty = (108 - th) // 2 - bbox[1]
draw.text((tx, ty), text, fill=(255, 255, 255, 255), font=font2)

for folder, size in densities.items():
    folder_path = os.path.join(base, folder)
    fg_resized = fg_img.resize((size, size), Image.LANCZOS)
    fg_resized.save(os.path.join(folder_path, "ic_launcher_foreground.png"))

# colors.xml
with open(os.path.join(base, "values", "colors.xml"), "w", encoding="utf-8") as f:
    f.write('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="ic_launcher_background">#2196F3</color>\n</resources>\n')

# adaptive icon XML
ic_foreground = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>
</adaptive-icon>"""

with open(os.path.join(base, "mipmap-anydpi-v26", "ic_launcher.xml"), "w", encoding="utf-8") as f:
    f.write(ic_foreground)
with open(os.path.join(base, "mipmap-anydpi-v26", "ic_launcher_round.xml"), "w", encoding="utf-8") as f:
    f.write(ic_foreground)

print("Done! All launcher icons created.")
