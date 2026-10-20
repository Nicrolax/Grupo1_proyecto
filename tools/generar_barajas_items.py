"""
Íconos (16 x 16), modelos e idiomas de las barajas coleccionables. Cada ícono es un mazo chiquito:
el dorso de esa baraja achicado, con dos cartas asomando detrás. Correr después de barajas_temas.py.
(La Baraja de Oro conserva su ícono de siempre: baraja_de_oro.png.)
"""
import json, os
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(HERE, "..", "src", "main", "resources", "assets", "trucoya")
CLAVES = ["criolla", "mandinga", "llanura", "sabana", "desierto", "monte", "nieve", "sierra", "fundadores"]
NOMBRES = {
    "criolla": ("Baraja Criolla", "Criollo Deck"), "mandinga": ("Baraja del Mandinga", "Mandinga's Deck"),
    "llanura": ("Baraja de la Llanura", "Plains Deck"), "sabana": ("Baraja de la Sabana", "Savanna Deck"),
    "desierto": ("Baraja del Desierto", "Desert Deck"), "monte": ("Baraja del Monte", "Forest Deck"),
    "nieve": ("Baraja de la Nieve", "Snow Deck"), "sierra": ("Baraja de la Sierra", "Mountain Deck"),
    "fundadores": ("Baraja de los Fundadores", "Founders' Deck"),
}


def wjson(obj, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write("\n")


def icono(clave):
    atlas = Image.open(os.path.join(A, "textures", "gui", f"cartas_{clave}.png")).convert("RGBA")
    dorso = atlas.crop((0, 4 * 192, 128, 5 * 192))
    cara = dorso.resize((8, 12), Image.LANCZOS)
    px = cara.load()
    for x in range(8):                           # alfa duro (pixel art)
        for y in range(12):
            r, g, b, a = px[x, y]
            px[x, y] = (r, g, b, 255)
    medio = dorso.resize((1, 1), Image.LANCZOS).getpixel((0, 0))[:3]
    borde = tuple(int(v * 0.45) for v in medio) + (255,)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    papel, sombra = (236, 226, 200, 255), (120, 104, 80, 255)
    for ox, oy in ((5, 0), (3, 1)):              # dos cartas detrás, apenas corridas
        d.rectangle([ox, oy, ox + 9, oy + 13], fill=papel, outline=sombra)
    x0, y0 = 1, 2
    d.rectangle([x0, y0, x0 + 9, y0 + 13], fill=borde)
    img.paste(cara, (x0 + 1, y0 + 1))
    for (x, y) in ((x0, y0), (x0, y0 + 13)):     # esquinas de afuera, redondeadas
        img.putpixel((x, y), (0, 0, 0, 0))
    return img


def main():
    es, en = {}, {}
    for clave in CLAVES:
        icono(clave).save(os.path.join(A, "textures", "item", f"baraja_{clave}.png"))
        wjson({"parent": "minecraft:item/generated", "textures": {"layer0": f"trucoya:item/baraja_{clave}"}},
              os.path.join(A, "models", "item", f"baraja_{clave}.json"))
        es[f"item.trucoya.baraja_{clave}"], en[f"item.trucoya.baraja_{clave}"] = NOMBRES[clave]
    L = os.path.join(A, "lang")
    for f in os.listdir(L):
        full = os.path.join(L, f)
        with open(full, encoding="utf-8") as fh:
            d = json.load(fh)
        d.update(en if f == "en_us.json" else es)
        wjson(d, full)
    print("barajas: íconos, modelos e idiomas listos")


if __name__ == "__main__":
    main()


# ---------------------------------------------------------------- encomienda y logros de 1.6.0

def encomienda():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    papel, osc, claro, hilo = (176, 128, 74, 255), (110, 74, 40, 255), (206, 162, 104, 255), (238, 226, 196, 255)
    d.rectangle([2, 4, 13, 13], fill=papel, outline=osc)
    d.line([3, 5, 12, 5], fill=claro)
    d.line([7, 4, 7, 13], fill=hilo)
    d.line([8, 4, 8, 13], fill=hilo)
    d.line([2, 8, 13, 8], fill=hilo)
    d.point([(6, 2), (9, 2), (5, 3), (10, 3)], fill=hilo)          # el moño
    d.point([(7, 3), (8, 3)], fill=(200, 60, 50, 255))
    d.rectangle([10, 10, 12, 12], fill=(180, 40, 40, 255))          # lacre
    return img


def extras():
    encomienda().save(os.path.join(A, "textures", "item", "encomienda.png"))
    wjson({"parent": "minecraft:item/generated", "textures": {"layer0": "trucoya:item/encomienda"}},
          os.path.join(A, "models", "item", "encomienda.json"))
    D = os.path.join(A, "..", "..", "data", "trucoya", "advancements")
    logros = [
        ("iniciacion/gaucho_de_palabra", "iniciacion/toma_asiento", "trucoya:encomienda", "task", 25,
         "Gaucho de palabra", "Cumplí cinco pedidos de los paisanos", "A Man of His Word", "Complete five villager requests"),
        ("iniciacion/siete_dias_seguidos", "iniciacion/toma_asiento", "minecraft:clock", "challenge", 50,
         "Siete días seguidos", "Cumplí los tres desafíos del día siete días seguidos", "Seven Days Straight",
         "Complete all three daily challenges seven days in a row"),
        ("historia/coleccionista", "historia/primer_sello", "trucoya:baraja_llanura", "goal", 40,
         "Coleccionista", "Juntá seis barajas distintas", "Collector", "Collect six different decks"),
    ]
    es = {"item.trucoya.encomienda": "Encomienda"}
    en = {"item.trucoya.encomienda": "Parcel"}
    for ruta, padre, icon, marco, xp, t_es, d_es, t_en, d_en in logros:
        clave = "advancements.trucoya." + ruta.replace("/", ".")
        wjson({"display": {"icon": {"item": icon}, "title": {"translate": clave + ".title"},
                           "description": {"translate": clave + ".description"}, "frame": marco,
                           "show_toast": True, "announce_to_chat": True, "hidden": False},
               "parent": "trucoya:" + padre, "criteria": {"hazana": {"trigger": "minecraft:impossible"}},
               "rewards": {"experience": xp}}, os.path.join(D, *(ruta + ".json").split("/")))
        es[clave + ".title"], es[clave + ".description"] = t_es, d_es
        en[clave + ".title"], en[clave + ".description"] = t_en, d_en
    L = os.path.join(A, "lang")
    for f in os.listdir(L):
        full = os.path.join(L, f)
        with open(full, encoding="utf-8") as fh:
            d = json.load(fh)
        d.update(en if f == "en_us.json" else es)
        wjson(d, full)


extras()
