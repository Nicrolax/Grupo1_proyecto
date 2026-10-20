"""
Rehace las tomas que el reconocedor no entendió (las marcadas con ?? en la salida de generar_voces_neural.py).
Uso: python3 tools/reparar_voces.py salida.log
"""
import os, re, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import generar_voces_neural as V
import calidad_voz

def main(log):
    fallas = []
    for linea in open(log, encoding="utf-8"):
        m = re.match(r"(\w+)\s+(\w+)\s+(\d)\s+\?\?", linea)
        if m:
            fallas.append((m.group(1), m.group(2), int(m.group(3))))
    sint = V.crear_sintetizador()
    asr = V.reconocedor()
    for voz, clave, i in fallas:
        estilos, textos = V.GUION[clave]
        estilo = estilos[i - 1]
        texto = V.AJUSTES.get((voz, clave, i), V.texto_para(voz, textos[i - 1]))
        sin_signos = texto.replace("¡", "").replace("!", ".")
        fin = "gritado" if estilo == "gritado" else "grave" if estilo == "grave" else ""
        hecho = False
        for alterno in (False, True):
            for t in (texto, sin_signos):
                for ajuste in (0.9, 1.1, 0.8, 1.2):
                    y = V.terminar(sint(voz, t, V.VELOCIDAD[estilo] * ajuste, semilla=i, alterno=alterno), fin)
                    s = asr.create_stream(); s.accept_waveform(V.SR, y); asr.decode_stream(s)
                    if V.CLAVES[clave] in V.normal(s.result.text):
                        V.guardar_ogg(y, os.path.join(V.OUT, voz, f"{clave}_{i}.ogg"))
                        print(f"reparada {voz} {clave} {i}: {t!r} vel {ajuste} alterno={alterno} -> {s.result.text!r} "
                              f"nota {calidad_voz.nota(y, V.SR):.2f}", flush=True)
                        hecho = True
                        break
                if hecho: break
            if hecho: break
        if not hecho:
            print(f"SIN REPARAR {voz} {clave} {i}", flush=True)

if __name__ == "__main__":
    main(sys.argv[1])
