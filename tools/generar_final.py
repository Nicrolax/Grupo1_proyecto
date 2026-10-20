"""
Cinemática FINAL (después de ganarle al Viejo Vizcacha): llegan los cuatro Fundadores, los creadores del mod.

Genera:
  sounds/cinematica/final_N.ogg       las voces (el Vizcacha y los cuatro Fundadores)
  sounds/cinematica/final_musica.ogg  la música, compuesta a la línea de tiempo
  cinematica/final.json               subtítulos, carteles y títulos (los lee el juego)
y agrega los sonidos a sounds.json.

  0-13 s   el Vizcacha entrega la Baraja de Oro
 13-27 s   "ESA MISMA TARDE": cuatro jinetes llegan al fogón
 27-66 s   cada Fundador dice lo suyo (Nicolás, Agustín, Matías, Thiago), 9,5 s cada uno
 66-78 s   la ronda alrededor del fuego
 78-100 s  la cámara sube al cielo: TRUCO YA y los créditos
100-110 s después de los créditos: pantalla negra, alguien baraja... "Setenta años, Vizcacha..." (La Salamanca)

Uso: python3 tools/generar_final.py [--prueba salida.ogg]
"""
import json, os, sys
import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import generar_voces_neural as V   # noqa: E402
import generar_musica as M          # noqa: E402
import generar_cinematica as C      # noqa: E402
import calidad_voz                   # noqa: E402

SR = V.SR
MSR = M.SR
# Cada Fundador tiene un turno de TURNO s (su primer plano); lo que viene después se corre CORRE s.
# (Tiene que coincidir con Cinematica.TURNO_FUNDADOR.)
TURNO = 9.5
CORRE = 4 * (TURNO - 6.0)
TITULOS = 86.0 + CORRE      # termina la música y los créditos
DURACION = TITULOS + 10.0   # y después de los créditos: pantalla negra, alguien baraja... (La Salamanca)
F0 = 28.2   # cuando empieza a hablar el primero


def turno(i, fin=8.3):
    return F0 + TURNO * i, F0 + TURNO * i + fin


# (inicio, fin máximo, voz, varispeed (más joven), texto)
LINEAS = [
    (1.4, 6.2, "viejo", 1.0, "Setenta inviernos esperé a alguien como vos, gurí."),
    (6.6, 12.2, "viejo", 1.0, "La Baraja de Oro es tuya. El Negro Ledesma estaría orgulloso."),
    (20.6, 26.4, "viejo", 1.0, "Mirá quiénes vienen por el camino... los que armaron todo esto."),
    (*turno(0), "hombre", 1.07,
     "¡No lo puedo creer! Le ganaste al Viejo Vizcacha. Nadie le ganaba hace setenta años, ¿te das cuenta?"),
    (*turno(1), "hombre_b", 1.10,
     "Te vi jugar la última mano y no sabía si mirar o taparme los ojos. Pero ganaste, que es lo que importa."),
    (*turno(2), "gaucho", 1.04,
     "Che, muy lindo todo, pero esto se festeja con un asado. ¿Alguien trajo carne? ... ¿Nadie? Bueno, voy yo."),
    (*turno(3, 10.0), "hombre", 1.12,
     "Gracias por jugar, en serio. Hicimos este juego entre amigos, con muchas ganas, y ahora vos también sos parte de esta mesa."),
    (54.4 + CORRE, 61.0 + CORRE, "viejo", 1.0, "Siéntense, muchachos. La noche es larga, y el truco no se termina nunca."),
    (TITULOS + 4.0, TITULOS + 9.0, "diablo", 1.0, "Setenta años, Vizcacha... ¿Todavía tenés mi baraja?"),
]
CARTELES = [
    (13.9, 18.6, "ESA MISMA TARDE", "en la Estancia del Caudillo", False),
    (65.0 + CORRE, 70.6 + CORRE, "TRUCO YA", "El Camino del Truco", True),
    (71.0 + CORRE, 73.8 + CORRE, "NICOLÁS BENTANCOUR", "creado por", False),
    (74.0 + CORRE, 76.8 + CORRE, "AGUSTÍN PERDOMO", "creado por", False),
    (77.0 + CORRE, 79.8 + CORRE, "MATÍAS PIÑEYRO", "creado por", False),
    (80.0 + CORRE, 82.8 + CORRE, "THIAGO RODRÍGUEZ", "creado por", False),
    (83.0 + CORRE, 86.0 + CORRE, "¡GRACIAS POR JUGAR!", "El Viejo Vizcacha te espera para la revancha", True),
]


