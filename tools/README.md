# Herramientas

Programas en Python que generan los recursos del mod: texturas, modelos, estructuras, sonidos y música. Lo que
generan queda en `src/main/resources` y ya está en el repositorio: solo hace falta correrlos para cambiar algo.

```
pip install -r tools/requirements.txt     # además, ffmpeg en el PATH
python3 tools/<herramienta>.py            # siempre desde la raíz del proyecto
```

Los modelos de voz se bajan solos a `tools/.modelos/` (no se suben a git).

## Gráficos

| Herramienta | Qué genera |
|---|---|
| `generar_recursos.py` | Texturas de la mesa, la silla y el anotador; modelos, idiomas, recetas y botín (base). |
| `generar_cartas.py` | El atlas de las 40 cartas españolas en alta resolución. |
| `barajas_temas.py` | Las diez barajas coleccionables (papel, marco, dorso). |
| `generar_barajas_items.py` | Íconos y modelos de las barajas como ítems. |
| `generar_gaucho.py` | Modelo GeckoLib, animaciones y texturas de todos los personajes. |
| `generar_armas.py` | Facón, rebenque y boleadoras. |
| `generar_fase5.py` | Mate, picada, bebidas, discos y logros. |
| `generar_historia.py` | Libreta, copa, banderín y logros de la historia. |
| `generar_salamanca.py` | Farol de las Ánimas, Facón de Ledesma y sus logros. |
| `generar_logo.py` | El logo del repositorio y el ícono del mod (con las cartas de la baraja criolla). |

## Estructuras

| Herramienta | Qué genera |
|---|---|
| `generar_estructuras.py` | Todas las plantillas `.nbt` (corre las de abajo). |
| `estructuras_base.py` | La plantilla y las piezas comunes (muros, techos, puertas, mesas). |
| `estructuras_pulperias.py` | Una pulpería por bioma. |
| `estructuras_estancia.py` | La Estancia del Caudillo. |
| `estructuras_guarida.py` | La Guarida Clandestina. |
| `estructuras_paso.py` | El Paso de las Ánimas (de día) y la Salamanca (de noche). |
| `estructuras_detalle.py` | La pasada de detalle: zócalos, cornisas, aleros, ventanas, terreno. |
| `estructuras_arboles.py` | Árboles orgánicos (ombú, eucalipto, pino, espinillo...). |
| `vista_estructuras.py` | Vista isométrica de una plantilla, para revisarla sin abrir el juego. |

## Sonido

| Herramienta | Qué genera |
|---|---|
| `generar_voces_neural.py` | Los cantos de los paisanos (cinco voces × 13 cantos × 5 tomas). |
| `reparar_voces.py` | Rehace las tomas que el control de calidad no entendió. |
| `calidad_voz.py` | Nota de naturalidad de una voz (DNSMOS), la usan las demás. |
| `generar_musica.py` | Los cuatro discos de música criolla, sintetizados de cero. |
| `generar_efectos.py` | Efectos de sonido: carta jugada, mezclar y poroto. |
| `generar_cinematica.py` | Voz, música y guion de la cinemática "El Último Truco". |
| `generar_final.py` | La cinemática final con los Fundadores. |
| `voces_salamanca.py` | Voces y guiones de las escenas de La Salamanca. |
| `usar_grabaciones.py` | Pone las grabaciones reales de los Fundadores en la cinemática final. |
| `clonar_voces.py` | Clona las voces de los Fundadores para frases nuevas (lo corre `scripts\clonar_voces.bat`). |
| `felicitaciones.py` | Prepara las frases clonadas para el juego. |

`grabaciones/` tiene las grabaciones originales de los cuatro Fundadores y cómo grabar ([LEEME.txt](grabaciones/LEEME.txt)).

## Documentación

| Herramienta | Qué genera |
|---|---|
| `docs/armar_informe.py` | `docs/Informe_TrucoYa.pdf` con toda la documentación. |
