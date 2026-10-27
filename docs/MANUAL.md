# Manual completo de Truco Ya

Estado: las 5 fases programadas: motor del Truco Uruguayo, mobiliario y tapete, gauchos con IA y
Prestigio Criollo, estructuras en el mundo, y la Fase 5 (mate, picada, bebidas, discos y árbol de logros).

## Requisitos

- JDK 8 (recomendado: Eclipse Temurin 8). Minecraft 1.16.5 corre sobre Java 8.
- Forge 1.16.5-36.2.39, ForgeGradle 5.1, Gradle 7.6.4 (incluido vía wrapper).
- GeckoLib 3 para Forge 1.16.5 (`geckolib_version` en `gradle.properties`; para los NPC animados de la Fase 3).

## Comandos

```
gradlew genIntellijRuns    (o genEclipseRuns)  # preparar el IDE
gradlew coreTests                              # pruebas del motor de Truco
gradlew runClient                              # abrir Minecraft con el mod
gradlew build                                  # genera build/libs/trucoya-<versión>.jar
```

Sin Gradle, las pruebas del motor también corren con el JDK solo:

```
javac --release 8 -encoding UTF-8 -d out src/main/java/com/trucoya/core/*.java src/test/java/com/trucoya/core/*.java
java -cp out com.trucoya.core.TrucoCoreTests
```

## Cómo se juega (Fase 2)

1. Fabricá una **Mesa de Truco** y hasta cuatro **Sillas Criollas**; poné las sillas pegadas a los
   lados de la mesa (se orientan solas hacia ella). Opcional: un **Anotador con Porotos** al lado de la mesa
   (en el piso o arriba de un mueble, a dos bloques como mucho; encima de la mesa no, porque tapa las cartas). Las recetas aparecen en el libro de recetas.
2. Clic derecho en una silla para sentarte: se abre el tapete. (Shift o la tecla de bajarse para levantarte.)
3. Elegí modalidad (1 contra 1 o 2 contra 2), 30 o 40 puntos y con o sin flor, y tocá **Repartir**.
   Los lugares vacíos los ocupan paisanos automáticos (bot básico).
4. En 2v2 los compañeros quedan enfrentados (sillas opuestas).
5. Si cerrás el tapete seguís sentado: lo reabrís con clic derecho en la mesa o con la tecla **J**.

| Recetas | |
|---|---|
| Silla Criolla | 3 tablas + 2 palos |
| Mesa de Truco | 4 tablas + 2 cueros + 1 lana |
| Anotador con Porotos | losa de madera + polvo de hueso + carbón o carbón vegetal (sin forma) |

### Controles en el tapete

| Tecla | Acción | Tecla | Acción |
|---|---|---|---|
| 1 · 2 · 3 | Jugar carta (o clic) | L | Flor |
| T | Truco / Retruco / Vale cuatro | C | Contraflor |
| E | Envido | A | Contraflor al resto |
| R | Real envido | X | Con flor me achico |
| F | Falta envido | Q / N | Quiero / No quiero |
| M | Me voy al mazo | G | Señas (2v2) |
| H | Historial de la mano | V | Vista compacta / clásica |

El tapete está pensado para verse limpio: solo aparecen los botones de lo que se puede hacer en ese
momento (el atajo se ve al pasar el mouse), el tanteador es un anotador con fósforos (cuadrados de
5: malas | buenas) y el historial queda escondido hasta que apretás H.

**Tantos a la vista:** cuando se quiere el envido (o se disputa la flor), los tantos se cantan
empezando por la mano, como en la mesa de verdad: el que no supera al que va ganando dice «son buenas»
(sin mostrar su número) y el compañero del que va ganando no habla. Lo dicho aparece al lado de cada
uno (dorado el que gana) y queda hasta que se reparte de nuevo. También se anota en el historial.

**Vista compacta (1.6.0):** el tapete ya no tapa la pantalla. Se ve la mesa de verdad y, abajo, una franja con
lo tuyo: tantos y muestra a la izquierda, tu mano en el medio y los botones a la derecha (solo los que se pueden
usar). La cámara apunta sola a la mesa y se esconde la barra de ítems. Con **V** se vuelve a la vista clásica.

**Las cartas en la mesa:** las cartas jugadas se ven apoyadas sobre la Mesa de Truco, en el mundo, para todos los
que estén cerca:
- cada una frente a la silla de quien la tiró, con las tres bazas en fila;
- la muestra cruzada sobre el mazo, al lado del que reparte;
- los dorsos de las cartas que cada uno todavía tiene en la mano.

Solo viaja lo que está a la vista: nadie puede espiar la mano de otro.

Las cartas son de alta resolución (128 x 192 cada una): diseño propio con las pintas de la baraja española,
ases ornamentados y sota, caballo y rey dibujados. Hay diez barajas distintas (ver **Barajas coleccionables**).

Las teclas G y J se cambian en Opciones → Controles → Truco Ya.

### Qué hace cada pieza

- **Tapete (GUI):** mano privada con las piezas resaltadas en dorado, muestra, cartas jugadas por
  posición, dorsos de lo que le queda a cada uno, bazas ganadas/perdidas, tus puntos de envido y flor,
  registro de los últimos cantos y marcador lateral de Las Malas / Las Buenas.
- **Botonera:** los 13 botones siempre visibles; solo se habilitan los que son legales en ese momento.
- **Voces:** cada canto suena en la mesa (3 voces distintas al azar) y aparece un globo sobre el jugador.
- **Señas (G):** 12 gestos. El compañero recibe el aviso; cada seña tiene un 20 % de chance de que
  un rival la descubra.
- **Anotador:** porotos blancos para el equipo de quien tocó Repartir y negros para los rivales, en grupos de 5.

## Gauchos, prestigio y apuestas (Fase 3)

- **Gauchos:** ocho tipos (paisano, paisana, cantinero o pulpera, forastero a caballo, matrero, peón de
  estancia, payador con guitarra y el Viejo Vizcacha), 28 pilchas distintas en total, con IA de nivel 1 a 5.
