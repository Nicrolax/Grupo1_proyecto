"""
El Camino del Truco: recursos de la historia.
  - texturas (16x16): Libreta del Truquero, Copa del Torneo, Banderín de cuadrera
  - modelos, blockstate del banderín (mira hacia donde corre la carrera), botín, recetas y desbloqueos
  - los 6 logros de la rama "historia" y las traducciones
Correr DESPUÉS de generar_fase5.py (agrega a los idiomas sin borrar lo anterior).
Uso: python3 tools/generar_historia.py
"""
import json, os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "src", "main", "resources")
A = os.path.join(RES, "assets", "trucoya")
D = os.path.join(RES, "data", "trucoya")
T = os.path.join(A, "textures")


def wjson(obj, *p):
    full = os.path.join(*p)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)


def c(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def pintar(mapa, paleta, *ruta):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, fila in enumerate(mapa):
        for x, ch in enumerate(fila):
            if ch in paleta:
                px[x, y] = paleta[ch]
    full = os.path.join(T, *ruta)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)


# ---------------------------------------------------------------- texturas
pintar([
    "................",
    "..cccccccccccc..",
    ".cCCCCCCCCCCCCo.",
    ".cCLLLLLLLLLLCo.",
    ".cCLppppppppLCo.",
    ".cCLpggggggpLCo.",
    ".cCLppppppppLCo.",
    ".cCLLLLLLLLLLCo.",
    ".cCCCCCCCCCCCCo.",
    ".cCCCCssCCCCCCo.",
    ".cCCCCssCCCCCCo.",
    ".cCCCCCCCCCCCCo.",
    ".cCCCCCCCCCCCCo.",
    ".cCCCCCCCCCCCCo.",
    "..cccccccccccco.",
    "...oooooooooooo.",
], {"c": c("#3A1E12"), "C": c("#7A3E22"), "L": c("#5A2A16"), "p": c("#F0E2BE"), "g": c("#B8860B"),
    "s": c("#D9B54A"), "o": c("#2A140C")}, "item", "libreta_truquero.png")
pintar([
    "................",
    "..o..........o..",
    ".oYo.oooooo.oYo.",
    ".oY.oYYYYYYo.Yo.",
    ".oY.oYWYYYYo.Yo.",
    "..oYoYWYYYYoYo..",
    "...ooYYYYYYoo...",
    ".....oYYYYo.....",
    "......oYYo......",
    ".......YY.......",
    "......oYYo......",
    ".....oyyyyo.....",
    "....oYYYYYYo....",
    "....oDDDDDDo....",
    "....oooooooo....",
    "................",
], {"o": c("#6A4A0A"), "Y": c("#E8B82A"), "y": c("#C8961E"), "W": c("#FFF3B0"), "D": c("#5A3A22")},
    "item", "copa_torneo.png")
pintar([
    "................",
    "..PRRRRRRR......",
    "..PRRWWRRRR.....",
    "..PRWWWWRRRRR...",
    "..PRRWWRRRRRRRR.",
    "..PRRRRRRRRRR...",
    "..PRRRRRRR......",
    "..P.............",
    "..P.............",
    "..P.............",
    "..P.............",
    "..P.............",
    "..P.............",
    "..P.............",
    ".PPP............",
    ".ppp............",
], {"P": c("#7A5230"), "p": c("#4A3020"), "R": c("#B3262C"), "W": c("#F4F1EA")}, "item", "raya_cuadrera.png")
# textura del bloque: palo y paño (la bandera se dibuja como un plano)
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
px = img.load()
for y in range(16):
    for x in range(0, 2):
        px[x, y] = c("#7A5230") if (y + x) % 5 else c("#6A4428")
for y in range(0, 8):
    for x in range(4, 16):
        rojo = c("#B3262C") if (x + y) % 7 else c("#9A1E24")
        px[x, y] = c("#F4F1EA") if (4 <= x - 4 <= 7 and 2 <= y <= 5 and abs((x - 4) - 5.5) + abs(y - 3.5) < 3) else rojo
os.makedirs(os.path.join(T, "block"), exist_ok=True)
img.save(os.path.join(T, "block", "raya_cuadrera.png"))

# ---------------------------------------------------------------- modelos y blockstate
M = os.path.join(A, "models")
for it in ("libreta_truquero", "copa_torneo", "raya_cuadrera"):
    wjson({"parent": "minecraft:item/generated", "textures": {"layer0": f"trucoya:item/{it}"}}, M, "item", f"{it}.json")
wjson({
    "parent": "minecraft:block/block",
    "textures": {"tex": "trucoya:block/raya_cuadrera", "particle": "trucoya:block/raya_cuadrera"},
    "elements": [
        {"from": [7, 0, 7], "to": [9, 16, 9], "faces": {
            f: {"uv": [0, 0, 2, 16], "texture": "#tex"} for f in ("north", "south", "east", "west")} | {
            "up": {"uv": [0, 0, 2, 2], "texture": "#tex"}, "down": {"uv": [0, 0, 2, 2], "texture": "#tex"}}},
        {"from": [8, 8, 9], "to": [8.01, 16, 21], "shade": False, "faces": {
            "east": {"uv": [4, 0, 16, 8], "texture": "#tex"}, "west": {"uv": [16, 0, 4, 8], "texture": "#tex"}}},
    ]}, M, "block", "raya_cuadrera.json")
