"""
Detalle y profundidad para las estructuras: lo que hace que una construcción de Minecraft no se vea "plana".
Técnicas de los buenos constructores: nada de paredes lisas de un solo bloque. En lugar de eso:
  - zócalo saliente (escalones contra el muro) y esquinas con pilares que se adelantan;
  - pilastras cada pocos bloques, que marcan el ritmo de la fachada;
  - cornisa bajo el techo (escalones invertidos) y aleros con ménsulas;
  - ventanas con alféizar y guardapolvo (dintel saliente);
  - texturas mezcladas (gradientes de bloques parecidos) y desgaste en la base;
  - un terreno vivo: manchas de tierra y senderos irregulares, pasto alto, helechos, matas, piedras,
    faroles en postes, leña, barriles.
Todo respeta lo que ya hay (no pisa puertas, ventanas, carteles ni nada que no sea aire).
"""
import math
from estructuras_base import AIR, FL, parse_state

W = "minecraft:"
DIRS = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}
OPUESTO = {"north": "south", "south": "north", "east": "west", "west": "east"}

NO_MURO = ("door", "pane", "bars", "fence", "trapdoor", "sign", "glass", "air", "lantern", "torch", "gate", "chain",
           "ladder", "banner", "vine", "slab", "stairs", "carpet", "leaves", "campfire", "water", "flower", "potted")


def _libre(t, x, y, z):
    sx, sy, sz = t.size
    if not (0 <= x < sx and 0 <= y < sy and 0 <= z < sz):
        return False
    b = t.get(x, y, z)
    return b is None or b == AIR or b.endswith(":air")


def _es_muro(t, x, y, z):
    b = t.get(x, y, z)
    if b is None:
        return False
    n = parse_state(b)[0]
    return not any(k in n for k in NO_MURO)


def _apoyo(t, x, y, z):
    """Hay algo firme abajo (suelo o bloque)."""
    b = t.get(x, y - 1, z)
    if b is None:
        return False
    n = parse_state(b)[0]
    return not any(k in n for k in ("air", "water", "flower", "grass", "fern", "snow[", "lily", "torch")) or n.endswith("grass_block")


def escalon(mat, facing, half="bottom", shape="straight"):
    return f"{W}{mat}_stairs[facing={facing},half={half},shape={shape},waterlogged=false]"


def _perimetro(x0, z0, x1, z1):
    """Celdas del muro con la dirección hacia afuera: (x, z, afuera)."""
    for x in range(x0 + 1, x1):
        yield x, z0, "north"
        yield x, z1, "south"
    for z in range(z0 + 1, z1):
        yield x0, z, "west"
        yield x1, z, "east"


def _cerca_de_abertura(t, x, z, y0, y1, afuera, radio=1):
    """¿Hay una puerta o ventana en el muro a 'radio' bloques?"""
    dx, dz = (1, 0) if afuera in ("north", "south") else (0, 1)
    for k in range(-radio, radio + 1):
        for y in range(y0, y1 + 1):
            if not _es_muro(t, x + dx * k, y, z + dz * k):
                return True
    return False


def zocalo(t, x0, z0, x1, z1, y, mat):
    """Zócalo saliente: escalones apoyados contra el pie del muro (le da peso a la base)."""
    for x, z, afuera in _perimetro(x0, z0, x1, z1):
        ox, oz = x + DIRS[afuera][0], z + DIRS[afuera][1]
        if _es_muro(t, x, y, z) and _libre(t, ox, y, oz) and _apoyo(t, ox, y, oz):
            t.set(ox, y, oz, escalon(mat, OPUESTO[afuera]))


