"""
Las barajas coleccionables de Truco Ya: diez barajas españolas, cada una con su papel, sus colores, su marco,
un dibujo apenas marcado detrás de cada carta y un dorso propio (diseños originales).

  criolla      la de siempre: papel crema, sol de rayos en el dorso bordó
  oro          la del Viejo Vizcacha: marco doble dorado con filigranas y canto dorado; dorso azul noche
  mandinga     la de la Salamanca: papel negro, tinta de hueso, llamas en el marco; dorso con el sombrero y los ojos
  llanura      guarda pampa verde y ocre; dorso: amanecer sobre el campo y un ombú
  sabana       rayos de sol en las esquinas; dorso: atardecer naranja, acacias y un ñandú
  desierto     escalonados de adobe; dorso: terracota con guarda escalonada, un cardón y el sol
  monte        enredadera de hojas; dorso: verde profundo con hojas y un hornero en su rama
  nieve        copos en las esquinas; dorso: noche azul, cerros nevados y un gran copo
  sierra       perfil de cerros arriba y abajo; dorso: sierras en capas al anochecer y un cóndor
  fundadores   estrellas en las esquinas; dorso: los cuatro colores de los Fundadores, un mate y sus iniciales

Genera textures/gui/cartas_<clave>.png (1280 x 960, cartas de 128 x 192). La misma textura, con mipmaps,
se usa en el tapete y para las cartas sobre la mesa en el mundo.
Uso: python3 tools/barajas_temas.py [clave ...]
"""
import math, os, sys
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import generar_cartas as G   # noqa: E402
from generar_cartas import s, P, mezclar, oscuro, claro, fuente, CW, CH, W, H   # noqa: E402

TEX = os.path.join(HERE, "..", "src", "main", "resources", "assets", "trucoya", "textures")


def rgba(c, a=255):
    return tuple(int(v) for v in c[:3]) + (a,)


def capa():
    return Image.new("RGBA", (W, H), (0, 0, 0, 0))


def fundir(img, lay, alfa):
    """Pega una capa sobre la carta con transparencia alfa (0-1)."""
    if alfa < 1.0:
        a = lay.split()[3].point(lambda v: int(v * alfa))
        lay.putalpha(a)
    img.alpha_composite(lay)


# ================================================================== siluetas y adornos (coordenadas de carta)

def ombu(d, cx, base, k, col):
    """Ombú: tronco grueso y bajo, copa ancha."""
    d.polygon(P([(cx - 5 * k, base), (cx - 3 * k, base - 14 * k), (cx + 3 * k, base - 14 * k), (cx + 6 * k, base)]), fill=col)
    for (dx, dy, r) in ((-14, -20, 11), (0, -26, 13), (14, -20, 11), (-7, -30, 9), (8, -31, 9), (-20, -15, 7), (20, -15, 7)):
        d.ellipse([s(cx + (dx - r) * k), s(base + (dy - r) * k), s(cx + (dx + r) * k), s(base + (dy + r) * k)], fill=col)


def acacia(d, cx, base, k, col):
    d.polygon(P([(cx - 1.5 * k, base), (cx - 1 * k, base - 16 * k), (cx + 1 * k, base - 16 * k), (cx + 1.5 * k, base)]), fill=col)
    d.line(P([(cx, base - 12 * k), (cx - 8 * k, base - 20 * k)]), fill=col, width=max(1, s(1.2 * k)))
    d.line(P([(cx, base - 12 * k), (cx + 9 * k, base - 21 * k)]), fill=col, width=max(1, s(1.2 * k)))
    d.ellipse([s(cx - 20 * k), s(base - 26 * k), s(cx + 20 * k), s(base - 18 * k)], fill=col)


def nandu(d, cx, base, k, col):
    """Ñandú caminando: cuerpo, cuello largo, patas."""
    d.ellipse([s(cx - 10 * k), s(base - 22 * k), s(cx + 8 * k), s(base - 10 * k)], fill=col)
    d.polygon(P([(cx + 4 * k, base - 19 * k), (cx + 8 * k, base - 36 * k), (cx + 11 * k, base - 36 * k),
                 (cx + 8 * k, base - 18 * k)]), fill=col)
    d.ellipse([s(cx + 7 * k), s(base - 39 * k), s(cx + 13 * k), s(base - 34 * k)], fill=col)
    d.polygon(P([(cx + 12.5 * k, base - 37 * k), (cx + 16 * k, base - 36 * k), (cx + 12 * k, base - 35 * k)]), fill=col)
    d.line(P([(cx - 2 * k, base - 11 * k), (cx - 5 * k, base)]), fill=col, width=max(1, s(1.2 * k)))
    d.line(P([(cx + 2 * k, base - 11 * k), (cx + 4 * k, base - 5 * k), (cx + 2 * k, base)]), fill=col, width=max(1, s(1.2 * k)))


def cardon(d, cx, base, k, col):
    """Cardón: tronco con brazos en candelabro."""
    def rama(x0, y0, x1, y1, w):
        d.rounded_rectangle([s(min(x0, x1) - w), s(min(y0, y1)), s(max(x0, x1) + w), s(max(y0, y1))], radius=s(w), fill=col)
    rama(cx, base - 40 * k, cx, base, 4 * k)
    rama(cx - 12 * k, base - 28 * k, cx - 12 * k, base - 14 * k, 3 * k)
    d.rectangle([s(cx - 12 * k), s(base - 17 * k), s(cx), s(base - 11 * k)], fill=col)
    rama(cx + 11 * k, base - 33 * k, cx + 11 * k, base - 20 * k, 3 * k)
    d.rectangle([s(cx), s(base - 23 * k), s(cx + 11 * k), s(base - 17 * k)], fill=col)


def pino(d, cx, base, k, col):
    d.rectangle([s(cx - 1.5 * k), s(base - 6 * k), s(cx + 1.5 * k), s(base)], fill=col)
    for i, (w, y) in enumerate(((14, 6), (11, 15), (8, 23), (5, 30))):
        d.polygon(P([(cx - w * k, base - y * k), (cx, base - (y + 13) * k), (cx + w * k, base - y * k)]), fill=col)


