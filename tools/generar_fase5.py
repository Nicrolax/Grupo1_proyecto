"""
Fase 5: recursos del mate, la picada, las bebidas, los discos y el árbol de logros.
  - texturas (pixel art 16x16 y el ícono del efecto Confraternidad 18x18)
  - modelos de ítem/bloque, blockstates, tablas de botín y recetas (+ desbloqueo en el libro)
  - sounds.json (discos), traducciones (es / en)
  - los 14 logros del árbol "Truco Ya"
Correr DESPUÉS de generar_recursos.py (agrega a sounds.json y a los idiomas sin borrar lo anterior).
Uso: python3 tools/generar_fase5.py
"""
import json, os, random
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "src", "main", "resources")
A = os.path.join(RES, "assets", "trucoya")
D = os.path.join(RES, "data", "trucoya")
R = random.Random(1810)


def wjson(obj, *p):
    full = os.path.join(*p)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)


def c(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def sh(col, k):
    return tuple(max(0, min(255, int(v * k))) for v in col[:3]) + (255,)


T = os.path.join(A, "textures")


def guardar(img, *p):
    full = os.path.join(T, *p)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)


def lienzo(n=16):
    return Image.new("RGBA", (n, n), (0, 0, 0, 0))


def pintar(mapa, paleta):
    """mapa: lista de 16 strings; cada carácter es una clave de la paleta ('.' transparente)."""
    img = lienzo(len(mapa))
    px = img.load()
    for y, fila in enumerate(mapa):
        for x, ch in enumerate(fila):
            if ch != "." and ch in paleta:
                px[x, y] = paleta[ch]
    return img


# ================================================================ texturas de ítems

MATE_BASE = [
    "................",
    "..........B.....",
    "..........B.....",
    ".........B......",
    ".....pppB.......",
    "...ppYYYYpp.....",
    "..pYYYYYYYYp....",
    "..pmmmmmmmmp....",
    ".pmmmmmmmmmmp...",
    ".pmmMmmmmmmmp...",
    ".pmMmmmmmmmmp...",
    ".pmmmmmmmmmmp...",
    "..pmmmmmmmmp....",
    "...pmmmmmmp.....",
    "....pppppp......",
    "................",
]
pal_mate = {"p": c("#4A2E14"), "m": c("#8C5A2B"), "M": c("#B07A44"), "B": c("#C8C8C0"),
            "Y": c("#4E2E16")}
guardar(pintar(MATE_BASE, pal_mate), "item", "mate.png")
pal_c = dict(pal_mate, Y=c("#6E9A3A"))
guardar(pintar(MATE_BASE, pal_c), "item", "mate_cargado.png")
mate_cebado = [r for r in MATE_BASE]
mate_cebado[5] = "...ppYWYYWpp...."
mate_cebado[6] = "..pYWYYYYWYp...."
pal_ce = dict(pal_mate, Y=c("#4E7A2A"), W=c("#B8D890"))
img = pintar(mate_cebado, pal_ce)
px = img.load()
for x, y in ((6, 2), (7, 1), (5, 3)):
    px[x, y] = (230, 230, 230, 160)                                            # vapor
guardar(img, "item", "mate_cebado.png")

guardar(pintar([
    "................", "...........gg...", "..........gGg...", ".........gGg....", "........gGg.....",
    ".......gGg......", "......gGg.......", ".....gGg........", "....gGg.........", "...sSs..........",
    "...sss..........", "..sSs...........", "..ss............", "................", "................",
    "................"], {"g": c("#A8A8A0"), "G": c("#E0E0D8"), "s": c("#9A8A50"), "S": c("#D9B54A")}),
    "item", "bombilla.png")

PAVA = [
    "................", "......kkkk......", ".....kKKKKk.....", "....k......k....", "...kkkkkkkkkk...",
    "..kSSSSSSSSSSk..", "..kSsSSSSSSSSkpp", "..kSSSSSSSSSSkp.", "..kSSSSSSSSSSk..", "..kSSSSSSSSSSk..",
    "..kSSSSSSSSSSk..", "...kSSSSSSSSk...", "....kkkkkkkk....", "................", "................",
    "................"]
