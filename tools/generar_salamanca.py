"""
La Salamanca (capítulo 7): recursos.
  - texturas (16x16): Farol de las Ánimas y Facón de Ledesma, con sus modelos
  - los dos logros nuevos de la rama "historia" y las traducciones
Correr DESPUÉS de generar_historia.py (agrega a los idiomas sin borrar lo anterior).
Uso: python3 tools/generar_salamanca.py
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


# Farol de las Ánimas: farol de campo de hierro viejo, con la llama verdosa de las luces malas
pintar([
    "......aaaa......",
    ".....a....a.....",
    "......aaaa......",
    ".....kkkkkk.....",
    "....kHHHHHHk....",
    "....kVwggwVk....",
    "....kVgLLgVk....",
    "....kVgLWgVk....",
    "....kVgLLgVk....",
    "....kVwggwVk....",
    "....kVVggVVk....",
    "....kHHHHHHk....",
    ".....kkkkkk.....",
    "......kHHk......",
    "................",
    "................",
], {"a": c("#5A5450"), "k": c("#2A2624"), "H": c("#6E6660"), "V": c("#3E4A46"), "w": c("#9FD8C8"),
    "g": c("#3FA88C"), "L": c("#7FF0C8"), "W": c("#E8FFF6")}, "item", "farol_animas.png")

# Facón de Ledesma: hoja oscura y larga, guarda de plata en "S" y cabo de plata con una piedra roja
pintar([
    "................",
    "..............l.",
    ".............lB.",
    "............lBd.",
    "...........lBd..",
    "..........lBd...",
    ".........lBd....",
    "........lBd.....",
    ".....S.lBd......",
    "......SSd.......",
    ".....SrSS.......",
    "....GgG..S......",
    "...GgG..........",
    "..GgG...........",
    ".Gg.............",
    ".S..............",
], {"l": c("#D8DCE2"), "B": c("#8C949E"), "d": c("#3E444C"), "S": c("#E6E8EC"), "r": c("#C0202A"),
    "G": c("#B8BCC4"), "g": c("#6A5040")}, "item", "facon_ledesma.png")

M = os.path.join(A, "models")
wjson({"parent": "minecraft:item/generated", "textures": {"layer0": "trucoya:item/farol_animas"}},
      M, "item", "farol_animas.json")
wjson({"parent": "minecraft:item/handheld", "textures": {"layer0": "trucoya:item/facon_ledesma"}},
      M, "item", "facon_ledesma.json")

LOGROS = [
    ("historia/la_salamanca", "historia/el_ultimo_truco", "trucoya:farol_animas", "goal", 60, "La Salamanca",
     "Seguí al Viejo Vizcacha hasta el Paso de las Ánimas", "The Salamanca",
     "Follow Old Vizcacha to the Pass of Souls"),
    ("historia/alma_libre", "historia/la_salamanca", "trucoya:facon_ledesma", "challenge", 200, "Alma Libre",
     "Ganale al Mandinga y liberá el alma del Negro Ledesma", "Free Soul",
     "Beat the Mandinga and free Negro Ledesma's soul"),
]
ES = {"item.trucoya.farol_animas": "Farol de las Ánimas", "item.trucoya.facon_ledesma": "Facón de Ledesma",
      "entity.trucoya.gaucho.mandinga": "Mandinga", "entity.trucoya.gaucho.ledesma": "Negro Ledesma",
      "item.trucoya.farol_animas.pista": "De noche, sus luces malas marcan el camino al Paso de las Ánimas.",
      "item.trucoya.facon_ledesma.pista": "El facón que el Negro Ledesma le ganó al Vizcacha la noche antes de la batalla."}
EN = {"item.trucoya.farol_animas": "Lantern of Souls", "item.trucoya.facon_ledesma": "Ledesma's Facón",
      "entity.trucoya.gaucho.mandinga": "Mandinga", "entity.trucoya.gaucho.ledesma": "Negro Ledesma",
      "item.trucoya.farol_animas.pista": "At night, its will-o'-the-wisps point the way to the Pass of Souls.",
      "item.trucoya.facon_ledesma.pista": "The knife Negro Ledesma won from Vizcacha the night before the battle."}
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
print("Salamanca: texturas, modelos y", len(LOGROS), "logros generados.")
