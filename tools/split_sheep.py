#!/usr/bin/env python3
"""Split sheep artwork into body + 4 leg layers for skeletal animation."""
from PIL import Image, ImageFilter
import json, os

SRC_DIR = "composeApp/src/commonMain/composeResources/drawable"
OUT_DIR = SRC_DIR
TMP_DIR = "/var/folders/7_/_9drmdh96h1f0b28q0kg7syh0000gn/T/opencode/sheep_split"

VARIANTS = {
    "white": os.path.join(SRC_DIR, "sheep.png"),
    "black": os.path.join(SRC_DIR, "black_sheep.png"),
}

# Generous search windows per leg (source coords, 1024-space); tightened later.
LEG_RECTS = {
    "front_near": (270, 670, 405, 885),
    "front_far": (420, 670, 560, 885),
    "rear_far": (565, 670, 685, 885),
    "rear_near": (695, 670, 830, 885),
}
LEG_ORDER = ["front_far", "rear_far", "front_near", "rear_near"]  # draw order
ROOT_TOP_Y = 620          # how far up leg roots are extended (hidden by body)
PAD_CROP = 16             # transparent bleed around tight crops
BODY_PAD = 12
PIVOT_Y = 705             # hip height in source coords

def is_leg(px):
    r, g, b, a = px[0], px[1], px[2], px[3]
    if a < 12:
        return False
    mx, mn = max(r, g, b), min(r, g, b)
    sat = mx - mn
    lum = 0.299 * r + 0.587 * g + 0.114 * b
    return sat > 26 or lum < 150

def build_leg_masks(img):
    """Return {leg: mask(L mode)} of leg-classified pixels per variant."""
    w, h = img.size
    masks = {}
    px = img.load()
    for name, (x0, y0, x1, y1) in LEG_RECTS.items():
        m = Image.new("L", (w, h), 0)
        mp = m.load()
        count = 0
        for y in range(y0, min(y1, h)):
            for x in range(x0, min(x1, w)):
                if is_leg(px[x, y]):
                    mp[x, y] = 255
                    count += 1
        # drop tiny specks with median-ish cleanup, keep solid areas
        m = m.filter(ImageFilter.MedianFilter(3))
        masks[name] = (m, count)
    return masks

def extend_root(mask, src):
    """Fill upward from each column's topmost leg pixel to ROOT_TOP_Y."""
    w, h = mask.size
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    mp, sp, op = mask.load(), src.load(), out.load()
    for x in range(w):
        top = None
        for y in range(ROOT_TOP_Y + 10, h):
            if mp[x, y] > 128:
                top = y
                break
        if top is None:
            continue
        fill_px = sp[x, min(top + 4, h - 1)]
        for y in range(top - 1, max(ROOT_TOP_Y - 1, 0), -1):
            op[x, y] = (fill_px[0], fill_px[1], fill_px[2], 255)
        for y in range(top, h):  # copy original leg pixels below
            if mp[x, y] > 128:
                op[x, y] = sp[x, y]
    return out

def main():
    os.makedirs(TMP_DIR, exist_ok=True)
    images = {v: Image.open(p).convert("RGBA") for v, p in VARIANTS.items()}
    all_masks = {v: build_leg_masks(img) for v, img in images.items()}

    for v, masks in all_masks.items():
        for name, (m, c) in masks.items():
            print(f"{v}/{name}: {c} leg pixels")

    meta = {"canvas": 1024, "legs": {}, "body": {}}

    # Union tight bbox per leg across variants -> identical crops.
    leg_boxes = {}
    for name in LEG_RECTS:
        xs0, ys0, xs1, ys1 = [], [], [], []
        for v in VARIANTS:
            m, _ = all_masks[v][name]
            bb = m.getbbox()
            if bb:
                xs0.append(bb[0]); ys0.append(bb[1]); xs1.append(bb[2]); ys1.append(bb[3])
        box = (min(xs0), min(ys0), max(xs1), max(ys1))
        cx = (box[0] + box[2]) // 2
        pivot = (cx, PIVOT_Y)
        crop_top = min(box[1] - PAD_CROP, ROOT_TOP_Y)
        crop = (
            max(box[0] - PAD_CROP, 0),
            max(crop_top, 0),
            min(box[2] + PAD_CROP, 1024),
            min(box[3] + PAD_CROP, 1024),
        )
        leg_boxes[name] = (crop, pivot)

    # Build layers.
    saved = {}
    for variant, img in images.items():
        body = img.copy()
        bpx = body.load()
        for name in LEG_RECTS:
            m, _ = all_masks[variant][name]
            dil = m.filter(ImageFilter.MaxFilter(5))  # erase AA fringe too
            dpx = dil.load()
            x0, y0, x1, y1 = LEG_RECTS[name]
            for y in range(y0, min(y1, 1024)):
                for x in range(x0, min(x1, 1024)):
                    if dpx[x, y] > 100:
                        bpx[x, y] = (0, 0, 0, 0)

        for name in LEG_RECTS:
            crop, pivot = leg_boxes[name]
            layer = extend_root(all_masks[variant][name][0], img).crop(crop)
            fname = f"sheep_{variant}_{name}_leg.png"
            path = os.path.join(TMP_DIR, fname)
            layer.save(path)
            saved[(variant, name)] = fname
            meta["legs"][f"{variant}_{name}"] = {
                "file": fname,
                "offsetX": crop[0], "offsetY": crop[1],
                "width": crop[2] - crop[0], "height": crop[3] - crop[1],
                "pivotX": pivot[0], "pivotY": pivot[1],
            }

        # Body: union sheep bbox across variants.
        bb = body.getbbox()
        body_crop = (
            max(bb[0] - BODY_PAD, 0),
            max(bb[1] - BODY_PAD, 0),
            min(bb[2] + BODY_PAD, 1024),
            min(bb[3] + BODY_PAD, 1024),
        )
        body_layer = body.crop(body_crop)
        fname = f"sheep_{variant}_body.png"
        body_layer.save(os.path.join(TMP_DIR, fname))
        saved[(variant, "body")] = fname
        meta["body"][variant] = {
            "file": fname,
            "offsetX": body_crop[0], "offsetY": body_crop[1],
            "width": body_crop[2] - body_crop[0], "height": body_crop[3] - body_crop[1],
        }

    # Neutral composite preview (legs behind body) for visual check.
    for variant in VARIANTS:
        comp = Image.new("RGBA", (1024, 1024), (255, 255, 255, 0))
        for name in LEG_ORDER:
            crop, _ = leg_boxes[name]
            leg = Image.open(os.path.join(TMP_DIR, saved[(variant, name)]))
            comp.alpha_composite(leg, (crop[0], crop[1]))
        body_img = Image.open(os.path.join(TMP_DIR, saved[(variant, "body")]))
        comp.alpha_composite(body_img, (meta["body"][variant]["offsetX"], meta["body"][variant]["offsetY"]))
        comp.save(os.path.join(TMP_DIR, f"composite_{variant}.png"))

    print(json.dumps(meta, indent=2))

if __name__ == "__main__":
    main()