pal_pava = {"k": c("#3A3A3E"), "K": c("#1E1E22"), "S": c("#9AA0A6"), "s": c("#D8DCE0"), "p": c("#6A6E72")}
guardar(pintar(PAVA, pal_pava), "item", "pava.png")
img = pintar(PAVA, pal_pava)
px = img.load()
for x, y in ((14, 4), (15, 3), (14, 2), (15, 1), (13, 3)):
    px[x, y] = (235, 235, 235, 170)
guardar(img, "item", "pava_caliente.png")


def hojas_img(seco=False):
    img = lienzo()
    px = img.load()
    verde = [c("#3E6E22"), c("#5A8C32"), c("#7AAA44")] if not seco else [c("#5A6E2A"), c("#7A8A3A"), c("#9AA850")]
    for cx, cy in ((5, 6), (10, 5), (7, 10), (11, 11)):
        for dx in range(-2, 3):
            for dy in range(-3, 4):
                if abs(dx) * 1.6 + abs(dy) <= 3.4:
                    px[cx + dx, cy + dy] = verde[(dx + dy) % 3]
        px[cx, cy] = c("#2A4A16")
    return img


guardar(hojas_img(), "item", "hojas_yerba.png")
# paquete de yerba
guardar(pintar([
    "................", "....wwwwwwww....", "...wWWWWWWWWw...", "...wWggggggWw...", "...wWgYYYYgWw...",
    "...wWgYYYYgWw...", "...wWggggggWw...", "...wWWWWWWWWw...", "...wWrrrrrrWw...", "...wWWWWWWWWw...",
    "...wWWWWWWWWw...", "...wWWWWWWWWw...", "...wWWWWWWWWw...", "...wwwwwwwwww...", "................",
    "................"], {"w": c("#B8A878"), "W": c("#E8DCB8"), "g": c("#2F6A2A"), "Y": c("#D9C04A"),
                         "r": c("#B3262C")}), "item", "yerba_mate.png")
guardar(pintar([
    "................", "................", "......s.........", "..........s.....", "....s...........",
    "........s.......", "..s.........s...", "......s.........", "...........s....", "....s....s......",
    "........s.......", "..s.............", ".......s....s...", "................", "................",
    "................"], {"s": c("#6A5A2A")}), "item", "semillas_yerba.png")
guardar(pintar([
    "................", "................", "...........rr...", "..........rRRr..", ".........rRwRr..",
    "........rRRRr...", ".......rRwRr....", "......rRRRr.....", ".....rRwRr......", "....rRRRr.......",
    "...rRwRr........", "..rRRRr.........", "..rrrr..........", "................", "................",
    "................"], {"r": c("#5A1E1A"), "R": c("#9B2D2D"), "w": c("#E8D8C8")}), "item", "salame.png")
guardar(pintar([
    "................", "................", "................", "...........yy...", ".........yyYYy..",
    ".......yyYYYYYy.", ".....yyYYoYYYYy.", "...yyYYYYYYoYYy.", "..yYYYoYYYYYYYy.", "..yYYYYYYYoYYyy.",
    "..yYYYYYYYYyy...", "..yyyyyyyyy.....", "................", "................", "................",
    "................"], {"y": c("#C8A040"), "Y": c("#F2D27A"), "o": c("#D9B050")}), "item", "queso.png")
guardar(pintar([
    "................", "................", "................", "....bbbbbbbb....", "...bBBBBBBBBb...",
    "..bBBdBBBBdBBb..", "..bBBBBBBBBBBb..", "..bBBBBdBBBBBb..", "..bBdBBBBBBdBb..", "..bBBBBBBBBBBb..",
    "...bBBBBdBBBb...", "....bbbbbbbb....", "................", "................", "................",
    "................"], {"b": c("#9A6A2A"), "B": c("#D9A85A"), "d": c("#B07A3A")}), "item", "galleta.png")


