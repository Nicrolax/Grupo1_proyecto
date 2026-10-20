"""
Música original para los discos de la Fase 5, sintetizada de cero (sin samples):
  milonga   "Milonga del Truco"          guitarras y bandoneón, Mi menor, ritmo 3-3-2
  candombe  "Candombe de Llamadas"       cuerda de tambores: chico, repique, piano y madera
  vals      "Vals de la Pulpería"        vals criollo en Re mayor: bandoneón y guitarra
  payada    "Payada del Forastero"       milonga lenta de guitarra sola, con bordoneo, La menor

Guitarras: cuerda pulsada (Karplus-Strong) con resonancia de caja. Bandoneón: lengüetas (aditiva).
Tambores: osciladores con caída de tono + ruido. Todo con una reverb simple de sala.
Melodías generadas con reglas (notas del acorde en los tiempos fuertes, pasos por grado en los débiles),
con semilla fija: siempre salen iguales. Salida: .ogg mono (ffmpeg + libvorbis).
Uso: python3 tools/generar_musica.py
"""
import os, subprocess, tempfile, wave
import numpy as np
from scipy.signal import lfilter, butter

SR = 44100
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources",
                   "assets", "trucoya", "sounds", "disco")
os.makedirs(OUT, exist_ok=True)

NOTAS = {"C": 0, "C#": 1, "Db": 1, "D": 2, "D#": 3, "Eb": 3, "E": 4, "F": 5, "F#": 6, "Gb": 6, "G": 7,
         "G#": 8, "Ab": 8, "A": 9, "A#": 10, "Bb": 10, "B": 11}


def midi(nombre):
    """'E4' -> 64"""
    n, o = nombre[:-1], int(nombre[-1])
    return 12 * (o + 1) + NOTAS[n]


def hz(m):
    return 440.0 * 2 ** ((m - 69) / 12.0)


# acordes como (raíz midi en octava 3, intervalos)
TIPOS = {"m": (0, 3, 7), "M": (0, 4, 7), "7": (0, 4, 7, 10), "m7": (0, 3, 7, 10)}


def acorde(simbolo):
    raiz = simbolo.rstrip("m7M") if not simbolo.endswith("m7") else simbolo[:-2]
    if simbolo.endswith("m7"):
        tipo = "m7"
    elif simbolo.endswith("7"):
        tipo, raiz = "7", simbolo[:-1]
    elif simbolo.endswith("m"):
        tipo, raiz = "m", simbolo[:-1]
    else:
        tipo, raiz = "M", simbolo
    return 48 + NOTAS[raiz], TIPOS[tipo]


# ---------------------------------------------------------------- instrumentos

class Pista:
    def __init__(self, segundos):
        self.buf = np.zeros(int(SR * (segundos + 4)))

    def poner(self, t, señal, gan=1.0):
        i = int(t * SR)
        if i < 0 or i >= len(self.buf):
            return
        n = min(len(señal), len(self.buf) - i)
        self.buf[i:i + n] += señal[:n] * gan


def guitarra(f, dur, vel=0.6, brillo=0.5, rng=np.random.default_rng(0)):
    """Cuerda pulsada (Karplus-Strong) con caída según la altura y un toque de caja."""
    N = max(2, int(SR / f))
    L = int(SR * dur)
    y = np.zeros(L + N + 1)
    ruido = rng.uniform(-1, 1, N)
    # el "ataque": ruido suavizado según el brillo de la púa/yema
    k = max(1, int(1 + (1 - brillo) * 6))
    ruido = np.convolve(ruido, np.ones(k) / k, mode="same")
    y[:N] = ruido
    decae = 0.996 if f < 200 else 0.994 if f < 500 else 0.990
    i = N
    while i < L + N:
        j = min(i + N, L + N)
        n = j - i
        y[i:j] = decae * 0.5 * (y[i - N:i - N + n] + y[i - N - 1:i - N - 1 + n] if i - N - 1 >= 0
                                else y[i - N:i - N + n] * 2)
        i = j
    s = y[N:N + L]
    env = np.ones(L)
    fin = min(L, int(0.03 * SR))
    env[-fin:] = np.linspace(1, 0, fin)
    return s * env * vel


def caja_guitarra(x):
    """Resonancias de la caja de madera (dos filtros pasa-banda suaves sumados)."""
    b1, a1 = butter(2, [90 / (SR / 2), 260 / (SR / 2)], btype="band")
    b2, a2 = butter(2, [1800 / (SR / 2), 4200 / (SR / 2)], btype="band")
    return x + 0.35 * lfilter(b1, a1, x) + 0.12 * lfilter(b2, a2, x)


