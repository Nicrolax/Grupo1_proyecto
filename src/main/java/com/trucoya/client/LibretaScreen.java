package com.trucoya.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.trucoya.core.Prestigio;
import com.mojang.blaze3d.systems.RenderSystem;
import com.trucoya.barajas.Baraja;
import com.trucoya.network.LibretaAccionPacket;
import com.trucoya.network.LibretaPacket;
import com.trucoya.network.TrucoNetwork;
import com.trucoya.progreso.Historia;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** La Libreta del Truquero: el Camino del Truco con sus capítulos, los seis sellos y el rumbo a la estancia. */
@OnlyIn(Dist.CLIENT)
public class LibretaScreen extends Screen {

    private static final int W = 300;
    private static final int H = 190;
    private static final int PAPEL = 0xFFF1E6C8;
    private static final int PAPEL_OSC = 0xFFE2D3AE;
    private static final int TINTA = 0xFF3A2A18;
    private static final int TINTA_SUAVE = 0xFF7A6448;
    private static final int ORO = 0xFFB8860B;
    private static final int TAPA = 0xFF5A2A1A;

    private static final String[] PESTANAS = {"Camino", "Barajas", "Pedidos", "Números"};
    private static final int[] COLORES_PESTANA = {0xFFB3262C, 0xFF23365A, 0xFF4C7A2E, 0xFF8A6A1E};
    private static final int PESTANA_W = 52;
    /** Se recuerda la pestaña al reabrir (la Libreta se vuelve a mandar después de cada acción). */
    private static int pestana = 0;
    private static int vista = -1;

    private final LibretaPacket d;
    private int left;
    private int top;

    public LibretaScreen(LibretaPacket data) {
        super(new StringTextComponent("Libreta del Truquero"));
        this.d = data;
    }