def esquinas(t, x0, z0, x1, z1, y0, y1, pilar, remate=None):
    """Pilares en las cuatro esquinas, adelantados en diagonal: la esquina deja de ser una arista plana."""
    for x, z, ax, az in ((x0, z0, -1, -1), (x1, z0, 1, -1), (x0, z1, -1, 1), (x1, z1, 1, 1)):
        for cx, cz in ((x + ax, z), (x, z + az)):
            if all(_libre(t, cx, y, cz) for y in range(y0, y1 + 1)) and _apoyo(t, cx, y0, cz):
                for y in range(y0, y1 + 1):
                    t.set(cx, y, cz, pilar)
                if remate and _libre(t, cx, y1 + 1, cz):
                    t.set(cx, y1 + 1, cz, remate)


def pilastras(t, x0, z0, x1, z1, y0, y1, bloque, paso=4, capitel=None):
    """Pilastras salientes cada 'paso' bloques, salteando puertas y ventanas."""
    for x, z, afuera in _perimetro(x0, z0, x1, z1):
        k = (x - x0) if afuera in ("north", "south") else (z - z0)
        if k % paso != 0 or k == 0:
            continue
        if _cerca_de_abertura(t, x, z, y0, y1, afuera):
            continue
        ox, oz = x + DIRS[afuera][0], z + DIRS[afuera][1]
        if not all(_libre(t, ox, y, oz) for y in range(y0, y1 + 1)):
            continue
        for y in range(y0, y1 + 1):
            t.set(ox, y, oz, bloque)
        if capitel and _libre(t, ox, y1 + 1, oz):
            t.set(ox, y1 + 1, oz, capitel(afuera) if callable(capitel) else capitel)


def cornisa(t, x0, z0, x1, z1, y, mat):
    """Cornisa: escalones invertidos corridos bajo el alero (una línea de sombra bajo el techo)."""
    for x, z, afuera in _perimetro(x0, z0, x1, z1):
        ox, oz = x + DIRS[afuera][0], z + DIRS[afuera][1]
        if _es_muro(t, x, y, z) and _libre(t, ox, y, oz):
            t.set(ox, y, oz, escalon(mat, OPUESTO[afuera], "top"))


def ventanas(t, mat, flores=None, prob_flor=0.6):
    """Alféizar (escalón) debajo y guardapolvo (escalón invertido) arriba de cada ventana registrada."""
    for x, y, z, afuera, alto in getattr(t, "ventanas", []):
        ox, oz = x + DIRS[afuera][0], z + DIRS[afuera][1]
        if _libre(t, ox, y - 1, oz):
            t.set(ox, y - 1, oz, escalon(mat, OPUESTO[afuera], "top"))
            # jardinera: una maceta sobre el alféizar no entra; va una flor en maceta solo si hay alféizar macizo
        if _libre(t, ox, y + alto, oz) and _es_muro(t, x, y + alto, z):
            t.set(ox, y + alto, oz, escalon(mat, OPUESTO[afuera], "top"))
        if flores and t.r.random() < prob_flor and _libre(t, ox, y - 2, oz) and _apoyo(t, ox, y - 2, oz):
            t.set(ox, y - 2, oz, W + t.r.choice(flores))


def mensulas(t, x0, z0, x1, z1, y, bloque, paso=3):
    """Ménsulas bajo el alero: cada 'paso' bloques un apoyo saliente (cerco o tronco) que sostiene el techo."""
    for x, z, afuera in _perimetro(x0, z0, x1, z1):
        k = (x - x0) if afuera in ("north", "south") else (z - z0)
        if k % paso:
            continue
        ox, oz = x + DIRS[afuera][0], z + DIRS[afuera][1]
        if _libre(t, ox, y, oz) and not _libre(t, ox, y + 1, oz):
            t.set(ox, y, oz, bloque)


def desgaste(t, x0, y0, z0, x1, y1, z1, reemplazos, prob=0.18):
    """Gradiente y desgaste: cambia algunos bloques por parecidos (más en la base)."""
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            for z in range(z0, z1 + 1):
                b = t.get(x, y, z)
                if b is None:
                    continue
                n = parse_state(b)[0]
                if n in reemplazos:
                    altura = (y - y0) / max(1, y1 - y0)
                    if t.r.random() < prob * (1.6 - altura):
                        t.set(x, y, z, t.r.choice(reemplazos[n]))


