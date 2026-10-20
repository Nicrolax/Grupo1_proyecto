package com.trucoya.progreso;

import com.trucoya.entity.GauchoEntity;
import com.trucoya.registry.ModItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Los pedidos de los paisanos: un gaucho con "¡Pedido!" sobre la cabeza te pide algo. Tres clases:
 * <ul>
 *   <li>TRAER: que le traigas cosas (queso, cuero, leña...) y se las des en mano;</li>
 *   <li>LLEVAR: que le lleves una encomienda a cualquier paisano de otra región;</li>
 *   <li>GANAR: un desafío (ganar en una pulpería de cierta región, cantar una flor, ganar una cuadrera...).</li>
 * </ul>
 * Cada jugador tiene hasta tres pedidos a la vez, guardados en sus datos persistentes. El pedido que ofrece cada
 * gaucho lo guarda el gaucho (ver {@link GauchoEntity}); varios jugadores pueden aceptar el mismo.
 */
public final class Pedidos {

    public static final int MAXIMO = 3;
    public static final String LISTA = "trucoya_pedidos";
    public static final String CUMPLIDOS = "pedidos_cumplidos";
    /** Los últimos pedidos cumplidos o cancelados (Ids): no se pueden volver a aceptar. */
    public static final String HECHOS = "trucoya_pedidos_hechos";

    public static final int TRAER = 0, LLEVAR = 1, GANAR = 2;
    /** Desafíos de GANAR. */
    public static final int RETO_PULPERIA = 0, RETO_FLOR = 1, RETO_CUADRERA = 2, RETO_FORASTERO = 3, RETO_PARTIDOS = 4;

    /** Lo que pueden pedir que les traigas: ítem, cantidad, esmeraldas de premio y cómo lo piden. */
    private static final class Cosa {
        final Supplier<Item> item;
        final int cant;
        final int premio;
        final String frase;

        Cosa(Supplier<Item> item, int cant, int premio, String frase) {
            this.item = item;
            this.cant = cant;
            this.premio = premio;
            this.frase = frase;
        }
    }

    private static final Cosa[] COSAS = {
            new Cosa(() -> ModItems.QUESO.get(), 3, 4, "Se me acabó el queso y esta noche viene gente a la picada."),
            new Cosa(() -> ModItems.SALAME.get(), 2, 4, "¿No tenés un salame? Hace días que como galleta sola."),
            new Cosa(() -> ModItems.YERBA_MATE.get(), 4, 5, "Me quedé sin yerba, paisano. Y un gaucho sin mate no es nadie."),
            new Cosa(() -> ModItems.VINO_TINTO.get(), 2, 4, "Para el asado del domingo me faltan un par de botellas de vino."),
            new Cosa(() -> Items.LEATHER, 6, 4, "Tengo que hacer unas riendas nuevas y me falta cuero."),
            new Cosa(() -> Items.WHITE_WOOL, 8, 3, "La patrona quiere tejer un poncho para el invierno. Le falta lana."),
            new Cosa(() -> Items.OAK_LOG, 16, 3, "Se vienen las heladas y no tengo leña para el fogón."),
            new Cosa(() -> Items.EGG, 12, 3, "Las gallinas no ponen y quiero hacer tortas fritas."),
            new Cosa(() -> Items.COOKED_BEEF, 8, 5, "Prometí un asado y la carne no alcanza."),
            new Cosa(() -> Items.BREAD, 6, 3, "¿Me conseguís pan? El horno de barro se me rajó."),
            new Cosa(() -> Items.IRON_INGOT, 6, 6, "Mi caballo perdió las herraduras. Necesito hierro."),
            new Cosa(() -> Items.HONEY_BOTTLE, 2, 5, "Con miel se hace la grapamiel. ¿Me traés un par de frascos?"),
            new Cosa(() -> Items.PUMPKIN, 4, 3, "Quiero hacer un puchero con zapallo como el de mi madre."),
            new Cosa(() -> Items.COOKED_COD, 6, 4, "Hace días que tengo ganas de pescado frito."),
    };

    /** Último pedido mostrado a cada jugador (para aceptarlo con un segundo clic). */
    private static final Map<UUID, Long> MOSTRADO = new HashMap<>();
    private static final Map<UUID, UUID> MOSTRADO_DE = new HashMap<>();
    /** Último clic de cada jugador a un paisano (mantener apretado el botón repite el clic cada 4 ticks). */
    private static final Map<UUID, Long> ULTIMO_CLIC = new HashMap<>();

