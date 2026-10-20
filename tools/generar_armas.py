"""
Armas gauchas: texturas (16x16), modelos de ítem, recetas, desbloqueo de recetas y traducciones.
  - Facón, Facón Platero, Rebenque y Boleadoras.
Correr DESPUÉS de generar_recursos.py (agrega sus textos a los archivos de idioma).
Uso: python3 tools/generar_armas.py
"""
import json, os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "src", "main", "resources")
A = os.path.join(RES, "assets", "trucoya")
D = os.path.join(RES, "data", "trucoya")


def wjson(obj, *p):
    full = os.path.join(*p)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)


def hexc(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def lienzo():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def diagonal(img, pts, color):
    px = img.load()
    for x, y in pts:
        if 0 <= x < 16 and 0 <= y < 16:
            px[x, y] = color


def facon(platero=False):
    img = lienzo()
    px = img.load()
    filo, lomo, brillo = hexc("#E4E8EC"), hexc("#9AA2AA"), hexc("#FFFFFF")
    # hoja: de (14,1) a (7,8), dos píxeles de ancho (filo abajo, lomo arriba)
    for i in range(8):
        x, y = 14 - i, 1 + i
        px[x, y] = brillo if i in (2, 4) else filo
        if 0 < i < 7:
            px[x + 1, y] = lomo
    # gavilán (cruz) de bronce, o de oro en el platero
    metal = hexc("#D9B54A") if platero else hexc("#B08A4A")
    for x, y in ((5, 8), (6, 9), (7, 10), (8, 11)):
        px[x, y] = metal
    # cabo de madera / guampa con anillos
    for i in range(4):
        x, y = 5 - i, 10 + i
        px[x, y] = hexc("#3A2418") if platero else hexc("#5A3A22")
        px[x + 1, y] = (hexc("#D9B54A") if i % 2 else hexc("#3A2418")) if platero else hexc("#7A5232")
    px[1, 14] = hexc("#D9B54A") if platero else hexc("#C8C8C0")              # pomo
    px[2, 14] = px[1, 14]
    return img


def rebenque():
    img = lienzo()
    px = img.load()
    cuero, oscuro, plata = hexc("#7A4A26"), hexc("#4A2A14"), hexc("#D8D8D0")
    # lonja (punta ancha) arriba a la derecha
    for x, y in ((13, 1), (14, 1), (13, 2), (14, 2), (12, 2), (12, 3), (13, 3)):
        px[x, y] = cuero
    # trenza
    for i in range(8):
        x, y = 11 - i, 4 + i
        px[x, y] = cuero if i % 2 else oscuro
        px[x + 1, y] = oscuro if i % 2 else cuero
    # virolas de plata y manija
    for x, y in ((3, 12), (4, 12)):
        px[x, y] = plata
    for x, y in ((2, 13), (3, 13), (1, 14), (2, 14)):
        px[x, y] = oscuro
    px[1, 15] = plata
    px[0, 15] = cuero
    return img


def boleadoras():
    img = lienzo()
    px = img.load()
    piedra, sombra, cuero, tiento = hexc("#9A9890"), hexc("#6A6860"), hexc("#7A4A26"), hexc("#B07A44")
    def bola(cx, cy):
        for dx in range(-1, 2):
            for dy in range(-1, 2):
                if abs(dx) + abs(dy) <= 2:
                    px[cx + dx, cy + dy] = cuero if (dx + dy) % 2 else piedra
        px[cx + 1, cy + 1] = sombra
    # cuerdas desde el centro (8,8)
    for i in range(1, 6):
        px[8 - i, 8 - i] = tiento
        px[8 + i, 8 - i] = tiento
    for i in range(1, 5):
        px[8, 8 + i] = tiento
    px[8, 8] = hexc("#5A3A22")
    bola(3, 3)
    bola(12, 3)
    bola(8, 13)
    return img


T = os.path.join(A, "textures", "item")
os.makedirs(T, exist_ok=True)
facon().save(os.path.join(T, "facon.png"))
facon(platero=True).save(os.path.join(T, "facon_platero.png"))
rebenque().save(os.path.join(T, "rebenque.png"))
boleadoras().save(os.path.join(T, "boleadoras.png"))

MI = os.path.join(A, "models", "item")
for it in ("facon", "facon_platero", "rebenque"):
    wjson({"parent": "item/handheld", "textures": {"layer0": f"trucoya:item/{it}"}}, MI, f"{it}.json")
wjson({"parent": "item/generated", "textures": {"layer0": "trucoya:item/boleadoras"}}, MI, "boleadoras.json")

R = os.path.join(D, "recipes")
recetas = {
    "facon": {"type": "minecraft:crafting_shaped", "pattern": [" I", "S "],
              "key": {"I": {"item": "minecraft:iron_ingot"}, "S": {"item": "minecraft:stick"}},
              "result": {"item": "trucoya:facon"}},
    "facon_platero": {"type": "minecraft:crafting_shaped", "pattern": ["  I", " I ", "G  "],
                      "key": {"I": {"item": "minecraft:iron_ingot"}, "G": {"item": "minecraft:gold_ingot"}},
                      "result": {"item": "trucoya:facon_platero"}},
    "rebenque": {"type": "minecraft:crafting_shaped", "pattern": ["  L", " L ", "S  "],
                 "key": {"L": {"item": "minecraft:leather"}, "S": {"item": "minecraft:stick"}},
                 "result": {"item": "trucoya:rebenque"}},
    "boleadoras": {"type": "minecraft:crafting_shaped", "pattern": ["C C", " L ", " C "],
                   "key": {"C": {"item": "minecraft:cobblestone"}, "L": {"item": "minecraft:leather"}},
                   "result": {"item": "trucoya:boleadoras", "count": 2}},
}
desbloqueo = {"facon": "minecraft:iron_ingot", "facon_platero": "minecraft:gold_ingot",
              "rebenque": "minecraft:leather", "boleadoras": "minecraft:leather"}
for nombre, rec in recetas.items():
    wjson(rec, R, f"{nombre}.json")
    wjson({"parent": "minecraft:recipes/root",
           "rewards": {"recipes": [f"trucoya:{nombre}"]},
           "criteria": {"tiene_material": {"trigger": "minecraft:inventory_changed",
                                           "conditions": {"items": [{"item": desbloqueo[nombre]}]}},
                        "tiene_receta": {"trigger": "minecraft:recipe_unlocked",
                                         "conditions": {"recipe": f"trucoya:{nombre}"}}},
           "requirements": [["tiene_material", "tiene_receta"]]},
          D, "advancements", "recipes", f"{nombre}.json")

ES = {"item.trucoya.facon": "Facón", "item.trucoya.facon_platero": "Facón Platero",
      "item.trucoya.rebenque": "Rebenque", "item.trucoya.boleadoras": "Boleadoras",
      "entity.trucoya.boleadoras": "Boleadoras"}
EN = {"item.trucoya.facon": "Facón (Gaucho Knife)", "item.trucoya.facon_platero": "Silver-Trim Facón",
      "item.trucoya.rebenque": "Rebenque (Riding Whip)", "item.trucoya.boleadoras": "Bolas",
      "entity.trucoya.boleadoras": "Bolas"}
L = os.path.join(A, "lang")
for f in os.listdir(L):
    full = os.path.join(L, f)
    with open(full, encoding="utf-8") as fh:
        d = json.load(fh)
    d.update(EN if f == "en_us.json" else ES)
    wjson(d, full)
print("Armas gauchas generadas: facón, facón platero, rebenque y boleadoras.")
