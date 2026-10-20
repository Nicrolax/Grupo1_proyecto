"""
Baraja española en alta resolución para el tapete (diseño original, inspirado en la baraja tradicional).

  textures/gui/cartas.png   atlas de 1280 x 960: 10 columnas (1-7, 10, 11, 12) x 4 palos
                            (espadas, bastos, oros, copas) + el dorso; cada carta mide 128 x 192.

Todo se dibuja a 4x y se achica con filtro Lanczos (bordes suaves, sin serrucho). Cada palo tiene su
color y sus "pintas" (los cortes del marco que en la baraja española dicen el palo: oros ninguno,
copas uno, espadas dos, bastos tres). Las figuras: sota (paje de pie), caballo (jinete) y rey.

Uso: python3 tools/generar_cartas.py   (requiere Pillow y numpy)
"""
import math, os
import numpy as np
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))

CW, CH = 128, 192
SS = 4
W, H = CW * SS, CH * SS
PALOS = ["espada", "basto", "oro", "copa"]
NUMEROS = [1, 2, 3, 4, 5, 6, 7, 10, 11, 12]
COLOR = {"espada": (38, 84, 160), "basto": (52, 112, 40), "oro": (196, 140, 20), "copa": (176, 34, 40)}
PINTAS = {"oro": 0, "copa": 1, "espada": 2, "basto": 3}
PAPEL = (248, 241, 222)
TINTA = (52, 36, 22)
ORO = (232, 182, 40)
ORO_OSC = (150, 100, 10)
PIEL = (240, 200, 165)
FUENTE = "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf"
# Ganchos para las barajas coleccionables (tools/barajas_temas.py los cambia para cada baraja):
FONDO = None        # fondo(img, palo, numero): un dibujo apenas marcado detrás de la carta
MARCO = None        # marco(d, palo): reemplaza el marco simple
BORDE = None        # (color, ancho) del canto del naipe


def s(v):
    return int(round(v * SS))


def P(pts):
    return [(s(x), s(y)) for x, y in pts]


def mezclar(a, b, k):
    return tuple(int(a[i] + (b[i] - a[i]) * k) for i in range(3))


def oscuro(c, k=0.7):
    return tuple(int(v * k) for v in c[:3])


def claro(c, k=0.35):
    return mezclar(c, (255, 255, 255), k)


def fuente(px):
    try:
        return ImageFont.truetype(FUENTE, s(px))
    except OSError:
        return ImageFont.load_default()


# ------------------------------------------------------------------ símbolos de los palos

def oro(d, cx, cy, r, grande=False):
    """Moneda de oro: canto oscuro, anillo, rosetón y brillo."""
    d.ellipse([s(cx - r), s(cy - r), s(cx + r), s(cy + r)], fill=ORO_OSC + (255,))
    r1 = r * 0.9
    d.ellipse([s(cx - r1), s(cy - r1), s(cx + r1), s(cy + r1)], fill=ORO + (255,))
    # sombreado: media luna más oscura abajo a la derecha
    d.chord([s(cx - r1), s(cy - r1), s(cx + r1), s(cy + r1)], 20, 160, fill=mezclar(ORO, ORO_OSC, 0.35) + (255,))
    r2 = r * 0.66
    d.ellipse([s(cx - r2), s(cy - r2), s(cx + r2), s(cy + r2)], outline=ORO_OSC + (255,), width=max(1, s(r * 0.07)))
    # rosetón de 8 pétalos
    for k in range(8):
        a = k * math.pi / 4
        px, py = cx + math.cos(a) * r * 0.36, cy + math.sin(a) * r * 0.36
        rp = r * 0.17
        d.ellipse([s(px - rp), s(py - rp), s(px + rp), s(py + rp)], fill=(205, 60, 40, 255), outline=ORO_OSC + (255,),
                  width=max(1, s(r * 0.03)))
    rc = r * 0.16
    d.ellipse([s(cx - rc), s(cy - rc), s(cx + rc), s(cy + rc)], fill=(250, 226, 120, 255), outline=ORO_OSC + (255,),
              width=max(1, s(r * 0.03)))
    if grande:
        for k in range(24):              # cordón de puntos en el canto
            a = k * math.pi / 12
            px, py = cx + math.cos(a) * r * 0.8, cy + math.sin(a) * r * 0.8
            rp = r * 0.045
            d.ellipse([s(px - rp), s(py - rp), s(px + rp), s(py + rp)], fill=(255, 240, 170, 255))
    # brillo
    d.arc([s(cx - r * 0.78), s(cy - r * 0.78), s(cx + r * 0.78), s(cy + r * 0.78)], 200, 255,
          fill=(255, 245, 190, 255), width=max(1, s(r * 0.09)))