- **El Viejo Vizcacha juega un escalón más arriba (IA nivel 6):** calcula cada baza con búsqueda
  completa sobre cientos de repartos posibles, usa los tantos que cantaste para deducir tus cartas,
  lee el 80 % de las señas que le hacés al compañero, interpreta tus respuestas (si quisiste el truco,
  algo tenés; si no quisiste el envido, poco) y juega con el tanteador (sabe cuándo un "no quiero" te
  regala el partido y cuándo le conviene arriesgar).
  Las texturas y la geometría se generan con `python3 tools/generar_gaucho.py`.
  Clic derecho en un gaucho (Shift + clic en el cantinero) para invitarlo a la mesa más cercana.
- **Prestigio Criollo:** de Aprendiz de Pulpería a Leyenda del Tapete (`/truco prestigio`).
- **Pozo de apuestas:** desde el tapete; en la mesa clandestina hace falta Nivel 3 o una Ficha de Clandestina.
- `/truco gaucho <rol> [nivel]` invoca un gaucho para probar.

## Armas gauchas

| Arma | Receta | Qué hace |
|---|---|---|
| **Facón** | lingote de hierro + palo en diagonal | Cuchillo criollo: 5 de daño, más rápido que una espada |
| **Facón Platero** | 2 lingotes de hierro + 1 de oro en diagonal | Mejor filo (6), dura el doble y se encanta fácil |
| **Rebenque** | 2 cueros + palo en diagonal | Pega poco pero empuja lejos. Montado a caballo, clic derecho: el caballo corre más rápido 10 s |
| **Boleadoras** (×2) | 3 piedras (cobblestone) + cuero | Se tiran con clic derecho: 3 de daño y el blanco queda enredado (sin moverse ni saltar) 4 s. Caen al piso y se juntan |

## Guardias de la Guarida

- La Guarida tiene **guardias armados con facón**: el Patovica al pie de la escalera y otros tres adentro.
- Para entrar hay que llevar una **Ficha de Clandestina** en el inventario (o tener Nivel 3 de Prestigio:
  a los Taitas los conocen). La ficha se compra al pulpero (24 esmeraldas) o aparece en cofres.
- Sin ficha: el guardia avisa ("¡Alto ahí, forastero!...") y si en 4 segundos no te vas, te ataca, y los
  otros guardias se suman. Si te alejás, te dejan ir. Si les pegás, se defienden aunque tengas ficha.
- A los guardias se los puede vencer: a veces sueltan su facón o una ficha.
- Para probar: `/truco guardia` hace aparecer un guardia donde estás parado.

## Estructuras (Fase 4)

### Pulperías: una por bioma, y una al lado de cada aldea

| Bioma | Pulpería |
|---|---|
| Llanura | Casa colonial blanqueada, tejas, galería, enramada con parrilla, aljibe, palenque y ombú |
| Sabana | Rancho de adobe con techo de paja, ramada de hojas, horno de barro y corral |
| Desierto | Casa de arenisca con azotea almenada, portal de arcos, cúpula, toldo a rayas, pozo y palmera |
| Mesa (badlands) | Igual que la del desierto, en arenisca roja con franjas de terracota |
| Taiga | Cabaña de troncos de abeto, techo empinado, chimenea de piedra, porche y leñera |
| Bosque | Cabaña de roble con techo de roble oscuro, hongos y helechos |
| Nevada | Cabaña de abeto con cumbrera nevada, nieve alrededor y abetos |
| Pantano | Palafito sobre pilotes con escalera, baranda, techo de paja y charcos con camalotes |
| Selva | Palafito de madera de jungla con techo de hojas y sandías |
| Montaña | Casa de pirca con techo de lajas, corral de piedra con llamas y una apacheta |

- Todas tienen pulpero detrás de la reja, una mesa 2 contra 2 y mesas 1 contra 1, y parroquianos.
- **Cada aldea tiene su pulpería** (con el estilo de su bioma), pegada a una de las casas de las orillas,
  mirando al pueblo y sin pisar ninguna construcción. Además hay pulperías sueltas en el campo.
- Ubicarlas: `/locate trucoya:pulperia_aldea` (la de la aldea más cercana) o `/locate trucoya:pulperia`.

### Guarida Clandestina

Bajo tierra en llanuras, sabanas, bosques, desiertos y taigas (`/locate trucoya:guarida`). Arriba, una tapera
en ruinas con un aljibe: la trampilla esconde la escalera. Abajo, un bar con barra, tablado del payador,
cuatro mesas clandestinas, reservado y guardias (ver "Guardias de la Guarida").

### Estancia del Caudillo: una sola en todo el mundo

Está en una llanura o sabana plana a unos 600-1200 bloques del origen (`/locate trucoya:estancia`). Hay una sola por mundo y siempre hay una: si para ese lado no hay praderas parejas, se busca en los otros rumbos y, en mundos casi sin praderas, en cualquier tierra firme y pareja. (Desde 1.4.2; en un mundo ya empezado aparece si esa zona todavía no se generó.)
Ocupa 64 x 64 bloques:

- Portón con arco y cartel, alambrado, alameda de eucaliptos y palenque con caballos ensillados.
- **Casco colonial de patio**: ala del frente de dos plantas con balcón corrido de hierro, zaguán con escalera,
  mirador con techo de tejas, salón del **Viejo Vizcacha** (hogar, biblioteca, sillones, tocadiscos),
  escritorio del patrón, dormitorio del caudillo y sala de armas (facón, rebenque, boleadoras), cuartos de
  huéspedes, costurero, despensa, cocina, comedor de la peonada con su pulpero, aperos y herrería.
  Patio con galerías de columnas, aljibe, naranjos y rosales.
- **Capilla** con espadaña de dos campanas, vitrales, bancos y altar.
- **Galpón** de chapa (fardos, herrería, esquila con ovejas) y **caballeriza** con boxes y caballos.
- **Tanque australiano** y **molino de viento**, **huerta** con trigo, zanahorias, papas y remolachas,
  espantapájaros, **gallinero**, **corral** grande con manga, vacas y ovejas, y el **fogón** de la peonada.

### Cómo se apoyan en el terreno

- El piso va sobre el punto más alto del terreno (la estancia, sobre la altura media) y debajo se rellena con
  tierra: no quedan enterradas ni flotando. Si el lugar tiene agua o es muy quebrado, se busca otro.
- Aparecen solo en **mundos nuevos** o en zonas sin explorar; no aparecen en mundos superplanos.
- Las plantillas se generan con `python3 tools/generar_estructuras.py` (requiere `pip install nbtlib`);
  el diseño de cada una está en `tools/estructuras_*.py`.

