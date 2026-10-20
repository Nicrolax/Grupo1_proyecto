"""
Voces de los cantos con síntesis neuronal (reemplaza a las voces robóticas de espeak).

Cinco voces, cada una con sus 13 cantos y 5 tomas por canto (325 audios):
  hombre   voz masculina joven, clara                     (Kokoro em_alex, un poco más plena)
  gaucho   voz masculina más gruesa                      (Kokoro: mezcla em_alex + em_santa)
  viejo    el Viejo Vizcacha: grave, lenta, con temblor    (Kokoro em_santa "envejecida" en su espacio de estilos)
  mujer    voz femenina rioplatense                       (Piper es_AR "daniela")
  paisana  otra voz femenina rioplatense, más grave        (Piper es_AR "daniela", otra afinación)
Acento rioplatense: sheísmo ("sho", "cashate": la ll y la y suenan como en Montevideo y Buenos Aires),
voseo y giros uruguayos ("bo", "ta", "botija", "gurí", "m'hijo").

Para que suenen humanas y no robóticas:
- nada de vocoders (el WORLD de antes "zumbaba"): la voz grave del Vizcacha sale del propio modelo,
  moviendo su vector de estilo en la dirección "más masculino / más grave";
- a la voz vieja se le agrega el temblor natural de la edad (4-6 Hz, irregular, muy leve);
- por cada toma se generan varias versiones y un juez automático (DNSMOS, entrenado con notas de
  oyentes humanos) elige la más natural entre las que el reconocedor de voz entiende bien.

Cada toma tiene su forma de decirlo: 1 firme, 2 gritado, 3 canchero, 4 grave y lento, 5 rápido
(en los de retirada: 1 normal, 2 resignado, 3 fastidiado, 4 canchero, 5 rápido).
Después: recorte de silencios, volumen parejo, un poco de "sala" de boliche, y .ogg mono.
Al final se verifica cada audio con un reconocedor de voz (Whisper) para descartar tomas que no se
entiendan; si una falla, se vuelve a generar con otra velocidad.

Requisitos: pip install sherpa-onnx pyworld scipy numpy ; ffmpeg.
Los modelos se descargan solos (GitHub, proyecto k2-fsa/sherpa-onnx) en tools/.modelos.
Licencias: Kokoro-82M Apache-2.0; daniela (Piper) entrenada con OpenSLR 61, CC BY-SA 4.0.
Uso: python3 tools/generar_voces_neural.py
"""
import json, os, shutil, subprocess, tarfile, tempfile, unicodedata, urllib.request, wave
import numpy as np
from scipy.signal import butter, lfilter

HERE = os.path.dirname(os.path.abspath(__file__))
MOD = os.path.join(HERE, ".modelos")
OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "trucoya", "sounds", "voz")
SOUNDS_JSON = os.path.join(HERE, "..", "src", "main", "resources", "assets", "trucoya", "sounds.json")
URL = "https://github.com/k2-fsa/sherpa-onnx/releases/download/{}/{}.tar.bz2"
SR = 24000

import sherpa_onnx  # noqa: E402
import pyworld as pw  # noqa: E402


def bajar(release, nombre):
    destino = os.path.join(MOD, nombre)
    if os.path.isdir(destino):
        return destino
    os.makedirs(MOD, exist_ok=True)
    tmp = os.path.join(MOD, nombre + ".tar.bz2")
    print("descargando", nombre, "...")
    urllib.request.urlretrieve(URL.format(release, nombre), tmp)
    with tarfile.open(tmp) as t:
        t.extractall(MOD)
    os.remove(tmp)
    return destino


# ---------------------------------------------------------------- motores