def hornero(d, cx, cy, k, col, pecho):
    """Hornero en una rama: cuerpo redondo, cola alzada, pico fino."""
    d.ellipse([s(cx - 9 * k), s(cy - 6 * k), s(cx + 7 * k), s(cy + 6 * k)], fill=col)
    d.ellipse([s(cx - 3 * k), s(cy - 3 * k), s(cx + 6 * k), s(cy + 5 * k)], fill=pecho)
    d.ellipse([s(cx + 3 * k), s(cy - 11 * k), s(cx + 11 * k), s(cy - 3 * k)], fill=col)
    d.polygon(P([(cx + 10.5 * k, cy - 7.5 * k), (cx + 16 * k, cy - 6 * k), (cx + 10.5 * k, cy - 6 * k)]), fill=(70, 50, 30, 255))
    d.ellipse([s(cx + 7.5 * k), s(cy - 8.5 * k), s(cx + 9 * k), s(cy - 7 * k)], fill=(20, 14, 10, 255))
    d.polygon(P([(cx - 8 * k, cy - 2 * k), (cx - 16 * k, cy - 9 * k), (cx - 14 * k, cy - 1 * k)]), fill=col)
    for dx in (-2, 2):
        d.line(P([(cx + dx * k, cy + 5 * k), (cx + dx * k, cy + 9 * k)]), fill=(70, 50, 30, 255), width=max(1, s(0.8 * k)))


def condor(d, cx, cy, k, col, blanco=(240, 236, 228, 255)):
    """Cóndor planeando: alas largas con 'dedos' en las puntas, collar blanco."""
    for lado in (-1, 1):
        pts = [(cx, cy - 2 * k), (cx + lado * 14 * k, cy - 6 * k), (cx + lado * 30 * k, cy - 3 * k)]
        for j in range(5):
            pts.append((cx + lado * (34 - j * 2.5) * k, cy + (-1 + j * 1.2) * k))
            pts.append((cx + lado * (30 - j * 2.5) * k, cy + (0.5 + j * 1.2) * k))
        pts += [(cx + lado * 14 * k, cy + 4 * k), (cx, cy + 4 * k)]
        d.polygon(P(pts), fill=col)
    d.ellipse([s(cx - 3 * k), s(cy - 4 * k), s(cx + 3 * k), s(cy + 8 * k)], fill=col)
    d.ellipse([s(cx - 2.6 * k), s(cy - 7 * k), s(cx + 2.6 * k), s(cy - 3 * k)], fill=blanco)
    d.polygon(P([(cx - 3 * k, cy + 7 * k), (cx, cy + 12 * k), (cx + 3 * k, cy + 7 * k)]), fill=col)


def montanas(d, y0, y1, picos, col, x0=0, x1=CW):
    pts = [(x0, y1)]
    n = len(picos)
    for i, h in enumerate(picos):
        x = x0 + (x1 - x0) * (i + 0.5) / n
        pts += [(x - (x1 - x0) / n * 0.5, y0 + (y1 - y0) * 0.6), (x, y0 + (y1 - y0) * (1 - h))]
    pts += [(x1, y0 + (y1 - y0) * 0.6), (x1, y1)]
    d.polygon(P(pts), fill=col)


def copo(d, cx, cy, r, col, ancho=1.0):
    for k in range(6):
        a = k * math.pi / 3
        x, y = cx + math.cos(a) * r, cy + math.sin(a) * r
        d.line(P([(cx, cy), (x, y)]), fill=col, width=max(1, s(ancho)))
        for f in (0.45, 0.72):
            bx, by = cx + math.cos(a) * r * f, cy + math.sin(a) * r * f
            for da in (-0.6, 0.6):
                d.line(P([(bx, by), (bx + math.cos(a + da) * r * 0.25, by + math.sin(a + da) * r * 0.25)]), fill=col,
                       width=max(1, s(ancho * 0.8)))
    d.ellipse([s(cx - r * 0.12), s(cy - r * 0.12), s(cx + r * 0.12), s(cy + r * 0.12)], fill=col)


def sol(d, cx, cy, r, col, rayos=16, cara=False, oscuro_=None):
    for k in range(rayos):
        a = k * 2 * math.pi / rayos
        L = r * (1.7 if k % 2 == 0 else 1.45)
        d.polygon(P([(cx + math.cos(a - 0.09) * r, cy + math.sin(a - 0.09) * r), (cx + math.cos(a) * L, cy + math.sin(a) * L),
                     (cx + math.cos(a + 0.09) * r, cy + math.sin(a + 0.09) * r)]), fill=col)
    d.ellipse([s(cx - r), s(cy - r), s(cx + r), s(cy + r)], fill=col)
    if cara and oscuro_:
        for lado in (-1, 1):
            d.arc([s(cx + lado * r * 0.38 - r * 0.2), s(cy - r * 0.3), s(cx + lado * r * 0.38 + r * 0.2), s(cy + r * 0.05)],
                  200, 340, fill=oscuro_, width=max(1, s(r * 0.07)))
        d.arc([s(cx - r * 0.38), s(cy + r * 0.05), s(cx + r * 0.38), s(cy + r * 0.55)], 20, 160, fill=oscuro_, width=max(1, s(r * 0.07)))


def mate(d, cx, cy, k, cuerpo, virola, bombilla):
    """Mate de calabaza con virola y bombilla."""
    d.ellipse([s(cx - 13 * k), s(cy - 10 * k), s(cx + 13 * k), s(cy + 16 * k)], fill=cuerpo)
    d.rectangle([s(cx - 11 * k), s(cy - 12 * k), s(cx + 11 * k), s(cy - 7 * k)], fill=virola)
    d.ellipse([s(cx - 11 * k), s(cy - 14 * k), s(cx + 11 * k), s(cy - 9 * k)], fill=oscuro(virola, 0.6) + (255,))
    d.line(P([(cx + 2 * k, cy - 11 * k), (cx + 12 * k, cy - 30 * k)]), fill=bombilla, width=max(1, s(2.2 * k)))
    d.ellipse([s(cx + 10 * k), s(cy - 32 * k), s(cx + 15 * k), s(cy - 28 * k)], fill=bombilla)
    d.arc([s(cx - 9 * k), s(cy - 4 * k), s(cx + 5 * k), s(cy + 12 * k)], 110, 200, fill=claro(cuerpo, 0.35) + (255,),
          width=max(1, s(1.4 * k)))