def bandoneon(f, dur, vel=0.5, vib=4.8):
    """Lengüetas libres: armónicos ricos, fuelle con ataque suave y un vibrato de mano."""
    L = int(SR * dur)
    t = np.arange(L) / SR
    v = 1 + 0.004 * np.sin(2 * np.pi * vib * t) * np.clip(t * 2, 0, 1)
    fase = 2 * np.pi * f * np.cumsum(v) / SR
    s = np.zeros(L)
    for n in range(1, 14):
        amp = (1.0 / n) * (1.25 if n % 2 else 0.8)
        s += amp * np.sin(n * fase)
    # segunda lengüeta apenas desafinada (el sonido doble del bandoneón)
    fase2 = 2 * np.pi * f * 1.0025 * np.cumsum(v) / SR
    for n in range(1, 8):
        s += 0.5 * (1.0 / n) * np.sin(n * fase2)
    att, rel = min(L, int(0.05 * SR)), min(L, int(0.09 * SR))
    env = np.ones(L)
    env[:att] = np.linspace(0, 1, att)
    env[-rel:] *= np.linspace(1, 0, rel)
    b, a = butter(2, 3800 / (SR / 2))
    return lfilter(b, a, s * env) * vel * 0.22


def tambor(f0, dur, vel, ruido=0.25, caida=6.0, rng=np.random.default_rng(1)):
    L = int(SR * dur)
    t = np.arange(L) / SR
    f = f0 * (1 + 0.6 * np.exp(-t * 40))
    fase = 2 * np.pi * np.cumsum(f) / SR
    s = np.sin(fase) * np.exp(-t * caida)
    n = rng.uniform(-1, 1, L) * np.exp(-t * 60) * ruido
    b, a = butter(2, [200 / (SR / 2), 5000 / (SR / 2)], btype="band")
    return (s + lfilter(b, a, n)) * vel


def madera(vel, rng=np.random.default_rng(2)):
    L = int(SR * 0.05)
    t = np.arange(L) / SR
    s = np.sin(2 * np.pi * 1750 * t) * np.exp(-t * 120) * 0.6
    n = rng.uniform(-1, 1, L) * np.exp(-t * 200)
    b, a = butter(2, [1500 / (SR / 2), 7000 / (SR / 2)], btype="band")
    return (s + lfilter(b, a, n)) * vel


def reverb(x, mezcla=0.22):
    """Reverb de sala (Schroeder: peines en paralelo + pasatodos en serie)."""
    salida = np.zeros_like(x)
    for d, g in ((1557, 0.80), (1617, 0.79), (1491, 0.78), (1422, 0.77)):
        a = np.zeros(d + 1)
        a[0], a[d] = 1, -g
        salida += lfilter([1], a, x)
    for d, g in ((225, 0.7), (556, 0.7)):
        b = np.zeros(d + 1)
        b[0], b[d] = -g, 1
        a = np.zeros(d + 1)
        a[0], a[d] = 1, -g
        salida = lfilter(b, a, salida)
    return x * (1 - mezcla) + salida * mezcla * 0.25


# ---------------------------------------------------------------- composición


def escala(tonica, menor=True):
    pasos = (0, 2, 3, 5, 7, 8, 11) if menor else (0, 2, 4, 5, 7, 9, 11)
    base = NOTAS[tonica]
    return sorted({12 * o + base + p for o in range(4, 7) for p in pasos})


def melodia(rng, acordes, golpes_por_compas, ritmos, esc, rango=(62, 81), inicio=None):
    """Genera una frase: lista de (compás, pulso_inicio, duración_en_pulsos, midi)."""
    notas = []
    actual = inicio or (rango[0] + rango[1]) // 2
    for c, simb in enumerate(acordes):
        raiz, ints = acorde(simb)
        del_acorde = [n for n in esc if (n - raiz) % 12 in [i % 12 for i in ints]]
        ritmo = ritmos[rng.integers(len(ritmos))]
        for k, (p, d) in enumerate(ritmo):
            fuerte = p in (0, golpes_por_compas // 2) or k == 0
            candidatos = del_acorde if fuerte else esc
            candidatos = [n for n in candidatos if rango[0] <= n <= rango[1] and abs(n - actual) <= 7]
            if not candidatos:
                candidatos = [min(del_acorde, key=lambda n: abs(n - actual))]
            pesos = np.array([1.0 / (1 + abs(n - actual)) ** 1.3 for n in candidatos])
            actual = candidatos[rng.choice(len(candidatos), p=pesos / pesos.sum())]
            notas.append((c, p, d, actual))
    return notas


def exportar(nombre, x):
    x = x / (np.max(np.abs(x)) + 1e-9) * 0.89
    fade = int(SR * 2.5)
    x[-fade:] *= np.linspace(1, 0, fade)
    x[:int(SR * 0.05)] *= np.linspace(0, 1, int(SR * 0.05))
    pcm = (x * 32767).astype(np.int16)
    with tempfile.TemporaryDirectory() as d:
        w = os.path.join(d, "t.wav")
        with wave.open(w, "wb") as f:
            f.setnchannels(1)
            f.setsampwidth(2)
            f.setframerate(SR)
            f.writeframes(pcm.tobytes())
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", w, "-c:a", "libvorbis", "-q:a", "3",
                        os.path.join(OUT, nombre + ".ogg")], check=True)
    print(nombre, f"{len(x) / SR:.0f} s")