def motor_kokoro(voces_bin):
    d = bajar("tts-models", "kokoro-multi-lang-v1_0")
    cfg = sherpa_onnx.OfflineTtsConfig(model=sherpa_onnx.OfflineTtsModelConfig(
        kokoro=sherpa_onnx.OfflineTtsKokoroModelConfig(
            model=os.path.join(d, "model.onnx"), voices=voces_bin, tokens=os.path.join(d, "tokens.txt"),
            data_dir=os.path.join(d, "espeak-ng-data"),
            lexicon=os.path.join(d, "lexicon-us-en.txt") + "," + os.path.join(d, "lexicon-zh.txt"),
            dict_dir=os.path.join(d, "dict"), lang="es-419"),
        num_threads=2, provider="cpu"), max_num_sentences=1)
    return sherpa_onnx.OfflineTts(cfg)


# lugares del voices.bin propio
GAUCHO, VIEJO, HOMBRE = 0, 1, 2


def voces_mezcladas():
    """
    voices.bin con voces nuevas, armadas en el espacio de estilos de Kokoro (sin tocar el audio):
      0 gaucho  55 % em_alex + 45 % em_santa, empujada hacia lo grave
      1 viejo   em_santa + 0.6 (em_santa - ef_dora): la dirección "voz de mujer -> voz de hombre"
                baja el tono de forma natural (~105 Hz) sin el zumbido de un vocoder
      2 hombre  em_alex con un poco más de cuerpo
    """
    d = bajar("tts-models", "kokoro-multi-lang-v1_0")
    v = np.fromfile(os.path.join(d, "voices.bin"), dtype=np.float32).reshape(54, 510, 256)
    v = v.copy()
    alex, santa, dora = v[29].copy(), v[53].copy(), v[28].copy()
    v[GAUCHO] = 0.55 * alex + 0.45 * santa + 0.25 * (santa - dora)
    v[VIEJO] = santa + 0.6 * (santa - dora)
    v[HOMBRE] = alex + 0.15 * (alex - dora)
    p = os.path.join(MOD, "voices_trucoya.bin")
    v.tofile(p)
    return p


def motor_piper(nombre):
    d = bajar("tts-models", "vits-piper-" + nombre)
    onnx = [f for f in os.listdir(d) if f.endswith(".onnx")][0]
    cfg = sherpa_onnx.OfflineTtsConfig(model=sherpa_onnx.OfflineTtsModelConfig(
        vits=sherpa_onnx.OfflineTtsVitsModelConfig(
            model=os.path.join(d, onnx), lexicon="", tokens=os.path.join(d, "tokens.txt"),
            data_dir=os.path.join(d, "espeak-ng-data"), noise_scale=0.667, noise_scale_w=0.8, length_scale=1.0),
        num_threads=2, provider="cpu"), max_num_sentences=1)
    return sherpa_onnx.OfflineTts(cfg)


def reconocedor():
    d = bajar("asr-models", "sherpa-onnx-whisper-small")
    return sherpa_onnx.OfflineRecognizer.from_whisper(
        encoder=os.path.join(d, "small-encoder.int8.onnx"), decoder=os.path.join(d, "small-decoder.int8.onnx"),
        tokens=os.path.join(d, "small-tokens.txt"), language="es", task="transcribe", num_threads=2)


# ---------------------------------------------------------------- procesamiento

