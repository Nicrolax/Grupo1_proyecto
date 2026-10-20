"""
Pulperías por bioma. Todas tienen lo mismo para jugar (mostrador con el pulpero, una mesa 2v2 y
mesas 1v1 con anotador, parroquianos), pero cada bioma tiene su arquitectura:

  llanura  casa colonial blanqueada, tejas, galería, enramada, aljibe y ombú
  sabana   rancho de adobe con techo de paja, ramada de hojas, horno de barro y corral
  desierto casa de arenisca con azotea almenada, portal de arcos, toldo, pozo y palmera
  mesa     lo mismo que el desierto pero en arenisca roja y terracotas de colores
  taiga    cabaña de troncos de abeto, techo empinado, chimenea de piedra y leñera
  bosque   cabaña de roble con techo de roble oscuro, hongos y musgo
  nevada   cabaña de abeto sobre piedra, techo nevado, faroles y nieve alrededor
  pantano  palafito sobre pilotes con muelle, techo de paja y charcos con camalotes
  selva    palafito de madera de jungla con techo de hojas y frutas
  serrana  casa de pirca (piedra) con techo de lajas, corral de piedra con llamas y apacheta

Frente al norte (z chico). Piso en y = FL; debajo, cimiento.
"""
from estructuras_base import *

W = "minecraft:"


def st(b, **props):
    """Estado de bloque: st('spruce_stairs', facing='north') -> 'minecraft:spruce_stairs[facing=north]'."""
    if not props:
        return W + b if ":" not in b else b
    return (W + b if ":" not in b else b) + "[" + ",".join(f"{k}={v}" for k, v in props.items()) + "]"


def escalon(mat, facing, half="bottom"):
    return st(f"{mat}_stairs", facing=facing, half=half, shape="straight", waterlogged="false")


def losa(mat, tipo="bottom"):
    return st(f"{mat}_slab", type=tipo, waterlogged="false")


OPUESTO = {"north": "south", "south": "north", "east": "west", "west": "east"}


# ---------------------------------------------------------------- techos extra

def techo_empinado(t, x0, z0, x1, z1, ybase, mat, hastial, cumbrera=None, alero=True):
    """Techo a dos aguas 1:1 (escalones) con cumbrera a lo largo de x; hastiales macizos."""
    zz0, zz1 = (z0 - 1, z1 + 1) if alero else (z0, z1)
    xx0, xx1 = (x0 - 1, x1 + 1) if alero else (x0, x1)
    for z in range(zz0, zz1 + 1):
        d = min(z - (z0 - 1), (z1 + 1) - z)
        y = ybase + d - 1
        mitad = (z0 + z1) / 2.0
        if abs(z - mitad) < 0.6:
            bloque = cumbrera or losa(mat)
        else:
            bloque = escalon(mat, "south" if z < mitad else "north")
        for x in range(xx0, xx1 + 1):
            if x in (x0, x1) and z0 <= z <= z1:
                for yy in range(ybase, y + 1):
                    t.set(x, yy, z, hastial)
            else:
                t.set(x, y, z, bloque)


def techo_paja(t, x0, z0, x1, z1, ybase, hastial, paja="minecraft:hay_block[axis=x]", alero=True):
    """Techo de paja (quincho): fardos escalonados de a dos filas por nivel, con alero."""
    zz0, zz1 = (z0 - 1, z1 + 1) if alero else (z0, z1)
    for z in range(zz0, zz1 + 1):
        d = min(z - (z0 - 1), (z1 + 1) - z)
        y = ybase + (d - 1) // 2
        for x in range(x0 - 1, x1 + 2):
            if x in (x0, x1) and z0 <= z <= z1:
                for yy in range(ybase, y + 1):
                    t.set(x, yy, z, hastial)
            else:
                t.set(x, y, z, paja)


# ---------------------------------------------------------------- interior común