def copa(d, cx, cy, r, grande=False):
    """Copa: cáliz dorado con banda roja y gemas."""
    w = r * 0.95
    top = cy - r * 0.95
    # boca (elipse) y cuenco
    cuenco = [(cx - w, top + r * 0.12), (cx - w * 0.92, top + r * 0.55), (cx - w * 0.6, top + r * 0.95),
              (cx - w * 0.22, top + r * 1.12), (cx + w * 0.22, top + r * 1.12), (cx + w * 0.6, top + r * 0.95),
              (cx + w * 0.92, top + r * 0.55), (cx + w, top + r * 0.12)]
    d.polygon(P(cuenco), fill=ORO + (255,), outline=ORO_OSC + (255,))
    d.polygon(P([(cx + w * 0.15, top + r * 0.15), (cx + w, top + r * 0.12), (cx + w * 0.92, top + r * 0.55),
                 (cx + w * 0.6, top + r * 0.95), (cx + w * 0.22, top + r * 1.12)]), fill=mezclar(ORO, ORO_OSC, 0.3) + (255,))
    # banda roja con gemas
    by = top + r * 0.45
    d.polygon(P([(cx - w * 0.95, by - r * 0.1), (cx + w * 0.95, by - r * 0.1), (cx + w * 0.85, by + r * 0.16),
                 (cx - w * 0.85, by + r * 0.16)]), fill=(178, 30, 36, 255))
    for k in (-0.5, 0, 0.5):
        gx = cx + w * k
        g = r * 0.09
        d.ellipse([s(gx - g), s(by - g + r * 0.03), s(gx + g), s(by + g + r * 0.03)], fill=(250, 226, 120, 255))
    d.ellipse([s(cx - w), s(top), s(cx + w), s(top + r * 0.26)], fill=(120, 20, 24, 255), outline=ORO_OSC + (255,),
              width=max(1, s(r * 0.05)))
    # tallo, nudo y pie
    st = r * 0.12
    d.rectangle([s(cx - st), s(top + r * 1.1), s(cx + st), s(cy + r * 0.62)], fill=ORO + (255,), outline=ORO_OSC + (255,))
    kn = r * 0.2
    d.ellipse([s(cx - kn), s(cy + r * 0.28), s(cx + kn), s(cy + r * 0.5)], fill=ORO + (255,), outline=ORO_OSC + (255,))
    d.polygon(P([(cx - r * 0.6, cy + r * 0.95), (cx - r * 0.15, cy + r * 0.6), (cx + r * 0.15, cy + r * 0.6),
                 (cx + r * 0.6, cy + r * 0.95)]), fill=ORO + (255,), outline=ORO_OSC + (255,))
    d.ellipse([s(cx - r * 0.62), s(cy + r * 0.86), s(cx + r * 0.62), s(cy + r * 1.02)], fill=mezclar(ORO, ORO_OSC, 0.3) + (255,),
              outline=ORO_OSC + (255,))
    # brillo
    d.line(P([(cx - w * 0.7, top + r * 0.35), (cx - w * 0.45, top + r * 0.85)]), fill=(255, 245, 190, 255),
           width=max(1, s(r * 0.08)))


