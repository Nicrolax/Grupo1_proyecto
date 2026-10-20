# 3. Stack tecnológico

Truco Ya es un **mod de Minecraft**. Las capas habituales de un sistema (frontend, backend, base de datos,
red) existen igual, pero dentro de la arquitectura cliente-servidor de Minecraft: el *frontend* es el
cliente del juego y el *backend* es el servidor (integrado si se juega solo, dedicado si se juega en red).

## 3.1 Tecnologías del producto

| Capa | Tecnología (versión) | Para qué se usa en Truco Ya | Por qué se eligió |
|---|---|---|---|
| Lenguaje | **Java 8** (OpenJDK / Temurin 8) | Todo el mod: motor de truco, lógica de servidor y pantallas. | Es el lenguaje y la versión que exige Minecraft 1.16.5. Tiene tipado estático y programación orientada a objetos, que es lo que se trabaja en el curso. |
| Plataforma | **Minecraft Java Edition 1.16.5** | El mundo, el render 3D, los jugadores y el servidor. | Ahí está el público (ver [01-situacion-problema.md](01-situacion-problema.md)). Corre en computadoras modestas y es de las versiones con más mods. |
| Framework (backend y frontend) | **Minecraft Forge 36.2.39** | Registro de bloques, ítems, entidades y sonidos. Bus de eventos, canal de red, configuración del servidor y generación de estructuras. | Es el cargador de mods estándar para 1.16.5. Trae de fábrica red, configuración y estructuras; con Fabric harían falta varias librerías aparte. |
| Mapeos | **Mapeos oficiales de Mojang** (`official 1.16.5`) | Nombres legibles de las clases de Minecraft al programar. | Son los nombres oficiales, publicados por Mojang. |
| Animación 3D | **GeckoLib 3.0.106** | Modelos y animaciones de los paisanos: caminar, sentarse, gestos al cantar, montar. | Es la librería estándar para animaciones complejas en Forge. Los modelos se exportan desde Blockbench. |
| Interfaz gráfica (frontend) | API de pantallas de Minecraft (`Screen`, `TileEntityRenderer`, `RenderSystem`) | El tapete, la Libreta, las señas, las cartas sobre la mesa y las cinemáticas. | Es lo nativo del juego: no agrega dependencias y respeta la escala de interfaz del jugador. |
| Comunicación cliente-servidor | **Forge SimpleChannel** (canal `trucoya:main`, protocolo v7), sobre la conexión TCP de Minecraft | 10 paquetes propios (ver [06-infraestructura.md](06-infraestructura.md)). | Viaja por la misma conexión que el juego, con versión de protocolo propia. Si cliente y servidor tienen versiones distintas del mod, no se conectan. |
| Persistencia ("base de datos") | **NBT** de Minecraft: `WorldSavedData` (datos del mundo), datos persistentes del jugador y datos de bloques y entidades | Progreso de cada jugador (prestigio, sellos, barajas, pedidos, desafíos), estado de cada mesa, pedidos de cada paisano, lugar del Paso de las Ánimas. | Se guarda junto con el mundo, en el mismo momento que el resto del juego. No requiere servidor de base de datos ni red, y una copia del mundo incluye todo. Se evaluó una base SQL (por ejemplo, SQLite) y se descartó: obliga a sincronizar dos almacenamientos y complica las copias de seguridad, sin necesidad de consultas complejas. |
| Configuración del servidor | **ForgeConfigSpec** (TOML) | `serverconfig/trucoya-server.toml`: Salamanca, pedidos y espera de comandos. | Es lo estándar de Forge: valida tipos y rangos, y se guarda con cada mundo. |
| Contenido de datos | JSON de Minecraft: modelos, estados de bloque, logros, recetas, idiomas, sonidos; plantillas de estructuras `.nbt` | Bloques, ítems, recetas, 20+ logros, 5 idiomas y las estructuras del mundo. | Es el formato de recursos y datos de Minecraft. |

## 3.2 Herramientas de construcción y calidad