    @Override
    protected void init() {
        left = (this.width - W) / 2;
        top = (this.height - H) / 2 + 8;
        if (vista < 0 || vista >= Baraja.values().length) {
            vista = d.elegida;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(MatrixStack ms, int mx, int my, float pt) {
        this.renderBackground(ms);
        // las cintas de las pestañas asoman por arriba de la tapa
        for (int i = 0; i < PESTANAS.length; i++) {
            int x0 = pestanaX(i);
            boolean activa = i == pestana;
            boolean sobre = mx >= x0 && mx < x0 + PESTANA_W && my >= top - 20 && my < top - 6;
            int col = activa ? COLORES_PESTANA[i] : (COLORES_PESTANA[i] & 0x00FFFFFF) | (sobre ? 0xE0000000 : 0xA0000000);
            fill(ms, x0, top - (activa ? 21 : 18), x0 + PESTANA_W, top - 4, col);
            fill(ms, x0, top - (activa ? 21 : 18), x0 + PESTANA_W, top - (activa ? 20 : 17), 0x40FFFFFF);
            centrado(ms, PESTANAS[i], x0 + PESTANA_W / 2, top - (activa ? 17 : 14), activa ? 0xFFFFFFFF : 0xFFE8DCC0);
        }
        // tapas de cuero y las dos páginas
        fill(ms, left - 6, top - 6, left + W + 6, top + H + 6, TAPA);
        fill(ms, left - 4, top - 4, left + W + 4, top + H + 4, 0xFF6E3522);
        fill(ms, left, top, left + W / 2 - 1, top + H, PAPEL);
        fill(ms, left + W / 2 + 1, top, left + W, top + H, PAPEL);
        fill(ms, left + W / 2 - 1, top, left + W / 2 + 1, top + H, PAPEL_OSC);
        fill(ms, left + 3, top, left + W / 2 - 3, top + 2, COLORES_PESTANA[pestana]);
        switch (pestana) {
            case 1: renderBarajas(ms, mx, my); break;
            case 2: renderPedidos(ms, mx, my); break;
            case 3: renderNumeros(ms); break;
            default: renderCamino(ms, mx, my); break;
        }
        super.render(ms, mx, my, pt);
        if (pestana == 0 && bajoMouse != null) {
            boolean tiene = (d.sellos & bajoMouse.bit()) != 0;
            this.renderTooltip(ms, java.util.Arrays.asList(
                    new StringTextComponent("Sello de " + bajoMouse.nombre).getVisualOrderText(),
                    new StringTextComponent(tiene ? "¡Ganado!" : bajoMouse.pista).getVisualOrderText(),
                    new StringTextComponent("Campeón: Nivel " + bajoMouse.nivelCampeon).getVisualOrderText()), mx, my);
        }
    }

    private int pestanaX(int i) {
        return left + 6 + i * (PESTANA_W + 4);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int boton) {
        for (int i = 0; i < PESTANAS.length; i++) {
            int x0 = pestanaX(i);
            if (mx >= x0 && mx < x0 + PESTANA_W && my >= top - 21 && my < top - 4) {
                if (pestana != i) {
                    pestana = i;
                    pasarHoja();
                }
                return true;
            }
        }
        if (pestana == 1) {
            for (int i = 0; i < Baraja.values().length; i++) {
                int x = mazoX(i), y = mazoY(i);
                if (mx >= x && mx < x + MAZO_W && my >= y && my < y + MAZO_H) {
                    if (vista != i) {
                        vista = i;
                        pasarHoja();
                    }
                    return true;
                }
            }
            Baraja b = Baraja.de(vista);
            if (tiene(b) && d.elegida != vista && dentro(mx, my, botonX(), botonY(), BOTON_W, 14)) {
                TrucoNetwork.CHANNEL.sendToServer(new LibretaAccionPacket(LibretaAccionPacket.ELEGIR_BARAJA, vista));
                return true;
            }
        }
        if (pestana == 2) {
            for (int i = 0; i < d.pedidos.size(); i++) {
                int[] c = cancelarEn(i);
                if (c != null && i < d.pedidoIds.length && dentro(mx, my, c[0], c[1], c[2], 9)) {
                    TrucoNetwork.CHANNEL.sendToServer(new LibretaAccionPacket(LibretaAccionPacket.CANCELAR_PEDIDO, d.pedidoIds[i]));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, boton);
    }

    private static boolean dentro(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void pasarHoja() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(net.minecraft.client.audio.SimpleSound.forUI(
                    net.minecraft.util.SoundEvents.BOOK_PAGE_TURN, 1.0F));
        }
    }

    private boolean tiene(Baraja b) {
        return ((d.barajas | Baraja.CRIOLLA.bit()) & b.bit()) != 0;
    }

    // ------------------------------------------------------------------ pestaña: el camino

    private Historia.Region bajoMouse;

    private void renderCamino(MatrixStack ms, int mx, int my) {
        for (int y = top + 22; y < top + H - 8; y += 11) {       // renglones
            fill(ms, left + 8, y, left + W / 2 - 8, y + 1, 0x18403020);
        }

        int nivel = Prestigio.nivel(d.prestigio);
        int sellos = Integer.bitCount(d.sellos & Historia.TODOS_LOS_SELLOS);
        boolean c1 = d.ganados > 0;
        boolean c2 = sellos == 6;
        boolean c3 = nivel >= Historia.NIVEL_VIZCACHA;
        // ---- página izquierda: los capítulos
        int x = left + 10;
        int y = top + 8;
        titulo(ms, "El Camino del Truco", left + W / 4, y);
        y += 16;
        y = capitulo(ms, x, y, c1, "I · Primeras manos", "Ganá tu primer partido.");
        y = capitulo(ms, x, y, c2, "II · La Ruta de las Pulperías", "Sellos: " + sellos + " de 6.");
        y = capitulo(ms, x, y, c3, "III · Taita del Truco", "Nivel " + nivel + " de " + Historia.NIVEL_VIZCACHA
                + " (" + Prestigio.rango(nivel) + ").");
        y = capitulo(ms, x, y, d.mapa, "IV · El Mapa del Baqueano", d.mapa ? "La cruz roja marca la estancia."
                : "Llega con los sellos y el nivel.");
        y = capitulo(ms, x, y, d.vizcacha, "V · El Último Truco", "Ganale al Viejo Vizcacha.");
        y = capitulo(ms, x, y, d.finalVisto, "Epílogo", d.finalVisto ? "Ya sos parte de la historia." : "...");
        String sal = d.salamanca == com.trucoya.progreso.Salamanca.LIBRE ? "El Negro Ledesma descansa en paz."
                : d.salamanca == com.trucoya.progreso.Salamanca.ABIERTA ? "Seguí las luces malas hasta el Paso de las Ánimas."
                : d.salamanca == com.trucoya.progreso.Salamanca.ESPERA ? "Al atardecer, alguien vendrá a buscarte." : "...";
        capitulo(ms, x, y, d.salamanca == com.trucoya.progreso.Salamanca.LIBRE, "VI · La Salamanca", sal);
        chico(ms, "Torneos ganados: " + d.torneos + "   ·   Cuadreras ganadas: " + d.cuadreras, x, top + H - 12, TINTA_SUAVE);

        // ---- página derecha: los sellos
        int rx = left + W / 2 + 8;
        titulo(ms, "Los seis sellos", left + 3 * W / 4, top + 8);
        Historia.Region[] regiones = Historia.Region.values();
        bajoMouse = null;
        for (int i = 0; i < regiones.length; i++) {
            Historia.Region r = regiones[i];
            int cx = rx + 22 + (i % 3) * 46;
            int cy = top + 46 + (i / 3) * 50;
            boolean tiene = (d.sellos & r.bit()) != 0;
            sello(ms, cx, cy, 15, tiene ? 0xFF000000 | r.color : 0xFFCFC2A2, tiene);
            String nombre = r.nombre.replace("la ", "").replace("el ", "");
            nombre = Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1);
            centrado(ms, nombre, cx, cy + 18, tiene ? TINTA : TINTA_SUAVE);
            if (Math.abs(mx - cx) <= 16 && Math.abs(my - cy) <= 16) {
                bajoMouse = r;
            }
        }
        // rumbo o próximo paso
        String rumbo;
        if (d.mapa && (d.dx != 0 || d.dz != 0)) {
            int dist = (int) Math.round(Math.sqrt((double) d.dx * d.dx + (double) d.dz * d.dz));
            rumbo = dist < 40 ? "Estás en la Estancia del Caudillo." : "La Estancia queda a " + dist + " bloques hacia el " + rumbo(d.dx, d.dz) + ".";
        } else if (!c1) {
            rumbo = "Sentate a una mesa de truco y ganá un partido.";
        } else if (!c2) {
            rumbo = "Buscá pulperías de las regiones que faltan (pasá el mouse por un sello).";
        } else if (!c3) {
            rumbo = "Te faltan " + Prestigio.faltanParaSubir(d.prestigio) + " puntos de prestigio para el próximo nivel.";
        } else if (!d.vizcacha) {
            rumbo = "Seguí el Mapa del Baqueano hasta la Estancia del Caudillo.";
        } else if (d.salamanca == com.trucoya.progreso.Salamanca.ABIERTA) {
            int dist = (int) Math.round(Math.sqrt((double) d.pasoDx * d.pasoDx + (double) d.pasoDz * d.pasoDz));
            rumbo = dist < 40 ? "Estás en el Paso de las Ánimas. De noche, con el farol, el Vizcacha te espera."
                    : "El Paso de las Ánimas queda a " + dist + " bloques hacia el " + rumbo(d.pasoDx, d.pasoDz)
                    + ". Andá de noche, con el Farol de las Ánimas en la mano.";
        } else if (d.salamanca == com.trucoya.progreso.Salamanca.ESPERA) {
            rumbo = "Algo quedó pendiente en el fogón... Esperá al atardecer.";
        } else {
            rumbo = "El Viejo Vizcacha te espera para la revancha.";
        }
        int ry = top + 150;
        for (net.minecraft.util.IReorderingProcessor linea : this.font.split(new StringTextComponent(rumbo), W / 2 - 18)) {
            this.font.draw(ms, linea, rx, ry, TINTA);
            ry += 10;
        }
    }

    private static String rumbo(int dx, int dz) {
        String[] nombres = {"este", "sureste", "sur", "suroeste", "oeste", "noroeste", "norte", "noreste"};
        double ang = Math.toDegrees(Math.atan2(dz, dx));
        int i = (int) Math.round(((ang % 360) + 360) % 360 / 45.0) % 8;
        return nombres[i];
    }

    private int capitulo(MatrixStack ms, int x, int y, boolean hecho, String titulo, String detalle) {
        fill(ms, x, y + 1, x + 7, y + 8, hecho ? 0xFF4C8A3A : 0xFFCFC2A2);
        if (hecho) {
            this.font.draw(ms, "✔", x, y, 0xFFFFFFFF);
        }
        this.font.draw(ms, titulo, x + 11, y, hecho ? TINTA : TINTA_SUAVE);
        chico(ms, detalle, x + 11, y + 10, TINTA_SUAVE);
        return y + 21;
    }

    private void titulo(MatrixStack ms, String s, int cx, int y) {
        this.font.draw(ms, s, cx - this.font.width(s) / 2.0F, y, ORO);
        fill(ms, cx - this.font.width(s) / 2, y + 10, cx + this.font.width(s) / 2, y + 11, ORO);
    }

    private void centrado(MatrixStack ms, String s, int cx, int y, int color) {
        this.font.draw(ms, s, cx - this.font.width(s) / 2.0F, y, color);
    }

    private void chico(MatrixStack ms, String s, float x, float y, int color) {
        ms.pushPose();
        ms.translate(x, y, 0);
        ms.scale(0.75F, 0.75F, 1.0F);
        this.font.draw(ms, s, 0, 0, color);
        ms.popPose();
    }

    /** Un sello de lacre: disco con borde y una estrella si está ganado. */
    private void sello(MatrixStack ms, int cx, int cy, int r, int color, boolean ganado) {
        int borde = ganado ? 0xFF6A4A1A : 0xFFB8A988;
        disco(ms, cx, cy, r, borde);
        disco(ms, cx, cy, r - 2, color);
        if (ganado) {
            disco(ms, cx, cy, r - 6, 0x40FFFFFF);
            centrado(ms, "✦", cx, cy - 4, 0xFFFFFFFF);
        } else {
            centrado(ms, "?", cx, cy - 4, 0xFF9A8B6A);
        }
    }

    private void disco(MatrixStack ms, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.round(Math.sqrt((double) r * r - (double) dy * dy));
            fill(ms, cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    // ------------------------------------------------------------------ pestaña: las barajas

    private static final int MAZO_W = 22, MAZO_H = 33, BOTON_W = 96;

    private int mazoX(int i) {
        return left + 12 + (i % 5) * 26;
    }

    private int mazoY(int i) {
        return top + 30 + (i / 5) * 48;
    }

    private int botonX() {
        return left + W / 2 + (W / 2 - BOTON_W) / 2;
    }

    private int botonY() {
        return top + H - 22;
    }

    /** Una carta del atlas de esa baraja (columna y fila del atlas; fila 4, columna 0 = el dorso). */
    private void carta(MatrixStack ms, Baraja b, int col, int fila, int x, int y, int w, int h, float luz) {
        this.minecraft.getTextureManager().bind(b.atlasGui());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(luz, luz, luz, 1.0F);
        blit(ms, x, y, w, h, col * 128, fila * 192, 128, 192, 1280, 960);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderBarajas(MatrixStack ms, int mx, int my) {
        int cant = Integer.bitCount((d.barajas | Baraja.CRIOLLA.bit()) & ((1 << Baraja.values().length) - 1));
        titulo(ms, "Tu colección", left + W / 4, top + 8);
        centrado(ms, cant + " de " + Baraja.values().length, left + W / 4, top + 132, TINTA_SUAVE);
        for (int i = 0; i < Baraja.values().length; i++) {
            Baraja b = Baraja.values()[i];
            int x = mazoX(i), y = mazoY(i);
            boolean la = tiene(b);
            boolean sobre = dentro(mx, my, x, y, MAZO_W, MAZO_H);
            if (i == vista) {
                fill(ms, x - 2, y - 2, x + MAZO_W + 2, y + MAZO_H + 2, 0xFF000000 | b.color);
            }
            // un mazo: dos cartas asomando detrás del dorso
            fill(ms, x + 3, y - 1, x + MAZO_W + 2, y + MAZO_H - 2, 0x30000000);
            carta(ms, b, 0, 4, x, y + (sobre ? -2 : 0), MAZO_W, MAZO_H, la ? 1.0F : 0.28F);
            if (!la) {
                centrado(ms, "?", x + MAZO_W / 2, y + MAZO_H / 2 - 4, 0xFFE8DCC0);
            }
            if (i == d.elegida) {
                centrado(ms, "▲", x + MAZO_W / 2, y + MAZO_H + 3, 0xFF000000 | b.color);
            }
        }
        chico(ms, "▲ con la que jugás. Tocá un mazo para mirarlo.", left + 12, top + 146, TINTA_SUAVE);
        chico(ms, "Se ganan con hazañas, en los torneos", left + 12, top + 160, TINTA_SUAVE);
        chico(ms, "de cada región o comprándolas al pulpero.", left + 12, top + 168, TINTA_SUAVE);

        // ---- página derecha: la baraja que se está mirando
        Baraja b = Baraja.de(vista);
        boolean la = tiene(b);
        int rx = left + W / 2 + 8;
        titulo(ms, b.nombre, left + 3 * W / 4, top + 8);
        int cy = top + 24;
        int cx = left + 3 * W / 4;
        if (la) {
            // abanico: tres cartas de cara y el dorso
            carta(ms, b, 0, 0, cx - 62, cy + 6, 30, 45, 1.0F);
            carta(ms, b, 6, 2, cx - 36, cy + 2, 30, 45, 1.0F);
            carta(ms, b, 9, 3, cx - 10, cy, 30, 45, 1.0F);
            carta(ms, b, 0, 4, cx + 22, cy + 4, 34, 51, 1.0F);
        } else {
            carta(ms, b, 0, 4, cx - 17, cy, 34, 51, 0.28F);
            centrado(ms, "?", cx, cy + 22, 0xFFE8DCC0);
        }
        int ty = cy + 62;
        for (net.minecraft.util.IReorderingProcessor linea : this.font.split(
                new StringTextComponent(la ? b.comoSeGana : "Todavía no la tenés. " + b.comoSeGana), W / 2 - 18)) {
            this.font.draw(ms, linea, rx, ty, la ? TINTA : TINTA_SUAVE);
            ty += 10;
        }
        int bx = botonX(), by = botonY();
        if (la && d.elegida == vista) {
            centrado(ms, "✔ Estás jugando con esta", cx, by + 3, 0xFF4C8A3A);
        } else if (la) {
            boolean sobre = dentro(mx, my, bx, by, BOTON_W, 14);
            fill(ms, bx, by, bx + BOTON_W, by + 14, sobre ? 0xFF2F4F8A : 0xFF23365A);
            fill(ms, bx, by, bx + BOTON_W, by + 1, 0x50FFFFFF);
            centrado(ms, "Jugar con esta", cx, by + 3, 0xFFFFFFFF);
        }
    }

    // ------------------------------------------------------------------ pestaña: los pedidos

    /** Dónde queda el "cancelar" del pedido i: x, y, ancho. */
    private int[] cancelarEn(int i) {
        if (i >= d.pedidos.size()) {
            return null;
        }
        String c = "✖ cancelar";
        int w = (int) (this.font.width(c) * 0.75F);
        return new int[]{left + W / 2 - 10 - w, top + 26 + i * 52, w};
    }

    private void renderPedidos(MatrixStack ms, int mx, int my) {
        titulo(ms, "Pedidos (" + d.pedidos.size() + " de 3)", left + W / 4, top + 8);
        if (d.pedidos.isEmpty()) {
            int y = top + 30;
            for (net.minecraft.util.IReorderingProcessor linea : this.font.split(new StringTextComponent(
                    "No tenés pedidos. Buscá paisanos con \"¡Pedido!\" sobre la cabeza y hacé clic derecho para escucharlos."),
                    W / 2 - 22)) {
                this.font.draw(ms, linea, left + 10, y, TINTA_SUAVE);
                y += 10;
            }
        }
        for (int i = 0; i < d.pedidos.size(); i++) {
            String[] partes = d.pedidos.get(i).split("\n");
            int y = top + 26 + i * 52;
            fill(ms, left + 8, y - 2, left + W / 2 - 8, y + 47, 0x14403020);
            fill(ms, left + 8, y - 2, left + 10, y + 47, COLORES_PESTANA[2]);
            int ly = y;
            int lineas = 0;
            for (net.minecraft.util.IReorderingProcessor linea : this.font.split(new StringTextComponent(partes[0]), W / 2 - 64)) {
                if (lineas++ < 2) {
                    this.font.draw(ms, linea, left + 14, ly, TINTA);
                    ly += 10;
                }
            }
            if (partes.length > 1) chico(ms, partes[1], left + 14, y + 24, TINTA_SUAVE);
            if (partes.length > 2) chico(ms, partes[2], left + 14, y + 34, 0xFF2F6A2A);
            int[] c = cancelarEn(i);
            boolean sobre = dentro(mx, my, c[0], c[1], c[2], 9);
            chico(ms, "✖ cancelar", c[0], c[1], sobre ? 0xFFB3262C : 0xFFA89878);
        }
        chico(ms, "Agachado, el clic invita a jugar.", left + 10, top + H - 10, TINTA_SUAVE);

        // ---- página derecha: los desafíos del día
        int rx = left + W / 2 + 8;
        int cx = left + 3 * W / 4;
        titulo(ms, "Desafíos de hoy", cx, top + 8);
        int y = top + 24;
        int hechos = 0;
        for (String s : d.diario) {
            String[] partes = s.split("\n");
            if (partes.length < 3) continue;
            int avance = parse(partes[1]), meta = Math.max(1, parse(partes[2]));
            boolean listo = avance >= meta;
            if (listo) hechos++;
            fill(ms, rx, y + 1, rx + 7, y + 8, listo ? 0xFF4C8A3A : 0xFFCFC2A2);
            if (listo) this.font.draw(ms, "✔", rx, y, 0xFFFFFFFF);
            int ly = y;
            for (net.minecraft.util.IReorderingProcessor linea : this.font.split(new StringTextComponent(partes[0]), W / 2 - 30)) {
                ms.pushPose();
                ms.translate(rx + 11, ly, 0);
                ms.scale(0.85F, 0.85F, 1.0F);
                this.font.draw(ms, linea, 0, 0, listo ? TINTA_SUAVE : TINTA);
                ms.popPose();
                ly += 9;
            }
            // barrita de avance
            int bx = rx + 11, bw = W / 2 - 40;
            fill(ms, bx, ly + 1, bx + bw, ly + 4, 0xFFDCCDA8);
            fill(ms, bx, ly + 1, bx + bw * Math.min(avance, meta) / meta, ly + 4, listo ? 0xFF4C8A3A : ORO);
            chico(ms, Math.min(avance, meta) + "/" + meta, bx + bw + 3, ly, TINTA_SUAVE);
            y = ly + 12;
        }
        chico(ms, "Cada uno: 2 esmeraldas y 25 de prestigio.", rx, top + 146, TINTA_SUAVE);
        chico(ms, "Los tres: 5 esmeraldas más.", rx, top + 154, TINTA_SUAVE);
        centrado(ms, hechos == 3 ? "¡Listos los tres!" : "Mañana hay otros.", cx, top + 165, hechos == 3 ? 0xFF4C8A3A : TINTA_SUAVE);
        centrado(ms, "Racha: " + d.diarioRacha + (d.diarioRacha == 1 ? " día" : " días seguidos"), cx, top + 177, ORO);
    }

    private static int parse(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void chicoDe(MatrixStack ms, String s, float x, float y, int color, float k) {
        ms.pushPose();
        ms.translate(x, y, 0);
        ms.scale(k, k, 1.0F);
        this.font.draw(ms, s, 0, 0, color);
        ms.popPose();
    }

    // ------------------------------------------------------------------ pestaña: los números

    private static final String[] NUMEROS = {"Partidos ganados", "Partidos perdidos", "Racha actual", "Flores cantadas",
            "Faroles (puro cuento)", "Forasteros vencidos", "Torneos ganados", "Cuadreras ganadas", "Pedidos cumplidos",
            "Victorias sobre el Vizcacha", "Mejor racha", "Días con los tres desafíos", "Mejor racha de desafíos"};

    private int numero(int i) {
        return i < d.numeros.length ? d.numeros[i] : 0;
    }

    private void renderNumeros(MatrixStack ms) {
        titulo(ms, "Los números del truquero", left + W / 4, top + 8);
        int y = top + 24;
        for (int i = 0; i < NUMEROS.length; i++) {
            fill(ms, left + 8, y + 9, left + W / 2 - 8, y + 10, 0x18403020);
            chicoDe(ms, NUMEROS[i], left + 10, y + 1, TINTA, 0.9F);
            String v = String.valueOf(numero(i));
            this.font.draw(ms, v, left + W / 2 - 10 - this.font.width(v), y, ORO);
            y += 12;
        }
        int ganados = numero(0), perdidos = numero(1);
        int total = ganados + perdidos;
        int cx = left + 3 * W / 4;
        titulo(ms, "En la mesa", cx, top + 8);
        int pct = total == 0 ? 0 : Math.round(100.0F * ganados / total);
        String grande = total == 0 ? "—" : pct + "%";
        ms.pushPose();
        ms.translate(cx - this.font.width(grande) * 1.5F, top + 30, 0);
        ms.scale(3.0F, 3.0F, 1.0F);
        this.font.draw(ms, grande, 0, 0, TINTA);
        ms.popPose();
        centrado(ms, "de partidos ganados", cx, top + 60, TINTA_SUAVE);
        int bx = left + W / 2 + 14, bw = W / 2 - 28, by = top + 74;
        fill(ms, bx, by, bx + bw, by + 8, 0xFFB3262C);
        fill(ms, bx, by, bx + (total == 0 ? 0 : bw * ganados / total), by + 8, 0xFF4C8A3A);
        chico(ms, ganados + " ganados", bx, by + 11, 0xFF4C8A3A);
        String pr = perdidos + " perdidos";
        chico(ms, pr, bx + bw - this.font.width(pr) * 0.75F, by + 11, 0xFFB3262C);
        int nivel = Prestigio.nivel(d.prestigio);
        centrado(ms, "Prestigio: " + d.prestigio, cx, top + 104, TINTA);
        centrado(ms, "Nivel " + nivel + " · " + Prestigio.rango(nivel), cx, top + 116, ORO);
        int sellos = Integer.bitCount(d.sellos & Historia.TODOS_LOS_SELLOS);
        int barajas = Integer.bitCount((d.barajas | Baraja.CRIOLLA.bit()) & ((1 << Baraja.values().length) - 1));
        centrado(ms, "Sellos: " + sellos + " de 6  ·  Barajas: " + barajas + " de " + Baraja.values().length, cx, top + 136,
                TINTA_SUAVE);
        String frase = total == 0 ? "Todavía no te sentaste a una mesa." : pct >= 70 ? "Un taita del truco."
                : pct >= 50 ? "Más ganadas que perdidas: vas bien." : "El truco se aprende perdiendo.";
        centrado(ms, frase, cx, top + 160, TINTA_SUAVE);
    }
}