## Mate, picada, bebidas y discos (Fase 5)

### El mate

1. **Yerba:** comprale Semillas de Yerba Mate al pulpero (o sacalas de la huerta de la estancia), plantalas en
   tierra arada y cosechá las Hojas de Yerba cuando el Yerbal esté maduro. Sapecalas en un horno, ahumador o
   fogata: salen paquetes de **Yerba Mate**.
2. **Mate:** calabaza (zapallo) + **Bombilla** (2 pepitas de hierro y 1 de oro, en columna).
3. **Pava:** lingotes de hierro en forma de pava con una pepita de tapa. Clic derecho mirando agua para
   llenarla y clic derecho sobre una fogata, horno o fuego prendidos para calentarla (12 cebadas).
4. **Cargar:** mate en una mano y yerba en la otra, clic derecho (10 cebadas por carga).
5. **Cebar:** mate cargado en una mano y la pava caliente en la otra, clic derecho con cualquiera de los dos.
   Si algo falta, el mensaje dice qué (la pava vacía, el agua fría, el mate sin yerba). En el horno la pava se
   calienta sin que se abra el horno.
6. **Tomar:** con el mate cebado, mantené clic derecho. **Convidar:** clic derecho sobre otro jugador o un
   gaucho (los gauchos agradecen). Cuando se terminan las cebadas, el mate "se lava".

**Confraternidad** (el efecto del mate): recupera vida de a poco y, en la mesa de truco, tus señas son el
doble de difíciles de descubrir para los rivales y ganar el partido da un 20 % más de Prestigio Criollo.
Se acumula hasta 6 minutos. Convidar da el logro **El que Ceba**.

### Picada y bebidas

| Comida / bebida | Receta | Efecto |
|---|---|---|
| Salame (×2) | chuleta de cerdo cruda + carne de vaca cruda | Comida |
| Queso de Campo (×3) | balde de leche | Comida |
| Galleta de Campaña (×3) | 2 trigos + huevo | Comida rápida |
| **Tabla de Picada** | salame + queso + galleta + losa de madera | Se apoya en la mesa y se come de a 4 porciones (como una torta); cada porción da Confraternidad. Quien termina la tabla gana **Picada Completa** |
| Vino Tinto de la Casa | botella + 3 bayas dulces | Regeneración |
| Caña | botella + 3 cañas de azúcar | Velocidad (y un mareíto) |
| Grapamiel | caña + botella de miel | Resistencia, ideal pal' frío |

### Discos de música criolla (originales)

| Disco | Dónde se consigue |
|---|---|
| Milonga del Truco (guitarras y bandoneón) | pulpero, cofre de la estancia |
| Vals de la Pulpería (vals criollo) | pulpero, cofre de la estancia |
| Candombe de Llamadas (cuerda de tambores) | tocadiscos y cofres de la Guarida |
| Payada del Forastero (guitarra sola) | tocadiscos del salón de la Estancia |

La música se compone y sintetiza con `python3 tools/generar_musica.py` (sin samples).

### Árbol de logros "Truco Ya" (14)

Tomá Asiento · Cartas sobre la Mesa · El que Ceba · Picada Completa · Flor de Viaje · Puro Cuento ·
Contraflor al Resto · Creciendo en el Boliche · El Desafío del Forastero · Noche Clandestina · Pescuezo Dorado ·
Remontada Histórica · Derrotando a la Leyenda · El Invicto del Uruguay. Se ven con la tecla L (Progresos).

## El Camino del Truco (modo historia)

Al entrar por primera vez recibís la **Libreta del Truquero** (clic derecho para leerla, o `/truco historia`).
Ahí está todo el camino, con los capítulos, los sellos y el rumbo:

1. **Primeras manos**: ganá tu primer partido.
2. **La Ruta de las Pulperías**: el mundo tiene 6 regiones y cada pulpería tiene su **campeón**. Mientras no
   tengas el sello de esa región, los gauchos de esa pulpería juegan al nivel del campeón. Ganales un partido
   y te llevás el sello.

   | Región | Biomas | Campeón |
   |---|---|---|
   | la Llanura | praderas (y las aldeas de llanura) | Nv. 2 |
   | la Sabana | sabana | Nv. 2 |
   | el Desierto | desierto | Nv. 3 |
   | el Monte | bosque, taiga, selva y pantano | Nv. 3 |
   | la Nieve | campos helados | Nv. 4 |
   | la Sierra | montañas y mesetas (mesa) | Nv. 4 |

3. **Taita del Truco**: llegá al Nivel 4 de Prestigio Criollo.
4. **El Mapa del Baqueano**: con los seis sellos y el nivel, un Fundador te trae un mapa con una cruz roja
   en la Estancia del Caudillo. La Libreta también te dice a cuántos bloques queda y hacia dónde.
5. **El Último Truco**: recién ahí el **Viejo Vizcacha** acepta sentarse con vos (antes te dice cuánto te
   falta) y empieza su cinemática. Juega con la IA más fuerte del mod.
6. **Epílogo**: si le ganás, la **cinemática final**: esa misma tarde llegan a caballo los cuatro Fundadores
   y se arma la ronda alrededor del fuego, con los títulos del mod. `/truco cinematica final` la repite.

### Los Fundadores

Los cuatro creadores del mod son personajes del juego y guían la historia. En cada momento importante uno
llega a caballo hasta donde estés, te habla, te deja algo y se va:

| Quién | Cuándo aparece | Qué te deja |
|---|---|---|
| **Nicolás Bentancour** | la primera vez que entrás | un mate para el camino |
| **Agustín Perdomo** | con tu primer sello | 5 esmeraldas para el primer torneo |
| **Matías Piñeyro** | con el tercer sello | el Banderín de Cuadrera |
| **Thiago Rodríguez** | con los seis sellos y Nivel 4 | el Mapa del Baqueano |

Nicolás te recibe apenas entrás al mundo, unos segundos después de cargar (también en mundos que ya tenías, si todavía no te había saludado). Si estás a cielo abierto llegan a caballo; si estás bajo techo o en una cueva, llegan caminando. Si no encuentran el camino, se te acercan solos. En otra dimensión esperan a que vuelvas; si pasa mucho tiempo, te mandan una carta.