def recortar(p, segundos):
    return p.buf[:int(SR * segundos)]


# ================================================================ Milonga del Truco

def milonga():
    rng = np.random.default_rng(1810)
    bpm = 104
    pulso = 60 / bpm                       # negra
    compas = 4 * pulso
    A = ["Em", "Em", "B7", "B7", "B7", "B7", "Em", "Em"]
    B = ["Am", "Am", "Em", "Em", "B7", "B7", "Em", "E7"]
    C = ["Am", "Am", "Em", "Em", "F#7", "B7", "Em", "Em"]
    forma = [("intro", ["Em", "B7"])] + [("A", A), ("A", A), ("B", B), ("C", C), ("A", A)] + [("fin", ["Em", "Em"])]
    total = sum(len(a) for _, a in forma) * compas
    gtr, bnd = Pista(total), Pista(total)
    esc = escala("E", menor=True)
    ritmos = [[(0, 1.5), (1.5, 1.5), (3, 1)], [(0, 0.5), (0.5, 0.5), (1, 1), (2, 1), (3, 1)],
              [(0, 1.5), (1.5, 0.5), (2, 1), (3, 0.5), (3.5, 0.5)], [(0, 2), (2, 1), (3, 1)]]
    frases = {}
    t0 = 0.0
    for nombre, acordes in forma:
        if nombre in ("A", "B", "C") and nombre not in frases:
            frases[nombre] = melodia(rng, acordes, 4, ritmos, esc, rango=(64, 83))
        for c, simb in enumerate(acordes):
            t = t0 + c * compas
            raiz, ints = acorde(simb)
            # bajo en 3-3-2: tónica, quinta, tónica (octava grave)
            for corch, intervalo in ((0, 0), (3, 7), (6, 12 if c % 2 else 0)):
                gtr.poner(t + corch * pulso / 2, guitarra(hz(raiz - 12 + intervalo), 1.0, 0.75, 0.3), 1.0)
            # rasguido/arpegio de acorde en 3-3-2
            voces = [raiz + i for i in ints] + [raiz + 12]
            for corch, gan in ((0, 0.35), (3, 0.5), (6, 0.45)):
                for k, m in enumerate(voces):
                    gtr.poner(t + corch * pulso / 2 + k * 0.012, guitarra(hz(m), 0.7, 0.25, 0.6), gan)
        if nombre in frases:
            for c, p, d, m in frases[nombre]:
                dur = d * pulso * 0.95
                bnd.poner(t0 + c * compas + p * pulso, bandoneon(hz(m), dur, 0.55), 1.0)
                if nombre == "A" and t0 > 3 * 8 * compas:     # última A: la guitarra dobla la melodía
                    gtr.poner(t0 + c * compas + p * pulso, guitarra(hz(m), dur + 0.3, 0.35, 0.8), 1.0)
        if nombre == "fin":
            for k, m in enumerate([52, 59, 64, 67, 71, 76]):
                gtr.poner(t0 + compas + k * 0.05, guitarra(hz(m), 3.0, 0.5, 0.6), 1.0)
            bnd.poner(t0, bandoneon(hz(76), compas * 1.8, 0.5), 1.0)
        t0 += len(acordes) * compas
    mezcla = caja_guitarra(gtr.buf) * 0.9 + bnd.buf * 1.0
    return reverb(recortar_buf(mezcla, total + 3), 0.25)


def recortar_buf(x, segundos):
    return x[:int(SR * segundos)]


# ================================================================ Candombe de Llamadas

