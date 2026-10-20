package com.trucoya.progreso;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.trucoya.TrucoYa;
import com.trucoya.core.Prestigio;
import com.trucoya.entity.GauchoEntity;
import com.trucoya.registry.ModEntities;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import com.trucoya.registry.ModItems;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.Locale;

/**
 * Comandos:
 * <pre>
 *   /truco prestigio                        muestra tu Prestigio Criollo y estadísticas
 *   /truco prestigio set &lt;jugador&gt; &lt;pts&gt;   (operadores) fija el prestigio
 *   /truco gaucho &lt;rol&gt; [nivel]             (operadores) hace aparecer un gaucho
 *   /truco completar [todo]                 (operadores) llena la Libreta del Truquero
 *   /truco salamanca visita|partida|cancelar (operadores) prueba La Salamanca
 *   /truco pedidos [cancelar &lt;n&gt;]           tus pedidos (hasta tres) y cancelar uno
 *   /truco baraja [&lt;clave&gt;]                 tu colección de barajas y elegir con cuál jugás
 *   /truco trailer marcar|borrar|parar|[seg] (operadores o dueño del mundo) modo tráiler para grabar
 * </pre>
 */
@Mod.EventBusSubscriber(modid = TrucoYa.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TrucoCommands {

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSource> d) {
        d.register(Commands.literal("truco")
                .then(Commands.literal("prestigio")
                        .executes(ctx -> mostrar(ctx.getSource()))
                        .then(Commands.literal("set").requires(src -> src.hasPermission(2))
                                .then(Commands.argument("jugador", EntityArgument.player())
                                        .then(Commands.argument("puntos", IntegerArgumentType.integer(0, 100000))
                                                .executes(ctx -> {
                                                    ServerPlayerEntity p = EntityArgument.getPlayer(ctx, "jugador");
                                                    int pts = IntegerArgumentType.getInteger(ctx, "puntos");
                                                    JugadorData.set(p, JugadorData.PRESTIGIO, pts);
                                                    ctx.getSource().sendSuccess(new StringTextComponent(
                                                            p.getGameProfile().getName() + ": " + Prestigio.describir(pts)), true);
                                                    return pts;
                                                })))))
                .then(Commands.literal("gaucho").requires(src -> src.hasPermission(2))
                        .then(Commands.argument("rol", StringArgumentType.word())
                                .suggests((ctx, b) -> ISuggestionProvider.suggest(Arrays.asList("paisano", "paisana", "cantinero", "forastero", "vizcacha", "matrero", "peon", "payador", "fundador"), b))
                                .executes(ctx -> gaucho(ctx.getSource(), StringArgumentType.getString(ctx, "rol"), 0))
                                .then(Commands.argument("nivel", IntegerArgumentType.integer(1, 5))
                                        .executes(ctx -> gaucho(ctx.getSource(), StringArgumentType.getString(ctx, "rol"),
                                                IntegerArgumentType.getInteger(ctx, "nivel"))))))
                // /truco voz <hombre|gaucho|viejo|mujer|paisana>: la voz con la que cantás
                .then(Commands.literal("voz")
                        .then(Commands.argument("voz", StringArgumentType.word())
                                .suggests((ctx, b) -> ISuggestionProvider.suggest(
                                        Arrays.asList("hombre", "gaucho", "viejo", "mujer", "paisana"), b))
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayerOrException();
                                    com.trucoya.core.Voz v = com.trucoya.core.Voz.porNombre(
                                            StringArgumentType.getString(ctx, "voz"));
                                    JugadorData.set(p, JugadorData.VOZ, v.ordinal());
                                    ctx.getSource().sendSuccess(new StringTextComponent(
                                            "Ahora cantás con la voz: " + v.clave()), false);
                                    return 1;
                                })))
                // /truco guardia: un matrero armado que cuida el lugar donde aparece (para probar)
                .then(Commands.literal("guardia").requires(src -> src.hasPermission(2))
                        .executes(ctx -> guardia(ctx.getSource())))
                // /truco cinematica: volver a ver "El Último Truco" (con el Vizcacha más cercano, si hay)
                // /truco pedidos: los pedidos de los paisanos que aceptaste
                .then(Commands.literal("pedidos")
                        .executes(ctx -> pedidos(ctx.getSource().getPlayerOrException()))
                        .then(Commands.literal("cancelar")
                                .then(Commands.argument("n", IntegerArgumentType.integer(1, Pedidos.MAXIMO)).executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayerOrException();
                                    if (!Pedidos.cancelarEn(p, IntegerArgumentType.getInteger(ctx, "n") - 1)) {
                                        ctx.getSource().sendFailure(new StringTextComponent("No tenés ese pedido."));
                                        return 0;
                                    }
                                    return 1;
                                })))
                        .then(Commands.literal("aceptar")
                                .then(Commands.argument("gaucho", StringArgumentType.word()).executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayerOrException();
                                    java.util.UUID id;
                                    try {
                                        id = java.util.UUID.fromString(StringArgumentType.getString(ctx, "gaucho"));
                                    } catch (IllegalArgumentException e) {
                                        return 0;
                                    }
                                    for (GauchoEntity g : p.getLevel().getEntitiesOfClass(GauchoEntity.class,
                                            p.getBoundingBox().inflate(10.0D))) {
                                        if (g.getUUID().equals(id) && g.pedidoOfrecido() != null && g.puedePedir()
                                                && com.trucoya.TrucoConfig.PEDIDOS.get()) {
                                            Pedidos.aceptar(g, p, g.pedidoOfrecido());
                                            return 1;
                                        }
                                    }
                                    ctx.getSource().sendFailure(new StringTextComponent("Tenés que estar cerca del paisano."));
                                    return 0;
                                }))))
                // /truco baraja [clave]: la colección y con cuál jugás
                .then(Commands.literal("baraja")
                        .executes(ctx -> barajas(ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("clave", StringArgumentType.word())
                                .suggests((ctx, b) -> ISuggestionProvider.suggest(Arrays.stream(com.trucoya.barajas.Baraja.values())
                                        .map(x -> x.clave), b))
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayerOrException();
                                    String c = StringArgumentType.getString(ctx, "clave");
                                    for (com.trucoya.barajas.Baraja b : com.trucoya.barajas.Baraja.values()) {
                                        if (b.clave.equalsIgnoreCase(c)) {
                                            if (!com.trucoya.barajas.Coleccion.tiene(p, b)) {
                                                ctx.getSource().sendFailure(new StringTextComponent("Todavía no la tenés. " + b.comoSeGana));
                                                return 0;
                                            }
                                            com.trucoya.barajas.Coleccion.elegir(p, b);
                                            return 1;
                                        }
                                    }
                                    ctx.getSource().sendFailure(new StringTextComponent("No existe esa baraja."));
                                    return 0;
                                })))
                // /truco trailer: la cámara de cine recorre las marcas, con carteles y música, para grabar el tráiler
                .then(Commands.literal("trailer")
                        .requires(src -> src.hasPermission(2) || (src.getEntity() instanceof ServerPlayerEntity
                                && src.getServer().isSingleplayerOwner(((ServerPlayerEntity) src.getEntity()).getGameProfile())))
                        .executes(ctx -> trailer(ctx.getSource(), 6.0F))
                        .then(Commands.argument("segundos", com.mojang.brigadier.arguments.FloatArgumentType.floatArg(2.0F, 30.0F))
                                .executes(ctx -> trailer(ctx.getSource(),
                                        com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "segundos"))))
                        .then(Commands.literal("marcar").executes(ctx ->
                                com.trucoya.trailer.TrailerServidor.marcar(ctx.getSource().getPlayerOrException())))
                        .then(Commands.literal("borrar").executes(ctx -> {
                            com.trucoya.trailer.TrailerServidor.borrar(ctx.getSource().getPlayerOrException());
                            return 1;
                        }))
                        .then(Commands.literal("parar").executes(ctx -> {
                            com.trucoya.trailer.TrailerServidor.parar(ctx.getSource().getPlayerOrException());
                            return 1;
                        })))
                .then(Commands.literal("cinematica")
                        .executes(ctx -> cinematica(ctx.getSource()))
                        .then(Commands.literal("final").executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayerOrException();
                            if (JugadorData.get(p, Historia.FINAL_VISTO) == 0 && !p.isCreative()) {
                                ctx.getSource().sendFailure(new StringTextComponent(
                                        "El final se gana: primero tenés que ganarle al Viejo Vizcacha."));
                                return 0;
                            }
                            if (!CinematicaServidor.enCurso(p) && puedeVerCinematica(ctx.getSource(), p)) {
                                CinematicaServidor.iniciarFinal(p);
                            }
                            return 1;
                        })))
                // /truco historia: abre la Libreta del Truquero (y te da una si la perdiste)
                .then(Commands.literal("historia").executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayerOrException();
                    if (!p.inventory.contains(new net.minecraft.item.ItemStack(com.trucoya.registry.ModItems.LIBRETA.get()))) {
                        p.inventory.add(new net.minecraft.item.ItemStack(com.trucoya.registry.ModItems.LIBRETA.get()));
                    }
                    Historia.abrirLibreta(p);
                    return 1;
                }))
                // /truco salamanca visita|partida|cancelar: (operadores) para probar La Salamanca
                .then(Commands.literal("salamanca").requires(src -> src.hasPermission(2))
                        .then(Commands.literal("visita").executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayerOrException();
                            if (Salamanca.etapa(p) < Salamanca.ESPERA) JugadorData.set(p, Salamanca.ETAPA, Salamanca.ESPERA);
                            String no = Salamanca.visita(p);
                            if (no != null) ctx.getSource().sendFailure(new StringTextComponent(no));
                            return no == null ? 1 : 0;
                        }))
                        .then(Commands.literal("partida").executes(ctx -> {
                            String no = Salamanca.irAlPaso(ctx.getSource().getPlayerOrException());
                            if (no != null) ctx.getSource().sendFailure(new StringTextComponent(no));
                            return no == null ? 1 : 0;
                        }))
                        .then(Commands.literal("cancelar").executes(ctx -> {
                            Salamanca.cancelar("comando");
                            return 1;
                        })))
                // /truco completar [todo]: (operadores) llena la Libreta del Truquero de una
                .then(Commands.literal("completar").requires(src -> src.hasPermission(2))
                        .executes(ctx -> completar(ctx.getSource(), false))
                        .then(Commands.literal("todo").executes(ctx -> completar(ctx.getSource(), true))))
                // /truco sello <region>: (operadores) para probar la historia
                .then(Commands.literal("sello").requires(src -> src.hasPermission(2))
                        .then(Commands.argument("region", StringArgumentType.word())
                                .suggests((ctx, b) -> ISuggestionProvider.suggest(Arrays.asList("llanura", "sabana",
                                        "desierto", "monte", "nieve", "sierra", "todos"), b))
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayerOrException();
                                    String r = StringArgumentType.getString(ctx, "region").toUpperCase(Locale.ROOT);
                                    for (Historia.Region reg : Historia.Region.values()) {
                                        if (r.equals("TODOS") || reg.name().equals(r)) Historia.ganoEnPulperia(p, reg);
                                    }
                                    return 1;
                                }))));
    }

    /**
     * Llena la Libreta: nivel para el Vizcacha, los seis sellos, un torneo y una cuadrera ganados (con la copa y
     * el banderín) y el Mapa del Baqueano en la mano. Con "todo", además queda el Vizcacha vencido y el final visto.
     * No manda a los Fundadores de visita (sería una cola de seis visitas seguidas).
     */
    private static int completar(CommandSource src, boolean todo) throws CommandSyntaxException {
        ServerPlayerEntity p = src.getPlayerOrException();
        int minimo = Prestigio.UMBRAL[Historia.NIVEL_VIZCACHA - 1];
        if (JugadorData.prestigio(p) < minimo) {
            JugadorData.addPrestigio(p, minimo - JugadorData.prestigio(p), "Libreta completa");
        }
        JugadorData.set(p, Historia.SELLOS, Historia.TODOS_LOS_SELLOS);
        Logros.award(p, Logros.PRIMER_SELLO);
        Logros.award(p, Logros.RUTA_COMPLETA);
        if (JugadorData.get(p, Historia.TORNEOS) == 0) JugadorData.set(p, Historia.TORNEOS, 1);
        Logros.award(p, Logros.CAMPEON_TORNEO);
        if (JugadorData.get(p, Historia.CUADRERAS) == 0) JugadorData.set(p, Historia.CUADRERAS, 1);
        Logros.award(p, Logros.PINGO_GANADOR);

        dar(p, new ItemStack(ModItems.LIBRETA.get()), true);
        dar(p, new ItemStack(ModItems.COPA_TORNEO.get()), true);
        dar(p, new ItemStack(ModItems.RAYA_CUADRERA.get()), true);
        dar(p, new ItemStack(Items.EMERALD, 5), false);

        ItemStack mapa = Historia.crearMapa(p);
        boolean conMapa = !mapa.isEmpty();
        if (conMapa) {
            JugadorData.set(p, Historia.MAPA, 1);
            Logros.award(p, Logros.MAPA_BAQUEANO);
            dar(p, mapa, false);
        }
        if (todo) {
            JugadorData.set(p, Historia.VIZCACHA_VENCIDO, 1);
            JugadorData.set(p, Historia.FINAL_VISTO, 1);
            Logros.award(p, Logros.EL_FINAL);
            Salamanca.vioElFinal(p);
            com.trucoya.barajas.Coleccion.ganar(p, com.trucoya.barajas.Baraja.ORO);
            com.trucoya.barajas.Coleccion.ganar(p, com.trucoya.barajas.Baraja.FUNDADORES);
        }
        p.sendMessage(new StringTextComponent("✦ Libreta completa ✦  seis sellos, nivel " + JugadorData.nivel(p)
                + ", torneo, cuadrera" + (conMapa ? " y el Mapa del Baqueano." : ".")
                + (todo ? " El Vizcacha queda vencido." : " Ahora andá a buscar al Viejo Vizcacha."))
                .withStyle(TextFormatting.GOLD), p.getUUID());
        if (!conMapa) {
            src.sendFailure(new StringTextComponent("No encontré la Estancia del Caudillo en este mundo, así que no hay mapa."));
        }
        Historia.abrirLibreta(p);
        return 1;
    }

    /** Pone algo en el inventario (o a los pies si está lleno); si {@code unico}, solo si no tiene uno. */
    private static void dar(ServerPlayerEntity p, ItemStack stack, boolean unico) {
        if (unico && p.inventory.contains(stack)) {
            return;
        }
        if (!p.inventory.add(stack)) {
            p.drop(stack, false);
        }
    }

    private static int mostrar(CommandSource src) throws CommandSyntaxException {
        ServerPlayerEntity p = src.getPlayerOrException();
        int pts = JugadorData.prestigio(p);
        src.sendSuccess(new StringTextComponent("Prestigio Criollo: " + Prestigio.describir(pts)
                + (Prestigio.faltanParaSubir(pts) > 0 ? " · faltan " + Prestigio.faltanParaSubir(pts) + " para subir" : "")), false);
        src.sendSuccess(new StringTextComponent("Partidos ganados: " + JugadorData.get(p, JugadorData.GANADOS)
                + " · perdidos: " + JugadorData.get(p, JugadorData.PERDIDOS)
                + " · racha: " + JugadorData.get(p, JugadorData.RACHA)
                + " · flores: " + JugadorData.get(p, JugadorData.FLORES)
                + " · faroles: " + JugadorData.get(p, JugadorData.FAROLES)), false);
        return pts;
    }

    private static int gaucho(CommandSource src, String rolName, int nivel) throws CommandSyntaxException {
        ServerPlayerEntity p = src.getPlayerOrException();
        ServerWorld world = p.getLevel();
        GauchoEntity g = ModEntities.GAUCHO.get().create(world);
        if (g == null) {
            return 0;
        }
        GauchoEntity.Rol rol = GauchoEntity.Rol.byName(rolName.toUpperCase(Locale.ROOT));
        g.setRol(rol);
        if (nivel > 0) g.setNivel(nivel);
        g.moveTo(p.getX(), p.getY(), p.getZ(), p.yRot + 180F, 0F);
        g.finalizeSpawn(world, world.getCurrentDifficultyAt(p.blockPosition()), SpawnReason.COMMAND, null, null);
        g.initDefaults();
        world.addFreshEntity(g);
        src.sendSuccess(new StringTextComponent("Apareció " + g.nombre() + " (Nv. " + g.getNivel() + ")"), true);
        return 1;
    }

    /** Cuándo puede volver a usar /truco cinematica cada jugador que no es operador. */
    private static final java.util.Map<java.util.UUID, Long> PROXIMA_CINEMATICA = new java.util.HashMap<>();

    static boolean puedeVerCinematica(CommandSource src, ServerPlayerEntity p) {
        if (src.hasPermission(2)) {
            return true;
        }
        long ahora = p.getServer().getTickCount();
        Long cuando = PROXIMA_CINEMATICA.get(p.getUUID());
        if (cuando != null && ahora < cuando) {
            src.sendFailure(new StringTextComponent("Esperá un rato para volver a verla (" + (1 + (cuando - ahora) / 1200) + " min)."));
            return false;
        }
        PROXIMA_CINEMATICA.put(p.getUUID(), ahora + 1200L * com.trucoya.TrucoConfig.ESPERA_CINEMATICA.get());
        return true;
    }

    private static int cinematica(CommandSource src) throws CommandSyntaxException {
        ServerPlayerEntity p = src.getPlayerOrException();
        if (CinematicaServidor.enCurso(p) || !puedeVerCinematica(src, p)) {
            return 0;
        }
        GauchoEntity viz = null;
        double mejor = 24.0D * 24.0D;
        for (GauchoEntity g : p.getLevel().getEntitiesOfClass(GauchoEntity.class, p.getBoundingBox().inflate(24.0D))) {
            if (g.getRol() == GauchoEntity.Rol.VIZCACHA && g.distanceToSqr(p) < mejor) {
                mejor = g.distanceToSqr(p);
                viz = g;
            }
        }
        JugadorData.set(p, CinematicaServidor.VISTA, 1);
        CinematicaServidor.iniciar(p, viz);
        return 1;
    }

    private static int guardia(CommandSource src) throws CommandSyntaxException {
        ServerPlayerEntity p = src.getPlayerOrException();
        ServerWorld world = p.getLevel();
        GauchoEntity g = ModEntities.GAUCHO.get().create(world);
        if (g == null) {
            return 0;
        }
        g.setRol(GauchoEntity.Rol.MATRERO);
        g.setNivel(4);
        g.moveTo(p.getX(), p.getY(), p.getZ(), p.yRot + 180F, 0F);
        g.finalizeSpawn(world, world.getCurrentDifficultyAt(p.blockPosition()), SpawnReason.COMMAND, null, null);
        g.initDefaults();
        g.setGuardia(true);
        world.addFreshEntity(g);
        src.sendSuccess(new StringTextComponent("Apareció " + g.nombre() + ", guardia de la clandestina. "
                + "Sin Ficha de Clandestina (o Nivel 3) te va a atacar."), true);
        return 1;
    }

    private TrucoCommands() {
    }

    private static int pedidos(ServerPlayerEntity p) {
        java.util.List<String> l = Pedidos.paraLibreta(p);
        if (l.isEmpty()) {
            p.sendMessage(new StringTextComponent("No tenés pedidos. Buscá paisanos con \"¡Pedido!\" sobre la cabeza.")
                    .withStyle(TextFormatting.GRAY), p.getUUID());
            return 0;
        }
        for (int i = 0; i < l.size(); i++) {
            String[] partes = l.get(i).split("\n");
            p.sendMessage(new StringTextComponent((i + 1) + ". " + partes[0]).withStyle(TextFormatting.YELLOW), p.getUUID());
            for (int k = 1; k < partes.length; k++) {
                p.sendMessage(new StringTextComponent("   " + partes[k]).withStyle(TextFormatting.GRAY), p.getUUID());
            }
        }
        return l.size();
    }

    private static int barajas(ServerPlayerEntity p) {
        com.trucoya.barajas.Baraja elegida = com.trucoya.barajas.Coleccion.elegida(p);
        p.sendMessage(new StringTextComponent("Tus barajas (" + com.trucoya.barajas.Coleccion.cantidad(p) + " de "
                + com.trucoya.barajas.Baraja.values().length + "):").withStyle(TextFormatting.GOLD), p.getUUID());
        for (com.trucoya.barajas.Baraja b : com.trucoya.barajas.Baraja.values()) {
            boolean tiene = com.trucoya.barajas.Coleccion.tiene(p, b);
            p.sendMessage(new StringTextComponent((b == elegida ? " ▶ " : tiene ? " ✔ " : " · ") + b.nombre
                    + (tiene ? "" : "  — " + b.comoSeGana)).withStyle(tiene ? TextFormatting.WHITE : TextFormatting.DARK_GRAY), p.getUUID());
        }
        return 1;
    }

    private static int trailer(CommandSource src, float segundos) throws CommandSyntaxException {
        String no = com.trucoya.trailer.TrailerServidor.empezar(src.getPlayerOrException(), segundos);
        if (no != null) {
            src.sendFailure(new StringTextComponent(no));
            return 0;
        }
        return 1;
    }
}
