"""
Genera el modelo GeckoLib de los gauchos (geometría Bedrock + animaciones), todas las texturas
(varias "pilchas" por cada tipo de personaje) y la clase Java GauchoAspectos con qué partes del
modelo usa cada variante. Diseño original.

Tipos: paisano, paisana, cantinero/pulpera, forastero, Viejo Vizcacha, matrero, peón y payador.
Uso: python3 tools/generar_gaucho.py
"""
import json, os, random
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "trucoya")
JAVA = os.path.join(HERE, "..", "src", "main", "java", "com", "trucoya", "entity", "GauchoAspectos.java")
TW, TH = 128, 64


def path(*p):
    full = os.path.join(ROOT, *p)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    return full


# ---------------------------------------------------------------- geometría
# (nombre, padre, pivote, rotación, [ (origen, tamaño, uv, inflate) ])
BONES = [
    ("root", None, [0, 0, 0], None, []),
    ("body", "root", [0, 24, 0], None, [([-4, 12, -2], [8, 12, 4], [32, 0], 0)]),
    ("poncho", "body", [0, 24, 0], None, [([-4, 14, -2], [8, 10, 4], [0, 16], 0.55)]),
    ("head", "body", [0, 24, 0], None, [([-4, 24, -4], [8, 8, 8], [0, 0], 0)]),
    ("hair_long", "head", [0, 24, 0], None, [([-4, 20, 3.6], [8, 12, 1], [0, 32], 0)]),
    ("hat", "head", [0, 32, 0], None, [([-3.5, 32.5, -3.5], [7, 3, 7], [24, 16], 0)]),
    ("brim", "head", [0, 32, 0], None, [([-6, 31.5, -6], [12, 1, 12], [52, 16], 0)]),
    ("guitar", "body", [0, 18, 3], [0, 0, 35], [([-3, 12, 2.7], [6, 8, 1], [48, 32], 0),
                                               ([-0.5, 20, 2.7], [1, 9, 1], [62, 32], 0)]),
    ("right_arm", "body", [-6, 22, 0], None, [([-8, 12, -2], [4, 12, 4], [56, 0], 0)]),
    ("knife", "right_arm", [-6, 12, 0], None, [([-6.5, 10, -0.5], [1, 3, 1], [68, 32], 0),     # cabo
                                               ([-7, 9, -1], [2, 1, 2], [72, 32], 0),          # gavilán
                                               ([-6.5, 2, -0.5], [1, 7, 1], [80, 32], 0)]),     # hoja
    ("left_arm", "body", [6, 22, 0], None, [([4, 12, -2], [4, 12, 4], [72, 0], 0)]),
    ("right_leg", "root", [-2, 12, 0], None, [([-4, 0, -2], [4, 12, 4], [88, 0], 0)]),
    ("left_leg", "root", [2, 12, 0], None, [([0, 0, -2], [4, 12, 4], [104, 0], 0)]),
    ("skirt", "root", [0, 12, 0], None, [([-4.5, 2, -2.5], [9, 11, 5], [20, 32], 0.1)]),
]

bones = []
for name, parent, pivot, rot, cubes in BONES:
    b = {"name": name, "pivot": pivot}
    if parent:
        b["parent"] = parent
    if rot:
        b["rotation"] = rot
    if cubes:
        b["cubes"] = []
        for origin, size, uv, inflate in cubes:
            c = {"origin": origin, "size": size, "uv": uv}
            if inflate:
                c["inflate"] = inflate
            b["cubes"].append(c)
    bones.append(b)

geo = {
    "format_version": "1.12.0",
    "minecraft:geometry": [{
        "description": {"identifier": "geometry.gaucho", "texture_width": TW, "texture_height": TH,
                        "visible_bounds_width": 2, "visible_bounds_height": 3, "visible_bounds_offset": [0, 1.5, 0]},
        "bones": bones,
    }],
}
with open(path("geo", "gaucho.geo.json"), "w") as f:
    json.dump(geo, f, indent=2)

# ---------------------------------------------------------------- animaciones


def kf(points):
    return {f"{t:.2f}": v for t, v in points}