def candombe():
    rng = np.random.default_rng(1830)
    bpm = 118
    semi = 60 / bpm / 4                    # semicorchea
    compases = 44
    total = compases * 16 * semi
    p = Pista(total)
    clave = [0, 3, 6, 10, 12]
    for c in range(compases):
        t = c * 16 * semi
        entra_chico = c >= 2
        entra_piano = c >= 4
        entra_repique = c >= 6
        llamada = c % 8 == 7                   # cada 8 compases, la "llamada" del repique
        # madera (palo contra el casco)
        for s in clave:
            p.poner(t + s * semi, madera(0.55), 1.0)
        # chico: el que lleva el tiempo, tres golpes por pulso
        if entra_chico:
            for pulso in range(4):
                for s, (vel, f) in zip((1, 2, 3), ((0.5, 420), (0.32, 410), (0.42, 430))):
                    p.poner(t + (pulso * 4 + s) * semi, tambor(f, 0.18, vel, ruido=0.35, caida=18), 1.0)
        # piano: la base grave, con variaciones
        if entra_piano:
            base = [(0, 0.9, "abierto"), (6, 0.7, "abierto"), (8, 0.6, "tapado"), (11, 0.75, "abierto"),
                    (14, 0.6, "tapado")] if c % 4 != 3 else \
                [(0, 0.9, "abierto"), (3, 0.6, "abierto"), (6, 0.8, "abierto"), (9, 0.6, "tapado"),
                 (10, 0.7, "abierto"), (12, 0.8, "abierto"), (14, 0.7, "abierto")]
            for s, vel, tipo in base:
                f, caida = (78, 4.5) if tipo == "abierto" else (95, 14)
                p.poner(t + s * semi, tambor(f, 0.7, vel, ruido=0.2, caida=caida), 1.0)
        # repique: improvisa entre madera y golpes abiertos
        if entra_repique:
            if llamada:
                for s in range(0, 16):
                    p.poner(t + s * semi, tambor(230, 0.25, 0.35 + 0.03 * s, ruido=0.3, caida=12), 1.0)
            else:
                for s in range(16):
                    r = rng.random()
                    if s in (2, 7, 11) and r < 0.75:
                        p.poner(t + s * semi, tambor(240, 0.3, 0.6, ruido=0.3, caida=10), 1.0)
                    elif r < 0.22:
                        p.poner(t + s * semi, madera(0.32), 1.0)
                    elif r < 0.30:
                        p.poner(t + s * semi, tambor(250, 0.2, 0.35, ruido=0.4, caida=16), 1.0)
    # pequeño "cierre" final: todos juntos
    t = compases * 16 * semi
    for s in range(0, 8, 2):
        p.poner(t + s * semi, tambor(80, 0.8, 0.9, caida=4), 1.0)
        p.poner(t + s * semi, tambor(240, 0.4, 0.6, caida=8), 1.0)
        p.poner(t + s * semi, tambor(420, 0.2, 0.5, caida=18), 1.0)
    b, a = butter(2, 30 / (SR / 2), btype="high")
    return reverb(lfilter(b, a, recortar(p, total + 3)), 0.18)


# ================================================================ Vals de la Pulpería