def a_sr(x, sr):
    if sr == SR:
        return x.astype(np.float32)
    from scipy.signal import resample_poly
    from math import gcd
    g = gcd(sr, SR)
    return resample_poly(x, SR // g, sr // g).astype(np.float32)


def recortar(x, umbral=0.03):
    env = np.convolve(np.abs(x), np.ones(240) / 240, mode="same")
    activo = np.where(env > umbral * env.max())[0]
    if len(activo) == 0:
        return x
    a, b = max(0, activo[0] - 240), min(len(x), activo[-1] + 1200)
    y = x[a:b].copy()
    y[:120] *= np.linspace(0, 1, 120)
    y[-960:] *= np.linspace(1, 0, 960)
    return y


def primer_segmento(x, sr, min_sil=0.11):
    """Hasta el primer silencio largo: para cortar la palabra 'de relleno' de las frases de una sola palabra."""
    env = np.convolve(np.abs(x), np.ones(int(sr * 0.01)) / int(sr * 0.01), mode="same")
    activo = env > 0.03 * env.max()
    i0 = int(np.argmax(activo))
    n, quietos = int(min_sil * sr), 0
    for i in range(i0, len(x)):
        if activo[i]:
            quietos = 0
        else:
            quietos += 1
            if quietos >= n:
                return x[max(0, i0 - int(0.02 * sr)):i - n + int(0.05 * sr)]
    return x[i0:]


def world(x, f0=1.0, formante=1.0, ronquera=0.0):
    f0s, sp, ap = pw.wav2world(x.astype(np.float64), SR)
    f0s = f0s * f0
    if formante != 1.0:
        K = sp.shape[1]
        idx = np.clip(np.arange(K) / formante, 0, K - 1)
        sp = np.array([np.interp(idx, np.arange(K), fr) for fr in sp])
        ap = np.array([np.interp(idx, np.arange(K), fr) for fr in ap])
    if ronquera:
        ap = np.clip(ap + ronquera * (1 - ap) * 0.5, 0, 1)
    return pw.synthesize(f0s, sp, ap, SR).astype(np.float32)


import re  # noqa: E402

_VOCAL = "aeiouáéíóúü"


def rioplatense(texto):
    """Sheísmo: la 'll' y la 'y' delante de vocal se dicen 'sh' (sho, cashate, sha). La 'y' sola ("y") y la
    del final ("voy", "hoy", "muy") quedan como están."""
    def cambiar(m):
        letras, sig = m.group(1), m.group(2)
        mayus = letras[0].isupper()
        return ("Sh" if mayus else "sh") + sig
    texto = re.sub(r"(ll|LL|Ll)([" + _VOCAL + "])", cambiar, texto)
    texto = re.sub(r"(?<![A-Za-zÁÉÍÓÚáéíóúñ])([Yy])([" + _VOCAL + "])", cambiar, texto)
    texto = re.sub(r"(?<=[" + _VOCAL + "])(y)([" + _VOCAL + "])", cambiar, texto)
    return texto


def temblor(x, frec=5.0, tono=0.009, amp=0.06, semilla=0):
    """Temblor de voz de persona mayor: pequeñas oscilaciones irregulares de tono y volumen (4-6 Hz)."""
    rng = np.random.default_rng(semilla)
    n = len(x)
    t = np.arange(n) / SR
    deriva = np.interp(t, np.linspace(0, t[-1] + 1e-9, 12), 1 + 0.15 * rng.standard_normal(12))
    fase = 2 * np.pi * np.cumsum(frec * deriva) / SR
    pos = np.arange(n) + (tono * SR / (2 * np.pi * frec)) * (1 - np.cos(fase))
    y = np.interp(np.clip(pos, 0, n - 1), np.arange(n), x)
    return (y * (1 + amp * np.sin(fase + 0.8))).astype(np.float32)


def aire(x, cantidad=0.03, semilla=1):
    """Un poco de soplo (aspiración) que sigue a la voz: la hace menos 'limpia de máquina'."""
    rng = np.random.default_rng(semilla)
    env = np.convolve(np.abs(x), np.ones(480) / 480, mode="same")
    b, a = butter(2, [1800 / (SR / 2), 7000 / (SR / 2)], btype="band")
    ruido = lfilter(b, a, rng.standard_normal(len(x)))
    return (x + ruido * env * cantidad * 6).astype(np.float32)


def varispeed(x, factor):
    """Cambia tono y tempo juntos (como acelerar una cinta): para pequeñas diferencias suena natural."""
    n = int(len(x) / factor)
    return np.interp(np.arange(n) * factor, np.arange(len(x)), x).astype(np.float32)


def eq(x, f, ganancia, tipo="peak"):
    if tipo == "hp":
        b, a = butter(2, f / (SR / 2), btype="high")
        return lfilter(b, a, x)
    b, a = butter(2, [f * 0.7 / (SR / 2), min(0.99, f * 1.4 / (SR / 2))], btype="band")
    return x + (10 ** (ganancia / 20) - 1) * lfilter(b, a, x)


def comprimir(x, umbral=0.25, razon=4.0):
    env = np.convolve(np.abs(x), np.ones(240) / 240, mode="same") + 1e-6
    g = np.where(env > umbral, (umbral + (env - umbral) / razon) / env, 1.0)
    return x * g


def sala(x, mezcla=0.08):
    salida = np.zeros(len(x) + int(SR * 0.4))
    y = np.concatenate([x, np.zeros(int(SR * 0.4))])
    for d, g in ((int(SR * 0.031), 0.55), (int(SR * 0.037), 0.5), (int(SR * 0.043), 0.45), (int(SR * 0.049), 0.4)):
        a = np.zeros(d + 1)
        a[0], a[d] = 1, -g
        salida += lfilter([1], a, y)
    return (y * (1 - mezcla) + salida * mezcla * 0.3).astype(np.float32)


def terminar(x, estilo):
    x = eq(x, 80, 0, "hp")
    if estilo == "gritado":
        x = comprimir(x / (np.abs(x).max() + 1e-9), 0.25, 3.0)
        x = np.tanh(x * 1.3) / np.tanh(1.3)              # apenas empujada (sin la aspereza de antes)
        x = eq(x, 2500, 3.0)
    elif estilo in ("grave",):
        x = eq(x, 180, 2.5)
    x = recortar(x)
    x = sala(x, 0.10)
    rms = np.sqrt(np.mean(x ** 2)) + 1e-9
    x = x * (0.12 / rms)                                 # volumen parejo entre tomas
    pico = np.abs(x).max()
    if pico > 0.95:
        x = x * (0.95 / pico)
    return x.astype(np.float32)


def guardar_ogg(x, ruta):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with tempfile.TemporaryDirectory() as d:
        w = os.path.join(d, "t.wav")
        with wave.open(w, "wb") as f:
            f.setnchannels(1)
            f.setsampwidth(2)
            f.setframerate(SR)
            f.writeframes((np.clip(x, -1, 1) * 32767).astype(np.int16).tobytes())
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", w, "-c:a", "libvorbis", "-q:a", "4", ruta],
                       check=True)