anims = {
    "animation.gaucho.idle": {"loop": True, "animation_length": 3.0, "bones": {
        "right_arm": {"rotation": kf([(0, [0, 0, 0]), (1.5, [0, 0, 3]), (3, [0, 0, 0])])},
        "left_arm": {"rotation": kf([(0, [0, 0, 0]), (1.5, [0, 0, -3]), (3, [0, 0, 0])])},
        "head": {"rotation": kf([(0, [0, 0, 0]), (1.5, [-2, 0, 0]), (3, [0, 0, 0])])},
    }},
    "animation.gaucho.walk": {"loop": True, "animation_length": 0.8, "bones": {
        "right_leg": {"rotation": kf([(0, [25, 0, 0]), (0.4, [-25, 0, 0]), (0.8, [25, 0, 0])])},
        "left_leg": {"rotation": kf([(0, [-25, 0, 0]), (0.4, [25, 0, 0]), (0.8, [-25, 0, 0])])},
        "right_arm": {"rotation": kf([(0, [-20, 0, 0]), (0.4, [20, 0, 0]), (0.8, [-20, 0, 0])])},
        "left_arm": {"rotation": kf([(0, [20, 0, 0]), (0.4, [-20, 0, 0]), (0.8, [20, 0, 0])])},
        "skirt": {"rotation": kf([(0, [4, 0, 0]), (0.4, [-4, 0, 0]), (0.8, [4, 0, 0])])},
    }},
    "animation.gaucho.talk": {"loop": True, "animation_length": 1.5, "bones": {
        "right_arm": {"rotation": kf([(0, [0, 0, 0]), (0.35, [-70, 0, 12]), (0.75, [-50, 0, 8]),
                                      (1.1, [-70, 0, 12]), (1.5, [0, 0, 0])])},
        "head": {"rotation": kf([(0, [0, 0, 0]), (0.75, [4, 0, 0]), (1.5, [0, 0, 0])])},
    }},
    "animation.gaucho.sit": {"loop": True, "animation_length": 4.0, "bones": {
        "right_leg": {"rotation": [-82, 8, 0]},
        "left_leg": {"rotation": [-82, -8, 0]},
        "skirt": {"rotation": [-60, 0, 0]},
        "right_arm": {"rotation": kf([(0, [-25, 0, 0]), (2, [-28, 0, 0]), (4, [-25, 0, 0])])},
        "left_arm": {"rotation": kf([(0, [-25, 0, 0]), (2, [-22, 0, 0]), (4, [-25, 0, 0])])},
    }},
    "animation.gaucho.sit_talk": {"loop": True, "animation_length": 1.5, "bones": {
        "right_leg": {"rotation": [-82, 8, 0]},
        "left_leg": {"rotation": [-82, -8, 0]},
        "skirt": {"rotation": [-60, 0, 0]},
        "left_arm": {"rotation": [-25, 0, 0]},
        "right_arm": {"rotation": kf([(0, [-25, 0, 0]), (0.35, [-75, 0, 10]), (0.75, [-55, 0, 8]),
                                      (1.1, [-75, 0, 10]), (1.5, [-25, 0, 0])])},
    }},
    "animation.gaucho.attack": {"loop": True, "animation_length": 0.35, "bones": {
        "right_arm": {"rotation": kf([(0, [-150, 0, 15]), (0.2, [-35, 0, -10]), (0.35, [-150, 0, 15])])},
        "body": {"rotation": kf([(0, [0, -8, 0]), (0.2, [0, 10, 0]), (0.35, [0, -8, 0])])},
        "left_arm": {"rotation": [-30, 0, -10]},
    }},
    "animation.gaucho.amenaza": {"loop": True, "animation_length": 1.2, "bones": {
        "right_arm": {"rotation": kf([(0, [-70, 0, 10]), (0.6, [-62, 0, 14]), (1.2, [-70, 0, 10])])},
        "left_arm": {"rotation": [-35, 0, -12]},
        "head": {"rotation": [6, 0, 0]},
        "right_leg": {"rotation": [-8, 0, 4]},
        "left_leg": {"rotation": [10, 0, -4]},
    }},
    "animation.gaucho.amenaza_walk": {"loop": True, "animation_length": 0.6, "bones": {
        "right_leg": {"rotation": kf([(0, [30, 0, 0]), (0.3, [-30, 0, 0]), (0.6, [30, 0, 0])])},
        "left_leg": {"rotation": kf([(0, [-30, 0, 0]), (0.3, [30, 0, 0]), (0.6, [-30, 0, 0])])},
        "right_arm": {"rotation": [-70, 0, 10]},
        "left_arm": {"rotation": kf([(0, [25, 0, -8]), (0.3, [-25, 0, -8]), (0.6, [25, 0, -8])])},
        "skirt": {"rotation": kf([(0, [5, 0, 0]), (0.3, [-5, 0, 0]), (0.6, [5, 0, 0])])},
    }},
    "animation.gaucho.ride": {"loop": True, "animation_length": 1.0, "bones": {
        "right_leg": {"rotation": [-70, 25, 0]},
        "left_leg": {"rotation": [-70, -25, 0]},
        "skirt": {"rotation": [-55, 0, 0]},
        "right_arm": {"rotation": kf([(0, [-35, 0, 0]), (0.5, [-40, 0, 0]), (1, [-35, 0, 0])])},
        "left_arm": {"rotation": kf([(0, [-35, 0, 0]), (0.5, [-30, 0, 0]), (1, [-35, 0, 0])])},
    }},
}
with open(path("animations", "gaucho.animation.json"), "w") as f:
    json.dump({"format_version": "1.8.0", "animations": anims}, f, indent=2)

# ---------------------------------------------------------------- utilidades de pintura


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (c[3],)


def mix(a, b, t):
    return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3)) + (255,)


CLEAR = (0, 0, 0, 0)
LUZ = {"up": 1.10, "north": 1.0, "south": 0.88, "east": 0.94, "west": 0.94, "down": 0.78}


class Painter:
    def __init__(self, seed):
        self.img = Image.new("RGBA", (TW, TH), CLEAR)
        self.px = self.img.load()
        self.r = random.Random(seed)
        self.k = 1.0  # luz de la cara actual

    def put(self, x, y, color, noise=0.0):
        if color is None:
            return
        if color[3] == 0:
            self.px[x, y] = CLEAR
            return
        k = self.k * (1 + (self.r.uniform(-noise, noise) if noise else 0))
        self.px[x, y] = shade(color, k)

    def rect(self, x, y, w, h, color, noise=0.05):
        for j in range(h):
            for i in range(w):
                self.put(x + i, y + j, color, noise)

    @staticmethod
    def faces(u, v, w, h, d):
        return {
            "up": (u + d, v, w, d), "down": (u + d + w, v, w, d),
            "east": (u, v + d, d, h), "north": (u + d, v + d, w, h),
            "west": (u + d + w, v + d, d, h), "south": (u + 2 * d + w, v + d, w, h),
        }

    def box(self, u, v, w, h, d, fn):
        """Llama fn(cara, x, y, ancho, alto) con la luz de cada cara ya puesta."""
        for name, (x, y, fw, fh) in self.faces(u, v, w, h, d).items():
            self.k = LUZ[name]
            fn(name, x, y, fw, fh)
        self.k = 1.0


SIDES = ("north", "south", "east", "west")

# ---------------------------------------------------------------- paleta de pilchas

PIELES = ["#E3B994", "#D4A77C", "#C99A6E", "#B9875C", "#A57449", "#8E6040"]
PELOS = ["#1A1410", "#2E2018", "#4A3222", "#6B4A2E", "#8A8A86", "#C8C8C4"]


def V(**kw):
    out = dict(mujer=False, sombrero=True, ala=True, pelo=False, pollera=False, guitarra=False,
               piel="#C99A6E", pelo_c="#2E2018", cara="bigote", ojos="#3A2A1A",
               camisa="#E8E2D6", chaleco=None, faja="#7A1E22", rastra=False, panuelo=None,
               poncho="#9B2D2D", guarda="#2A1A16", dibujo="rayas",
               bombacha="#4A4A50", chiripa=None, botas="#5A3A22", potro=False, espuelas=False,
               sombrero_c="#2A2420", cinta="#6B4423", boina=False, vincha=None,
               pollera_c="#6B2A3A", delantal=None, rebozo=None, panuelo_cabeza=None,
               remiendos=False, mascara=None, delantal_pulpero=None, lentes=None,
               rulos=False, flequillo=False, pecas=False, frente=0, sonrisa=False)
    out.update(kw)
    return out


