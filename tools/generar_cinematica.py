"""
Cinemática "El Último Truco" (al encontrar al Viejo Vizcacha por primera vez).

Genera:
  sounds/cinematica/narracion_N.ogg   la voz del Vizcacha contando la historia
  sounds/cinematica/carga.ogg         el grito de los jinetes ("¡A la carga!")
  sounds/cinematica/musica.ogg        la banda sonora, compuesta segundo a segundo sobre la línea de tiempo
  cinematica/vizcacha.json            subtítulos y carteles sincronizados (lo lee el juego)
y agrega los sonidos a sounds.json.

LA HISTORIA (original)
  Presente   El Vizcacha reconoce al jugador: "Antes de las cartas, yo conocí la guerra".
  El fogón   Invierno del cuarenta, Paso de las Ánimas. La noche antes de la batalla, cuatro jinetes
             juegan al truco junto al fuego. El Negro Ledesma canta la flor más linda y le gana el facón.
  La carga   Al alba, trescientos jinetes cargan bajo los truenos.
  La caída   Ledesma cae. Lo último que hace es darle su baraja: la Baraja de Oro.
  Presente   "Juré no jugar más que contra quien la mereciera. Sentate, gurí".

Uso: python3 tools/generar_cinematica.py   (requiere lo mismo que generar_voces_neural.py)
"""
import json, os, sys
import numpy as np
from scipy.signal import butter, lfilter

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import generar_voces_neural as V   # noqa: E402
import generar_musica as M          # noqa: E402

ASSETS = os.path.join(HERE, "..", "src", "main", "resources", "assets", "trucoya")
SND = os.path.join(ASSETS, "sounds", "cinematica")
SR = 24000            # voz
MSR = M.SR            # música (44,1 kHz)
DURACION = 90.0

# --------------------------------------------------------------- guion: (inicio, fin máximo, texto)
NARRACION = [
    (1.6, 6.3, "Así que vos sos el que anda ganando en todas las pulperías..."),
    (6.6, 11.4, "Mirá, gurí. Antes de las cartas... yo conocí la guerra."),
    (16.0, 24.6, "Fue en el invierno del cuarenta, en el Paso de las Ánimas. "
                 "La noche antes de la pelea, jugábamos al truco junto al fogón."),
    (25.0, 31.3, "El Negro Ledesma cantó la flor más linda que vi en mi vida... y se ganó mi facón."),
    (35.0, 41.0, "Al alba sonó el clarín. Éramos trescientos jinetes contra el mundo entero."),
    (47.0, 54.5, "La tierra temblaba bajo los cascos... y el cielo se partía en truenos."),
    (59.0, 66.8, "Ahí cayó el Negro. Lo último que hizo fue darme su baraja. La Baraja de Oro."),
    (70.6, 76.6, "Desde ese día juré no jugar más que contra quien la mereciera."),
    (77.2, 83.4, "Setenta inviernos esperé. Sentate, gurí... vamos a ver si sos digno."),
]
CARTELES = [
    (14.2, 19.5, "PASO DE LAS ÁNIMAS", "Invierno del cuarenta"),
    (83.6, 88.6, "EL ÚLTIMO TRUCO", "del Viejo Vizcacha"),
]
GRITO = 41.6


def guardar(x, sr, ruta):
    import tempfile, wave, subprocess
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    x = np.asarray(x, dtype=np.float32)
    with tempfile.TemporaryDirectory() as d:
        w = os.path.join(d, "t.wav")
        with wave.open(w, "wb") as f:
            f.setnchannels(1)
            f.setsampwidth(2)
            f.setframerate(sr)
            f.writeframes((np.clip(x, -1, 1) * 32767).astype(np.int16).tobytes())
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", w, "-c:a", "libvorbis", "-q:a", "4", ruta],
                       check=True)


# =============================================================== voces