def boliche(t, x0, z0, x1, z1, techo, madera="dark_oak", reja=W + "iron_bars", alma=False, mesas_extra=True,
            cantinero_nivel=2):
    """
    Interior de pulpería entre los muros x0..x1 / z0..z1 (frente en z0), piso en FL, cielorraso en 'techo'.
    Mostrador enrejado con ventanilla, estantería del fondo, pulpero, mesa 2v2 y mesas 1v1.
    """
    y = FL
    ix0, ix1, iz0, iz1 = x0 + 1, x1 - 1, z0 + 1, z1 - 1
    zc = iz1 - 3
    mid = (ix0 + ix1 - 2) // 2
    for x in range(ix0, ix1 - 1):
        t.set(x, y + 1, zc, st(f"{madera}_planks"))
        t.set(x, y + 2, zc, losa(madera) if x in (mid, mid + 1) else reja)
        for yy in range(y + 3, techo):
            t.set(x, yy, zc, reja)
    t.set(ix1 - 1, y + 1, zc, st(f"{madera}_fence_gate", facing="north", in_wall="false", open="false", powered="false"))
    # estantería
    for x in range(ix0, ix1 - 1):
        t.set(x, y + 1, iz1, st("barrel", facing="north", open="false"))
        if techo > y + 2:
            t.set(x, y + 2, iz1, W + "bookshelf" if x % 2 else st("barrel", facing="north", open="false"))
        if techo > y + 3:
            t.set(x, y + 3, iz1, losa(madera, "bottom"))
            if x % 3 == 0:
                t.set(x, y + 3, iz1, st("brewing_stand", has_bottle_0="true", has_bottle_1="true", has_bottle_2="false"))
    t.set(ix1 - 1, y + 1, iz1, st("smoker", facing="north", lit="false"))
    t.set(ix1, y + 1, iz1, st("cauldron", level="3"))
    t.set(ix0, y + 1, zc + 1, st("hay_block", axis="y"))
    t.gaucho(mid, y + 1, zc + 2, "CANTINERO", cantinero_nivel, mirar=180)
    # mesas
    zt = iz0 + 2
    t.mesa_con_sillas(ix0 + 2, y + 1, zt, cuatro=True)
    t.mesa_con_sillas(ix1 - 2, y + 1, zt)
    if mesas_extra and ix1 - ix0 >= 12:
        t.mesa_con_sillas((ix0 + ix1) // 2, y + 1, zt + 1)
    # faroles colgados de vigas
    for z in (zt, zc + 1):
        for x in range(ix0, ix1 + 1):
            if t.get(x, techo, z) in (None, AIR):
                t.set(x, techo, z, st(f"stripped_{'dark_oak' if madera == 'dark_oak' else madera}_log", axis="x")
                      if madera not in ("crimson", "warped") else W + "dark_oak_planks")
    for x, z in ((ix0 + 2, zt), (ix1 - 2, zt), (mid, zc + 1)):
        t.farol(x, techo - 1, z, alma=alma)
    return dict(zc=zc, zt=zt, mid=mid)


def gente(t, lista):
    for x, y, z, rol, nivel in lista:
        t.gaucho(x, y, z, rol, nivel)


def flores(t, x0, z0, x1, z1, opciones, prob=0.2):
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if t.r.random() < prob and t.get(x, FL + 1, z) in (None, AIR):
                b = t.get(x, FL, z) or ""
                if "grass_block" in b or "dirt" in b or "sand" in b or "podzol" in b:
                    t.set(x, FL + 1, z, W + t.r.choice(opciones))


def suelo(t, x0, z0, x1, z1, mezcla, sub=W + "dirt"):
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if (x, FL, z) not in t.blocks:
                t.set(x, FL, z, mezcla())
            for y in range(0, FL):
                if (x, y, z) not in t.blocks:
                    t.set(x, y, z, sub)


# ================================================================ llanura (colonial)

def llanura():
    SX, SZ = 26, 25
    t = Template(SX, 14, SZ, seed=7)
    BL = W + "smooth_quartz"
    X0, X1, Z0, Z1 = 1, 17, 4, 16
    t.cimiento(X0, 1, X1, Z1)
    t.fill(X0, FL, Z0, X1, FL, Z1, W + "stone_bricks")
    t.fill(X0 + 1, FL, Z0 + 1, X1 - 1, FL, 11, W + "spruce_planks")
    for x in range(X0 + 1, X1):
        if x % 4 == 0:
            t.set(x, FL, 8, st("stripped_spruce_log", axis="x"))
    t.fill(X0 + 1, FL, 12, X1 - 1, FL, Z1 - 1, t.azar((W + "terracotta", 3), (W + "polished_granite", 2)))
    t.muros_coloniales(X0, Z0, X1, Z1, 5)
    t.fill(X0 + 1, h(1), Z0 + 1, X1 - 1, h(5), Z1 - 1, AIR)
    t.techo_dos_aguas(X0, Z0, X1, Z1, h(6), eje="x", hastial=BL, aleros=(True, True, False, True))
    for z in (6, 9, 12, 15):
        t.fill(X0 + 1, h(5), z, X1 - 1, h(5), z, st("stripped_spruce_log", axis="x"))
    for x in range(X0 + 4, X1, 4):
        t.fill(x, h(6), 10, x, h(8), 10, st("stripped_spruce_log", axis="y"))
    t.puerta(8, h(1), Z0, "south", "dark_oak", "right")
    t.puerta(9, h(1), Z0, "south", "dark_oak", "left")
    t.set(8, h(3), Z0, W + "dark_oak_planks")
    t.set(9, h(3), Z0, W + "dark_oak_planks")
    for x in (3, 4, 13, 14):
        t.ventana(x, h(2), Z0, "north", postigo="spruce")
    for z in (7, 10, 14):
        t.ventana(X0, h(2), z, "west", postigo="spruce")
        if z != 10:
            t.ventana(X1, h(2), z, "east", postigo="spruce")
    t.cartel(8, h(4), Z0 - 1, "north", ["", "PULPERÍA", "y almacén", ""])
    t.cartel(9, h(4), Z0 - 1, "north", ["", "LA ESQUINA", "de Don Truco", ""])
    # galería
    t.fill(X0, FL, 1, X1, FL, 3, W + "polished_granite")
    for x in range(X0, X1 + 1, 4):
        t.fill(x, h(1), 1, x, h(3), 1, st("stripped_spruce_log", axis="y"))
    t.fill(X0 - 1, h(4), 1, X1 + 1, h(4), 1, losa("brick"))
    t.fill(X0 - 1, h(4), 2, X1 + 1, h(4), 2, losa("brick", "top"))
    t.fill(X0 - 1, h(5), 3, X1 + 1, h(5), 3, losa("brick", "double"))
    t.fill(X0, h(4), 3, X1, h(4), 3, W + "spruce_planks")
    for x in (4, 5, 13, 14):
        t.set(x, h(1), 3, escalon("spruce", "south"))
    t.farol(6, h(3), 2)
    t.farol(11, h(3), 2)
    # interior
    boliche(t, X0, Z0, X1, Z1, h(5), madera="dark_oak")
    # estufa a leña (pared este)
    t.set(16, h(1), 8, st("furnace", facing="west", lit="true"))
    for z in (7, 9):
        t.set(16, h(1), z, W + "bricks")
        t.set(16, h(2), z, escalon("brick", "west"))
    t.fill(16, h(2), 8, 16, h(9), 8, W + "bricks")
    t.set(16, h(10), 8, st("campfire", facing="north", lit="true", signal_fire="false", waterlogged="false"))
    for x in (2, 3):
        t.set(x, h(1), 5, escalon("spruce", "north"))
    # enramada con parrilla
    t.cimiento(19, 4, 24, 12)
    t.fill(19, FL, 4, 24, FL, 12, W + "coarse_dirt")
    for x, z in ((19, 4), (24, 4), (19, 12), (24, 12), (19, 8), (24, 8)):
        t.fill(x, h(1), z, x, h(3), z, W + "spruce_fence")
    t.fill(19, h(4), 4, 24, h(4), 12, losa("spruce"))
    for x in (22, 23):
        t.set(x, h(1), 6, st("campfire", facing="north", lit="true", signal_fire="false", waterlogged="false"))
        t.set(x, h(2), 6, st("iron_trapdoor", facing="north", half="bottom", open="false", powered="false", waterlogged="false"))
    t.set(21, h(1), 6, W + "bricks")
    t.set(24, h(1), 6, W + "bricks")
    t.set(21, h(1), 10, W + "spruce_planks")
    t.set(22, h(1), 10, W + "spruce_planks")
    for x in (21, 22):
        t.set(x, h(1), 9, escalon("spruce", "south"))
        t.set(x, h(1), 11, escalon("spruce", "north"))
    t.fill(20, h(1), 12, 20, h(2), 12, st("hay_block", axis="y"))
    t.farol(21, h(3), 8)
    # palenque, caballos, aljibe y ombú
    t.fill(19, h(1), 1, 24, h(1), 1, W + "spruce_fence")
    for x in (20, 23):
        t.animal(x, h(1), 2, W + "horse", {"Tame": Byte(1)})
    t.cimiento(19, 15, 23, 19)
    t.fill(19, FL, 15, 23, FL, 19, W + "polished_andesite")
    t.box(20, h(1), 16, 22, h(1), 18, W + "stone_bricks")
    for y in (FL - 1, FL, h(1)):
        t.set(21, y, 17, st("water", level="0"))
    for x, z in ((20, 16), (22, 16), (20, 18), (22, 18)):
        t.fill(x, h(2), z, x, h(3), z, W + "spruce_fence")
    t.fill(20, h(4), 16, 22, h(4), 18, losa("brick"))
    t.set(21, h(4), 17, W + "bricks")
    t.set(21, h(3), 17, st("chain", axis="y", waterlogged="false"))
    t.ombu(4, 20)
    t.terreno(0, 0, SX - 1, SZ - 1)
    t.sendero([(x, 0) for x in range(7, 11)] + [(18, z) for z in range(2, 18)] + [(x, 13) for x in range(19, 24)])
    gente(t, [(3, h(1), 3, "PAISANO", 1), (12, h(1), 7, "PAISANA", 2), (14, h(1), 3, "PAYADOR", 2),
              (22, h(1), 8, "PEON", 2)])
    t.aire_si_vacio(0, FL + 1, 0, SX - 1, 13, SZ - 1)
    return t


# ================================================================ sabana (rancho de adobe y paja)

def sabana():
    SX, SZ = 25, 24
    t = Template(SX, 14, SZ, seed=31)
    ADOBE = t.azar((W + "terracotta", 6), (W + "orange_terracotta", 1))
    X0, X1, Z0, Z1 = 2, 16, 5, 15
    t.cimiento(X0, Z0, X1, Z1)
    # piso de tierra apisonada adentro, tablas detrás del mostrador
    t.fill(X0, FL, Z0, X1, FL, Z1, W + "brown_terracotta")
    t.fill(X0 + 1, FL, Z0 + 1, X1 - 1, FL, Z1 - 1, t.azar((W + "coarse_dirt", 3), (W + "dirt", 1)))
    t.fill(X0 + 1, FL, Z1 - 3, X1 - 1, FL, Z1 - 1, W + "acacia_planks")
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            if x in (X0, X1) or z in (Z0, Z1):
                t.set(x, h(1), z, W + "brown_terracotta")
                for y in range(h(2), h(5)):
                    t.set(x, y, z, ADOBE())
                if x in (X0, X1) and z in (Z0, Z1):
                    t.fill(x, h(1), z, x, h(4), z, st("acacia_log", axis="y"))
    t.fill(X0 + 1, h(1), Z0 + 1, X1 - 1, h(4), Z1 - 1, AIR)
    # vigas y techo de paja
    for z in range(Z0, Z1 + 1, 3):
        t.fill(X0, h(5), z, X1, h(5), z, st("acacia_log", axis="x"))
    techo_paja(t, X0, Z0, X1, Z1, h(5), W + "terracotta")
    t.fill(X0 - 1, h(8), 10, X1 + 1, h(8), 10, losa("acacia"))
    # puerta, ventanas con postigos de acacia y cartel
    t.puerta(9, h(1), Z0, "south", "acacia", "left")
    t.set(9, h(3), Z0, W + "acacia_planks")
    for x in (4, 5, 13, 14):
        t.ventana(x, h(2), Z0, "north", reja=W + "acacia_fence", postigo="acacia", alto=1)
    for z in (8, 12):
        t.ventana(X0, h(2), z, "west", reja=W + "acacia_fence", postigo="acacia", alto=1)
    t.cartel(9, h(4), Z0 - 1, "north", ["", "PULPERÍA", "El Rancho", ""], madera="acacia")
    boliche(t, X0, Z0, X1, Z1, h(5), madera="acacia", reja=W + "acacia_fence")
    # fogón con salida por el techo
    t.set(X1 - 1, h(1), 8, st("campfire", facing="west", lit="true", signal_fire="false", waterlogged="false"))
    t.fill(X1, h(1), 7, X1, h(9), 9, W + "brown_terracotta")
    t.set(X1, h(10), 8, st("campfire", facing="north", lit="true", signal_fire="false", waterlogged="false"))
    t.set(X1 - 1, h(2), 8, W + "brown_terracotta")
    # ramada de hojas al frente
    suelo(t, 0, 0, SX - 1, SZ - 1, t.azar((W + "grass_block[snowy=false]", 6), (W + "coarse_dirt", 2)))
    t.fill(X0, FL, 1, X1, FL, 4, W + "coarse_dirt")
    for x in range(X0, X1 + 1, 4):
        t.fill(x, h(1), 1, x, h(3), 1, W + "acacia_fence")
    t.fill(X0, h(4), 1, X1, h(4), 1, st("acacia_log", axis="x"))
    t.fill(X0, h(4), 2, X1, h(4), 4, st("acacia_leaves", distance="1", persistent="true"))
    for x in (4, 5, 12, 13):
        t.set(x, h(1), 3, escalon("acacia", "south"))
    t.set(8, h(1), 2, W + "acacia_planks")
    t.set(8, h(2), 2, W + "potted_red_tulip")
    t.farol(6, h(3), 3)
    t.farol(12, h(3), 3)
    # horno de barro
    t.fill(19, h(1), 6, 22, h(1), 9, W + "bricks")
    t.fill(19, h(2), 6, 22, h(3), 9, W + "terracotta")
    t.fill(20, h(4), 7, 21, h(4), 8, W + "terracotta")
    t.fill(20, h(2), 7, 21, h(2), 8, AIR)
    t.set(20, h(2), 6, AIR)
    t.set(20, h(2), 7, st("campfire", facing="north", lit="true", signal_fire="false", waterlogged="false"))
    # corral de acacia con animales
    for x in range(17, 24):
        t.set(x, h(1), 12, W + "acacia_fence")
        t.set(x, h(1), 21, W + "acacia_fence")
    for z in range(12, 22):
        t.set(17, h(1), z, W + "acacia_fence")
        t.set(23, h(1), z, W + "acacia_fence")
    t.set(20, h(1), 12, st("acacia_fence_gate", facing="north", in_wall="false", open="false", powered="false"))
    t.set(22, h(1), 20, st("cauldron", level="3"))
    t.set(18, h(1), 20, st("hay_block", axis="y"))
    for x, tipo in ((19, "horse"), (21, "cow"), (20, "cow")):
        t.animal(x, h(1), 16, W + tipo, {"Tame": Byte(1)} if tipo == "horse" else None)
    # palenque
    t.fill(1, h(1), 20, 8, h(1), 20, W + "acacia_fence")
    flores(t, 0, 0, SX - 1, SZ - 1, ["grass", "grass", "dead_bush", "orange_tulip"], 0.12)
    t.sendero([(9, z) for z in range(0, 5)])
    gente(t, [(6, h(1), 2, "PAISANO", 1), (14, h(1), 2, "PEON", 2), (11, h(1), 9, "PAISANA", 2)])
    t.aire_si_vacio(0, FL + 1, 0, SX - 1, 13, SZ - 1)
    return t


# ================================================================ desierto / mesa

def desierto(roja=False):
    SX, SZ = 27, 25
    t = Template(SX, 15, SZ, seed=41 if not roja else 43)
    A = "red_sandstone" if roja else "sandstone"
    LISO, CORT, CINC = W + f"smooth_{A}", W + f"cut_{A}", W + f"chiseled_{A}"
    ARENA = W + ("red_sand" if roja else "sand")
    acento = [W + "orange_terracotta", W + "white_terracotta", W + "red_terracotta"] if roja else \
        [W + "orange_terracotta", W + "yellow_terracotta"]
    X0, X1, Z0, Z1 = 2, 18, 6, 17
    t.cimiento(X0, 2, X1, Z1, W + A)
    suelo(t, 0, 0, SX - 1, SZ - 1, t.azar((ARENA, 8), (W + "coarse_dirt", 1)), sub=W + A)
    t.fill(X0, FL, Z0, X1, FL, Z1, CORT)
    for x in range(X0 + 1, X1):
        for z in range(Z0 + 1, Z1):
            t.set(x, FL, z, LISO if (x + z) % 2 else W + ("red_terracotta" if roja else "orange_terracotta"))
    # muros con zócalo cortado, franja de color y esquinas cinceladas
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            if x in (X0, X1) or z in (Z0, Z1):
                t.set(x, h(1), z, CORT)
                for y in range(h(2), h(6)):
                    t.set(x, y, z, LISO)
                t.set(x, h(5), z, acento[(x + z) % len(acento)])
                if x in (X0, X1) and z in (Z0, Z1):
                    t.fill(x, h(1), z, x, h(7), z, CINC)
    t.fill(X0 + 1, h(1), Z0 + 1, X1 - 1, h(5), Z1 - 1, AIR)
    # azotea con pretil almenado y gárgolas
    t.fill(X0 + 1, h(6), Z0 + 1, X1 - 1, h(6), Z1 - 1, LISO)
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            if x in (X0, X1) or z in (Z0, Z1):
                t.set(x, h(6), z, LISO)
                if (x + z) % 2 == 0 and not (x in (X0, X1) and z in (Z0, Z1)):
                    t.set(x, h(7), z, losa(f"smooth_{A}"))
    for x in (6, 14):
        t.set(x, h(5), Z0 - 1, escalon(A, "south", "top"))
    # cúpula sobre la esquina noreste
    for x in range(X1 - 4, X1):
        for z in range(Z0 + 1, Z0 + 5):
            t.set(x, h(7), z, W + ("red_terracotta" if roja else "white_terracotta"))
    for x in range(X1 - 3, X1 - 1):
        for z in range(Z0 + 2, Z0 + 4):
            t.set(x, h(8), z, W + ("orange_terracotta"))
    t.set(X1 - 2, h(9), Z0 + 3, st("lantern", hanging="false"))
    # portal de arcos al frente
    t.fill(X0, FL, 2, X1, FL, Z0 - 1, CORT)
    for x in range(X0, X1 + 1, 4):
        t.fill(x, h(1), 2, x, h(4), 2, W + f"cut_{A}")
        t.set(x, h(1), 2, CINC)
    for x in range(X0, X1 + 1):
        if (x - X0) % 4 in (1, 3):
            t.set(x, h(4), 2, escalon(A, "east" if (x - X0) % 4 == 1 else "west", "top"))
        elif (x - X0) % 4 == 2:
            t.set(x, h(4), 2, losa(f"smooth_{A}", "top"))
    t.fill(X0, h(5), 2, X1, h(5), Z0 - 1, losa(f"smooth_{A}"))
    t.fill(X0, h(5), 2, X1, h(5), 2, LISO)
    # puertas, ventanas y cartel
    t.puerta(10, h(1), Z0, "south", "jungle", "left")
    t.set(10, h(3), Z0, CINC)
    for x in (5, 6, 14, 15):
        t.ventana(x, h(2), Z0, "north", postigo="jungle")
    for z in (10, 14):
        t.ventana(X0, h(2), z, "west", postigo="jungle")
        t.ventana(X1, h(2), z, "east", postigo="jungle")
    t.cartel(10, h(4), Z0 - 1, "north", ["", "PULPERÍA", "El Oasis" if not roja else "La Quebrada", ""], madera="jungle")
    boliche(t, X0, Z0, X1, Z1, h(6), madera="jungle")
    for x in (4, 5):
        t.set(x, h(1), Z0 + 1, escalon("jungle", "north"))
    # toldo a rayas sobre el lado este y patio con pozo y palmera
    for z in range(Z0, Z0 + 8):
        for k, x in enumerate((X1 + 1, X1 + 2, X1 + 3)):
            t.set(x, h(4) - k // 2, z, W + ("white_wool" if z % 2 else ("red_wool" if roja else "orange_wool")))
    for z in (Z0, Z0 + 7):
        t.fill(X1 + 3, h(1), z, X1 + 3, h(2), z, W + "jungle_fence")
    t.set(X1 + 2, h(1), Z0 + 3, W + "barrel[facing=up,open=false]")
    t.set(X1 + 2, h(1), Z0 + 4, W + "barrel[facing=up,open=false]")
    t.set(X1 + 2, h(2), Z0 + 3, W + "potted_cactus")
    # pozo
    t.box(21, h(1), 17, 23, h(1), 19, CORT)
    for y in (FL - 1, FL, h(1)):
        t.set(22, y, 18, st("water", level="0"))
    for x, z in ((21, 17), (23, 17), (21, 19), (23, 19)):
        t.fill(x, h(2), z, x, h(3), z, W + f"{A}_wall")
    t.fill(21, h(4), 17, 23, h(4), 19, losa(f"smooth_{A}"))
    t.set(22, h(3), 18, st("chain", axis="y", waterlogged="false"))
    # palmera
    for y in range(h(1), h(8)):
        t.set(24 if y < h(4) else 23, y, 22, st("jungle_log", axis="y"))
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1), (2, 0), (-2, 0), (0, 2), (0, -2), (1, 1), (-1, -1),
                   (1, -1), (-1, 1), (3, 0), (-3, 0), (0, 3), (0, -3)):
        yy = h(8) if abs(dx) + abs(dz) <= 1 else h(7)
        t.set(23 + dx, yy, 22 + dz, st("jungle_leaves", distance="1", persistent="true"))
    t.set(23, h(8), 22, st("jungle_leaves", distance="1", persistent="true"))
    t.set(23, h(9), 22, st("jungle_leaves", distance="1", persistent="true"))
    for x, z in ((1, 20), (5, 22), (12, 21), (25, 3), (3, 1)):
        t.set(x, h(1), z, W + "cactus")
        t.set(x, h(2), z, W + "cactus")
    flores(t, 0, 0, SX - 1, SZ - 1, ["dead_bush"], 0.05)
    # palenque con camellos... caballos
    t.fill(1, h(1), 0, 7, h(1), 0, W + "jungle_fence")
    t.animal(4, h(1), 1, W + "horse", {"Tame": Byte(1)})
    gente(t, [(7, h(1), 3, "PAISANO", 2), (13, h(1), 3, "PAYADOR", 2), (21, h(1), 10, "PEON", 2)])
    t.aire_si_vacio(0, FL + 1, 0, SX - 1, 14, SZ - 1)
    return t


