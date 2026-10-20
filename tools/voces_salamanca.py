"""
LA SALAMANCA (capítulo 7): voces y guiones de las escenas.

Genera:
  sounds/cinematica/esc_<escena>_<n>.ogg     las voces (el Vizcacha, el Mandinga y el fantasma de Ledesma)
  cinematica/escena_<escena>.json             subtítulos, carteles, duración y marcas de tiempo (las leen el
                                              servidor, que mueve a los personajes, y el cliente, que filma)
  sounds/voz/diablo/<canto>_<1..5>.ogg        los cantos del Mandinga en la mesa (¡Truco!, ¡Quiero!...)
y agrega todo a sounds.json.

Las voces salen de la misma síntesis que las demás (tools/generar_voces_neural.py):
  el Vizcacha   la voz de viejo de siempre
  el Mandinga   la voz gaucha, pasada por un vocoder (WORLD) una cuarta abajo, con la garganta más grande,
                una sombra una octava más grave y un eco de caverna
  Ledesma       la voz gaucha, apenas más grave, con un doble eco corto y mucha sala (un fantasma)
Cada frase se prueba a varias velocidades y se elige la que entra en su lugar de la escena; el reconocedor de
voz confirma que se entiende.

Uso: python3 tools/voces_salamanca.py [--solo-escenas | --solo-cantos]
"""
import json, os, sys
import numpy as np
import pyworld as pw
from scipy.signal import butter, lfilter

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import generar_voces_neural as V   # noqa: E402
import calidad_voz                  # noqa: E402

SR = V.SR
ASSETS = os.path.join(HERE, "..", "src", "main", "resources", "assets", "trucoya")
SND = os.path.join(ASSETS, "sounds", "cinematica")
SOUNDS_JSON = os.path.join(ASSETS, "sounds.json")


# ---------------------------------------------------------------- las voces

def _formantes(sp, k):
    n = sp.shape[1]
    idx = np.clip(np.arange(n) * k, 0, n - 1)
    out = np.empty_like(sp)
    for i in range(sp.shape[0]):
        out[i] = np.interp(idx, np.arange(n), sp[i])
    return out


def diablo(x, nivel=0.13):
    """El Mandinga: una cuarta más grave, garganta grande, sombra de octava y caverna."""
    x = np.asarray(x, dtype=np.float64)
    f0, t = pw.harvest(x, SR, f0_floor=60, f0_ceil=400, frame_period=5.0)
    sp = pw.cheaptrick(x, f0, t, SR)
    ap = pw.d4c(x, f0, t, SR)
    y = pw.synthesize(f0 * 0.70, _formantes(sp, 0.88), ap, SR, 5.0)
    z = pw.synthesize(f0 * 0.35, _formantes(sp, 0.81), ap, SR, 5.0)
    n = min(len(y), len(z))
    y = y[:n] + 0.22 * z[:n]
    b, a = butter(2, 70 / (SR / 2), btype="high")
    y = lfilter(b, a, y)
    y = np.tanh(y / (np.abs(y).max() + 1e-9) * 1.6) / np.tanh(1.6)
    y = V.sala(y.astype(np.float32), 0.18)
    y = V.recortar(y)
    return (y * (nivel / (np.sqrt(np.mean(y ** 2)) + 1e-9))).astype(np.float32)


