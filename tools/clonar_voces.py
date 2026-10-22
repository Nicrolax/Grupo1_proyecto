"""
Clona las voces de los cuatro Fundadores (sus grabaciones) para decir frases nuevas.

Corre en la PC (lo llama scripts/clonar_voces.bat, que instala todo en la carpeta .clonar, que no se sube a GitHub).
Modelo: XTTS-v2 de Coqui (clonación de voz con unos segundos de muestra, habla español). Licencia del modelo:
Coqui Public Model License (uso no comercial), que alcanza para este proyecto de clase.

Entrada:  tools/grabaciones/referencias/<quien>.flac (las grabaciones de cada uno, ya limpias)
          tools/grabaciones/clonar_textos.json       (id, quién y qué dice)
Salida:   tools/grabaciones/clonadas/<id>_<toma>.wav   (tres tomas por frase; después se elige la mejor)
"""
import json, os, sys, time

AQUI = os.path.dirname(os.path.abspath(__file__))
RAIZ = os.path.dirname(AQUI)
GRAB = os.path.join(AQUI, "grabaciones")
TOMAS = int(os.environ.get("TOMAS", "3"))


def main():
    os.environ.setdefault("COQUI_TOS_AGREED", "1")
    import torch
    from TTS.api import TTS
    print("torch", torch.__version__, "| hilos", torch.get_num_threads(), flush=True)
    t0 = time.time()
    tts = TTS("tts_models/multilingual/multi-dataset/xtts_v2").to("cpu")
    print("modelo cargado en %.0f s" % (time.time() - t0), flush=True)
    datos = json.load(open(os.path.join(GRAB, "clonar_textos.json"), encoding="utf-8"))
    sal = os.path.join(GRAB, "clonadas")
    os.makedirs(sal, exist_ok=True)
    for l in datos["lineas"]:
        ref = os.path.join(GRAB, "referencias", l["quien"] + ".flac")
        for k in range(TOMAS):
            out = os.path.join(sal, "%s_%d.wav" % (l["id"], k))
            if os.path.exists(out):
                continue
            torch.manual_seed(1000 + k)
            t = time.time()
            tts.tts_to_file(text=l["texto"], speaker_wav=ref, language="es", file_path=out,
                            temperature=0.65 + 0.05 * k, repetition_penalty=5.0, top_p=0.85, split_sentences=True)
            print("OK %s toma %d (%.0f s)" % (l["id"], k, time.time() - t), flush=True)
    print("LISTO", flush=True)


if __name__ == "__main__":
    try:
        main()
    except Exception:
        import traceback
        traceback.print_exc()
        sys.exit(1)