def botella(liquido, etiqueta, nombre):
    img = pintar([
        "................", "......kk........", "......cc........", "......gg........", "......gg........",
        ".....gGGg.......", "....gGLLGg......", "....gLLLLg......", "....gEEEEg......", "....gEEEEg......",
        "....gLLLLg......", "....gLLLLg......", "....gLLLLg......", "....gggggg......", "................",
        "................"], {"k": c("#5A3A1A"), "c": c("#7A5A2A"), "g": c("#2E3A2A"), "G": c("#5A6A5A"),
                             "L": liquido, "E": etiqueta})
    guardar(img, "item", nombre + ".png")


botella(c("#5A0E1A"), c("#E8DCC0"), "vino_tinto")
botella(c("#D8E0E8"), c("#2F4A6B"), "cana")
botella(c("#D99A2A"), c("#F2D27A"), "grapamiel")
# tabla de picada (ícono)
guardar(pintar([
    "................", "................", "................", "................", "................",
    "......rr.yy.....", "....rRRryYYy.bb.", "...rRwRryyy.bBBb", "..ttttttttttttt.", ".tTTTTTTTTTTTTTt",
    ".tTTTTTTTTTTTTTt", "..ttttttttttttt.", "................", "................", "................",
    "................"], {"r": c("#5A1E1A"), "R": c("#9B2D2D"), "w": c("#E8D8C8"), "y": c("#C8A040"),
                         "Y": c("#F2D27A"), "b": c("#9A6A2A"), "B": c("#D9A85A"), "t": c("#6A4A2A"),
                         "T": c("#A8784A")}), "item", "tabla_picada.png")


def disco(centro, nombre):
    img = lienzo()
    px = img.load()
    for x in range(16):
        for y in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d <= 7.2:
                px[x, y] = c("#1A1A1C") if int(d * 2) % 3 else c("#2A2A2E")
                if d <= 3.2:
                    px[x, y] = centro
                if d <= 0.9:
                    px[x, y] = c("#0A0A0A")
    px[5, 4] = c("#5A5A60")
    px[4, 5] = c("#5A5A60")
    guardar(img, "item", nombre + ".png")


disco(c("#6CACE4"), "disco_milonga")          # celeste
disco(c("#B3262C"), "disco_candombe")         # rojo tambor
disco(c("#D9B54A"), "disco_vals")             # dorado
disco(c("#3E6E22"), "disco_payada")           # verde campo

# ================================================================ texturas de bloques

for etapa in range(4):
    img = lienzo()
    px = img.load()
    alto = 4 + etapa * 3
    for tallo in (3, 7, 11):
        base_x = tallo + R.choice([-1, 0, 1])
        for y in range(15, 15 - alto, -1):
            px[base_x, y] = c("#4A3A1E") if y > 15 - alto // 3 else c("#3E6E22")
        for k in range(etapa + 1):
            y = 15 - alto + 1 + k * 3
            for dx in (-2, -1, 1, 2):
                if 0 <= base_x + dx < 16 and 0 <= y < 16:
                    px[base_x + dx, y] = (c("#5A8C32") if abs(dx) == 1 else c("#7AAA44"))
            if etapa == 3 and 0 <= y - 1:
                px[base_x, max(0, y - 1)] = c("#7AAA44")
    guardar(img, "block", f"yerbal_stage{etapa}.png")


def bloque_liso(col, ruido, nombre, vetas=False):
    img = Image.new("RGBA", (16, 16), col)
    px = img.load()
    for x in range(16):
        for y in range(16):
            k = 1 + R.uniform(-ruido, ruido)
            if vetas and y % 4 == 0:
                k *= 0.82
            px[x, y] = sh(col, k)
    guardar(img, "block", nombre + ".png")