#### Las voces reales de los Fundadores
Desde 1.7.0, las cuatro líneas de los Fundadores en la cinemática final tienen **las voces de verdad** de
Nicolás, Agustín, Matías y Thiago. Las grabaciones originales están en `tools/grabaciones/`. El guion y cómo
grabar están en `tools/grabaciones/LEEME.txt`.

`python3 tools/usar_grabaciones.py` procesa cada grabación:
- La limpia con un limpiador de voz neuronal (GTCRN) que saca el ruido de fondo y el eco del cuarto. Solo se queda con la versión limpia si la nota de oyente (DNSMOS) mejora.
- Corta los silencios.
- Empareja el volumen con la voz del Vizcacha (unos -17 dB en lo hablado).
- Le da el mismo aire de campo que al resto.
- La acomoda al turno de 9,5 s de cada uno; si se pasa, la acelera hasta un 12 % sin cambiar el tono.
- Rehace la música, que baja el volumen donde habla cada uno, y los tiempos de los subtítulos.

Para cambiar una voz, se reemplaza el archivo en `tools/grabaciones/` y se vuelve a correr.

## Torneos en las pulperías

En cualquier mesa de una pulpería aparece el botón **Torneo (5 esmeraldas)**. Son tres rivales seguidos
que llegan a caballo, cada uno un nivel más fuerte, y se juegan partidos cortos a 20 puntos (10 malas y
10 buenas). El campeón se lleva 20 esmeraldas, la **Copa del Torneo** y 60 de prestigio. Si perdés un
partido, quedás afuera.

### Carreras cuadreras

Poné un **Banderín de Cuadrera** (palos, lana blanca y tinte rojo, o el que te da Matías). La carrera va
hacia donde mirabas al ponerlo. Montá tu caballo y tocalo: después de la cuenta regresiva largás contra
dos jinetes del pago, 120 bloques en línea recta. Sus caballos van parejos con el tuyo, así que gana el
que mejor corre. Si largás antes de tiempo, se anula.

Si tenés esmeraldas en la mano al tocar el banderín, las apostás: ganando cobrás el doble.

### Comandos de la historia

`/truco historia` · `/truco cinematica` · `/truco cinematica final` · `/truco sello <región|todos>` · `/truco completar` (llena la Libreta: nivel, seis sellos, torneo, cuadrera, copa, banderín y Mapa del Baqueano) · `/truco completar todo` (además deja al Vizcacha vencido y el final visto) · `/truco salamanca visita|partida|cancelar`
(operadores, para probar) · `/truco gaucho fundador`.

## La Salamanca (capítulo 7)

Lo que pasa después de ganarle al Viejo Vizcacha. La historia: la Baraja de Oro nunca fue del Negro Ledesma;
se la ganó al **Mandinga** en una salamanca, con un pacto: al morir, su alma sería del diablo. Por eso, al
caer en el Paso de las Ánimas, se la dio al Vizcacha para escondérsela, y el Mandinga se lo llevó de compañero
de mesa para siempre. "No estuve setenta años esperando un rival, gurí. Estuve setenta años esperando a alguien
que me acompañara a buscarlo."

1. **El final** (la cinemática de los Fundadores): durante la ronda, el fantasma de Ledesma aparece entre el humo
   del fogón. Después de los créditos, en negro, alguien baraja: "Setenta años, Vizcacha... ¿todavía tenés mi baraja?"
2. **La visita** (media cinemática): al atardecer del día siguiente, si estás al aire libre, el Vizcacha llega al
   galope con un tordillo ensillado para vos. Te cuenta la verdad y te da el **Farol de las Ánimas**.
3. **La cabalgata**: lo seguís a caballo; cae la noche, llueve, caen rayos y las luces malas marcan el camino.
   Si el Paso queda lejos, "esa noche" la película salta al último tramo.
4. **El Paso de las Ánimas** (`/locate trucoya:paso_animas`, uno por mundo, a 500-900 bloques de la estancia,
   cerca de un río): de día es una tapera en ruinas con un ombú, las cruces de los caídos, lanzas oxidadas y la
   cruz del Negro Ledesma. Al llegar, entre rayos, se rearma la **Salamanca**: el Mandinga y el fantasma de Ledesma
   esperan a la mesa.
5. **La partida**: vos y el Vizcacha (compañeros, enfrentados) contra el Mandinga y Ledesma, a 30 con flor.
   Ledesma juega obligado pero te hace señas a escondidas; cuando les faltan 3 puntos o menos, tiene flor... y
   contesta "con flor me achico".
6. **El amanecer**: si ganan, el Mandinga se esfuma, Ledesma se despide del Vizcacha y se va con el sol, la
   Salamanca vuelve a ser tapera y te llevás el **Facón de Ledesma** (+150 de prestigio, logro "Alma Libre").
   Si pierden, otra noche, con el farol en la mano, volvé al Paso: el Vizcacha te espera en la puerta.

Durante el capítulo nadie te puede lastimar ni aparecen monstruos cerca, no se puede dormir, y si te vas del
mundo o se corta el juego, todo vuelve a quedar como estaba (la tapera en ruinas, sin personajes). Las escenas
no se saltean (el mundo cambia al mismo tiempo). En mundos creados antes de la 1.5 el Paso se levanta la
primera vez que hace falta, en el lugar que le toca a ese mundo.

Las voces (el Vizcacha, el Mandinga y Ledesma) salen de `tools/voces_salamanca.py`, que también escribe los
tiempos de cada escena (`cinematica/escena_*.json`, que leen igual el servidor y el cliente) y los cantos del
Mandinga en la mesa. Las plantillas de día y de noche: `tools/estructuras_paso.py`.

Comandos de prueba (operadores): `/truco salamanca visita` (que venga el Vizcacha ahora),
`/truco salamanca partida` (directo al Paso, de noche), `/truco salamanca amanecer` (al Paso y, sin jugar la
partida, directo al amanecer y la llegada de los Fundadores), `/truco salamanca derrota` (lo mismo, pero a la
escena de la derrota), `/truco salamanca cancelar`.

## Mesa más limpia y cámaras de la Salamanca (1.7.6)

