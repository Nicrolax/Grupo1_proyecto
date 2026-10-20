"""
Base común para las plantillas de estructuras (clase Template y piezas de arquitectura).
Genera las plantillas de estructuras (.nbt, formato de Minecraft 1.16.5) de la Fase 4:
  - pulperia.nbt           Pulpería de campaña: azotea, galería, mostrador enrejado, estufa,
                           enramada con parrilla, aljibe, palenque y un ombú
  - aldea/pulperia.nbt     Versión compacta con conexión (jigsaw) para insertarse en las aldeas
  - guarida.nbt            Bar clandestino subterráneo: barra, tablado del payador, reservado,
                           entrada disimulada en un aljibe dentro de una tapera
  - estancia.nbt           Casco de la Estancia del Caudillo: casa de patio con galerías, mirador,
                           campana, salón del Viejo Vizcacha, cocina, dormitorio y corral
Diseños originales. Todas las plantillas traen cimiento (2 capas bajo el piso) y aire explícito
sobre el piso, para no quedar enterradas ni con tierra adentro.

Uso: python3 tools/generar_estructuras.py   (requiere: pip install nbtlib)
"""
import json, os, random
import nbtlib
from nbtlib.tag import Compound, List, Int, Double, String, Byte, Float

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "data", "trucoya")
DATA_VERSION = 2586  # 1.16.5
FL = 2               # altura local del piso (debajo: 2 capas de cimiento)


def path(*p):
    full = os.path.join(ROOT, *p)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    return full


def parse_state(s):
    if "[" in s:
        name, props = s[:-1].split("[", 1)
        d = {}
        for kv in props.split(","):
            k, v = kv.split("=")
            d[k] = v
        return name, d
    return s, {}


def txt(t):
    return String(json.dumps({"text": t}, ensure_ascii=False))


AIR = "minecraft:air"


