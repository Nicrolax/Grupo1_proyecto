"""
El Paso de las Ánimas (capítulo 7, La Salamanca). Terreno de 34 x 34, frente (puerta) al norte, sin rotar.

  paso_animas.nbt  DE DÍA (la que genera el mundo): una tapera en ruinas, el ombú, las cruces de los
                   caídos en la batalla, lanzas oxidadas clavadas y la cruz del Negro Ledesma con su cartel.
  salamanca.nbt    DE NOCHE (la pone el juego cuando llegan el jugador y el Vizcacha): la pulpería rearmada,
                   muros enteros, techo, velas de alma, el mostrador y la mesa de truco con cuatro sillas,
                   y fuego de alma sobre las tumbas.

Las dos tienen el mismo tamaño y el mismo afuera (mismo azar), así cambiar una por la otra solo cambia lo
que tiene que cambiar. Las coordenadas que usa el juego están en progreso/SalamancaSitio.java:
  mesa (17, 3, 17) · sillas N (17,3,16) S (17,3,18) O (16,3,17) E (18,3,17) · puerta (17, 3, 12)
  llegada (17, 3, 3) · cruz de Ledesma (28, 3, 17)
"""
import random
from estructuras_base import *

SX, SY, SZ = 34, 24, 34
X0, Z0, X1, Z1 = 11, 12, 23, 22          # muros de la tapera
MESA = (17, 17)
CRUZ_LEDESMA = (28, 17)
CRUCES = [(27, 12), (30, 14), (31, 20), (27, 23), (30, 26), (25, 28), (21, 30), (8, 27), (5, 22)]
LANZAS = [(24, 25), (19, 28), (31, 8), (13, 29), (9, 24), (3, 17), (25, 6)]


def sitio(t):
    """Todo lo de afuera: igual de día y de noche (mismo azar, mismo orden)."""
    t.cimiento(0, 0, SX - 1, SZ - 1)
    # el piso de la tapera se reserva primero (cada versión lo cambia después)
    t.fill(X0, FL, Z0, X1, FL, Z1, "minecraft:dirt")
    # sendero que viene del norte hasta la puerta
    for z in range(0, Z0):
        for x in (16, 17, 18):
            if x == 17 or t.r.random() < 0.55:
                t.set(x, FL, z, "minecraft:grass_path")
    # las tumbas (montículos de tierra) delante de cada cruz
    for x, z in CRUCES + [CRUZ_LEDESMA]:
        t.set(x, FL, z + 1, "minecraft:coarse_dirt")
        t.set(x, FL, z + 2, "minecraft:coarse_dirt")
    t.terreno(0, 0, SX - 1, SZ - 1)
    # nada crece sobre las tumbas ni donde van las flores de Ledesma
    cx, cz = CRUZ_LEDESMA
    for x in range(cx - 1, cx + 2):
        for z in range(cz - 1, cz + 3):
            if (x, FL + 1, z) in t.blocks:
                del t.blocks[(x, FL + 1, z)]
    for x, z in CRUCES + [CRUZ_LEDESMA]:
        for dz in (1, 2):
            t.blocks.pop((x, FL + 1, z + dz), None)
    t.ombu(5, 6)
    # cruces de palo de los caídos
    for x, z in CRUCES:
        for y in range(FL + 1, FL + 4):
            t.set(x, y, z, "minecraft:spruce_fence")
        t.set(x - 1, FL + 3, z, "minecraft:spruce_fence")
        t.set(x + 1, FL + 3, z, "minecraft:spruce_fence")
    # la cruz del Negro Ledesma: más alta, de madera oscura, con su cartel
    for y in range(FL + 1, FL + 5):
        t.set(cx, y, cz, "minecraft:dark_oak_fence")
    t.set(cx - 1, FL + 4, cz, "minecraft:dark_oak_fence")
    t.set(cx + 1, FL + 4, cz, "minecraft:dark_oak_fence")
    lineas = ["Aquí cayó", "el Negro Ledesma", "Paso de las Ánimas", "invierno del 40"]
    tx = [txt(l) for l in lineas]
    t.set(cx, FL + 1, cz - 1, "minecraft:dark_oak_sign[rotation=8,waterlogged=false]", Compound({
        "id": String("minecraft:sign"), "Text1": tx[0], "Text2": tx[1], "Text3": tx[2], "Text4": tx[3],
        "Color": String("black")}))
    # lanzas oxidadas clavadas en el barro (algunas con un jirón de divisa)
    for i, (x, z) in enumerate(LANZAS):
        alto = 2 + (i % 2)
        for y in range(FL + 1, FL + 1 + alto):
            t.set(x, y, z, "minecraft:iron_bars")
        t.set(x, FL + 1 + alto, z, "minecraft:chain[axis=y,waterlogged=false]")
        if i % 3 == 0:
            t.set(x + 1, FL + alto, z, "minecraft:red_wall_banner[facing=east]")


