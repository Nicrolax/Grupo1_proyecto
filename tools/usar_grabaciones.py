"""
Pone las voces REALES de los cuatro Fundadores en la cinemática final.

Grabaciones: una por persona, en cualquier formato que lea ffmpeg (audio de WhatsApp .opus/.ogg, .m4a del
celular, .mp3, .wav...), con el nombre de quien habla:
    tools/grabaciones/nicolas.*   agustin.*   matias.*   thiago.*
(No hace falta que estén las cuatro: las que falten siguen con la voz sintética.)

Si alguno cambió la frase, va en tools/grabaciones/textos.txt, una línea por persona, para el subtítulo:
    matias: Esto se festeja con un asado...

Qué hace con cada grabación: corta los silencios del principio y el final, saca el zumbido grave y un poco
de ruido de fondo, empareja el volumen con el resto de la película, le da el mismo "aire" de campo que a las
otras voces y, si se pasa un poquito del tiempo que tiene en la escena, la acelera sin cambiarle el tono
(hasta un 12 %). Si se pasa más que eso, avisa y no la usa.
Después rehace la música (baja el volumen justo donde habla cada uno) y los subtítulos.

Uso: python3 tools/usar_grabaciones.py [carpeta] [--prueba mezcla.ogg]
"""
import glob, json, os, subprocess, sys, tempfile
import numpy as np
from scipy.signal import butter, lfilter

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import generar_cinematica as C   # noqa: E402

RS = 44100
# (quién, número de línea en final.json, segundos que tiene en la escena)
# (cada uno tiene su turno de 9.5 s en la escena; Thiago, el último, un poco más hasta que habla el Vizcacha)
FUNDADORES = [("nicolas", 4, 9.3), ("agustin", 5, 9.3), ("matias", 6, 9.3), ("thiago", 7, 11.4)]
ACELERAR_MAX = 1.12
NIVEL = 0.13          # RMS de las demás voces de la película
HABLA = 0.135         # RMS de la parte hablada (unos -17 dB, como las líneas del Vizcacha)


def _decodificar(ruta, sr, filtro):
    with tempfile.TemporaryDirectory() as d:
        w = os.path.join(d, "x.raw")
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", ruta, "-af", filtro, "-f", "f32le", "-ac", "1",
                        "-ar", str(sr), w], check=True)
        return np.fromfile(w, dtype=np.float32).astype(np.float64)


_DENOISER = None


def _gtcrn():
    """Limpiador de voz neuronal (GTCRN, sherpa-onnx): saca ruido de fondo y eco de cuarto."""
    global _DENOISER
    if _DENOISER is None:
        import sherpa_onnx, urllib.request
        modelo = os.path.join(HERE, ".modelos", "gtcrn_simple.onnx")
        if not os.path.exists(modelo):
            os.makedirs(os.path.dirname(modelo), exist_ok=True)
            urllib.request.urlretrieve("https://github.com/k2-fsa/sherpa-onnx/releases/download/"
                                       "speech-enhancement-models/gtcrn_simple.onnx", modelo)
        cfg = sherpa_onnx.OfflineSpeechDenoiserConfig(model=sherpa_onnx.OfflineSpeechDenoiserModelConfig(
            gtcrn=sherpa_onnx.OfflineSpeechDenoiserGtcrnModelConfig(model=modelo)))
        _DENOISER = sherpa_onnx.OfflineSpeechDenoiser(cfg)
    return _DENOISER


def limpiar(x, x16):
    """
    Limpieza de banda completa: el limpiador neuronal trabaja a 16 kHz (hasta 8 kHz de agudos), así que los
    graves y medios salen de él y los agudos de la grabación original, abiertos solo donde hay voz (con la
    envolvente de la versión limpia).
    """
    from scipy.signal import resample_poly
    pico = np.abs(x16).max() + 1e-9
    y16 = np.array(_gtcrn()(x16.astype(np.float32) / pico * 0.7, 16000).samples, dtype=np.float64) * pico / 0.7
    baja = resample_poly(y16, 441, 160)[:len(x)]
    baja = np.concatenate([baja, np.zeros(len(x) - len(baja))])
    b, a = butter(4, 7200 / (RS / 2), btype="high")
    agudos = lfilter(b, a, x)
    k = int(RS * 0.03)
    env_l = np.sqrt(np.convolve(baja ** 2, np.ones(k) / k, mode="same"))
    env_o = np.sqrt(np.convolve(lfilter(*butter(4, 7200 / (RS / 2)), x) ** 2, np.ones(k) / k, mode="same"))
    ganancia = np.clip(env_l / (env_o + 1e-6), 0.0, 1.0) ** 2
    return baja + agudos * ganancia