bloque_liso(c("#A8784A"), 0.06, "picada_tabla", vetas=True)
img = Image.new("RGBA", (16, 16), c("#9B2D2D"))
px = img.load()
for x in range(16):
    for y in range(16):
        px[x, y] = c("#E8D8C8") if R.random() < 0.14 else sh(c("#9B2D2D"), 1 + R.uniform(-0.08, 0.08))
guardar(img, "block", "picada_salame.png")
bloque_liso(c("#F2D27A"), 0.05, "picada_queso")
bloque_liso(c("#D9A85A"), 0.07, "picada_galleta")

# ícono del efecto Confraternidad (18x18): un mate con dos bombillas cruzadas
ico = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
mate_ico = pintar(MATE_BASE, pal_ce)
ico.alpha_composite(mate_ico, (1, 1))
px = ico.load()
for i in range(6):
    px[4 + i, 2 + i] = c("#E0E0D8")
guardar(ico, "mob_effect", "confraternidad.png")

# ================================================================ modelos y blockstates

MI = os.path.join(A, "models", "item")
MB = os.path.join(A, "models", "block")
BS = os.path.join(A, "blockstates")
for it in ("hojas_yerba", "yerba_mate", "bombilla", "semillas_yerba", "salame", "queso", "galleta",
           "vino_tinto", "cana", "grapamiel", "disco_milonga", "disco_candombe", "disco_vals", "disco_payada",
           "mate_cargado", "mate_cebado", "pava_caliente"):
    wjson({"parent": "item/generated", "textures": {"layer0": f"trucoya:item/{it}"}}, MI, f"{it}.json")
wjson({"parent": "item/handheld", "textures": {"layer0": "trucoya:item/mate"},
       "overrides": [{"predicate": {"trucoya:estado": 1}, "model": "trucoya:item/mate_cargado"},
                     {"predicate": {"trucoya:estado": 2}, "model": "trucoya:item/mate_cebado"}]}, MI, "mate.json")
wjson({"parent": "item/generated", "textures": {"layer0": "trucoya:item/pava"},
       "overrides": [{"predicate": {"trucoya:estado": 2}, "model": "trucoya:item/pava_caliente"}]}, MI, "pava.json")
wjson({"parent": "item/generated", "textures": {"layer0": "trucoya:item/tabla_picada"}}, MI, "tabla_picada.json")

for etapa in range(4):
    wjson({"parent": "block/crop", "textures": {"crop": f"trucoya:block/yerbal_stage{etapa}"}}, MB, f"yerbal_stage{etapa}.json")
wjson({"variants": {f"age={a}": {"model": f"trucoya:block/yerbal_stage{a // 2}"} for a in range(8)}}, BS, "yerbal.json")


def cubo(desde, hasta, tex, uv=None):
    caras = {}
    for cara in ("north", "south", "east", "west", "up", "down"):
        caras[cara] = {"texture": "#" + tex}
    return {"from": desde, "to": hasta, "faces": caras}


for comidas in range(4):
    el = [cubo([1, 0, 2], [15, 1.5, 14], "tabla"), cubo([13, 0.5, 6.5], [16, 1, 9.5], "tabla")]
    porciones = [
        [cubo([2, 1.5, 3], [5, 3.5, 6], "salame"), cubo([5.5, 1.5, 3.5], [8, 3, 6], "salame")],
        [cubo([9, 1.5, 3], [12, 3.5, 6], "queso"), cubo([10, 3.5, 4], [11, 4.5, 5], "queso")],
        [cubo([2, 1.5, 8], [6, 2.5, 12], "galleta"), cubo([3, 2.5, 9], [7, 3.5, 13], "galleta")],
        [cubo([8, 1.5, 8], [11, 3.5, 11], "queso"), cubo([11.5, 1.5, 9], [13.5, 3, 12], "salame")],
    ]
    for p in porciones[comidas:]:
        el.extend(p)
    wjson({"parent": "block/block", "textures": {"particle": "trucoya:block/picada_tabla",
                                                 "tabla": "trucoya:block/picada_tabla",
                                                 "salame": "trucoya:block/picada_salame",
                                                 "queso": "trucoya:block/picada_queso",
                                                 "galleta": "trucoya:block/picada_galleta"},
           "elements": el}, MB, f"tabla_picada_{comidas}.json")
