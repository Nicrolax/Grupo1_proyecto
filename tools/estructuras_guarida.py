"""Guarida clandestina (bar subterráneo)."""
from estructuras_base import *


# ================================================================ Guarida clandestina

def guarida():
    """
    Bar clandestino bajo tierra. La superficie (suelo) está en y = S; en ella, una tapera en ruinas
    con un aljibe: la trampilla del aljibe esconde la escalera que baja al bar.
    Bajo tierra: salón x 1..17, z 1..21 (barra al fondo, tablado del payador, 4 mesas clandestinas),
    antesala x 19..25 z 1..6 (pie de la escalera, con un guardia) y reservado x 19..25 z 8..21.
    """
    S = 20
    SX, SY, SZ = 27, S + 6, 23
    t = Template(SX, SY, SZ, seed=11)
    piedra = t.azar(("minecraft:stone_bricks", 10), ("minecraft:mossy_stone_bricks", 2),
                    ("minecraft:cracked_stone_bricks", 2))
    negra = t.azar(("minecraft:polished_blackstone_bricks", 8), ("minecraft:cracked_polished_blackstone_bricks", 2))

    # cáscara: piso (y1), muros y techo (y8), con relleno de piedra en y0
    t.fill(0, 0, 0, SX - 1, 0, SZ - 1, "minecraft:stone")
    t.fill(0, 1, 0, SX - 1, 1, SZ - 1, negra)
    t.box(0, 2, 0, SX - 1, 7, SZ - 1, piedra)
    t.fill(0, 8, 0, SX - 1, 8, SZ - 1, piedra)
    t.fill(18, 2, 0, 18, 7, SZ - 1, piedra)                              # tabique este del salón
    t.fill(19, 2, 7, 25, 7, 7, piedra)                                   # antesala | reservado
    t.fill(19, 2, 7, 25, 6, 7, piedra)

    # ---------- salón: piso de roble oscuro con guarda de piedra negra
    t.fill(1, 1, 1, 17, 1, 21, "minecraft:dark_oak_planks")
    for x in range(1, 18):
        for z in range(1, 22):
            if x in (1, 17) or z in (1, 21):
                t.set(x, 1, z, "minecraft:polished_blackstone")
    # boiserie (madera abajo) y tapices rojos en las paredes
    for x in range(1, 18):
        for z in (0, 22):
            t.set(x, 2, z, "minecraft:dark_oak_planks")
    for z in range(1, 22):
        t.set(0, 2, z, "minecraft:dark_oak_planks")
        t.set(18, 2, z, "minecraft:dark_oak_planks")
    for z in (4, 5, 9, 10, 14, 15):
        t.fill(0, 4, z, 0, 5, z, "minecraft:red_wool")
    # bóveda: arcos de escalones invertidos y vigas
    for z in range(1, 22):
        t.set(1, 7, z, "minecraft:stone_brick_stairs[facing=west,half=top,shape=straight,waterlogged=false]")
        t.set(17, 7, z, "minecraft:stone_brick_stairs[facing=east,half=top,shape=straight,waterlogged=false]")
    for z in (4, 9, 14, 19):
        t.fill(2, 7, z, 16, 7, z, "minecraft:dark_oak_log[axis=x]")
    # columnas
    for x, z in ((6, 9), (12, 9), (6, 14), (12, 14)):
        t.set(x, 2, z, "minecraft:chiseled_polished_blackstone")
        t.fill(x, 3, z, x, 6, z, "minecraft:polished_blackstone")
        t.set(x, 7, z, "minecraft:chiseled_polished_blackstone")

    # ---------- la barra (fondo, z = 18) y la estantería de bebidas (z = 21)
    for x in range(3, 13):
        t.set(x, 2, 18, "minecraft:dark_oak_stairs[facing=south,half=top,shape=straight,waterlogged=false]")
    t.set(2, 2, 18, "minecraft:dark_oak_planks")
    t.set(13, 2, 18, "minecraft:dark_oak_planks")
    for z in (19, 20):
        t.set(13, 2, z, "minecraft:dark_oak_fence_gate[facing=east,in_wall=false,open=false,powered=false]")
    for x in range(2, 14):
        t.set(x, 2, 21, "minecraft:barrel[facing=north,open=false]")
        t.set(x, 3, 21, "minecraft:bookshelf" if x % 2 == 0 else "minecraft:barrel[facing=north,open=false]")
        t.set(x, 4, 21, "minecraft:dark_oak_slab[type=top,waterlogged=false]")
        if x % 2:
            t.set(x, 5, 21, "minecraft:brewing_stand[has_bottle_0=true,has_bottle_1=true,has_bottle_2=true]")
    t.set(7, 5, 21, "minecraft:gilded_blackstone")
    t.set(8, 5, 21, "minecraft:gilded_blackstone")
    t.set(3, 2, 20, "minecraft:cauldron[level=3]")
    t.set(11, 2, 20, "minecraft:jukebox[has_record=true]", Compound({
        "id": String("minecraft:jukebox"),
        "RecordItem": Compound({"id": String("trucoya:disco_candombe"), "Count": Byte(1)})}))
    for x in range(4, 13, 2):                                              # banquetas
        t.set(x, 2, 17, "minecraft:dark_oak_fence")
        t.set(x, 3, 17, "minecraft:red_carpet")
    for x in (5, 8, 11):
        t.farol(x, 6, 19, alma=False, cadena=1)

    # ---------- tablado del payador (esquina noroeste)
    t.fill(1, 2, 1, 4, 2, 4, "minecraft:dark_oak_slab[type=bottom,waterlogged=false]")
    t.fill(0, 3, 1, 0, 6, 4, "minecraft:red_wool")
    t.fill(1, 3, 0, 4, 6, 0, "minecraft:red_wool")
    t.farol(3, 6, 3, alma=True)

    # ---------- mesas clandestinas
    t.mesa_con_sillas(9, 2, 5, cuatro=True, clandestina=True)
    t.mesa_con_sillas(14, 2, 5, clandestina=True)
    t.mesa_con_sillas(9, 2, 12, cuatro=True, clandestina=True)
    t.mesa_con_sillas(3, 2, 12, eje="z", clandestina=True)
    for x, z in ((9, 5), (14, 5), (9, 12), (3, 12)):
        t.farol(x, 5, z, alma=(x == 3), cadena=2)

    # ---------- antesala: pie de la escalera y puerta al salón
    t.fill(19, 1, 1, 25, 1, 6, "minecraft:polished_blackstone_bricks")
    t.puerta(18, 2, 3, "west", "dark_oak", "left")
    t.set(18, 4, 3, "minecraft:chiseled_polished_blackstone")
    t.set(19, 4, 2, "minecraft:wall_torch[facing=east]")
    t.cartel(19, 3, 4, "east", ["Sin Ficha de", "Clandestina", "no se pasa.", "-La Casa-"], madera="dark_oak")
    t.set(24, 2, 1, "minecraft:barrel[facing=up,open=false]")
    t.set(25, 2, 1, "minecraft:barrel[facing=up,open=false]")

    # ---------- reservado: la mesa de los que apuestan fuerte
    t.fill(19, 1, 8, 25, 1, 21, "minecraft:dark_oak_planks")
    for x in range(20, 25):
        for z in range(10, 20):
            t.set(x, 2, z, "minecraft:red_carpet" if (x + z) % 2 else "minecraft:black_carpet")
    t.puerta(18, 2, 16, "east", "dark_oak", "left")
    t.ventana(18, 3, 12, "west", alto=2)                                 # mirilla con reja
    t.mesa_con_sillas(22, 2, 14, cuatro=True, clandestina=True)
    for z in (13, 14, 15):
        for x in (21, 22, 23):
            if t.get(x, 2, z) and "carpet" in t.get(x, 2, z):
                t.set(x, 2, z, AIR)
    t.mesa_con_sillas(22, 2, 14, cuatro=True, clandestina=True)
    t.farol(22, 5, 14, cadena=2)
    t.set(25, 2, 20, "minecraft:gold_block")
    t.cofre(25, 2, 19, "west", "trucoya:chests/guarida")
    t.cofre(19, 2, 20, "east", "trucoya:chests/guarida")
    for z in (9, 21):
        t.set(25, 4, z, "minecraft:wall_torch[facing=west]")
    for z in (10, 13, 16, 19):
        t.fill(26, 3, z, 26, 5, z, "minecraft:red_wool")

    # ---------- pozo con escalera desde la antesala hasta la superficie (23, y, 3)
    for y in range(8, S):
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                if dx or dz:
                    t.set(23 + dx, y, 3 + dz, piedra() if y < S - 1 else "minecraft:mossy_cobblestone")
    t.fill(23, 2, 4, 23, 7, 4, "minecraft:stone_bricks")                  # apoyo de la escalera
    for y in range(2, S + 1):
        t.set(23, y, 3, "minecraft:ladder[facing=north,waterlogged=false]")
    t.set(23, 1, 3, "minecraft:polished_blackstone_bricks")

    # ---------- superficie: tapera en ruinas con el aljibe
    t.fill(18, S, 0, 26, S, 7, t.azar(("minecraft:coarse_dirt", 4), ("minecraft:grass_block[snowy=false]", 3),
                                      ("minecraft:cobblestone", 1)))
    for x in range(22, 25):
        for z in range(2, 5):
            if (x, z) != (23, 3):
                t.set(x, S, z, "minecraft:mossy_cobblestone")
    for x in range(22, 25):                                               # brocal del aljibe
        for z in range(2, 5):
            if (x, z) != (23, 3):
                t.set(x, S + 1, z, "minecraft:cobblestone_wall")
    t.set(23, S + 1, 3, "minecraft:spruce_trapdoor[facing=south,half=bottom,open=false,powered=false,waterlogged=false]")
    for x, z in ((22, 2), (24, 2), (22, 4), (24, 4)):
        t.fill(x, S + 2, z, x, S + 3, z, "minecraft:spruce_fence")
    t.fill(22, S + 4, 2, 24, S + 4, 4, "minecraft:spruce_slab[type=bottom,waterlogged=false]")
    t.set(23, S + 3, 3, "minecraft:chain[axis=y,waterlogged=false]")
    # muros caídos de la tapera
    ruina = t.azar(("minecraft:cobblestone", 3), ("minecraft:mossy_cobblestone", 3), ("minecraft:coarse_dirt", 1))
    for x in range(18, 27):
        for y in range(S + 1, S + 1 + t.r.choice([0, 1, 1, 2, 2, 3])):
            t.set(x, y, 0, ruina())
        for y in range(S + 1, S + 1 + t.r.choice([0, 0, 1, 2])):
            t.set(x, y, 7, ruina())
    for z in range(0, 8):
        for y in range(S + 1, S + 1 + t.r.choice([0, 1, 2, 3])):
            t.set(18, y, z, ruina())
        for y in range(S + 1, S + 1 + t.r.choice([0, 1, 1, 2])):
            t.set(26, y, z, ruina())
    t.set(20, S + 1, 2, "minecraft:dead_bush")
    t.set(25, S + 1, 6, "minecraft:dead_bush")
    t.set(20, S + 1, 5, "minecraft:spruce_fence")
    t.set(20, S + 2, 5, "minecraft:spruce_fence")
    t.set(21, S + 2, 5, "minecraft:spruce_wall_sign[facing=east,waterlogged=false]", Compound({
        "id": String("minecraft:sign"), "Text1": txt(""), "Text2": txt("Acá no hay"),
        "Text3": txt("nada que ver."), "Text4": txt("(sin ficha, no)"), "Color": String("black")}))
    t.aire_si_vacio(18, S + 1, 0, 26, SY - 1, 7)

    # ---------- interiores en aire (lo que no se llenó queda vacío, no con tierra)
    t.aire_si_vacio(1, 2, 1, 17, 7, 21)
    t.aire_si_vacio(19, 2, 1, 25, 6, 6)
    t.aire_si_vacio(19, 2, 8, 25, 7, 21)

    # ---------- gente
    t.gaucho(7, 2, 20, "CANTINERO", 3, nombre="El Tano, cantinero", mirar=180)
    t.gaucho(2, 3, 2, "PAYADOR", 2, mirar=135)
    # guardias armados con facón: sin Ficha de Clandestina (o Nivel 3) avisan y después atacan
    t.gaucho(21, 2, 4, "MATRERO", 4, nombre="El Patovica Ríos", variante=0, guardia=True, mirar=90)
    t.gaucho(16, 2, 3, "MATRERO", 4, nombre="El Hachero Funes", variante=2, guardia=True)
    t.gaucho(10, 2, 16, "MATRERO", 3, variante=1, guardia=True)
    t.gaucho(20, 2, 18, "MATRERO", 4, nombre="El Tigre Almada", variante=3, guardia=True)
    # jugadores de la casa
    t.gaucho(5, 2, 15, "MATRERO", 3)
    t.gaucho(14, 2, 11, "PAISANA", 3)
    t.gaucho(23, 2, 11, "MATRERO", 4, nombre="El Matrero Gómez")
    return t


