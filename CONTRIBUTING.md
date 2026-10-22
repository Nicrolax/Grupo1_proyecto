# Cómo colaborar

¡Gracias por querer sumar! Truco Ya es un proyecto de clase, pero se reciben arreglos e ideas.

Al mandar un aporte aceptás que se publique con la [licencia MIT](LICENSE) del proyecto.

## Antes de empezar

- **Errores e ideas:** abrí un [issue](https://github.com/Nicrolax/Grupo1_proyecto/issues/new/choose) con la
  plantilla que corresponda. Fijate antes si ya hay uno igual.
- **Cambios grandes:** contalos primero en un issue, así no se pierde trabajo.

## Preparar el entorno

1. JDK 8 (Eclipse Temurin). En Windows, `scripts\probar.bat` lo baja solo.
2. `gradlew coreTests` corre las pruebas del motor del truco.
3. `gradlew runClient` abre Minecraft con el mod.

Para cambiar texturas, estructuras o sonidos, mirá [tools/README.md](tools/README.md): los recursos se generan
con esas herramientas, no se editan a mano.

## Estilo del código

- Java 8. Sangría de **4 espacios**, sin tabulaciones, líneas de hasta **120 caracteres** (está en
  [`.editorconfig`](.editorconfig)).
- Nombres y comentarios en español, como el resto del código.
- El motor del truco (`com.trucoya.core`) no puede depender de Minecraft: tiene que poder probarse solo.
- Toda jugada se valida en el servidor; el cliente solo muestra y pide.
- Antes de mandar, pasá el análisis estático con [PMD](https://pmd.github.io):
  `pmd check -d src/main/java -R config/pmd/reglas.xml -f text` (ver [Calidad del código](docs/08-calidad.md)).

## Pull requests

- Una cosa por pull request, con una descripción corta de qué cambia y por qué.
- Si arregla un issue, nombralo (`Arregla #12`).
- Las pruebas tienen que pasar (`gradlew coreTests`); si cambiás reglas del truco, agregá una prueba en
  `src/test/java/com/trucoya/core/TrucoCoreTests.java`.
- Si el cambio se nota en el juego, sumalo a [CHANGELOG.md](CHANGELOG.md) en "Sin publicar".
- Si usaste IA generativa para una parte importante del aporte, decilo en la descripción (ver
  [Uso de inteligencia artificial](README.md#-uso-de-inteligencia-artificial)).
