"""
Estancia del Caudillo (única en cada mundo). Terreno de 64 x 64, frente al norte.

  portón con arco y cartel, alambrado perimetral y alameda de eucaliptos
  casco colonial de patio (x 16..47, z 16..43):
      ala del frente de DOS PLANTAS con balcón corrido, zaguán con escalera y mirador
      salón del Viejo Vizcacha (hogar, biblioteca, sillones), escritorio del patrón
      planta alta: dormitorio del caudillo y sala de armas
      ala oeste: cuartos de huéspedes, costurero y despensa
      ala este: cocina y comedor de la peonada (con pulpero)
      ala del fondo: cuarto de aperos y herrería
      patio con galerías de columnas, aljibe, naranjos y canteros
  capilla con espadaña y campanas, vitrales y bancos
  galpón de chapa (fardos, esquila, herrería) y caballeriza con boxes
  tanque australiano y molino de viento
  huerta con quinta y espantapájaros, gallinero
  corral grande con manga, vacas y ovejas
  fogón de la peonada bajo una ramada
"""
from estructuras_base import *
from estructuras_pulperias import st, escalon, losa, W

SX, SY, SZ = 64, 24, 64


def arbol(t, x, z, tronco, hoja, alto=7, radio=2):
    for y in range(h(1), h(alto)):
        t.set(x, y, z, tronco)
    for y in range(h(alto - 2), h(alto + 2)):
        r = radio if y < h(alto + 1) else radio - 1
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                if dx * dx + dz * dz <= r * r + 1 and t.get(x + dx, y, z + dz) in (None, AIR) and t.r.random() > 0.1:
                    t.set(x + dx, y, z + dz, hoja)


def eucalipto(t, x, z):
    arbol(t, x, z, st("birch_log", axis="y"), st("oak_leaves", distance="1", persistent="true"), alto=9, radio=2)


def naranjo(t, x, z):
    for y in (h(1), h(2)):
        t.set(x, y, z, st("oak_log", axis="y"))
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            for y in (h(3), h(4)):
                if not (abs(dx) + abs(dz) == 2 and y == h(4)):
                    t.set(x + dx, y, z + dz, st("oak_leaves", distance="1", persistent="true"))
    t.set(x, h(5), z, st("oak_leaves", distance="1", persistent="true"))


def cerco(t, x0, z0, x1, z1, bloque, puertas=()):
    for x in range(x0, x1 + 1):
        for z in (z0, z1):
            if (x, z) not in puertas:
                t.set(x, h(1), z, bloque)
    for z in range(z0, z1 + 1):
        for x in (x0, x1):
            if (x, z) not in puertas:
                t.set(x, h(1), z, bloque)


def muro_blanco(t, x0, z0, x1, z1, y0, y1, blanco=W + "smooth_quartz", zocalo=W + "stone_bricks",
                esquina=W + "quartz_pillar[axis=y]"):
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x in (x0, x1) or z in (z0, z1):
                for y in range(y0, y1 + 1):
                    b = zocalo if y == h(1) else blanco
                    if x in (x0, x1) and z in (z0, z1) and y > h(1):
                        b = esquina
                    t.set(x, y, z, b)
    t.fill(x0 + 1, y0, z0 + 1, x1 - 1, y1, z1 - 1, AIR)


def fuego(t, x, y, z, prendido=True):
    t.set(x, y, z, st("campfire", facing="north", lit="true" if prendido else "false", signal_fire="false",
                      waterlogged="false"))