def voces():
    sint = V.crear_sintetizador()
    asr = V.reconocedor()
    subs = []
    for n, (t0, t1, voz, vs, texto) in enumerate(LINEAS, start=1):
        hueco = t1 - t0
        if voz == "diablo":
            import voces_salamanca as VS
            # dicha con intención: pausa después de "Vizcacha..." y la pregunta con melodía de pregunta
            partes = [p.strip() for p in texto.replace("... ", "...|").split("|")]
            x = VS.diablo_frase([sint("hombre", p_, 0.78 if not p_.startswith("¿") else 0.88, semilla=n) for p_ in partes],
                                [0.75] * (len(partes) - 1) + [0.0], [p_.startswith("¿") for p_ in partes])
            C.guardar(x, SR, os.path.join(C.SND, f"final_{n}.ogg"))
            subs.append({"t": t0, "fin": round(t0 + len(x) / SR + 0.5, 2), "texto": texto, "sonido": f"cinematica.final_{n}"})
            print(f"final {n} (diablo): {len(x) / SR:.1f} s", flush=True)
            continue
        if voz != "viejo":
            # los Fundadores ponen su propia voz (tools/usar_grabaciones.py); hasta entonces, solo el subtítulo
            C.guardar(np.zeros(int(SR * 0.2)), SR, os.path.join(C.SND, f"final_{n}.ogg"))
            lectura = min(hueco, max(3.0, len(texto) / 14.0))
            subs.append({"t": t0, "fin": round(t0 + lectura, 2), "texto": texto, "sonido": f"cinematica.final_{n}"})
            print(f"final {n} ({voz}): sin voz, la graban los Fundadores", flush=True)
            continue
        alterno = voz == "hombre_b"
        voz_real = "hombre" if alterno else voz
        mejor = None
        for vel in (1.0, 1.05, 1.1, 1.16, 1.24):
            x = sint(voz_real, texto, vel * (0.93 if voz == "viejo" else 1.0), semilla=n, alterno=alterno)
            if vs != 1.0:
                x = V.varispeed(x, vs)         # un toque más agudo y ágil: voces jóvenes
            x = V.recortar(x)
            x = V.eq(x, 80, 0, "hp")
            x = V.sala(x, 0.12 if voz == "viejo" else 0.08)
            x = x * (0.13 / (np.sqrt(np.mean(x ** 2)) + 1e-9))
            x = x * min(1.0, 0.95 / (np.abs(x).max() + 1e-9))
            nota = calidad_voz.nota(x, SR)
            entra = len(x) / SR <= hueco
            if mejor is None or (entra and (not mejor[3] or nota > mejor[1])):
                mejor = (x, nota, vel, entra)
            if entra and len(x) / SR > hueco - 0.8:
                break
        x, nota, vel, _ = mejor
        C.guardar(x, SR, os.path.join(C.SND, f"final_{n}.ogg"))
        s = asr.create_stream()
        s.accept_waveform(SR, x)
        asr.decode_stream(s)
        dur = len(x) / SR
        print(f"final {n} ({voz}): {dur:.1f} s (hueco {hueco:.1f}) nota {nota:.2f} -> {s.result.text}", flush=True)
        subs.append({"t": t0, "fin": round(t0 + max(dur, 2.0) + 0.4, 2), "texto": texto, "sonido": f"cinematica.final_{n}"})
    return subs