def llama(d, cx, base, alto, ancho, col):
    pts = [(cx - ancho, base)]
    for j in range(7):
        t = j / 6
        pts.append((cx - ancho + 2 * ancho * t, base - alto * (0.35 + 0.65 * math.sin(math.pi * t) ** 2) * (0.75 + 0.25 * (j % 2))))
    pts.append((cx + ancho, base))
    d.polygon(P(pts), fill=col)


def cuernos(d, cx, cy, k, col):
    for lado in (-1, 1):
        pts = []
        for j in range(12):
            t = j / 11
            a = math.pi * (0.5 + 0.75 * t)
            r = 10 * k * (1 - 0.35 * t)
            pts.append((cx + lado * (4 * k + math.cos(a) * -r * 0.8 + r * 0.9 * t), cy - math.sin(a) * r * 1.3))
        for j in range(11, -1, -1):
            t = j / 11
            a = math.pi * (0.5 + 0.75 * t)
            r = 10 * k * (1 - 0.35 * t) - 3 * k * (1 - t)
            pts.append((cx + lado * (4 * k + math.cos(a) * -r * 0.8 + r * 0.9 * t), cy - math.sin(a) * r * 1.3))
        d.polygon(P(pts), fill=col)


def estrella(d, cx, cy, r, col, puntas=5):
    pts = []
    for k in range(puntas * 2):
        a = -math.pi / 2 + k * math.pi / puntas
        rr = r if k % 2 == 0 else r * 0.45
        pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
    d.polygon(P(pts), fill=col)


def guarda_pampa(d, x0, y0, x1, alto, c1, c2, c3):
    """Banda de guarda pampa: rombos escalonados y triángulos alternados."""
    d.rectangle([s(x0), s(y0), s(x1), s(y0 + alto)], fill=c1)
    paso = alto * 1.4
    n = int((x1 - x0) / paso)
    off = x0 + ((x1 - x0) - n * paso) / 2
    for i in range(n):
        x = off + i * paso
        d.polygon(P([(x, y0 + alto / 2), (x + paso / 2, y0 + 1), (x + paso, y0 + alto / 2), (x + paso / 2, y0 + alto - 1)]),
                  fill=c2 if i % 2 == 0 else c3)
        d.polygon(P([(x + paso / 2 - alto * 0.18, y0 + alto / 2), (x + paso / 2, y0 + alto * 0.32),
                     (x + paso / 2 + alto * 0.18, y0 + alto / 2), (x + paso / 2, y0 + alto * 0.68)]), fill=c1)


def escalonado(d, x0, y0, tam, col, orient=1):
    """Triángulo escalonado (como en los tejidos y la alfarería del norte)."""
    pasos = 4
    for i in range(pasos):
        w = tam * (pasos - i) / pasos
        d.rectangle([s(x0), s(y0 + orient * i * tam / pasos), s(x0 + w), s(y0 + orient * (i + 1) * tam / pasos)], fill=col)


# ================================================================== marcos

def marco_base(d, palo, color, ancho=1.6, doble=None):
    m = 7
    d.rounded_rectangle([s(m), s(m), s(CW - m), s(CH - m)], radius=s(5), outline=rgba(color), width=s(ancho))
    if doble is not None:
        d.rounded_rectangle([s(m + 2.6), s(m + 2.6), s(CW - m - 2.6), s(CH - m - 2.6)], radius=s(4), outline=rgba(doble),
                            width=s(0.7))
    n = G.PINTAS[palo]
    if n:
        anchop, total = 6, 0
        total = n * anchop + (n - 1) * 5
        x0 = CW / 2 - total / 2
        for k in range(n):
            x = x0 + k * (anchop + 5)
            for y in (m, CH - m):
                d.rectangle([s(x), s(y - 2.2), s(x + anchop), s(y + 2.2)], fill=rgba(G.PAPEL))


def esquinas(d, dibujar):
    """Llama a dibujar(cx, cy, sx, sy) en las cuatro esquinas (sx, sy = hacia adentro)."""
    for sx, sy, x, y in ((1, 1, 13, 13), (-1, 1, CW - 13, 13), (1, -1, 13, CH - 13), (-1, -1, CW - 13, CH - 13)):
        dibujar(x, y, sx, sy)


# ================================================================== temas

def tema_criolla():
    return dict(nombre="criolla")


def _oro_marco(d, palo):
    oro, osc = (214, 164, 40), (140, 96, 14)
    marco_base(d, palo, oro, 1.8, doble=osc)

    def filigrana(x, y, sx, sy):
        if (sx, sy) == (1, 1) or (sx, sy) == (-1, -1):
            return    # las esquinas del índice quedan libres
        for r, w in ((7, 1.0), (4.5, 0.8)):
            d.arc([s(x - r), s(y - r), s(x + r), s(y + r)], 0, 360, fill=rgba(oro), width=s(w))
        for a in range(4):
            ang = a * math.pi / 2 + math.pi / 4
            d.line(P([(x, y), (x + math.cos(ang) * 9, y + math.sin(ang) * 9)]), fill=rgba(osc), width=s(0.7))
        d.ellipse([s(x - 1.6), s(y - 1.6), s(x + 1.6), s(y + 1.6)], fill=rgba((176, 30, 36)))
    esquinas(d, filigrana)


def _oro_fondo(img, palo, numero):
    if numero > 10:
        return
    lay = capa()
    sol(ImageDraw.Draw(lay), CW / 2, CH / 2, 26, rgba((214, 164, 40)), rayos=24)
    fundir(img, lay, 0.10)