# ---------------------------------------------------------------- guion

SUBE = ["firme", "gritado", "canchero", "grave", "rapido"]
BAJA = ["normal", "resignado", "fastidiado", "canchero", "rapido"]

GUION = {
    "truco": (SUBE, ["¡Truco!", "¡Truco!", "¡Truco, bo!", "Truco. A ver qué tenés.", "¡Truco, nomás!"]),
    "retruco": (SUBE, ["¡Quiero retruco!", "¡Quiero retruco!", "¡Quiero retruco, hermano!", "Quiero retruco.",
                       "¡Quiero retruco, vamos!"]),
    "vale_cuatro": (SUBE, ["¡Quiero vale cuatro!", "¡Quiero vale cuatro!", "¡Quiero vale cuatro, che!",
                           "Quiero vale cuatro.", "¡Vale cuatro, vamos!"]),
    "envido": (SUBE, ["¡Envido!", "¡Envido!", "Envido, bo.", "Envido.", "¡Te canto envido!"]),
    "real_envido": (SUBE, ["¡Real envido!", "¡Real envido!", "¡Real envido, che!", "Real envido.",
                           "¡Real envido, nomás!"]),
    "falta_envido": (SUBE, ["¡Falta envido!", "¡Falta envido!", "¡Falta envido, hermano!", "Falta envido.",
                            "¡Falta envido, vamos!"]),
    "flor": (SUBE, ["¡Flor!", "¡Flor!", "¡Tengo flor, che!", "Tengo flor.", "¡Flor, señores!"]),
    "contraflor": (SUBE, ["¡Contraflor!", "¡Contraflor!", "¡Contraflor, che!", "Contraflor.",
                          "¡Contraflor, nomás!"]),
    "contraflor_al_resto": (SUBE, ["¡Contraflor al resto!", "¡Contraflor al resto!",
                                   "¡Contraflor al resto, hermano!", "Contraflor al resto.",
                                   "¡Contraflor al resto, vamos!"]),
    "quiero": (SUBE, ["¡Quiero!", "¡Quiero!", "¡Quiero, che!", "Quiero.", "¡Ta, quiero!"]),
    "con_flor_me_achico": (BAJA, ["Con flor me achico.", "Bueno... con flor me achico.", "Uh... con flor me achico.",
                                  "Con flor me achico, nomás.", "Con flor me achico."]),
    "no_quiero": (BAJA, ["No quiero.", "Uh... no quiero.", "¡No quiero, che!", "No quiero nada.", "Ta... no quiero."]),
    "me_voy_al_mazo": (BAJA, ["Me voy al mazo.", "Uf... me voy al mazo.", "¡Me voy al mazo, che!",
                              "Me voy al mazo, nomás.", "Me voy al mazo."]),
}