# ---------------------------------------------------------------- terreno

def _ruido(x, z, semilla, escala=4.0):
    """Ruido suave (suma de senos) para manchas orgánicas."""
    a = math.sin((x + semilla * 1.7) / escala) + math.sin((z - semilla) / (escala * 0.8))
    b = math.sin((x + z + semilla * 3.1) / (escala * 1.3)) * 0.8
    return (a + b) / 2.8


def paisaje(t, zona, estilo="pradera", evitar=(), matas=True):
    """
    Enriquece el suelo libre (y = FL) de la zona: manchas, pasto alto, helechos, matas, piedras.
    'evitar': rectángulos (x0, z0, x1, z1) donde no se pone vegetación alta (frente a puertas, caminos).
    """
    x0, z0, x1, z1 = zona
    sem = t.r.randint(0, 999)
    P = {
        "pradera": dict(manchas=[W + "coarse_dirt", W + "grass_path", W + "podzol[snowy=false]"],
                        plantas=["grass", "grass", "grass", "tall_grass", "dandelion", "poppy", "cornflower",
                                 "oxeye_daisy", "azure_bluet"],
                        mata=W + "oak_leaves[distance=1,persistent=true]", piedra=[W + "cobblestone", W + "mossy_cobblestone", W + "andesite"]),
        "bosque": dict(manchas=[W + "podzol[snowy=false]", W + "coarse_dirt", W + "moss_block" if False else W + "mossy_cobblestone"],
                       plantas=["fern", "fern", "grass", "large_fern", "brown_mushroom", "red_mushroom", "lily_of_the_valley"],
                       mata=W + "spruce_leaves[distance=1,persistent=true]", piedra=[W + "mossy_cobblestone", W + "cobblestone", W + "stone"]),
        "seco": dict(manchas=[W + "coarse_dirt", W + "red_sand", W + "gravel"],
                     plantas=["dead_bush", "grass", "dead_bush"],
                     mata=W + "acacia_leaves[distance=1,persistent=true]", piedra=[W + "smooth_sandstone", W + "sandstone", W + "terracotta"]),
        "sabana": dict(manchas=[W + "coarse_dirt", W + "grass_path"],
                       plantas=["grass", "grass", "tall_grass", "dead_bush", "orange_tulip"],
                       mata=W + "acacia_leaves[distance=1,persistent=true]", piedra=[W + "cobblestone", W + "andesite"]),
        "nieve": dict(manchas=[W + "packed_ice", W + "snow_block"],
                      plantas=[], mata=W + "spruce_leaves[distance=1,persistent=true]",
                      piedra=[W + "cobblestone", W + "stone", W + "andesite"]),
        "humedo": dict(manchas=[W + "podzol[snowy=false]", W + "coarse_dirt", W + "mossy_cobblestone"],
                       plantas=["fern", "grass", "blue_orchid", "grass", "large_fern"],
                       mata=W + "oak_leaves[distance=1,persistent=true]", piedra=[W + "mossy_cobblestone", W + "cobblestone"]),
        "sierra": dict(manchas=[W + "gravel", W + "coarse_dirt", W + "stone", W + "andesite"],
                       plantas=["grass", "grass", "poppy", "oxeye_daisy", "fern"],
                       mata=W + "spruce_leaves[distance=1,persistent=true]", piedra=[W + "andesite", W + "cobblestone", W + "stone", W + "diorite"]),
    }[estilo]

    ocupados = set()
    for ex, ey, ez, _ in t.entities:        # donde hay gente o animales no crece nada (que no queden atrapados)
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                ocupados.add((int(math.floor(ex)) + dx, int(math.floor(ez)) + dz))

    def evitado(x, z):
        return (x, z) in ocupados or any(a <= x <= c and b <= z <= d for a, b, c, d in evitar)

    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b = t.get(x, FL, z) or ""
            if not ("grass_block" in b or "sand" in b or "dirt" in b or "podzol" in b or "snow_block" in b):
                continue
            if not _libre(t, x, FL + 1, z) and "snow[" not in (t.get(x, FL + 1, z) or ""):
                continue
            r = _ruido(x, z, sem)
            # manchas orgánicas de otra tierra
            if r > 0.55 and P["manchas"] and t.r.random() < 0.75:
                t.set(x, FL, z, t.r.choice(P["manchas"]))
                continue
            if evitado(x, z):
                continue
            k = t.r.random()
            if P["plantas"] and k < 0.30 and _libre(t, x, FL + 1, z):
                p = t.r.choice(P["plantas"])
                if p in ("tall_grass", "large_fern"):
                    if _libre(t, x, FL + 2, z):
                        t.set(x, FL + 1, z, f"{W}{p}[half=lower]")
                        t.set(x, FL + 2, z, f"{W}{p}[half=upper]")
                else:
                    t.set(x, FL + 1, z, W + p)
            elif not matas:
                continue
            elif k < 0.33 and _libre(t, x, FL + 1, z):
                # mata baja de hojas
                t.set(x, FL + 1, z, P["mata"])
                for dx, dz in ((1, 0), (0, 1), (-1, 0), (0, -1)):
                    if t.r.random() < 0.45 and _libre(t, x + dx, FL + 1, z + dz) and not evitado(x + dx, z + dz) \
                            and _apoyo(t, x + dx, FL + 1, z + dz) and "path" not in (t.get(x + dx, FL, z + dz) or ""):
                        t.set(x + dx, FL + 1, z + dz, P["mata"])
            elif k < 0.345 and _libre(t, x, FL + 1, z):
                # piedra suelta
                t.set(x, FL + 1, z, t.r.choice(P["piedra"]))
                if t.r.random() < 0.5 and _libre(t, x + 1, FL + 1, z):
                    t.set(x + 1, FL + 1, z, f"{W}{'cobblestone' if 'cobble' in P['piedra'][0] else 'stone'}_slab[type=bottom,waterlogged=false]"
                          if "sandstone" not in P["piedra"][0] else f"{W}sandstone_slab[type=bottom,waterlogged=false]")


