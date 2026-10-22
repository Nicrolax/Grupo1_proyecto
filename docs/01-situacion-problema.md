# 1. Desarrollo teórico de la situación problema

**Proyecto:** Truco Ya, el truco uruguayo dentro de Minecraft
**Curso:** Programación Avanzada (UTU)
**Grupo 1:** Nicolás Bentancour, Agustín Perdomo, Matías Piñeyro y Thiago Rodríguez

---

## 1.1 Contexto

El truco es el juego de cartas más jugado del Uruguay. Se juega en familia, en el liceo, en los clubes y en
las ruedas de mate. La versión uruguaya tiene reglas propias que la distinguen de la argentina:

- la **muestra**;
- las **piezas** (el 2, 4, 5, 10 y 11 del palo de la muestra);
- la **flor** con contraflor y "con flor me achico";
- el partido a 30 o 40 puntos, partido en **malas y buenas**.

Esas reglas se aprenden jugando con alguien que ya sabe, y cada vez menos jóvenes las aprenden.

Al mismo tiempo, los adolescentes pasan muchas horas en Minecraft, uno de los juegos más jugados del mundo.
Minecraft no tiene ningún juego de cartas. Los mods de cartas que existen copian juegos genéricos (póker,
blackjack) o la versión española del truco, y no respetan las reglas uruguayas.

## 1.2 El problema

> **No existe una forma de jugar al truco uruguayo, con sus reglas reales, dentro del mundo de Minecraft:
> ni contra la computadora ni con amigos en un servidor. Tampoco hay una que enseñe el juego y su
> cultura (el mate, la pulpería, el gaucho) a quien no lo conoce.**

Consecuencias:

- Quien quiere jugar al truco con sus amigos en Minecraft no puede. Tiene que salir del juego y usar otra aplicación, que tampoco suele tener las reglas uruguayas.
- Quien no sabe jugar no tiene dónde practicar a su ritmo, con un rival que respete las reglas y le muestre qué puede cantar en cada momento.
- La cultura rural uruguaya (pulperías, gauchos, payadas, mate) casi no aparece en los videojuegos que juegan los adolescentes.

## 1.3 Actores

| Actor | Quién es | Qué necesita del sistema |
|---|---|---|
| **Jugador** | Persona que juega Minecraft Java 1.16.5 con el mod instalado, sola o en un servidor. | Jugar partidos completos de truco uruguayo, contra paisanos (NPC) o contra otros jugadores, con las reglas correctas y una interfaz clara. |
| **Grupo de amigos (jugadores en red)** | Dos a cuatro jugadores conectados al mismo servidor. | Sentarse a la misma mesa (1 contra 1 o 2 contra 2), ver solo sus propias cartas, hacerse señas con el compañero. |
| **Administrador del servidor** | Quien levanta un servidor dedicado para su grupo. | Instalar el mod fácilmente y configurar qué puede afectar a todos (hora, clima, pedidos). Que nadie pueda hacer trampa desde su cliente. |
| **Paisano (NPC, actor del sistema)** | Personajes controlados por la inteligencia artificial del mod. | Jugar con distintos niveles de habilidad, aceptar invitaciones, ofrecer pedidos. |
| **Docente / evaluador** | Quien prueba el producto en vivo. | Instalar, ejecutar y verificar cada requisito en poco tiempo, con una guía de prueba. |

## 1.4 Alcance

**Dentro del alcance**

1. Un motor completo del truco uruguayo:
   - muestra y piezas;
   - envido, real envido y falta envido;
   - flor, contraflor, contraflor al resto y con flor me achico;
   - truco, retruco y vale cuatro;
   - me voy al mazo, pardas, malas y buenas, partidos a 30 o 40;
   - 1 contra 1 y 2 contra 2.
2. Objetos del juego para jugar en el mundo: la Mesa de Truco, las Sillas Criollas y un Anotador. Además, un tapete (interfaz gráfica) que muestra solo las acciones legales.
3. Paisanos con inteligencia artificial de cinco niveles, que leen la mesa y "mienten" (farolean).
4. Progresión y modo historia:
   - Prestigio Criollo;
   - seis regiones con pulperías generadas en el mundo;
   - torneos, sellos y la Estancia del Caudillo;
   - el Viejo Vizcacha, cinemáticas y el capítulo de La Salamanca.
