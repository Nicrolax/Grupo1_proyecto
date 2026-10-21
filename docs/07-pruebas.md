# 7. Pruebas

## 7.1 Pruebas automáticas del motor

```
gradlew coreTests                 (con Gradle)
```

Sin Gradle, solo con el JDK:

```
javac -encoding UTF-8 -d out src/main/java/com/trucoya/core/*.java src/test/java/com/trucoya/core/*.java
java -cp out com.trucoya.core.TrucoCoreTests
java -cp out com.trucoya.core.MedicionIA      (tiempo de decisión de la IA, RNF-R4)
```

Las 33 pruebas cubren:
- mazo de 40;
- piezas, jerarquía de cartas y muestra;
- cálculo de envido y flor;
- truco, retruco y vale cuatro, querido y no querido;
- cadenas de envido y falta envido;
- contraflor, al resto y "con flor me achico";
- pardas y me voy al mazo;
- malas, buenas y fin de partido;
- turnos en 2 contra 2;
- **rechazo de jugadas ilegales** y **vista privada** (RNF-S1, RNF-S2);
- serialización de red;
- partidos completos entre bots sin jugadas ilegales;
- los niveles 1 a 6 de la IA;
- torneo, prestigio y el mazo armado de la Salamanca.

Se corren solas en GitHub Actions en cada push ([`.github/workflows/build.yml`](../.github/workflows/build.yml)).
Resultado actual: **33 de 33 correctas**.

## 7.2 Guía de prueba en vivo (para el docente)

**Preparación:**
1. Instalar Forge 1.16.5-36.2.39 y copiar a `.minecraft\mods` los dos `.jar` de la carpeta `entrega/`. En la PC de desarrollo, `jugar.bat` hace lo mismo.
2. Crear un mundo nuevo en **Creativo, con trucos activados**. Así se pueden usar los comandos de operador para ir rápido.