| Herramienta | Uso | Justificación |
|---|---|---|
| **Gradle 7.6.4** (wrapper incluido) + **ForgeGradle 5.1** | Compilar, ofuscar de vuelta (reobf) y armar el `.jar`. También `coreTests`, `runClient` y `runServer`. | Es el sistema de construcción oficial de Forge. El wrapper baja la versión correcta sin instalar nada. |
| **Pruebas propias del motor** (`TrucoCoreTests`, Java puro) | 33 pruebas automáticas de reglas, IA y serialización. | El motor no depende de Minecraft, así que se prueba en segundos sin abrir el juego ni bajar librerías. |
| **GitHub Actions** | Integración continua: compila y corre las pruebas en cada push. | Gratis para repositorios de GitHub; detecta enseguida si un cambio rompe algo. |
| **PMD 7** (reglas en `config/pmd/reglas.xml`) | Análisis estático: errores probables, seguridad y buenas prácticas. | Herramienta libre, estándar para Java (ver [08-calidad.md](08-calidad.md)). |
| **PlantUML** | Diagramas UML en `docs/uml/*.puml` (texto versionado). | Los diagramas se mantienen como código, junto con el programa. |
| Scripts `.bat` de Windows | `probar.bat`, `jugar.bat`, `actualizar.bat`, `servidor.bat` y `armar_jar.bat`. | El grupo trabaja en Windows: con doble clic se baja el JDK, se compila, se juega y se arma el servidor. |

## 3.3 Herramientas de producción de contenido (no van dentro del `.jar`)

Las texturas, estructuras, voces y música del mod se generan con programas propios en Python, en `tools/`.
Así todo el contenido es original y se puede volver a generar.

| Herramienta | Uso |
|---|---|
| **Python 3** con **NumPy**, **SciPy** y **Pillow** | Texturas (las 10 barajas de 40 cartas, ítems, bloques) y plantillas de estructuras `.nbt` (pulperías, estancia, Salamanca). |
| **sherpa-onnx** (Kokoro, Piper, Whisper, GTCRN), **ONNX Runtime**, **pyworld**, **librosa** | Voces de los cantos y de los personajes con síntesis neuronal. Control de calidad con reconocimiento de voz (Whisper) y nota de oyente (DNSMOS). Limpieza de las grabaciones reales de los Fundadores (GTCRN). |
| **FFmpeg** | Conversión de audio a OGG Vorbis, el formato de Minecraft. |
| **nbtlib** + `tools/vista_estructuras.py` | Escribir las plantillas de estructuras (`.nbt`) y dibujarlas en isométrico, con las texturas de Minecraft, para revisarlas sin abrir el juego. |

Dependencias: `tools/requirements.txt`. Los modelos de voz se descargan solos a `tools/.modelos/`, que no
se sube al repositorio.

## 3.4 Correspondencia entre el stack y el código

| Declarado | Dónde está en el código |
|---|---|
| Java 8 | `build.gradle` (`java.toolchain.languageVersion = 8`); el JDK 8 lo baja `probar.bat`. |
| Forge 36.2.39, Minecraft 1.16.5 | `gradle.properties` (`forge_version`, `minecraft_version`), `META-INF/mods.toml`. |
| GeckoLib 3.0.106 | `gradle.properties` (`geckolib_version`), `entity/GauchoEntity.java` (`IAnimatable`), `client/GauchoModel.java`, `client/GauchoRenderer.java`. |
| SimpleChannel | `network/TrucoNetwork.java` y los 10 `*Packet.java`. |
| NBT | `progreso/JugadorData.java`, `progreso/SalamancaSitio.Datos` (`WorldSavedData`), `tileentity/MesaTrucoTileEntity.save/load`, `entity/GauchoEntity.addAdditionalSaveData`. |
| ForgeConfigSpec | `TrucoConfig.java`, registrado en `TrucoYa.java`. |
| Pruebas | `src/test/java/com/trucoya/core/TrucoCoreTests.java`, tarea `coreTests` en `build.gradle`. |
| CI | `.github/workflows/build.yml`. |
| PMD | `config/pmd/reglas.xml`. |