def espada(d, cx, cy, r, grande=False, angulo=0.0):
    """Espada: hoja de acero con canal, guarnición dorada con gavilanes curvos, empuñadura y pomo."""
    capa = Image.new("RGBA", (s(r * 2.4), s(r * 2.4)), (0, 0, 0, 0))
    e = ImageDraw.Draw(capa)
    c = r * 1.2
    hw = r * 0.15
    punta = c - r * 1.08
    guard = c + r * 0.42
    acero, acero_osc, filo = (150, 178, 210), (60, 84, 120), (226, 236, 248)
    e.polygon(P([(c, punta), (c + hw, punta + r * 0.28), (c + hw, guard), (c - hw, guard), (c - hw, punta + r * 0.28)]),
              fill=acero + (255,), outline=acero_osc + (255,))
    e.polygon(P([(c, punta), (c + hw, punta + r * 0.28), (c + hw, guard), (c, guard)]), fill=mezclar(acero, acero_osc, 0.3) + (255,))
    e.line(P([(c, punta + r * 0.12), (c, guard - r * 0.04)]), fill=filo + (255,), width=max(1, s(r * 0.05)))
    # gavilanes curvos
    g = r * 0.62
    e.arc([s(c - g), s(guard - g * 0.55), s(c), s(guard + g * 0.45)], 90, 200, fill=ORO_OSC + (255,), width=max(2, s(r * 0.13)))
    e.arc([s(c), s(guard - g * 0.55), s(c + g), s(guard + g * 0.45)], -20, 90, fill=ORO_OSC + (255,), width=max(2, s(r * 0.13)))
    e.arc([s(c - g), s(guard - g * 0.55), s(c), s(guard + g * 0.45)], 90, 200, fill=ORO + (255,), width=max(1, s(r * 0.07)))
    e.arc([s(c), s(guard - g * 0.55), s(c + g), s(guard + g * 0.45)], -20, 90, fill=ORO + (255,), width=max(1, s(r * 0.07)))
    e.rounded_rectangle([s(c - r * 0.5), s(guard - r * 0.04), s(c + r * 0.5), s(guard + r * 0.1)], radius=s(r * 0.05),
                        fill=ORO + (255,), outline=ORO_OSC + (255,))
    # empuñadura con vueltas de cuero
    e.rectangle([s(c - r * 0.1), s(guard + r * 0.1), s(c + r * 0.1), s(guard + r * 0.5)], fill=(110, 66, 34, 255),
                outline=(60, 34, 16, 255))
    for k in range(4):
        yy = guard + r * (0.15 + 0.09 * k)
        e.line(P([(c - r * 0.1, yy), (c + r * 0.1, yy + r * 0.04)]), fill=(70, 40, 18, 255), width=max(1, s(r * 0.03)))
    pr = r * 0.13
    e.ellipse([s(c - pr), s(guard + r * 0.48), s(c + pr), s(guard + r * 0.48 + 2 * pr)], fill=ORO + (255,),
              outline=ORO_OSC + (255,))
    if angulo:
        capa = capa.rotate(angulo, resample=Image.BICUBIC, center=(capa.width / 2, capa.height / 2))
    d._image.alpha_composite(capa, (s(cx) - capa.width // 2, s(cy) - capa.height // 2))


def basto(d, cx, cy, r, grande=False, angulo=0.0):
    """Basto: garrote de madera nudosa, más ancho arriba, con dos brotes de hojas."""
    capa = Image.new("RGBA", (s(r * 2.4), s(r * 2.4)), (0, 0, 0, 0))
    e = ImageDraw.Draw(capa)
    c = r * 1.2
    madera, madera_osc, madera_cl = (150, 96, 46), (74, 44, 18), (190, 132, 72)
    pts = [(c - r * 0.34, c - r * 1.0), (c + r * 0.36, c - r * 1.02), (c + r * 0.3, c - r * 0.5), (c + r * 0.22, c),
           (c + r * 0.17, c + r * 0.6), (c + r * 0.13, c + r * 1.0), (c - r * 0.13, c + r * 1.0), (c - r * 0.17, c + r * 0.6),
           (c - r * 0.23, c), (c - r * 0.3, c - r * 0.5)]
    e.polygon(P(pts), fill=madera + (255,), outline=madera_osc + (255,))
    e.polygon(P([(c + r * 0.05, c - r * 1.0), (c + r * 0.36, c - r * 1.02), (c + r * 0.3, c - r * 0.5), (c + r * 0.22, c),
                 (c + r * 0.17, c + r * 0.6), (c + r * 0.13, c + r * 1.0), (c + r * 0.02, c + r * 1.0)]),
              fill=mezclar(madera, madera_osc, 0.35) + (255,))
    e.line(P([(c - r * 0.18, c - r * 0.9), (c - r * 0.1, c + r * 0.9)]), fill=madera_cl + (255,), width=max(1, s(r * 0.06)))
    e.ellipse([s(c - r * 0.34), s(c - r * 1.12), s(c + r * 0.36), s(c - r * 0.9)], fill=madera_cl + (255,),
              outline=madera_osc + (255,))
    e.ellipse([s(c - r * 0.2), s(c - r * 1.06), s(c + r * 0.2), s(c - r * 0.96)], outline=madera_osc + (255,),
              width=max(1, s(r * 0.03)))
    # nudos (muñones de ramas cortadas)
    for (kx, ky, kr) in ((-0.28, -0.55, 0.13), (0.27, -0.2, 0.12), (-0.22, 0.25, 0.11), (0.18, 0.62, 0.09)):
        x, y = c + r * kx, c + r * ky
        rr = r * kr
        e.ellipse([s(x - rr), s(y - rr * 0.8), s(x + rr), s(y + rr * 0.8)], fill=madera_osc + (255,))
        e.ellipse([s(x - rr * 0.55), s(y - rr * 0.45), s(x + rr * 0.55), s(y + rr * 0.45)], fill=madera_cl + (255,))
    # brotes
    hoja, hoja_osc = (70, 140, 52), (30, 70, 20)
    for (bx, by, ang) in ((0.3, -0.35, -35), (-0.26, 0.05, 210)):
        x, y = c + r * bx, c + r * by
        for k, da in enumerate((-25, 20)):
            a = math.radians(ang + da)
            L = r * (0.42 if k == 0 else 0.34)
            tip = (x + math.cos(a) * L, y + math.sin(a) * L)
            mid1 = (x + math.cos(a + 0.5) * L * 0.5, y + math.sin(a + 0.5) * L * 0.5)
            mid2 = (x + math.cos(a - 0.5) * L * 0.5, y + math.sin(a - 0.5) * L * 0.5)
            e.polygon(P([(x, y), mid1, tip, mid2]), fill=hoja + (255,), outline=hoja_osc + (255,))
    if angulo:
        capa = capa.rotate(angulo, resample=Image.BICUBIC, center=(capa.width / 2, capa.height / 2))
    d._image.alpha_composite(capa, (s(cx) - capa.width // 2, s(cy) - capa.height // 2))


SIMBOLO = {"oro": oro, "copa": copa, "espada": espada, "basto": basto}

# posiciones de los símbolos (fracciones del área central)
PIPS = {
    2: [(0.5, 0.16), (0.5, 0.84)],
    3: [(0.5, 0.14), (0.5, 0.5), (0.5, 0.86)],
    4: [(0.27, 0.18), (0.73, 0.18), (0.27, 0.82), (0.73, 0.82)],
    5: [(0.27, 0.16), (0.73, 0.16), (0.5, 0.5), (0.27, 0.84), (0.73, 0.84)],
    6: [(0.27, 0.15), (0.73, 0.15), (0.27, 0.5), (0.73, 0.5), (0.27, 0.85), (0.73, 0.85)],
    7: [(0.27, 0.13), (0.73, 0.13), (0.5, 0.33), (0.27, 0.53), (0.73, 0.53), (0.27, 0.87), (0.73, 0.87)],
}


# ------------------------------------------------------------------ figuras

def figura(d, palo, numero):
    col = COLOR[palo]
    seg = {"espada": (176, 34, 40), "basto": (196, 140, 20), "oro": (38, 84, 160), "copa": (52, 112, 40)}[palo]
    cx = CW / 2
    # sombra en el piso
    d.ellipse([s(cx - 34), s(CH - 40), s(cx + 34), s(CH - 30)], fill=mezclar(PAPEL, TINTA, 0.12) + (255,))
    if numero != 11:
        # fondo de la figura: un arco apenas teñido detrás
        d.rounded_rectangle([s(30), s(24), s(CW - 30), s(CH - 36)], radius=s(18), fill=mezclar(PAPEL, col, 0.09) + (255,))
    if numero == 11:
        caballo(d, palo, col, seg)
    else:
        persona(d, palo, numero, col, seg, cx)


def persona(d, palo, numero, col, seg, cx):
    rey = numero == 12
    pies = CH - 34
    # piernas y zapatos
    calza = seg if not rey else oscuro(col, 0.8)
    for lado in (-1, 1):
        x = cx + lado * 7
        d.polygon(P([(x - 5, pies - 34), (x + 5, pies - 34), (x + 4.5, pies - 6), (x - 4.5, pies - 6)]), fill=calza + (255,),
                  outline=TINTA + (255,))
        d.ellipse([s(x - 7 + lado * 1.5), s(pies - 8), s(x + 7 + lado * 1.5), s(pies)], fill=(60, 40, 26, 255),
                  outline=TINTA + (255,))
    # manto / túnica
    largo = 70 if rey else 52
    hombro = pies - 34 - largo + 6
    tunica = [(cx - 19, pies - 30 + (14 if rey else 0)), (cx - 15, hombro + 6), (cx - 10, hombro), (cx + 10, hombro),
              (cx + 15, hombro + 6), (cx + 19, pies - 30 + (14 if rey else 0))]
    d.polygon(P(tunica), fill=col + (255,), outline=TINTA + (255,))
    d.polygon(P([(cx + 2, hombro), (cx + 10, hombro), (cx + 15, hombro + 6), (cx + 19, pies - 30 + (14 if rey else 0)),
                 (cx + 4, pies - 30 + (14 if rey else 0))]), fill=oscuro(col, 0.82) + (255,))
    # dibujos de la tela
    for k in range(4):
        y = hombro + 14 + k * 11
        d.line(P([(cx - 14 + k * 0.5, y), (cx + 14 - k * 0.5, y)]), fill=claro(col, 0.25) + (255,), width=s(1.2))
    if rey:   # armiño en el borde del manto y en los hombros
        d.rectangle([s(cx - 19), s(pies - 18), s(cx + 19), s(pies - 12)], fill=(250, 248, 240, 255), outline=TINTA + (255,))
        for k in range(6):
            x = cx - 16 + k * 6.4
            d.line(P([(x, pies - 17), (x, pies - 13)]), fill=(20, 20, 20, 255), width=s(1.4))
        d.polygon(P([(cx - 16, hombro + 5), (cx + 16, hombro + 5), (cx + 12, hombro + 13), (cx - 12, hombro + 13)]),
                  fill=(250, 248, 240, 255), outline=TINTA + (255,))
    # cinturón
    d.rectangle([s(cx - 15), s(hombro + 26), s(cx + 15), s(hombro + 30)], fill=ORO + (255,), outline=TINTA + (255,))
    # capa atrás (sota) / brazos
    if not rey:
        d.polygon(P([(cx - 15, hombro + 4), (cx - 24, pies - 26), (cx - 18, pies - 28), (cx - 12, hombro + 10)]),
                  fill=seg + (255,), outline=TINTA + (255,))
    # cuello y cabeza
    cabeza_y = hombro - 12
    d.rectangle([s(cx - 3), s(hombro - 4), s(cx + 3), s(hombro + 1)], fill=PIEL + (255,))
    d.ellipse([s(cx - 8), s(cabeza_y - 9), s(cx + 8), s(cabeza_y + 9)], fill=PIEL + (255,), outline=TINTA + (255,))
    # pelo
    d.chord([s(cx - 8.5), s(cabeza_y - 10), s(cx + 8.5), s(cabeza_y + 6)], 180, 360, fill=(96, 60, 30, 255))
    for lado in (-1, 1):
        d.ellipse([s(cx + lado * 7.5 - 2.5), s(cabeza_y - 2), s(cx + lado * 7.5 + 2.5), s(cabeza_y + 8)], fill=(96, 60, 30, 255))
    # cara
    for lado in (-1, 1):
        d.ellipse([s(cx + lado * 3.2 - 1.1), s(cabeza_y - 0.8), s(cx + lado * 3.2 + 1.1), s(cabeza_y + 1.2)],
                  fill=(40, 26, 16, 255))
    d.line(P([(cx, cabeza_y + 1), (cx - 0.8, cabeza_y + 4)]), fill=(190, 140, 110, 255), width=s(0.8))
    if rey:   # barba y bigote
        d.polygon(P([(cx - 7, cabeza_y + 3), (cx + 7, cabeza_y + 3), (cx + 5, cabeza_y + 11), (cx, cabeza_y + 15),
                     (cx - 5, cabeza_y + 11)]), fill=(150, 130, 120, 255), outline=TINTA + (255,))
        d.line(P([(cx - 4, cabeza_y + 4.5), (cx + 4, cabeza_y + 4.5)]), fill=(110, 95, 88, 255), width=s(1.2))
    else:
        d.line(P([(cx - 2.2, cabeza_y + 5.5), (cx + 2.2, cabeza_y + 5.5)]), fill=(170, 80, 70, 255), width=s(0.9))
    # sombrero o corona
    if rey:
        base = cabeza_y - 8
        d.polygon(P([(cx - 9, base + 1), (cx - 10, base - 9), (cx - 5, base - 4), (cx, base - 11), (cx + 5, base - 4),
                     (cx + 10, base - 9), (cx + 9, base + 1)]), fill=ORO + (255,), outline=TINTA + (255,))
        d.rectangle([s(cx - 9), s(base - 1), s(cx + 9), s(base + 2)], fill=ORO_OSC + (255,))
        for x in (cx - 10, cx, cx + 10):
            d.ellipse([s(x - 1.4), s(base - 12 + (2 if x != cx else 0)), s(x + 1.4), s(base - 9 + (2 if x != cx else 0))],
                      fill=(200, 40, 40, 255))
    else:
        base = cabeza_y - 7
        d.ellipse([s(cx - 12), s(base - 2), s(cx + 12), s(base + 3)], fill=seg + (255,), outline=TINTA + (255,))
        d.chord([s(cx - 8), s(base - 9), s(cx + 8), s(base + 3)], 180, 360, fill=seg + (255,), outline=TINTA + (255,))
        d.line(P([(cx + 6, base - 3), (cx + 13, base - 13), (cx + 15, base - 16)]), fill=(250, 248, 240, 255), width=s(2.2))
    # brazo que sostiene el símbolo del palo
    mano = (cx + 13, hombro + 20)
    d.line(P([(cx + 12, hombro + 6), mano]), fill=oscuro(col, 0.75) + (255,), width=s(5))
    d.ellipse([s(mano[0] - 3), s(mano[1] - 3), s(mano[0] + 3), s(mano[1] + 3)], fill=PIEL + (255,), outline=TINTA + (255,))
    sim = SIMBOLO[palo]
    if palo == "espada":
        espada(d, mano[0] + 1, mano[1] - 14, 14, angulo=-12)
    elif palo == "basto":
        basto(d, mano[0] + 2, mano[1] - 12, 14, angulo=-15)
    else:
        sim(d, mano[0] + 4, mano[1] - 6, 9)


def caballo(d, palo, col, seg):
    pelaje, pelaje_osc = (150, 96, 56), (96, 58, 30)
    suelo = CH - 36
    # patas
    for (x0, x1, adel) in ((40, 37, 0), (48, 52, 0), (78, 74, 1), (88, 94, 1)):
        d.polygon(P([(x0 - 3, suelo - 30), (x0 + 3, suelo - 30), (x1 + 2.5, suelo - 2), (x1 - 2.5, suelo - 2)]),
                  fill=(pelaje_osc if x0 in (48, 88) else pelaje) + (255,), outline=TINTA + (255,))
        d.rectangle([s(x1 - 3), s(suelo - 3), s(x1 + 3), s(suelo)], fill=(40, 30, 24, 255))
    # cuerpo
    d.ellipse([s(30), s(suelo - 52), s(98), s(suelo - 24)], fill=pelaje + (255,), outline=TINTA + (255,))
    d.chord([s(30), s(suelo - 52), s(98), s(suelo - 24)], 10, 170, fill=pelaje_osc + (255,))
    # cola
    d.polygon(P([(31, suelo - 44), (20, suelo - 36), (17, suelo - 16), (24, suelo - 22), (29, suelo - 36)]),
              fill=(60, 36, 20, 255), outline=TINTA + (255,))
    # cuello y cabeza
    d.polygon(P([(80, suelo - 48), (90, suelo - 76), (100, suelo - 80), (104, suelo - 70), (94, suelo - 42)]),
              fill=pelaje + (255,), outline=TINTA + (255,))
    d.polygon(P([(96, suelo - 82), (110, suelo - 68), (108, suelo - 62), (98, suelo - 66), (92, suelo - 76)]),
              fill=pelaje + (255,), outline=TINTA + (255,))
    d.polygon(P([(93, suelo - 82), (95, suelo - 90), (99, suelo - 83)]), fill=pelaje + (255,), outline=TINTA + (255,))
    d.ellipse([s(99), s(suelo - 76), s(102), s(suelo - 73)], fill=(20, 14, 10, 255))
    # crin
    d.polygon(P([(88, suelo - 78), (78, suelo - 50), (83, suelo - 50), (92, suelo - 74)]), fill=(60, 36, 20, 255))
    # montura y manta con el color del palo
    d.polygon(P([(50, suelo - 54), (76, suelo - 54), (78, suelo - 36), (48, suelo - 36)]), fill=col + (255,),
              outline=TINTA + (255,))
    for k in range(3):
        d.line(P([(50, suelo - 40 + k * 0), (78, suelo - 40)]), fill=ORO + (255,), width=s(1.5))
    # rienda
    d.line(P([(104, suelo - 66), (72, suelo - 70)]), fill=(50, 30, 16, 255), width=s(1.2))
    # jinete
    cx, base = 63, suelo - 54
    d.polygon(P([(cx - 10, base + 2), (cx - 8, base - 26), (cx + 8, base - 26), (cx + 10, base + 2)]), fill=seg + (255,),
              outline=TINTA + (255,))
    d.polygon(P([(cx - 4, base), (cx + 2, base), (cx + 6, base + 18), (cx + 1, base + 20)]), fill=oscuro(seg, 0.8) + (255,),
              outline=TINTA + (255,))
    d.ellipse([s(cx - 6.5), s(base - 40), s(cx + 6.5), s(base - 26)], fill=PIEL + (255,), outline=TINTA + (255,))
    for lado in (-1, 1):
        d.ellipse([s(cx + lado * 2.7 - 0.9), s(base - 34.5), s(cx + lado * 2.7 + 0.9), s(base - 32.7)], fill=(40, 26, 16, 255))
    d.line(P([(cx - 3.5, base - 29), (cx + 3.5, base - 29)]), fill=(90, 60, 40, 255), width=s(1.1))   # bigote
    # chambergo
    d.ellipse([s(cx - 12), s(base - 42), s(cx + 12), s(base - 37)], fill=(44, 30, 22, 255), outline=TINTA + (255,))
    d.chord([s(cx - 7), s(base - 50), s(cx + 7), s(base - 37)], 180, 360, fill=(44, 30, 22, 255), outline=TINTA + (255,))
    d.rectangle([s(cx - 7), s(base - 41.5), s(cx + 7), s(base - 39.5)], fill=col + (255,))
    # brazo en alto con el símbolo
    mano = (cx - 14, base - 34)
    d.line(P([(cx - 7, base - 22), mano]), fill=oscuro(seg, 0.75) + (255,), width=s(4.5))
    d.ellipse([s(mano[0] - 2.8), s(mano[1] - 2.8), s(mano[0] + 2.8), s(mano[1] + 2.8)], fill=PIEL + (255,),
              outline=TINTA + (255,))
    if palo == "espada":
        espada(d, mano[0] - 2, mano[1] - 14, 13, angulo=14)
    elif palo == "basto":
        basto(d, mano[0] - 2, mano[1] - 12, 13, angulo=18)
    else:
        SIMBOLO[palo](d, mano[0] - 2, mano[1] - 8, 8.5)


# ------------------------------------------------------------------ carta

def papel(w, h, base, semilla):
    """Papel de naipe: color parejo con un grano muy fino y bordes apenas más tostados."""
    rng = np.random.default_rng(semilla)
    a = np.zeros((h, w, 3), dtype=np.float32) + np.array(base, dtype=np.float32)
    grano = rng.normal(0, 3.0, (h // 4, w // 4)).astype(np.float32)
    grano = np.array(Image.fromarray(grano).resize((w, h), Image.BICUBIC))
    a += grano[..., None]
    yy, xx = np.mgrid[0:h, 0:w]
    borde = np.minimum(np.minimum(xx, w - 1 - xx), np.minimum(yy, h - 1 - yy)) / (0.12 * w)
    a *= (0.95 + 0.05 * np.clip(borde, 0, 1))[..., None]
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8)).convert("RGBA")


def mascara_redondeada(w, h, radio):
    m = Image.new("L", (w, h), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, w - 1, h - 1], radius=radio, fill=255)
    return m


def indice(img, palo, numero):
    d = ImageDraw.Draw(img)
    f = fuente(19)
    txt = str(numero)
    col = COLOR[palo]
    tw = d.textlength(txt, font=f)
    x = s(15.5) - tw / 2

    def dibujar(e):
        # el índice "corta" el marco, como en los naipes de verdad
        e.rectangle([s(4), s(9), s(27), s(48)], fill=PAPEL + (255,))
        e.text((x, s(9)), txt, font=f, fill=col + (255,))
        if palo in ("espada", "basto"):
            SIMBOLO[palo](e, 15.5, 39, 8.5)
        else:
            SIMBOLO[palo](e, 15.5, 39, 6.5)

    dibujar(d)
    capa = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    dibujar(ImageDraw.Draw(capa))
    img.alpha_composite(capa.rotate(180))      # el mismo índice, girado, abajo a la derecha


def marco(d, palo):
    col = COLOR[palo]
    m = 7
    d.rounded_rectangle([s(m), s(m), s(CW - m), s(CH - m)], radius=s(5), outline=col + (255,), width=s(1.6))
    n = PINTAS[palo]
    if n:
        # cortes del marco arriba y abajo (las "pintas" que dicen el palo)
        ancho = 6
        total = n * ancho + (n - 1) * 5
        x0 = CW / 2 - total / 2
        for k in range(n):
            x = x0 + k * (ancho + 5)
            for y in (m, CH - m):
                d.rectangle([s(x), s(y - 2), s(x + ancho), s(y + 2)], fill=PAPEL + (255,))


def carta(palo, numero):
    img = papel(W, H, PAPEL, semilla=numero * 7 + PALOS.index(palo))
    if FONDO is not None:
        FONDO(img, palo, numero)
    d = ImageDraw.Draw(img)
    (MARCO or marco)(d, palo)
    indice(img, palo, numero)
    d = ImageDraw.Draw(img)
    x0, y0, x1, y1 = 26, 40, CW - 26, CH - 40
    if numero == 1:
        cx, cy = CW / 2, CH / 2
        if palo == "oro":
            # el as de oros: gran moneda con rayos
            for k in range(32):
                a = k * math.pi / 16
                L = 40 if k % 2 == 0 else 34
                d.polygon(P([(cx + math.cos(a - 0.05) * 26, cy + math.sin(a - 0.05) * 26),
                             (cx + math.cos(a) * L, cy + math.sin(a) * L),
                             (cx + math.cos(a + 0.05) * 26, cy + math.sin(a + 0.05) * 26)]), fill=ORO + (255,),
                          outline=ORO_OSC + (255,))
            oro(d, cx, cy, 28, grande=True)
        elif palo == "copa":
            copa(d, cx, cy + 2, 36, grande=True)
        elif palo == "espada":
            espada(d, cx, cy, 52, grande=True)
        else:
            basto(d, cx, cy, 50, grande=True)
        # cinta con el nombre
        f = fuente(7)
        nombre = {"oro": "AS DE OROS", "copa": "AS DE COPAS", "espada": "AS DE ESPADAS", "basto": "AS DE BASTOS"}[palo]
        tw = d.textlength(nombre, font=f)
        d.rounded_rectangle([s(CW / 2) - tw / 2 - s(5), s(CH - 31), s(CW / 2) + tw / 2 + s(5), s(CH - 21)], radius=s(3),
                            fill=COLOR[palo] + (255,))
        d.text((s(CW / 2) - tw / 2, s(CH - 30.5)), nombre, font=f, fill=PAPEL + (255,))
    elif numero <= 7:
        k = 0 if numero <= 3 else 1 if numero <= 6 else 2
        for (fx, fy) in PIPS[numero]:
            cx = x0 + (x1 - x0) * fx
            cy = y0 + (y1 - y0) * fy
            if palo == "espada":
                espada(d, cx, cy, {2: 25, 3: 18.5}.get(numero, 17.5 if numero <= 6 else 15))
            elif palo == "basto":
                basto(d, cx, cy, {2: 23, 3: 18}.get(numero, 17 if numero <= 6 else 14.5))
            else:
                SIMBOLO[palo](d, cx, cy, (15, 13, 11.5)[k])
    else:
        figura(d, palo, numero)
    # borde del naipe
    d = ImageDraw.Draw(img)
    color_borde, ancho_borde = BORDE or (TINTA, 1.2)
    d.rounded_rectangle([0, 0, W - 1, H - 1], radius=s(9), outline=tuple(color_borde) + (255,), width=s(ancho_borde))
    img.putalpha(mascara_redondeada(W, H, s(9)))
    return img.resize((CW, CH), Image.LANCZOS)


def dorso():
    img = Image.new("RGBA", (W, H), (110, 22, 30, 255))
    d = ImageDraw.Draw(img)
    # enrejado de rombos
    paso = 12
    for y in range(-paso, CH + paso, paso):
        for x in range(-paso, CW + paso, paso):
            if ((x // paso) + (y // paso)) % 2 == 0:
                d.polygon(P([(x + paso / 2, y), (x + paso, y + paso / 2), (x + paso / 2, y + paso), (x, y + paso / 2)]),
                          fill=(134, 32, 40, 255))
            d.ellipse([s(x - 0.9), s(y - 0.9), s(x + 0.9), s(y + 0.9)], fill=(220, 170, 60, 255))
    m = 7
    d.rounded_rectangle([s(m), s(m), s(CW - m), s(CH - m)], radius=s(5), outline=ORO + (255,), width=s(1.6))
    d.rounded_rectangle([s(m + 3), s(m + 3), s(CW - m - 3), s(CH - m - 3)], radius=s(4), outline=ORO_OSC + (255,), width=s(0.8))
    # medallón con un sol de rayos rectos y ondulados
    cx, cy = CW / 2, CH / 2 - 6
    d.ellipse([s(cx - 34), s(cy - 34), s(cx + 34), s(cy + 34)], fill=(90, 16, 24, 255), outline=ORO + (255,), width=s(1.4))
    for k in range(16):
        a = k * math.pi / 8 - math.pi / 2
        if k % 2 == 0:
            d.polygon(P([(cx + math.cos(a - 0.12) * 13, cy + math.sin(a - 0.12) * 13), (cx + math.cos(a) * 30, cy + math.sin(a) * 30),
                         (cx + math.cos(a + 0.12) * 13, cy + math.sin(a + 0.12) * 13)]), fill=ORO + (255,))
        else:
            pts = []
            for j in range(9):
                t = j / 8
                rr = 13 + 15 * t
                aa = a + 0.09 * math.sin(t * math.pi * 3)
                pts.append((cx + math.cos(aa) * rr, cy + math.sin(aa) * rr))
            d.line(P(pts), fill=ORO + (255,), width=s(1.6))
    d.ellipse([s(cx - 12), s(cy - 12), s(cx + 12), s(cy + 12)], fill=ORO + (255,), outline=ORO_OSC + (255,), width=s(0.8))
    for lado in (-1, 1):
        d.arc([s(cx + lado * 4.5 - 2.5), s(cy - 4), s(cx + lado * 4.5 + 2.5), s(cy)], 200, 340, fill=ORO_OSC + (255,), width=s(1))
    d.arc([s(cx - 4.5), s(cy + 1), s(cx + 4.5), s(cy + 7)], 20, 160, fill=ORO_OSC + (255,), width=s(1))
    f = fuente(9)
    txt = "TRUCO YA"
    tw = d.textlength(txt, font=f)
    d.rounded_rectangle([s(cx) - tw / 2 - s(6), s(cy + 40), s(cx) + tw / 2 + s(6), s(cy + 54)], radius=s(3),
                        fill=(90, 16, 24, 255), outline=ORO + (255,), width=s(1))
    d.text((s(cx) - tw / 2, s(cy + 41)), txt, font=f, fill=ORO + (255,))
    d.rounded_rectangle([0, 0, W - 1, H - 1], radius=s(9), outline=(40, 8, 12, 255), width=s(1.2))
    img.putalpha(mascara_redondeada(W, H, s(9)))
    return img.resize((CW, CH), Image.LANCZOS)


def main():
    """Las diez barajas (la criolla es este dibujo tal cual): ver barajas_temas.py."""
    import barajas_temas
    barajas_temas.main(list(barajas_temas.TEMAS))


if __name__ == "__main__":
    main()