    private Pedidos() {
    }

    // ------------------------------------------------------------------ el pedido que ofrece un gaucho

    /** Arma un pedido nuevo para que lo ofrezca este gaucho. */
    public static CompoundNBT crear(GauchoEntity g, Random r) {
        CompoundNBT t = new CompoundNBT();
        t.putInt("Id", r.nextInt(Integer.MAX_VALUE));
        t.putUUID("De", g.getUUID());
        t.putString("Nombre", g.nombre());
        t.putLong("Donde", g.blockPosition().asLong());
        Historia.Region aca = Historia.Region.de(g.level.getBiome(g.blockPosition()).getBiomeCategory());
        int tipo = r.nextInt(10) < 5 ? TRAER : r.nextInt(2) == 0 ? LLEVAR : GANAR;
        t.putInt("Tipo", tipo);
        if (tipo == TRAER) {
            int i = r.nextInt(COSAS.length);
            Cosa c = COSAS[i];
            t.putInt("Cosa", i);
            t.putInt("Meta", c.cant);
            t.putInt("Esmeraldas", c.premio);
            t.putInt("Prestigio", 20);
        } else if (tipo == LLEVAR) {
            Historia.Region[] rs = Historia.Region.values();
            Historia.Region destino = rs[r.nextInt(rs.length)];
            if (destino == aca) destino = rs[(destino.ordinal() + 1 + r.nextInt(rs.length - 1)) % rs.length];
            t.putInt("Region", destino.ordinal());
            t.putInt("Meta", 1);
            t.putInt("Esmeraldas", 7 + r.nextInt(5));
            t.putInt("Prestigio", 45);
        } else {
            int reto = r.nextInt(5);
            t.putInt("Reto", reto);
            Historia.Region[] rs = Historia.Region.values();
            t.putInt("Region", rs[r.nextInt(rs.length)].ordinal());
            t.putInt("Meta", reto == RETO_PARTIDOS ? 3 : 1);
            t.putInt("Esmeraldas", reto == RETO_PULPERIA ? 8 : reto == RETO_PARTIDOS ? 7 : 5);
            t.putInt("Prestigio", reto == RETO_FLOR ? 30 : 50);
        }
        return t;
    }

    /** Lo que dice el gaucho al ofrecerlo. */
    public static String frase(CompoundNBT t) {
        switch (t.getInt("Tipo")) {
            case TRAER: {
                Cosa c = COSAS[Math.floorMod(t.getInt("Cosa"), COSAS.length)];
                return c.frase + " Traeme " + c.cant + " de " + nombreItem(c) + ".";
            }
            case LLEVAR:
                return "Tengo una encomienda para un pariente en " + region(t).nombre
                        + ". ¿Se la llevás? Dásela a cualquier paisano de allá.";
            default:
                return reto(t, true);
        }
    }

    /** Una línea corta para la Libreta. */
    public static String titulo(CompoundNBT t) {
        switch (t.getInt("Tipo")) {
            case TRAER: {
                Cosa c = COSAS[Math.floorMod(t.getInt("Cosa"), COSAS.length)];
                return "Traerle " + c.cant + " de " + nombreItem(c) + " a " + t.getString("Nombre");
            }
            case LLEVAR:
                return "Llevar la encomienda de " + t.getString("Nombre") + " a " + region(t).nombre;
            default:
                return reto(t, false) + (t.getInt("Meta") > 1 ? " (" + t.getInt("Avance") + "/" + t.getInt("Meta") + ")" : "");
        }
    }

    private static String reto(CompoundNBT t, boolean dicho) {
        switch (t.getInt("Reto")) {
            case RETO_PULPERIA:
                return dicho ? "Dicen que en " + region(t).nombre + " nadie les gana a los de la pulpería. Andá y demostrales que se equivocan."
                        : "Ganar un partido en una pulpería de " + region(t).nombre;
            case RETO_FLOR:
                return dicho ? "Quiero ver si sos de los que cantan flor. Cantá una en una partida y te pago."
                        : "Cantar una flor en una partida";
            case RETO_CUADRERA:
                return dicho ? "Aposté a que le ganás a cualquiera en una cuadrera. No me hagas quedar mal."
                        : "Ganar una carrera cuadrera";
            case RETO_FORASTERO:
                return dicho ? "Anda un forastero sacándole plata a todos. Ganale un partido y bajale el copete."
                        : "Ganarle un partido a un forastero";
            default:
                return dicho ? "Ganá tres partidos de truco y te convido con lo que tengo."
                        : "Ganar tres partidos de truco";
        }
    }

