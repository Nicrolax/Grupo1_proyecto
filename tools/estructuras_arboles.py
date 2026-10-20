"""
Árboles "orgánicos" para las estructuras, como los que arman los buenos constructores de Minecraft: nada de
bolas de hojas sobre un palo. Cada árbol tiene
  - un tronco que se ensancha en la base (raíces que salen y se hunden en el pasto) y que se inclina un poco;
  - ramas de madera que salen del tronco hacia arriba y hacia afuera, cada una con su copa;
  - copas irregulares (elipsoides con ruido, con huecos), hojas persistentes (no se secan);
  - según la especie: enredaderas que cuelgan (monte y bañado), copa chata (espinillo / acacia), conos (pino),
    tronco blanco y alto (eucalipto), o el ombú: tronco enorme, raíces anchas y una copa ancha y baja.
Todo es determinista (misma semilla, mismo árbol) y nunca pisa bloques que ya están puestos.
"""
import math
import random

from estructuras_base import AIR, FL

W = "minecraft:"

ESPECIES = {
    #             madera          hoja              alto  grosor ramas copa  achatar  raices enreda
    "ombu":      ("dark_oak",     "dark_oak",        6,    2,     6,   3.6,  0.55,    4,     0.0),
    "roble":     ("oak",          "oak",             7,    1,     4,   2.8,  0.65,    2,     0.0),
    "monte":     ("oak",          "jungle",          8,    1,     4,   2.6,  0.70,    2,     0.25),
    "eucalipto": ("birch",        "oak",            11,    1,     3,   2.2,  0.85,    1,     0.0),
    "espinillo": ("acacia",       "acacia",          5,    1,     3,   3.4,  0.35,    1,     0.0),
    "sauce":     ("oak",          "oak",             6,    1,     4,   3.0,  0.60,    2,     0.45),
    "pino":      ("spruce",       "spruce",         10,    1,     0,   0.0,  1.00,    1,     0.0),
}


def _libre(t, x, y, z):
    sx, sy, sz = t.size
    if not (0 <= x < sx and 0 <= y < sy and 0 <= z < sz):
        return False
    b = t.get(x, y, z)
    return b is None or b == AIR or b.endswith(":air") or "grass" in b or "fern" in b or "flower" in b


def _poner(t, x, y, z, estado, forzar=False):
    if forzar or _libre(t, x, y, z):
        t.set(x, y, z, estado)


def _madera(m, eje="y"):
    return f"{W}{m}_wood[axis={eje}]" if m != "birch" or eje != "y" else f"{W}birch_log[axis=y]"


def _tronco(m, eje="y"):
    return f"{W}{m}_log[axis={eje}]"


def _hoja(h):
    return f"{W}{h}_leaves[distance=1,persistent=true]"


def _eje(dx, dy, dz):
    a = (abs(dx), abs(dy), abs(dz))
    return "y" if a[1] >= max(a[0], a[2]) else ("x" if a[0] >= a[2] else "z")


def copa(t, cx, cy, cz, radio, achatar, hoja, r, huecos=0.12):
    """Una copa de hojas: elipsoide con el borde irregular (ruido) y algunos huecos."""
    R = int(math.ceil(radio)) + 1
    ry = max(1.2, radio * achatar)
    fase = r.random() * 6.28
    for dx in range(-R, R + 1):
        for dy in range(-int(math.ceil(ry)) - 1, int(math.ceil(ry)) + 1):
            for dz in range(-R, R + 1):
                ang = math.atan2(dz, dx)
                borde = radio * (0.82 + 0.18 * math.sin(3 * ang + fase) + 0.12 * (r.random() - 0.5))
                d = (dx / max(0.5, borde)) ** 2 + (dz / max(0.5, borde)) ** 2 + (dy / ry) ** 2
                if d <= 1.0:
                    # más tupida adentro, con huecos hacia el borde
                    if d > 0.55 and r.random() < huecos + (d - 0.55) * 0.5:
                        continue
                    _poner(t, cx + dx, cy + dy, cz + dz, _hoja(hoja))


def enredaderas(t, x0, y0, z0, x1, y1, z1, prob, r):
    """Lianas que cuelgan del borde de abajo de la copa."""
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            for y in range(y1, y0 - 1, -1):
                b = t.get(x, y, z)
                if b and "leaves" in b and _libre(t, x, y - 1, z):
                    if r.random() < prob:
                        largo = 1     # una sola: colgada de la hoja (debajo de otra liana no se sostiene)
                        for k in range(1, largo + 1):
                            if not _libre(t, x, y - k, z) or y - k <= FL + 1:
                                break
                            # la liana se apoya en la hoja de arriba (cara "up")
                            t.set(x, y - k, z, f"{W}vine[east=false,north=false,south=false,up=true,west=false]")
                    break