# ================================================================ cabaña (taiga / bosque / nevada)

def cabana(estilo="taiga"):
    SX, SZ = 24, 22
    t = Template(SX, 17, SZ, seed={"taiga": 51, "bosque": 53, "nevada": 57}[estilo])
    madera = {"taiga": "spruce", "bosque": "oak", "nevada": "spruce"}[estilo]
    techo = {"taiga": "spruce", "bosque": "dark_oak", "nevada": "spruce"}[estilo]
    X0, X1, Z0, Z1 = 2, 16, 5, 15
    piedra = t.azar((W + "cobblestone", 4), (W + "mossy_cobblestone", 2 if estilo != "nevada" else 0),
                    (W + "stone", 1))
    t.cimiento(X0, Z0, X1, Z1)
    t.fill(X0, FL, Z0, X1, FL, Z1, piedra)
    t.fill(X0 + 1, FL, Z0 + 1, X1 - 1, FL, Z1 - 1, W + f"{madera}_planks")
    # muros de troncos sobre zócalo de piedra
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            if x in (X0, X1) or z in (Z0, Z1):
                t.set(x, h(1), z, piedra())
                eje = "x" if z in (Z0, Z1) else "z"
                for y in range(h(2), h(5)):
                    t.set(x, y, z, st(f"{madera}_log", axis=eje))
                if x in (X0, X1) and z in (Z0, Z1):
                    t.fill(x, h(1), z, x, h(4), z, st(f"stripped_{madera}_log", axis="y"))
    t.fill(X0 + 1, h(1), Z0 + 1, X1 - 1, h(4), Z1 - 1, AIR)
    # techo empinado con hastiales de tablas y ventana redonda
    techo_empinado(t, X0, Z0, X1, Z1, h(5), techo, W + f"{madera}_planks",
                   cumbrera=W + "snow_block" if estilo == "nevada" else None)
    for z in range(Z0 + 1, Z1, 3):
        t.fill(X0 + 1, h(5), z, X1 - 1, h(5), z, st(f"stripped_{madera}_log", axis="x"))
    for x in (X0, X1):
        t.set(x, h(7), 10, W + "glass_pane")
        t.set(x, h(8), 10, W + "glass_pane")
    if estilo == "nevada":
        for x in range(X0 - 1, X1 + 2):
            for z in (Z0 - 1, Z1 + 1):
                t.set(x, h(4), z, W + "snow_block")
    # puerta, ventanas y cartel
    t.puerta(9, h(2), Z0, "south", madera, "left")
    t.puerta(9, h(1), Z0, "south", madera, "left")
    t.set(9, h(3), Z0, st(f"stripped_{madera}_log", axis="x"))
    for x in (5, 13):
        t.ventana(x, h(2), Z0, "north", reja=W + "glass_pane", postigo=madera)
    for z in (8, 12):
        t.ventana(X0, h(2), z, "west", reja=W + "glass_pane", postigo=madera)
    t.cartel(9, h(4), Z0 - 1, "north", ["", "PULPERÍA", {"taiga": "El Abetal", "bosque": "La Arboleda",
                                                            "nevada": "La Helada"}[estilo], ""], madera=madera)
    boliche(t, X0, Z0, X1, Z1, h(5), madera=madera, reja=W + f"{madera}_fence")
    # salamandra y chimenea de piedra afuera (hastial este)
    t.set(X1 - 1, h(1), 9, st("furnace", facing="west", lit="true"))
    t.set(X1 - 1, h(1), 10, st("blast_furnace", facing="west", lit="true"))
    t.fill(X1 + 1, h(1), 8, X1 + 2, h(12), 10, W + "cobblestone")
    t.fill(X1 + 1, h(13), 9, X1 + 1, h(13), 9, st("campfire", facing="north", lit="true", signal_fire="false", waterlogged="false"))
    t.set(X1, h(2), 9, W + "cobblestone")
    t.set(X1, h(2), 10, W + "cobblestone")
    # porche con baranda
    t.fill(X0, h(1) - 1, 1, X1, FL, 4, W + f"{madera}_planks")
    for x in range(X0, X1 + 1):
        if x not in (8, 9, 10):
            t.set(x, h(1), 1, W + f"{madera}_fence")
    for x in (X0, X1, 6, 12):
        t.fill(x, h(1), 1, x, h(3), 1, W + f"{madera}_fence")
        t.set(x, h(1), 1, st(f"stripped_{madera}_log", axis="y"))
    t.fill(X0 - 1, h(4), 1, X1 + 1, h(4), 2, escalon(techo, "south"))
    t.fill(X0 - 1, h(4), 3, X1 + 1, h(4), 4, W + f"{techo}_planks")
    for x in (4, 5, 13, 14):
        t.set(x, h(1), 4, escalon(madera, "south"))
    t.farol(7, h(3), 3)
    t.farol(11, h(3), 3)
    # leñera
    for z in range(5, 11):
        t.fill(19, h(1), z, 21, h(2), z, st(f"{madera}_log", axis="x") if z % 3 else st(f"stripped_{madera}_log", axis="x"))
    t.fill(18, h(3), 4, 22, h(3), 11, losa(techo))
    for x, z in ((18, 4), (22, 4), (18, 11), (22, 11)):
        t.fill(x, h(1), z, x, h(2), z, W + f"{madera}_fence")
    # suelo según el estilo
    if estilo == "nevada":
        suelo(t, 0, 0, SX - 1, SZ - 1, t.azar((W + "snow_block", 2), (W + "grass_block[snowy=true]", 5)))
        for x in range(SX):
            for z in range(SZ):
                b = t.get(x, FL, z) or ""
                if ("grass_block" in b or "snow_block" in b) and t.get(x, FL + 1, z) in (None, AIR):
                    t.set(x, FL + 1, z, st("snow", layers=str(t.r.choice([1, 1, 2]))))
        for x, z in ((21, 17), (3, 19)):
            for y in range(h(1), h(7)):
                t.set(x, y, z, st("spruce_log", axis="y"))
            for y, r in ((h(3), 2), (h(4), 2), (h(5), 1), (h(6), 1), (h(7), 0)):
                for dx in range(-r, r + 1):
                    for dz in range(-r, r + 1):
                        if abs(dx) + abs(dz) <= r + 1 and t.get(x + dx, y, z + dz) in (None, AIR):
                            t.set(x + dx, y, z + dz, st("spruce_leaves", distance="1", persistent="true"))
        t.farol(9, h(3), 3, alma=False)
        gente(t, [(5, h(1), 3, "PAISANO", 2), (12, h(1), 9, "PEON", 3)])
    elif estilo == "bosque":
        suelo(t, 0, 0, SX - 1, SZ - 1, t.azar((W + "grass_block[snowy=false]", 7), (W + "podzol[snowy=false]", 2),
                                             (W + "coarse_dirt", 1)))
        flores(t, 0, 0, SX - 1, SZ - 1, ["grass", "fern", "brown_mushroom", "red_mushroom", "lily_of_the_valley"], 0.2)
        gente(t, [(5, h(1), 3, "PAISANO", 1), (12, h(1), 9, "PAISANA", 2), (14, h(1), 2, "PAYADOR", 2)])
    else:
        suelo(t, 0, 0, SX - 1, SZ - 1, t.azar((W + "podzol[snowy=false]", 4), (W + "grass_block[snowy=false]", 4),
                                             (W + "coarse_dirt", 1)))
        flores(t, 0, 0, SX - 1, SZ - 1, ["fern", "fern", "grass", "sweet_berry_bush[age=3]"], 0.12)
        gente(t, [(5, h(1), 3, "PEON", 2), (12, h(1), 9, "PAISANO", 2)])
    t.sendero([(x, 0) for x in range(8, 11)], bloque=W + "grass_path" if estilo != "nevada" else W + "packed_ice")
    t.aire_si_vacio(0, FL + 1, 0, SX - 1, 16, SZ - 1)
    return t


