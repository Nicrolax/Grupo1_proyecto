# Registro de cambios

Todos los cambios importantes de Truco Ya. El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/)
y las versiones usan [versionado semántico](https://semver.org/lang/es/). El detalle de cada función está en el
[Manual](docs/MANUAL.md).

## [Sin publicar]

## [1.7.5] - 2026-10-25

### Agregado
- Comando de prueba `/truco salamanca amanecer` (operadores): va al Paso de las Ánimas y salta la partida, para ver
  el final de la Salamanca y la llegada de los Fundadores sin jugar los 30 puntos.

### Cambiado
- Los cuatro Fundadores se paran a unos 5 bloques en media ronda: a caballo ya no tapan la vista.
- Capturas nuevas en `docs/capturas/`.

## [1.7.4] - 2026-10-21

### Agregado
- Logo del mod: se ve en la lista de mods del juego y en el repositorio.
- Datos del mod completos: autores, página, reporte de errores y créditos.

### Cambiado
- Repositorio ordenado: los scripts de Windows pasan a `scripts/`, las guías a `docs/` y las grabaciones a
  `tools/grabaciones/`; el `.jar` ya no se sube al repositorio (se descarga de Modrinth).
- README nuevo, registro de cambios, guía para colaborar, créditos y plantillas para reportar errores.

### Corregido
- Los botones del tapete compacto ya no cortan el texto ("Al mazo", "Me achico", "Flor al resto").

## [1.7.3] - 2026-10-21

### Agregado
- Al vencer al Mandinga llegan los cuatro Fundadores juntos y te felicitan, cada uno con su voz (clonada de sus
  grabaciones, con su permiso). Te regalan la Baraja de los Fundadores. Comando de prueba: `/truco fundadores`.
- Herramienta para hacer frases nuevas con las voces de los Fundadores: `scripts\clonar_voces.bat` y
  `tools/felicitaciones.py`.

### Cambiado
- Los tantos se cantan como en la mesa de verdad: el que no supera al que va ganando dice "son buenas" sin mostrar
  su número, y el compañero del que va ganando no habla.
- Con el Anotador de porotos sobre la mesa, las cartas van apoyadas encima y no quedan tapadas.
- En pantallas grandes, las bazas quedan en una columna contra el borde derecho.
- `/truco completar todo` también da la Baraja de Oro.

### Corregido
- El caballo del Vizcacha (y el de cualquier jinete del mod) ya no anda de costado ni marcha atrás.
- Los caballos de los Fundadores caminan de verdad hasta el jugador (con montura no se movían).
- El Vizcacha te viene a buscar aunque el primer atardecer estuvieras bajo techo, sentado o durmiendo.

## [1.7.2] - 2026-10-20

### Cambiado
- Las bazas en pantalla pasan al costado derecho: el centro queda libre para ver la carta del rival.
- El historial (H) pasa a la izquierda, debajo del anotador.

### Corregido
- El amanecer de la Salamanca: la cámara ya no queda detrás del respaldo de una silla y se ve a Ledesma.
- El fantasma de Ledesma en "El Último Truco" se ve de frente.

## [1.7.1] - 2026-10-20

### Agregado
- Las bazas en pantalla, grandes y derechas, con el nombre de quien tiró cada carta.
- Lupa: al pasar el mouse por una carta, se ve en grande.
- Árboles orgánicos (ombú, eucaliptos, pinos), postigos y faroles en las estructuras.

### Cambiado
- Cartas con números más grandes y textura con mipmaps: se leen bien de cerca y de lejos.
- Pasada de detalle en pulperías, Estancia y Guarida: zócalos, pilastras, cornisas, aleros, ventanas con
  alféizar, materiales gastados y terreno con vida.

## [1.7.0] - 2026-10-20

### Agregado
- Las voces reales de los cuatro Fundadores en la cinemática final.
- Desafíos del día: tres por jugador, distintos cada día.
- Modo tráiler: `/truco trailer` recorre marcas con la cámara de cine.
- Documentación completa del proyecto (requisitos, UML, casos de uso, infraestructura, pruebas, calidad) e
  informe en PDF.

## [1.6.0]

### Agregado
- Diez barajas coleccionables, cada una con su diseño y su forma de ganarla.
- Pedidos de los paisanos: traer cosas, llevar encomiendas y desafíos.
- La Libreta con cuatro pestañas: Camino, Barajas, Pedidos y Números.
- Vista compacta del tapete: se ve la mesa del mundo y abajo una franja con tu mano.
- Servidor dedicado con `scripts\servidor.bat` y opciones en `trucoya-server.toml`.
- Capítulo 7: La Salamanca.

## [1.0.0 – 1.5.0]

Las seis fases del proyecto:
- **Fase 1:** motor del truco uruguayo (Java puro, con pruebas automáticas).
- **Fase 2:** Mesa de Truco, Sillas Criollas, Anotador y el tapete.
- **Fase 3:** paisanos con IA, prestigio criollo, apuestas y armas gauchas.
- **Fase 4:** pulperías por bioma, la Guarida Clandestina y la Estancia del Caudillo.
- **Fase 5:** mate, picada, bebidas, discos de música criolla y logros.
- **Fase 6:** el Camino del Truco, torneos, cuadreras y la cinemática "El Último Truco".

[1.7.5]: https://github.com/Nicrolax/Grupo1_proyecto/commits/main
[1.7.4]: https://github.com/Nicrolax/Grupo1_proyecto/commit/d7488c9
[1.7.3]: https://github.com/Nicrolax/Grupo1_proyecto/commit/cafc900
[1.7.2]: https://github.com/Nicrolax/Grupo1_proyecto/commit/0a982bb
[1.7.1]: https://github.com/Nicrolax/Grupo1_proyecto/commit/373720b
[1.7.0]: https://github.com/Nicrolax/Grupo1_proyecto/commit/faf66d2
