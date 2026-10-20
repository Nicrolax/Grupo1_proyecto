# 2. Requisitos

Cada requisito tiene un identificador, una redacción en términos de lo que hace el sistema y la forma de
verificarlo. La guía de prueba en vivo ([07-pruebas.md](07-pruebas.md)) recorre todos los requisitos
funcionales en orden.

## 2.1 Requisitos funcionales

### Mesa y partido de truco (núcleo)

| ID | Requisito | Verificación |
|---|---|---|
| RF-01 | El sistema permite colocar una **Mesa de Truco** y hasta cuatro **Sillas Criollas** pegadas a ella. Con clic derecho en una silla, el jugador queda sentado mirando a la mesa. | Colocar mesa y silla; clic derecho en la silla. |
| RF-02 | El jugador sentado abre el **tapete** (clic en la mesa o tecla J) y elige 1 contra 1 o 2 contra 2, partido a 30 o 40 y con o sin flor. Después empieza el partido. | Configurar y apretar «Empezar». |
| RF-03 | Al empezar cada mano, el sistema reparte 3 cartas a cada jugador de un mazo español de 40, da vuelta la **muestra** y marca las **piezas** (2, 4, 5, 10 y 11 del palo de la muestra). | Ver la mano y la muestra; las piezas se resaltan. |
| RF-04 | El tapete muestra solo las acciones **legales** en ese momento. Cada una tiene su atajo de teclado (1 a 3, T, E, R, F, L, C, A, X, Q, N y M). | En cada estado aparecen solo los botones válidos. |
| RF-05 | El sistema permite cantar **envido, real envido y falta envido** (y sus combinaciones), en la primera baza. Calcula los tantos según las reglas uruguayas, con el valor de las piezas. Los tantos se cantan en voz alta empezando por la mano y se anotan. | Cantar envido y comparar los tantos mostrados con el cálculo manual. |
| RF-06 | El sistema permite cantar **flor**, **contraflor**, **contraflor al resto** y **con flor me achico**. «Con flor me achico» suma 3 puntos. | Con una mano de flor, cantar y ver los puntos. |
| RF-07 | El sistema permite cantar **truco, retruco y vale cuatro**, con quiero y no quiero, e **irse al mazo**. En 2 contra 2, si uno se va al mazo se va todo el equipo. | Cantar truco, subirlo, irse al mazo en 2v2. |
| RF-08 | El sistema resuelve cada baza y las **pardas**, decide quién gana la mano y anota los puntos en **malas y buenas** (15/15 o 20/20). El partido termina al llegar al objetivo. | Jugar un partido completo; ver el anotador. |
| RF-09 | La **falta envido** y la **contraflor al resto** valen el partido entero. Si un equipo está en malas y el otro en buenas, valen lo que le falta al que va ganando. | Probar con `/truco` en un partido avanzado o con las pruebas automáticas. |
| RF-10 | Los asientos libres se completan con **paisanos** (NPC) que juegan solos, con cinco niveles de habilidad. El Viejo Vizcacha juega con un sexto nivel. | Empezar con un solo humano. |
| RF-11 | Con clic derecho, el jugador invita a un paisano a jugar. El paisano camina hasta la mesa más cercana (24 bloques como máximo) y se sienta. | Clic derecho (agachado, si tiene un pedido) a un paisano cerca de una mesa. |
| RF-12 | En 2 contra 2, el jugador le hace **señas** a su compañero (tecla G). Si un rival lo ve, se entera. | Partido 2v2 con un compañero; apretar G. |
| RF-13 | Las cartas jugadas, la muestra y los dorsos de las manos se ven **sobre la mesa en el mundo**, para todos los jugadores cercanos. | Mirar la mesa desde afuera con otro jugador. |
| RF-14 | Antes de empezar, los jugadores pueden **apostar** lo que tienen en la mano (esmeraldas, por ejemplo) y retirarlo. En la Guarida Clandestina solo se apuesta oro, esmeraldas, diamantes o netherite. Al terminar, el equipo ganador se lleva el pozo. | Apostar, jugar, cobrar. |

### Progresión y modo historia