# ================================================================ palafito (pantano / selva)

def palafito(estilo="pantano"):
    SX, SZ = 23, 23
    t = Template(SX, 16, SZ, seed=61 if estilo == "pantano" else 67)
    madera = "dark_oak" if estilo == "pantano" else "jungle"
    P = 2                                         # el piso del palafito está 2 bloques arriba del suelo
    Y = FL + P
    X0, X1, Z0, Z1 = 3, 17, 7, 17
    # suelo barroso con charcos y camalotes
    base = t.azar((W + "grass_block[snowy=false]", 5), (W + "coarse_dirt", 2), (W + "podzol[snowy=false]", 2))
    suelo(t, 0, 0, SX - 1, SZ - 1, base)
    for cx, cz, r in ((2, 3, 2), (19, 4, 2), (20, 19, 2), (1, 19, 1)):
        for x in range(cx - r, cx + r + 1):
            for z in range(cz - r, cz + r + 1):
                if 0 <= x < SX and 0 <= z < SZ and (x - cx) ** 2 + (z - cz) ** 2 <= r * r + 1:
                    t.set(x, FL, z, st("water", level="0"))
                    t.set(x, FL - 1, z, W + "dirt")
                    if t.r.random() < 0.4:
                        t.set(x, FL + 1, z, W + "lily_pad")
    # pilotes
    for x in range(X0 - 2, X1 + 3, 4):
        for z in list(range(Z0 - 5, Z1 + 1, 4)) + [Z1]:
            if 0 <= x < SX:
                t.fill(x, 0, z, x, Y, z, st(f"{madera}_log", axis="y"))
    # plataforma y deck delantero
    t.fill(X0 - 2, Y, Z0 - 5, X1 + 2, Y, Z1 + 1, W + f"{madera}_planks")
    for x in range(X0 - 2, X1 + 3):
        for z in range(Z0 - 5, Z1 + 2):
            if x in (X0 - 2, X1 + 2) or z in (Z0 - 5, Z1 + 1):
                t.set(x, Y + 1, z, W + f"{madera}_fence")
    for x in (9, 10, 11):
        t.set(x, Y + 1, Z0 - 5, AIR)
    # escalera hasta el suelo
    for k in range(P):
        for x in (9, 10, 11):
            t.set(x, Y - k, Z0 - 6 - k, escalon(madera, "south"))
            t.fill(x, FL - 1, Z0 - 6 - k, x, Y - k - 1, Z0 - 6 - k, W + f"{madera}_planks")
            for yy in range(0, Y - k):
                t.set(x, yy, Z0 - 6 - k, W + f"{madera}_planks") if yy >= FL - 1 and False else None
    # casa
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            if x in (X0, X1) or z in (Z0, Z1):
                for y in range(Y + 1, Y + 5):
                    t.set(x, y, z, W + f"{madera}_planks" if (x + y) % 5 else W + f"stripped_{madera}_log[axis=y]")
                if x in (X0, X1) and z in (Z0, Z1):
                    t.fill(x, Y + 1, z, x, Y + 4, z, st(f"stripped_{madera}_log", axis="y"))
    t.fill(X0 + 1, Y + 1, Z0 + 1, X1 - 1, Y + 4, Z1 - 1, AIR)
    for z in range(Z0, Z1 + 1, 3):
        t.fill(X0, Y + 5, z, X1, Y + 5, z, st(f"{madera}_log", axis="x"))
    if estilo == "pantano":
        techo_paja(t, X0, Z0, X1, Z1, Y + 5, W + f"{madera}_planks")
    else:
        techo_paja(t, X0, Z0, X1, Z1, Y + 5, W + f"{madera}_planks",
                   paja=st("jungle_leaves", distance="1", persistent="true"))
        t.fill(X0 - 1, Y + 8, 12, X1 + 1, Y + 8, 12, st("jungle_log", axis="x"))
    t.puerta(10, Y + 1, Z0, "south", madera, "left")
    for x in (6, 14):
        t.ventana(x, Y + 2, Z0, "north", reja=W + f"{madera}_fence", postigo=madera, alto=1)
    for z in (10, 14):
        t.ventana(X0, Y + 2, z, "west", reja=W + f"{madera}_fence", postigo=madera, alto=1)
        t.ventana(X1, Y + 2, z, "east", reja=W + f"{madera}_fence", postigo=madera, alto=1)
    t.cartel(10, Y + 4, Z0 - 1, "north", ["", "PULPERÍA", "El Bañado" if estilo == "pantano" else "El Monte", ""],
             madera=madera)
    # el interior se arma con el mismo "boliche", corrido a la altura del palafito
    sub = Template(SX, 16, SZ, seed=5)
    boliche(sub, X0, Z0, X1, Z1, FL + 5, madera=madera, reja=W + f"{madera}_fence")
    for (x, y, z), v in sub.blocks.items():
        t.blocks[(x, y + P, z)] = v
    for x, y, z, nbt in sub.entities:
        t.entities.append((x, y + P, z, nbt))
    t.set(X1 - 1, Y + 1, 10, st("campfire", facing="west", lit="false", signal_fire="false", waterlogged="false"))
    # muelle y canoa
    for x, z in ((6, Z0 - 3), (14, Z0 - 3)):
        t.set(x, Y + 1, z, escalon(madera, "south"))
        t.set(x + 1, Y + 1, z, escalon(madera, "south"))
    for x in (X0 - 2, X1 + 2):
        t.set(x, Y + 2, Z0 - 5, st("lantern", hanging="false"))
    if estilo == "selva":
        for x, z in ((1, 12), (21, 12)):
            t.set(x, FL + 1, z, W + "melon")
        flores(t, 0, 0, SX - 1, SZ - 1, ["fern", "grass"], 0.25)
    else:
        flores(t, 0, 0, SX - 1, SZ - 1, ["blue_orchid", "grass", "fern"], 0.2)
    gente(t, [(7, Y + 1, Z0 - 2, "PAISANO", 1), (13, Y + 1, Z0 - 3, "PAISANA", 2), (8, Y + 1, 10, "PAYADOR", 2)])
    t.aire_si_vacio(0, FL + 1, 0, SX - 1, 15, SZ - 1)
    return t