def leer(ruta):
    """Lee la grabación y elige, con DNSMOS (nota de oyente), si conviene limpiarla o dejarla como vino."""
    import calidad_voz as Q
    x = _decodificar(ruta, RS, "adeclip,highpass=f=80")
    x16 = _decodificar(ruta, 16000, "adeclip,highpass=f=80")
    candidatos = {"original": x}
    try:
        candidatos["limpia"] = limpiar(x, x16)
    except Exception as e:   # sin el modelo (sin red) se usa la grabación como vino
        print("   (sin limpiador neuronal: " + str(e) + ")")
    notas = {k: Q.nota(v / (np.abs(v).max() + 1e-9) * 0.7, RS) for k, v in candidatos.items()}
    elegida = max(notas, key=lambda k: notas[k] + (0.05 if k == "original" else 0.0))
    print("   calidad (DNSMOS): " + ", ".join(f"{k} {v:.2f}" for k, v in notas.items()) + f" -> {elegida}")
    y = candidatos[elegida]
    # el mismo compresor suave que antes, para que no haya saltos de volumen
    with tempfile.TemporaryDirectory() as d:
        i, o = os.path.join(d, "i.raw"), os.path.join(d, "o.raw")
        y.astype(np.float32).tofile(i)
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "f32le", "-ar", str(RS), "-ac", "1", "-i", i,
                        "-af", "acompressor=threshold=-20dB:ratio=2.5:attack=8:release=120", "-f", "f32le", o], check=True)
        return np.fromfile(o, dtype=np.float32).astype(np.float64)


def recortar(x):
    """Saca los silencios de las puntas (deja 80 ms de aire antes y 150 ms después)."""
    k = int(RS * 0.02)
    env = np.sqrt(np.convolve(x ** 2, np.ones(k) / k, mode="same"))
    piso = np.percentile(env, 10)
    umbral = max(piso * 4, env.max() * 0.04)
    activo = np.where(env > umbral)[0]
    if len(activo) == 0:
        return x
    a = max(0, activo[0] - int(RS * 0.08))
    b = min(len(x), activo[-1] + int(RS * 0.15))
    return x[a:b]


def acelerar(x, factor):
    with tempfile.TemporaryDirectory() as d:
        i, o = os.path.join(d, "i.raw"), os.path.join(d, "o.raw")
        x.astype(np.float32).tofile(i)
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "f32le", "-ar", str(RS), "-ac", "1", "-i", i,
                        "-af", f"atempo={factor:.4f}", "-f", "f32le", o], check=True)
        return np.fromfile(o, dtype=np.float32).astype(np.float64)


def aire(x, mezcla=0.07):
    """El mismo reverb corto de campo abierto que llevan las voces sintéticas."""
    cola = int(RS * 0.4)
    y = np.concatenate([x, np.zeros(cola)])
    r = np.zeros_like(y)
    for d, g in ((0.031, 0.55), (0.037, 0.5), (0.043, 0.45), (0.049, 0.4)):
        n = int(RS * d)
        c = np.zeros_like(y)
        for t in range(n, len(y), n):          # peine de realimentación, por bloques
            m = min(n, len(y) - t)
            c[t:t + m] = y[t - n:t - n + m] + g * c[t - n:t - n + m]
        r += c
    b, a = butter(1, 3500 / (RS / 2))
    r = lfilter(b, a, r) / 4
    return recortar_cola(y + mezcla * r)