def ruina():
    t = Template(SX, SY, SZ, seed=1840)
    sitio(t)
    r = random.Random(4041)
    piso = lambda: r.choice(["minecraft:coarse_dirt", "minecraft:coarse_dirt", "minecraft:podzol",
                             "minecraft:cobblestone", "minecraft:mossy_cobblestone", "minecraft:gravel"])
    for x in range(X0 + 1, X1):
        for z in range(Z0 + 1, Z1):
            t.set(x, FL, z, piso())
    muro = lambda: r.choice(["minecraft:mossy_cobblestone", "minecraft:cobblestone", "minecraft:cracked_stone_bricks",
                             "minecraft:white_terracotta", "minecraft:mossy_cobblestone", "minecraft:andesite"])
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            if x not in (X0, X1) and z not in (Z0, Z1):
                continue
            t.set(x, FL, z, "minecraft:cobblestone")
            esquina = x in (X0, X1) and z in (Z0, Z1)
            if x == 17 and z == Z0:
                continue                                    # el hueco de la puerta
            alto = 4 if esquina else max(0, min(4, int(r.gauss(1.6, 1.3))))
            for y in range(FL + 1, FL + 1 + alto):
                t.set(x, y, z, muro())
    # adentro: la mesa volteada, palos quemados, telarañas y yuyos
    t.set(16, FL + 1, 18, "minecraft:dark_oak_slab[type=bottom,waterlogged=false]")
    t.set(18, FL + 1, 16, "minecraft:oak_fence")
    t.set(14, FL + 1, 14, "minecraft:stripped_dark_oak_log[axis=x]")
    t.set(15, FL + 1, 14, "minecraft:stripped_dark_oak_log[axis=x]")
    t.set(20, FL + 1, 20, "minecraft:stripped_dark_oak_log[axis=z]")
    for x, z in ((12, 13), (22, 13), (12, 21), (22, 21)):
        t.set(x, FL + 5, z, "minecraft:cobweb")
    for x, z in ((13, 19), (21, 15), (19, 20), (15, 16)):
        t.set(x, FL + 1, z, "minecraft:dead_bush")
    t.aire_si_vacio(0, FL + 1, 0, SX - 1, SY - 1, SZ - 1)
    return t