class Template:
    def __init__(self, sx, sy, sz, seed=1):
        self.size = (sx, sy, sz)
        self.blocks = {}
        self.entities = []
        self.ventanas = []          # (x, y, z, afuera, alto): para ponerles alféizar y guardapolvo
        self.puertas = []           # (x, y, z, facing): para los faroles de la entrada
        self.r = random.Random(seed)

    # ------------------------------------------------------------ básicos
    def set(self, x, y, z, state, nbt=None):
        sx, sy, sz = self.size
        if 0 <= x < sx and 0 <= y < sy and 0 <= z < sz:
            if not state.startswith("minecraft:") and ":" not in state:
                state = "minecraft:" + state
            self.blocks[(x, y, z)] = (state, nbt)

    def get(self, x, y, z):
        b = self.blocks.get((x, y, z))
        return b[0] if b else None

    def fill(self, x0, y0, z0, x1, y1, z1, state):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, state() if callable(state) else state)

    def box(self, x0, y0, z0, x1, y1, z1, wall, inside=AIR):
        self.fill(x0, y0, z0, x1, y1, z1, inside)
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    if x in (x0, x1) or z in (z0, z1):
                        self.set(x, y, z, wall() if callable(wall) else wall)

    def aire_si_vacio(self, x0, y0, z0, x1, y1, z1):
        """Aire explícito donde no hay nada: así la plantilla limpia el terreno que tenga adentro."""
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    if (x, y, z) not in self.blocks:
                        self.set(x, y, z, AIR)

    def azar(self, *opciones):
        """Elige un bloque al azar: azar(('stone_bricks', 6), ('mossy_stone_bricks', 1))."""
        tot = sum(w for _, w in opciones)
        def f():
            k = self.r.uniform(0, tot)
            for b, w in opciones:
                k -= w
                if k <= 0:
                    return b
            return opciones[-1][0]
        return f

    # ------------------------------------------------------------ piezas del mod
    def entity(self, x, y, z, nbt):
        self.entities.append((x, y, z, nbt))

    def gaucho(self, x, y, z, rol, nivel, nombre=None, variante=None, mirar=0.0, guardia=False):
        nbt = {"id": String("trucoya:gaucho"), "Rol": String(rol), "Nivel": Int(nivel),
               "PersistenceRequired": Byte(1), "Rotation": List[Float]([Float(mirar), Float(0)])}
        if guardia:
            nbt["Guardia"] = Byte(1)
        if nombre:
            nbt["CustomName"] = txt(nombre)
        if variante is not None:
            nbt["Variante"] = Int(variante)
        self.entity(x + 0.5, y, z + 0.5, Compound(nbt))

    def animal(self, x, y, z, tipo, extra=None):
        nbt = {"id": String(tipo), "PersistenceRequired": Byte(1)}
        if extra:
            nbt.update(extra)
        self.entity(x + 0.5, y, z + 0.5, Compound(nbt))

    def mesa(self, x, y, z, clandestina=False, players=2):
        self.set(x, y, z, "trucoya:mesa_truco", Compound({
            "id": String("trucoya:mesa_truco"), "CfgPlayers": Int(players), "CfgTarget": Int(30),
            "CfgFlor": Byte(1), "Clandestina": Byte(1 if clandestina else 0), "Pozo": List[Compound]([])}))

    def silla(self, x, y, z, facing):
        self.set(x, y, z, f"trucoya:silla_criolla[facing={facing}]")

    def mesa_con_sillas(self, x, y, z, cuatro=False, clandestina=False, anotador=True, eje="x"):
        self.mesa(x, y, z, clandestina, 4 if cuatro else 2)
        if eje == "x" or cuatro:
            self.silla(x - 1, y, z, "east")
            self.silla(x + 1, y, z, "west")
        if eje == "z" or cuatro:
            self.silla(x, y, z - 1, "south")
            self.silla(x, y, z + 1, "north")
        if anotador:
            self.set(x, y + 1, z, "trucoya:anotador[blancos=0,negros=0]")

    def cartel(self, x, y, z, facing, lineas, madera="spruce"):
        t = [txt(l) for l in (lineas + ["", "", "", ""])[:4]]
        self.set(x, y, z, f"minecraft:{madera}_wall_sign[facing={facing},waterlogged=false]", Compound({
            "id": String("minecraft:sign"), "Text1": t[0], "Text2": t[1], "Text3": t[2], "Text4": t[3],
            "Color": String("black")}))

    def cofre(self, x, y, z, facing, tabla):
        self.set(x, y, z, f"minecraft:chest[facing={facing},type=single,waterlogged=false]", Compound({
            "id": String("minecraft:chest"), "LootTable": String(tabla)}))

    # ------------------------------------------------------------ piezas de arquitectura
    def ventana(self, x, y, z, afuera, alto=2, reja="minecraft:iron_bars", postigo=None, dintel=None):
        """Ventana con reja en un muro; 'afuera' es la dirección hacia el exterior (north/south/east/west)."""
        for h in range(alto):
            self.set(x, y + h, z, reja)
        self.ventanas.append((x, y, z, afuera, alto))
        if dintel:
            self.set(x, y + alto, z, dintel)
        if postigo:
            dx, dz = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}[afuera]
            ox, oz = x + dx, z + dz
            lados = [(-1, 0), (1, 0)] if afuera in ("north", "south") else [(0, -1), (0, 1)]
            for lx, lz in lados:
                for h in range(alto):
                    if self.get(ox + lx, y + h, oz + lz) in (None, AIR):
                        self.set(ox + lx, y + h, oz + lz,
                                 f"minecraft:{postigo}_trapdoor[facing={afuera},half=bottom,open=true,"
                                 f"powered=false,waterlogged=false]")

    def puerta(self, x, y, z, facing, madera="dark_oak", hinge="left"):
        self.puertas.append((x, y, z, facing))
        self.set(x, y, z, f"minecraft:{madera}_door[facing={facing},half=lower,hinge={hinge},open=false,powered=false]")
        self.set(x, y + 1, z, f"minecraft:{madera}_door[facing={facing},half=upper,hinge={hinge},open=false,powered=false]")

    def farol(self, x, y, z, alma=False, cadena=0):
        for c in range(cadena):
            self.set(x, y + 1 + c, z, "minecraft:chain[axis=y,waterlogged=false]")
        self.set(x, y, z, f"minecraft:{'soul_' if alma else ''}lantern[hanging=true]")

    def cimiento(self, x0, z0, x1, z1, bloque=None):
        b = bloque or self.azar(("minecraft:cobblestone", 5), ("minecraft:mossy_cobblestone", 1),
                                ("minecraft:stone", 2))
        self.fill(x0, 0, z0, x1, FL - 1, z1, b)

    def terreno(self, x0, z0, x1, z1, borde=True):
        """Suelo natural alrededor del edificio (pasto con algo de tierra y senderos)."""
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if (x, FL, z) in self.blocks:
                    continue
                k = self.r.random()
                top = "minecraft:grass_block[snowy=false]" if k < 0.82 else (
                    "minecraft:coarse_dirt" if k < 0.93 else "minecraft:grass_path")
                self.set(x, FL, z, top)
                for y in range(0, FL):
                    if (x, y, z) not in self.blocks:
                        self.set(x, y, z, "minecraft:dirt")
                if top.startswith("minecraft:grass_block") and self.r.random() < 0.18:
                    if (x, FL + 1, z) not in self.blocks:
                        self.set(x, FL + 1, z, self.r.choice(["minecraft:grass", "minecraft:grass",
                                                               "minecraft:dandelion", "minecraft:poppy",
                                                               "minecraft:cornflower", "minecraft:oxeye_daisy"]))

    def sendero(self, puntos, ancho=1, bloque="minecraft:grass_path"):
        for x, z in puntos:
            for a in range(ancho):
                self.set(x + a, FL, z, bloque)
                for y in range(0, FL):
                    if (x + a, y, z) not in self.blocks:
                        self.set(x + a, y, z, "minecraft:dirt")
                if self.get(x + a, FL + 1, z) and self.get(x + a, FL + 1, z) != AIR:
                    self.set(x + a, FL + 1, z, AIR)

    def azotea(self, x0, z0, x1, z1, y, muro, cornisa=True, pretil=True, deck="minecraft:bricks"):
        """Techo plano criollo: piso de ladrillo, pretil perimetral con remate y cornisa saliente."""
        self.fill(x0 + 1, y, z0 + 1, x1 - 1, y, z1 - 1, deck)
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    self.set(x, y, z, muro)
                    if pretil:
                        self.set(x, y + 1, z, muro)
                        self.set(x, y + 2, z, "minecraft:stone_brick_slab[type=bottom,waterlogged=false]")
        if cornisa:
            for x in range(x0, x1 + 1):
                for z, f in ((z0 - 1, "south"), (z1 + 1, "north")):
                    if self.get(x, y - 1, z) in (None, AIR):
                        self.set(x, y - 1, z, f"minecraft:smooth_quartz_stairs[facing={f},half=top,shape=straight,waterlogged=false]")
            for z in range(z0, z1 + 1):
                for x, f in ((x0 - 1, "east"), (x1 + 1, "west")):
                    if self.get(x, y - 1, z) in (None, AIR):
                        self.set(x, y - 1, z, f"minecraft:smooth_quartz_stairs[facing={f},half=top,shape=straight,waterlogged=false]")

    def techo_dos_aguas(self, x0, z0, x1, z1, ybase, eje="x", teja="minecraft:brick",
                        hastial="minecraft:smooth_quartz", hastiales=(True, True), aleros=(True, True, True, True)):
        """
        Techo de tejas a dos aguas con pendiente suave (media altura por bloque) y aleros.
        x0..x1 / z0..z1 son las líneas de los muros; eje = dirección de la cumbrera.
        hastiales: (inicio, fin) del eje con muro triangular; aleros: (x0-1, x1+1, z0-1, z1+1).
        """
        def losa(y_half):
            if y_half % 2 == 0:
                return y_half // 2, f"{teja}_slab[type=bottom,waterlogged=false]"
            return (y_half - 1) // 2, f"{teja}_slab[type=top,waterlogged=false]"
        if eje == "x":
            a0, a1, b0, b1 = x0, x1, z0, z1      # a: a lo largo de la cumbrera, b: a lo ancho
        else:
            a0, a1, b0, b1 = z0, z1, x0, x1
        ea0 = a0 - 1 if (aleros[0] if eje == "x" else aleros[2]) else a0
        ea1 = a1 + 1 if (aleros[1] if eje == "x" else aleros[3]) else a1
        eb0 = b0 - 1 if (aleros[2] if eje == "x" else aleros[0]) else b0
        eb1 = b1 + 1 if (aleros[3] if eje == "x" else aleros[1]) else b1
        for b in range(eb0, eb1 + 1):
            d = min(b - (b0 - 1), (b1 + 1) - b)
            y, st = losa(2 * ybase + d - 1)
            for a in range(ea0, ea1 + 1):
                x, z = (a, b) if eje == "x" else (b, a)
                es_hastial = (a == a0 and hastiales[0]) or (a == a1 and hastiales[1])
                if es_hastial and b0 <= b <= b1:
                    for yy in range(ybase, y + 1):
                        self.set(x, yy, z, hastial)
                else:
                    self.set(x, y, z, st)

    def muros_coloniales(self, x0, z0, x1, z1, h, blanco="minecraft:smooth_quartz",
                         zocalo="minecraft:stone_bricks", esquina="minecraft:quartz_pillar[axis=y]"):
        """Muros blanqueados de h de alto con zócalo de piedra y pilastras en las esquinas."""
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    for y in range(FL + 1, FL + h + 1):
                        b = zocalo if y == FL + 1 else blanco
                        if (x in (x0, x1)) and (z in (z0, z1)):
                            b = esquina if y > FL + 1 else zocalo
                        self.set(x, y, z, b)

    def ombu(self, cx, cz):
        """Ombú: tronco enorme con raíces a la vista y una copa ancha y baja (ver estructuras_arboles.py)."""
        import estructuras_arboles as A
        A.arbol(self, cx, cz, "ombu", semilla=cx * 31 + cz)

    # --- conexiones: las plantillas de aldea se colocan sin recalcular formas, así que
    #     los paneles, rejas, cercos y muros deben guardarse ya conectados.
    FULL = ("planks", "bricks", "log", "wood", "terracotta", "stone", "cobblestone", "blackstone",
            "bookshelf", "barrel", "furnace", "smoker", "quartz", "pillar", "granite", "andesite", "hay_block")

    @staticmethod
    def _familia(name):
        if name.endswith("glass_pane") or name == "minecraft:iron_bars":
            return "pane"
        if name.endswith("_fence"):
            return "fence"
        if name.endswith("_wall") and "torch" not in name and "sign" not in name:
            return "wall"
        return None

    def _conecta(self, fam, pos):
        if pos not in self.blocks:
            return False
        name, props = parse_state(self.blocks[pos][0])
        f2 = self._familia(name)
        if f2 is not None:
            return f2 == fam or (fam == "fence" and name.endswith("fence_gate"))
        if name.endswith("fence_gate"):
            return fam in ("fence", "wall")
        if any(k in name for k in ("slab", "stairs", "door", "carpet", "trapdoor", "lantern", "torch", "sign",
                                   "leaves", "chain", "ladder", "bars", "pane", "air", "path", "grass",
                                   "flower", "potted", "campfire", "bell", "lectern", "brewing", "cauldron",
                                   "anotador", "mesa", "silla", "bed", "water")):
            return False
        return any(k in name for k in self.FULL)

    def _conectar(self):
        dirs = {"north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0)}
        nuevos = {}
        for pos, (state, nbt) in self.blocks.items():
            name, props = parse_state(state)
            fam = self._familia(name)
            if fam is None:
                continue
            n = 0
            for d, (dx, dy, dz) in dirs.items():
                c = self._conecta(fam, (pos[0] + dx, pos[1] + dy, pos[2] + dz))
                n += c
                if fam == "wall":
                    props[d] = "low" if c else "none"
                else:
                    props[d] = "true" if c else "false"
            if fam == "wall":
                recto = (props["north"] != "none" and props["south"] != "none" and props["east"] == "none"
                         and props["west"] == "none") or (props["east"] != "none" and props["west"] != "none"
                                                          and props["north"] == "none" and props["south"] == "none")
                props["up"] = "false" if recto else "true"
            props.setdefault("waterlogged", "false")
            nuevos[pos] = (name + "[" + ",".join(f"{k}={v}" for k, v in sorted(props.items())) + "]", nbt)
        self.blocks.update(nuevos)

    def save(self, *p):
        self._conectar()
        palette = []
        index = {}
        blocks = []
        for (x, y, z), (state, nbt) in sorted(self.blocks.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
            if state not in index:
                name, props = parse_state(state)
                entry = {"Name": String(name)}
                if props:
                    entry["Properties"] = Compound({k: String(v) for k, v in props.items()})
                index[state] = len(palette)
                palette.append(Compound(entry))
            b = {"pos": List[Int]([Int(x), Int(y), Int(z)]), "state": Int(index[state])}
            if nbt is not None:
                b["nbt"] = nbt
            blocks.append(Compound(b))
        ents = []
        for x, y, z, nbt in self.entities:
            ents.append(Compound({
                "pos": List[Double]([Double(x), Double(y), Double(z)]),
                "blockPos": List[Int]([Int(int(x)), Int(int(y)), Int(int(z))]),
                "nbt": nbt,
            }))
        root = Compound({
            "DataVersion": Int(DATA_VERSION),
            "size": List[Int]([Int(v) for v in self.size]),
            "palette": List[Compound](palette),
            "blocks": List[Compound](blocks),
            "entities": List[Compound](ents),
        })
        nbtlib.File(root, gzipped=True).save(path("structures", *p))
        return self


def h(n):
    """Altura local n bloques por encima del piso."""
    return FL + n