def poste_farol(t, x, z, cerco=W + "spruce_fence", alto=2, base=None, alma=False):
    """Poste con farol: base de piedra, cerco y farol arriba."""
    y = FL + 1
    if not _libre(t, x, y, z):
        return
    if base:
        t.set(x, y, z, base)
        y += 1
    for k in range(alto):
        t.set(x, y + k, z, cerco)
    t.set(x, y + alto, z, f"{W}{'soul_' if alma else ''}lantern[hanging=false]")


def lenia(t, x, z, largo=3, madera="spruce", eje="x"):
    """Pila de leña con tronco y losas."""
    for k in range(largo):
        cx, cz = (x + k, z) if eje == "x" else (x, z + k)
        if _libre(t, cx, FL + 1, cz):
            t.set(cx, FL + 1, cz, f"{W}{madera}_log[axis={'z' if eje == 'x' else 'x'}]")
            if k % 2 == 0 and _libre(t, cx, FL + 2, cz):
                t.set(cx, FL + 2, cz, f"{W}{madera}_slab[type=bottom,waterlogged=false]")


def sendero_organico(t, puntos, bloques=(W + "grass_path", W + "coarse_dirt", W + "gravel"), ancho=2):
    """Camino irregular: ancho variable y mezcla de tierra, grava y huella."""
    for i, (x, z) in enumerate(puntos):
        a = ancho + (1 if t.r.random() < 0.3 else 0)
        for k in range(a):
            for (cx, cz) in ((x + k, z), (x, z + k)):
                b = t.get(cx, FL, cz) or ""
                if "grass_block" in b or "dirt" in b or "sand" in b or "podzol" in b or "snow" in b or b == "":
                    t.set(cx, FL, cz, t.r.choice(bloques))
                    if not _libre(t, cx, FL + 1, cz):
                        n = parse_state(t.get(cx, FL + 1, cz))[0]
                        if any(p in n for p in ("grass", "fern", "flower", "leaves", "poppy", "dandelion", "bush", "tulip")):
                            t.set(cx, FL + 1, cz, AIR)
                            if t.get(cx, FL + 2, cz) and "upper" in t.get(cx, FL + 2, cz):
                                t.set(cx, FL + 2, cz, AIR)


