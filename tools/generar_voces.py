"""Genera las voces de los cantos con espeak-ng (es-419) y las procesa con ffmpeg
para que suenen más graves y con ambiente de boliche. Salida: .ogg mono (Minecraft)."""
import ctypes, os, subprocess, sys, wave
import espeakng_loader as el

OUT = sys.argv[1]
TMP = os.path.join(os.path.dirname(os.path.abspath(__file__)), "wav")
os.makedirs(OUT, exist_ok=True)
os.makedirs(TMP, exist_ok=True)

lib = ctypes.cdll.LoadLibrary(el.get_library_path())
SYNCH = 2
rate = lib.espeak_Initialize(SYNCH, 0, el.get_data_path().encode(), 0)
assert rate > 0, "espeak init failed"

buf = bytearray()
CB = ctypes.CFUNCTYPE(ctypes.c_int, ctypes.POINTER(ctypes.c_short), ctypes.c_int, ctypes.c_void_p)

@CB
def callback(wav, n, events):
    if n > 0:
        buf.extend(ctypes.string_at(wav, n * 2))
    return 0

lib.espeak_SetSynthCallback(callback)

def synth(text, voice, speed, pitch, rng, path):
    buf.clear()
    assert lib.espeak_SetVoiceByName(voice.encode()) == 0, voice
    lib.espeak_SetParameter(1, speed, 0)   # rate
    lib.espeak_SetParameter(3, pitch, 0)   # pitch
    lib.espeak_SetParameter(4, rng, 0)     # range (entonación)
    data = text.encode("utf-8")
    lib.espeak_Synth(data, len(data) + 1, 0, 0, 0, 1, None, None)  # espeakCHARS_UTF8
    lib.espeak_Synchronize()
    with wave.open(path, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(rate)
        w.writeframes(bytes(buf))

# clave -> texto (con signos para que la entonación sea exclamativa)
LINES = {
    "truco": "¡Truco!",
    "retruco": "¡Quiero retruco!",
    "vale_cuatro": "¡Quiero vale cuatro!",
    "envido": "¡Envido!",
    "real_envido": "¡Real envido!",
    "falta_envido": "¡Falta envido!",
    "flor": "¡Flor!",
    "contraflor": "¡Contraflor!",
    "contraflor_al_resto": "¡Contraflor al resto!",
    "con_flor_me_achico": "Con flor me achico.",
    "quiero": "¡Quiero!",
    "no_quiero": "No quiero.",
    "me_voy_al_mazo": "Me voy al mazo.",
}

# Tres "personajes": paisano (grave), gaucho viejo (más lento) y mozo (más ágil).
# (variante espeak, velocidad, tono, rango, factor de grave para ffmpeg)
VOICES = [
    ("es-419+m3", 150, 35, 70, 0.86),
    ("es-419+grandpa", 135, 30, 60, 0.90),
    ("es-419+m7", 165, 45, 80, 0.92),
]

for key, text in LINES.items():
    for i, (voice, speed, pitch, rng, deep) in enumerate(VOICES, start=1):
        wav = os.path.join(TMP, f"{key}_{i}.wav")
        synth(text, voice, speed, pitch, rng, wav)
        ogg = os.path.join(OUT, f"{key}_{i}.ogg")
        # Bajar el tono sin cambiar duración, calidez (graves), leve ambiente de boliche,
        # silencio recortado y volumen normalizado. Mono 44.1 kHz.
        af = (
            f"asetrate={rate}*{deep},aresample=44100,atempo={1/deep:.4f},"
            "silenceremove=start_periods=1:start_threshold=-45dB,"
            "areverse,silenceremove=start_periods=1:start_threshold=-45dB,areverse,"
            "bass=g=4:f=180,treble=g=-3:f=5000,"
            "aecho=0.8:0.6:35|70:0.18|0.10,"
            "loudnorm=I=-16:TP=-1.5:LRA=7,apad=pad_dur=0.08"
        )
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-af", af,
                        "-ac", "1", "-ar", "44100", "-c:a", "libvorbis", "-q:a", "5", ogg], check=True)
        print("ok", ogg)