# El orden de los roles tiene que coincidir con GauchoEntity.Rol
ROLES = {
    "paisano": [
        V(),
        V(piel=PIELES[1], pelo_c=PELOS[3], cara="barba", camisa="#F0EBDD", chaleco="#3B2A20", faja="#2F4A6B",
          poncho=None, bombacha="#6B5A44", botas="#D9C3A0", potro=True, sombrero_c="#4A3020", cinta="#1A1410",
          panuelo="#C8A23A", rastra=True),
        V(piel=PIELES[3], pelo_c=PELOS[0], poncho="#E8DCC0", guarda="#5A2A1A", dibujo="pampa", faja="#3A2A20",
          bombacha="#2A2A2E", sombrero_c="#1E1A18", cinta="#1E1A18", espuelas=True),
        V(piel=PIELES[4], pelo_c=PELOS[1], cara="chiva", camisa="#D8CFC0", poncho="#2F4A6B", guarda="#E8DCC0",
          dibujo="guarda", bombacha="#5A4A3A", sombrero_c="#6B4A2E", cinta="#2A1A10", panuelo="#B3262C"),
        V(piel=PIELES[2], pelo_c=PELOS[2], cara="lampino", camisa="#E8E2D6", chaleco="#2A2420", poncho=None,
          faja="#9B2D2D", chiripa="#3A3A3E", botas="#D9C3A0", potro=True, sombrero_c="#3A3634",
          cinta="#9B2D2D", panuelo="#E8E2D6"),
        V(piel=PIELES[5], pelo_c=PELOS[0], cara="bigote", poncho="#6B3A2A", guarda="#D9B35A", dibujo="rayas",
          bombacha="#3A3A3E", sombrero_c="#C9B48A", cinta="#3A2A1A", rastra=True, espuelas=True),
    ],
    "cantinero": [
        V(piel=PIELES[1], pelo_c=PELOS[2], camisa="#EDE6D6", faja="#2F4A6B", poncho=None, chaleco="#2F4A2F",
          bombacha="#6B4A2E", botas="#3A2A1A", ala=False, boina=True, sombrero_c="#23283A", cinta="#23283A",
          delantal_pulpero="#F4F1EA"),
        V(piel=PIELES[2], pelo_c=PELOS[4], cara="barba", camisa="#F0EBDD", poncho=None, chaleco="#5A2A1A",
          faja="#1E1A18", bombacha="#3A3A3E", ala=False, boina=True, sombrero_c="#1E1A18", cinta="#1E1A18",
          delantal_pulpero="#E8DCC0"),
        V(mujer=True, sombrero=False, ala=False, pelo=True, pollera=True, piel=PIELES[0], pelo_c=PELOS[2],
          cara="mujer", camisa="#EDE6D6", poncho=None, rebozo="#2F4A6B", pollera_c="#3B2A20",
          delantal="#F4F1EA", panuelo_cabeza=None, faja="#7A1E22"),
    ],
    "forastero": [
        V(piel=PIELES[3], pelo_c=PELOS[0], camisa="#D8CFC0", faja="#B3262C", poncho="#B0802E", guarda="#E8D8B0",
          dibujo="rayas", bombacha="#3A3A3E", botas="#1E1A18", sombrero_c="#4A3020", cinta="#2A1A10",
          panuelo="#B3262C", espuelas=True),
        V(piel=PIELES[4], pelo_c=PELOS[1], cara="barba", poncho="#3A3A3E", guarda="#B3262C", dibujo="pampa",
          bombacha="#2A2A2E", botas="#D9C3A0", potro=True, sombrero_c="#1E1A18", cinta="#B3262C",
          panuelo="#E8E2D6", espuelas=True),
        V(piel=PIELES[2], pelo_c=PELOS[3], cara="chiva", poncho="#5A6B3A", guarda="#E8DCC0", dibujo="guarda",
          chiripa="#4A3A2A", botas="#5A3A22", sombrero_c="#8A6A44", cinta="#2A1A10", panuelo="#2F4A6B"),
        V(piel=PIELES[5], pelo_c=PELOS[0], cara="bigote", poncho="#7A1E22", guarda="#1A1410", dibujo="pampa",
          bombacha="#4A4A50", botas="#1E1A18", sombrero_c="#2A2420", cinta="#C8A23A", rastra=True,
          espuelas=True),
    ],
    "vizcacha": [
        V(piel="#C2936A", pelo_c="#B8B8B4", cara="barba_blanca", camisa="#9A9A96", faja="#4E3A2A",
          poncho="#6E6E6E", guarda="#4A4A4A", dibujo="rayas", remiendos=True, bombacha="#2A2A2C",
          botas="#3A2A1E", sombrero_c="#3A3634", cinta="#5A4A3A"),
    ],
    "paisana": [
        V(mujer=True, sombrero=False, ala=False, pelo=True, pollera=True, piel=PIELES[1], pelo_c=PELOS[0],
          cara="mujer", camisa="#F0EBDD", poncho=None, rebozo="#7A1E22", pollera_c="#2F4A6B",
          delantal="#E8E2D6", panuelo_cabeza="#B3262C", faja="#2A1A16"),
        V(mujer=True, sombrero=True, ala=True, pelo=True, pollera=True, piel=PIELES[3], pelo_c=PELOS[1],
          cara="mujer", camisa="#E8DCC0", poncho=None, rebozo="#5A6B3A", pollera_c="#6B2A3A",
          sombrero_c="#C9B48A", cinta="#7A1E22", faja="#3A2A20"),
        V(mujer=True, sombrero=False, ala=False, pelo=True, pollera=True, piel=PIELES[0], pelo_c=PELOS[3],
          cara="mujer", camisa="#EDE6D6", poncho=None, rebozo="#3A3A3E", pollera_c="#7A5A2A",
          delantal="#F4F1EA", panuelo_cabeza="#2F4A6B", faja="#7A1E22"),
        V(mujer=True, sombrero=False, ala=False, pelo=True, pollera=True, piel=PIELES[4], pelo_c=PELOS[2],
          cara="mujer", camisa="#D8CFC0", poncho=None, rebozo="#9B2D2D", pollera_c="#1E1A18",
          panuelo_cabeza=None, faja="#C8A23A"),
    ],
    "matrero": [
        V(piel=PIELES[3], pelo_c=PELOS[0], cara="barba", camisa="#5A5048", poncho="#2A2420", guarda="#7A1E22",
          dibujo="guarda", bombacha="#2A2A2E", botas="#1E1A18", sombrero_c="#1A1410", cinta="#7A1E22",
          panuelo="#7A1E22", mascara="#7A1E22", espuelas=True),
        V(piel=PIELES[5], pelo_c=PELOS[0], cara="chiva", camisa="#3A3A3E", chaleco="#1A1410", poncho=None,
          faja="#B3262C", bombacha="#3A2A20", botas="#D9C3A0", potro=True, sombrero_c="#3A2A20",
          cinta="#1A1410", panuelo="#1A1410", rastra=True),
        V(piel=PIELES[2], pelo_c=PELOS[2], cara="barba", camisa="#6B5A44", poncho="#4A3A2A", guarda="#1A1410",
          dibujo="rayas", chiripa="#2A2420", botas="#5A3A22", sombrero_c="#2A2420", cinta="#2A2420",
          mascara="#2A2A2E", panuelo="#2A2A2E"),
        V(piel=PIELES[4], pelo_c=PELOS[1], cara="bigote", camisa="#4A4A50", poncho="#1E1A18", guarda="#C8A23A",
          dibujo="pampa", bombacha="#1E1A18", botas="#1E1A18", sombrero_c="#1E1A18", cinta="#C8A23A",
          rastra=True, espuelas=True),
    ],
    "peon": [
        V(piel=PIELES[3], pelo_c=PELOS[0], cara="lampino", camisa="#D8CFC0", poncho=None, faja="#2F4A6B",
          chiripa="#6B5A44", botas="#D9C3A0", potro=True, sombrero=False, ala=False, vincha="#B3262C"),
        V(piel=PIELES[2], pelo_c=PELOS[2], cara="bigote", camisa="#C9B48A", poncho="#7A5A2A", guarda="#3A2A1A",
          dibujo="rayas", bombacha="#5A4A3A", botas="#5A3A22", sombrero_c="#C9B48A", cinta="#3A2A1A"),
        V(piel=PIELES[4], pelo_c=PELOS[0], cara="bigote", camisa="#E8E2D6", chaleco="#5A4A3A", poncho=None,
          faja="#9B2D2D", bombacha="#3A3A3E", botas="#3A2A1A", sombrero=True, ala=False, boina=True,
          sombrero_c="#1E1A18", cinta="#1E1A18"),
        V(piel=PIELES[1], pelo_c=PELOS[3], cara="barba", camisa="#B8B0A0", poncho="#5A6B3A", guarda="#E8DCC0",
          dibujo="guarda", chiripa="#3A3A3E", botas="#D9C3A0", potro=True, sombrero_c="#6B4A2E",
          cinta="#2A1A10", panuelo="#E8E2D6"),
    ],
    "payador": [
        V(guitarra=True, piel=PIELES[2], pelo_c=PELOS[0], cara="bigote", camisa="#F0EBDD", chaleco="#1A1410",
          poncho=None, faja="#7A1E22", bombacha="#1E1A18", botas="#1E1A18", sombrero_c="#1E1A18",
          cinta="#7A1E22", panuelo="#E8E2D6", rastra=True, espuelas=True),
        V(guitarra=True, piel=PIELES[4], pelo_c=PELOS[4], cara="barba", camisa="#E8DCC0", poncho="#2F4A6B",
          guarda="#E8DCC0", dibujo="pampa", bombacha="#3A3A3E", botas="#5A3A22", sombrero_c="#4A3020",
          cinta="#2F4A6B"),
    ],
}
# Los Fundadores: los cuatro creadores del mod (guías de la historia), con su pinta y pilcha gaucha.
ROLES["fundador"] = [
    # Nicolás Bentancour: rulos castaño oscuro, pecas, buzo verde agua -> poncho verde agua
    V(sombrero=False, ala=False, piel="#E6BE9C", pelo_c="#3A2618", cara="lampino", rulos=True, pecas=True, frente=2, sonrisa=True,
      ojos="#4A2E1A", camisa="#1E2228", poncho="#6E9C9A", guarda="#E8E2D6", dibujo="guarda", faja="#2A3A44",
      bombacha="#3A3A40", botas="#3A2A1A", panuelo="#E8E2D6"),
    # Agustín Perdomo: rubio, flaco, polo azul marino -> chaleco y camisa azul marino
    V(sombrero=False, ala=False, piel="#EEC9AA", pelo_c="#B88A4E", cara="lampino", ojos="#5A7A8A", frente=2,
      camisa="#1E2A44", chaleco="#16203A", poncho=None, faja="#E8E2D6", bombacha="#1A1A1E", botas="#2A1E16",
      panuelo="#C8A23A", rastra=True),
    # Matías Piñeyro: pelo oscuro, lentes de armazón negro, buzo negro -> poncho negro
    V(sombrero=False, ala=False, piel="#DDB08A", pelo_c="#241A14", cara="lampino", lentes="#121212", frente=2,
      ojos="#2A1A10", camisa="#2A2A2E", poncho="#1C1C20", guarda="#7A1E22", dibujo="pampa", faja="#7A1E22",
      bombacha="#2E3440", botas="#1A1410", panuelo="#7A1E22"),
    # Thiago Rodríguez: pelo oscuro lacio con flequillo, cara redonda, campera azul -> poncho azul
    V(sombrero=False, ala=False, piel="#EAC2A2", pelo_c="#1E1612", cara="lampino", flequillo=True,
      ojos="#3A2A1A", camisa="#E8E2D6", poncho="#23365A", guarda="#C8A23A", dibujo="rayas", faja="#C8A23A",
      bombacha="#2A2A30", botas="#3A2A1A", espuelas=True),
]
# La Salamanca: el Mandinga (el diablo de las leyendas del campo, de gaucho elegante y oscuro) y el
# fantasma del Negro Ledesma (la misma pilcha con la que cayó en el Paso de las Ánimas: ver la cinemática).
ROLES["mandinga"] = [
    V(piel="#D9BCA4", pelo_c="#0E0A08", cara="chiva", ojos="#FF2A1A", camisa="#1A1414", chaleco="#4A0A10",
      poncho="#141012", guarda="#B3121A", dibujo="guarda", faja="#B3121A", rastra=True, bombacha="#141012",
      botas="#0E0A08", espuelas=True, sombrero_c="#0E0A08", cinta="#B3121A", panuelo="#B3121A"),
]
ROLES["ledesma"] = [dict(ROLES["matrero"][1])]
ORDEN = ["paisano", "cantinero", "forastero", "vizcacha", "paisana", "matrero", "peon", "payador", "fundador",
         "mandinga", "ledesma"]

