"""
Prepara las felicitaciones de los Fundadores (voces clonadas con tools/clonar_voces.py en la PC) para el juego:
elige la toma, recorta silencios (también las pausas largas del medio), empareja el volumen con las otras voces
y las guarda en .ogg mono (para que se escuchen desde donde está cada uno).
Uso: python3 tools/felicitaciones.py tools/grabaciones/clonadas nicolas=0 agustin=1 matias=0 thiago=1
"""
import json, os, subprocess, sys, tempfile, wave
import numpy as np

AQUI = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(AQUI, "..", "src", "main", "resources", "assets", "trucoya")
SALIDA = os.path.join(RES, "sounds", "voz", "fundadores")
NIVEL = 0.13


def leer(p):
    with wave.open(p) as w:
        sr = w.getframerate()
        x = np.frombuffer(w.readframes(w.getnframes()), dtype=np.int16).astype(np.float64) / 32768
    return x, sr


def sin_pausas(x, sr, max_pausa=0.45, deja=0.28):
    k = int(sr * 0.02)
    env = np.sqrt(np.convolve(x ** 2, np.ones(k) / k, mode="same"))
    umbral = max(np.percentile(env, 15) * 3, env.max() * 0.03)
    voz = env > umbral
    idx = np.where(voz)[0]
    if len(idx) == 0:
        return x
    a, b = max(0, idx[0] - int(sr * 0.06)), min(len(x), idx[-1] + int(sr * 0.18))
    x, voz = x[a:b], voz[a:b]
    partes, i, n = [], 0, len(x)
    while i < n:
        if not voz[i]:
            j = i
            while j < n and not voz[j]:
                j += 1
            largo = j - i
            if largo > max_pausa * sr:
                m = int(deja * sr / 2)
                partes.append(x[i:i + m] * np.linspace(1, 0, m))
                partes.append(x[j - m:j] * np.linspace(0, 1, m))
            else:
                partes.append(x[i:j])
            i = j
        else:
            j = i
            while j < n and voz[j]:
                j += 1
            partes.append(x[i:j])
            i = j
    return np.concatenate(partes)


def main():
    carpeta = sys.argv[1]
    elegidas = dict(a.split("=") for a in sys.argv[2:])
    os.makedirs(SALIDA, exist_ok=True)
    sj = os.path.join(RES, "sounds.json")
    sounds = json.load(open(sj, encoding="utf-8"))
    duraciones = {}
    for quien, toma in elegidas.items():
        x, sr = leer(os.path.join(carpeta, "felicita_%s_%s.wav" % (quien, toma)))
        x = sin_pausas(x, sr)
        k = int(sr * 0.05)
        env = np.sqrt(np.convolve(x ** 2, np.ones(k) / k, mode="same"))
        rms = np.sqrt(np.mean(x[env > env.max() * 0.1] ** 2))
        x = np.clip(x * (NIVEL * 1.25 / (rms + 1e-9)), -0.98, 0.98)
        duraciones[quien] = round(len(x) / sr, 2)
        with tempfile.TemporaryDirectory() as d:
            raw = os.path.join(d, "x.raw")
            x.astype(np.float32).tofile(raw)
            subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "f32le", "-ar", str(sr), "-ac", "1", "-i", raw,
                            "-af", "highpass=f=70,afade=t=in:d=0.02", "-ar", "44100", "-ac", "1", "-c:a", "libvorbis",
                            "-q:a", "5", os.path.join(SALIDA, "felicita_%s.ogg" % quien)], check=True)
        sounds["fundadores.felicita_" + quien] = {"category": "voice", "subtitle": "subtitles.trucoya.fundador",
                                                  "sounds": [{"name": "trucoya:voz/fundadores/felicita_" + quien,
                                                              "attenuation_distance": 24}]}
    json.dump(sounds, open(sj, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    print(json.dumps(duraciones))


if __name__ == "__main__":
    main()