def arbol(t, x, z, especie="roble", alto=None, semilla=None, y0=FL, escala=1.0):
    """Planta un árbol de la especie dada con la base en (x, y0, z) (y0 = el suelo)."""
    m, h, alto_def, grosor, nramas, rcopa, achatar, nraices, enreda = ESPECIES[especie]
    alto = alto or alto_def
    rcopa *= escala
    r = random.Random(semilla if semilla is not None else (x * 7919 + z * 104729 + alto))
    if especie == "pino":
        return _pino(t, x, z, alto, r, y0)
    # ---- tronco (inclinado apenas)
    lx, lz = r.choice((-1, 0, 1)) * 0.12, r.choice((-1, 0, 1)) * 0.12
    cimas = []
    for k in range(alto):
        px, pz = int(round(x + lx * k)), int(round(z + lz * k))
        for gx in range(grosor):
            for gz in range(grosor):
                _poner(t, px + gx, y0 + 1 + k, pz + gz, _madera(m) if grosor > 1 else _tronco(m), forzar=True)
        cimas.append((px, y0 + 1 + k, pz))
    tx, ty, tz = cimas[-1]
    # ---- raíces: salen en la base y se hunden
    for _ in range(nraices):
        ang = r.random() * 6.28
        ux, uz = math.cos(ang), math.sin(ang)
        largo = r.randint(1, 2 + grosor)
        for k in range(1, largo + 1):
            rx = int(round(x + (grosor - 1) / 2 + ux * (k + grosor / 2)))
            rz = int(round(z + (grosor - 1) / 2 + uz * (k + grosor / 2)))
            yy = y0 + 1 if k < largo - 1 else y0
            _poner(t, rx, yy, rz, _madera(m, _eje(ux, 0, uz)), forzar=(yy == y0))
    # ---- ramas, cada una con su copa
    copas = [(tx + grosor // 2, ty + 1, tz + grosor // 2, rcopa)]
    for i in range(nramas):
        ang = i * 6.28 / max(1, nramas) + r.random() * 0.8
        ux, uz = math.cos(ang), math.sin(ang)
        base_k = int(alto * (0.55 + 0.35 * r.random()))
        bx, by, bz = cimas[min(base_k, len(cimas) - 1)]
        largo = r.randint(2, 3 + grosor) + (2 if especie in ("ombu", "espinillo") else 0)
        sube = 0.35 if especie in ("ombu", "espinillo") else 0.7
        ex, ey, ez = bx, by, bz
        for k in range(1, largo + 1):
            ex = int(round(bx + (grosor - 1) / 2 + ux * k))
            ez = int(round(bz + (grosor - 1) / 2 + uz * k))
            ey = int(round(by + sube * k))
            _poner(t, ex, ey, ez, _madera(m, _eje(ux, sube, uz)))
        copas.append((ex, ey + 1, ez, rcopa * (0.75 + 0.25 * r.random())))
    for cx, cy, cz, rad in copas:
        copa(t, cx, cy, cz, rad, achatar, h, r)
    if enreda > 0:
        R = int(rcopa * 2.5) + 3
        enredaderas(t, x - R, y0 + 2, z - R, x + R, ty + 6, z + R, enreda, r)


def _pino(t, x, z, alto, r, y0):
    """Pino: tronco recto y pisos de ramas que se achican hacia la punta."""
    for k in range(alto):
        _poner(t, x, y0 + 1 + k, z, _tronco("spruce"), forzar=True)
    for k in range(2, alto + 2):
        frac = 1 - (k - 2) / alto
        rad = 0.6 + 3.0 * frac * (0.75 if k % 2 else 1.0)
        R = int(math.ceil(rad))
        for dx in range(-R, R + 1):
            for dz in range(-R, R + 1):
                d = math.hypot(dx, dz)
                if d <= rad and (d < rad - 0.6 or r.random() > 0.35):
                    _poner(t, x + dx, y0 + 1 + k, z + dz, _hoja("spruce"))
    _poner(t, x, y0 + alto + 2, z, _hoja("spruce"))


def plantar_donde_entre(t, especie, cantidad=1, radio=3, alto=None, separacion=7, evitar=(), semilla=7):
    """
    Busca lugares de pasto libres (sin edificio, sin gente, sin caminos marcados en 'evitar') donde entra el
    árbol entero sin salirse de la plantilla, y planta hasta 'cantidad' árboles separados entre sí.
    """
    sx, sy, sz = t.size
    alto = min(alto or ESPECIES[especie][2], sy - FL - 5)   # que entre en la altura de la plantilla
    if alto < 4:
        return []
    r = random.Random(semilla)
    ocupados = [(ex, ez) for ex, ey, ez, _ in t.entities]
    candidatos = [(x, z) for x in range(radio + 1, sx - radio - 1) for z in range(radio + 1, sz - radio - 1)]
    r.shuffle(candidatos)
    puestos = []
    for x, z in candidatos:
        if len(puestos) >= cantidad:
            break
        if any(ax0 - 1 <= x <= ax1 + 1 and az0 - 1 <= z <= az1 + 1 for ax0, az0, ax1, az1 in evitar):
            continue
        if any(math.hypot(x - px, z - pz) < separacion for px, pz in puestos):
            continue
        if any(math.hypot(x - ex, z - ez) < radio + 1 for ex, ez in ocupados):
            continue
        suelo = t.get(x, FL, z) or ""
        if not any(k in suelo for k in ("grass_block", "dirt", "podzol", "sand", "snow")):
            continue
        libre = all(_libre(t, x + dx, y, z + dz)
                    for dx in range(-radio, radio + 1) for dz in range(-radio, radio + 1)
                    for y in range(FL + 1, min(sy, FL + alto + 3)))
        if not libre:
            continue
        arbol(t, x, z, especie, alto=alto, semilla=r.randint(0, 10 ** 6), escala=0.8)
        puestos.append((x, z))
    return puestos