def tema_oro():
    return dict(nombre="oro", PAPEL=(250, 244, 226), TINTA=(70, 46, 12),
                COLOR={"espada": (28, 58, 120), "basto": (40, 92, 40), "oro": (172, 118, 8), "copa": (146, 22, 34)},
                ORO=(236, 186, 44), ORO_OSC=(140, 94, 10), MARCO=_oro_marco, FONDO=_oro_fondo,
                BORDE=((196, 148, 30), 2.4))


def _mandinga_marco(d, palo):
    rojo, brasa = (176, 30, 30), (232, 120, 40)
    marco_base(d, palo, (150, 110, 70), 1.4, doble=(90, 20, 20))
    for y, sentido in ((CH - 9.5, 1), (9.5, -1)):
        for i in range(9):
            x = 22 + i * 10.5
            if abs(x - CW / 2) < 14:
                continue
            capa_ = Image.new("RGBA", (W, H), (0, 0, 0, 0))
            e = ImageDraw.Draw(capa_)
            llama(e, x, y, 6.5 + (i % 3) * 1.5, 3.2, rgba(rojo))
            llama(e, x, y, 3.5 + (i % 2) * 1.2, 1.6, rgba(brasa))
            if sentido < 0:
                capa_ = capa_.transpose(Image.FLIP_TOP_BOTTOM)
            d._image.alpha_composite(capa_)


def _mandinga_fondo(img, palo, numero):
    lay = capa()
    e = ImageDraw.Draw(lay)
    cuernos(e, CW / 2, CH / 2 - 8 if numero <= 10 else CH - 52, 2.2, rgba((150, 24, 24)))
    fundir(img, lay, 0.16 if numero <= 10 else 0.10)


def tema_mandinga():
    return dict(nombre="mandinga", PAPEL=(34, 28, 28), TINTA=(8, 6, 6),
                COLOR={"espada": (168, 176, 214), "basto": (132, 184, 98), "oro": (226, 176, 64), "copa": (226, 70, 64)},
                ORO=(200, 150, 56), ORO_OSC=(110, 70, 20), PIEL=(214, 186, 170),
                MARCO=_mandinga_marco, FONDO=_mandinga_fondo, BORDE=((120, 20, 20), 2.0))


def _llanura_marco(d, palo):
    verde, ocre, crema = (70, 112, 48), (200, 150, 60), (246, 240, 220)
    marco_base(d, palo, verde, 1.6)
    guarda_pampa(d, 32, 3.2, CW - 32, 5.6, rgba(verde), rgba(ocre), rgba(crema))
    guarda_pampa(d, 32, CH - 8.8, CW - 32, 5.6, rgba(verde), rgba(ocre), rgba(crema))


def _llanura_fondo(img, palo, numero):
    lay = capa()
    ombu(ImageDraw.Draw(lay), CW / 2, CH - 26, 1.6, rgba((70, 112, 48)))
    fundir(img, lay, 0.10 if numero <= 10 else 0.06)


def tema_llanura():
    return dict(nombre="llanura", PAPEL=(247, 241, 222), MARCO=_llanura_marco, FONDO=_llanura_fondo,
                COLOR={"espada": (40, 86, 150), "basto": (58, 112, 40), "oro": (192, 134, 20), "copa": (170, 40, 40)},
                BORDE=((60, 90, 40), 1.4))


def _sabana_marco(d, palo):
    ocre, tostado = (182, 118, 36), (130, 70, 24)
    marco_base(d, palo, ocre, 1.6, doble=tostado)

    def rayos(x, y, sx, sy):
        if (sx, sy) in ((1, 1), (-1, -1)):
            return
        for k in range(7):
            a = (k / 6) * math.pi / 2
            dx, dy = math.cos(a) * sx, math.sin(a) * sy
            d.line(P([(x - sx * 6 + dx * 3, y - sy * 6 + dy * 3), (x - sx * 6 + dx * 11, y - sy * 6 + dy * 11)]),
                   fill=rgba(ocre), width=s(0.9))
        d.ellipse([s(x - sx * 6 - 2.5), s(y - sy * 6 - 2.5), s(x - sx * 6 + 2.5), s(y - sy * 6 + 2.5)], fill=rgba(tostado))
    esquinas(d, rayos)


def _sabana_fondo(img, palo, numero):
    lay = capa()
    nandu(ImageDraw.Draw(lay), CW / 2 - 4, CH - 28, 1.7, rgba((150, 90, 30)))
    fundir(img, lay, 0.10 if numero <= 10 else 0.05)


def tema_sabana():
    return dict(nombre="sabana", PAPEL=(248, 236, 208), MARCO=_sabana_marco, FONDO=_sabana_fondo,
                COLOR={"espada": (44, 80, 136), "basto": (98, 110, 30), "oro": (200, 128, 16), "copa": (176, 52, 30)},
                BORDE=((120, 70, 24), 1.4))


def _desierto_marco(d, palo):
    terra = (178, 82, 48)
    marco_base(d, palo, terra, 1.8)

    def esc(x, y, sx, sy):
        if (sx, sy) in ((1, 1), (-1, -1)):
            return
        lay = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        escalonado(ImageDraw.Draw(lay), 0, 0, 12, rgba(terra))
        recorte = lay.crop((0, 0, s(12), s(12)))
        if sx < 0:
            recorte = recorte.transpose(Image.FLIP_LEFT_RIGHT)
        if sy < 0:
            recorte = recorte.transpose(Image.FLIP_TOP_BOTTOM)
        d._image.alpha_composite(recorte, (s(x - (12 if sx < 0 else 0) + sx * -4), s(y - (12 if sy < 0 else 0) + sy * -4)))
    esquinas(d, esc)
    for y in (12.5, CH - 14.5):
        for i in range(12):
            x = 34 + i * 5.2
            d.rectangle([s(x), s(y), s(x + 2.6), s(y + 2)], fill=rgba(terra if i % 2 == 0 else (120, 60, 34)))