def diablo_frase(partes, pausas, preguntas, nivel=0.13, aspereza=1.2):
    """
    Una frase del Mandinga dicha con intención: varias partes con silencios entre medio, y las que son pregunta
    con la entonación de pregunta (la voz baja un poco y sube al final, como "¿todavía tenés mi baraja?").
    partes: audios crudos (24 kHz); pausas: segundos de silencio después de cada parte; preguntas: True/False.
    """
    trozos = []
    for x, pausa, pregunta in zip(partes, pausas, preguntas):
        x = np.asarray(V.recortar(x), dtype=np.float64)
        f0, t = pw.harvest(x, SR, f0_floor=60, f0_ceil=400, frame_period=5.0)
        sp = pw.cheaptrick(x, f0, t, SR)
        ap = pw.d4c(x, f0, t, SR)
        sonoro = np.where(f0 > 0)[0]
        if pregunta and len(sonoro) > 10:
            # La síntesis dice las preguntas como afirmación (la voz cae al final). Se le dibuja otra melodía:
            # alta y pareja, un leve descanso, y la subida en la última palabra ("¿...mi baraja?").
            # De la original se conserva solo el temblor fino (que no suene a robot).
            base = float(np.median(f0[sonoro]))
            a, fin = sonoro[0], sonoro[-1]
            u = np.clip((np.arange(len(f0)) - a) / max(1, fin - a), 0.0, 1.0)
            meta = np.where(u < 0.68, base * (1.04 - 0.10 * u / 0.68),
                            base * (0.94 + 0.56 * ((np.clip(u, 0.68, 1.0) - 0.68) / 0.32) ** 1.3))
            liso = np.convolve(np.where(f0 > 0, f0, base), np.ones(15) / 15, mode="same")
            fino = np.where(f0 > 0, (f0 / np.maximum(liso, 1.0)) ** 0.5, 1.0)
            f0 = np.where(f0 > 0, meta * fino, 0.0)
        y = pw.synthesize(f0 * 0.72, _formantes(sp, 0.89), ap, SR, 5.0)
        z = pw.synthesize(f0 * 0.36, _formantes(sp, 0.82), ap, SR, 5.0)
        k = min(len(y), len(z))
        y = y[:k] + 0.14 * z[:k]
        trozos.append(y / (np.sqrt(np.mean(y ** 2)) + 1e-9))
        trozos.append(np.zeros(int(SR * pausa)))
    y = np.concatenate(trozos)
    b, a = butter(2, 70 / (SR / 2), btype="high")
    y = lfilter(b, a, y)
    y = np.tanh(y / (np.abs(y).max() + 1e-9) * aspereza) / np.tanh(aspereza)
    y = V.sala(y.astype(np.float32), 0.20)
    y = V.recortar(y)
    return (y * (nivel / (np.sqrt(np.mean(y ** 2)) + 1e-9))).astype(np.float32)


def fantasma(x, nivel=0.12):
    """El Negro Ledesma: apenas más grave, con doble eco corto y mucha sala."""
    y = V.varispeed(x, 0.97)
    b, a = butter(2, 160 / (SR / 2), btype="high")
    y = lfilter(b, a, y)
    d1, d2 = int(SR * 0.017), int(SR * 0.029)
    yy = np.concatenate([y, np.zeros(d2)])
    yy[d1:d1 + len(y)] += 0.45 * y
    yy[d2:d2 + len(y)] += 0.3 * y
    yy = V.sala(yy.astype(np.float32), 0.30)
    yy = V.recortar(yy)
    return (yy * (nivel / (np.sqrt(np.mean(yy ** 2)) + 1e-9))).astype(np.float32)


def viejo(x, nivel=0.13):
    x = V.recortar(x)
    x = V.eq(x, 80, 0, "hp")
    x = V.sala(x, 0.12)
    x = x * (nivel / (np.sqrt(np.mean(x ** 2)) + 1e-9))
    return (x * min(1.0, 0.95 / (np.abs(x).max() + 1e-9))).astype(np.float32)


VOCES = {"vizcacha": ("viejo", 0.93, viejo), "mandinga": ("gaucho", 0.92, diablo), "ledesma": ("gaucho", 0.95, fantasma)}


# ---------------------------------------------------------------- las escenas (tiempos fijos: ver EscenaViva.java)

