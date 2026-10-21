# Truco Ya en un servidor

Para jugar con amigos, cada uno en su compu, en el mismo mundo.

## Armar el servidor (en la compu que va a ser el servidor)

1. Corré `probar.bat` una vez, si nunca lo corriste. Baja el JDK 8.
2. Corré **`servidor.bat`**. Hace todo esto solo:
   - compila el mod;
   - baja e instala Forge 36.2.39 para servidor;
   - consigue GeckoLib 3.0.106;
   - arma la carpeta `servidor/`.
3. La primera vez pregunta si aceptás la [EULA de Minecraft](https://aka.ms/MinecraftEULA). Sin aceptarla, Minecraft no deja abrir servidores.
4. Para arrancarlo, corré `servidor\iniciar.bat`. Para cerrarlo, escribí `stop` en esa ventana.

Cada vez que llegue una actualización del mod, primero corré `actualizar.bat` y después otra vez `servidor.bat`. Reemplaza el mod viejo y no toca el mundo.

## Lo que necesita cada jugador

- Minecraft Java 1.16.5 con **Forge 36.2.39**: el instalador está en <https://files.minecraftforge.net>, versión 1.16.5 → 36.2.39, instalador "Installer → Install client".
- Los dos `.jar` de `servidor\para_los_jugadores`: `trucoya-1.7.2.jar` y `geckolib-forge-1.16.5-3.0.106.jar`. Van en la carpeta `mods` de su `.minecraft`. En Windows es `%appdata%\.minecraft\mods`.
- Tienen que tener **la misma versión** del mod que el servidor; si no, el juego no los deja entrar.

## Conectarse

- **Misma casa o misma red WiFi:** en Multijugador → Agregar servidor, se pone la IP local de la compu servidor (por ejemplo `192.168.1.20`). Se ve con `ipconfig`, en "Dirección IPv4".
- **Desde otras casas:** hay que abrir el puerto 25565 en el router de la compu servidor y pasar la IP pública. Otra opción más fácil es un programa de red virtual como Radmin VPN o ZeroTier: todos entran a la misma red y usan la IP que les muestra el programa.
- Para dar permisos de operador (comandos de prueba como `/truco completar`), escribí en la ventana del servidor: `op NombreDelJugador`.

## Opciones del mod en el servidor

Están en `servidor\world\serverconfig\trucoya-server.toml`. Se crea al arrancar por primera vez.

| Opción | Qué hace |
| --- | --- |
| `salamanca.cambiaElCielo` | `SOLO` (recomendado): la Salamanca hace de noche y trae lluvia solo si no hay otros jugadores lejos en el mundo de arriba. `SIEMPRE`: siempre. `NUNCA`: nunca; la historia se juega igual, con el cielo que haya. |
| `pedidos.activados` | Si los paisanos ofrecen pedidos. |
| `comandos.esperaCinematica` | Minutos de espera entre una y otra vez que alguien que no es operador usa `/truco cinematica`. |

## Cómo se comporta en multijugador

- La historia (sellos, nivel, barajas, pedidos, la Salamanca) es **de cada jugador**: cada uno tiene su Libreta.
- Una mesa se puede jugar entre humanos (1 contra 1 o 2 contra 2) o con paisanos. Cada uno ve solo sus cartas.
- Las cartas sobre la mesa se ven desde afuera, para todos. La baraja la pone el que arma la partida.
- La Salamanca la juega un jugador por vez en todo el servidor. Si otro la quiere empezar, le avisa que espere.
- Las barajas de región y la criolla se pueden regalar o cambiar. Las de hazaña (Oro, Mandinga y Fundadores) solo le sirven a quien las ganó.
- Mientras alguien mira una cinemática no lo pueden lastimar, pero él tampoco puede pegarle a nadie.