def _desierto_fondo(img, palo, numero):
    lay = capa()
    cardon(ImageDraw.Draw(lay), CW / 2, CH - 26, 2.2, rgba((140, 100, 40)))
    fundir(img, lay, 0.10 if numero <= 10 else 0.05)


def tema_desierto():
    return dict(nombre="desierto", PAPEL=(245, 230, 198), MARCO=_desierto_marco, FONDO=_desierto_fondo,
                COLOR={"espada": (46, 74, 128), "basto": (110, 96, 40), "oro": (196, 130, 24), "copa": (178, 64, 38)},
                BORDE=((150, 70, 40), 1.6))


def _monte_marco(d, palo):
    verde, claro_ = (40, 92, 52), (96, 150, 80)
    marco_base(d, palo, verde, 1.5)
    for x in (9.5, CW - 9.5):
        for i in range(10):
            y = 50 + i * 9.6
            lado = 1 if i % 2 == 0 else -1
            d.line(P([(x, y), (x + lado * 3.5, y + 4)]), fill=rgba(verde), width=s(0.6))
            hx, hy = x + lado * 4.4, y + 4.8
            d.polygon(P([(hx - 2.2, hy), (hx, hy - 2.6), (hx + 2.2, hy), (hx, hy + 2.6)]), fill=rgba(claro_ if i % 3 else verde))


def _monte_fondo(img, palo, numero):
    lay = capa()
    e = ImageDraw.Draw(lay)
    pino(e, CW / 2 - 18, CH - 24, 1.2, rgba((40, 92, 52)))
    pino(e, CW / 2 + 16, CH - 24, 1.5, rgba((40, 92, 52)))
    fundir(img, lay, 0.10 if numero <= 10 else 0.05)


def tema_monte():
    return dict(nombre="monte", PAPEL=(240, 243, 228), MARCO=_monte_marco, FONDO=_monte_fondo,
                COLOR={"espada": (36, 76, 132), "basto": (44, 104, 46), "oro": (176, 126, 20), "copa": (156, 36, 40)},
                BORDE=((36, 74, 40), 1.4))


def _nieve_marco(d, palo):
    hielo, hondo = (86, 136, 186), (40, 74, 120)
    marco_base(d, palo, hielo, 1.5, doble=(170, 200, 228))

    def cp(x, y, sx, sy):
        if (sx, sy) in ((1, 1), (-1, -1)):
            return
        copo(d, x - sx * 3, y - sy * 3, 7, rgba(hondo), 0.8)
    esquinas(d, cp)


def _nieve_fondo(img, palo, numero):
    lay = capa()
    copo(ImageDraw.Draw(lay), CW / 2, CH / 2, 34 if numero <= 10 else 22, rgba((86, 136, 186)), 2.0)
    fundir(img, lay, 0.09)


def tema_nieve():
    return dict(nombre="nieve", PAPEL=(241, 246, 250), MARCO=_nieve_marco, FONDO=_nieve_fondo,
                COLOR={"espada": (36, 70, 140), "basto": (40, 100, 80), "oro": (178, 128, 30), "copa": (164, 40, 64)},
                BORDE=((70, 110, 160), 1.4))


def _sierra_marco(d, palo):
    pizarra = (82, 86, 104)
    marco_base(d, palo, pizarra, 1.6)
    for y0, voltear in ((CH - 14, False), (3, True)):
        lay = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        e = ImageDraw.Draw(lay)
        montanas(e, CH - 14, CH - 6, [0.55, 0.9, 0.6, 0.8, 0.5, 0.95, 0.65], rgba(pizarra), 32, CW - 32)
        if voltear:
            lay = lay.transpose(Image.FLIP_TOP_BOTTOM)
        d._image.alpha_composite(lay)


def _sierra_fondo(img, palo, numero):
    lay = capa()
    condor(ImageDraw.Draw(lay), CW / 2, CH / 2 - 10 if numero <= 10 else 34, 1.6, rgba((60, 62, 76)), rgba((200, 200, 200)))
    fundir(img, lay, 0.10 if numero <= 10 else 0.06)


def tema_sierra():
    return dict(nombre="sierra", PAPEL=(238, 236, 228), MARCO=_sierra_marco, FONDO=_sierra_fondo,
                COLOR={"espada": (50, 70, 124), "basto": (70, 96, 60), "oro": (170, 130, 40), "copa": (150, 46, 52)},
                BORDE=((70, 72, 88), 1.5))


def _fund_marco(d, palo):
    azul, teal = (35, 54, 90), (110, 156, 154)
    marco_base(d, palo, azul, 1.7, doble=teal)

    def st(x, y, sx, sy):
        if (sx, sy) in ((1, 1), (-1, -1)):
            return
        estrella(d, x - sx * 4, y - sy * 4, 4.5, rgba((200, 162, 58)))
    esquinas(d, st)


def _fund_fondo(img, palo, numero):
    if numero > 10:
        return
    lay = capa()
    mate(ImageDraw.Draw(lay), CW / 2, CH / 2 + 6, 1.6, rgba((80, 110, 90)), rgba((200, 162, 58)), rgba((200, 200, 210)))
    fundir(img, lay, 0.08)


def tema_fundadores():
    return dict(nombre="fundadores", PAPEL=(247, 243, 233), MARCO=_fund_marco, FONDO=_fund_fondo,
                COLOR={"espada": (35, 54, 110), "basto": (60, 120, 110), "oro": (190, 140, 30), "copa": (160, 40, 50)},
                BORDE=((35, 54, 90), 1.6))


# ================================================================== dorsos

def dorso_base(fondo, marco1, marco2, borde):
    img = Image.new("RGBA", (W, H), rgba(fondo))
    return img, ImageDraw.Draw(img)