| # | Prueba | Pasos | Resultado esperado | RF |
|---|---|---|---|---|
| 1 | Armar la mesa | Buscar en el inventario creativo la pestaña «Truco Ya». Poner la Mesa de Truco y dos Sillas Criollas pegadas, y clic derecho en una silla. | El jugador queda sentado mirando a la mesa. | RF-01 |
| 2 | Empezar | Tecla J, 1 contra 1, a 30, con flor, «Empezar». | Se reparten tres cartas y aparece la muestra. Las piezas se destacan y se sienta un paisano. | RF-02, RF-03, RF-10 |
| 3 | Solo lo legal | Mirar los botones en cada momento. | Solo aparecen los botones válidos. «Envido» desaparece después de la primera baza y cada botón muestra su atajo. | RF-04 |
| 4 | Envido | Cantar «Envido» (E). | El paisano responde. Si quiere, se cantan los tantos desde la mano (el que no gana dice «son buenas»); el ganador queda en dorado y los puntos en el anotador. | RF-05 |
| 5 | Truco | Cantar «Truco» (T) y jugar la mano. | El paisano quiere, no quiere o sube. Los puntos se anotan según el nivel. | RF-07, RF-08 |
| 6 | Mesa en el mundo | Tecla V (vista compacta / clásica) o salir del tapete y mirar la mesa. | Las cartas jugadas están apoyadas frente a cada silla y la muestra sobre el mazo. | RF-13 |
| 7 | Partido completo | Jugar hasta 30, o 20 en un torneo. | Malas y buenas se marcan con fósforos y el partido termina; sube el prestigio (`/truco prestigio`). | RF-08, RF-15 |
| 8 | Flor | Jugar hasta que toque una flor (o ver la prueba automática «Contraflor, al resto y achique»). | Aparece «Flor» y el envido queda bloqueado. | RF-06, RF-09 |
| 9 | 2 contra 2 y señas | Cuatro sillas, 2 contra 2, tecla G. | Los cuatro asientos se completan; la seña le llega al compañero. | RF-12 |
| 10 | Invitar | Dar con `/truco gaucho paisano` y hacer clic derecho al paisano (agachado) cerca de la mesa. | El paisano camina hasta la mesa y se sienta. | RF-11 |
| 11 | Apuesta | Con esmeraldas en la mano, sentado, «Apostar». Después jugar. | El pozo aparece en el tapete y lo cobra el ganador. | RF-14 |
| 12 | Libreta | Clic derecho con la Libreta; recorrer las cuatro pestañas. | Camino, Barajas, Pedidos (con los desafíos del día) y Números. | RF-17, RF-25 |
| 13 | Historia rápida | `/truco completar`, luego `/locate trucoya:estancia` y viajar hasta ahí. | Llegan los seis sellos y el Mapa del Baqueano. En la Estancia, la cinemática del Vizcacha. | RF-16, RF-18, RF-19 |
| 14 | Final | `/truco cinematica final`. | La película final con las **voces reales** de Nicolás, Agustín, Matías y Thiago. | RF-19 |
| 15 | Salamanca | `/truco salamanca visita`. | Llega el Vizcacha a caballo, la cabalgata con tormenta, el Paso de las Ánimas y la partida 2v2. | RF-20 |
| 16 | Mate | Pava al fuego, mate con yerba en la otra mano, cebar; clic derecho a un paisano con el mate cebado. | El paisano agradece y aparece el efecto Confraternidad. | RF-21 |
| 17 | Barajas | Pestaña Barajas, tocar otra baraja (con `/truco completar todo` ya están casi todas) y «Jugar con esta». Empezar un partido. | Las cartas del tapete y de la mesa cambian de diseño. | RF-23 |
| 18 | Pedidos | Buscar un paisano con «¡Pedido!» (o esperar un día), clic derecho dos veces. Cumplirlo. | El pedido aparece en la Libreta; al cumplirlo da esmeraldas y no se puede aceptar de nuevo. | RF-24 |
| 19 | Logros | Tecla L. | Aparece el árbol «Truco Ya» con los logros ganados. | RF-26 |
| 20 | Tráiler | Dos veces `/truco trailer marcar` en lugares distintos, después `/truco trailer 5`. | La cámara recorre las marcas con carteles y música, y el jugador vuelve a su lugar. | RF-29 |
| 21 | Servidor | `servidor.bat`, `servidor\iniciar.bat`, entrar con dos clientes y sentarse a la misma mesa. | Cada uno ve solo sus cartas y la jugada de uno le llega al otro. | RF-27 |
| 22 | Permisos | Desde un cliente sin operador: `/truco completar`. | El comando no existe para ese jugador. | RF-30, RNF-S4 |
| 23 | Configuración | Poner `cambiaElCielo = "NUNCA"` en `trucoya-server.toml`, reiniciar y hacer `/truco salamanca visita`. | La historia sigue, pero no cambian la hora ni el clima. | RF-28 |
| 24 | Idioma | Opciones → Idioma → English. | Los ítems tienen nombre en inglés. | RF-31 |

## 7.3 Pruebas de seguridad y robustez

| Prueba | Cómo | Esperado |
|---|---|---|
| Jugada ilegal | Prueba automática «Rechazo de jugadas ilegales». | `ActionResult.fail`; el estado no cambia. |
| Mano ajena | Prueba automática «Vista privada del jugador». | La `PlayerView` de un asiento no trae cartas de otro. |
| Acción a distancia | Alejarse más de 8 bloques e intentar retirar una apuesta. | No pasa nada. |
| Desconexión en la Salamanca | Cerrar el juego en medio de la partida y volver a entrar. | La Salamanca vuelve a ser tapera; el capítulo se puede retomar. |
| Pedido repetido | Cumplir un pedido y volver a hacerle clic al paisano, o apretar de nuevo [Aceptar] en el chat. | El paisano no lo vuelve a ofrecer; [Aceptar] responde «Ese pedido ya lo tenés». |