5. Contenido de la cultura local:
   - mate, pava y yerba;
   - picada y bebidas;
   - discos de música criolla;
   - carreras cuadreras;
   - armas gauchas.
6. Barajas coleccionables, pedidos de los paisanos, desafíos del día y estadísticas en la Libreta.
7. Juego en red sobre un servidor dedicado, con el servidor como única autoridad.
8. Modo tráiler, para grabar un video promocional.

**Fuera del alcance**

- **Apuestas con dinero real** o cualquier moneda fuera del juego. Las apuestas usan solo esmeraldas de Minecraft.
- **Otras variantes del truco**: argentino sin flor, truco español o gallo. El mod implementa solo el uruguayo, con la flor desactivable.
- **Otras versiones de Minecraft**: Bedrock (consolas y celulares) y Java distinta de 1.16.5.
- **Rankings globales en internet, cuentas propias o base de datos externa.** Los datos quedan en cada mundo y en cada servidor.
- **Moderación de chat o anti-trampas** más allá de validar en el servidor cada jugada.

## 1.5 Alternativas consideradas

| Alternativa | Ventajas | Por qué se descartó |
|---|---|---|
| **A. Aplicación web o móvil de truco** (por ejemplo, React y Node.js) | Fácil de distribuir; se juega en el celular. | No resuelve el problema planteado: el público ya está dentro de Minecraft y tendría que salir del juego. Además, ya existen apps de truco; la novedad de este proyecto es llevarlo al mundo donde están los jugadores. |
| **B. Plugin de servidor (Spigot/Paper)** | No requiere instalar nada en el cliente. | Un plugin no puede agregar modelos 3D, texturas propias, animaciones, sonidos ni pantallas nuevas sin "trucos" con ítems de vanilla. No permitiría la mesa con cartas visibles, los gauchos animados ni el tapete. |
| **C. Datapack de Minecraft** | Sin programar en Java. | La lógica del truco (estados de la mano, envido, flor, IA con simulación) es inviable con comandos. Tampoco permite interfaces nuevas. |
| **D. Mod para Fabric** | Más liviano y actualizado. | En 1.16.5, Forge trae de fábrica lo que el proyecto necesita: canal de red entre cliente y servidor, configuración de servidor, generación de estructuras, eventos y registro de contenido. Con Fabric habría que sumar varias librerías aparte. Además, es el cargador con más mods compatibles en esa versión. |
| **E. Minecraft más nuevo (1.20+)** | Más funciones del motor. | 1.16.5 sigue siendo de las versiones con más mods disponibles, corre en computadoras modestas y usa Java 8, que viene con el entorno de desarrollo del proyecto (`scripts\probar.bat` lo descarga solo). |
| **F. Mod para Forge 1.16.5 (elegida)** | Contenido propio completo (bloques, entidades, pantallas, sonidos, estructuras), cliente y servidor con la misma base de código, y servidor dedicado para jugar en grupo. | — |

## 1.6 Por qué la solución elegida resuelve el problema

- **Lleva el juego a donde ya está el público:** se juega al truco sin salir de Minecraft, solo o con amigos.
- **Respeta las reglas uruguayas:** un motor propio, separado de Minecraft (`com.trucoya.core`), implementa las reglas y se prueba de forma automática (33 pruebas).
- **Enseña jugando:** el tapete muestra solo las jugadas legales, el historial explica cada mano y los paisanos de nivel bajo perdonan errores. El modo historia lleva de a poco de las primeras manos al Viejo Vizcacha.
- **Transmite la cultura:** pulperías por región, mate, payadas, la leyenda de la Salamanca y el Viejo Vizcacha de *Martín Fierro*.
- **Es seguro para jugar en grupo:** el servidor es la única autoridad. Cada cliente recibe solo su propia mano, y toda jugada se valida en el servidor.