    private static Historia.Region region(CompoundNBT t) {
        Historia.Region[] rs = Historia.Region.values();
        return rs[Math.floorMod(t.getInt("Region"), rs.length)];
    }

    private static String nombreItem(Cosa c) {
        return new ItemStack(c.item.get()).getHoverName().getString();
    }

    // ------------------------------------------------------------------ los pedidos de cada jugador

    private static CompoundNBT datos(PlayerEntity p) {
        CompoundNBT pers = p.getPersistentData();
        if (!pers.contains(PlayerEntity.PERSISTED_NBT_TAG)) {
            pers.put(PlayerEntity.PERSISTED_NBT_TAG, new CompoundNBT());
        }
        return pers.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
    }

    private static boolean hecho(PlayerEntity p, int id) {
        for (int x : datos(p).getIntArray(HECHOS)) {
            if (x == id) return true;
        }
        return false;
    }

    private static void anotarHecho(PlayerEntity p, int id) {
        int[] viejos = datos(p).getIntArray(HECHOS);
        int n = Math.min(viejos.length, 47);
        int[] nuevos = new int[n + 1];
        System.arraycopy(viejos, viejos.length - n, nuevos, 0, n);
        nuevos[n] = id;
        datos(p).putIntArray(HECHOS, nuevos);
    }

    public static ListNBT lista(PlayerEntity p) {
        CompoundNBT d = datos(p);
        if (!d.contains(LISTA, 9)) {
            d.put(LISTA, new ListNBT());
        }
        return d.getList(LISTA, 10);
    }

    private static boolean yaTiene(PlayerEntity p, CompoundNBT oferta) {
        if (hecho(p, oferta.getInt("Id"))) {
            return true;
        }
        for (int i = 0; i < lista(p).size(); i++) {
            if (lista(p).getCompound(i).getInt("Id") == oferta.getInt("Id")) return true;
        }
        return false;
    }