# ---------------------------------------------------------------- más profundidad: techos, postes y plantas

def variar(t, reemplazos, prob=0.15, zona=None):
    """
    Mezcla materiales parecidos sin cambiar la forma (mantiene facing, half, type...): techos de tejas con
    tejas de otro tono, pisos con piezas gastadas. reemplazos: {nombre: [nombres alternativos]}.
    """
    for (x, y, z), (estado, nbt) in list(t.blocks.items()):
        if zona and not (zona[0] <= x <= zona[2] and zona[1] <= z <= zona[3]):
            continue
        nombre, props = parse_state(estado)
        if nombre in reemplazos and nbt is None and t.r.random() < prob:
            nuevo = t.r.choice(reemplazos[nombre])
            t.set(x, y, z, nuevo + ("[" + ",".join(f"{k}={v}" for k, v in props.items()) + "]" if props else ""))


def zapatas(t, mat, postes=("fence", "stripped", "_log", "_wall"), y_min=FL + 3):
    """
    Zapatas: donde un poste (cerco, tronco, muro) sostiene una viga o un techo, escalones invertidos a los
    costados del poste, pegados al techo. Es el detalle que más "carpintería" le da a una galería.
    """
    nuevos = []
    for (x, y, z), (estado, nbt) in t.blocks.items():
        if y < y_min:
            continue
        n = parse_state(estado)[0]
        if not any(p in n for p in postes) or "gate" in n or "sign" in n or "torch" in n:
            if not n.endswith("_log"):
                continue
        props = parse_state(estado)[1]
        if n.endswith("_log") and props.get("axis", "y") != "y":
            continue
        arriba = t.get(x, y + 1, z)
        if arriba is None or arriba == AIR or arriba.endswith(":air"):
            continue
        abajo = t.get(x, y - 1, z)
        if abajo is None or abajo == AIR or not any(p in parse_state(abajo)[0] for p in postes + ("planks",)):
            continue                     # solo postes de verdad (con algo abajo), no bloques sueltos
        for d, (dx, dz) in DIRS.items():
            cx, cz = x + dx, z + dz
            if _libre(t, cx, y, cz) and not _libre(t, cx, y + 1, cz) and _libre(t, cx, y - 1, cz):
                nuevos.append((cx, y, cz, escalon(mat, OPUESTO[d], "top")))
    for x, y, z, b in nuevos:
        if _libre(t, x, y, z):
            t.set(x, y, z, b)


def enredaderas(t, x0, z0, x1, z1, y0, y1, prob=0.12):
    """Enredaderas colgando de algunos paños de muro (afuera)."""
    for x, z, afuera in _perimetro(x0, z0, x1, z1):
        if t.r.random() > prob:
            continue
        ox, oz = x + DIRS[afuera][0], z + DIRS[afuera][1]
        lado = OPUESTO[afuera]
        largo = t.r.randint(2, max(2, y1 - y0))
        for y in range(y1, y1 - largo, -1):
            if _libre(t, ox, y, oz) and _es_muro(t, x, y, z):
                caras = {d: "false" for d in ("north", "south", "east", "west")}
                caras[lado] = "true"
                t.set(ox, y, oz, W + "vine[" + ",".join(f"{k}={v}" for k, v in sorted(caras.items())) + ",up=false]")
            else:
                break