wjson({"variants": {f"comidas={k}": {"model": f"trucoya:block/tabla_picada_{k}"} for k in range(4)}},
      BS, "tabla_picada.json")

# ================================================================ botín, recetas y desbloqueos

LT = os.path.join(D, "loot_tables", "blocks")
madura = {"condition": "minecraft:block_state_property", "block": "trucoya:yerbal", "properties": {"age": "7"}}
wjson({"type": "minecraft:block", "pools": [
    {"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "name": "trucoya:hojas_yerba", "conditions": [madura],
         "functions": [{"function": "minecraft:set_count", "count": {"min": 2, "max": 4}}]},
        {"type": "minecraft:item", "name": "trucoya:semillas_yerba"}]}]},
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "trucoya:semillas_yerba",
                              "functions": [{"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune",
                                             "formula": "minecraft:binomial_with_bonus_count",
                                             "parameters": {"extra": 2, "probability": 0.5}}]}],
     "conditions": [madura]}],
    "functions": [{"function": "minecraft:explosion_decay"}]}, LT, "yerbal.json")
wjson({"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "trucoya:tabla_picada"}],
                                             "conditions": [{"condition": "minecraft:block_state_property",
                                                             "block": "trucoya:tabla_picada",
                                                             "properties": {"comidas": "0"}}]}]},
      LT, "tabla_picada.json")

RC = os.path.join(D, "recipes")
def item(i):
    return {"item": i if ":" in i else "minecraft:" + i}

recetas = {
    "bombilla": {"type": "minecraft:crafting_shaped", "pattern": ["N", "N", "G"],
                 "key": {"N": item("iron_nugget"), "G": item("gold_nugget")}, "result": {"item": "trucoya:bombilla"}},
    "mate": {"type": "minecraft:crafting_shapeless", "ingredients": [item("pumpkin"), item("trucoya:bombilla")],
             "result": {"item": "trucoya:mate"}},
    "pava": {"type": "minecraft:crafting_shaped", "pattern": [" N ", "I I", "III"],
             "key": {"N": item("iron_nugget"), "I": item("iron_ingot")}, "result": {"item": "trucoya:pava"}},
    "salame": {"type": "minecraft:crafting_shapeless", "ingredients": [item("porkchop"), item("beef")],
               "result": {"item": "trucoya:salame", "count": 2}},
    "queso": {"type": "minecraft:crafting_shapeless", "ingredients": [item("milk_bucket")],
              "result": {"item": "trucoya:queso", "count": 3}},
    "galleta": {"type": "minecraft:crafting_shapeless", "ingredients": [item("wheat"), item("wheat"), item("egg")],
                "result": {"item": "trucoya:galleta", "count": 3}},
    "tabla_picada": {"type": "minecraft:crafting_shapeless",
                     "ingredients": [item("trucoya:salame"), item("trucoya:queso"), item("trucoya:galleta"),
                                     {"tag": "minecraft:wooden_slabs"}], "result": {"item": "trucoya:tabla_picada"}},
    "vino_tinto": {"type": "minecraft:crafting_shapeless",
                   "ingredients": [item("glass_bottle"), item("sweet_berries"), item("sweet_berries"), item("sweet_berries")],
                   "result": {"item": "trucoya:vino_tinto"}},
    "cana": {"type": "minecraft:crafting_shapeless",
             "ingredients": [item("glass_bottle"), item("sugar_cane"), item("sugar_cane"), item("sugar_cane")],
             "result": {"item": "trucoya:cana"}},
    "grapamiel": {"type": "minecraft:crafting_shapeless", "ingredients": [item("trucoya:cana"), item("honey_bottle")],
                  "result": {"item": "trucoya:grapamiel"}},
    "yerba_mate_horno": {"type": "minecraft:smelting", "ingredient": item("trucoya:hojas_yerba"),
                         "result": "trucoya:yerba_mate", "experience": 0.2, "cookingtime": 200},
    "yerba_mate_ahumador": {"type": "minecraft:smoking", "ingredient": item("trucoya:hojas_yerba"),
                            "result": "trucoya:yerba_mate", "experience": 0.2, "cookingtime": 100},
    "yerba_mate_fogata": {"type": "minecraft:campfire_cooking", "ingredient": item("trucoya:hojas_yerba"),
                          "result": "trucoya:yerba_mate", "experience": 0.2, "cookingtime": 400},
}
desbloqueo = {"bombilla": "minecraft:iron_nugget", "mate": "minecraft:pumpkin", "pava": "minecraft:iron_ingot",
              "salame": "minecraft:porkchop", "queso": "minecraft:milk_bucket", "galleta": "minecraft:wheat",
              "tabla_picada": "trucoya:salame", "vino_tinto": "minecraft:sweet_berries", "cana": "minecraft:sugar_cane",
              "grapamiel": "trucoya:cana", "yerba_mate_horno": "trucoya:hojas_yerba",
              "yerba_mate_ahumador": "trucoya:hojas_yerba", "yerba_mate_fogata": "trucoya:hojas_yerba"}
