"""
Vista previa de las plantillas de estructuras (.nbt) con las texturas de Minecraft: dibujo isométrico con caras
sombreadas, escalones, losas, cercos, muros, rejas, trampillas, faroles, flores y hojas. Sirve para revisar
el diseño sin abrir el juego.

Necesita el .jar del cliente de Minecraft 1.16.5 (de ahí salen las texturas). Gradle lo deja en
  %USERPROFILE%/.gradle/caches/forge_gradle/... (client-extra.jar)
Uso:
  python3 tools/vista_estructuras.py <plantilla.nbt> <salida.png> [--jar client-extra.jar] [--escala 16]
         [--girar 0|1|2|3] [--hasta Y]
"""
import argparse, glob, io, math, os, sys, zipfile
import nbtlib
from PIL import Image, ImageDraw, ImageEnhance

TINTES = {"grass_block_top": (124, 189, 107), "grass": (124, 189, 107), "tall_grass": (124, 189, 107), "fern": (110, 170, 95),
          "large_fern": (110, 170, 95), "oak_leaves": (87, 150, 60), "dark_oak_leaves": (80, 140, 55),
          "jungle_leaves": (70, 160, 50), "acacia_leaves": (110, 150, 50), "birch_leaves": (128, 167, 85),
          "spruce_leaves": (97, 153, 97), "vine": (87, 150, 60), "lily_pad": (32, 128, 48), "water_still": (63, 118, 228),
          "sugar_cane": (124, 189, 107)}

TRANSPARENTES = ("glass", "leaves", "bars", "pane", "fence", "wall", "stairs", "slab", "door", "trapdoor", "lantern",
                 "torch", "chain", "ladder", "flower", "grass", "fern", "bush", "sapling", "rose", "tulip", "poppy",
                 "dandelion", "orchid", "allium", "bluet", "daisy", "cornflower", "lily", "sign", "carpet", "potted",
                 "campfire", "bell", "cauldron", "brewing", "anvil", "lectern", "water", "path", "farmland", "snow",
                 "button", "pressure_plate", "candle", "sea_pickle", "mushroom", "wheat", "carrots", "potatoes",
                 "beetroots", "cane", "vine", "cobweb", "mesa_truco", "silla", "anotador", "raya", "picada", "yerbal",
                 "bed", "chest", "barrel_", "composter", "hopper", "dead_bush", "kelp", "head", "skull", "end_rod")