def recortar_cola(x):
    k = int(RS * 0.02)
    env = np.sqrt(np.convolve(x ** 2, np.ones(k) / k, mode="same"))
    fin = np.where(env > env.max() * 0.01)[0]
    return x[:fin[-1] + 1] if len(fin) else x


def procesar(ruta, ventana):
    x = leer(ruta)
    if len(x) < RS * 0.3:
        raise ValueError("la grabación está vacía o es muy corta")
    x = recortar(x)
    dur = len(x) / RS
    nota = ""
    if dur > ventana:
        f = dur / (ventana - 0.05)
        if f > ACELERAR_MAX:
            raise ValueError(f"dura {dur:.1f} s y en la escena entran {ventana:.1f} s: hay que decirla un poco más "
                             f"rápido o acortar la frase (hasta {ventana * ACELERAR_MAX:.1f} s la acomodo yo)")
        x = acelerar(x, f)
        nota = f" (acelerada {100 * (f - 1):.0f} %)"
    # volumen: RMS de la parte hablada, igual al de las otras voces
    k = int(RS * 0.05)
    env = np.sqrt(np.convolve(x ** 2, np.ones(k) / k, mode="same"))
    habla = env > env.max() * 0.15
    rms = np.sqrt(np.mean(x[habla] ** 2)) if habla.any() else np.sqrt(np.mean(x ** 2))
    x = x * (HABLA / (rms + 1e-9))             # la parte hablada al mismo nivel que la voz del Vizcacha
    x = aire(x)
    x = np.tanh(x * 1.1) / np.tanh(1.1) if np.abs(x).max() > 0.9 else x   # techo suave, sin recortes
    x = x * min(1.0, 0.95 / (np.abs(x).max() + 1e-9))
    f = int(RS * 0.01)
    x[:f] *= np.linspace(0, 1, f)
    x[-f:] *= np.linspace(1, 0, f)
    return x, nota


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    if "--prueba" in sys.argv:
        prueba_ruta = sys.argv[sys.argv.index("--prueba") + 1]
        args = [a for a in args if a != prueba_ruta]
    else:
        prueba_ruta = None
    carpeta = args[0] if args else os.path.join(HERE, "grabaciones")
    textos = {}
    tp = os.path.join(carpeta, "textos.txt")
    if os.path.exists(tp):
        with open(tp, encoding="utf-8-sig") as f:
            for linea in f:
                if ":" in linea:
                    quien, texto = linea.split(":", 1)
                    textos[quien.strip().lower().replace("á", "a").replace("í", "i")] = texto.strip()

    ruta_json = os.path.join(C.ASSETS, "cinematica", "final.json")
    with open(ruta_json, encoding="utf-8") as f:
        datos = json.load(f)
    subs = datos["subtitulos"]
    usadas = 0
    for quien, n, ventana in FUNDADORES:
        s = next(s for s in subs if s["sonido"] == f"cinematica.final_{n}")
        archivos = [a for a in glob.glob(os.path.join(carpeta, quien + ".*")) if not a.endswith(".txt")]
        if not archivos:
            print(f"{quien}: sin grabación, queda la voz sintética")
            continue
        try:
            x, nota = procesar(archivos[0], ventana)
        except (ValueError, subprocess.CalledProcessError) as e:
            print(f"{quien}: NO SE USÓ: {e}")
            continue
        C.guardar(x, RS, os.path.join(C.SND, f"final_{n}.ogg"))
        if quien in textos:           # el subtítulo cambia solo si la voz nueva entró
            s["texto"] = textos[quien]
        dur = len(x) / RS
        s["fin"] = round(s["t"] + max(dur, 2.0) + 0.4, 2)
        usadas += 1
        print(f"{quien}: {dur:.1f} s de {ventana:.1f}{nota} -> \"{s['texto']}\"")

    with open(ruta_json, "w", encoding="utf-8") as f:
        json.dump(datos, f, indent=2, ensure_ascii=False)
    import generar_final as F   # la música baja el volumen donde habla cada uno
    F.musica(subs)
    if prueba_ruta:
        F.prueba(subs, prueba_ruta)
    print(f"Listo: {usadas} de 4 voces reales en la cinemática final.")


if __name__ == "__main__":
    main()