for nombre, rec in recetas.items():
    wjson(rec, RC, f"{nombre}.json")
    wjson({"parent": "minecraft:recipes/root", "rewards": {"recipes": [f"trucoya:{nombre}"]},
           "criteria": {"tiene_material": {"trigger": "minecraft:inventory_changed",
                                           "conditions": {"items": [{"item": desbloqueo[nombre]}]}},
                        "tiene_receta": {"trigger": "minecraft:recipe_unlocked",
                                         "conditions": {"recipe": f"trucoya:{nombre}"}}},
           "requirements": [["tiene_material", "tiene_receta"]]},
          D, "advancements", "recipes", f"{nombre}.json")

# ================================================================ sonidos de los discos

sp = os.path.join(A, "sounds.json")
with open(sp, encoding="utf-8") as f:
    sonidos = json.load(f)
for d in ("milonga", "candombe", "vals", "payada"):
    sonidos[f"disco.{d}"] = {"category": "record", "sounds": [{"name": f"trucoya:disco/{d}", "stream": True}]}
wjson(sonidos, sp)

# ================================================================ árbol de logros

LOGROS = [
    # ruta, padre, ícono, marco, xp, título es, descripción es, título en, descripción en
    ("root", None, "trucoya:mesa_truco", "task", 0, "Truco Ya", "El Truco Uruguayo llegó al boliche",
     "Truco Ya", "Uruguayan Truco comes to the tavern"),
    ("iniciacion/toma_asiento", "root", "trucoya:silla_criolla", "task", 5, "Tomá Asiento",
     "Sentate en una Silla Criolla junto a la Mesa de Truco", "Take a Seat", "Sit on a Criollo Chair by a Truco Table"),
    ("iniciacion/cartas_sobre_la_mesa", "iniciacion/toma_asiento", "trucoya:anotador", "task", 10,
     "Cartas sobre la Mesa", "Ganá tu primer partido de truco", "Cards on the Table", "Win your first truco match"),
    ("iniciacion/el_ceba", "root", "trucoya:mate", "task", 10, "El que Ceba",
     "Cebale un mate a otro jugador o a un gaucho", "The One Who Serves", "Serve a mate to another player or a gaucho"),
    ("iniciacion/picada_completa", "iniciacion/el_ceba", "trucoya:tabla_picada", "task", 10, "Picada Completa",
     "Terminá una tabla de picada", "Full Platter", "Finish a picada platter"),
    ("tigre/flor_de_viaje", "iniciacion/cartas_sobre_la_mesa", "minecraft:poppy", "task", 15, "Flor de Viaje",
     "Cantá flor en una partida", "A Flower on the Road", "Call flor in a match"),
    ("tigre/puro_cuento", "iniciacion/cartas_sobre_la_mesa", "minecraft:fox_spawn_egg", "task", 20, "Puro Cuento",
     "Ganá un truco con mano floja: el rival no quiso y era farol", "Pure Bluff",
     "Win a truco with a weak hand: they folded on your bluff"),
    ("tigre/contraflor_al_resto", "tigre/flor_de_viaje", "minecraft:rose_bush", "goal", 30, "Contraflor al Resto",
     "Ganá una contraflor al resto", "Counter-Flower for the Rest", "Win a contraflor al resto"),
    ("tigre/creciendo_en_el_boliche", "iniciacion/cartas_sobre_la_mesa", "minecraft:emerald", "goal", 30,
     "Creciendo en el Boliche", "Llegá al nivel Tigre de Boliche del Prestigio Criollo", "Rising in the Tavern",
     "Reach the Tavern Tiger rank of Criollo Prestige"),
    ("tigre/el_desafio_del_forastero", "tigre/creciendo_en_el_boliche", "minecraft:saddle", "goal", 40,
     "El Desafío del Forastero", "Ganale al forastero que llega a caballo", "The Stranger's Challenge",
     "Beat the stranger who rides in on horseback"),
    ("leyenda/noche_clandestina", "tigre/creciendo_en_el_boliche", "trucoya:ficha_clandestina", "goal", 40,
     "Noche Clandestina", "Ganá un partido en una mesa de la Guarida Clandestina", "Clandestine Night",
     "Win a match at a table in the Clandestine Den"),
    ("leyenda/pescuezo_dorado", "leyenda/noche_clandestina", "minecraft:gold_block", "challenge", 60,
     "Pescuezo Dorado", "Llevate un pozo con oro en la Guarida Clandestina", "Golden Neck",
     "Take a pot with gold in the Clandestine Den"),
    ("leyenda/remontada_historica", "tigre/el_desafio_del_forastero", "minecraft:totem_of_undying", "challenge", 60,
     "Remontada Histórica", "Ganá un partido que ibas perdiendo por 10 puntos o más", "Historic Comeback",
     "Win a match after trailing by 10 points or more"),
    ("leyenda/derrotando_a_la_leyenda", "leyenda/remontada_historica", "trucoya:baraja_de_oro", "challenge", 100,
     "Derrotando a la Leyenda", "Ganale al Viejo Vizcacha en la Estancia del Caudillo", "Defeating the Legend",
     "Beat Old Vizcacha at the Caudillo's Ranch"),
    ("leyenda/el_invicto_del_uruguay", "leyenda/derrotando_a_la_leyenda", "minecraft:nether_star", "challenge", 150,
     "El Invicto del Uruguay", "Ganá 10 partidos seguidos o llegá a Leyenda del Tapete", "Uruguay's Unbeaten",
     "Win 10 matches in a row or reach Legend of the Felt"),
]
assert len(LOGROS) == 15
es_extra, en_extra = {}, {}
for ruta, padre, icono, marco, xp, t_es, d_es, t_en, d_en in LOGROS:
    clave = "advancements.trucoya." + ruta.replace("/", ".")
    adv = {"display": {"icon": {"item": icono}, "title": {"translate": clave + ".title"},
                       "description": {"translate": clave + ".description"}, "frame": marco,
                       "show_toast": ruta != "root", "announce_to_chat": ruta != "root", "hidden": False}}
    if padre is None:
        adv["display"]["background"] = "minecraft:textures/block/dark_oak_planks.png"
        adv["criteria"] = {"entrar": {"trigger": "minecraft:tick"}}
    else:
        adv["parent"] = "trucoya:" + padre
        adv["criteria"] = {"hazana": {"trigger": "minecraft:impossible"}}
        if xp:
            adv["rewards"] = {"experience": xp}
    wjson(adv, D, "advancements", *(ruta + ".json").split("/"))
    es_extra[clave + ".title"], es_extra[clave + ".description"] = t_es, d_es
    en_extra[clave + ".title"], en_extra[clave + ".description"] = t_en, d_en