def voces():
    sintetizar = V.crear_sintetizador()
    asr = V.reconocedor()
    import calidad_voz
    subtitulos = []
    for n, (t0, t1, texto) in enumerate(NARRACION, start=1):
        hueco = t1 - t0
        mejor = None
        for vel in (0.93, 0.97, 1.01, 1.05, 1.10, 1.16):       # (x0.86 en la voz del viejo)
            x = V.recortar(sintetizar("viejo", texto, vel, semilla=n))
            if len(x) / SR > hueco and vel < 1.16:
                continue
            x = V.eq(x, 80, 0, "hp")
            x = V.eq(x, 200, 2.0)                                       # cuerpo de voz grave
            x = V.sala(x, 0.12)
            x = x * (0.13 / (np.sqrt(np.mean(x ** 2)) + 1e-9))
            x = x * min(1.0, 0.95 / (np.abs(x).max() + 1e-9))
            nota = calidad_voz.nota(x, SR)
            if mejor is None or nota > mejor[1]:
                mejor = (x, nota, vel)
            if len(x) / SR <= hueco - 0.6:     # entra holgada: probamos una versión más (más natural, quizá)
                continue
            break
        x, nota, vel = mejor
        guardar(x, SR, os.path.join(SND, f"narracion_{n}.ogg"))
        dur = len(x) / SR
        s = asr.create_stream()
        s.accept_waveform(SR, x)
        asr.decode_stream(s)
        print(f"narración {n}: {dur:.1f} s (hueco {hueco:.1f}) vel {vel:.2f} nota {nota:.2f} -> {s.result.text}", flush=True)
        subtitulos.append({"t": t0, "fin": round(t0 + max(dur, 2.0) + 0.4, 2), "texto": texto,
                           "sonido": f"cinematica.narracion_{n}"})
    # el grito de la carga: varias voces juntas, desparejas como un escuadrón
    capas = []
    for voz, vel, vs in (("hombre", 1.05, 1.0), ("gaucho", 1.0, 0.96), ("viejo", 1.25, 1.0),
                         ("hombre", 0.95, 0.92), ("gaucho", 1.08, 1.05)):
        x = sintetizar(voz, "¡A la carga, muchachos!", vel)
        if vs != 1.0:
            x = V.varispeed(x, vs)
        capas.append(V.terminar(x, "gritado"))
    L = max(len(c) for c in capas) + int(SR * 0.4)
    mezcla = np.zeros(L)
    for k, c in enumerate(capas):
        off = int(SR * 0.06 * k)
        mezcla[off:off + len(c)] += c * (1.0 if k == 0 else 0.7)
    mezcla = V.sala(mezcla / np.abs(mezcla).max() * 0.9, 0.3)
    guardar(mezcla / np.abs(mezcla).max() * 0.9, SR, os.path.join(SND, "carga.ogg"))
    return subtitulos


# =============================================================== música

def pad(freqs, dur, vol=0.2, ataque=1.5, brillo=1800):
    L = int(MSR * dur)
    t = np.arange(L) / MSR
    s = np.zeros(L)
    for f in freqs:
        for det in (-0.004, 0.0, 0.005):
            fase = 2 * np.pi * f * (1 + det) * t
            s += (2 * ((fase / (2 * np.pi)) % 1) - 1) * 0.33        # sierras desafinadas (cuerdas)
    b, a = butter(2, brillo / (MSR / 2))
    s = lfilter(b, a, s)
    env = np.minimum(1, t / ataque) * np.minimum(1, (dur - t) / min(ataque, dur / 2))
    return s * env * vol / max(1, len(freqs))


def coro(f, dur, vol=0.18):
    """'Aah' de coro: sierra filtrada por formantes."""
    L = int(MSR * dur)
    t = np.arange(L) / MSR
    vib = 1 + 0.006 * np.sin(2 * np.pi * 5.2 * t)
    fase = 2 * np.pi * f * np.cumsum(vib) / MSR
    s = 2 * ((fase / (2 * np.pi)) % 1) - 1
    salida = np.zeros(L)
    for fc, bw, g in ((750, 120, 1.0), (1150, 140, 0.6), (2900, 250, 0.25)):
        b, a = butter(2, [(fc - bw) / (MSR / 2), (fc + bw) / (MSR / 2)], btype="band")
        salida += lfilter(b, a, s) * g
    env = np.minimum(1, t / 1.2) * np.minimum(1, (dur - t) / 1.2)
    return salida * env * vol