ESCENAS = {
    "visita": {
        "duracion": 71.6, "marcas": {"bajarse": 1.5}, "carteles": [],
        "lineas": [
            (1.6, 6.3, "vizcacha", "Buenas tardes, gurí. Tranquilo, que hoy no vengo a jugarte."),
            (6.6, 11.7, "vizcacha", "Anoche, en el fogón... vos también lo viste, ¿no? Entre el humo."),
            (12.0, 16.9, "vizcacha", "Era el Negro Ledesma. Setenta años sin verle la cara."),
            (17.2, 22.1, "vizcacha", "Te debo la verdad. La Baraja de Oro nunca fue del Negro."),
            (22.4, 27.7, "vizcacha", "Se la ganó al Mandinga, en una salamanca. Y el diablo no regala nada."),
            (28.0, 33.3, "vizcacha", "El trato era claro: cuando el Negro muriera, su alma iba a ser de él."),
            (33.6, 38.7, "vizcacha", "Por eso, cuando cayó en el Paso, me la dio a mí. Para escondérsela."),
            (39.0, 45.2, "vizcacha", "Y el Mandinga se lo llevó. Lo tiene sentado a su mesa, de compañero, para siempre."),
            (45.6, 49.5, "vizcacha", "No estuve setenta años esperando un rival, gurí."),
            (49.8, 55.2, "vizcacha", "Estuve setenta años esperando a alguien que me acompañara a buscarlo."),
            (55.6, 58.6, "vizcacha", "Y ese sos vos."),
            (59.0, 64.6, "vizcacha", "Tomá este farol. Con su luz se ven las luces malas: ellas conocen el camino."),
            (65.0, 70.6, "vizcacha", "Te traje un pingo. Ensillá, que esta noche vamos al Paso de las Ánimas."),
        ],
    },
    "viaje": {
        "duracion": 7.0, "marcas": {"salto": 3.0}, "lineas": [],
        "carteles": [(0.9, 6.2, "ESA NOCHE", "camino al Paso de las Ánimas")],
    },
    "llegada": {
        "duracion": 42.4, "marcas": {"rayo1": 3.0, "rayo2": 4.4, "salamanca": 5.0, "entra": 6.5},
        "carteles": [(0.6, 4.8, "EL PASO DE LAS ÁNIMAS", "medianoche")],
        "lineas": [
            (7.5, 12.8, "mandinga", "Mirá quién se acordó del camino... Setenta años tardaste, Vizcacha."),
            (13.2, 16.3, "vizcacha", "Vengo por el Negro, Mandinga."),
            (16.6, 21.6, "mandinga", "¿Y con qué pensás pagar? Lo único que me interesa es esa baraja."),
            (22.0, 26.6, "vizcacha", "Una partida, a treinta. Si ganamos, el Negro queda libre."),
            (27.0, 31.8, "mandinga", "Y si pierden, el Negro se queda conmigo... y la próxima me traen la baraja."),
            (32.2, 37.0, "ledesma", "Vizcacha... hermano. No tendrías que haber venido."),
            (37.4, 41.6, "mandinga", "Sentate, gurí. La noche es larga, y el diablo no tiene apuro."),
        ],
    },
    "amanecer": {
        "duracion": 46.8,
        "marcas": {"mandinga_a": 3.0, "mandinga_b": 6.0, "ledesma_camina": 5.5, "ledesma_a": 31.0,
                   "ledesma_b": 35.0, "ruina": 33.0, "facon": 37.0},
        "carteles": [],
        "lineas": [
            (0.9, 2.95, "mandinga", "¡No! ¡No puede ser!"),
            (7.0, 10.1, "ledesma", "Setenta años, hermano."),
            (10.4, 13.5, "vizcacha", "Perdoname, Negro. Tardé mucho."),
            (13.8, 17.3, "ledesma", "Llegaste. Eso es lo único que importa."),
            (17.6, 23.2, "ledesma", "Tomá. Este facón es tuyo: te lo gané con una flor que no era mía."),
            (23.6, 27.2, "vizcacha", "Siempre supe que esa flor era demasiado linda."),
            (27.6, 30.8, "ledesma", "Gracias, gurí. Cuidámelo a este viejo."),
            (36.8, 41.6, "vizcacha", "Tomá, gurí. El facón del Negro. Te lo ganaste."),
            (41.9, 45.9, "vizcacha", "Ahora sí... puedo descansar tranquilo."),
        ],
    },
    "derrota": {
        "duracion": 15.0, "marcas": {"negro": 9.0, "ruina": 10.5}, "carteles": [],
        "lineas": [
            (0.8, 4.3, "mandinga", "Je, je... ¿Eso es todo lo que traías, Vizcacha?"),
            (4.6, 8.8, "mandinga", "El Negro se queda conmigo. Volvé cuando aprendas a jugar."),
            (9.4, 14.4, "vizcacha", "Otra noche será, gurí. Yo te espero acá, en el Paso."),
        ],
    },
}


def decir(sint, quien, texto, hueco, semilla, asr):
    """La frase de 'quien', a la velocidad más natural que entre en 'hueco' segundos."""
    voz, vel0, proc = VOCES[quien]
    mejor = None
    for vel in (1.0, 1.06, 1.12, 1.18, 1.26):
        x = sint(voz, texto, vel0 * vel, semilla=semilla)
        y = proc(x)
        entra = len(y) / SR <= hueco
        nota = calidad_voz.nota(y, SR)
        if mejor is None or (entra and (not mejor[2] or nota > mejor[1])):
            mejor = (y, nota, entra, vel)
        if entra and len(y) / SR > hueco - 0.9:
            break
    y, nota, entra, vel = mejor
    s = asr.create_stream()
    s.accept_waveform(SR, y)
    asr.decode_stream(s)
    return y, nota, entra, s.result.text