# ================================================================ traducciones

ES = {
    "block.trucoya.yerbal": "Yerbal", "block.trucoya.tabla_picada": "Tabla de Picada",
    "item.trucoya.semillas_yerba": "Semillas de Yerba Mate", "item.trucoya.hojas_yerba": "Hojas de Yerba",
    "item.trucoya.yerba_mate": "Yerba Mate", "item.trucoya.bombilla": "Bombilla",
    "item.trucoya.mate": "Mate", "item.trucoya.mate.cargado": "Mate cargado", "item.trucoya.mate.cebado": "Mate cebado",
    "item.trucoya.pava": "Pava", "item.trucoya.salame": "Salame", "item.trucoya.queso": "Queso de Campo",
    "item.trucoya.galleta": "Galleta de Campaña", "item.trucoya.tabla_picada": "Tabla de Picada",
    "item.trucoya.vino_tinto": "Vino Tinto de la Casa", "item.trucoya.cana": "Caña", "item.trucoya.grapamiel": "Grapamiel",
    "item.trucoya.disco_milonga": "Disco", "item.trucoya.disco_milonga.desc": "Los Porotos - Milonga del Truco",
    "item.trucoya.disco_candombe": "Disco", "item.trucoya.disco_candombe.desc": "Comparsa La Esquina - Candombe de Llamadas",
    "item.trucoya.disco_vals": "Disco", "item.trucoya.disco_vals.desc": "Bandoneón del Boliche - Vals de la Pulpería",
    "item.trucoya.disco_payada": "Disco", "item.trucoya.disco_payada.desc": "El Payador - Payada del Forastero",
    "effect.trucoya.confraternidad": "Confraternidad",
    "subtitles.trucoya.disco": "Suena un disco",
}
EN = {
    "block.trucoya.yerbal": "Yerba Mate Bush", "block.trucoya.tabla_picada": "Picada Platter",
    "item.trucoya.semillas_yerba": "Yerba Mate Seeds", "item.trucoya.hojas_yerba": "Yerba Leaves",
    "item.trucoya.yerba_mate": "Yerba Mate", "item.trucoya.bombilla": "Bombilla (Straw)",
    "item.trucoya.mate": "Mate Gourd", "item.trucoya.mate.cargado": "Loaded Mate", "item.trucoya.mate.cebado": "Served Mate",
    "item.trucoya.pava": "Kettle", "item.trucoya.salame": "Salami", "item.trucoya.queso": "Farm Cheese",
    "item.trucoya.galleta": "Country Cracker", "item.trucoya.tabla_picada": "Picada Platter",
    "item.trucoya.vino_tinto": "House Red Wine", "item.trucoya.cana": "Caña (Cane Spirit)", "item.trucoya.grapamiel": "Grapamiel",
    "item.trucoya.disco_milonga": "Music Disc", "item.trucoya.disco_milonga.desc": "Los Porotos - Milonga del Truco",
    "item.trucoya.disco_candombe": "Music Disc", "item.trucoya.disco_candombe.desc": "Comparsa La Esquina - Candombe de Llamadas",
    "item.trucoya.disco_vals": "Music Disc", "item.trucoya.disco_vals.desc": "Bandoneón del Boliche - Vals de la Pulpería",
    "item.trucoya.disco_payada": "Music Disc", "item.trucoya.disco_payada.desc": "El Payador - Payada del Forastero",
    "effect.trucoya.confraternidad": "Brotherhood (Confraternidad)",
    "subtitles.trucoya.disco": "Record plays",
}
ES.update(es_extra)
EN.update(en_extra)
L = os.path.join(A, "lang")
for f in os.listdir(L):
    full = os.path.join(L, f)
    with open(full, encoding="utf-8") as fh:
        d = json.load(fh)
    d.update(EN if f == "en_us.json" else ES)
    wjson(d, full)
print("Fase 5: texturas, modelos, recetas, botín, sonidos, traducciones y 15 logros generados.")