def bronce(f, dur, vol=0.25):
    L = int(MSR * dur)
    t = np.arange(L) / MSR
    s = sum(np.sin(2 * np.pi * f * n * t) / n for n in range(1, 16))
    env = np.minimum(1, t / 0.06) * np.exp(-t * 0.6) * np.minimum(1, (dur - t) / 0.2)
    corte = 1 - np.exp(-t * 8)
    b, a = butter(2, 2600 / (MSR / 2))
    return lfilter(b, a, s * (0.4 + 0.6 * corte)) * env * vol * 0.3


def platillo(dur, vol=0.15, crece=True):
    L = int(MSR * dur)
    t = np.arange(L) / MSR
    n = np.random.default_rng(7).uniform(-1, 1, L)
    b, a = butter(2, 5000 / (MSR / 2), btype="high")
    env = (t / dur) ** 2 if crece else np.exp(-t * 2)
    return lfilter(b, a, n) * env * vol


def campana(f, dur, vol=0.15):
    L = int(MSR * dur)
    t = np.arange(L) / MSR
    return (np.sin(2 * np.pi * f * t) + 0.5 * np.sin(2 * np.pi * f * 2.76 * t)) * np.exp(-t * 1.5) * vol


def musica(subtitulos):
    p = M.Pista(DURACION)
    hz, midi = M.hz, M.midi

    # --- Presente (0-13): bordón grave, coro lejano y un latido
    p.poner(0.0, pad([hz(midi("D2")), hz(midi("A2"))], 13.5, 0.35, ataque=3), 1.0)
    p.poner(2.0, coro(hz(midi("A4")), 10, 0.10), 1.0)
    for k in range(6):
        p.poner(1.0 + k * 2.0, M.tambor(52, 1.2, 0.5, ruido=0.1, caida=4), 1.0)
    p.poner(11.0, platillo(2.5, 0.10), 1.0)

    # --- El fogón (13.5-33): milonga lenta de guitarra y el lamento del bandoneón
    acordes = ["Dm", "Dm", "A7", "A7", "Gm", "Dm", "A7", "Dm"]
    compas = 2.4
    t = 13.6
    for c, simb in enumerate(acordes):
        raiz, ints = M.acorde(simb)
        for corch, iv in zip((0, 3, 6), (0, 7, 12)):
            p.poner(t + corch * compas / 8, M.guitarra(hz(raiz - 12 + iv), 1.6, 0.6, 0.3), 1.0)
        voces = [raiz + 12 + i for i in ints[:3]]
        for corch in (1, 2, 4, 5, 7):
            p.poner(t + corch * compas / 8, M.guitarra(hz(voces[(corch + c) % 3]), 0.9, 0.22, 0.7), 1.0)
        t += compas
    lamento = [("A4", 0, 1.2), ("D5", 1.2, 1.2), ("C#5", 2.4, 0.6), ("E5", 3.0, 1.8), ("D5", 4.8, 2.4),
               ("F5", 7.2, 1.2), ("E5", 8.4, 0.6), ("D5", 9.0, 0.6), ("C5", 9.6, 1.2), ("Bb4", 10.8, 1.2),
               ("A4", 12.0, 2.4), ("G4", 14.4, 1.2), ("A4", 15.6, 3.0)]
    for nota, ini, d in lamento:
        p.poner(14.0 + ini, M.bandoneon(hz(midi(nota)), d * 0.95, 0.45, vib=5.0), 1.0)
    p.poner(31.0, platillo(2.5, 0.12), 1.0)

    # --- La carga (33.5-58): tambores que crecen, cuerdas en ostinato y bronces
    prog = [("D", "m"), ("Bb", "M"), ("C", "M"), ("A", "7")]
    negra = 60 / 132
    t = 33.6
    k = 0
    while t < 57.6:
        raiz, ints = M.acorde(prog[(k // 8) % 4][0] + ("m" if prog[(k // 8) % 4][1] == "m" else
                                                     "7" if prog[(k // 8) % 4][1] == "7" else ""))
        intensidad = min(1.0, (t - 33.6) / 14)
        # ostinato de cuerdas (corcheas)
        for h in (0, 0.5):
            f = hz(raiz + (0 if (k + int(h * 2)) % 2 == 0 else 7))
            p.poner(t + h * negra, pad([f], negra * 0.48, 0.35 + 0.25 * intensidad, ataque=0.01, brillo=3000), 1.0)
        # tambores: bombo en 1 y 3, toms en contratiempo cuando crece
        if k % 2 == 0:
            p.poner(t, M.tambor(55, 1.0, 0.8, ruido=0.15, caida=3.5), 1.0)
        if intensidad > 0.4 and k % 4 == 3:
            p.poner(t + negra / 2, M.tambor(95, 0.5, 0.5 * intensidad, ruido=0.2, caida=8), 1.0)
        if intensidad > 0.7:
            p.poner(t + negra * 0.75, M.tambor(140, 0.3, 0.35, ruido=0.3, caida=12), 1.0)
        t += negra
        k += 1
    # colchón de cuerdas por acorde
    for i, inicio in enumerate(np.arange(33.6, 57.6, 8 * negra)):
        raiz, ints = M.acorde(["Dm", "Bb", "C", "A7"][i % 4])
        p.poner(inicio, pad([hz(raiz + j) for j in ints[:3]], 8 * negra + 0.3, 0.30, ataque=0.8, brillo=2400), 1.0)
    # el tema de bronces al grito y en el clímax
    tema = [("D4", 0, 0.9), ("F4", 0.9, 0.45), ("A4", 1.35, 0.45), ("D5", 1.8, 1.8), ("C5", 3.6, 0.9),
            ("Bb4", 4.5, 0.9), ("A4", 5.4, 2.4)]
    for base in (GRITO - 0.2, 49.0):
        for nota, ini, d in tema:
            p.poner(base + ini, bronce(hz(midi(nota)), d, 0.5), 1.0)
            p.poner(base + ini, bronce(hz(midi(nota) - 12), d, 0.35), 1.0)
    p.poner(55.5, platillo(2.5, 0.25), 1.0)
    p.poner(57.9, M.tambor(48, 3.0, 1.0, ruido=0.3, caida=1.5), 1.0)       # golpe final

    # --- La caída (58-69): todo se apaga; coro y campanas
    p.poner(58.0, pad([hz(midi("D2")), hz(midi("A2")), hz(midi("D3"))], 11.5, 0.3, ataque=2.0, brillo=900), 1.0)
    for nota, ini in (("F4", 58.5), ("A4", 61.0), ("D5", 63.5)):
        p.poner(ini, coro(hz(midi(nota)), 6.0, 0.12), 1.0)
    for nota, ini in (("D6", 60.0), ("A5", 62.0), ("F5", 64.0), ("D5", 66.0)):
        p.poner(ini, campana(hz(midi(nota)), 3.5, 0.10), 1.0)
    p.poner(67.0, platillo(2.5, 0.12), 1.0)

    # --- El último truco (69.5-90): el tema del fogón, ahora en mayor, y el final
    acordes = ["Dm", "Gm", "A7", "Dm", "Bb", "A7", "D", "D"]
    t = 69.8
    for c, simb in enumerate(acordes):
        raiz, ints = M.acorde(simb)
        p.poner(t, M.guitarra(hz(raiz - 12), 2.2, 0.6, 0.3), 1.0)
        for corch in (1, 2, 4, 5, 7):
            p.poner(t + corch * compas / 8, M.guitarra(hz(raiz + 12 + ints[(corch + c) % 3]), 0.9, 0.2, 0.7), 1.0)
        p.poner(t, pad([hz(raiz + j) for j in ints[:3]], compas + 0.4, 0.18, ataque=0.6, brillo=1600), 1.0)
        t += compas
    final = [("A4", 0, 1.2), ("D5", 1.2, 2.4), ("E5", 3.6, 1.2), ("F#5", 4.8, 3.6)]
    for nota, ini, d in final:
        p.poner(83.6 + ini, M.bandoneon(hz(midi(nota)), d * 0.95, 0.5, vib=5.0), 1.0)
        p.poner(83.6 + ini, bronce(hz(midi(nota) - 12), d, 0.25), 1.0)
    for j, m in enumerate([50, 57, 62, 66, 69, 74]):
        p.poner(84.8 + j * 0.05, M.guitarra(hz(m), 5.0, 0.45, 0.6), 1.0)
    p.poner(84.8, M.tambor(55, 3.0, 0.7, ruido=0.1, caida=1.8), 1.0)
    x = M.reverb(M.caja_guitarra(p.buf[:int(MSR * DURACION)]), 0.30)
    x = x / (np.abs(x).max() + 1e-9) * 0.85
    # la música "le hace lugar" a la voz del Vizcacha (-7 dB mientras habla, con rampas suaves)
    env = np.ones(len(x))
    for s in subtitulos:
        a, b = int(MSR * (s["t"] - 0.35)), int(MSR * (s["fin"] - 0.2))
        env[max(0, a):max(0, b)] = 0.2 if 33.0 <= s["t"] < 58.0 else 0.45   # en la carga la orquesta baja más
    k = int(MSR * 0.35)
    env = np.convolve(env, np.ones(k) / k, mode="same")
    x = x * env
    fin = int(MSR * 1.5)
    x[-fin:] *= np.linspace(1, 0, fin)
    guardar(x, MSR, os.path.join(SND, "musica.ogg"))
    print("música:", len(x) / MSR, "s")


def mezcla_de_prueba(subtitulos, ruta):
    """Todo junto (música + narración + grito), como suena en el juego: para escucharla sin abrir Minecraft."""
    import subprocess, tempfile
    with tempfile.TemporaryDirectory() as d:
        def leer(ogg, sr):
            w = os.path.join(d, "x.raw")
            subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", ogg, "-f", "f32le", "-ac", "1", "-ar", str(sr), w], check=True)
            return np.fromfile(w, dtype=np.float32)
        total = leer(os.path.join(SND, "musica.ogg"), MSR).astype(np.float64)
        for s in subtitulos:
            n = s["sonido"].split("_")[-1]
            v = leer(os.path.join(SND, f"narracion_{n}.ogg"), MSR)
            i = int(MSR * s["t"])
            total[i:i + len(v)] += v[:max(0, len(total) - i)]
        g = leer(os.path.join(SND, "carga.ogg"), MSR)
        i = int(MSR * GRITO)
        total[i:i + len(g)] += g * 0.9
        total = total / max(1.0, np.abs(total).max() / 0.98)
        guardar(total, MSR, ruta)


def main():
    if "--musica" in sys.argv:          # solo rehacer la música (las voces ya están)
        with open(os.path.join(ASSETS, "cinematica", "vizcacha.json"), encoding="utf-8") as f:
            subtitulos = json.load(f)["subtitulos"]
    else:
        subtitulos = voces()
    musica(subtitulos)
    if "--prueba" in sys.argv:
        mezcla_de_prueba(subtitulos, sys.argv[sys.argv.index("--prueba") + 1])
    datos = {"duracion": DURACION, "subtitulos": subtitulos, "grito": GRITO,
             "carteles": [{"t": a, "fin": b, "titulo": c, "sub": d} for a, b, c, d in CARTELES]}
    ruta = os.path.join(ASSETS, "cinematica", "vizcacha.json")
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, "w", encoding="utf-8") as f:
        json.dump(datos, f, indent=2, ensure_ascii=False)
    sp = os.path.join(ASSETS, "sounds.json")
    with open(sp, encoding="utf-8") as f:
        sonidos = json.load(f)
    sonidos["cinematica.musica"] = {"category": "music", "sounds": [{"name": "trucoya:cinematica/musica", "stream": True}]}
    sonidos["cinematica.carga"] = {"category": "voice", "sounds": ["trucoya:cinematica/carga"]}
    for n in range(1, len(NARRACION) + 1):
        sonidos[f"cinematica.narracion_{n}"] = {"category": "voice", "sounds": [f"trucoya:cinematica/narracion_{n}"]}
    with open(sp, "w", encoding="utf-8") as f:
        json.dump(sonidos, f, indent=2, ensure_ascii=False)
    print("listo:", ruta)


if __name__ == "__main__":
    main()
