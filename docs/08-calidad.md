# 8. Calidad del código y análisis estático

## 8.1 Análisis con PMD

Antes de la entrega, el código Java se analizó con **PMD 7.7.0**. El conjunto de reglas está en
[`config/pmd/reglas.xml`](../config/pmd/reglas.xml) e incluye:
- todas las de *errorprone* (errores probables);
- todas las de *security*;
- reglas de *bestpractices*: variables, campos, métodos y parámetros sin usar, `printStackTrace`, `System.out` y conservar la causa de las excepciones;
- reglas de *design* y *codestyle*: excepciones genéricas, `if` booleanos simplificables e imports sin usar.

Para correrlo:

```
pmd check -d src/main/java -R config/pmd/reglas.xml -f text
```

### Resultado

| | Antes | Después |
|---|---|---|
| Hallazgos de prioridad 1 y 2 de seguridad | 0 | 0 |
| Variables, campos e imports sin usar | 6 | 0 |
| `toLowerCase()` sin `Locale` | 5 | 0 |
| Excepciones capturadas sin registrar (`catch` vacío) | 6 | 0 (ahora se registran en el log) |
| Hallazgos menores de estilo que quedan | — | 17 |

Los 17 hallazgos que quedan son de estilo y no cambian el funcionamiento:

- **Asignaciones dentro de una condición** (4). Por ejemplo, `while ((x = ...) != null)`.
- **`break` al final de un bucle** (4). Se usan para buscar el primer elemento.
- **Devolver `null` en lugar de una colección vacía** (4). En esos métodos (`@Nullable`), `null` significa «no hay» y se distingue de «vacío».
- **Una visibilidad cambiada con `setAccessible`** (1). Es para registrar estructuras en Forge 1.16.5, que no tiene una forma pública de hacerlo.
- **`if` que se podría escribir como un solo `return`** (2). Se dejó así porque se lee mejor.
- **Un parámetro sin usar** (1): el constructor de `CimientoPiece` tiene la firma que exige Forge para cargar piezas de estructura.
- **Un campo no serializable en un `enum`** (1): `ArmasCriollas` guarda un proveedor de ítem de reparación. Los `enum` de Java no se serializan campo por campo, así que no tiene efecto.

**Falsos positivos excluidos:** `CloseResource` marca 41 usos de `Minecraft`, `World` y `MinecraftServer`
porque implementan `AutoCloseable`. Son objetos del juego que el mod nunca abre ni debe cerrar, y
cerrarlos rompería la partida. Por eso la regla está excluida, con su justificación, en el archivo de reglas.

## 8.2 Herramientas de Python

Las herramientas de `tools/` se revisaron con **pyflakes**: no quedan imports ni variables sin usar. Las
plantillas de estructuras usan `from estructuras_base import *` a propósito: es un pequeño lenguaje para
describir edificios (bloques, paredes, techos).

## 8.3 Buenas prácticas aplicadas

- **Separación en capas.** El motor del truco (`core`) no conoce Minecraft, la lógica de servidor no conoce las pantallas, y las clases de cliente están marcadas con `@OnlyIn(Dist.CLIENT)` o se cargan por `DistExecutor`. Así el servidor dedicado arranca sin código de pantalla.
- **Validación de toda entrada del cliente.** Cada paquete limita tamaños y el servidor valida asiento, turno, distancia y legalidad (ver RNF-S1 a RNF-S5).
- **Manejo de errores.** Las escenas, cinemáticas y efectos están protegidos con `try/catch`: si algo falla, se corta esa escena, se registra en el log y el juego sigue. Los errores de generación del mundo se registran sin tumbar el servidor.
- **Sin credenciales ni secretos.** El repositorio no contiene claves ni tokens, y el mod no se conecta a internet mientras se juega.
- **Pruebas automáticas e integración continua:** 33 pruebas en cada push (GitHub Actions).
- **Nombres y comentarios.** Clases y métodos con nombres descriptivos. El código de Minecraft sigue los nombres oficiales (en inglés); las partes propias del juego van en español rioplatense, como el resto del proyecto.
