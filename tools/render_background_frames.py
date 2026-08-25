#!/usr/bin/env python3
"""Generate seamless animated frames from Shweep background layers."""
import math
import os
import sys
from pathlib import Path

from PIL import Image

ROOT = Path("art/background/layers")
OUT = Path("/tmp/shweep_frames")
OUT.mkdir(parents=True, exist_ok=True)

W, H = 1672, 941
FPS = 24
DURATION = 8.0
FRAMES = int(FPS * DURATION)

# Load layers once
sky = Image.open(ROOT / "00_sky.png").convert("RGBA")
glow = Image.open(ROOT / "01_sun_glow.png").convert("RGBA")
clouds_rear = Image.open(ROOT / "02_clouds_rear.png").convert("RGBA")
landscape = Image.open(ROOT / "03_landscape.png").convert("RGBA")
clouds_front = Image.open(ROOT / "04_clouds_front.png").convert("RGBA")
sheep = Image.open(ROOT / "05_sheep.png").convert("RGBA")
moon = Image.open(ROOT / "06_moon.png").convert("RGBA")

# Pre-scale full-width cloud layers to provide translate margin
def scale_margin(img, factor):
    nw, nh = int(img.width * factor), int(img.height * factor)
    return img.resize((nw, nh), Image.LANCZOS)

REAR_SCALE = 1.08
FRONT_SCALE = 1.10
clouds_rear = scale_margin(clouds_rear, REAR_SCALE)
clouds_front = scale_margin(clouds_front, FRONT_SCALE)

# Motion amplitudes (px)
REAR_AMP = 25.0
FRONT_AMP = 30.0
MOON_AMP = 3.0

transparent = (0, 0, 0, 0)

def paste_offset(base, layer, dx, dy):
    return Image.alpha_composite(
        base,
        Image.frombytes("RGBA", (W, H), b"\x00" * (W * H * 4))
    ) if False else None

print(f"Rendering {FRAMES} frames at {W}x{H}...", file=sys.stderr)

for i in range(FRAMES):
    t = i / FPS
    phase = 2.0 * math.pi * t / DURATION

    # Seamless sinusoidal motion
    rear_dx = REAR_AMP * math.sin(phase)
    front_dx = FRONT_AMP * math.sin(phase + math.pi)  # opposite phase
    moon_dx = MOON_AMP * math.sin(phase)
    moon_dy = -MOON_AMP * (0.5 + 0.5 * math.sin(phase + math.pi / 2))
    glow_factor = 0.85 + 0.15 * (0.5 + 0.5 * math.sin(phase + math.pi / 2))

    base = sky.copy()

    # Glow (opacity pulse)
    if glow_factor < 1.0:
        frame = Image.new("RGBA", (W, H), transparent)
        glow_cur = Image.blend(frame, glow, glow_factor)
        base = Image.alpha_composite(base, glow_cur)
    else:
        base = Image.alpha_composite(base, glow)

    # Rear clouds
    frame = Image.new("RGBA", (W, H), transparent)
    cx = int((W - clouds_rear.width) / 2 + rear_dx)
    cy = int((H - clouds_rear.height) / 2)
    frame.paste(clouds_rear, (cx, cy), clouds_rear)
    base = Image.alpha_composite(base, frame)

    # Landscape (static)
    base = Image.alpha_composite(base, landscape)

    # Front clouds
    frame = Image.new("RGBA", (W, H), transparent)
    cx = int((W - clouds_front.width) / 2 + front_dx)
    cy = int((H - clouds_front.height) / 2)
    frame.paste(clouds_front, (cx, cy), clouds_front)
    base = Image.alpha_composite(base, frame)

    # Sheep (static)
    base = Image.alpha_composite(base, sheep)

    # Moon
    frame = Image.new("RGBA", (W, H), transparent)
    frame.paste(moon, (int(moon_dx), int(moon_dy)), moon)
    base = Image.alpha_composite(base, frame)

    rgb = base.convert("RGB")
    rgb.save(OUT / f"frame_{i:04d}.ppm")

    if (i + 1) % 32 == 0:
        print(f"  {i + 1}/{FRAMES}", file=sys.stderr)

print("Done.", file=sys.stderr)