def salamanca():
    t = Template(SX, SY, SZ, seed=1840)
    sitio(t)
    r = random.Random(666)
    # piso de tablas oscuras con guarda de piedra negra y un paño rojo bajo la mesa
    for x in range(X0 + 1, X1):
        for z in range(Z0 + 1, Z1):
            borde = x in (X0 + 1, X1 - 1) or z in (Z0 + 1, Z1 - 1)
            t.set(x, FL, z, "minecraft:polished_blackstone" if borde else "minecraft:dark_oak_planks")
    mx, mz = MESA
    for x in range(mx - 2, mx + 3):
        for z in range(mz - 2, mz + 3):
            t.set(x, FL, z, "minecraft:red_wool" if abs(x - mx) < 2 and abs(z - mz) < 2 else "minecraft:red_terracotta")
    # muros enteros: zócalo de piedra negra, revoque blanco, esquineros de madera
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            if x not in (X0, X1) and z not in (Z0, Z1):
                continue
            t.set(x, FL, z, "minecraft:polished_blackstone_bricks")
            esquina = x in (X0, X1) and z in (Z0, Z1)
            for y in range(FL + 1, FL + 5):
                if esquina:
                    b = "minecraft:stripped_dark_oak_log[axis=y]"
                elif y == FL + 1:
                    b = "minecraft:polished_blackstone_bricks"
                else:
                    b = "minecraft:smooth_quartz"
                t.set(x, y, z, b)
    # puerta (abierta: la Salamanca espera) y ventanas con reja
    t.set(17, FL + 1, Z0, "minecraft:dark_oak_door[facing=south,half=lower,hinge=left,open=true,powered=false]")
    t.set(17, FL + 2, Z0, "minecraft:dark_oak_door[facing=south,half=upper,hinge=left,open=true,powered=false]")
    for x, z in ((14, Z0), (20, Z0), (14, Z1), (20, Z1), (X0, 15), (X0, 19), (X1, 15), (X1, 19)):
        t.set(x, FL + 2, z, "minecraft:iron_bars")
        t.set(x, FL + 3, z, "minecraft:iron_bars")
    # techo de madera oscura a dos aguas
    t.techo_dos_aguas(X0, Z0, X1, Z1, FL + 5, eje="x", teja="minecraft:dark_oak", hastial="minecraft:smooth_quartz")
    for x in range(X0 + 1, X1):
        t.set(x, FL + 5, 17, "minecraft:stripped_dark_oak_log[axis=x]")      # la viga madre
    # la mesa de truco con cuatro sillas
    t.mesa_con_sillas(mx, FL + 1, mz, cuatro=True)
    # el mostrador del fondo, con botellas, calaveras y yuyos del infierno
    for x in range(13, 22):
        t.set(x, FL + 1, 20, "minecraft:dark_oak_stairs[facing=north,half=top,shape=straight,waterlogged=false]")
    for x in range(12, 23):
        if x in (12, 22):
            t.set(x, FL + 1, 21, "minecraft:barrel[facing=up,open=false]")
        else:
            t.set(x, FL + 1, 21, "minecraft:dark_oak_planks")
            deco = r.choice(["minecraft:brewing_stand[has_bottle_0=true,has_bottle_1=true,has_bottle_2=false]",
                             "minecraft:potted_crimson_roots", "minecraft:skeleton_skull[rotation=8]",
                             "minecraft:potted_wither_rose", "minecraft:brewing_stand[has_bottle_0=true,has_bottle_1=false,has_bottle_2=true]"])
            t.set(x, FL + 2, 21, deco)
    for x in (14, 17, 20):
        t.set(x, FL + 2, 20, "minecraft:soul_lantern[hanging=false,waterlogged=false]")
    # el fogón del rincón, con fuego de alma
    t.set(13, FL + 1, 14, "minecraft:soul_campfire[facing=south,lit=true,signal_fire=false,waterlogged=false]")
    t.set(21, FL + 1, 14, "minecraft:soul_campfire[facing=south,lit=true,signal_fire=false,waterlogged=false]")
    # antorchas de alma en las paredes
    for x in (13, 16, 18, 21):
        t.set(x, FL + 3, Z0 + 1, "minecraft:soul_wall_torch[facing=south]")
    for z in (14, 17, 20):
        t.set(X0 + 1, FL + 3, z, "minecraft:soul_wall_torch[facing=east]")
        t.set(X1 - 1, FL + 3, z, "minecraft:soul_wall_torch[facing=west]")
    # telarañas en los rincones altos
    for x, z in ((12, 13), (22, 13), (12, 19), (22, 19)):
        t.set(x, FL + 4, z, "minecraft:cobweb")
    # afuera: fuego de alma sobre las tumbas y faroles de alma en las cruces
    for x, z in CRUCES:
        t.set(x, FL, z + 1, "minecraft:soul_soil")
        t.set(x, FL + 1, z + 1, "minecraft:soul_fire")
    t.set(16, FL + 3, Z0 - 1, "minecraft:soul_lantern[hanging=false,waterlogged=false]")
    t.set(16, FL + 1, Z0 - 1, "minecraft:dark_oak_fence")
    t.set(16, FL + 2, Z0 - 1, "minecraft:dark_oak_fence")
    t.set(18, FL + 1, Z0 - 1, "minecraft:dark_oak_fence")
    t.set(18, FL + 2, Z0 - 1, "minecraft:dark_oak_fence")
    t.set(18, FL + 3, Z0 - 1, "minecraft:soul_lantern[hanging=false,waterlogged=false]")
    t.aire_si_vacio(0, FL + 1, 0, SX - 1, SY - 1, SZ - 1)
    return t