# ---------------------------------------------------------------- pintura de una variante


def paint(role, idx, v):
    C = {k: (hexc(val) if isinstance(val, str) and val.startswith("#") else val) for k, val in v.items()}
    p = Painter(sum(ord(ch) for ch in role) * 31 + idx)
    skin, hair = C["piel"], C["pelo_c"]

    # ---- cabeza (0,0) 8x8x8
    def cabeza(face, x, y, w, h):
        p.rect(x, y, w, h, skin, 0.035)
        if face == "up":
            p.rect(x, y, w, h, hair, 0.08)
        elif face in ("east", "west"):
            largo = 7 if v["mujer"] else 3
            p.rect(x, y, w, largo, hair, 0.08)
            if not v["mujer"]:
                p.put(x + (w - 2 if face == "east" else 1), y + 3, hair, 0.08)   # patilla
            p.put(x + (3 if face == "east" else 4), y + 4, shade(skin, 0.85))     # oreja
        elif face == "south":
            p.rect(x, y, w, 8 if v["mujer"] else 5, hair, 0.08)
        elif face == "north":
            p.rect(x, y, w, 1, hair, 0.08)                                      # pelo de arriba
            if v["mujer"]:
                p.rect(x, y + 1, 2, 1, hair, 0.08)
                p.rect(x + 6, y + 1, 2, 1, hair, 0.08)
                for yy in range(2, 8):                                           # pelo a los costados
                    p.put(x, y + yy, hair, 0.08)
                    p.put(x + 7, y + yy, hair, 0.08)
            else:
                p.rect(x + 1, y + 1, 6, 1, hair, 0.08) if p.r.random() < 0.5 else p.rect(x, y + 1, 3, 1, hair, 0.08)
            ceja = shade(hair, 0.8)
            for ex in (1, 5):
                p.put(x + ex, y + 3, ceja)
                p.put(x + ex + 1, y + 3, ceja)
                p.put(x + ex, y + 4, hexc("#F2F2F2"))
                p.put(x + ex + 1, y + 4, C["ojos"])
            p.put(x + 3, y + 5, shade(skin, 0.86))
            p.put(x + 4, y + 5, shade(skin, 0.80))
            if C["lentes"]:                                                      # lentes: armazón alrededor de los ojos
                for ex in (1, 5):
                    p.put(x + ex, y + 3, C["lentes"])
                    p.put(x + ex + 1, y + 3, C["lentes"])
                for bx in (0, 3, 4, 7):
                    p.put(x + bx, y + 4, C["lentes"])
            cara = v["cara"]
            oscuro = shade(hair, 0.9)
            if cara == "mujer":
                p.put(x + 1, y + 5, mix(skin, hexc("#D9776A"), 0.35))
                p.put(x + 6, y + 5, mix(skin, hexc("#D9776A"), 0.35))
                p.put(x + 3, y + 6, hexc("#A8463E"))
                p.put(x + 4, y + 6, hexc("#A8463E"))
                p.put(x + 1, y + 3, shade(hair, 0.7))
                p.put(x + 6, y + 3, shade(hair, 0.7))
            elif cara in ("bigote", "barba", "chiva", "barba_blanca"):
                must = hexc("#E6E6E2") if cara == "barba_blanca" else oscuro
                for mx in range(2, 6):
                    p.put(x + mx, y + 6, must)
                p.put(x + 1, y + 7, must) if cara == "bigote" and p.r.random() < 0.5 else None
                if cara in ("barba", "barba_blanca"):
                    for by in (6, 7):
                        for bx in range(0, 8):
                            if not (by == 6 and 2 <= bx <= 5):
                                p.put(x + bx, y + by, must, 0.08)
                    p.put(x + 3, y + 7, shade(skin, 0.7))
                    p.put(x + 4, y + 7, shade(skin, 0.7))
                if cara == "chiva":
                    p.put(x + 3, y + 7, oscuro)
                    p.put(x + 4, y + 7, oscuro)
            else:  # lampiño
                p.put(x + 3, y + 6, shade(skin, 0.72))
                p.put(x + 4, y + 6, shade(skin, 0.72))
            if C["mascara"]:                                                     # pañuelo de matrero
                for yy in (5, 6, 7):
                    for xx in range(8):
                        p.put(x + xx, y + yy, C["mascara"], 0.06)
                p.put(x + 3, y + 7, shade(C["mascara"], 0.75))
                p.put(x + 4, y + 7, shade(C["mascara"], 0.75))
        if face == "down":
            p.rect(x, y, w, h, shade(skin, 0.9), 0.03)
            if v["cara"] in ("barba", "barba_blanca"):
                p.rect(x, y, w, 3, hexc("#E6E6E2") if v["cara"] == "barba_blanca" else shade(hair, 0.9), 0.08)
        # pañuelo en la cabeza (paisanas) o vincha (peones)
        if C["panuelo_cabeza"]:
            pc = C["panuelo_cabeza"]
            if face == "up":
                p.rect(x, y, w, h, pc, 0.05)
                for i in range(0, 8, 3):
                    p.put(x + i, y + (i * 5) % 8, hexc("#F2F2F2"))
            elif face in ("east", "west", "south"):
                p.rect(x, y, w, 3, pc, 0.05)
                p.put(x + 2, y + 1, hexc("#F2F2F2"))
                if face == "south":
                    p.rect(x + 3, y + 3, 2, 2, shade(pc, 0.8))                   # nudo
            elif face == "north":
                p.rect(x, y, w, 1, pc, 0.05)
        if C["vincha"] and face in SIDES:
            p.rect(x, y + 2, w, 1, C["vincha"], 0.05)
        if v["flequillo"] and face == "north":                                 # flequillo recto (corte "taza")
            p.rect(x, y, w, 3, hair, 0.07)
            p.put(x + 2, y + 3, shade(hair, 0.85))
            p.put(x + 5, y + 3, shade(hair, 0.85))
        if v["flequillo"] and face in ("east", "west"):
            p.rect(x, y, w, 4, hair, 0.07)
        if v["rulos"] and face in ("up", "north", "east", "west", "south"):   # rulos: pelo más alto y desparejo
            alto = {"up": h, "north": 2, "east": 4, "west": 4, "south": 6}[face]
            for yy in range(alto):
                for xx in range(w):
                    if face == "north" and yy == 1 and xx in (2, 5):
                        continue
                    p.put(x + xx, y + yy, shade(hair, 0.78 if (xx + yy * 3) % 4 == 0 else 1.08 if (xx * 2 + yy) % 5 == 0 else 1.0))
        if v["frente"] and face == "north":                                  # pelo que cae sobre la frente
            p.rect(x, y, w, v["frente"], hair, 0.07)
            if v["rulos"]:                                                       # rulos: borde desparejo
                for xx in (0, 1, 3, 4, 6, 7):
                    p.put(x + xx, y + v["frente"], shade(hair, 0.9 if xx % 3 else 1.1))
        if v["sonrisa"] and face == "north":                                  # sonrisa grande, con dientes
            p.put(x + 2, y + 6, hexc("#9A4A44"))
            p.put(x + 3, y + 6, hexc("#F4F0E6"))
            p.put(x + 4, y + 6, hexc("#F4F0E6"))
            p.put(x + 5, y + 6, hexc("#9A4A44"))
        if v["pecas"] and face == "north":
            for (px_, py_) in ((1, 5), (2, 6), (5, 5), (6, 6), (3, 5)):
                p.put(x + px_, y + py_, mix(skin, hexc("#8A5A3A"), 0.35))

    p.box(0, 0, 8, 8, 8, cabeza)

    # ---- pelo largo / trenza (0,32) 8x12x1
    def pelo_largo(face, x, y, w, h):
        p.rect(x, y, w, h, hair, 0.09)
        if face == "south":
            for yy in range(h):                                                 # trenza central
                p.put(x + 3 + (yy % 2), y + yy, shade(hair, 0.75))
            p.rect(x, y + h - 3, 2, 3, CLEAR, 0)
            p.rect(x + w - 2, y + h - 3, 2, 3, CLEAR, 0)
            p.put(x + 3, y + h - 1, C["faja"])                                  # moño
            p.put(x + 4, y + h - 1, C["faja"])
        if face == "north":
            p.rect(x + 1, y, w - 2, 4, CLEAR, 0)  # no se ve (queda pegado a la nuca)
    p.box(0, 32, 8, 12, 1, pelo_largo)

    # ---- cuerpo (32,0) 8x12x4
    def cuerpo(face, x, y, w, h):
        p.rect(x, y, w, h, C["camisa"], 0.05)
        if face in SIDES:
            if face == "north":
                p.put(x + 3, y + 2, shade(C["camisa"], 0.85))
                p.put(x + 3, y + 5, shade(C["camisa"], 0.85))
                p.put(x + 3, y + 8, shade(C["camisa"], 0.85))
            if C["chaleco"]:
                if face == "north":
                    p.rect(x, y, 3, 9, C["chaleco"], 0.05)
                    p.rect(x + 5, y, 3, 9, C["chaleco"], 0.05)
                    p.put(x + 2, y + 4, hexc("#C8C8C0"))
                    p.put(x + 2, y + 7, hexc("#C8C8C0"))
                else:
                    p.rect(x, y, w, 9, C["chaleco"], 0.05)
            if C["panuelo"] and face in ("north", "south"):
                p.rect(x + 1, y, w - 2, 1, C["panuelo"], 0.04)
                if face == "north":
                    p.rect(x + 3, y + 1, 2, 2, C["panuelo"], 0.04)
                    p.put(x + 3, y + 3, shade(C["panuelo"], 0.8))
            if C["delantal_pulpero"] and face == "north":
                p.rect(x + 1, y + 5, w - 2, 7, C["delantal_pulpero"], 0.03)
            if v["mujer"]:
                if face == "north":
                    p.rect(x + 2, y, 4, 1, shade(C["camisa"], 0.85))           # escote
            p.rect(x, y + 9, w, 2, C["faja"], 0.06)
            if C["rastra"] and face == "north":
                for rx in (1, 3, 4, 6):
                    p.put(x + rx, y + 10, hexc("#D8D8D0"))
                p.rect(x + 3, y + 9, 2, 2, hexc("#E8E4D0"))
            p.rect(x, y + 11, w, 1, C["chiripa"] or C["bombacha"] if not v["pollera"] else C["pollera_c"], 0.05)
            if C["delantal"] and face == "north":
                p.rect(x + 1, y + 10, w - 2, 2, C["delantal"], 0.03)
    p.box(32, 0, 8, 12, 4, cuerpo)

    # ---- brazos 4x12x4
    manga = C["camisa"]
    for u in (56, 72):
        def brazo(face, x, y, w, h):
            p.rect(x, y, w, h, manga, 0.05)
            if face in SIDES:
                if C["rebozo"]:
                    p.rect(x, y, w, 4, C["rebozo"], 0.05)
                p.rect(x, y + 9, w, 1, shade(manga, 0.85), 0.03)               # puño
                p.rect(x, y + 10, w, 2, skin, 0.035)
            if face == "down":
                p.rect(x, y, w, h, skin, 0.035)
            if face == "up" and C["rebozo"]:
                p.rect(x, y, w, h, C["rebozo"], 0.05)
        p.box(u, 0, 4, 12, 4, brazo)

    # ---- piernas 4x12x4
    for u in (88, 104):
        def pierna(face, x, y, w, h):
            tela = C["bombacha"]
            p.rect(x, y, w, h, tela, 0.07)
            if face in SIDES:
                if C["chiripa"]:
                    p.rect(x, y, w, 5, C["chiripa"], 0.06)                      # chiripá
                    for xx in range(w):                                         # calzoncillo cribado
                        p.put(x + xx, y + 5, hexc("#F2EEE4"))
                        p.put(x + xx, y + 6, hexc("#F2EEE4") if xx % 2 else hexc("#E0D8C8"))
                        p.put(x + xx, y + 7, hexc("#E8E2D6"))
                else:
                    p.rect(x, y + 3, w, 1, shade(tela, 0.8), 0.03)              # pliegue
                    p.put(x + 1, y + 6, shade(tela, 0.82))
                if v["mujer"]:
                    p.rect(x, y + 7, w, 3, hexc("#E8E2D6"), 0.03)               # medias
                    p.rect(x, y + 10, w, 2, hexc("#2A2018"), 0.05)               # zapatos
                else:
                    p.rect(x, y + 8, w, 4, C["botas"], 0.06)
                    if v["potro"] and face == "north":
                        p.rect(x + 1, y + 11, 2, 1, skin, 0.03)                 # dedos (bota de potro)
                    if v["espuelas"] and face == "south":
                        p.put(x + 1, y + 10, hexc("#D8D8D0"))
                        p.put(x + 2, y + 10, hexc("#B8B8B0"))
            if face == "down":
                p.rect(x, y, w, h, C["botas"] if not v["mujer"] else hexc("#2A2018"), 0.05)
        p.box(u, 0, 4, 12, 4, pierna)

    # ---- poncho / rebozo (0,16) 8x10x4 (inflado)
    def poncho(face, x, y, w, h):
        if C["rebozo"]:
            reb = C["rebozo"]
            if face in SIDES:
                p.rect(x, y, w, h, CLEAR, 0)
                p.rect(x, y, w, 4, reb, 0.05)
                if face == "north":
                    p.rect(x + 2, y, 4, 2, CLEAR, 0)
                    p.rect(x + 3, y + 4, 2, 2, reb, 0.05)                       # puntas cruzadas
                for fx in range(0, w, 2):
                    p.put(x + fx, y + 4, shade(reb, 0.75))                       # flecos
            elif face == "up":
                p.rect(x, y, w, h, reb, 0.05)
                p.rect(x + 2, y + 1, 4, 2, CLEAR, 0)
            else:
                p.rect(x, y, w, h, CLEAR, 0)
            return
        if not C["poncho"]:
            p.rect(x, y, w, h, CLEAR, 0)
            return
        base, g = C["poncho"], C["guarda"]
        if face == "down":
            p.rect(x, y, w, h, CLEAR, 0)
            return
        p.rect(x, y, w, h, base, 0.06)
        if face == "up":
            p.rect(x + 2, y + 1, 4, 2, shade(base, 0.6))                       # boca del poncho
            return
        dib = v["dibujo"]
        if dib == "rayas":
            for sy in (2, 6):
                p.rect(x, y + sy, w, 1, g, 0.04)
            for sx in range(0, w, 3 if w > 4 else 2):
                p.put(x + sx, y + 4, shade(g, 1.2))
        elif dib == "guarda":
            p.rect(x, y + 6, w, 3, g, 0.04)
            for gx in range(w):                                                 # greca
                p.put(x + gx, y + 7 - (1 if gx % 4 in (1, 2) else 0), base)
        elif dib == "pampa":
            for gx in range(w):                                                 # rombos pampa
                for gy in range(3, 8):
                    if abs((gx % 4) - 1.5) + abs(gy - 5) <= 1.6:
                        p.put(x + gx, y + gy, g, 0.04)
            p.rect(x, y + 1, w, 1, g, 0.04)
        for fx in range(w):                                                     # flecos
            if (fx + (1 if face in ("east", "west") else 0)) % 2 == 0:
                p.put(x + fx, y + h - 1, CLEAR)
            else:
                p.put(x + fx, y + h - 1, shade(g, 0.9))
        if C["remiendos"] and face in ("north", "south"):
            for _ in range(5):
                px_, py_ = x + p.r.randrange(w - 2), y + p.r.randrange(h - 3)
                p.rect(px_, py_, 2, 2, hexc("#4E4E4E"), 0.05)
                p.put(px_, py_, hexc("#8A7A5A"))
        if face == "north" and C["delantal_pulpero"]:
            p.rect(x, y, w, h, CLEAR, 0)
    p.box(0, 16, 8, 10, 4, poncho)

    # ---- sombrero: copa (24,16) 7x3x7 y ala (52,16) 12x1x12
    hat = C["sombrero_c"]
    def copa(face, x, y, w, h):
        p.rect(x, y, w, h, hat, 0.05)
        if face in SIDES and not v["boina"]:
            p.rect(x, y + 2, w, 1, C["cinta"], 0.03)
            if face == "east":
                p.put(x + 2, y + 2, hexc("#D8D8D0"))
        if face == "up":
            if v["boina"]:
                p.put(x + 3, y + 3, shade(hat, 1.6))
            else:
                p.rect(x + 1, y + 1, w - 2, w - 2, shade(hat, 0.92), 0.03)
    p.box(24, 16, 7, 3, 7, copa)

    def ala(face, x, y, w, h):
        p.rect(x, y, w, h, hat, 0.06)
        if face == "up":
            p.rect(x, y, w, 1, shade(hat, 0.85))
            p.rect(x, y + h - 1, w, 1, shade(hat, 0.85))
            p.rect(x, y, 1, h, shade(hat, 0.85))
            p.rect(x + w - 1, y, 1, h, shade(hat, 0.85))
    p.box(52, 16, 12, 1, 12, ala)

    # ---- pollera (20,32) 9x11x5
    def pollera(face, x, y, w, h):
        if not v["pollera"]:
            p.rect(x, y, w, h, CLEAR, 0)
            return
        pc = C["pollera_c"]
        p.rect(x, y, w, h, pc, 0.06)
        if face in SIDES:
            for yy in range(1, h, 3):
                for xx in range(0, w, 2):
                    p.put(x + xx, y + yy, shade(pc, 0.85))                      # pliegues
            p.rect(x, y + h - 2, w, 1, shade(pc, 1.25), 0.03)                  # volado
            p.rect(x, y + h - 1, w, 1, hexc("#F2EEE4"), 0.03)                  # enagua
            if face == "north" and C["delantal"]:
                p.rect(x + 2, y, w - 4, h - 2, C["delantal"], 0.03)
        if face == "down":
            p.rect(x, y, w, h, CLEAR, 0)
    p.box(20, 32, 9, 11, 5, pollera)

    # ---- guitarra (48,32) 6x8x1 + mástil (62,32) 1x9x1
    madera, oscuro = hexc("#B07A3A"), hexc("#4A2A14")
    def caja(face, x, y, w, h):
        if not v["guitarra"]:
            p.rect(x, y, w, h, CLEAR, 0)
            return
        p.rect(x, y, w, h, madera, 0.05)
        if face in ("north", "south"):
            p.rect(x, y, w, 1, shade(madera, 0.75))
            p.put(x, y + 3, CLEAR)
            p.put(x + w - 1, y + 3, CLEAR)                                     # cintura de la guitarra
            p.put(x, y, CLEAR)
            p.put(x + w - 1, y, CLEAR)
            if face == "south":
                p.rect(x + 2, y + 2, 2, 2, oscuro)                              # boca
                p.rect(x + 2, y + 6, 2, 1, oscuro)                              # puente
    p.box(48, 32, 6, 8, 1, caja)

    def mastil(face, x, y, w, h):
        if not v["guitarra"]:
            p.rect(x, y, w, h, CLEAR, 0)
            return
        p.rect(x, y, w, h, oscuro, 0.05)
        if face in SIDES:
            p.rect(x, y, w, 2, shade(madera, 0.8))                              # clavijero
    p.box(62, 32, 1, 9, 1, mastil)

    # ---- facón de los guardias: cabo (68,32) 1x3x1, gavilán (72,32) 2x1x2, hoja (80,32) 1x7x1
    cuero, oro, acero = hexc("#4A2E1A"), hexc("#D9B54A"), hexc("#D8DCE0")
    def cabo(face, x, y, w, h):
        p.rect(x, y, w, h, cuero, 0.08)
        if face in SIDES:
            p.put(x, y + h - 1, hexc("#C8C8C0"))                                # virola
    p.box(68, 32, 1, 3, 1, cabo)
    p.box(72, 32, 2, 1, 2, lambda f, x, y, w, h: p.rect(x, y, w, h, oro, 0.06))
    def hoja(face, x, y, w, h):
        for yy in range(h):
            p.rect(x, y + yy, w, 1, shade(acero, 1.05 - yy * 0.03), 0.02)
        if face in SIDES:
            p.put(x, y + h - 1, hexc("#F4F6F8"))                                # punta
    p.box(80, 32, 1, 7, 1, hoja)

    p.img.save(path("textures", "entity", f"gaucho_{role}_{idx}.png"))


