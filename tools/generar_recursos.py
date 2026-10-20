"""
Genera los recursos gráficos y de datos de Truco Ya (Fase 2):
  - Texturas de bloques (silla criolla, mesa de truco, anotador, porotos)
  - Atlas de las 40 cartas españolas + dorso: ver generar_cartas.py (alta resolución)
  - Fondo del tapete para la GUI
  - Modelos de bloque/ítem, blockstates (incluye el multipart de porotos del anotador)
  - sounds.json, recetas, loot tables, traducciones
Uso:  python3 tools/generar_recursos.py   (desde la raíz del proyecto)
"""
import json, math, os, random
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
A = os.path.join(ROOT, "assets", "trucoya")
D = os.path.join(ROOT, "data", "trucoya")
MOD = "trucoya"
rnd = random.Random(1806)


def path(*p):
    full = os.path.join(*p)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    return full


def wjson(obj, *p):
    with open(path(*p), "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write("\n")


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (c[3],)


# ---------------------------------------------------------------- texturas 16x16

def wood(base, grain_dark, size=16, vertical=False, seed=0):
    r = random.Random(seed)
    img = Image.new("RGBA", (size, size), hexc(base))
    px = img.load()
    for y in range(size):
        line_k = 1.0 + 0.08 * math.sin(y * 1.7 + seed) + r.uniform(-0.04, 0.04)
        for x in range(size):
            k = line_k + r.uniform(-0.05, 0.05)
            px[x, y] = shade(hexc(base), k)
    for _ in range(3):
        y0 = r.randrange(size)
        for x in range(size):
            yy = (y0 + int(1.2 * math.sin(x / 3.0 + seed))) % size
            px[x, yy] = hexc(grain_dark)
    if vertical:
        img = img.rotate(90)
    return img


def felt(base, size, border=None, seed=0):
    r = random.Random(seed)
    img = Image.new("RGBA", (size, size), hexc(base))
    px = img.load()
    for y in range(size):
        for x in range(size):
            px[x, y] = shade(hexc(base), 1.0 + r.uniform(-0.07, 0.07))
    if border:
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, size - 1, size - 1], outline=hexc(border))
    return img