rot = {"north": 0, "east": 90, "south": 180, "west": 270}
wjson({"variants": {f"facing={k}": {"model": "trucoya:block/raya_cuadrera", "y": v} for k, v in rot.items()}},
      A, "blockstates", "raya_cuadrera.json")
wjson({"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item",
      "name": "trucoya:raya_cuadrera"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]},
      D, "loot_tables", "blocks", "raya_cuadrera.json")


# ---------------------------------------------------------------- recetas
def item(i):
    return {"item": i if ":" in i else "minecraft:" + i}


recetas = {
    "libreta_truquero": ({"type": "minecraft:crafting_shapeless",
                          "ingredients": [item("book"), item("leather"), item("ink_sac")],
                          "result": {"item": "trucoya:libreta_truquero"}}, "minecraft:book"),
    "raya_cuadrera": ({"type": "minecraft:crafting_shaped", "pattern": ["SW", "SR", "S "],
                       "key": {"S": item("stick"), "W": item("white_wool"), "R": item("red_dye")},
                       "result": {"item": "trucoya:raya_cuadrera"}}, "minecraft:white_wool"),
}
for nombre, (rec, desbloqueo) in recetas.items():
    wjson(rec, D, "recipes", f"{nombre}.json")
    wjson({"parent": "minecraft:recipes/root", "rewards": {"recipes": [f"trucoya:{nombre}"]},
           "criteria": {"tiene_material": {"trigger": "minecraft:inventory_changed",
                                           "conditions": {"items": [{"item": desbloqueo}]}},
                        "tiene_receta": {"trigger": "minecraft:recipe_unlocked", "conditions": {"recipe": f"trucoya:{nombre}"}}},
           "requirements": [["tiene_material", "tiene_receta"]]}, D, "advancements", "recipes", f"{nombre}.json")

# ---------------------------------------------------------------- logros de la historia
LOGROS = [
    ("historia/primer_sello", "root", "trucoya:libreta_truquero", "task", 15, "Primer Sello",
     "Ganale al campeón de una pulpería y llevate su sello", "First Seal", "Beat a tavern champion and earn their seal"),
    ("historia/la_ruta_completa", "historia/primer_sello", "minecraft:filled_map", "goal", 60, "La Ruta Completa",
     "Juntá los seis sellos de la Ruta de las Pulperías", "The Full Route", "Collect all six seals of the Tavern Route"),
    ("historia/el_mapa_del_baqueano", "historia/la_ruta_completa", "minecraft:compass", "goal", 40, "El Mapa del Baqueano",
     "Recibí el mapa que lleva a la Estancia del Caudillo", "The Scout's Map", "Receive the map to the Caudillo's Ranch"),
    ("historia/campeon_del_torneo", "historia/primer_sello", "trucoya:copa_torneo", "challenge", 50, "Campeón del Torneo",
     "Ganá los tres partidos de un torneo de pulpería", "Tournament Champion", "Win all three matches of a tavern tournament"),
    ("historia/pingo_ganador", "historia/primer_sello", "trucoya:raya_cuadrera", "goal", 25, "Pingo Ganador",
     "Ganá una carrera cuadrera", "Winning Steed", "Win a cuadrera horse race"),
    ("historia/el_ultimo_truco", "historia/el_mapa_del_baqueano", "trucoya:baraja_de_oro", "challenge", 150, "El Último Truco",
     "Ganale al Viejo Vizcacha y conocé a los Fundadores", "The Last Truco", "Beat Old Vizcacha and meet the Founders"),
]
ES = {"item.trucoya.libreta_truquero": "Libreta del Truquero", "item.trucoya.copa_torneo": "Copa del Torneo",
      "block.trucoya.raya_cuadrera": "Banderín de Cuadrera", "item.trucoya.raya_cuadrera": "Banderín de Cuadrera",
      "entity.trucoya.gaucho.fundador": "Fundador"}
EN = {"item.trucoya.libreta_truquero": "Truco Player's Notebook", "item.trucoya.copa_torneo": "Tournament Cup",
      "block.trucoya.raya_cuadrera": "Race Start Flag", "item.trucoya.raya_cuadrera": "Race Start Flag",
      "entity.trucoya.gaucho.fundador": "Founder"}
for ruta, padre, icono, marco, xp, t_es, d_es, t_en, d_en in LOGROS:
    clave = "advancements.trucoya." + ruta.replace("/", ".")
    wjson({"display": {"icon": {"item": icono}, "title": {"translate": clave + ".title"},
                       "description": {"translate": clave + ".description"}, "frame": marco,
                       "show_toast": True, "announce_to_chat": True, "hidden": False},
           "parent": "trucoya:" + padre, "criteria": {"hazana": {"trigger": "minecraft:impossible"}},
           "rewards": {"experience": xp}}, D, "advancements", *(ruta + ".json").split("/"))
    ES[clave + ".title"], ES[clave + ".description"] = t_es, d_es
    EN[clave + ".title"], EN[clave + ".description"] = t_en, d_en
L = os.path.join(A, "lang")
for f in os.listdir(L):
    full = os.path.join(L, f)
    with open(full, encoding="utf-8") as fh:
        d = json.load(fh)
    d.update(EN if f == "en_us.json" else ES)
    wjson(d, full)
print("Historia: texturas, modelos, recetas y", len(LOGROS), "logros generados.")