def musica(subs):
    p = M.Pista(TITULOS)
    hz, midi = M.hz, M.midi
    compas = 2.4
    # 0-13: el bandoneón del Vizcacha, sobre un colchón grave (re menor que se abre a mayor)
    p.poner(0.0, C.pad([hz(midi("D2")), hz(midi("A2")), hz(midi("F3"))], 13.4, 0.28, ataque=2.5, brillo=1200), 1.0)
    tema = [("A4", 0, 1.2), ("D5", 1.2, 1.8), ("E5", 3.0, 0.6), ("F5", 3.6, 1.8), ("E5", 5.4, 1.2), ("D5", 6.6, 2.4),
            ("C#5", 9.0, 1.2), ("D5", 10.2, 2.6)]
    for nota, ini, d in tema:
        p.poner(0.8 + ini, M.bandoneon(hz(midi(nota)), d * 0.95, 0.42, vib=5.0), 1.0)
    p.poner(10.5, C.platillo(2.4, 0.08), 1.0)
    # 13.5-27: llegan los jinetes: milonga en re mayor que crece, con bombo
    acordes = ["D", "D", "A7", "A7", "G", "D", "A7", "D"]
    t = 13.6
    for k in range(6):
        simb = acordes[k % len(acordes)]
        raiz, ints = M.acorde(simb)
        crece = 0.5 + 0.5 * k / 5
        for corch, iv in zip((0, 3, 6), (0, 7, 12)):
            p.poner(t + corch * compas / 8, M.guitarra(hz(raiz - 12 + iv), 1.6, 0.55 * crece, 0.3), 1.0)
        for corch in (1, 2, 4, 5, 7):
            p.poner(t + corch * compas / 8, M.guitarra(hz(raiz + 12 + ints[(corch + k) % 3]), 0.9, 0.22 * crece, 0.7), 1.0)
        for b in (0, 3):
            p.poner(t + b * compas / 8, M.tambor(62, 0.8, 0.45 * crece, ruido=0.15, caida=4), 1.0)
        t += compas
    p.poner(24.8, C.platillo(2.4, 0.14), 1.0)
    # 27.5-(52+CORRE): los Fundadores hablan: milonga suave y alegre por debajo
    t = 27.6
    k = 0
    while t < 51.8 + CORRE:
        simb = acordes[k % len(acordes)]
        raiz, ints = M.acorde(simb)
        p.poner(t, M.guitarra(hz(raiz - 12), 1.8, 0.45, 0.3), 1.0)
        for corch in (2, 3, 5, 6):
            p.poner(t + corch * compas / 8, M.guitarra(hz(raiz + 12 + ints[(corch + k) % 3]), 0.8, 0.17, 0.7), 1.0)
        t += compas
        k += 1
    # (52-64)+CORRE: la ronda: guitarra y bandoneón juntos, la melodía en mayor
    t = 51.8 + CORRE
    melodia = [("F#4", 0, 1.2), ("A4", 1.2, 1.2), ("D5", 2.4, 2.4), ("C#5", 4.8, 1.2), ("B4", 6.0, 1.2), ("A4", 7.2, 2.4),
               ("G4", 9.6, 1.2), ("F#4", 10.8, 1.2)]
    for nota, ini, d in melodia:
        p.poner(t + ini, M.bandoneon(hz(midi(nota)), d * 0.95, 0.4, vib=5.0), 1.0)
    for k in range(5):
        raiz, ints = M.acorde(["D", "G", "A7", "D", "D"][k])
        p.poner(t + k * compas, C.pad([hz(raiz + j) for j in ints[:3]], compas + 0.4, 0.16, ataque=0.5, brillo=1500), 1.0)
        for corch in (0, 3, 6):
            p.poner(t + k * compas + corch * compas / 8, M.guitarra(hz(raiz - 12 + (0, 7, 12)[corch // 3]), 1.4, 0.4, 0.4), 1.0)
    # (64-86)+CORRE: títulos: el tema del Último Truco, ahora en mayor y grande
    p.poner(63.8 + CORRE, C.platillo(1.6, 0.18), 1.0)
    p.poner(64.6 + CORRE, M.tambor(52, 2.5, 0.8, ruido=0.2, caida=1.6), 1.0)
    final = [("D4", 0, 0.9), ("F#4", 0.9, 0.45), ("A4", 1.35, 0.45), ("D5", 1.8, 1.8), ("C#5", 3.6, 0.9),
             ("B4", 4.5, 0.9), ("A4", 5.4, 2.4), ("B4", 7.8, 0.9), ("C#5", 8.7, 0.9), ("D5", 9.6, 3.0)]
    for base in (64.6 + CORRE, 77.0 + CORRE):
        for nota, ini, d in final:
            p.poner(base + ini, C.bronce(hz(midi(nota)), d, 0.38), 1.0)
            p.poner(base + ini, M.bandoneon(hz(midi(nota)), d * 0.95, 0.3, vib=5.0), 1.0)
    for k in range(9):
        raiz, ints = M.acorde(["D", "G", "A7", "D", "G", "D", "A7", "D", "D"][k])
        p.poner(64.6 + CORRE + k * compas, C.pad([hz(raiz + j) for j in ints[:3]], compas + 0.5, 0.2, ataque=0.6, brillo=2000), 1.0)
        p.poner(64.6 + CORRE + k * compas, C.coro(hz(raiz + 12 + ints[1]), compas + 0.6, 0.06), 1.0)
    for j, m in enumerate([50, 57, 62, 66, 69, 74]):
        p.poner(82.0 + CORRE + j * 0.06, M.guitarra(hz(m), 4.0, 0.42, 0.6), 1.0)
    p.poner(82.0 + CORRE, C.campana(hz(midi("D6")), 3.5, 0.08), 1.0)
    x = M.reverb(M.caja_guitarra(p.buf[:int(MSR * TITULOS)]), 0.28)
    x = x / (np.abs(x).max() + 1e-9) * 0.85
    env = np.ones(len(x))
    for s in subs:
        a, b = int(MSR * (s["t"] - 0.35)), int(MSR * (s["fin"] - 0.2))
        env[max(0, a):max(0, b)] = 0.42
    k = int(MSR * 0.35)
    x = x * np.convolve(env, np.ones(k) / k, mode="same")
    fin = int(MSR * 1.4)
    x[-fin:] *= np.linspace(1, 0, fin)
    C.guardar(x, MSR, os.path.join(C.SND, "final_musica.ogg"))
    print("música final:", len(x) / MSR, "s")


def prueba(subs, ruta):
    import subprocess, tempfile
    with tempfile.TemporaryDirectory() as d:
        def leer(ogg):
            w = os.path.join(d, "x.raw")
            subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", ogg, "-f", "f32le", "-ac", "1", "-ar", str(MSR), w], check=True)
            return np.fromfile(w, dtype=np.float32)
        total = leer(os.path.join(C.SND, "final_musica.ogg")).astype(np.float64)
        for s in subs:
            v = leer(os.path.join(C.SND, s["sonido"].split(".")[1] + ".ogg"))
            i = int(MSR * s["t"])
            total[i:i + len(v)] += v[:max(0, len(total) - i)]
        total = total / max(1.0, np.abs(total).max() / 0.98)
        C.guardar(total, MSR, ruta)


def main():
    subs = voces()
    musica(subs)
    datos = {"duracion": DURACION, "subtitulos": subs,
             "carteles": [{"t": a, "fin": b, "titulo": c, "sub": d, "grande": g} for a, b, c, d, g in CARTELES]}
    ruta = os.path.join(C.ASSETS, "cinematica", "final.json")
    with open(ruta, "w", encoding="utf-8") as f:
        json.dump(datos, f, indent=2, ensure_ascii=False)
    sp = os.path.join(C.ASSETS, "sounds.json")
    with open(sp, encoding="utf-8") as f:
        sonidos = json.load(f)
    sonidos["cinematica.final_musica"] = {"category": "music",
                                          "sounds": [{"name": "trucoya:cinematica/final_musica", "stream": True}]}
    for n in range(1, len(LINEAS) + 1):
        sonidos[f"cinematica.final_{n}"] = {"category": "voice", "sounds": [f"trucoya:cinematica/final_{n}"]}
    with open(sp, "w", encoding="utf-8") as f:
        json.dump(sonidos, f, indent=2, ensure_ascii=False)
    if "--prueba" in sys.argv:
        prueba(subs, sys.argv[sys.argv.index("--prueba") + 1])
    print("listo:", ruta)


if __name__ == "__main__":
    main()