def vals():
    rng = np.random.default_rng(1856)
    bpm = 168
    pulso = 60 / bpm
    compas = 3 * pulso
    A1 = ["D", "D", "A7", "A7", "A7", "A7", "D", "D"]
    A2 = ["D", "D7", "G", "G", "A7", "A7", "D", "D"]
    B1 = ["Bm", "Bm", "F#7", "F#7", "F#7", "F#7", "Bm", "Bm"]
    B2 = ["G", "G", "D", "D", "E7", "A7", "D", "D"]
    forma = [("A1", A1), ("A2", A2), ("B1", B1), ("B2", B2), ("A1", A1), ("A2", A2), ("B1", B1), ("B2", B2),
             ("A1", A1), ("A2", A2), ("fin", ["D", "D"])]
    total = sum(len(a) for _, a in forma) * compas
    gtr, bnd = Pista(total), Pista(total)
    esc_M = escala("D", menor=False)
    esc_m = escala("B", menor=True)
    ritmos = [[(0, 1), (1, 1), (2, 1)], [(0, 2), (2, 1)], [(0, 1.5), (1.5, 0.5), (2, 1)], [(0, 3)],
              [(0, 1), (1, 0.5), (1.5, 0.5), (2, 1)]]
    frases = {}
    t0 = 0.0
    for nombre, acordes in forma:
        if nombre != "fin" and nombre not in frases:
            frases[nombre] = melodia(rng, acordes, 3, ritmos, esc_m if nombre.startswith("B") else esc_M,
                                     rango=(66, 86))
        for c, simb in enumerate(acordes):
            t = t0 + c * compas
            raiz, ints = acorde(simb)
            bajo = raiz - 12 + (0 if c % 2 == 0 else 7)
            gtr.poner(t, guitarra(hz(bajo), 1.2, 0.8, 0.3), 1.0)                # tum
            voces = [raiz + i for i in ints[:3]] + [raiz + 12]
            for pulso_n in (1, 2):                                                # pa, pa
                for k, m in enumerate(voces):
                    gtr.poner(t + pulso_n * pulso + k * 0.008, guitarra(hz(m), 0.32, 0.28, 0.7), 1.0)
        if nombre in frases:
            for c, p, d, m in frases[nombre]:
                bnd.poner(t0 + c * compas + p * pulso, bandoneon(hz(m), d * pulso * 0.92, 0.5, vib=5.5), 1.0)
                # la segunda voz del bandoneón, una tercera abajo, en las partes B
                if nombre.startswith("B"):
                    bnd.poner(t0 + c * compas + p * pulso, bandoneon(hz(m - 4), d * pulso * 0.92, 0.25), 1.0)
        if nombre == "fin":
            for k, m in enumerate([50, 57, 62, 66, 69, 74]):
                gtr.poner(t0 + k * 0.06, guitarra(hz(m), 3.5, 0.45, 0.6), 1.0)
            bnd.poner(t0, bandoneon(hz(74), compas * 2, 0.5), 1.0)
        t0 += len(acordes) * compas
    mezcla = caja_guitarra(gtr.buf) * 0.85 + bnd.buf
    return reverb(recortar_buf(mezcla, total + 3), 0.26)


# ================================================================ Payada del Forastero

def payada():
    rng = np.random.default_rng(1872)
    bpm = 76
    pulso = 60 / bpm
    compas = 4 * pulso
    estrofa = ["Am", "Am", "E7", "E7", "E7", "E7", "Am", "Am", "A7", "A7", "Dm", "Dm", "Am", "E7", "Am", "Am"]
    forma = [("intro", ["Am", "E7", "Am", "Am"]), ("est", estrofa), ("punteo", estrofa), ("fin", ["Am", "Am"])]
    total = sum(len(a) for _, a in forma) * compas
    g = Pista(total)
    esc = escala("A", menor=True)
    t0 = 0.0
    for nombre, acordes in forma:
        frase = melodia(rng, acordes, 4, [[(0, 1), (1, 0.5), (1.5, 0.5), (2, 2)], [(0, 1.5), (1.5, 0.5), (2, 1), (3, 1)],
                                          [(0, 0.5), (0.5, 0.5), (1, 0.5), (1.5, 0.5), (2, 2)]],
                        esc, rango=(64, 81)) if nombre == "punteo" else []
        for c, simb in enumerate(acordes):
            t = t0 + c * compas
            raiz, ints = acorde(simb)
            # bordoneo: el bajo canta en 3-3-2 sobre las cuerdas graves
            camino = [0, 7, 12] if c % 2 == 0 else [0, 3 if simb.endswith("m") else 4, 7]
            for corch, iv in zip((0, 3, 6), camino):
                g.poner(t + corch * pulso / 2, guitarra(hz(raiz - 12 + iv), 1.4, 0.8, 0.25), 1.0)
            # arpegio de dedos en las cuerdas agudas
            voces = [raiz + 12 + i for i in ints[:3]]
            for corch in (1, 2, 4, 5, 7):
                m = voces[(corch + c) % len(voces)]
                g.poner(t + corch * pulso / 2, guitarra(hz(m), 0.8, 0.3 if nombre != "punteo" else 0.18, 0.7), 1.0)
        for c, p, d, m in frase:
            g.poner(t0 + c * compas + p * pulso, guitarra(hz(m), d * pulso + 0.6, 0.55, 0.85), 1.0)
            if rng.random() < 0.25:                                           # ligado: nota de adorno
                g.poner(t0 + c * compas + p * pulso + 0.07, guitarra(hz(m + 2), 0.2, 0.2, 0.9), 1.0)
        if nombre == "fin":
            for k, m in enumerate([45, 52, 57, 60, 64, 69]):
                g.poner(t0 + k * 0.07, guitarra(hz(m), 4.0, 0.5, 0.5), 1.0)
        t0 += len(acordes) * compas
    return reverb(recortar_buf(caja_guitarra(g.buf), total + 4), 0.28)


if __name__ == "__main__":
    exportar("milonga", milonga())
    exportar("candombe", candombe())
    exportar("vals", vals())
    exportar("payada", payada())
