# Publicar en Modrinth

Todo lo que pide Modrinth, listo para copiar. La cuenta y la subida las hace una persona del grupo (Modrinth pide
iniciar sesión).

## El proyecto (una sola vez)

| Campo | Valor |
|---|---|
| Nombre | `Truco Ya` |
| URL (slug) | `truco-ya` |
| Resumen | `El truco uruguayo en Minecraft: muestra, piezas, envido, flor y truco, contra paisanos con IA o con amigos, y un modo historia por las pulperías.` |
| Tipo | Mod |
| Descripción | El contenido de [descripcion.md](descripcion.md) |
| Ícono | [icono.png](icono.png) (recorte de una captura del juego) |
| Categorías | `minigame`, `adventure`, `worldgen` (adicionales: `mobs`, `decoration`, `food`) |
| Cliente / servidor | Obligatorio en los dos |
| Licencia | MIT |
| Código fuente | https://github.com/Nicrolax/Grupo1_proyecto |
| Reportar problemas | https://github.com/Nicrolax/Grupo1_proyecto/issues |
| Wiki | https://github.com/Nicrolax/Grupo1_proyecto/blob/main/docs/MANUAL.md |
| Monetización | **Apagada** (la licencia de XTTS-v2, usado para las voces clonadas, es de uso no comercial) |

**Declaración de IA** (pestaña *Disclosures* de la configuración): marcar *Contains AI-generated content*, con
código y audio (voces) asistidos por IA. Es obligatorio desde agosto de 2026.

**Galería** (capturas del juego, en `docs/capturas/`):

| Imagen | Título | Destacada |
|---|---|---|
| `tapete.jpg` | Una mano de truco | sí |
| `mesa.jpg` | Las cartas sobre la mesa | |
| `pulperia.jpg` | Una pulpería en la sabana | |
| `estancia.jpg` | La Estancia del Caudillo | |
| `fachada.jpg` | La casa principal de la Estancia | |
| `vizcacha.jpg` | El Viejo Vizcacha | |
| `paso.jpg` | La cinemática "El Último Truco" | |
| `carga.jpg` | La carga al alba | |
| `libreta.jpg` | La Libreta del Truquero | |
| `fundadores.jpg` | Los cuatro Fundadores | |

## Cada versión

| Campo | Valor |
|---|---|
| Archivo | `trucoya-<versión>.jar` (lo arma `scripts\armar_jar.bat` en `entrega/`) |
| Número de versión | el de `gradle.properties` (por ejemplo `1.7.3`) |
| Canal | Release |
| Cargador | Forge |
| Versión de Minecraft | 1.16.5 |
| Dependencia | GeckoLib, **obligatoria** (buscarla como `geckolib` y elegir la 3.0.106 para Forge 1.16.5) |
| Novedades | la sección de esa versión en [CHANGELOG.md](../../CHANGELOG.md) |

Modrinth revisa cada proyecto nuevo antes de mostrarlo: suele tardar uno o dos días.
