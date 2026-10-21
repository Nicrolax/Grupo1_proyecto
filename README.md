# Truco Ya

**El truco uruguayo dentro de Minecraft.** Es un mod para Minecraft Java 1.16.5 (Forge) que permite:
- jugar al truco con las reglas uruguayas de verdad (muestra, piezas, flor, malas y buenas), contra paisanos con inteligencia artificial o con amigos en un servidor;
- recorrer un modo historia por las pulperías del Uruguay hasta el Viejo Vizcacha y la leyenda de la Salamanca.

Proyecto del **Grupo 1** de Programación Avanzada (UTU): Nicolás Bentancour, Agustín Perdomo, Matías
Piñeyro y Thiago Rodríguez.

![Compilar y probar](https://github.com/Nicrolax/Grupo1_proyecto/actions/workflows/build.yml/badge.svg)

---

## Probarlo ya

**Jugadores:**
1. Instalar [Forge 1.16.5-36.2.39](https://files.minecraftforge.net) ("Installer").
2. Copiar a `%appdata%\.minecraft\mods` los dos `.jar` de la carpeta [`entrega/`](entrega/): `trucoya-1.7.3.jar` y `geckolib-forge-1.16.5-3.0.106.jar`.
3. Abrir Minecraft con el perfil de Forge.
4. Poner una **Mesa de Truco** y una **Silla Criolla**, sentarse y apretar **J**.

**Desarrollo (Windows), con doble clic:**

| Script | Qué hace |
|---|---|
| `probar.bat` | Baja el JDK 8, corre las pruebas y compila. |
| `jugar.bat` | Abre Minecraft con el mod. |
| `armar_jar.bat` | Deja el `.jar` en `entrega/`. |
| `servidor.bat` | Arma un servidor dedicado (ver [SERVIDOR.md](SERVIDOR.md)). |

**Con Gradle:**

```
gradlew coreTests    # 33 pruebas del motor de truco
gradlew runClient    # abre Minecraft con el mod
gradlew build        # genera build/libs/trucoya-1.7.3.jar
```

## Documentación del proyecto

| # | Documento | Contenido |
|---|---|---|
| 1 | [Situación problema](docs/01-situacion-problema.md) | Contexto, problema, actores, alcance, alternativas descartadas y justificación. |
| 2 | [Requisitos](docs/02-requisitos.md) | 31 requisitos funcionales numerados; no funcionales de rendimiento, seguridad, usabilidad y mantenibilidad, con criterios medibles. |
| 3 | [Stack tecnológico](docs/03-stack-tecnologico.md) | Cada tecnología con su versión, su justificación y dónde se usa en el código. |
| 4 | [Diagramas UML](docs/04-uml.md) | 4 diagramas de clases, uno de secuencia y dos de estados (fuentes PlantUML en [`docs/uml/`](docs/uml/)). |
| 5 | [Casos de uso](docs/05-casos-de-uso.md) | Diagrama y 15 descripciones (precondición, flujo principal, alternativos, postcondición). |
| 6 | [Infraestructura](docs/06-infraestructura.md) | Diagrama de despliegue, protocolos, puertos y mensajes de red. |
| 7 | [Pruebas](docs/07-pruebas.md) | Pruebas automáticas y guía de prueba en vivo, paso a paso. |
| 8 | [Calidad del código](docs/08-calidad.md) | Análisis estático (PMD), resultados y buenas prácticas. |
| — | [Manual completo](docs/MANUAL.md) | Todas las funciones del juego, controles, reglas y herramientas. |
| — | [Servidor](SERVIDOR.md) | Cómo armar un servidor y conectarse. |
| — | [Informe en PDF](docs/Informe_TrucoYa.pdf) | Toda la documentación en un solo archivo. |

## Arquitectura en una línea

```
cliente (pantallas, render)  ⇄  canal trucoya:main (Forge SimpleChannel, TCP 25565)  ⇄  servidor (autoridad)
                                                                                         ├─ core: motor del truco (Java puro, probado)
                                                                                         ├─ mesa, paisanos, historia, pedidos, barajas
                                                                                         └─ NBT: progreso guardado con el mundo
```

## Estructura del repositorio

| Ruta | Contenido |
|---|---|
| `src/main/java/com/trucoya/core` | Motor del truco: reglas, IA y vistas (sin Minecraft). |
| `src/main/java/com/trucoya/{block,tileentity,entity,item}` | Contenido del juego. |
| `src/main/java/com/trucoya/{progreso,barajas,trailer}` | Historia, pedidos, desafíos, barajas, Salamanca y modo tráiler. |
| `src/main/java/com/trucoya/{network,client}` | Red y pantallas. |
| `src/main/resources` | Texturas, modelos, sonidos, estructuras, idiomas y logros. |
| `src/test/java` | Pruebas del motor. |
| `tools/` | Generadores (Python) de texturas, estructuras, voces y música. |
| `grabaciones/` | Las voces reales de los cuatro Fundadores. |
| `docs/` | Documentación y diagramas. |
| `config/pmd` | Reglas del análisis estático. |
| `entrega/` | El `.jar` listo para instalar. |

## Licencia

[MIT](LICENSE). Minecraft es una marca de Mojang Studios; este proyecto no está afiliado a Mojang ni a
Microsoft.