- **Sobre la mesa, solo la baza que se juega:** una carta delante de cada uno, en cruz en el medio del paño.
  Cuando alguien tira la primera carta de la baza siguiente, las de la anterior se levantan (mientras tanto quedan
  a la vista, para ver quién la ganó). Las tres bazas siguen en la columna del costado de la pantalla.
- **El mazo y la muestra** van en la esquina a la derecha de quien repartió, sin salirse del paño.
- **El anotador ya no va arriba de la mesa:** tapaba las cartas. Las estructuras ya no lo traen, no se deja
  poner encima de la mesa, y si en un mundo viejo había uno, al repartir se levanta y queda en el piso. Puesto
  al lado de la mesa (a dos bloques como mucho) sigue contando los porotos.
- **Las cámaras de la Salamanca:** en la llegada, la derrota y el comienzo del amanecer, cada uno aparece de
  frente y desde arriba del paño mientras habla; ninguna toma queda detrás de un respaldo.
- **Si perdés contra el Mandinga** el Vizcacha no te viene a buscar de nuevo: amanecés en el Paso, y otra noche
  volvés con el Farol de las Ánimas en la mano. Las luces malas te marcan el camino y, cerca del Paso, el Vizcacha
  ya te está esperando para la revancha.

## Los Fundadores al final, "son buenas" y arreglos (1.7.3)

- **Los cuatro Fundadores te felicitan.** Cuando le ganás al Mandinga en la Salamanca, después del amanecer llegan
  los cuatro creadores juntos a caballo, se paran en media ronda delante tuyo y cada uno te felicita **con su
  propia voz**: las frases nuevas se hicieron clonando las grabaciones que mandaron (`tools/grabaciones/`), con el
  modelo XTTS-v2 (`scripts\clonar_voces.bat` en la PC, `tools/clonar_voces.py` y `tools/felicitaciones.py`). Después te
  regalan la Baraja de los Fundadores y se van. Si ya habías vencido al Mandinga antes de esta versión, vienen la
  próxima vez que entres. Para probarlo: `/truco fundadores` (operadores).
- **"Son buenas".** Al querer un envido (o comparar flores), los tantos se cantan como en la mesa de verdad: la
  mano dice su número; cada rival del que va ganando o lo supera y dice el suyo, o dice **"son buenas"** sin
  mostrar nada; el compañero del que va ganando no habla. En el 2 contra 2, si cantás 33 y los dos rivales tienen
  menos, los dos dicen "son buenas". El empate es para el más cerca de la mano.
- **El caballo del Vizcacha va de frente.** En Minecraft un caballo con un jinete que no es jugador copia el giro del
  jinete y el jinete el del caballo, y los dos quedaban trabados mirando para donde aparecieron: parecía que el
  caballo iba marcha atrás. Ahora los jinetes del mod miran hacia donde camina el caballo. Los caballos de los
  Fundadores van en pelo, porque con montura esperan que el jinete los maneje y no caminan.
- **El Vizcacha te viene a buscar seguro.** Antes había un solo minuto al atardecer, y si justo estabas bajo techo,
  sentado a una mesa o durmiendo, no venía. Ahora el primer atardecer tiene tres horas de juego, y si ese día no
  pudo, a partir del día siguiente viene a cualquier hora en cuanto estés a cielo abierto. Si estás adentro, te
  avisa: "Se oye un galope a lo lejos...".
- **Las bazas, bien al costado:** en pantallas grandes quedan en una columna contra el borde derecho, centradas
  a lo alto; si no entran, de a dos por fila.
- `/truco completar todo` también te da la Baraja de Oro (antes solo la habilitaba en la colección).

## Afinado: bazas al costado y el final de la Salamanca (1.7.2)

- **Las bazas, contra el costado derecho.** El cuadro con las cartas de la primera, segunda y tercera baza ya no
  está arriba al centro: queda recostado contra el borde derecho, debajo del pozo, y el centro de la pantalla
  queda libre para ver la carta que tira el rival en la mesa. El tapete elige solo cómo acomodarlas (las tres en
  fila, de a dos o una debajo de otra) para que las cartas salgan lo más grandes posible sin tapar la botonera,
  y el tamaño no cambia a mitad de la mano. El historial (H) pasó a la izquierda, debajo del anotador.
- **El amanecer de la Salamanca, bien encuadrado.** En la despedida, Ledesma y el Vizcacha quedan parados entre la
  mesa y el mostrador; antes la cámara los buscaba desde el lado de la puerta y se veía el respaldo de una silla.
  Ahora las tomas siguen en vivo a los dos personajes y se ponen en el pasillo entre ellos (de frente a quien
  habla) o altas del lado de la mesa, por encima de los respaldos. La última toma de Ledesma lo muestra de
  cuerpo entero mientras se desvanece con el sol.
- **El fantasma de Ledesma en "El Último Truco".** En la ronda alrededor del fogón, la cámara termina la vuelta
  del lado abierto (entre el Vizcacha y los Fundadores) y se acerca de frente al fantasma, así no lo tapa nadie.

## Cartas más claras y estructuras con profundidad (1.7.1)

**Cartas que se leen bien:**
- Las cartas del tapete y las de la mesa usan la misma textura grande (128 x 192 por carta) **con mipmaps y filtro suave** (`client/CartasTextura.java`): se ven nítidas en cualquier tamaño y no titilan de lejos.
- Sobre la mesa son más grandes (0,21 x 0,315 bloques), cada baza a su altura para que no se pisen. Tienen una luz mínima, así que se leen también de noche o dentro de un rancho oscuro.
- En la vista compacta del tapete, las cartas crecen con la pantalla (hasta 56 x 84).
- **Las bazas en pantalla:** contra el costado derecho (desde 1.7.2) se ven derechas y grandes las cartas jugadas en la mano, un grupo por baza. Cada carta lleva el nombre de quien la tiró, el rótulo dice de quién fue cada baza, y un marco dorado indica a quién le toca tirar.
- **Lupa:** al pasar el mouse por una carta de la mano o de las bazas, se ve en grande.
- El número de cada carta es más grande y grueso, y se lee aunque la carta esté chica.