    /**
     * Clic derecho a un gaucho (sin agacharse). Devuelve true si la charla fue por un pedido (entregar, aceptar o
     * mostrar uno) y no hay que hacer lo de siempre (invitarlo a la mesa).
     */
    public static boolean hablar(GauchoEntity g, ServerPlayerEntity p, @Nullable CompoundNBT oferta) {
        long ahora = p.level.getGameTime();
        Long ultimo = ULTIMO_CLIC.put(p.getUUID(), ahora);
        if (ultimo != null && ahora - ultimo < 8) {
            return true;    // el botón sigue apretado: no es un clic nuevo
        }
        if (entregar(g, p)) {
            return true;
        }
        if (oferta == null) {
            return false;
        }
        if (yaTiene(p, oferta)) {
            if (!hecho(p, oferta.getInt("Id")) && oferta.getInt("Tipo") == TRAER) {
                g.say("¿Y? ¿Me trajiste lo que te pedí?");
                return true;
            }
            return false;
        }
        Long antes = MOSTRADO.get(p.getUUID());
        if (antes != null && ahora - antes >= 8 && ahora - antes < 200 && g.getUUID().equals(MOSTRADO_DE.get(p.getUUID()))) {
            MOSTRADO.remove(p.getUUID());
            aceptar(g, p, oferta);
            return true;
        }
        MOSTRADO.put(p.getUUID(), ahora);
        MOSTRADO_DE.put(p.getUUID(), g.getUUID());
        g.say(frase(oferta));
        IFormattableTextComponent aceptar = new StringTextComponent(" [Aceptar]").withStyle(s -> s
                .withColor(TextFormatting.GREEN).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/truco pedidos aceptar " + g.getUUID()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("Aceptar el pedido"))));
        p.sendMessage(new StringTextComponent(g.nombre() + ": ").withStyle(TextFormatting.GOLD)
                .append(new StringTextComponent(frase(oferta)).withStyle(TextFormatting.WHITE)), p.getUUID());
        p.sendMessage(new StringTextComponent("  Premio: " + oferta.getInt("Esmeraldas") + " esmeraldas y "
                + oferta.getInt("Prestigio") + " de prestigio. Volvé a hacerle clic para aceptar,").withStyle(TextFormatting.GRAY)
                .append(aceptar).append(new StringTextComponent("  (agachado: invitarlo a jugar)").withStyle(TextFormatting.DARK_GRAY)),
                p.getUUID());
        return true;
    }

    public static void aceptar(GauchoEntity g, ServerPlayerEntity p, CompoundNBT oferta) {
        if (yaTiene(p, oferta)) {
            p.displayClientMessage(new StringTextComponent("Ese pedido ya lo tenés.").withStyle(TextFormatting.GRAY), true);
            return;
        }
        ListNBT l = lista(p);
        if (l.size() >= MAXIMO) {
            p.sendMessage(new StringTextComponent("Ya tenés " + MAXIMO + " pedidos. Cumplí alguno (o cancelalo desde la Libreta).")
                    .withStyle(TextFormatting.RED), p.getUUID());
            return;
        }
        CompoundNBT t = oferta.copy();
        t.putInt("Avance", 0);
        l.add(t);
        g.say("¡Trato hecho, " + p.getName().getString() + "!");
        p.sendMessage(new StringTextComponent("✎ Pedido anotado en la Libreta: " + titulo(t)).withStyle(TextFormatting.YELLOW),
                p.getUUID());
        p.level.playSound(null, p.blockPosition(), SoundEvents.VILLAGER_YES, SoundCategory.NEUTRAL, 0.8F, 1.0F);
        if (t.getInt("Tipo") == LLEVAR) {
            ItemStack enc = com.trucoya.item.EncomiendaItem.crear(t.getInt("Id"), region(t).ordinal(), g.nombre());
            if (!p.inventory.add(enc)) {
                p.drop(enc, false);
            }
        }
    }

    /** Si el jugador trae lo pedido (o una encomienda para la región de este gaucho), se entrega. */
    private static boolean entregar(GauchoEntity g, ServerPlayerEntity p) {
        ListNBT l = lista(p);
        Historia.Region aca = Historia.Region.de(g.level.getBiome(g.blockPosition()).getBiomeCategory());
        String faltan = null;
        for (int i = 0; i < l.size(); i++) {
            CompoundNBT t = l.getCompound(i);
            int tipo = t.getInt("Tipo");
            if (tipo == TRAER && t.hasUUID("De") && t.getUUID("De").equals(g.getUUID())) {
                Cosa c = COSAS[Math.floorMod(t.getInt("Cosa"), COSAS.length)];
                Item item = c.item.get();
                int hay = 0;
                for (int s = 0; s < p.inventory.getContainerSize(); s++) {
                    ItemStack st = p.inventory.getItem(s);
                    if (st.getItem() == item) hay += st.getCount();
                }
                if (hay < c.cant) {
                    faltan = "Todavía me faltan " + (c.cant - hay) + " de " + nombreItem(c) + ", paisano.";
                    continue;
                }
                int falta = c.cant;
                for (int s = 0; s < p.inventory.getContainerSize() && falta > 0; s++) {
                    ItemStack st = p.inventory.getItem(s);
                    if (st.getItem() == item) {
                        int saco = Math.min(falta, st.getCount());
                        st.shrink(saco);
                        falta -= saco;
                    }
                }
                l.remove(i);
                g.say("¡Gracias, gurí! Esto no me lo olvido.");
                pagar(p, t);
                return true;
            }
            if (tipo == LLEVAR && region(t) == aca && g.puedePedir() && !(t.hasUUID("De") && t.getUUID("De").equals(g.getUUID()))) {
                int slot = com.trucoya.item.EncomiendaItem.buscar(p, t.getInt("Id"));
                if (slot < 0) {
                    continue;
                }
                p.inventory.getItem(slot).shrink(1);
                l.remove(i);
                g.say("¡Una encomienda de " + t.getString("Nombre") + "! Gracias por traerla hasta acá.");
                pagar(p, t);
                return true;
            }
        }
        if (faltan != null) {
            g.say(faltan);
            return true;
        }
        return false;
    }

    /** Algo que hizo el jugador y puede cumplir un desafío (GANAR). La región puede ser null. */
    public static void evento(ServerPlayerEntity p, int reto, @Nullable Historia.Region reg) {
        ListNBT l = lista(p);
        for (int i = 0; i < l.size(); i++) {
            CompoundNBT t = l.getCompound(i);
            if (t.getInt("Tipo") != GANAR || t.getInt("Reto") != reto) continue;
            if (reto == RETO_PULPERIA && reg != region(t)) continue;
            t.putInt("Avance", t.getInt("Avance") + 1);
            if (t.getInt("Avance") >= t.getInt("Meta")) {
                l.remove(i);
                p.sendMessage(new StringTextComponent("A " + t.getString("Nombre") + " le llegó la noticia.")
                        .withStyle(TextFormatting.GRAY), p.getUUID());
                pagar(p, t);
                return;
            }
            p.displayClientMessage(new StringTextComponent(titulo(t)).withStyle(TextFormatting.YELLOW), true);
        }
    }

    private static void pagar(ServerPlayerEntity p, CompoundNBT t) {
        anotarHecho(p, t.getInt("Id"));
        ItemStack esm = new ItemStack(Items.EMERALD, t.getInt("Esmeraldas"));
        if (!p.inventory.add(esm)) {
            p.drop(esm, false);
        }
        int n = JugadorData.increment(p, CUMPLIDOS);
        Diario.evento(p, Diario.PEDIDO);
        JugadorData.addPrestigio(p, t.getInt("Prestigio"), "Pedido cumplido");
        p.sendMessage(new StringTextComponent("✔ Pedido cumplido: " + titulo(t) + "  (+" + t.getInt("Esmeraldas")
                + " esmeraldas)").withStyle(TextFormatting.GREEN), p.getUUID());
        p.level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.6F, 1.4F);
        if (n == 5) {
            Logros.award(p, Logros.GAUCHO_DE_PALABRA);
        }
    }