def terminar_dorso(img, d, marco1, marco2, borde):
    m = 7
    d.rounded_rectangle([s(m), s(m), s(CW - m), s(CH - m)], radius=s(5), outline=rgba(marco1), width=s(1.6))
    d.rounded_rectangle([s(m + 3), s(m + 3), s(CW - m - 3), s(CH - m - 3)], radius=s(4), outline=rgba(marco2), width=s(0.8))
    d.rounded_rectangle([0, 0, W - 1, H - 1], radius=s(9), outline=rgba(borde), width=s(1.4))
    img.putalpha(G.mascara_redondeada(W, H, s(9)))
    return img.resize((CW, CH), Image.LANCZOS)


def cinta(d, cx, y, txt, fondo, letra, borde, px=8):
    f = fuente(px)
    tw = d.textlength(txt, font=f)
    d.rounded_rectangle([s(cx) - tw / 2 - s(6), s(y), s(cx) + tw / 2 + s(6), s(y + px + 5)], radius=s(3), fill=rgba(fondo),
                        outline=rgba(borde), width=s(0.9))
    d.text((s(cx) - tw / 2, s(y + 1)), txt, font=f, fill=rgba(letra))


def degrade(img, arriba, abajo, y0=0, y1=CH):
    d = ImageDraw.Draw(img)
    for y in range(s(y0), s(y1)):
        t = (y - s(y0)) / max(1, s(y1) - s(y0))
        d.line([(0, y), (W, y)], fill=rgba(mezclar(arriba, abajo, t)))


