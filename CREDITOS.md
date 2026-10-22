# Créditos y licencias de terceros

## Autores

**Grupo 1 — los Fundadores:** Nicolás Bentancour, Agustín Perdomo, Matías Piñeyro y Thiago Rodríguez.
Curso Programación Avanzada (UTU), 2026.

El código del mod y de las herramientas es [MIT](LICENSE).

## Dependencia del mod

| Qué | Para qué | Licencia |
|---|---|---|
| [GeckoLib 3.0.106](https://github.com/bernie-g/geckolib) | Modelos y animaciones de los gauchos | MIT |
| [Minecraft Forge 36.2.39](https://github.com/MinecraftForge/MinecraftForge) | Cargador de mods | LGPL-2.1 |

GeckoLib no va dentro del `.jar` de Truco Ya: se instala aparte.

## Modelos usados para hacer las voces

Estos modelos **no** van dentro del mod: se usaron en la PC para generar los audios `.ogg` que sí van.

| Modelo | Qué se hizo con él | Licencia |
|---|---|---|
| [Kokoro-82M](https://huggingface.co/hexgrad/Kokoro-82M) (vía [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx)) | Voces masculinas de los cantos, el Vizcacha, el Mandinga y Ledesma | Apache-2.0 |
| [Piper](https://github.com/rhasspy/piper), voz es_AR "daniela" | Voces femeninas de los cantos | Modelo entrenado con el corpus OpenSLR 61 (CC BY-SA 4.0) |
| [XTTS-v2](https://huggingface.co/coqui/XTTS-v2) (vía [coqui-tts](https://github.com/idiap/coqui-ai-TTS)) | Felicitaciones finales de los Fundadores, clonando sus propias grabaciones con su permiso | [Coqui Public Model License](https://coqui.ai/cpml) (uso no comercial) |
| [Whisper](https://github.com/openai/whisper) | Control de calidad: verificar que cada audio se entienda | MIT |
| [DNSMOS](https://github.com/microsoft/DNS-Challenge) | Control de calidad: nota de naturalidad de cada voz | MIT (CC BY 4.0 para los modelos) |
| [GTCRN](https://github.com/Xiaobin-Rong/gtcrn) | Limpiar el ruido de las grabaciones de los Fundadores | MIT |

Por la licencia de XTTS-v2, **Truco Ya no se distribuye con fines comerciales**: no se monetiza en Modrinth ni
en ningún otro lado.

## Obras y cultura

- El **Viejo Vizcacha** es un personaje de *Martín Fierro* (José Hernández, 1872–1879), obra de dominio público.
  La historia del mod es original.
- La **Salamanca** y el **Mandinga** son leyendas del folclore rioplatense.
- La música (milonga, candombe, vals, payada y la banda de las cinemáticas) se compuso para el mod y se genera
  con `tools/generar_musica.py`.

## Imágenes

Las texturas, los modelos y el logo los dibujan programas propios en `tools/` (escritos con asistencia de IA);
no se usaron generadores de imágenes. Las capturas de `docs/capturas/` son del juego.

Minecraft es una marca de Mojang Studios. Este proyecto no está afiliado a Mojang ni a Microsoft.