**Estructuras con profundidad.** Todas las pulperías, la Estancia y la Guarida tienen ahora una pasada de detalle (`tools/estructuras_detalle.py`), con las técnicas de los grandes constructores de Minecraft:
- zócalo saliente de escalones y pilares de esquina adelantados;
- pilastras con capitel que marcan el ritmo de la fachada;
- cornisa bajo el techo, y alero con espesor (losas invertidas bajo el borde del techo);
- ménsulas y zapatas (escalones invertidos) donde los postes sostienen vigas y techos;
- ventanas con alféizar, guardapolvo y flores al pie;
- materiales mezclados y gastados: tejas de dos tonos, piedra con musgo y grietas, revoque con manchas;
- enredaderas en algunos paños;
- postigos abiertos a los costados de las ventanas y faroles colgando a los lados de las puertas;
- **árboles orgánicos** (`tools/estructuras_arboles.py`): el ombú tiene tronco enorme, raíces que se hunden en el pasto, ramas y una copa ancha e irregular; también hay eucaliptos altos de tronco blanco en la Estancia y pinos por pisos en la nieve;
- terreno vivo: manchas de tierra, grava y podzol, pasto alto, helechos, matas, piedras sueltas, postes con farol y pilas de leña.

Nada de esto toca lo que importa para jugar: mesas, sillas, puertas, carteles y personajes quedan donde estaban, y donde hay gente o animales no crece nada.

**Vista previa de estructuras sin abrir el juego:**

```
python3 tools/vista_estructuras.py plantilla.nbt salida.png [--girar 0-3] [--escala 16]
```

Dibuja la plantilla en isométrico con las texturas de Minecraft.

## Barajas coleccionables (1.6.0)

Diez barajas españolas completas, cada una con su papel, sus colores, su marco, un dibujo apenas marcado
detrás de cada carta y un dorso propio. Se juega con la que elegís y la ve toda la mesa (la pone quien arma la
partida).

| Baraja | Diseño | Cómo se gana |
|---|---|---|
| Criolla | la de siempre, sol de rayos en el dorso bordó | viene con la Libreta |
| de Oro | marco doble dorado con filigranas, dorso azul noche "El Último Truco" | ganándole al Viejo Vizcacha |
| del Mandinga | papel negro, llamas en el marco, el sombrero y los ojos en el dorso | al amanecer en la Salamanca |
| de la Llanura | guarda pampa, ombú, amanecer en el campo | sello de la Llanura, o el pulpero de allá |
| de la Sabana | rayos de sol, ñandú, atardecer con acacias | sello de la Sabana, o el pulpero |
| del Desierto | escalonados de adobe, cardón | sello del Desierto, o el pulpero |
| del Monte | enredadera, pinos, hornero en su rama | sello del Monte, o el pulpero |
| de la Nieve | copos, cerros nevados de noche | sello de la Nieve, o el pulpero |
| de la Sierra | perfil de cerros, cóndor al anochecer | sello de la Sierra, o el pulpero |
| de los Fundadores | los cuatro colores de los Fundadores, un mate y sus iniciales | después del final |

**Elegir con qué baraja jugar.** Hay tres formas:
- en la Libreta, pestaña **Barajas** (tocás un mazo para mirarlo y "Jugar con esta");
- con clic derecho en el mazo;
- con `/truco baraja <clave>`.

**Comprar y regalar.** Cada pulpero vende la baraja de su región por 16 esmeraldas, y la criolla por 3. Las de
región y la criolla se pueden regalar: el mazo se usa al sumarlo a la colección. Las de hazaña (Oro, Mandinga y
Fundadores) solo le sirven a quien las ganó.

Las partidas empezadas antes de 1.6.0 reciben al entrar las barajas de lo que ya habían hecho.

Logro: **Coleccionista** (seis barajas).

Generador: `tools/barajas_temas.py`. Arma `textures/gui/cartas_<clave>.png` (1280 x 960) y
`textures/block/cartas_<clave>.png` (640 x 480, para la mesa del mundo). Después,
`tools/generar_barajas_items.py` arma los íconos, los modelos y los idiomas.

## Pedidos de los paisanos (1.6.0)

Algunos paisanos, paisanas, peones y payadores tienen **"¡Pedido!"** flotando sobre la cabeza. Cada día de
juego, alguno se acuerda de algo que necesita. Con clic derecho te lo cuenta. Para aceptarlo, otro clic (o
**[Aceptar]** en el chat). Agachado, el clic lo invita a jugar como siempre.

| Clase | Qué hay que hacer |
|---|---|
| Traer | Juntar lo que pide (queso, salame, yerba, vino, cuero, lana, leña, huevos, carne, pan, hierro, miel, zapallo, pescado) y dárselo en mano. |
| Llevar | Te da una **Encomienda** para cualquier paisano de otra región. |
| Desafío | Ganar en una pulpería de cierta región, cantar una flor, ganar una cuadrera, ganarle a un forastero o ganar tres partidos. Se cumple solo. |

- **Premio:** esmeraldas y prestigio.
- **Límite:** hasta 3 pedidos a la vez.
- **No se repiten:** un pedido cumplido o cancelado no se puede volver a aceptar.
- **Dónde verlos:** en la Libreta, pestaña **Pedidos**, con el rumbo al paisano y el botón para cancelar. También con `/truco pedidos` y `/truco pedidos cancelar <n>`.

Logro: **Gaucho de palabra** (cinco pedidos).

## Desafíos del día (1.7.0)

Cada día de calendario (según el reloj del servidor), cada jugador tiene **tres desafíos chicos**, distintos
para cada uno. Por ejemplo:
- ganar dos partidos;
- ganar tres envidos;
- cantar una flor;
- hacer un farol;
- ganar una mano con retruco o con vale cuatro querido;
- ganar una cuadrera;
- convidar tres mates;
- cumplir un pedido;
- ganarle a un forastero;
- ganar en una pulpería.

Como mucho sale uno difícil por día.

**Premios:**
- Cada desafío cumplido da 2 esmeraldas y 25 de prestigio.
- Los tres juntos dan 5 esmeraldas y 40 de prestigio más, y suman a la **racha de días seguidos**. Si un día no se completan los tres, la racha se corta.

**Dónde verlos:** en la Libreta, pestaña Pedidos, en la página derecha.

**Logro:** *Siete días seguidos*.

## La Libreta (1.6.0)