def dorso_oro():
    fondo, oro, osc = (16, 24, 52), (226, 178, 50), (140, 96, 16)
    img, d = dorso_base(fondo, oro, osc, (8, 10, 24))
    paso = 16
    for y in range(-paso, CH + paso, paso):
        for x in range(-paso, CW + paso, paso):
            ox = x + (paso / 2 if (y // paso) % 2 else 0)
            d.arc([s(ox - 5), s(y - 5), s(ox + 5), s(y + 5)], 0, 360, fill=rgba((40, 52, 90)), width=s(0.7))
            d.ellipse([s(ox - 1), s(y - 1), s(ox + 1), s(y + 1)], fill=rgba((96, 80, 40)))
    cx, cy = CW / 2, CH / 2 - 8
    d.ellipse([s(cx - 36), s(cy - 36), s(cx + 36), s(cy + 36)], fill=rgba((12, 18, 40)), outline=rgba(oro), width=s(1.6))
    sol(d, cx, cy, 15, rgba(oro), rayos=24, cara=True, oscuro_=rgba(osc))
    for a in range(8):
        ang = a * math.pi / 4
        x, y = cx + math.cos(ang) * 31, cy + math.sin(ang) * 31
        d.ellipse([s(x - 2), s(y - 2), s(x + 2), s(y + 2)], fill=rgba(oro))
    cinta(d, cx, cy + 42, "EL ÚLTIMO TRUCO", (12, 18, 40), oro, oro, 7)
    for (x, y) in ((20, 20), (CW - 20, 20), (20, CH - 20), (CW - 20, CH - 20)):
        estrella(d, x, y, 5, rgba(oro), 4)
    return terminar_dorso(img, d, oro, osc, (8, 10, 24))


def dorso_mandinga():
    img, d = dorso_base((10, 6, 6), (150, 30, 30), (70, 14, 14), (0, 0, 0))
    lay = capa()
    e = ImageDraw.Draw(lay)
    for i in range(14):
        llama(e, 8 + i * 9, CH, 26 + (i * 37 % 19), 7, rgba((110 + (i * 13 % 60), 20, 16)))
    for i in range(14):
        llama(e, 12 + i * 9, CH, 12 + (i * 23 % 11), 3.5, rgba((230, 120, 40)))
    img.alpha_composite(lay.filter(ImageFilter.GaussianBlur(s(0.6))))
    d = ImageDraw.Draw(img)
    cx, cy = CW / 2, CH / 2 - 14
    # el sombrero negro y los ojos que brillan en la oscuridad
    halo = capa()
    ImageDraw.Draw(halo).ellipse([s(cx - 30), s(cy - 4), s(cx + 30), s(cy + 26)], fill=(160, 20, 20, 120))
    img.alpha_composite(halo.filter(ImageFilter.GaussianBlur(s(6))))
    d = ImageDraw.Draw(img)
    d.ellipse([s(cx - 34), s(cy - 4), s(cx + 34), s(cy + 6)], fill=rgba((4, 2, 2)))
    d.rounded_rectangle([s(cx - 17), s(cy - 26), s(cx + 17), s(cy + 1)], radius=s(4), fill=rgba((4, 2, 2)))
    d.rectangle([s(cx - 17), s(cy - 6), s(cx + 17), s(cy - 2)], fill=rgba((150, 20, 20)))
    for lado in (-1, 1):
        ojo = capa()
        ImageDraw.Draw(ojo).ellipse([s(cx + lado * 9 - 6), s(cy + 9), s(cx + lado * 9 + 6), s(cy + 17)], fill=(255, 60, 30, 200))
        img.alpha_composite(ojo.filter(ImageFilter.GaussianBlur(s(1.6))))
        d = ImageDraw.Draw(img)
        d.polygon(P([(cx + lado * 9 - 5, cy + 13), (cx + lado * 9, cy + 10.5), (cx + lado * 9 + 5, cy + 13),
                     (cx + lado * 9, cy + 15)]), fill=rgba((255, 200, 120)))
    cuernos(d, cx, cy - 30, 1.1, rgba((60, 10, 10)))
    cinta(d, cx, cy + 44, "LA SALAMANCA", (20, 4, 4), (226, 70, 50), (150, 30, 30), 7)
    return terminar_dorso(img, d, (150, 30, 30), (70, 14, 14), (0, 0, 0))


def dorso_llanura():
    img = Image.new("RGBA", (W, H), (0, 0, 0, 255))
    degrade(img, (150, 196, 226), (252, 222, 160), 0, 120)
    d = ImageDraw.Draw(img)
    sol(d, CW / 2 + 22, 96, 12, rgba((250, 200, 90)), rayos=20)
    for i, (col, y0) in enumerate((((140, 176, 80), 104), ((104, 150, 60), 118), ((76, 122, 46), 136))):
        pts = [(0, CH)]
        for x in range(0, CW + 8, 8):
            pts.append((x, y0 + 5 * math.sin(x / 19 + i * 1.7)))
        pts.append((CW, CH))
        d.polygon(P(pts), fill=rgba(col))
    ombu(d, CW / 2 - 18, 140, 1.25, rgba((40, 58, 30)))
    for x in (20, 30, 98, 108):
        d.line(P([(x, 160), (x, 152)]), fill=rgba((90, 70, 40)), width=s(1))
    d.line(P([(14, 155), (114, 155)]), fill=rgba((90, 70, 40)), width=s(0.6))
    guarda_pampa(d, 12, 12, CW - 12, 7, rgba((70, 112, 48)), rgba((200, 150, 60)), rgba((246, 240, 220)))
    guarda_pampa(d, 12, CH - 19, CW - 12, 7, rgba((70, 112, 48)), rgba((200, 150, 60)), rgba((246, 240, 220)))
    cinta(d, CW / 2, 30, "LA LLANURA", (70, 112, 48), (246, 240, 220), (200, 150, 60), 7)
    return terminar_dorso(img, d, (70, 112, 48), (200, 150, 60), (40, 60, 28))


def dorso_sabana():
    img = Image.new("RGBA", (W, H), (0, 0, 0, 255))
    degrade(img, (238, 120, 40), (250, 214, 120), 0, 130)
    d = ImageDraw.Draw(img)
    d.ellipse([s(CW / 2 - 26), s(70), s(CW / 2 + 26), s(122)], fill=rgba((255, 236, 150)))
    d.rectangle([0, s(126), W, H], fill=rgba((150, 92, 34)))
    for x, k in ((24, 1.0), (100, 1.3), (70, 0.7)):
        acacia(d, x, 128, k, rgba((50, 26, 12)))
    nandu(d, CW / 2 - 6, 150, 1.25, rgba((50, 26, 12)))
    for y in range(140, CH - 12, 6):
        d.line(P([(12, y), (CW - 12, y)]), fill=rgba((170, 108, 44)), width=s(0.5))
    cinta(d, CW / 2, 26, "LA SABANA", (120, 60, 20), (252, 226, 160), (60, 30, 10), 7)
    return terminar_dorso(img, d, (120, 60, 20), (240, 180, 90), (60, 30, 10))


def dorso_desierto():
    fondo, crema, osc = (178, 82, 48), (244, 228, 196), (110, 48, 26)
    img, d = dorso_base(fondo, crema, osc, osc)
    for fila in range(12):
        y = 10 + fila * 15
        for col in range(9):
            x = 6 + col * 14 + (7 if fila % 2 else 0)
            escalonado(d, x, y, 8, rgba(osc if (fila + col) % 2 else (196, 110, 70)), 1)
    cx, cy = CW / 2, CH / 2 - 6
    d.ellipse([s(cx - 36), s(cy - 36), s(cx + 36), s(cy + 36)], fill=rgba(crema), outline=rgba(osc), width=s(2))
    sol(d, cx + 12, cy - 12, 8, rgba((230, 150, 50)), rayos=12)
    cardon(d, cx - 6, cy + 26, 1.25, rgba((70, 96, 50)))
    d.rectangle([s(cx - 33), s(cy + 26), s(cx + 33), s(cy + 28)], fill=rgba((200, 160, 110)))
    cinta(d, cx, cy + 42, "EL DESIERTO", crema, osc, osc, 7)
    return terminar_dorso(img, d, crema, osc, osc)


def dorso_monte():
    fondo, hoja, hoja2 = (22, 52, 32), (46, 96, 56), (66, 126, 72)
    img, d = dorso_base(fondo, hoja2, hoja, (10, 26, 14))
    for fila in range(16):
        for col in range(10):
            x = 4 + col * 13 + (6 if fila % 2 else 0)
            y = 4 + fila * 12.5
            ang = (fila * 7 + col * 3) % 8 * 0.5
            hx, hy = math.cos(ang) * 4.5, math.sin(ang) * 4.5
            d.polygon(P([(x - hx, y - hy), (x - hy * 0.5, y + hx * 0.5), (x + hx, y + hy), (x + hy * 0.5, y - hx * 0.5)]),
                      fill=rgba(hoja if (fila + col) % 3 else hoja2))
    cx, cy = CW / 2, CH / 2 - 6
    d.ellipse([s(cx - 36), s(cy - 36), s(cx + 36), s(cy + 36)], fill=rgba((236, 230, 204)), outline=rgba(hoja2), width=s(2))
    d.line(P([(cx - 30, cy + 14), (cx + 30, cy + 6)]), fill=rgba((96, 66, 36)), width=s(2.6))
    for k in (-18, -4, 14):
        d.polygon(P([(cx + k, cy + 10), (cx + k + 5, cy + 3), (cx + k + 9, cy + 9)]), fill=rgba(hoja2))
    hornero(d, cx - 2, cy + 2, 1.35, rgba((168, 108, 58)), rgba((222, 176, 120)))
    cinta(d, cx, cy + 42, "EL MONTE", (236, 230, 204), (30, 70, 40), (30, 70, 40), 7)
    return terminar_dorso(img, d, hoja2, (180, 150, 80), (10, 26, 14))


def dorso_nieve():
    img = Image.new("RGBA", (W, H), (0, 0, 0, 255))
    degrade(img, (14, 26, 60), (60, 96, 150), 0, CH)
    d = ImageDraw.Draw(img)
    import random
    r = random.Random(7)
    for _ in range(90):
        x, y = r.uniform(10, CW - 10), r.uniform(10, CH - 10)
        rr = r.uniform(0.4, 1.1)
        d.ellipse([s(x - rr), s(y - rr), s(x + rr), s(y + rr)], fill=(240, 246, 255, 200))
    montanas(d, 120, CH, [0.5, 0.95, 0.7, 0.85, 0.55], rgba((200, 216, 236)), 0, CW)
    montanas(d, 140, CH, [0.6, 0.8, 0.5, 0.9], rgba((150, 176, 210)), -10, CW + 10)
    cx, cy = CW / 2, 70
    d.ellipse([s(cx - 30), s(cy - 30), s(cx + 30), s(cy + 30)], fill=rgba((20, 36, 80)), outline=rgba((200, 222, 246)), width=s(1.4))
    copo(d, cx, cy, 24, rgba((236, 244, 255)), 1.6)
    cinta(d, CW / 2, 106, "LA NIEVE", (20, 36, 80), (236, 244, 255), (200, 222, 246), 7)
    return terminar_dorso(img, d, (200, 222, 246), (100, 140, 190), (10, 18, 40))


def dorso_sierra():
    img = Image.new("RGBA", (W, H), (0, 0, 0, 255))
    degrade(img, (64, 54, 96), (226, 150, 120), 0, 120)
    d = ImageDraw.Draw(img)
    d.ellipse([s(CW / 2 - 14), s(92), s(CW / 2 + 14), s(120)], fill=rgba((250, 210, 150)))
    capas = [((120, 104, 140), 92, [0.6, 0.9, 0.7, 0.95, 0.65]), ((92, 84, 116), 110, [0.7, 0.5, 0.85, 0.6, 0.9]),
             ((66, 64, 88), 130, [0.55, 0.8, 0.6, 0.75]), ((44, 44, 62), 150, [0.5, 0.7, 0.55])]
    for col, y0, picos in capas:
        montanas(d, y0, CH, picos, rgba(col), -6, CW + 6)
    condor(d, CW / 2 - 4, 58, 1.5, rgba((24, 22, 30)), rgba((236, 232, 224)))
    condor(d, CW / 2 + 30, 40, 0.7, rgba((40, 34, 50)), rgba((200, 196, 190)))
    cinta(d, CW / 2, 22, "LA SIERRA", (44, 44, 62), (236, 232, 224), (160, 150, 170), 7)
    return terminar_dorso(img, d, (200, 196, 210), (110, 100, 130), (20, 20, 30))


def dorso_fundadores():
    img = Image.new("RGBA", (W, H), (0, 0, 0, 255))
    d = ImageDraw.Draw(img)
    colores = [(110, 156, 154), (30, 42, 68), (28, 28, 32), (35, 54, 90)]   # Nicolás, Agustín, Matías, Thiago
    cx, cy = CW / 2, CH / 2
    d.rectangle([0, 0, s(cx), s(cy)], fill=rgba(colores[0]))
    d.rectangle([s(cx), 0, W, s(cy)], fill=rgba(colores[1]))
    d.rectangle([0, s(cy), s(cx), H], fill=rgba(colores[2]))
    d.rectangle([s(cx), s(cy), W, H], fill=rgba(colores[3]))
    oro = (200, 162, 58)
    for (x, y, letra) in ((cx / 2, cy / 2, "N"), (cx * 1.5, cy / 2, "A"), (cx / 2, cy * 1.5, "M"), (cx * 1.5, cy * 1.5, "T")):
        f = fuente(18)
        tw = d.textlength(letra, font=f)
        d.text((s(x) - tw / 2, s(y - 11)), letra, font=f, fill=rgba(oro))
    d.line(P([(cx, 10), (cx, CH - 10)]), fill=rgba(oro), width=s(1))
    d.line(P([(10, cy), (CW - 10, cy)]), fill=rgba(oro), width=s(1))
    d.ellipse([s(cx - 26), s(cy - 26), s(cx + 26), s(cy + 26)], fill=rgba((246, 240, 224)), outline=rgba(oro), width=s(1.6))
    mate(d, cx - 2, cy + 6, 1.05, rgba((120, 80, 46)), rgba(oro), rgba((210, 210, 220)))
    cinta(d, cx, CH - 30, "LOS FUNDADORES", (246, 240, 224), (35, 54, 90), oro, 6)
    return terminar_dorso(img, d, oro, (246, 240, 224), (10, 14, 24))


# ================================================================== armado

TEMAS = {
    "criolla": (tema_criolla, G.dorso),
    "oro": (tema_oro, dorso_oro),
    "mandinga": (tema_mandinga, dorso_mandinga),
    "llanura": (tema_llanura, dorso_llanura),
    "sabana": (tema_sabana, dorso_sabana),
    "desierto": (tema_desierto, dorso_desierto),
    "monte": (tema_monte, dorso_monte),
    "nieve": (tema_nieve, dorso_nieve),
    "sierra": (tema_sierra, dorso_sierra),
    "fundadores": (tema_fundadores, dorso_fundadores),
}

BASE = dict(PAPEL=G.PAPEL, TINTA=G.TINTA, COLOR=dict(G.COLOR), ORO=G.ORO, ORO_OSC=G.ORO_OSC, PIEL=G.PIEL,
            FONDO=None, MARCO=None, BORDE=None)


def aplicar(t):
    for k, v in BASE.items():
        setattr(G, k, v)
    for k, v in t.items():
        if k != "nombre":
            setattr(G, k, v)


def atlas(clave):
    tema, dorso = TEMAS[clave]
    aplicar(tema())
    img = Image.new("RGBA", (10 * CW, 5 * CH), (0, 0, 0, 0))
    for fi, palo in enumerate(G.PALOS):
        for ci, n in enumerate(G.NUMEROS):
            img.paste(G.carta(palo, n), (ci * CW, fi * CH))
    img.paste(dorso(), (0, 4 * CH))
    aplicar({})
    return img


def main(claves):
    for clave in claves:
        a = atlas(clave)
        gui = os.path.join(TEX, "gui", f"cartas_{clave}.png")
        os.makedirs(os.path.dirname(gui), exist_ok=True)
        a.save(gui, optimize=True)
        print("baraja", clave, "->", os.path.getsize(gui) // 1024, "KB")


if __name__ == "__main__":
    main(sys.argv[1:] or list(TEMAS))