def borde_techo(t, mat, y_min=FL + 3):
    """
    Remate de los hastiales: donde termina el techo (escalón o losa al borde de un hastial), un escalón más
    oscuro hacia afuera marca el canto, como una tapa de madera.
    """
    nuevos = []
    for (x, y, z), (estado, nbt) in t.blocks.items():
        if y < y_min:
            continue
        n, p = parse_state(estado)
        if not (n.endswith("_stairs") or n.endswith("_slab")) or p.get("half") == "top":
            continue
        for d, (dx, dz) in DIRS.items():
            cx, cz = x + dx, z + dz
            if not _libre(t, cx, y, cz) or not _libre(t, cx, y - 1, cz):
                continue
            # el canto es el lado perpendicular a la pendiente (el escalón mira hacia la pendiente)
            if n.endswith("_stairs") and p.get("facing") in (d, OPUESTO[d]):
                continue
            nuevos.append((cx, y, cz, f"{W}{mat}_slab[type=bottom,waterlogged=false]"))
    for x, y, z, b in nuevos:
        if _libre(t, x, y, z):
            t.set(x, y, z, b)


def espesor_alero(t, mat, y_min=FL + 4):
    """
    Alero con espesor: debajo de cada pieza del techo que vuela por fuera del muro, una losa invertida de otra
    madera o piedra. El borde del techo deja de ser una lámina de medio bloque y gana sombra y volumen.
    """
    nuevos = []
    for (x, y, z), (estado, nbt) in t.blocks.items():
        if y < y_min:
            continue
        n, p = parse_state(estado)
        if not (n.endswith("_stairs") or n.endswith("_slab")) or p.get("half") == "top" or p.get("type") in ("top", "double"):
            continue
        if not (_libre(t, x, y - 1, z) and _libre(t, x, y - 2, z)):
            continue
        # tiene que estar pegado a un muro (vuela un solo bloque)
        if not any(_es_muro(t, x + dx, y - 1, z + dz) for dx, dz in DIRS.values()):
            continue
        nuevos.append((x, y - 1, z, f"{W}{mat}_slab[type=top,waterlogged=false]"))
    for x, y, z, b in nuevos:
        if _libre(t, x, y, z):
            t.set(x, y, z, b)


# ---------------------------------------------------------------- postigos y faroles de la entrada

def postigos(t, madera="spruce", prob=0.85):
    """Postigos abiertos a los costados de cada ventana (le dan sombra y relieve a la fachada)."""
    for x, y, z, afuera, alto in getattr(t, "ventanas", []):
        if t.r.random() > prob:
            continue
        dx, dz = DIRS[afuera]
        ox, oz = x + dx, z + dz
        lados = [(-1, 0), (1, 0)] if afuera in ("north", "south") else [(0, -1), (0, 1)]
        for lx, lz in lados:
            if not _es_muro(t, x + lx, y, z + lz):
                continue
            for hh in range(alto):
                if _libre(t, ox + lx, y + hh, oz + lz):
                    t.set(ox + lx, y + hh, oz + lz, f"{W}{madera}_trapdoor[facing={afuera},half=bottom,open=true,"
                                                     "powered=false,waterlogged=false]")


def faroles_puerta(t, alma=False):
    """Un farol colgando a cada lado de las puertas, debajo del alero (si hay alero o techo arriba)."""
    for x, y, z, facing in getattr(t, "puertas", []):
        for sentido in (1, -1):
            dx, dz = DIRS[facing]
            ox, oz = x + dx * sentido, z + dz * sentido      # un lado de la puerta (adentro o afuera)
            lados = [(-1, 0), (1, 0)] if facing in ("north", "south") else [(0, -1), (0, 1)]
            for lx, lz in lados:
                fx, fz = ox + lx, oz + lz
                if (_libre(t, fx, y + 2, fz) and _libre(t, fx, y + 1, fz) and _libre(t, fx, y, fz)
                        and _es_muro(t, fx, y + 3, fz)):
                    t.set(fx, y + 2, fz, f"{W}{'soul_' if alma else ''}lantern[hanging=true]")
                    break