# velocidad por estilo
VELOCIDAD = {"firme": 1.0, "gritado": 1.08, "canchero": 0.92, "grave": 0.82, "rapido": 1.25,
             "normal": 1.0, "resignado": 0.85, "fastidiado": 1.05}


# Ajustes puntuales: frases que una voz no decía bien (verificado con el reconocedor)
AJUSTES = {("mujer", "flor", 1): "¡Tengo flor!", ("mujer", "flor", 2): "¡Tengo flor!",
           ("viejo", "flor", 2): "¡Tengo flor!", ("mujer", "me_voy_al_mazo", 2): "Bueno... me voy al mazo.",
           ("viejo", "truco", 3): "¡Truco, m'hijo!",
           # la voz rioplatense de mujer se entiende mejor si el "real" no va al principio
           ("mujer", "real_envido", 1): "¡Te canto real envido!", ("mujer", "real_envido", 2): "¡Te canto real envido!",
           ("paisana", "real_envido", 1): "¡Te canto real envido!", ("paisana", "real_envido", 2): "¡Te canto real envido!",
           ("paisana", "flor", 1): "¡Tengo flor!", ("paisana", "flor", 2): "¡Tengo flor!", ("mujer", "flor", 5): "¡Tengo flor!"}


def texto_para(voz, texto):
    if voz == "viejo":
        texto = texto.replace("hermano", "m'hijo").replace(", che", ", m'hijo")
    if voz in ("mujer", "paisana"):
        texto = texto.replace(", hermano", ", che")
    return texto


def normal(s):
    s = unicodedata.normalize("NFD", s.lower())
    return "".join(c for c in s if c.isalpha() or c == " ")


CLAVES = {"truco": "truc", "retruco": "retruc", "vale_cuatro": "vale", "envido": "e", "real_envido": "real",
          "falta_envido": "falta", "flor": "flo", "contraflor": "contra", "contraflor_al_resto": "resto",
          "quiero": "quier", "con_flor_me_achico": "chico", "no_quiero": "quier", "me_voy_al_mazo": "voy"}


def crear_sintetizador():
    """Devuelve sintetizar(voz, texto, velocidad) -> audio crudo a 24 kHz (sin terminar)."""
    kok_mix = motor_kokoro(voces_mezcladas())
    kok = motor_kokoro(os.path.join(MOD, "kokoro-multi-lang-v1_0", "voices.bin"))
    dan = motor_piper("es_AR-daniela-high")

    def sintetizar(voz, texto, vel, semilla=0, alterno=False):
        """alterno=True usa la voz original de Kokoro (para rehacer una toma que no se entendió)."""
        texto = rioplatense(texto)
        una_palabra = " " not in texto.strip()
        if voz in ("mujer", "paisana"):
            if voz == "paisana":
                vel *= 0.95
            a = dan.generate(texto, sid=0, speed=vel * 0.92)
            x = a_sr(np.array(a.samples), a.sample_rate)
            return varispeed(x, 0.93) if voz == "paisana" else x     # otra mujer: más grave
        motor, sid = {"hombre": (kok_mix, HOMBRE), "gaucho": (kok_mix, GAUCHO), "viejo": (kok_mix, VIEJO)}[voz]
        if alterno:
            motor, sid = {"hombre": (kok, 29), "gaucho": (kok, 29), "viejo": (kok, 53)}[voz]
        if voz == "viejo":
            vel *= 0.86
        t = texto + " Dale, dale." if una_palabra else texto
        a = motor.generate(t, sid=sid, speed=vel)
        x = a_sr(np.array(a.samples), a.sample_rate)
        if una_palabra:
            x = primer_segmento(x, SR)
        if voz == "viejo":
            x = temblor(x, tono=0.007, amp=0.05, semilla=semilla)   # el temblor de la edad, apenas
        return x

    return sintetizar