| ID | Requisito | Verificación |
|---|---|---|
| RF-15 | Cada partido suma o resta **Prestigio Criollo**. El prestigio define cinco niveles con su rango. `/truco prestigio` muestra el prestigio y las estadísticas. | Ganar y perder; usar el comando. |
| RF-16 | El mundo genera **pulperías** por bioma (y junto a las aldeas), la **Guarida Clandestina**, una sola **Estancia del Caudillo** y el **Paso de las Ánimas**. | Explorar, o usar `/locate trucoya:pulperia_llanura`. |
| RF-17 | La **Libreta del Truquero** tiene cuatro pestañas: capítulos y sellos, barajas, pedidos y desafíos del día, y estadísticas. | Clic derecho con la Libreta. |
| RF-18 | Ganar en la pulpería de cada una de las seis regiones da su **sello**. Con los seis sellos y el nivel 4 llega el **Mapa del Baqueano**, que marca la Estancia. Cada pulpería tiene un **torneo** de 3 rondas. | `/truco completar` acelera el camino. |
| RF-19 | En la Estancia espera el **Viejo Vizcacha**, con una cinemática al conocerlo. Ganarle da la Baraja de Oro y la **cinemática final**, con las voces de los cuatro Fundadores. | `/truco cinematica` y `/truco cinematica final`. |
| RF-20 | El capítulo **La Salamanca**: el Vizcacha viene a buscar al jugador, cabalgan bajo la tormenta hasta el Paso de las Ánimas y juegan un 2 contra 2 contra el Mandinga y el Negro Ledesma. Al amanecer el alma de Ledesma queda libre. | `/truco salamanca visita` (operador). |
| RF-21 | **Mate y picada:** se cultiva yerba, se calienta la pava, se ceba y se convida mate (efecto Confraternidad). Hay picada, bebidas y cuatro discos de música criolla. | Ver la Guía de prueba, sección Fase 5. |
| RF-22 | **Carreras cuadreras** contra paisanos a caballo, entre dos Rayas Cuadreras. | Colocar las rayas y desafiar. |
| RF-23 | Hay **diez barajas coleccionables**. Se ganan con hazañas o se compran al pulpero; se elige con cuál jugar, y la ve toda la mesa. | Pestaña Barajas de la Libreta. |
| RF-24 | **Pedidos de los paisanos:** traer cosas, llevar una encomienda o cumplir un desafío. Hay hasta 3 a la vez, con premio en esmeraldas y prestigio, y un pedido cumplido no se repite. | Paisano con «¡Pedido!»; `/truco pedidos`. |
| RF-25 | **Desafíos del día:** tres por jugador y por día, con premio y racha de días seguidos. | Pestaña Pedidos de la Libreta. |
| RF-26 | Hay un árbol de **logros** (advancements) propio. | Menú de Progresos (L). |

### Juego en red, administración y herramientas

| ID | Requisito | Verificación |
|---|---|---|
| RF-27 | El mod funciona en un **servidor dedicado**. El progreso de cada jugador (prestigio, sellos, barajas, pedidos) queda guardado con su personaje y sobrevive a la muerte y al reinicio. | `servidor.bat`; entrar con dos clientes. |
| RF-28 | El administrador configura en `serverconfig/trucoya-server.toml` si la Salamanca puede cambiar la hora y el clima, si hay pedidos y la espera de `/truco cinematica`. | Editar el archivo y reiniciar. |
| RF-29 | **Modo tráiler:** el jugador marca lugares y la cámara de cine los recorre con carteles y música. | `/truco trailer marcar`, luego `/truco trailer`. |
| RF-30 | **Comandos de prueba para operadores:** `/truco completar [todo]`, `/truco gaucho <rol> [nivel]`, `/truco salamanca …`, `/truco prestigio set …`. | Probarlos como operador y como no operador. |
| RF-31 | Todos los textos están en **español rioplatense** (es_uy, es_ar, es_es y es_mx). Los nombres de ítems están además en inglés (en_us). | Cambiar el idioma del juego. |

## 2.2 Requisitos no funcionales

### Rendimiento

| ID | Requisito | Criterio medible | Cómo se mide |
|---|---|---|---|
| RNF-R1 | La lógica de las mesas no afecta la fluidez del servidor. | Con 2 mesas en juego y 8 paisanos, el servidor se mantiene en **20 TPS** (≥ 19,5). | `/forge tps` durante un partido. |
| RNF-R2 | Los paisanos responden con un ritmo natural, sin trabar la partida. | Cada decisión de un paisano llega entre **1,0 y 2,25 s** después de su turno (20 a 44 ticks). | Constantes `BOT_MIN_DELAY` y `BOT_EXTRA_DELAY` de `MesaTrucoTileEntity`; se ve al jugar. |
| RNF-R3 | El tráfico de red por mesa es acotado. | Cada vista de mesa ocupa **≤ 16 KB** (`TableViewPacket.MAX_BYTES`). Los globos de diálogo y los avisos ocupan ≤ 1 KB, y los textos se recortan a 200 caracteres. | Límites en el código: lo que se pasa del límite se rechaza al decodificar. |
| RNF-R4 | La inteligencia artificial decide rápido aun en su nivel más alto. | En el nivel 5 (con simulación de Monte Carlo), cada decisión tarda **< 10 ms en promedio y < 250 ms como máximo**, menos que la pausa de 1 s que hace el paisano antes de jugar. Medido en 30 partidos 2v2 entre bots de nivel 5: 3526 decisiones, **3,2 ms** de promedio y **97 a 135 ms** de máximo, según la corrida (el máximo es la primera decisión, antes de que la JVM optimice el código). | `java com.trucoya.core.MedicionIA` (en `src/test`). |