class Texturas:
    def __init__(self, jar):
        self.z = zipfile.ZipFile(jar)
        self.n = set(self.z.namelist())
        self.cache = {}

    def get(self, nombre):
        if nombre in self.cache:
            return self.cache[nombre]
        p = f"assets/minecraft/textures/block/{nombre}.png"
        img = None
        if p in self.n:
            img = Image.open(io.BytesIO(self.z.read(p))).convert("RGBA")
            if img.height > img.width:                       # animadas: el primer cuadro
                img = img.crop((0, 0, img.width, img.width))
            if nombre in TINTES:      # pasto y hojas vienen en gris: se tiñen como en un bioma templado
                gris = img.convert("L")
                img = Image.merge("RGBA", [gris.point(lambda v, k=k: v * TINTES[nombre][k] // 255)
                                           for k in range(3)] + [img.split()[3]])
        self.cache[nombre] = img
        return img

    def primera(self, *nombres):
        for n in nombres:
            t = self.get(n)
            if t is not None:
                return t
        return None


def base_de(n):
    """Nombre de bloque sin sufijos de forma: oak_stairs -> oak (y alias de texturas)."""
    for suf in ("_stairs", "_slab", "_wall", "_fence_gate", "_fence", "_pressure_plate", "_button", "_trapdoor", "_door",
                "_carpet", "_wall_sign", "_sign", "_pane"):
        if n.endswith(suf):
            return n[: -len(suf)], suf
    return n, ""


ALIAS = {"brick": "bricks", "stone_brick": "stone_bricks", "mossy_stone_brick": "mossy_stone_bricks",
         "nether_brick": "nether_bricks", "red_nether_brick": "red_nether_bricks", "end_stone_brick": "end_stone_bricks",
         "polished_blackstone_brick": "polished_blackstone_bricks", "quartz": "quartz_block_side",
         "smooth_quartz": "quartz_block_bottom", "smooth_sandstone": "sandstone_top", "smooth_red_sandstone": "red_sandstone_top",
         "smooth_stone": "smooth_stone", "prismarine_brick": "prismarine_bricks", "purpur": "purpur_block",
         "petrified_oak": "oak_planks", "cut_sandstone": "cut_sandstone", "snow_block": "snow", "hay_block": "hay_block_side",
         "glass": "glass", "iron": "iron_block"}
MADERAS = ("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "crimson", "warped")


def texturas_de(tx, nombre, props):
    """(arriba, costado) para un bloque."""
    n = nombre.split(":")[-1]
    b, suf = base_de(n)
    if n.startswith("trucoya:") or nombre.startswith("trucoya:"):
        t = tx.get("dark_oak_planks") if "mesa" in n else tx.get("spruce_planks")
        return (tx.get("green_wool") if "mesa" in n else t), t
    if b in MADERAS and suf:
        if suf in ("_door", "_trapdoor"):
            t = tx.primera(f"{b}_trapdoor")
            return t, t
        p = tx.get(f"{b}_planks")
        return p, p
    if n.endswith("_log") or n.endswith("_wood") or n.endswith("_stem") or n.endswith("_hyphae"):
        raiz = n.replace("_wood", "_log").replace("_hyphae", "_stem")
        lado = tx.get(raiz)
        tapa = tx.get(raiz + "_top") if n.endswith("_log") or n.endswith("_stem") else lado
        eje = props.get("axis", "y")
        return (tapa, lado) if eje == "y" else (lado, lado)
    if n in ("quartz_pillar", "purpur_pillar"):
        return tx.get(n + "_top"), tx.get(n)
    b2 = ALIAS.get(b, b)
    arriba = tx.primera(b2 + "_top", b2, b2 + "_block", b + "_planks", n)
    costado = tx.primera(b2 + "_side", b2, b2 + "_front", b2 + "_block", b + "_planks", n + "_side", n)
    if n == "grass_block":
        arriba, costado = tx.get("grass_block_top"), tx.get("grass_block_side")
    if n in ("barrel",):
        arriba, costado = tx.get("barrel_top"), tx.get("barrel_side")
    if n in ("furnace", "smoker", "blast_furnace"):
        arriba, costado = tx.get(n + "_top") or tx.get("furnace_top"), tx.get(n + "_front") or tx.get("furnace_front")
    if n == "bookshelf":
        arriba, costado = tx.get("oak_planks"), tx.get("bookshelf")
    if n == "crafting_table":
        arriba, costado = tx.get("crafting_table_top"), tx.get("crafting_table_front")
    if n in ("water",):
        arriba = costado = tx.get("water_still")
    if n == "lava":
        arriba = costado = tx.get("lava_still")
    if n == "campfire":
        arriba, costado = tx.get("campfire_log_lit"), tx.get("campfire_log")
    if n == "chest":
        arriba = costado = tx.get("oak_planks")
    if n in ("lantern", "soul_lantern"):
        arriba = costado = tx.get(n)
    if n == "grass_path":
        arriba, costado = tx.get("grass_path_top"), tx.get("grass_path_side")
    if n == "farmland":
        arriba, costado = tx.get("farmland_moist"), tx.get("dirt")
    if n == "snow":
        arriba = costado = tx.get("snow")
    if n == "podzol":
        arriba, costado = tx.get("podzol_top"), tx.get("podzol_side")
    if n == "mycelium":
        arriba, costado = tx.get("mycelium_top"), tx.get("mycelium_side")
    if n == "composter":
        arriba, costado = tx.get("composter_top"), tx.get("composter_side")
    if n == "cauldron":
        arriba, costado = tx.get("cauldron_top"), tx.get("cauldron_side")
    if n == "jukebox":
        arriba, costado = tx.get("jukebox_top"), tx.get("jukebox_side")
    if n == "pumpkin" or n == "carved_pumpkin" or n == "jack_o_lantern":
        arriba, costado = tx.get("pumpkin_top"), tx.get("pumpkin_side")
    if n == "melon":
        arriba, costado = tx.get("melon_top"), tx.get("melon_side")
    if n.endswith("_bed"):
        arriba = costado = tx.get(n.replace("_bed", "_wool"))
    return arriba, costado


def cajas(nombre, props):
    """Cajas (x0,y0,z0,x1,y1,z1) que forman el bloque, o 'cruz' para plantas, o None si no se dibuja."""
    n = nombre.split(":")[-1]
    if n in ("air", "cave_air", "void_air", "structure_void", "barrier", "jigsaw"):
        return None
    if n.endswith("_stairs"):
        f, hf = props.get("facing", "north"), props.get("half", "bottom")
        y0, y1 = (0, 0.5) if hf == "bottom" else (0.5, 1)
        s0, s1 = (0.5, 1) if hf == "bottom" else (0, 0.5)
        paso = {"north": (0, s0, 0, 1, s1, 0.5), "south": (0, s0, 0.5, 1, s1, 1),
                "east": (0.5, s0, 0, 1, s1, 1), "west": (0, s0, 0, 0.5, s1, 1)}[f]
        return [(0, y0, 0, 1, y1, 1), paso]
    if n.endswith("_slab"):
        t = props.get("type", "bottom")
        return [(0, 0, 0, 1, 0.5, 1)] if t == "bottom" else [(0, 0.5, 0, 1, 1, 1)] if t == "top" else [(0, 0, 0, 1, 1, 1)]
    if n.endswith("_fence") or n.endswith("_wall") and "sign" not in n and "torch" not in n:
        a = 0.375 if n.endswith("_fence") else 0.25
        alto = 1.0
        cs = [(a, 0, a, 1 - a, alto, 1 - a)]
        b0, b1 = (0.44, 0.56) if n.endswith("_fence") else (0.31, 0.69)
        top = 0.94 if n.endswith("_fence") else 0.81
        for d, caja in (("north", (b0, 0.38, 0, b1, top, a)), ("south", (b0, 0.38, 1 - a, b1, top, 1)),
                        ("east", (1 - a, 0.38, b0, 1, top, b1)), ("west", (0, 0.38, b0, a, top, b1))):
            if props.get(d) in ("true", "low", "tall"):
                cs.append(caja)
        return cs
    if n.endswith("_fence_gate"):
        f = props.get("facing", "north")
        return [(0, 0.3, 0.44, 1, 0.94, 0.56)] if f in ("north", "south") else [(0.44, 0.3, 0, 0.56, 0.94, 1)]
    if n.endswith("_pane") or n == "iron_bars":
        cs = [(0.44, 0, 0.44, 0.56, 1, 0.56)]
        for d, caja in (("north", (0.44, 0, 0, 0.56, 1, 0.5)), ("south", (0.44, 0, 0.5, 0.56, 1, 1)),
                        ("east", (0.5, 0, 0.44, 1, 1, 0.56)), ("west", (0, 0, 0.44, 0.5, 1, 0.56))):
            if props.get(d) == "true":
                cs.append(caja)
        return cs
    if n.endswith("_trapdoor"):
        if props.get("open") == "true":
            f = props.get("facing", "north")
            return [{"north": (0, 0, 0.81, 1, 1, 1), "south": (0, 0, 0, 1, 1, 0.19), "east": (0, 0, 0, 0.19, 1, 1),
                     "west": (0.81, 0, 0, 1, 1, 1)}[f]]
        return [(0, 0, 0, 1, 0.19, 1)] if props.get("half", "bottom") == "bottom" else [(0, 0.81, 0, 1, 1, 1)]
    if n.endswith("_door"):
        f = props.get("facing", "north")
        return [{"north": (0, 0, 0.81, 1, 1, 1), "south": (0, 0, 0, 1, 1, 0.19), "east": (0, 0, 0, 0.19, 1, 1),
                 "west": (0.81, 0, 0, 1, 1, 1)}[f]]
    if n.endswith("_carpet") or n == "snow":
        return [(0, 0, 0, 1, 0.12, 1)]
    if n.endswith("pressure_plate"):
        return [(0.06, 0, 0.06, 0.94, 0.06, 0.94)]
    if n in ("lantern", "soul_lantern"):
        y0 = 0.06 if props.get("hanging") == "true" else 0
        return [(0.31, y0, 0.31, 0.69, y0 + 0.5, 0.69)]
    if n == "chain":
        return [(0.44, 0, 0.44, 0.56, 1, 0.56)]
    if n in ("campfire", "soul_campfire"):
        return [(0, 0, 0, 1, 0.44, 1)]
    if n.endswith("_bed"):
        return [(0, 0, 0, 1, 0.56, 1)]
    if n in ("chest", "trapped_chest", "ender_chest"):
        return [(0.06, 0, 0.06, 0.94, 0.88, 0.94)]
    if n in ("cauldron", "composter"):
        return [(0, 0, 0, 1, 1, 1)]
    if n.endswith("_wall_sign") or n.endswith("_sign") or n == "ladder" or n.endswith("wall_torch") or n == "torch" \
            or n == "lever" or n.endswith("_button"):
        return "chico"
    if n.startswith("potted_") or n == "flower_pot":
        return [(0.31, 0, 0.31, 0.69, 0.38, 0.69), "cruz_maceta"]
    if n in ("grass", "fern", "dead_bush", "dandelion", "poppy", "blue_orchid", "allium", "azure_bluet", "red_tulip",
             "orange_tulip", "white_tulip", "pink_tulip", "oxeye_daisy", "cornflower", "lily_of_the_valley",
             "sweet_berry_bush", "brown_mushroom", "red_mushroom", "sugar_cane", "wheat", "carrots", "potatoes",
             "beetroots", "oak_sapling", "spruce_sapling", "birch_sapling", "jungle_sapling", "acacia_sapling",
             "dark_oak_sapling", "tall_grass", "large_fern", "rose_bush", "peony", "lilac", "sunflower", "bamboo",
             "cactus", "vine", "kelp", "seagrass", "crimson_roots", "warped_roots", "nether_sprouts"):
        if n == "cactus":
            return [(0.06, 0, 0.06, 0.94, 1, 0.94)]
        return "cruz"
    if n == "lily_pad":
        return [(0, 0, 0, 1, 0.03, 1)]
    if n in ("bell", "brewing_stand", "end_rod", "lightning_rod"):
        return [(0.3, 0.1, 0.3, 0.7, 0.8, 0.7)]
    if n in ("water", "lava"):
        nivel = int(props.get("level", "0"))
        return [(0, 0, 0, 1, 0.88 if nivel == 0 else max(0.1, 0.88 - nivel * 0.11), 1)]
    if nombre.startswith("trucoya:"):
        if "mesa" in n:
            return [(0, 0.81, 0, 1, 1, 1), (0.06, 0, 0.06, 0.19, 0.81, 0.19), (0.81, 0, 0.06, 0.94, 0.81, 0.19),
                    (0.06, 0, 0.81, 0.19, 0.81, 0.94), (0.81, 0, 0.81, 0.94, 0.81, 0.94)]
        if "silla" in n:
            return [(0.12, 0.44, 0.12, 0.88, 0.56, 0.88), (0.12, 0, 0.12, 0.25, 0.44, 0.25), (0.75, 0, 0.75, 0.88, 0.44, 0.88),
                    (0.12, 0, 0.75, 0.25, 0.44, 0.88), (0.75, 0, 0.12, 0.88, 0.44, 0.25)]
        if "anotador" in n:
            return [(0.25, 0, 0.3, 0.75, 0.06, 0.7)]
        return [(0, 0, 0, 1, 1, 1)]
    return [(0, 0, 0, 1, 1, 1)]


def es_cubo_opaco(nombre, props):
    n = nombre.split(":")[-1]
    if any(k in n for k in TRANSPARENTES) or nombre.startswith("trucoya:"):
        return False
    return cajas(nombre, props) == [(0, 0, 0, 1, 1, 1)]


def girar_pos(x, z, sx, sz, k):
    for _ in range(k):
        x, z = sz - 1 - z, x
        sx, sz = sz, sx
    return x, z


ROT = {"north": "east", "east": "south", "south": "west", "west": "north"}


def girar_props(props, k):
    p = dict(props)
    for _ in range(k):
        if "facing" in p and p["facing"] in ROT:
            p["facing"] = ROT[p["facing"]]
        if "axis" in p:
            p["axis"] = {"x": "z", "z": "x"}.get(p["axis"], p["axis"])
        lados = {d: p.get(d) for d in ("north", "east", "south", "west")}
        if any(v is not None for v in lados.values()):
            for d, v in lados.items():
                if v is not None:
                    p[ROT[d]] = v
    return p


def dibujar(plantilla, salida, jar, escala=16, girar=0, hasta=None, fondo=(205, 222, 238)):
    tx = Texturas(jar)
    nb = nbtlib.load(plantilla)
    sx, sy, sz = [int(v) for v in nb["size"]]
    pal = []
    for e in nb["palette"]:
        pal.append((str(e["Name"]), {k: str(v) for k, v in e.get("Properties", {}).items()}))
    gx, gz = (sx, sz) if girar % 2 == 0 else (sz, sx)
    bloques = {}
    for b in nb["blocks"]:
        x, y, z = [int(v) for v in b["pos"]]
        if hasta is not None and y > hasta:
            continue
        nombre, props = pal[int(b["state"])]
        if nombre.split(":")[-1] in ("air", "cave_air", "structure_void"):
            continue
        X, Z = girar_pos(x, z, sx, sz, girar)
        bloques[(X, y, Z)] = (nombre, girar_props(props, girar))
    opacos = {p for p, (nm, pr) in bloques.items() if es_cubo_opaco(nm, pr)}
    c30 = math.cos(math.radians(30))
    s = escala

    def P(x, y, z):
        return ((x - z) * c30 * s, (x + z) * 0.5 * s - y * s)

    W = int((gx + gz) * c30 * s) + 2 * s
    H = int((gx + gz) * 0.5 * s + sy * s) + 2 * s
    ox, oy = gz * c30 * s + s, sy * s + s
    lienzo = Image.new("RGBA", (W, H), fondo + (255,))

    def cara(tex, p0, p1, p3, brillo, recorte=(0, 0, 16, 16)):
        """Pega la textura en el paralelogramo p0 (origen), p1 (eje u), p3 (eje v)."""
        if tex is None:
            return
        t = tex.crop(recorte) if recorte != (0, 0, 16, 16) else tex
        if t.width < 1 or t.height < 1:
            return
        p0 = (p0[0] + ox, p0[1] + oy); p1 = (p1[0] + ox, p1[1] + oy); p3 = (p3[0] + ox, p3[1] + oy)
        p2 = (p1[0] + p3[0] - p0[0], p1[1] + p3[1] - p0[1])
        xs = [p0[0], p1[0], p2[0], p3[0]]; ys = [p0[1], p1[1], p2[1], p3[1]]
        bx0, by0 = int(math.floor(min(xs))), int(math.floor(min(ys)))
        bx1, by1 = int(math.ceil(max(xs))) + 1, int(math.ceil(max(ys))) + 1
        if bx1 - bx0 < 1 or by1 - by0 < 1:
            return
        ux, uy = p1[0] - p0[0], p1[1] - p0[1]
        vx, vy = p3[0] - p0[0], p3[1] - p0[1]
        det = ux * vy - uy * vx
        if abs(det) < 1e-6:
            return
        tw, th = t.width, t.height
        # (X,Y) de salida -> (a,b) en [0,1]x[0,1] -> textura
        ia, ib = vy / det, -vx / det
        ja, jb = -uy / det, ux / det
        a0 = -(ia * (p0[0] - bx0) + ib * (p0[1] - by0))
        b0 = -(ja * (p0[0] - bx0) + jb * (p0[1] - by0))
        coef = (ia * tw, ib * tw, a0 * tw, ja * th, jb * th, b0 * th)
        parche = t.transform((bx1 - bx0, by1 - by0), Image.AFFINE, coef, resample=Image.NEAREST)
        if brillo != 1.0:
            r, g, b_, a = parche.split()
            rgb = Image.merge("RGB", (r, g, b_))
            rgb = ImageEnhance.Brightness(rgb).enhance(brillo)
            parche = Image.merge("RGBA", (*rgb.split(), a))
        mascara = Image.new("L", parche.size, 0)
        ImageDraw.Draw(mascara).polygon([(p[0] - bx0, p[1] - by0) for p in (p0, p1, p2, p3)], fill=255)
        alfa = Image.composite(parche.split()[3], Image.new("L", parche.size, 0), mascara)
        parche.putalpha(alfa)
        lienzo.alpha_composite(parche, (bx0, by0))

    orden = sorted(bloques.items(), key=lambda kv: (kv[0][0] + kv[0][2], kv[0][1], kv[0][0]))
    for (x, y, z), (nombre, props) in orden:
        forma = cajas(nombre, props)
        if forma is None:
            continue
        arriba, costado = texturas_de(tx, nombre, props)
        n = nombre.split(":")[-1]
        if forma in ("cruz", "chico") or (isinstance(forma, list) and "cruz_maceta" in forma):
            if isinstance(forma, list):
                for cj in forma:
                    if isinstance(cj, tuple):
                        x0, y0, z0, x1, y1, z1 = cj
                        maceta = tx.get("flower_pot")
                        cara(maceta, P(x + x0, y + y1, z + z0), P(x + x1, y + y1, z + z0), P(x + x0, y + y1, z + z1), 1.0)
                        cara(maceta, P(x + x0, y + y1, z + z1), P(x + x1, y + y1, z + z1), P(x + x0, y + y0, z + z1), 0.8)
                planta = n.replace("potted_", "")
                tp = tx.primera(planta, planta + "_top", "fern")
                cara(tp, P(x + 0.15, y + 1.0, z + 0.5), P(x + 0.85, y + 1.0, z + 0.5), P(x + 0.15, y + 0.3, z + 0.5), 1.0)
                continue
            if forma == "chico":
                t = tx.primera(n.replace("wall_", "").replace("_sign", "_planks"), n, "oak_planks")
                cara(t, P(x + 0.3, y + 0.8, z + 0.5), P(x + 0.7, y + 0.8, z + 0.5), P(x + 0.3, y + 0.3, z + 0.5), 0.9)
                continue
            planta = {"tall_grass": "tall_grass_bottom", "large_fern": "large_fern_bottom", "rose_bush": "rose_bush_bottom",
                      "peony": "peony_bottom", "lilac": "lilac_bottom", "sunflower": "sunflower_bottom",
                      "sweet_berry_bush": "sweet_berry_bush_stage3", "wheat": "wheat_stage7", "carrots": "carrots_stage3",
                      "potatoes": "potatoes_stage3", "beetroots": "beetroots_stage3", "bamboo": "bamboo_stalk"}.get(n, n)
            if props.get("half") == "upper":
                planta = planta.replace("_bottom", "_top")
            tp = tx.primera(planta, n)
            cara(tp, P(x, y + 1, z), P(x + 1, y + 1, z + 1), P(x, y, z), 1.0)
            cara(tp, P(x + 1, y + 1, z), P(x, y + 1, z + 1), P(x + 1, y, z), 0.9)
            continue
        es_cubo = forma == [(0, 0, 0, 1, 1, 1)]
        for cj in forma:
            x0, y0, z0, x1, y1, z1 = cj
            X0, X1, Y0, Y1, Z0, Z1 = x + x0, x + x1, y + y0, y + y1, z + z0, z + z1
            arriba_tapada = es_cubo and (x, y + 1, z) in opacos
            sur_tapado = es_cubo and (x, y, z + 1) in opacos
            este_tapado = es_cubo and (x + 1, y, z) in opacos
            rec_top = (int(x0 * 16), int(z0 * 16), max(int(x0 * 16) + 1, int(x1 * 16)), max(int(z0 * 16) + 1, int(z1 * 16)))
            rec_z = (int(x0 * 16), int((1 - y1) * 16), max(int(x0 * 16) + 1, int(x1 * 16)), max(int((1 - y1) * 16) + 1, int((1 - y0) * 16)))
            rec_x = (int(z0 * 16), int((1 - y1) * 16), max(int(z0 * 16) + 1, int(z1 * 16)), max(int((1 - y1) * 16) + 1, int((1 - y0) * 16)))
            if not arriba_tapada:
                cara(arriba, P(X0, Y1, Z0), P(X1, Y1, Z0), P(X0, Y1, Z1), 1.0, rec_top)
            if not sur_tapado:
                cara(costado, P(X0, Y1, Z1), P(X1, Y1, Z1), P(X0, Y0, Z1), 0.80, rec_z)
            if not este_tapado:
                cara(costado, P(X1, Y1, Z1), P(X1, Y1, Z0), P(X1, Y0, Z1), 0.62, rec_x)
    lienzo.convert("RGB").save(salida)
    return lienzo.size


def buscar_jar():
    for p in glob.glob(os.path.expanduser("~/.gradle/caches/forge_gradle/**/client-extra.jar"), recursive=True):
        return p
    return None


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("plantilla")
    ap.add_argument("salida")
    ap.add_argument("--jar", default=None)
    ap.add_argument("--escala", type=int, default=16)
    ap.add_argument("--girar", type=int, default=0)
    ap.add_argument("--hasta", type=int, default=None)
    a = ap.parse_args()
    jar = a.jar or buscar_jar()
    if not jar:
        sys.exit("No encuentro client-extra.jar: pasalo con --jar")
    print(dibujar(a.plantilla, a.salida, jar, a.escala, a.girar, a.hasta))