def main():
    import calidad_voz
    sintetizar = crear_sintetizador()
    asr = reconocedor()

    def entiende(x, clave):
        s = asr.create_stream()
        s.accept_waveform(SR, x)
        asr.decode_stream(s)
        return clave in normal(s.result.text), s.result.text

    for voz in ("hombre", "gaucho", "viejo", "mujer", "paisana"):
        if os.path.isdir(os.path.join(OUT, voz)):
            shutil.rmtree(os.path.join(OUT, voz))
    informe = []
    for voz in ("hombre", "gaucho", "viejo", "mujer", "paisana"):
        for clave, (estilos, textos) in GUION.items():
            for i, (estilo, texto) in enumerate(zip(estilos, textos), start=1):
                texto = AJUSTES.get((voz, clave, i), texto_para(voz, texto))
                estilo_fin = "gritado" if estilo == "gritado" else "grave" if estilo == "grave" else ""
                # varias versiones; gana la más natural (juez DNSMOS) entre las que se entienden
                candidatos = []
                for ajuste in (1.0, 0.93, 1.07):
                    x = sintetizar(voz, texto, VELOCIDAD[estilo] * ajuste, semilla=i)
                    y = terminar(x, estilo_fin)
                    ok, oido = entiende(y, CLAVES[clave])
                    candidatos.append((ok, calidad_voz.nota(y, SR), y, oido))
                candidatos.sort(key=lambda c: (c[0], c[1]), reverse=True)
                ok, nota, y, oido = candidatos[0]
                guardar_ogg(y, os.path.join(OUT, voz, f"{clave}_{i}.ogg"))
                informe.append((voz, clave, i, texto, oido, ok, nota))
                print(f"{voz:8s} {clave:20s} {i}  {'OK ' if ok else '?? '} {nota:.2f} {texto!r} -> {oido!r}", flush=True)
    # sounds.json: un evento por voz y canto, con sus 5 tomas al azar
    with open(SOUNDS_JSON, encoding="utf-8") as f:
        sonidos = json.load(f)
    propias = ("hombre", "gaucho", "viejo", "mujer", "paisana")   # las del Mandinga las hace voces_salamanca.py
    for k in [k for k in sonidos if k.startswith("voice.") or (k.startswith("voz.") and k.split(".")[1] in propias)]:
        del sonidos[k]
    for voz in ("hombre", "gaucho", "viejo", "mujer", "paisana"):
        for clave in GUION:
            sonidos[f"voz.{voz}.{clave}"] = {"category": "player", "subtitle": f"subtitles.trucoya.voice.{clave}",
                                             "sounds": [f"trucoya:voz/{voz}/{clave}_{i}" for i in range(1, 6)]}
    with open(SOUNDS_JSON, "w", encoding="utf-8") as f:
        json.dump(sonidos, f, indent=2, ensure_ascii=False)
    bien = sum(1 for r in informe if r[5])
    print(f"\n{bien}/{len(informe)} tomas verificadas por el reconocedor de voz")
    for voz in ("hombre", "gaucho", "viejo", "mujer", "paisana"):
        notas = [r[6] for r in informe if r[0] == voz]
        print(f"nota media {voz}: {sum(notas) / len(notas):.2f}")


if __name__ == "__main__":
    main()