# ---------------------------------------------------------------- salida

for old in os.listdir(os.path.join(ROOT, "textures", "entity")):
    if old.startswith("gaucho_") and old.endswith(".png"):
        os.remove(os.path.join(ROOT, "textures", "entity", old))

flags_java = []
for role in ORDEN:
    fila = []
    for i, v in enumerate(ROLES[role]):
        paint(role, i, v)
        f = (1 if v["mujer"] else 0) | (2 if v["sombrero"] else 0) | (4 if v["ala"] else 0) \
            | (8 if v["pelo"] else 0) | (16 if v["pollera"] else 0) | (32 if v["guitarra"] else 0)
        fila.append(str(f))
    flags_java.append("            {" + ", ".join(fila) + "}, // " + role)

java = f"""package com.trucoya.entity;

/**
 * Aspecto de cada variante de gaucho (generado por tools/generar_gaucho.py: no editar a mano).
 * Fila = {{@link GauchoEntity.Rol}} (mismo orden), columna = variante.
 * Textura: textures/entity/gaucho_&lt;rol&gt;_&lt;variante&gt;.png
 */
public final class GauchoAspectos {{

    public static final int MUJER = 1;
    public static final int SOMBRERO = 2;
    public static final int ALA = 4;
    public static final int PELO_LARGO = 8;
    public static final int POLLERA = 16;
    public static final int GUITARRA = 32;

    private static final int[][] FLAGS = {{
{chr(10).join(flags_java)}
    }};

    private GauchoAspectos() {{
    }}

    public static int variantes(GauchoEntity.Rol rol) {{
        return rol.ordinal() < FLAGS.length ? FLAGS[rol.ordinal()].length : 1;
    }}

    /** Variante válida (las guardadas fuera de rango se reducen con módulo). */
    public static int normalizar(GauchoEntity.Rol rol, int variante) {{
        int n = variantes(rol);
        return Math.floorMod(Math.max(0, variante), n);
    }}

    public static int flags(GauchoEntity.Rol rol, int variante) {{
        if (rol.ordinal() >= FLAGS.length) return SOMBRERO | ALA;
        return FLAGS[rol.ordinal()][normalizar(rol, variante)];
    }}

    public static boolean tiene(GauchoEntity.Rol rol, int variante, int flag) {{
        return (flags(rol, variante) & flag) != 0;
    }}
}}
"""
with open(JAVA, "w", encoding="utf-8") as f:
    f.write(java)
print("Gaucho: geometría, animaciones y", sum(len(ROLES[r]) for r in ORDEN), "texturas generadas.")