    public static int[] ids(ServerPlayerEntity p) {
        ListNBT l = lista(p);
        int[] ids = new int[l.size()];
        for (int i = 0; i < l.size(); i++) ids[i] = l.getCompound(i).getInt("Id");
        return ids;
    }

    public static boolean cancelarId(ServerPlayerEntity p, int id) {
        ListNBT l = lista(p);
        for (int i = 0; i < l.size(); i++) {
            if (l.getCompound(i).getInt("Id") == id) return cancelarEn(p, i);
        }
        return false;
    }

    /** Cancela (el pedido ya no se puede volver a aceptar: así no se juntan encomiendas de más). */
    public static boolean cancelarEn(ServerPlayerEntity p, int indice) {
        ListNBT l = lista(p);
        if (indice < 0 || indice >= l.size()) {
            return false;
        }
        CompoundNBT t = l.getCompound(indice);
        l.remove(indice);
        anotarHecho(p, t.getInt("Id"));
        if (t.getInt("Tipo") == LLEVAR) {
            int slot = com.trucoya.item.EncomiendaItem.buscar(p, t.getInt("Id"));
            if (slot >= 0) p.inventory.getItem(slot).shrink(1);
        }
        p.sendMessage(new StringTextComponent("Pedido cancelado: " + titulo(t)).withStyle(TextFormatting.GRAY), p.getUUID());
        return true;
    }

    /** Para la Libreta: título, detalle (dónde / cuánto falta) y premio de cada pedido. */
    public static List<String> paraLibreta(ServerPlayerEntity p) {
        List<String> out = new ArrayList<>();
        ListNBT l = lista(p);
        for (int i = 0; i < l.size(); i++) {
            CompoundNBT t = l.getCompound(i);
            String detalle;
            if (t.getInt("Tipo") == TRAER) {
                BlockPos donde = BlockPos.of(t.getLong("Donde"));
                int dx = donde.getX() - p.blockPosition().getX(), dz = donde.getZ() - p.blockPosition().getZ();
                int dist = (int) Math.round(Math.sqrt((double) dx * dx + (double) dz * dz));
                detalle = dist < 16 ? t.getString("Nombre") + " está por acá." : t.getString("Nombre") + " quedó a " + dist
                        + " bloques hacia el " + rumbo(dx, dz) + ".";
            } else if (t.getInt("Tipo") == LLEVAR) {
                detalle = "Dásela a cualquier paisano de " + region(t).nombre + ".";
            } else {
                detalle = "De parte de " + t.getString("Nombre") + ".";
            }
            out.add(titulo(t) + "\n" + detalle + "\n" + t.getInt("Esmeraldas") + " esmeraldas · " + t.getInt("Prestigio") + " de prestigio");
        }
        return out;
    }

    public static String rumbo(int dx, int dz) {
        String[] nombres = {"este", "sureste", "sur", "suroeste", "oeste", "noroeste", "norte", "noreste"};
        double ang = Math.toDegrees(Math.atan2(dz, dx));
        int i = (int) Math.round(((ang % 360) + 360) % 360 / 45.0) % 8;
        return nombres[i];
    }

    public static void olvidar(UUID jugador) {
        ULTIMO_CLIC.remove(jugador);
        MOSTRADO.remove(jugador);
        MOSTRADO_DE.remove(jugador);
    }
}