def escenas(sint, asr, sonidos):
    for nombre, e in ESCENAS.items():
        subs = []
        for n, (t0, t1, quien, texto) in enumerate(e["lineas"], start=1):
            y, nota, entra, oido = decir(sint, quien, texto, t1 - t0, n, asr)
            V.guardar_ogg(y, os.path.join(SND, f"esc_{nombre}_{n}.ogg"))
            dur = len(y) / SR
            print(f"{nombre} {n} ({quien}): {dur:.1f}/{t1 - t0:.1f} s {'' if entra else 'NO ENTRA '}nota {nota:.2f} -> {oido}",
                  flush=True)
            subs.append({"t": t0, "fin": round(t0 + max(dur, 1.8) + 0.35, 2), "texto": texto,
                         "sonido": f"escena.{nombre}_{n}", "quien": quien})
            sonidos[f"escena.{nombre}_{n}"] = {"category": "voice", "sounds": [f"trucoya:cinematica/esc_{nombre}_{n}"]}
        datos = {"duracion": e["duracion"], "marcas": e["marcas"], "subtitulos": subs,
                 "carteles": [{"t": a, "fin": b, "titulo": c, "sub": d, "grande": False} for a, b, c, d in e["carteles"]]}
        with open(os.path.join(ASSETS, "cinematica", f"escena_{nombre}.json"), "w", encoding="utf-8") as f:
            json.dump(datos, f, indent=2, ensure_ascii=False)


# ---------------------------------------------------------------- los cantos del Mandinga en la mesa

def texto_diablo(texto):
    for a, b in ((", bo", ", gurí"), (", hermano", ", gurí"), (", che", ", gurí"), ("Ta, ", "Je, "),
                 ("Ta... ", "Je... "), ("A ver qué tenés", "A ver qué traés")):
        texto = texto.replace(a, b)
    return texto


def cantos(sint, asr, sonidos):
    out = os.path.join(ASSETS, "sounds", "voz", "diablo")
    os.makedirs(out, exist_ok=True)
    bien = 0
    total = 0
    for clave, (estilos, textos) in V.GUION.items():
        for i, (estilo, texto) in enumerate(zip(estilos, textos), start=1):
            texto = texto_diablo(texto)
            mejor = None
            for ajuste in (1.0, 0.93, 1.07):
                x = sint("gaucho", texto, V.VELOCIDAD[estilo] * ajuste * 0.95, semilla=i)
                y = diablo(x, 0.12)
                s = asr.create_stream()
                s.accept_waveform(SR, y)
                asr.decode_stream(s)
                ok = V.CLAVES[clave] in V.normal(s.result.text)
                cand = (ok, calidad_voz.nota(y, SR), y, s.result.text)
                if mejor is None or (cand[0], cand[1]) > (mejor[0], mejor[1]):
                    mejor = cand
            ok, nota, y, oido = mejor
            V.guardar_ogg(y, os.path.join(out, f"{clave}_{i}.ogg"))
            total += 1
            bien += ok
            print(f"diablo {clave:20s} {i} {'OK ' if ok else '?? '} {nota:.2f} {texto!r} -> {oido!r}", flush=True)
        sonidos[f"voz.diablo.{clave}"] = {"category": "player", "subtitle": f"subtitles.trucoya.voice.{clave}",
                                          "sounds": [f"trucoya:voz/diablo/{clave}_{i}" for i in range(1, 6)]}
    print(f"cantos del Mandinga: {bien}/{total} entendidos por el reconocedor")


def main():
    sint = V.crear_sintetizador()
    asr = V.reconocedor()
    with open(SOUNDS_JSON, encoding="utf-8") as f:
        sonidos = json.load(f)
    if "--solo-cantos" not in sys.argv:
        escenas(sint, asr, sonidos)
    if "--solo-escenas" not in sys.argv:
        cantos(sint, asr, sonidos)
    with open(SOUNDS_JSON, "w", encoding="utf-8") as f:
        json.dump(sonidos, f, indent=2, ensure_ascii=False)
    print("listo")


if __name__ == "__main__":
    main()
