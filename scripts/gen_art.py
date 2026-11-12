"""Draws the mod's original art. Needs Pillow.

python3 scripts/gen_art.py <frame_texture.png> <icon.png> [<modrinth_icon.png>]

- frame texture: 16x16 iron frame ring used by the Cloak Frame model
- icon: 128x128 mod icon, a brick block half peeled open on a glowing cable
- modrinth icon: 512x512 version of the icon on a rounded tile
"""
import sys
from PIL import Image, ImageDraw, ImageFilter

IRON = [(52, 54, 60), (88, 92, 100), (138, 142, 150), (186, 190, 196), (226, 228, 232)]


def frame_texture():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            ring = min(x, y, 15 - x, 15 - y)
            if ring > 1:
                continue
            # outer pixel lighter top-left, darker bottom-right; inner pixel mid
            if ring == 0:
                c = IRON[4] if (x == 0 or y == 0) and x + y < 16 else IRON[1]
            else:
                c = IRON[2] if x + y < 15 else IRON[1]
            img.putpixel((x, y), c + (255,))
    # rivets in the corners
    for (x, y) in [(1, 1), (14, 1), (1, 14), (14, 14)]:
        img.putpixel((x, y), IRON[0] + (255,))
    return img


BRICK = [(92, 44, 34), (134, 64, 46), (164, 84, 60), (190, 110, 82)]
MORTAR = (168, 160, 150)
CABLE = [(14, 70, 92), (24, 128, 160), (56, 225, 255), (190, 248, 255)]


def icon_sprite():
    """16x16: a brick cloak in an iron frame, the lower-right half cut away to show the cable inside."""
    spr = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    frame = frame_texture()
    for y in range(16):
        for x in range(16):
            cut = x + y > 17  # lower-right triangle is open
            if not cut:
                # brick courses of 4 rows, offset every other course
                row = y % 4
                off = 0 if (y // 4) % 2 == 0 else 4
                if row == 3 or (x + off) % 8 == 7:
                    c = MORTAR
                else:
                    c = BRICK[2 if row == 0 else 1 if row == 2 else 2]
                    if (x * 7 + y * 3) % 11 == 0:
                        c = BRICK[3]
                spr.putpixel((x, y), c + (255,))
    # the cable: a horizontal pipe through the middle, visible in the cut
    for y in range(6, 10):
        for x in range(16):
            if x + y > 17:
                shade = {6: 3, 7: 2, 8: 1, 9: 0}[y]
                spr.putpixel((x, y), CABLE[shade] + (255,))
    for y in range(16):
        for x in range(6, 10):
            if x + y > 17:
                shade = {6: 3, 7: 2, 8: 1, 9: 0}[x]
                spr.putpixel((x, y), CABLE[shade] + (255,))
    # dark edge along the cut
    for x in range(2, 16):
        y = 17 - x
        if 0 <= y < 16:
            spr.putpixel((x, y), BRICK[0] + (255,))
    spr.alpha_composite(frame)
    return spr


def place(canvas, spr, scale, dy, shadow_blur, shadow_off):
    size = canvas.size[0]
    big = spr.resize((16 * scale, 16 * scale), Image.NEAREST)
    x0 = (size - big.size[0]) // 2
    y0 = (size - big.size[1]) // 2 + dy
    shadow = Image.new("RGBA", big.size, (0, 0, 0, 0))
    shadow.putalpha(big.split()[3].point(lambda v: 150 if v else 0))
    canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(shadow_blur)), (x0 + shadow_off[0], y0 + shadow_off[1]))
    canvas.alpha_composite(big, (x0, y0))


def main():
    frame_texture().save(sys.argv[1])
    spr = icon_sprite()

    n = 128
    icon = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    glow = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse((22, 22, n - 22, n - 22), fill=(56, 225, 255, 100))
    icon.alpha_composite(glow.filter(ImageFilter.GaussianBlur(16)))
    place(icon, spr, 7, 0, 3, (2, 4))
    icon.save(sys.argv[2])

    if len(sys.argv) > 3:
        t = 512
        tile = Image.new("RGBA", (t, t), (0, 0, 0, 0))
        bg = Image.new("RGBA", (t, t), (0, 0, 0, 0))
        ImageDraw.Draw(bg).rounded_rectangle((0, 0, t - 1, t - 1), radius=100, fill=(22, 26, 32, 255))
        glow = Image.new("RGBA", (t, t), (0, 0, 0, 0))
        ImageDraw.Draw(glow).ellipse((96, 96, t - 96, t - 96), fill=(56, 225, 255, 140))
        tile.alpha_composite(bg)
        tile.alpha_composite(Image.composite(glow.filter(ImageFilter.GaussianBlur(80)), Image.new("RGBA", (t, t)), bg.split()[3]))
        place(tile, spr, 22, 6, 10, (6, 14))
        tile.save(sys.argv[3])


main()