def woven(a, b, size=16):
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            band = ((x // 4) + (y // 4)) % 2
            edge = (x % 4 == 0) if band else (y % 4 == 0)
            c = hexc(a) if band else hexc(b)
            px[x, y] = shade(c, 0.78 if edge else 1.0 + rnd.uniform(-0.04, 0.04))
    return img


def bean(color, speck):
    img = Image.new("RGBA", (16, 16), hexc(color))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = shade(hexc(color), 1.0 + rnd.uniform(-0.06, 0.06))
    for _ in range(6):
        px[rnd.randrange(16), rnd.randrange(16)] = hexc(speck)
    return img


def save(img, *p):
    img.save(path(*p))


TB = (A, "textures", "block")
save(wood("#6B4423", "#4A2D16", seed=1), *TB, "silla_madera.png")
save(woven("#A0703C", "#8A5A2C"), *TB, "silla_asiento.png")
save(wood("#7E4F27", "#5A3519", seed=2), *TB, "mesa_madera.png")
save(felt("#2E6B3A", 16, border="#24552E", seed=3), *TB, "mesa_tapete.png")
save(wood("#C49A6C", "#A47A4E", seed=4), *TB, "anotador_tabla.png")
save(bean("#EFE6D2", "#B9AE95"), *TB, "poroto_blanco.png")
save(bean("#3A2219", "#1E120C"), *TB, "poroto_negro.png")

# ---------------------------------------------------------------- cartas

SS = 4                    # supersampling
CW, CH = 48, 72           # tamaño final de cada carta en el atlas
SUITS = ["espada", "basto", "oro", "copa"]
NUMBERS = [1, 2, 3, 4, 5, 6, 7, 10, 11, 12]
SUIT_COLOR = {"espada": "#2F5FA8", "basto": "#3F7A2A", "oro": "#C8961E", "copa": "#B3262C"}
GAPS = {"oro": 0, "copa": 1, "espada": 2, "basto": 3}  # cortes del marco, como la baraja española

FONT_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf"


def font(px):
    try:
        return ImageFont.truetype(FONT_BOLD, px)
    except OSError:
        return ImageFont.load_default()


def draw_oro(d, cx, cy, r):
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=hexc("#E8B82A"), outline=hexc("#7A5200"), width=max(2, r // 6))
    r2 = int(r * 0.62)
    d.ellipse([cx - r2, cy - r2, cx + r2, cy + r2], outline=hexc("#9C6A00"), width=max(2, r // 8))
    r3 = max(2, r // 5)
    d.ellipse([cx - r3, cy - r3, cx + r3, cy + r3], fill=hexc("#B3262C"))


def draw_copa(d, cx, cy, r):
    gold, red, dark = hexc("#E8B82A"), hexc("#B3262C"), hexc("#5A1012")
    bw = r
    d.pieslice([cx - bw, cy - r - r // 2, cx + bw, cy + r // 3], 0, 180, fill=red, outline=dark, width=max(2, r // 7))
    d.rectangle([cx - bw, cy - r // 2 - 2, cx + bw, cy - r // 2 + r // 5], fill=gold, outline=dark, width=max(1, r // 10))
    d.rectangle([cx - r // 7, cy + r // 4, cx + r // 7, cy + int(r * 0.75)], fill=gold, outline=dark, width=max(1, r // 10))
    d.ellipse([cx - int(r * 0.6), cy + int(r * 0.65), cx + int(r * 0.6), cy + r], fill=gold, outline=dark, width=max(1, r // 10))


def draw_espada(d, cx, cy, r):
    blade, edge, dark = hexc("#7FA6D6"), hexc("#D9E6F5"), hexc("#1F3E70")
    w = max(3, r // 4)
    top, guard = cy - r, cy + int(r * 0.35)
    d.polygon([(cx, top - r // 4), (cx + w, top + r // 4), (cx + w, guard), (cx - w, guard), (cx - w, top + r // 4)],
              fill=blade, outline=dark)
    d.line([(cx, top), (cx, guard - 2)], fill=edge, width=max(1, w // 3))
    d.rectangle([cx - int(r * 0.6), guard, cx + int(r * 0.6), guard + max(3, r // 6)], fill=hexc("#E8B82A"), outline=dark)
    d.rectangle([cx - w // 2 - 1, guard + r // 6, cx + w // 2 + 1, cy + int(r * 0.85)], fill=hexc("#6B4423"), outline=dark)
    rr = max(3, r // 6)
    d.ellipse([cx - rr, cy + int(r * 0.85) - 1, cx + rr, cy + int(r * 0.85) + 2 * rr - 1], fill=hexc("#E8B82A"), outline=dark)


def draw_basto(d, cx, cy, r):
    wood_c, dark, leaf = hexc("#8A5A2C"), hexc("#3E2410"), hexc("#3F7A2A")
    top_w, bot_w = int(r * 0.55), int(r * 0.22)
    d.polygon([(cx - top_w, cy - r), (cx + top_w, cy - r), (cx + bot_w, cy + r), (cx - bot_w, cy + r)],
              fill=wood_c, outline=dark)
    d.ellipse([cx - top_w, cy - r - top_w // 2, cx + top_w, cy - r + top_w // 2], fill=shade(wood_c, 1.15), outline=dark)
    for k in (-0.35, 0.15, 0.55):
        yy = cy + int(r * k)
        kx = cx + (1 if k > 0 else -1) * int(r * 0.25)
        kr = max(2, r // 7)
        d.ellipse([kx - kr, yy - kr, kx + kr, yy + kr], fill=leaf, outline=dark)


DRAW = {"oro": draw_oro, "copa": draw_copa, "espada": draw_espada, "basto": draw_basto}

PIPS = {
    1: [(0.5, 0.5)],
    2: [(0.5, 0.27), (0.5, 0.73)],
    3: [(0.5, 0.22), (0.5, 0.5), (0.5, 0.78)],
    4: [(0.3, 0.27), (0.7, 0.27), (0.3, 0.73), (0.7, 0.73)],
    5: [(0.3, 0.25), (0.7, 0.25), (0.5, 0.5), (0.3, 0.75), (0.7, 0.75)],
    6: [(0.3, 0.22), (0.7, 0.22), (0.3, 0.5), (0.7, 0.5), (0.3, 0.78), (0.7, 0.78)],
    7: [(0.3, 0.2), (0.7, 0.2), (0.5, 0.35), (0.3, 0.52), (0.7, 0.52), (0.3, 0.8), (0.7, 0.8)],
}


def frame(d, w, h, color, gaps):
    m = 3 * SS
    d.rounded_rectangle([m, m, w - m, h - m], radius=3 * SS, outline=hexc(color), width=2 * SS)
    paper = hexc("#F7F0DE")
    if gaps:
        step = (h - 2 * m) / (gaps + 1)
        for g in range(1, gaps + 1):
            y = int(m + step * g)
            for x0 in (m - SS, w - m - SS):
                d.rectangle([x0, y - 2 * SS, x0 + 3 * SS, y + 2 * SS], fill=paper)


def draw_figure(d, w, h, suit, number):
    col = hexc(SUIT_COLOR[suit])
    dark = hexc("#2A1A10")
    cx = w // 2
    # cuerpo / manto
    d.polygon([(cx - 11 * SS, h - 12 * SS), (cx + 11 * SS, h - 12 * SS), (cx + 7 * SS, 34 * SS), (cx - 7 * SS, 34 * SS)],
              fill=col, outline=dark)
    # cabeza
    d.ellipse([cx - 5 * SS, 23 * SS, cx + 5 * SS, 33 * SS], fill=hexc("#E9C4A0"), outline=dark)
    if number == 12:  # rey: corona
        d.polygon([(cx - 6 * SS, 23 * SS), (cx - 6 * SS, 17 * SS), (cx - 3 * SS, 20 * SS), (cx, 16 * SS),
                   (cx + 3 * SS, 20 * SS), (cx + 6 * SS, 17 * SS), (cx + 6 * SS, 23 * SS)],
                  fill=hexc("#E8B82A"), outline=dark)
    elif number == 11:  # caballo: cabeza de caballo al costado
        d.polygon([(cx + 6 * SS, 30 * SS), (cx + 14 * SS, 24 * SS), (cx + 17 * SS, 27 * SS), (cx + 12 * SS, 36 * SS)],
                  fill=hexc("#8A5A2C"), outline=dark)
        d.rectangle([cx - 6 * SS, 21 * SS, cx + 6 * SS, 24 * SS], fill=hexc("#3A2219"))  # chambergo
    else:  # sota: gorro con pluma
        d.rectangle([cx - 6 * SS, 21 * SS, cx + 6 * SS, 24 * SS], fill=col, outline=dark)
        d.line([(cx + 4 * SS, 21 * SS), (cx + 9 * SS, 15 * SS)], fill=hexc("#F7F0DE"), width=2 * SS)
    # emblema del palo sobre el pecho
    DRAW[suit](d, cx, 46 * SS, 6 * SS)


def card(suit, number):
    w, h = CW * SS, CH * SS
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 0, w - 1, h - 1], radius=5 * SS, fill=hexc("#F7F0DE"), outline=hexc("#4A3A28"), width=SS)
    frame(d, w, h, SUIT_COLOR[suit], GAPS[suit])
    f = font(9 * SS)
    label = str(number)
    d.text((6 * SS, 4 * SS), label, font=f, fill=hexc(SUIT_COLOR[suit]))
    tw = d.textlength(label, font=f)
    d.text((w - 6 * SS - tw, h - 14 * SS), label, font=f, fill=hexc(SUIT_COLOR[suit]))
    if number <= 7:
        area = (8 * SS, 13 * SS, w - 8 * SS, h - 13 * SS)
        pips = PIPS[number]
        r = int((7.5 if number == 1 else 5.2 if number <= 3 else 4.6) * SS)
        if number == 1:
            r = int(11 * SS)
        for px_, py_ in pips:
            cx = int(area[0] + (area[2] - area[0]) * px_)
            cy = int(area[1] + (area[3] - area[1]) * py_)
            DRAW[suit](d, cx, cy, r)
    else:
        draw_figure(d, w, h, suit, number)
    return img.resize((CW, CH), Image.LANCZOS)


def back():
    w, h = CW * SS, CH * SS
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 0, w - 1, h - 1], radius=5 * SS, fill=hexc("#7A1E22"), outline=hexc("#2A0A0C"), width=SS)
    m = 4 * SS
    d.rounded_rectangle([m, m, w - m, h - m], radius=3 * SS, outline=hexc("#E8B82A"), width=SS)
    step = 6 * SS
    for y in range(m, h - m, step):
        for x in range(m, w - m, step):
            if ((x - m) // step + (y - m) // step) % 2 == 0:
                d.polygon([(x + step // 2, y), (x + step, y + step // 2), (x + step // 2, y + step), (x, y + step // 2)],
                          fill=hexc("#94282D"))
    draw_oro(d, w // 2, h // 2, 8 * SS)
    return img.resize((CW, CH), Image.LANCZOS)


# Los atlas de las barajas (textures/gui y textures/block/cartas_<clave>.png) se hacen con barajas_temas.py.
import generar_cartas  # noqa: E402
generar_cartas.main()

# Fondo del tapete (GUI 256x256; se usa la región 256x200)
tap = felt("#2E6B3A", 256, seed=9)
dt = ImageDraw.Draw(tap)
wood_frame = wood("#6B4423", "#4A2D16", size=256, seed=11)
mask = Image.new("L", (256, 256), 0)
ImageDraw.Draw(mask).rectangle([0, 0, 255, 199], fill=255)
ImageDraw.Draw(mask).rectangle([8, 8, 247, 191], fill=0)
tap.paste(wood_frame, (0, 0), mask)
dt.rectangle([8, 8, 247, 191], outline=hexc("#1E4A27"))
dt.rectangle([0, 200, 255, 255], fill=(0, 0, 0, 0))
save(tap, A, "textures", "gui", "tapete.png")

# ---------------------------------------------------------------- modelos de bloque


def face(tex, uv=None, cull=None):
    f = {"texture": "#" + tex}
    if uv:
        f["uv"] = uv
    if cull:
        f["cullface"] = cull
    return f


def box(frm, to, tex_all=None, up=None, down=None, sides=None):
    sides = sides or tex_all
    faces = {k: face(sides) for k in ("north", "south", "east", "west")}
    faces["up"] = face(up or tex_all)
    faces["down"] = face(down or tex_all)
    return {"from": frm, "to": to, "faces": faces}


MB = (A, "models", "block")

silla = {
    "parent": "block/block",
    "textures": {"particle": f"{MOD}:block/silla_madera", "madera": f"{MOD}:block/silla_madera",
                 "asiento": f"{MOD}:block/silla_asiento"},
    "elements": [
        box([2, 0, 2], [4, 8, 4], "madera"), box([12, 0, 2], [14, 8, 4], "madera"),
        box([2, 0, 12], [4, 8, 14], "madera"), box([12, 0, 12], [14, 8, 14], "madera"),
        box([1.5, 8, 1.5], [14.5, 10, 14.5], "madera", up="asiento"),
        box([2, 10, 12], [4, 22, 14], "madera"), box([12, 10, 12], [14, 22, 14], "madera"),
        box([4, 14, 12.5], [12, 15.5, 13.5], "madera"), box([4, 19, 12.5], [12, 20.5, 13.5], "madera"),
        box([3, 3, 2.5], [13, 4, 3.5], "madera"), box([3, 3, 12.5], [13, 4, 13.5], "madera"),
    ],
}
wjson(silla, *MB, "silla_criolla.json")

mesa = {
    "parent": "block/block",
    "textures": {"particle": f"{MOD}:block/mesa_madera", "madera": f"{MOD}:block/mesa_madera",
                 "tapete": f"{MOD}:block/mesa_tapete"},
    "elements": [
        box([0, 13, 0], [16, 16, 16], "madera", up="tapete"),
        box([1, 0, 1], [3, 13, 3], "madera"), box([13, 0, 1], [15, 13, 3], "madera"),
        box([1, 0, 13], [3, 13, 15], "madera"), box([13, 0, 13], [15, 13, 15], "madera"),
        box([3, 11, 1.5], [13, 13, 2.5], "madera"), box([3, 11, 13.5], [13, 13, 14.5], "madera"),
        box([1.5, 11, 3], [2.5, 13, 13], "madera"), box([13.5, 11, 3], [14.5, 13, 13], "madera"),
    ],
}
wjson(mesa, *MB, "mesa_truco.json")

wjson({
    "parent": "block/block",
    "textures": {"particle": f"{MOD}:block/anotador_tabla", "tabla": f"{MOD}:block/anotador_tabla"},
    "elements": [box([3, 0, 3], [13, 0.75, 13], "tabla"),
                 box([7.75, 0.75, 3.25], [8.25, 0.9, 12.75], "tabla")],
}, *MB, "anotador_base.json")


def bean_pos(team, i):
    """Posición del poroto i (1..40) del equipo. Grupos de a 5 (4 en cuadro + 1 encima)."""
    g, k = (i - 1) // 5, (i - 1) % 5
    x0 = 3.5 + (g % 4) * 2.4
    z0 = (3.5 if team == 0 else 8.5) + (g // 4) * 2.0
    offs = [(0, 0, 0), (1.2, 0, 0), (0, 0, 1.2), (1.2, 0, 1.2), (0.6, 0.45, 0.6)]
    dx, dy, dz = offs[k]
    return round(x0 + dx, 2), round(0.75 + dy, 2), round(z0 + dz, 2)


TEAM_BEAN = ["poroto_blanco", "poroto_negro"]
TEAM_PROP = ["blancos", "negros"]
for team in (0, 1):
    for i in range(1, 41):
        x, y, z = bean_pos(team, i)
        wjson({
            "parent": "block/block",
            "textures": {"particle": f"{MOD}:block/{TEAM_BEAN[team]}", "p": f"{MOD}:block/{TEAM_BEAN[team]}"},
            "elements": [box([x, y, z], [round(x + 0.8, 2), round(y + 0.5, 2), round(z + 0.8, 2)], "p")],
        }, *MB, "porotos", f"{TEAM_BEAN[team]}_{i}.json")

# ---------------------------------------------------------------- blockstates

BS = (A, "blockstates")
FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}
wjson({"variants": {f"facing={f}": {"model": f"{MOD}:block/silla_criolla", "y": y} for f, y in FACING_Y.items()}},
      *BS, "silla_criolla.json")
wjson({"variants": {"": {"model": f"{MOD}:block/mesa_truco"}}}, *BS, "mesa_truco.json")
multipart = [{"apply": {"model": f"{MOD}:block/anotador_base"}}]
for team in (0, 1):
    for i in range(1, 41):
        multipart.append({
            "when": {TEAM_PROP[team]: "|".join(str(v) for v in range(i, 41))},
            "apply": {"model": f"{MOD}:block/porotos/{TEAM_BEAN[team]}_{i}"},
        })
wjson({"multipart": multipart}, *BS, "anotador.json")

# ---------------------------------------------------------------- modelos de ítem

MI = (A, "models", "item")
for b in ("silla_criolla", "mesa_truco"):
    wjson({"parent": f"{MOD}:block/{b}"}, *MI, f"{b}.json")
# El ítem del anotador muestra la tabla con algunos porotos de ejemplo.
wjson({
    "parent": "block/block",
    "textures": {"particle": f"{MOD}:block/anotador_tabla", "tabla": f"{MOD}:block/anotador_tabla",
                 "b": f"{MOD}:block/poroto_blanco", "n": f"{MOD}:block/poroto_negro"},
    "elements": [box([3, 0, 3], [13, 0.75, 13], "tabla")] +
                [box(list(bean_pos(0, i)), [bean_pos(0, i)[0] + 0.8, bean_pos(0, i)[1] + 0.5, bean_pos(0, i)[2] + 0.8], "b") for i in range(1, 8)] +
                [box(list(bean_pos(1, i)), [bean_pos(1, i)[0] + 0.8, bean_pos(1, i)[1] + 0.5, bean_pos(1, i)[2] + 0.8], "n") for i in range(1, 12)],
    "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, 3, 0], "scale": [1.3, 1.3, 1.3]},
                "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
                "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
                "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
                "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 4, 0], "scale": [0.6, 0.6, 0.6]}},
}, *MI, "anotador.json")

# ---------------------------------------------------------------- sonidos

VOICE_KEYS = ["truco", "retruco", "vale_cuatro", "envido", "real_envido", "falta_envido", "flor", "contraflor",
              "contraflor_al_resto", "con_flor_me_achico", "quiero", "no_quiero", "me_voy_al_mazo"]
sounds = {}
# Las voces de los cantos (voz.<voz>.<canto>) las agrega tools/generar_voces_neural.py, y los discos
# tools/generar_fase5.py: correr esos dos DESPUÉS de este script.
for k in ("card_play", "card_shuffle", "poroto"):
    sounds[k] = {"category": "block", "subtitle": f"subtitles.{MOD}.{k}", "sounds": [f"{MOD}:sfx/{k}"]}
wjson(sounds, A, "sounds.json")

# ---------------------------------------------------------------- recetas y loot

R = (D, "recipes")
wjson({"type": "minecraft:crafting_shaped", "pattern": ["P ", "PP", "SS"],
       "key": {"P": {"tag": "minecraft:planks"}, "S": {"item": "minecraft:stick"}},
       "result": {"item": f"{MOD}:silla_criolla"}}, *R, "silla_criolla.json")
wjson({"type": "minecraft:crafting_shaped", "pattern": ["CWC", "P P", "P P"],
       "key": {"P": {"tag": "minecraft:planks"}, "C": {"item": "minecraft:leather"}, "W": {"tag": "minecraft:wool"}},
       "result": {"item": f"{MOD}:mesa_truco"}}, *R, "mesa_truco.json")
# Anotador: una losa de madera + porotos blancos (polvo de hueso) + porotos negros (carbón o carbón vegetal)
wjson({"type": "minecraft:crafting_shapeless",
       "ingredients": [{"tag": "minecraft:wooden_slabs"}, {"item": "minecraft:bone_meal"}, {"tag": "minecraft:coals"}],
       "result": {"item": f"{MOD}:anotador"}}, *R, "anotador.json")
# Desbloqueo en el libro de recetas (aparecen solas al tener madera)
for rec in ("silla_criolla", "mesa_truco", "anotador"):
    wjson({"parent": "minecraft:recipes/root",
           "rewards": {"recipes": [f"{MOD}:{rec}"]},
           "criteria": {"tiene_madera": {"trigger": "minecraft:inventory_changed",
                                         "conditions": {"items": [{"tag": "minecraft:planks"}]}},
                        "tiene_losa": {"trigger": "minecraft:inventory_changed",
                                       "conditions": {"items": [{"tag": "minecraft:wooden_slabs"}]}},
                        "tiene_receta": {"trigger": "minecraft:recipe_unlocked",
                                         "conditions": {"recipe": f"{MOD}:{rec}"}}},
           "requirements": [["tiene_madera", "tiene_losa", "tiene_receta"]]},
          D, "advancements", "recipes", f"{rec}.json")

for b in ("silla_criolla", "mesa_truco", "anotador"):
    wjson({"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD}:{b}"}],
                                                 "conditions": [{"condition": "minecraft:survives_explosion"}]}]},
          D, "loot_tables", "blocks", f"{b}.json")

# ---------------------------------------------------------------- Fase 3: ítems

TI = (A, "textures", "item")


def item_baraja():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for k, off in enumerate((3, 2, 1)):
        d.rectangle([2 + off, 1 + k, 12 + off - 3, 14 - (2 - k)], fill=hexc("#C8961E"), outline=hexc("#6B4A00"))
    d.rectangle([3, 3, 10, 13], fill=hexc("#F2CF4A"), outline=hexc("#7A5200"))
    d.rectangle([5, 5, 8, 11], outline=hexc("#B3262C"))
    d.point([(6, 8), (7, 8), (6, 7), (7, 9)], fill=hexc("#B3262C"))
    return img


def item_ficha():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([2, 2, 13, 13], fill=hexc("#1E1E1E"), outline=hexc("#C8961E"))
    d.ellipse([5, 5, 10, 10], outline=hexc("#C8961E"))
    for x, y in ((7, 2), (7, 13), (2, 7), (13, 7)):
        d.point((x, y), fill=hexc("#E8E2D6"))
    d.point([(7, 7), (8, 8)], fill=hexc("#B3262C"))
    return img


save(item_baraja(), *TI, "baraja_de_oro.png")
save(item_ficha(), *TI, "ficha_clandestina.png")
for it in ("baraja_de_oro", "ficha_clandestina"):
    wjson({"parent": "item/generated", "textures": {"layer0": f"{MOD}:item/{it}"}}, *MI, f"{it}.json")
for egg in ("huevo_paisano", "huevo_cantinero", "huevo_forastero", "huevo_vizcacha",
            "huevo_paisana", "huevo_matrero", "huevo_peon", "huevo_payador"):
    wjson({"parent": "item/template_spawn_egg"}, *MI, f"{egg}.json")

wjson({"replace": False, "values": [
    "minecraft:gold_ingot", "minecraft:gold_block", "minecraft:gold_nugget", "minecraft:emerald", "minecraft:emerald_block",
    "minecraft:diamond", "minecraft:diamond_block", "minecraft:netherite_ingot", "minecraft:netherite_block",
    "minecraft:netherite_scrap", "trucoya:baraja_de_oro"]}, D, "tags", "items", "apuesta_clandestina.json")

# ---------------------------------------------------------------- traducciones

SPEECH = {"truco": "¡Truco!", "retruco": "¡Quiero retruco!", "vale_cuatro": "¡Quiero vale cuatro!",
          "envido": "¡Envido!", "real_envido": "¡Real envido!", "falta_envido": "¡Falta envido!", "flor": "¡Flor!",
          "contraflor": "¡Contraflor!", "contraflor_al_resto": "¡Contraflor al resto!",
          "con_flor_me_achico": "Con flor me achico", "quiero": "¡Quiero!", "no_quiero": "No quiero",
          "me_voy_al_mazo": "Me voy al mazo"}
es = {
    "itemGroup.trucoya": "Truco Ya",
    "block.trucoya.silla_criolla": "Silla Criolla",
    "block.trucoya.mesa_truco": "Mesa de Truco",
    "block.trucoya.anotador": "Anotador con Porotos",
    "key.categories.trucoya": "Truco Ya",
    "key.trucoya.senas": "Señas",
    "key.trucoya.tapete": "Abrir el tapete",
    "subtitles.trucoya.card_play": "Carta sobre la mesa",
    "subtitles.trucoya.card_shuffle": "Se mezcla el mazo",
    "subtitles.trucoya.poroto": "Cae un poroto",
    "entity.trucoya.gaucho": "Gaucho",
    "entity.trucoya.seat": "Asiento",
    "item.trucoya.baraja_de_oro": "Baraja de Oro",
    "item.trucoya.ficha_clandestina": "Ficha de Clandestina",
    "item.trucoya.huevo_paisano": "Huevo de Paisano",
    "item.trucoya.huevo_cantinero": "Huevo de Cantinero",
    "item.trucoya.huevo_forastero": "Huevo de Gaucho Forastero",
    "item.trucoya.huevo_vizcacha": "Huevo del Viejo Vizcacha",
    "item.trucoya.huevo_paisana": "Huevo de Paisana",
    "item.trucoya.huevo_matrero": "Huevo de Matrero",
    "item.trucoya.huevo_peon": "Huevo de Peón de Estancia",
    "item.trucoya.huevo_payador": "Huevo de Payador",
}
for k, v in SPEECH.items():
    es[f"subtitles.trucoya.voice.{k}"] = v
en = dict(es)
en.update({
    "block.trucoya.silla_criolla": "Criollo Chair",
    "block.trucoya.mesa_truco": "Truco Table",
    "block.trucoya.anotador": "Bean Scoreboard",
    "key.trucoya.senas": "Signals",
    "key.trucoya.tapete": "Open card table",
    "subtitles.trucoya.card_play": "Card played",
    "subtitles.trucoya.card_shuffle": "Deck shuffled",
    "subtitles.trucoya.poroto": "Bean drops",
    "entity.trucoya.gaucho": "Gaucho",
    "item.trucoya.baraja_de_oro": "Golden Deck",
    "item.trucoya.ficha_clandestina": "Clandestine Token",
    "item.trucoya.huevo_paisano": "Paisano Spawn Egg",
    "item.trucoya.huevo_cantinero": "Barkeeper Spawn Egg",
    "item.trucoya.huevo_forastero": "Wandering Gaucho Spawn Egg",
    "item.trucoya.huevo_vizcacha": "Old Vizcacha Spawn Egg",
    "item.trucoya.huevo_paisana": "Paisana Spawn Egg",
    "item.trucoya.huevo_matrero": "Outlaw Gaucho Spawn Egg",
    "item.trucoya.huevo_peon": "Ranch Hand Spawn Egg",
    "item.trucoya.huevo_payador": "Payador Spawn Egg",
})
for loc in ("es_es", "es_uy", "es_ar", "es_mx"):
    wjson(es, A, "lang", f"{loc}.json")
wjson(en, A, "lang", "en_us.json")

print("Recursos generados en", os.path.abspath(ROOT))