### Seguridad

| ID | Requisito | Criterio medible | Cómo se verifica |
|---|---|---|---|
| RNF-S1 | El servidor es la **única autoridad** del partido. | El cliente solo manda intenciones («jugar la carta X», «cantar truco»). El servidor valida asiento, turno y legalidad (`TrucoMatch.isLegal`) y rechaza el **100 %** de las jugadas ilegales. | `handleAction` y `handleCard` en `MesaTrucoTileEntity`; pruebas automáticas de jugadas ilegales. |
| RNF-S2 | **Nadie ve las cartas de otro.** | Cada cliente recibe solo su `PlayerView`, que trae su propia mano. La vista pública de la mesa solo trae cartas ya jugadas y cuántas le quedan a cada uno. Ningún paquete lleva manos ajenas. | Lectura de `PlayerView` / `vistaTag()`. |
| RNF-S3 | Solo se actúa sobre una mesa estando cerca. | Las acciones de mesa se rechazan a más de **8 bloques**. | `inReach()`; intentar desde lejos. |
| RNF-S4 | Los comandos que alteran el progreso son solo para operadores. | `completar`, `gaucho`, `guardia`, `salamanca`, `sello`, `prestigio set` y `trailer` exigen **permiso nivel 2**. `trailer` también lo puede usar el dueño de un mundo de un jugador. | Probarlos sin ser operador: responden «comando desconocido». |
| RNF-S5 | Los paquetes no se pueden usar para trabar o tumbar el servidor. | Cada paquete limita tamaños y cantidades al decodificar: listas ≤ 8, textos ≤ 1024, índices validados. | Revisión de `decode()` de cada paquete. |
| RNF-S6 | No hay credenciales ni conexiones externas. | El mod no hace ninguna conexión a internet mientras se juega. El repositorio no contiene claves ni tokens. | Búsqueda en el código; análisis estático ([08-calidad.md](08-calidad.md)). |
| RNF-S7 | No se pueden explotar las recompensas. | Un pedido cumplido o cancelado no se acepta de nuevo, cada desafío diario paga **una vez**, y las barajas de hazaña no se transfieren. | Pruebas de la guía, sección Pedidos. |

### Usabilidad

| ID | Requisito | Criterio medible |
|---|---|---|
| RNF-U1 | Empezar a jugar es inmediato. | Un jugador nuevo arranca su primer partido en **3 pasos**: poner mesa y silla, sentarse y apretar «Empezar». |
| RNF-U2 | La interfaz no confunde. | El tapete muestra **solo** botones de acciones legales, y el **100 %** tiene atajo de teclado. |
| RNF-U3 | La mesa se sigue viendo mientras se juega. | En la vista compacta, la mano y los botones ocupan **≤ 30 %** de la altura de la pantalla, en la franja inferior; el resto queda libre para ver la mesa. |
| RNF-U4 | El juego habla como el jugador. | El **100 %** de los textos del juego está en español rioplatense. |
| RNF-U5 | El jugador siempre sabe qué hacer después. | La Libreta muestra el próximo paso del modo historia en **toda** etapa. |

### Compatibilidad, mantenibilidad y fiabilidad

| ID | Requisito | Criterio medible |
|---|---|---|
| RNF-C1 | Plataforma | Minecraft Java **1.16.5**, Forge **36.2.39**, GeckoLib **3.0.106** y Java **8**. Probado en Windows 10/11. |
| RNF-M1 | Motor separado | El paquete `com.trucoya.core` no importa ninguna clase de Minecraft. Se compila y se prueba solo con el JDK. |
| RNF-M2 | Pruebas automáticas | **33 pruebas** del motor (`gradlew coreTests`), en la integración continua de GitHub Actions en cada push. |
| RNF-F1 | Tolerancia a fallos | Si una cinemática o un efecto falla, se corta solo esa escena, el juego sigue y el error queda en el log. Una partida o escena cortada (desconexión, cierre) deja el mundo como estaba: la Salamanca vuelve a ser tapera y el jugador vuelve a su lugar. |
| RNF-F2 | Persistencia | Todo el progreso se guarda en formato NBT con el mundo. Al cerrar y abrir el juego, el progreso es el mismo. |