Ahora tiene cuatro pestañas:
- **Camino:** los capítulos y los sellos, como antes.
- **Barajas:** la colección.
- **Pedidos.**
- **Números:** partidos ganados y perdidos, racha y mejor racha, flores, faroles, forasteros, torneos, cuadreras, pedidos y victorias sobre el Vizcacha, con el porcentaje de partidos ganados.

## Modo tráiler (1.7.0)

Sirve para grabar el tráiler del mod con la cámara de cine. Lo pueden usar los operadores, o quien juega su
propio mundo.

**Comandos:**
- `/truco trailer marcar` en cada lugar por donde tiene que pasar la cámara, mirando hacia donde tiene que mirar (hasta 16 marcas).
- `/truco trailer [segundos]` lo arranca. Los segundos son el tiempo entre una marca y la siguiente, de 2 a 30; por defecto, 6.
- `/truco trailer parar` lo corta.
- `/truco trailer borrar` borra las marcas.

**Qué hace:** la cámara recorre las marcas con curvas suaves, franjas de cine y la milonga de fondo. Muestra
carteles que cuentan el mod y cierra con el título y los nombres de los cuatro Fundadores.

**Cómo funciona por dentro:**
- El jugador no aparece en cuadro.
- El servidor lo lleva junto a la cámara, así se carga el mundo que se filma, y al final lo devuelve donde estaba.
- Sin marcas, la cámara da una vuelta alrededor del jugador.

**Para grabar en Windows:** `Win + Alt + R` (barra de juegos de Xbox) u OBS.

## Servidor (1.6.0)

Se puede jugar en un servidor con amigos. Los pasos están en **[SERVIDOR.md](SERVIDOR.md)**: `scripts\servidor.bat` arma
todo solo. La historia es de cada jugador.

Las opciones del servidor están en `serverconfig/trucoya-server.toml`:
- si la Salamanca puede cambiar la hora y el clima, que es para todos;
- si hay pedidos;
- la espera de `/truco cinematica`.

Cambios para multijugador:
- la Salamanca deja el clima como estaba;
- nunca hace retroceder el día;
- quien mira una cinemática no puede pegarle a nadie;
- las apuestas solo se retiran estando en la mesa.

## Cinemática: "El Último Truco" (Fase 6)

La primera vez que te acercás al **Viejo Vizcacha** (a menos de 9 bloques, viéndolo y con la Ruta completa), arranca una
cinemática de 90 segundos hecha con el propio motor del juego: cámara de cine que se mueve en curvas
suaves cuadro a cuadro (tan fluida como tus FPS y a tu resolución), franjas negras 2.39:1, viñeta,
subtítulos, carteles, la voz del Vizcacha narrando y una banda sonora compuesta segundo a segundo.

| Tiempo | Escena |
|---|---|
| 0–13 s | **Presente.** El Vizcacha te reconoce: "Antes de las cartas, yo conocí la guerra". |
| 13–33 s | **El fogón** (recuerdo en sepia). Paso de las Ánimas, invierno del cuarenta: la noche antes de la batalla, cuatro gauchos jóvenes juegan al truco. El Negro Ledesma canta la flor más linda y se gana el facón del Vizcacha. |
| 33–58 s | **La carga al alba.** Clarín, trescientos jinetes, "¡A la carga, muchachos!", rayos y cámara lenta hasta el choque. |
| 58–68 s | **La caída.** Entre el humo, Ledesma le da su baraja: la Baraja de Oro. |
| 68–90 s | **Presente.** "Setenta inviernos esperé. Sentate, gurí... vamos a ver si sos digno." — *EL ÚLTIMO TRUCO*. |

- **Mantené la tecla de salto (Espacio)** para saltearla. Mientras dura, nadie te puede lastimar.
- Se ve **una sola vez** por jugador. Para volver a verla: `/truco cinematica` (con el Vizcacha más
  cercano, o con uno que aparece solo para la función).
- El recuerdo se arma en el cielo, por encima del mundo, y **existe solo en tu pantalla**: los bloques
  se devuelven tal como estaban y los actores no existen en el servidor. Nada se guarda en el mapa.
- Audio: `tools/generar_cinematica.py` genera la narración (voz "viejo"), el grito de la carga, la
  música y `assets/trucoya/cinematica/vizcacha.json` (subtítulos sincronizados). Con
  `--musica --prueba salida.ogg` rehace solo la música y exporta la mezcla completa para escucharla.
- La música del juego se pausa durante la función; la banda sonora suena en el canal "Discos"
  (Jukebox/Note Blocks) y la narración en "Voz".

## Voces y recursos gráficos

### Las voces de los cantos

Cinco voces con síntesis neuronal (`tools/generar_voces_neural.py`), cada una con los 13 cantos en
5 tomas distintas (firme, gritado, canchero, grave y rápido; en los de retirada: normal, resignado,
fastidiado, canchero y rápido) que suenan al azar. Acento rioplatense: sheísmo ("sho", "cashate"),
voseo y giros uruguayos ("bo", "ta", "che", "nomás", "m'hijo", "gurí").

Para que suenen humanas: nada de vocoders (la voz grave del Vizcacha sale del propio modelo, moviendo
su "vector de estilo" hacia lo grave), un temblor leve de la edad en la voz vieja, y por cada toma se
generan varias versiones: gana la que un juez automático entrenado con notas de oyentes humanos
(DNSMOS, de Microsoft) considera más natural, entre las que el reconocedor de voz entiende bien.
`tools/reparar_voces.py` rehace las pocas tomas que no se entendieron.

| Voz | Quién la usa |
|---|---|
| hombre | jugadores (por defecto) y paisanos |
| gaucho | voz más gruesa: peones, matreros y algunos paisanos |
| viejo | el Viejo Vizcacha: grave, lenta, con el temblor de los años |
| mujer | rioplatense: paisanas y pulperas |
| paisana | otra voz femenina rioplatense, más grave |

Cada jugador elige la suya con `/truco voz hombre|gaucho|viejo|mujer|paisana`. La voz sale de la boca de
quien canta (se oye de qué lado de la mesa viene).

Motores: Kokoro-82M (Apache-2.0) y la voz argentina "daniela" de Piper (datos OpenSLR 61, CC BY-SA 4.0),
corridos con sherpa-onnx. Cada toma se verifica con un reconocedor de voz (Whisper) y se rehace si no se entiende.
Para regenerar: `pip install sherpa-onnx pyworld` y `python3 tools/generar_voces_neural.py`
(baja los modelos solo, en `tools/.modelos`). Orden de los generadores: `generar_recursos.py`,
`generar_fase5.py`, `generar_voces_neural.py`.