def estancia():
    t = Template(SX, SY, SZ, seed=1810)
    BL = W + "smooth_quartz"
    VERDE = "warped"

    # ============================================================ alambrado, portón y alameda
    cerco(t, 1, 1, 62, 62, W + "spruce_fence", puertas=[(x, 1) for x in range(30, 34)])
    for x in (29, 34):
        t.fill(x, h(1), 1, x, h(4), 1, W + "bricks")
        t.set(x, h(1), 1, W + "stone_bricks")
        t.set(x, h(6), 1, st("lantern", hanging="false"))
    t.fill(29, h(5), 1, 34, h(5), 1, W + "dark_oak_planks")
    t.fill(30, h(6), 1, 33, h(6), 1, W + "iron_bars")
    for x in (30, 33):
        t.farol(x, h(4), 1)
    t.cartel(31, h(5), 0, "north", ["", "ESTANCIA", "", ""], madera="dark_oak")
    t.cartel(32, h(5), 0, "north", ["", "EL CAUDILLO", "", ""], madera="dark_oak")
    t.sendero([(x, z) for z in range(0, 16) for x in range(30, 34)], bloque=W + "grass_path")
    for z in range(2, 16):
        t.set(29, FL, z, W + "gravel")
        t.set(34, FL, z, W + "gravel")
    for z in (4, 8, 12):
        eucalipto(t, 26, z)
        eucalipto(t, 37, z)
    # palenque frente al casco
    t.fill(20, h(1), 13, 27, h(1), 13, W + "dark_oak_fence")
    for x in (21, 25):
        t.animal(x, h(1), 12, W + "horse", {"Tame": Byte(1), "SaddleItem": Compound({"id": String("minecraft:saddle"),
                                                                                      "Count": Byte(1)})})

    # ============================================================ casco
    X0, X1, Z0, Z1 = 16, 47, 16, 43
    t.cimiento(X0, Z0, X1, Z1)
    t.fill(X0, FL, Z0, X1, FL, Z1, W + "stone_bricks")

    # --- ala del frente, dos plantas (x 16..47, z 16..24)
    muro_blanco(t, 16, 16, 47, 24, h(1), h(10))
    t.fill(17, FL, 17, 46, FL, 23, W + "dark_oak_planks")
    t.fill(17, h(6), 17, 46, h(6), 23, W + "spruce_planks")                     # entrepiso
    for x in range(16, 48):                                                      # moldura entre plantas
        t.set(x, h(6), 15, escalon("smooth_quartz", "south", "top"))
    t.fill(29, h(1), 17, 29, h(5), 23, BL)                                       # tabiques del zaguán
    t.fill(34, h(1), 17, 34, h(5), 23, BL)
    t.fill(29, h(7), 17, 29, h(10), 23, BL)                                      # tabique planta alta
    t.techo_dos_aguas(16, 16, 47, 24, h(11), eje="x", hastial=BL)
    # portón principal y puerta al patio
    t.puerta(31, h(1), 16, "south", "dark_oak", "right")
    t.puerta(32, h(1), 16, "south", "dark_oak", "left")
    t.fill(30, h(1), 16, 30, h(4), 16, W + "quartz_pillar[axis=y]")
    t.fill(33, h(1), 16, 33, h(4), 16, W + "quartz_pillar[axis=y]")
    t.fill(31, h(3), 16, 32, h(3), 16, W + "dark_oak_planks")
    t.fill(30, h(5), 16, 33, h(5), 16, W + "chiseled_quartz_block")
    t.puerta(31, h(1), 24, "south", "dark_oak", "right")
    t.puerta(32, h(1), 24, "south", "dark_oak", "left")
    t.puerta(29, h(1), 21, "west", "dark_oak", "left")
    t.puerta(34, h(1), 17, "east", "dark_oak", "left")
    # escalera del zaguán a la planta alta
    for k in range(6):
        t.set(33, h(1) + k, 18 + k, escalon("dark_oak", "south"))
        for y in range(h(1), h(1) + k):
            t.set(33, y, 18 + k, W + "dark_oak_planks")
        if k < 5:
            t.set(33, h(6), 18 + k, AIR)
    for z in range(18, 23):
        t.set(32, h(7), z, W + "dark_oak_fence")                               # baranda de la escalera
    # ventanas planta baja con rejas y postigos verdes
    for x in (19, 22, 25, 37, 40, 43, 46):
        t.ventana(x, h(2), 16, "north", postigo=VERDE)
    for z in (19, 22):
        t.ventana(16, h(2), z, "west", postigo=VERDE)
        t.ventana(47, h(2), z, "east", postigo=VERDE)
    # balcón corrido de la planta alta (z 14..15) con ménsulas y baranda
    t.fill(18, h(6), 14, 45, h(6), 15, W + "dark_oak_planks")
    for x in range(18, 46, 3):
        t.set(x, h(5), 15, escalon("stone_brick", "south", "top"))
        t.set(x, h(5), 14, escalon("stone_brick", "south", "top"))
    for x in range(18, 46):
        t.set(x, h(7), 14, W + "iron_bars")
    for x in (18, 45):
        t.set(x, h(7), 15, W + "iron_bars")
    for x in (21, 26, 37, 42):
        t.puerta(x, h(7), 16, "north", "dark_oak", "left")
        t.set(x, h(9), 16, W + "chiseled_quartz_block")
    for x in (24, 31, 32, 39):
        t.ventana(x, h(8), 16, "north", reja=W + "glass_pane", postigo=VERDE)
    for z in (19, 22):
        t.ventana(16, h(8), z, "west", reja=W + "glass_pane", postigo=VERDE)
        t.ventana(47, h(8), z, "east", reja=W + "glass_pane", postigo=VERDE)

    # --- salón del Viejo Vizcacha (x 17..28)
    for x in range(19, 28):
        for z in range(18, 23):
            t.set(x, h(1), z, W + ("red_carpet" if (x + z) % 2 else "brown_carpet"))
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            t.set(23 + dx, h(1), 20 + dz, AIR)
    t.mesa_con_sillas(23, h(1), 20, cuatro=True)
    t.farol(23, h(5), 20, cadena=0)
    # hogar con chimenea que sale por el techo
    fuego(t, 17, h(1), 20)
    for z in (19, 21):
        t.fill(17, h(1), z, 17, h(2), z, W + "bricks")
    t.set(17, h(3), 20, W + "bricks")
    t.fill(17, h(3), 19, 17, h(3), 21, escalon("brick", "east", "top"))
    t.fill(17, h(4), 20, 17, h(5), 20, W + "bricks")
    t.fill(17, h(6), 20, 17, h(16), 20, W + "bricks")
    fuego(t, 17, h(17), 20)
    for x in range(19, 28):
        if x not in (19, 22, 25):
            t.fill(x, h(1), 17, x, h(3), 17, W + "bookshelf")
    for z in (18, 22):
        t.set(18, h(1), z, escalon("dark_oak", "west"))
    t.set(27, h(1), 23, st("lectern", facing="west", has_book="false", powered="false"))
    t.set(27, h(1), 17, W + "jukebox[has_record=true]", Compound({
        "id": String("minecraft:jukebox"),
        "RecordItem": Compound({"id": String("trucoya:disco_payada"), "Count": Byte(1)})}))
    t.set(37, h(2), 20, "trucoya:tabla_picada[comidas=0]")
    for z in (18, 22):
        t.farol(20, h(5), z)
        t.farol(26, h(5), z)

    # --- escritorio del patrón (x 35..46)
    t.fill(37, h(1), 20, 39, h(1), 20, W + "cartography_table")
    t.set(38, h(1), 21, escalon("spruce", "south"))
    t.set(38, h(2), 20, st("lantern", hanging="false"))
    t.cofre(46, h(1), 23, "west", "trucoya:chests/estancia")
    for z in range(17, 24):
        t.fill(46, h(1), z, 46, h(3), z, W + "bookshelf") if z not in (19, 22, 23) else None
    t.mesa_con_sillas(42, h(1), 21)
    t.set(36, h(1), 23, W + "loom")
    t.farol(38, h(5), 20)
    t.farol(42, h(5), 21)

    # --- planta alta: dormitorio del caudillo (oeste) y sala de armas (este)
    t.set(19, h(7), 22, st("red_bed", facing="north", occupied="false", part="foot"))
    t.set(19, h(7), 21, st("red_bed", facing="north", occupied="false", part="head"))
    t.set(18, h(7), 21, W + "dark_oak_planks")
    t.set(18, h(8), 21, st("lantern", hanging="false"))
    t.set(20, h(7), 21, W + "dark_oak_planks")
    t.fill(26, h(7), 23, 28, h(8), 23, st("barrel", facing="north", open="false"))
    t.set(23, h(7), 23, st("chest", facing="north", type="single", waterlogged="false"))
    for x in range(20, 28):
        t.set(x, h(7), 19, W + "red_carpet") if x % 2 else None
    t.entity(25.5, h(7), 19.5, Compound({"id": String("minecraft:armor_stand"), "ShowArms": Byte(1),
                                         "Rotation": List[Float]([Float(180), Float(0)]),
                                         "HandItems": List[Compound]([Compound({"id": String("trucoya:facon_platero"),
                                                                                "Count": Byte(1)}), Compound({})]),
                                         "ArmorItems": List[Compound]([
                                             Compound({"id": String("minecraft:leather_boots"), "Count": Byte(1)}),
                                             Compound({"id": String("minecraft:leather_leggings"), "Count": Byte(1),
                                                       "tag": Compound({"display": Compound({"color": Int(0x2A2A2E)})})}),
                                             Compound({"id": String("minecraft:leather_chestplate"), "Count": Byte(1),
                                                       "tag": Compound({"display": Compound({"color": Int(0x8B2E2E)})})}),
                                             Compound({"id": String("minecraft:leather_helmet"), "Count": Byte(1),
                                                       "tag": Compound({"display": Compound({"color": Int(0x2A2420)})})})])}))
    for x, z in ((22, 20), (27, 20)):
        t.farol(x, h(10), z)
    # sala de armas: maniquíes y estantes
    for i, x in enumerate((36, 39, 42)):
        mano = ["trucoya:facon", "trucoya:rebenque", "trucoya:boleadoras"][i]
        t.entity(x + 0.5, h(7), 18.5, Compound({"id": String("minecraft:armor_stand"), "ShowArms": Byte(1),
                                                "Rotation": List[Float]([Float(0), Float(0)]),
                                                "HandItems": List[Compound]([Compound({"id": String(mano), "Count": Byte(1)}),
                                                                             Compound({})]),
                                                "ArmorItems": List[Compound]([Compound({}), Compound({}),
                                                                              Compound({"id": String("minecraft:leather_chestplate"), "Count": Byte(1)}),
                                                                              Compound({})])}))
    for z in range(17, 24):
        t.set(46, h(7), z, st("barrel", facing="west", open="false") if z % 2 else W + "bookshelf")
    t.mesa_con_sillas(40, h(7), 21)
    t.farol(40, h(10), 21)
    # mirador sobre el zaguán con escalera de mano
    for y in range(h(7), h(12)):
        t.set(31, y, 20, st("ladder", facing="north", waterlogged="false"))
    t.fill(31, h(7), 21, 31, h(10), 21, W + "spruce_planks")
    for y in range(h(11), h(15)):
        for x in range(30, 34):
            for z in range(18, 22):
                t.set(x, y, z, BL if (x in (30, 33) or z in (18, 21)) else AIR)
    for x, z in ((30, 18), (33, 18), (30, 21), (33, 21)):
        t.fill(x, h(11), z, x, h(14), z, W + "quartz_pillar[axis=y]")
    t.fill(31, h(11), 19, 32, h(11), 20, W + "spruce_planks")
    t.set(31, h(11), 20, st("ladder", facing="north", waterlogged="false"))
    for x in (31, 32):
        t.ventana(x, h(12), 18, "north", reja=W + "glass_pane", alto=2)
        t.ventana(x, h(12), 21, "south", reja=W + "glass_pane", alto=2)
    for z in (19, 20):
        t.ventana(30, h(12), z, "west", reja=W + "glass_pane", alto=2)
        t.ventana(33, h(12), z, "east", reja=W + "glass_pane", alto=2)
    for x in range(29, 35):
        for z in range(17, 23):
            borde = x in (29, 34) or z in (17, 22)
            if borde:
                f = "south" if z == 17 else "north" if z == 22 else "east" if x == 29 else "west"
                t.set(x, h(15), z, escalon("brick", f))
            else:
                t.set(x, h(15), z, W + "bricks")
    for x in range(30, 34):
        for z in range(18, 22):
            if x in (30, 33) or z in (18, 21):
                f = "south" if z == 18 else "north" if z == 21 else "east" if x == 30 else "west"
                t.set(x, h(16), z, escalon("brick", f))
            else:
                t.set(x, h(16), z, W + "bricks")
    t.fill(31, h(17), 19, 32, h(17), 20, losa("brick"))
    t.farol(31, h(14), 19)

    # --- alas laterales y del fondo (una planta)
    muro_blanco(t, 16, 24, 24, 43, h(1), h(5))
    muro_blanco(t, 39, 24, 47, 43, h(1), h(5))
    muro_blanco(t, 24, 37, 39, 43, h(1), h(5))
    t.fill(17, FL, 25, 23, FL, 42, W + "spruce_planks")
    t.fill(40, FL, 25, 46, FL, 42, t.azar((W + "terracotta", 2), (W + "polished_granite", 1)))
    t.fill(25, FL, 38, 38, FL, 42, W + "spruce_planks")
    # reabrir los encuentros entre alas
    t.fill(17, h(1), 24, 23, h(5), 24, BL)
    t.fill(40, h(1), 24, 46, h(5), 24, BL)
    t.fill(25, h(1), 38, 38, h(5), 42, AIR)
    t.techo_dos_aguas(16, 25, 24, 43, h(6), eje="z", hastial=BL, hastiales=(False, True),
                      aleros=(True, True, False, True))
    t.techo_dos_aguas(39, 25, 47, 43, h(6), eje="z", hastial=BL, hastiales=(False, True),
                      aleros=(True, True, False, True))
    t.techo_dos_aguas(25, 37, 38, 43, h(6), eje="x", hastial=BL, hastiales=(False, False),
                      aleros=(False, False, True, True))
    for x0 in (16, 39):
        for z in range(26, 43, 3):
            t.fill(x0 + 1, h(5), z, x0 + 7, h(5), z, st("stripped_dark_oak_log", axis="x"))
    for z in (28, 34):
        t.ventana(16, h(2), z, "west", postigo=VERDE)
        t.ventana(47, h(2), z, "east", postigo=VERDE)
    for z in (40,):
        t.ventana(16, h(2), z, "west", postigo=VERDE)
        t.ventana(47, h(2), z, "east", postigo=VERDE)
    for x in (19, 27, 31, 36, 44):
        t.ventana(x, h(2), 43, "south", postigo=VERDE)

    # ala oeste: cuartos de huéspedes, costurero, despensa
    for z in (31, 37):
        t.fill(17, h(1), z, 23, h(5), z, BL)
        t.puerta(23, h(1), z, "south", "spruce", "left")
    for z, x in ((26, 18), (26, 21)):
        t.set(x, h(1), z + 1, st("white_bed", facing="north", occupied="false", part="foot"))
        t.set(x, h(1), z, st("white_bed", facing="north", occupied="false", part="head"))
    t.set(23, h(1), 26, st("chest", facing="west", type="single", waterlogged="false"))
    t.farol(20, h(4), 28)
    t.set(18, h(1), 33, W + "loom")
    t.set(18, h(1), 35, W + "loom")
    t.fill(21, h(1), 32, 22, h(1), 32, W + "white_wool")
    t.set(21, h(1), 36, W + "cartography_table")
    t.farol(20, h(4), 34)
    for z in range(38, 43):
        t.set(17, h(1), z, st("barrel", facing="east", open="false"))
        t.set(17, h(2), z, st("barrel", facing="east", open="false"))
        t.set(23, h(1), z, st("hay_block", axis="y") if z % 2 else st("barrel", facing="up", open="false"))
    t.set(20, h(1), 42, st("smoker", facing="north", lit="false"))
    t.farol(20, h(4), 40)
    # puertas de las alas al patio
    for z in (28, 34, 40):
        t.puerta(24, h(1), z, "east", "spruce", "left")
        t.puerta(39, h(1), z, "west", "spruce", "left")

    # ala este: cocina y comedor con pulpero
    t.fill(40, h(1), 31, 46, h(5), 31, BL)
    t.puerta(43, h(1), 31, "south", "spruce", "left")
    for z, b in ((25, st("smoker", facing="west", lit="true")), (26, st("furnace", facing="west", lit="true")),
                 (27, st("cauldron", level="3")), (28, W + "crafting_table"), (29, st("barrel", facing="up", open="false"))):
        t.set(46, h(1), z, b)
    t.fill(46, h(2), 25, 46, h(2), 26, escalon("brick", "west"))
    t.fill(47, h(6), 26, 47, h(10), 26, W + "bricks")
    fuego(t, 47, h(11), 26)
    t.set(41, h(1), 25, W + "spruce_planks")
    t.set(41, h(2), 25, W + "potted_red_tulip")
    t.set(42, h(1), 28, W + "hay_block[axis=y]")
    t.farol(43, h(4), 27)
    t.mesa_con_sillas(43, h(1), 34)
    t.mesa_con_sillas(43, h(1), 38)
    for x in range(40, 47):
        t.set(x, h(1), 41, W + "spruce_planks" if x != 43 else W + "spruce_fence_gate[facing=north,in_wall=false,open=false,powered=false]")
        t.set(x, h(2), 41, W + "spruce_fence" if x not in (42, 43, 44) else AIR)
    t.fill(40, h(1), 42, 46, h(1), 42, st("barrel", facing="north", open="false"))
    t.fill(40, h(2), 42, 46, h(2), 42, W + "bookshelf")
    t.farol(43, h(4), 36)

    # ala del fondo: aperos y herrería
    t.fill(31, h(1), 38, 31, h(5), 42, BL)
    t.puerta(31, h(1), 40, "east", "spruce", "left")
    t.puerta(28, h(1), 37, "south", "spruce", "left")
    t.puerta(35, h(1), 37, "south", "spruce", "left")
    for i, x in enumerate((26, 28)):
        t.entity(x + 0.5, h(1), 41.5, Compound({"id": String("minecraft:armor_stand"), "ShowArms": Byte(0),
                                                "Rotation": List[Float]([Float(180), Float(0)]),
                                                "ArmorItems": List[Compound]([Compound({}), Compound({}),
                                                                              Compound({"id": String("minecraft:leather_chestplate"), "Count": Byte(1)}),
                                                                              Compound({})])}))
    t.set(30, h(1), 42, st("hay_block", axis="y"))
    t.set(25, h(1), 38, st("chest", facing="south", type="single", waterlogged="false"))
    t.set(34, h(1), 42, W + "anvil[facing=north]")
    t.set(35, h(1), 42, W + "grindstone[face=floor,facing=north]")
    t.set(36, h(1), 42, W + "smithing_table")
    t.set(37, h(1), 42, st("blast_furnace", facing="north", lit="true"))
    t.fill(37, h(2), 42, 37, h(5), 42, W + "bricks")
    t.fill(37, h(6), 43, 37, h(9), 43, W + "bricks")
    fuego(t, 37, h(10), 43)
    t.farol(28, h(4), 40)
    t.farol(35, h(4), 40)

    # --- patio con galerías, aljibe y naranjos (x 25..38, z 25..36)
    t.fill(25, FL, 25, 38, FL, 36, W + "polished_granite")
    t.fill(27, FL, 27, 36, FL, 34, W + "grass_block[snowy=false]")
    t.sendero([(31, z) for z in range(27, 35)] + [(32, z) for z in range(27, 35)] +
              [(x, 30) for x in range(27, 37)] + [(x, 31) for x in range(27, 37)], bloque=W + "polished_andesite")
    for x in range(25, 39):
        for z in range(25, 37):
            borde_ext = x in (25, 38) or z in (25, 36)
            borde_int = x in (26, 37) or z in (26, 35)
            if borde_ext:
                t.set(x, h(4), z, losa("brick", "top"))
            elif borde_int and 26 <= x <= 37 and 26 <= z <= 35:
                t.set(x, h(4), z, losa("brick"))
    for x in range(26, 38, 3):
        for z in (26, 35):
            t.fill(x, h(1), z, x, h(3), z, W + "quartz_pillar[axis=y]")
    for z in range(26, 36, 3):
        for x in (26, 37):
            t.fill(x, h(1), z, x, h(3), z, W + "quartz_pillar[axis=y]")
    for x in range(27, 37):
        if (x - 26) % 3:
            t.set(x, h(3), 26, escalon("smooth_quartz", "north", "top"))
            t.set(x, h(3), 35, escalon("smooth_quartz", "south", "top"))
    for z in range(27, 35):
        if (z - 26) % 3:
            t.set(26, h(3), z, escalon("smooth_quartz", "west", "top"))
            t.set(37, h(3), z, escalon("smooth_quartz", "east", "top"))
    for x, z in ((29, 25), (34, 25), (25, 30), (38, 30), (29, 36), (34, 36)):
        t.farol(x, h(3), z)
    # aljibe
    t.box(30, h(1), 29, 33, h(1), 32, W + "stone_bricks")
    for x in (31, 32):
        for z in (30, 31):
            for y in (FL - 1, FL, h(1)):
                t.set(x, y, z, st("water", level="0"))
    for x, z in ((30, 29), (33, 29), (30, 32), (33, 32)):
        t.fill(x, h(2), z, x, h(3), z, W + "dark_oak_fence")
    t.fill(30, h(4), 29, 33, h(4), 32, losa("brick"))
    t.fill(31, h(4), 30, 32, h(4), 31, W + "bricks")
    t.set(31, h(3), 30, st("chain", axis="y", waterlogged="false"))
    for x, z in ((28, 28), (35, 28), (28, 33), (35, 33)):
        naranjo(t, x, z)
    for x, z in ((27, 30), (36, 31), (31, 27), (32, 34)):
        t.set(x, h(1), z, st("rose_bush", half="lower"))
        t.set(x, h(2), z, st("rose_bush", half="upper"))

    # ============================================================ capilla (x 3..12, z 18..34)
    t.cimiento(3, 18, 12, 34)
    t.fill(3, FL, 18, 12, FL, 34, W + "stone_bricks")
    for x in range(4, 12):
        for z in range(21, 34):
            t.set(x, FL, z, W + ("polished_diorite" if (x + z) % 2 else "polished_andesite"))
    muro_blanco(t, 3, 20, 12, 34, h(1), h(6))
    t.techo_dos_aguas(3, 20, 12, 34, h(7), eje="z", hastial=BL, hastiales=(False, True),
                      aleros=(True, True, False, True))
    # fachada con espadaña y dos campanas
    for x in range(3, 13):
        alto = h(9) if x in (3, 12) else h(11) if x in (4, 11) else h(13)
        t.fill(x, h(1), 20, x, alto, 20, BL)
        t.set(x, alto + 1, 20, losa("smooth_quartz"))
    for x in (6, 9):
        t.fill(x, h(10), 20, x, h(11), 20, AIR)
        t.set(x, h(11), 20, st("bell", attachment="ceiling", facing="north", powered="false"))
    t.set(7, h(14), 20, W + "chiseled_quartz_block")
    t.set(8, h(14), 20, W + "chiseled_quartz_block")
    t.set(7, h(15), 20, losa("smooth_quartz"))
    t.puerta(7, h(1), 20, "south", "spruce", "right")
    t.puerta(8, h(1), 20, "south", "spruce", "left")
    t.fill(7, h(3), 20, 8, h(3), 20, W + "chiseled_quartz_block")
    t.set(7, h(6), 20, W + "orange_stained_glass_pane")
    t.set(8, h(6), 20, W + "orange_stained_glass_pane")
    for z, vidrio in ((23, "yellow"), (27, "red"), (31, "blue")):
        for x, f in ((3, "west"), (12, "east")):
            t.ventana(x, h(2), z, f, reja=W + f"{vidrio}_stained_glass_pane", alto=3)
    for z in range(23, 31, 2):
        for x in (5, 6, 9, 10):
            t.set(x, h(1), z, escalon("spruce", "north"))
    t.fill(6, h(1), 32, 9, h(1), 32, W + "quartz_pillar[axis=y]")
    t.fill(6, h(2), 32, 9, h(2), 32, losa("smooth_quartz", "bottom"))
    t.fill(7, h(2), 33, 8, h(5), 33, st("stripped_spruce_log", axis="y"))
    t.fill(6, h(4), 33, 9, h(4), 33, st("stripped_spruce_log", axis="x"))
    t.set(6, h(3), 32, st("lantern", hanging="false"))
    t.set(9, h(3), 32, st("lantern", hanging="false"))
    for z in (24, 29):
        t.farol(7, h(6), z)
        t.set(7, h(7), z, W + "spruce_planks")
    t.sendero([(x, z) for z in range(16, 20) for x in (7, 8)])
    t.sendero([(x, 16) for x in range(9, 16)])

    # ============================================================ galpón (x 50..61, z 17..33)
    t.cimiento(50, 17, 61, 33)
    t.fill(50, FL, 17, 61, FL, 33, t.azar((W + "coarse_dirt", 3), (W + "gravel", 1)))
    for x in range(50, 62):
        for z in range(17, 34):
            if x in (50, 61) or z in (17, 33):
                for y in range(h(1), h(6)):
                    t.set(x, y, z, W + "bricks")
                if x in (50, 61) and z in (17, 33):
                    t.fill(x, h(1), z, x, h(5), z, W + "stone_bricks")
    t.fill(51, h(1), 18, 60, h(5), 32, AIR)
    t.fill(54, h(1), 17, 57, h(4), 17, AIR)                                     # portón abierto
    t.fill(53, h(5), 17, 58, h(5), 17, st("spruce_log", axis="x"))
    t.techo_dos_aguas(50, 17, 61, 33, h(6), eje="z", teja=W + "smooth_stone", hastial=W + "bricks")
    for z in range(19, 33, 3):
        t.fill(51, h(5), z, 60, h(5), z, st("spruce_log", axis="x"))
    for z in (21, 25, 29):
        t.ventana(50, h(3), z, "west", reja=W + "iron_bars", alto=1)
        t.ventana(61, h(3), z, "east", reja=W + "iron_bars", alto=1)
    for z in range(19, 27):
        t.fill(51, h(1), z, 52, h(2), z, st("hay_block", axis="z"))
    t.fill(51, h(3), 20, 52, h(3), 24, st("hay_block", axis="x"))
    t.set(60, h(1), 19, W + "anvil[facing=west]")
    t.set(60, h(1), 20, W + "grindstone[face=floor,facing=west]")
    t.set(60, h(1), 21, st("blast_furnace", facing="west", lit="true"))
    t.set(60, h(1), 22, st("barrel", facing="up", open="false"))
    t.set(60, h(1), 23, W + "smithing_table")
    for x in range(53, 61):
        t.set(x, h(1), 28, W + "oak_fence" if x != 56 else W + "oak_fence_gate[facing=north,in_wall=false,open=false,powered=false]")
    for x in (53, 55, 58):
        t.animal(x, h(1), 31, W + "sheep")
    t.set(59, h(1), 32, st("cauldron", level="3"))
    for z in (21, 26, 30):
        t.farol(56, h(4), z)
    t.sendero([(x, z) for z in range(14, 17) for x in range(54, 58)])

    # ============================================================ caballeriza (x 50..61, z 37..47)
    t.cimiento(50, 37, 61, 47)
    t.fill(50, FL, 37, 61, FL, 47, t.azar((W + "coarse_dirt", 2), (W + "dirt", 1)))
    for x in range(50, 62):
        for z in range(37, 48):
            if x in (50, 61) or z in (37, 47):
                for y in range(h(1), h(5)):
                    t.set(x, y, z, W + "spruce_planks")
                if (x in (50, 61) and z % 3 == 0) or (z in (37, 47) and x % 3 == 0):
                    t.fill(x, h(1), z, x, h(4), z, st("stripped_spruce_log", axis="y"))
    t.fill(51, h(1), 38, 60, h(4), 46, AIR)
    t.fill(54, h(1), 37, 57, h(3), 37, AIR)
    t.techo_dos_aguas(50, 37, 61, 47, h(5), eje="x", teja=W + "dark_oak", hastial=W + "spruce_planks")
    for x in range(51, 61):
        if (x - 51) % 3 == 2:
            t.fill(x, h(1), 43, x, h(2), 46, W + "spruce_fence")
    for i, x in enumerate((52, 55, 58)):
        t.set(x, h(1), 46, st("hay_block", axis="y"))
        t.animal(x, h(1), 44, W + "horse", {"Tame": Byte(1)})
    for x in range(51, 61):
        t.set(x, h(1), 42, W + "spruce_fence" if x not in (52, 55, 58) else
              W + "spruce_fence_gate[facing=north,in_wall=false,open=false,powered=false]")
    t.farol(55, h(4), 40)
    t.farol(55, h(4), 45)

    # ============================================================ tanque australiano y molino
    cx, cz = 8, 40
    for x in range(cx - 4, cx + 5):
        for z in range(cz - 4, cz + 5):
            d = ((x - cx) ** 2 + (z - cz) ** 2) ** 0.5
            if d <= 4.4:
                t.set(x, 0, z, W + "stone")
                t.set(x, 1, z, W + "stone")
                if d > 3.4:
                    t.set(x, FL, z, W + "smooth_stone")
                    t.set(x, h(1), z, W + "smooth_stone")
                    t.set(x, h(2), z, W + "smooth_stone_slab[type=bottom,waterlogged=false]")
                else:
                    t.set(x, FL, z, st("water", level="0"))
                    t.set(x, h(1), z, st("water", level="0"))
    mx, mz = 15, 40
    for dx in (-1, 1):
        for dz in (-1, 1):
            t.fill(mx + dx, h(1), mz + dz, mx + dx, h(10), mz + dz, W + "iron_bars")
    for y in (h(4), h(7)):
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                if abs(dx) + abs(dz) == 1:
                    t.set(mx + dx, y, mz + dz, W + "iron_bars")
    t.fill(mx - 1, h(11), mz - 1, mx + 1, h(11), mz + 1, W + "smooth_stone_slab[type=bottom,waterlogged=false]")
    t.fill(mx, h(11), mz, mx, h(13), mz, W + "iron_bars")
    t.set(mx, h(13), mz - 1, W + "iron_block")
    for dx, dy in ((0, 1), (0, 2), (0, -1), (0, -2), (1, 0), (2, 0), (-1, 0), (-2, 0), (1, 1), (-1, -1), (1, -1), (-1, 1)):
        t.set(mx + dx, h(13) + dy, mz - 1, st("iron_trapdoor", facing="north", half="bottom", open="true",
                                               powered="false", waterlogged="false"))
    t.fill(mx, h(13), mz + 1, mx, h(13), mz + 2, W + "iron_bars")
    t.set(mx, h(13), mz + 3, st("iron_trapdoor", facing="east", half="bottom", open="true", powered="false",
                                waterlogged="false"))
    t.fill(mx - 2, h(1), mz, cx + 5, h(1), mz, W + "iron_bars")

    # ============================================================ huerta, espantapájaros y gallinero
    cerco(t, 3, 51, 15, 61, W + "oak_fence", puertas=[(9, 51)])
    t.set(9, h(1), 51, st("oak_fence_gate", facing="north", in_wall="false", open="false", powered="false"))
    cultivos = [st("wheat", age="7"), st("carrots", age="7"), st("potatoes", age="7"), st("beetroots", age="3")]
    for x in range(4, 15):
        for z in range(52, 61):
            if x in (7, 11):
                t.set(x, FL, z, st("water", level="0"))
            else:
                t.set(x, FL, z, st("farmland", moisture="7"))
                t.set(x, h(1), z, cultivos[(x // 4 + z // 3) % 4] if x != 4 else "trucoya:yerbal[age=7]")
            t.set(x, FL - 1, z, W + "dirt")
    t.set(9, h(1), 56, W + "oak_fence")
    t.set(9, h(2), 56, st("hay_block", axis="y"))
    t.set(9, h(3), 56, st("carved_pumpkin", facing="north"))
    # gallinero
    cerco(t, 17, 52, 22, 58, W + "oak_fence")
    t.fill(18, h(1), 57, 21, h(1), 57, st("hay_block", axis="x"))
    t.fill(17, h(3), 56, 22, h(3), 58, losa("spruce"))
    for x in (17, 22):
        t.set(x, h(2), 58, W + "oak_fence")
        t.set(x, h(2), 56, W + "oak_fence")
    for x in (18, 19, 20, 21):
        t.animal(x, h(1), 54, W + "chicken")

    # ============================================================ corral grande con manga
    cerco(t, 25, 48, 47, 61, W + "oak_fence", puertas=[(36, 48)])
    t.set(36, h(1), 48, st("oak_fence_gate", facing="north", in_wall="false", open="false", powered="false"))
    for x in range(27, 35):
        t.set(x, h(1), 50, W + "oak_fence")
        t.set(x, h(1), 52, W + "oak_fence")
    for x in (40, 41, 42):
        t.set(x, h(1), 60, st("cauldron", level="3"))
    t.fill(26, h(1), 59, 27, h(2), 60, st("hay_block", axis="y"))
    for x, z, tipo in ((30, 56, "cow"), (33, 55, "cow"), (37, 57, "cow"), (40, 54, "cow"),
                       (43, 57, "sheep"), (44, 53, "sheep"), (35, 59, "sheep")):
        t.animal(x, h(1), z, W + tipo)

    # ============================================================ fogón de la peonada (x 50..61, z 51..60)
    t.fill(50, FL, 51, 61, FL, 60, t.azar((W + "coarse_dirt", 3), (W + "grass_block[snowy=false]", 2)))
    for x, z in ((50, 51), (61, 51), (50, 60), (61, 60), (55, 51), (55, 60)):
        t.fill(x, h(1), z, x, h(3), z, st("spruce_log", axis="y"))
    t.fill(50, h(4), 51, 61, h(4), 60, losa("spruce"))
    for x in range(50, 62):
        t.set(x, h(4), 50, losa("spruce"))
    fuego(t, 55, h(1), 55)
    fuego(t, 56, h(1), 56)
    for x in range(53, 59):
        t.set(x, h(1), 53, st("stripped_oak_log", axis="x"))
        t.set(x, h(1), 58, st("stripped_oak_log", axis="x"))
    for z in range(54, 58):
        t.set(52, h(1), z, st("stripped_oak_log", axis="z"))
        t.set(59, h(1), z, st("stripped_oak_log", axis="z"))
    t.farol(55, h(3), 53)
    t.farol(55, h(3), 58)

    # ============================================================ caminos y árboles
    t.terreno(0, 0, SX - 1, SZ - 1)
    t.sendero([(x, z) for x in range(17, 62) for z in (45, 46)])
    t.sendero([(x, z) for z in range(14, 46) for x in (48, 49)])
    t.sendero([(x, z) for z in range(14, 36) for x in (14, 15)])
    t.sendero([(x, z) for x in range(14, 50) for z in (14,)])
    t.sendero([(x, z) for z in range(44, 48) for x in (36,)])
    for x, z in ((4, 4), (10, 9), (5, 13), (58, 5), (52, 10), (60, 12), (3, 38), (60, 35), (24, 3), (40, 3)):
        eucalipto(t, x, z)

    # ============================================================ la gente de la estancia
    t.gaucho(23, h(1), 22, "VIZCACHA", 5, mirar=0)
    t.gaucho(26, h(1), 19, "PAYADOR", 3)
    t.gaucho(31, h(1), 28, "PEON", 3, nombre="Don Ciriaco, capataz")
    t.gaucho(44, h(1), 27, "PAISANA", 2, nombre="Doña Pilar, cocinera", mirar=90)
    t.gaucho(43, h(1), 40, "CANTINERO", 2, nombre="Don Fermín, pulpero de la estancia", mirar=180)
    t.gaucho(41, h(1), 36, "PEON", 2)
    t.gaucho(8, h(1), 26, "PAISANA", 1)
    t.gaucho(56, h(1), 22, "PEON", 2)
    t.gaucho(55, h(1), 40, "PEON", 2)
    t.gaucho(35, h(1), 55, "PEON", 2)
    t.gaucho(54, h(1), 54, "PAYADOR", 2)
    t.gaucho(57, h(1), 57, "PAISANO", 2)
    t.gaucho(39, h(7), 20, "MATRERO", 4, nombre="El Tuerto Leiva, guardaespaldas", guardia=False)
    t.aire_si_vacio(0, FL + 1, 0, SX - 1, SY - 1, SZ - 1)
    return t