# ================================================================ serrana (pirca y lajas)

def serrana():
    SX, SZ = 26, 24
    t = Template(SX, 15, SZ, seed=71)
    PIRCA = t.azar((W + "cobblestone", 5), (W + "mossy_cobblestone", 2), (W + "stone", 2), (W + "andesite", 2))
    X0, X1, Z0, Z1 = 2, 16, 5, 15
    suelo(t, 0, 0, SX - 1, SZ - 1, t.azar((W + "grass_block[snowy=false]", 5), (W + "stone", 1), (W + "gravel", 1),
                                         (W + "coarse_dirt", 1)), sub=W + "stone")
    t.cimiento(X0, Z0, X1, Z1)
    t.fill(X0, FL, Z0, X1, FL, Z1, W + "stone_bricks")
    t.fill(X0 + 1, FL, Z0 + 1, X1 - 1, FL, Z1 - 1, t.azar((W + "polished_andesite", 3), (W + "spruce_planks", 2)))
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            if x in (X0, X1) or z in (Z0, Z1):
                for y in range(h(1), h(5)):
                    t.set(x, y, z, PIRCA())
                if x in (X0, X1) and z in (Z0, Z1):
                    t.fill(x, h(1), z, x, h(4), z, W + "stone_bricks")
    t.fill(X0 + 1, h(1), Z0 + 1, X1 - 1, h(4), Z1 - 1, AIR)
    t.techo_dos_aguas(X0, Z0, X1, Z1, h(5), eje="x", teja=W + "stone_brick", hastial=W + "cobblestone")
    for z in range(Z0 + 1, Z1, 3):
        t.fill(X0 + 1, h(4), z, X1 - 1, h(4), z, st("stripped_spruce_log", axis="x"))
    t.puerta(9, h(1), Z0, "south", "spruce", "left")
    t.set(9, h(3), Z0, st("spruce_log", axis="x"))
    t.set(8, h(3), Z0, st("spruce_log", axis="x"))
    t.set(10, h(3), Z0, st("spruce_log", axis="x"))
    for x in (5, 13):
        t.ventana(x, h(2), Z0, "north", postigo="spruce", alto=1, dintel=st("spruce_log", axis="x"))
    for z in (9, 12):
        t.ventana(X0, h(2), z, "west", postigo="spruce", alto=1)
    t.cartel(9, h(4), Z0 - 1, "north", ["", "PULPERÍA", "La Cuesta", ""], madera="spruce")
    boliche(t, X0, Z0, X1, Z1, h(4), madera="spruce", mesas_extra=False)
    # hogar de piedra
    t.set(X1 - 1, h(1), 10, st("campfire", facing="west", lit="true", signal_fire="false", waterlogged="false"))
    t.set(X1 - 1, h(1), 9, W + "stone_bricks")
    t.set(X1 - 1, h(1), 11, W + "stone_bricks")
    t.fill(X1 - 1, h(2), 9, X1 - 1, h(3), 11, W + "stone_bricks")
    t.fill(X1, h(5), 10, X1, h(10), 10, W + "cobblestone")
    t.set(X1, h(11), 10, st("campfire", facing="north", lit="true", signal_fire="false", waterlogged="false"))
    # alero de lajas al frente con bancos de piedra
    t.fill(X0, FL, 2, X1, FL, 4, W + "gravel")
    for x in (X0, 6, 12, X1):
        t.fill(x, h(1), 2, x, h(3), 2, W + "spruce_fence")
    t.fill(X0 - 1, h(4), 2, X1 + 1, h(4), 4, losa("stone_brick"))
    for x in (4, 5, 13, 14):
        t.set(x, h(1), 4, escalon("stone_brick", "south"))
    t.farol(9, h(3), 3)
    # corral de pirca con llamas
    for x in range(18, 25):
        t.set(x, h(1), 4, W + "cobblestone_wall")
        t.set(x, h(1), 16, W + "cobblestone_wall")
    for z in range(4, 17):
        t.set(18, h(1), z, W + "cobblestone_wall")
        t.set(24, h(1), z, W + "cobblestone_wall")
    t.set(18, h(1), 10, st("spruce_fence_gate", facing="east", in_wall="true", open="false", powered="false"))
    for z in (7, 11, 14):
        t.animal(21, h(1), z, W + "llama", {"Tame": Byte(1)})
    t.set(23, h(1), 15, st("hay_block", axis="y"))
    # apacheta (montículo de piedras de los caminantes)
    t.fill(3, h(1), 19, 5, h(1), 21, W + "cobblestone")
    t.fill(4, h(2), 20, 4, h(3), 20, W + "mossy_cobblestone")
    t.set(4, h(2), 19, W + "cobblestone_wall")
    flores(t, 0, 0, SX - 1, SZ - 1, ["grass", "poppy", "oxeye_daisy", "cornflower"], 0.12)
    t.sendero([(9, z) for z in range(0, 5)], bloque=W + "gravel")
    gente(t, [(6, h(1), 3, "PEON", 2), (12, h(1), 9, "PAISANO", 2), (20, h(1), 2, "PAISANA", 2)])
    t.aire_si_vacio(0, FL + 1, 0, SX - 1, 14, SZ - 1)
    return t


ESTILOS = {
    "llanura": llanura,
    "sabana": sabana,
    "desierto": lambda: desierto(False),
    "mesa": lambda: desierto(True),
    "taiga": lambda: cabana("taiga"),
    "bosque": lambda: cabana("bosque"),
    "nevada": lambda: cabana("nevada"),
    "pantano": lambda: palafito("pantano"),
    "selva": lambda: palafito("selva"),
    "serrana": serrana,
}