### Gráficos

- Texturas, modelos y gaucho: `tools/generar_recursos.py` y `tools/generar_gaucho.py` (diseños originales).

## Arquitectura

```
src/main/java/com/trucoya/
  TrucoYa.java              Clase @Mod: registros y red
  core/                     Motor de Truco SIN dependencias de Minecraft (probado)
    Card, Suit, Deck        Baraja española de 40 cartas (instancias únicas, id 0..39)
    CardRules               Muestra, piezas, jerarquía, envido y flor
    TrucoMatch, Round       Partido y máquina de estados de cada ronda
    ActionType              Cantos y respuestas, clave de audio y texto del globo
    GameEvent               Eventos para audio, globos y registro
    PlayerView              Vista privada por jugador (serializable)
    TableSnapshot           Lo que se envía al cliente (vista + nombres + registro + config)
    TrucoBot                Jugador automático básico (rellena asientos vacíos)
    Sena                    Señas de la rueda G
  registry/                 DeferredRegister de bloques, ítems, tile entities, entidades y sonidos
  block/                    Silla Criolla, Mesa de Truco, Anotador con Porotos
  entity/SeatEntity         Asiento invisible para sentarse
  tileentity/               MesaTrucoTileEntity: aloja la partida, bots, sonidos y anotador
  network/                  Canal SimpleChannel y paquetes (acción, vista, globo, seña)
  client/                   Tapete, menú de señas, teclas, globos, renderizador del asiento
tools/                      Generadores de texturas, modelos, voces y efectos
```

Decisiones que evitan errores en multijugador:

- **El servidor es la única autoridad.** El cliente envía intenciones (botón, carta por id, seña) y el
  servidor las valida (distancia, que esté sentado, que sea su asiento y que la jugada sea legal).
- **Sin duplicación ni ítems en juego.** Las cartas no son ítems; las sillas, mesa y anotador sueltan
  su propio bloque. Ningún paquete puede crear ni mover objetos.
- **Sin filtrado de información.** El cliente solo recibe su mano, la muestra y lo jugado.
- **Paquetes del lado cliente aislados** con `DistExecutor`, así el servidor dedicado no carga clases de cliente.
- **Asientos autolimpiables:** la entidad del asiento desaparece cuando el jugador se levanta o la silla se rompe.
- Si un jugador se levanta a mitad de partida, lo reemplaza un paisano; si no queda nadie, la partida se suspende.

Limitación conocida: las partidas en curso no se guardan al cerrar el mundo (la mesa queda libre).

## Reglas implementadas

### Cartas
| Concepto | Regla |
|---|---|
| Mazo | 40 cartas, sin 8 ni 9 |
| Muestra | Tras repartir 3 cartas a cada uno (de a una, empezando por la mano), la siguiente carta del mazo queda boca arriba |
| Piezas (palo de la muestra) | 2 (30) > 4 (29) > 5 (28) > Caballo (27) > Sota (27) |
| Muestra mata pieza | Si la muestra es 2, 4, 5, 11 o 10, el Rey de ese palo toma su valor y jerarquía |
| Jerarquía restante | 1 Espadas > 1 Bastos > 7 Espadas > 7 Oro > 3 > 2 > 1 falsos > 12 > 11 > 10 > 7 falsos > 6 > 5 > 4 |

### Envido
- Con pieza: valor de la pieza + la carta de mayor valor entre las otras dos (ej.: 4 de la muestra, 3 y 6 = 35).
- Sin pieza: dos del mismo palo = 20 + ambas; si no, la carta más alta. Figuras = 0.
- Cantos: Envido (2), Real Envido (3), Falta Envido. Envido → Envido / Real / Falta; Real → Falta.
- No querido: 1 si era el primer canto; si no, lo acumulado antes de la última subida.
- Empate: gana el jugador más cercano a la mano.
- Solo en la primera baza, por un jugador que todavía no jugó carta y antes de que se cante truco.
  Excepción "el envido está primero": se puede responder al primer Truco con un envido.

### Flor
- Tres del mismo palo; pieza + dos del mismo palo; dos piezas + cualquier carta; tres piezas.
- Puntos: 20 + suma (pieza vale 10/9/8/7/7). Máximo: 2 + 4 + 5 de la muestra = 47.
- Es obligatoria: quien tiene flor debe cantarla antes de jugar su primera carta. La flor anula el envido.
- Sin flor rival: 3 puntos. Con flor rival, el rival responde:

| Situación | Resultado |
|---|---|
| Con flor me achico | quien cantó flor suma 3 |
| Contraflor querida | la mejor flor suma 6 |
| Contraflor no querida | quien cantó contraflor suma 3 |
| Contraflor al resto querida | la mejor flor suma la falta (ver Partido) |
| Contraflor al resto no querida | 6 si venía de contraflor; si no, 3 |

El punto de la ronda se suma aparte: si después del achique el rival se va al mazo sin truco, es 1 punto más.

### Truco
- Truco (2), Retruco (3), Vale Cuatro (4). No querido: 1, 2, 3.
- Solo puede subir el equipo que aceptó el último canto, en su turno.
- Me voy al mazo: el rival suma el valor vigente (si había un truco sin responder, cuenta como no quiero).
  En 2v2, un jugador se va al mazo en nombre de su equipo.

### Pardas
- 1ª parda: decide la 2ª. 1ª ganada y 2ª parda: gana quien ganó la 1ª.
- 1-1 y 3ª parda: gana quien ganó la 1ª. Todas pardas: gana la mano.
- Tras una parda vuelve a salir quien salió en esa baza.

### Partido
- A 30 (15 malas + 15 buenas) o a 40 (20 + 20). El partido termina apenas un equipo llega al tope.
- **Falta envido y contraflor al resto:** se juega el partido entero, salvo que un equipo esté en
  Las Malas y el otro en Las Buenas; en ese caso vale lo que le falta al que va ganando.
  (Alternativa configurable: `MatchConfig.FaltaMode.LEADER_NEEDS_TO_WIN`.)
