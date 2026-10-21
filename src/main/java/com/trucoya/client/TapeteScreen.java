package com.trucoya.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.trucoya.TrucoYa;
import com.trucoya.core.ActionType;
import com.trucoya.core.Card;
import com.trucoya.core.PlayerView;
import com.trucoya.core.TableSnapshot;
import com.trucoya.network.TableActionPacket;
import com.trucoya.network.TrucoNetwork;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * GUI del tapete, pensada para que se vea limpia: la mano, la muestra, lo jugado, un anotador con
 * fósforos (cuadrados de 5, como se anota en el boliche) y SOLO los botones de lo que se puede
 * hacer en ese momento. Los tantos cantados (envido o flor) aparecen junto a cada jugador.
 * El registro de la mano queda escondido: se abre con H.
 *
 * Atajos: 1-2-3 juegan carta · T truco/retruco/vale cuatro · E envido · R real envido ·
 * F falta envido · L flor · C contraflor · A contraflor al resto · X con flor me achico ·
 * Q quiero · N no quiero · M me voy al mazo · G señas · H historial · Esc cierra (seguís sentado).
 */
@OnlyIn(Dist.CLIENT)
public class TapeteScreen extends Screen {

    private static final ResourceLocation TAPETE = new ResourceLocation(TrucoYa.MOD_ID, "textures/gui/tapete.png");
    private static final ResourceLocation CARTAS = com.trucoya.barajas.Baraja.CRIOLLA.atlasGui();

    private static final int PANEL_W = 256;
    private static final int PANEL_H = 200;
    private static final int COL_W = 120;
    private static final int GAP = 4;
    /** Atlas de cartas en alta resolución (10 x 5 cartas de 128 x 192). */
    private static final int CARD_W = 128;
    private static final int CARD_H = 192;
    private static final int ATLAS_W = 1280;
    private static final int ATLAS_H = 960;
    private static final int HAND_W = 36;
    private static final int HAND_H = 54;
    private static final int TABLE_W = 30;
    private static final int TABLE_H = 45;

    private static final int WHITE = 0xFFFFFF;
    private static final int GOLD = 0xFFE8B82A;
    private static final int SOFT = 0xD8E8D0;

    /** Botonera en el orden pedido, con su atajo. */
    private static final ActionType[] BUTTONS = {
        ActionType.TRUCO, ActionType.RETRUCO, ActionType.VALE_CUATRO,
        ActionType.ENVIDO, ActionType.REAL_ENVIDO, ActionType.FALTA_ENVIDO,
        ActionType.FLOR, ActionType.CONTRAFLOR, ActionType.CONTRAFLOR_AL_RESTO, ActionType.CON_FLOR_ME_ACHICO,
        ActionType.QUIERO, ActionType.NO_QUIERO, ActionType.ME_VOY_AL_MAZO
    };

    // Selección para repartir (se recuerda mientras dure la sesión).
    private static int selPlayers = -1;
    private static int selTarget = 30;
    private static boolean selFlor = true;
    /** Historial de la mano visible (tecla H). */
    private static boolean verHistorial;
    /** Dónde puede empezar el historial (debajo del anotador y de los tantos cantados). */
    private int historialY = 40;
    /**
     * Vista clásica (el tapete grande, con las cartas jugadas dibujadas) o la compacta (por defecto): solo una
     * franja abajo con tu mano y los cantos, y la mesa de verdad a la vista, con las cartas sobre el paño.
     * La tecla V cambia de una a otra.
     */
    private static boolean clasica;
    private boolean apuntado;
    @javax.annotation.Nullable
    private Boolean hudAntes;

    @Override
    public void removed() {
        if (hudAntes != null && this.minecraft != null) {
            this.minecraft.options.hideGui = hudAntes;
            hudAntes = null;
        }
        super.removed();
    }

    // medidas de la vista compacta
    /** Tamaño de las cartas en la vista compacta: crece con la pantalla (mínimo 34 x 51). */
    private int manoW = 34, manoH = 51;
    private int muestraW = 24, muestraH = 36;
    private static final int BOTON_W = 66, BOTON_H = 14;

    private int left;
    private int top;

    public TapeteScreen() {
        super(new StringTextComponent("Truco"));
    }

    /** Llamado al recibir una vista nueva del servidor. */
    public void refresh() {
        if (this.minecraft != null) {
            this.init(this.minecraft, this.width, this.height);
        }
    }

    private static TableSnapshot snap() {
        return ClientTrucoState.snapshot;
    }

    private static BlockPos pos() {
        return ClientTrucoState.tablePos;
    }

    @Override
    protected void init() {
        manoH = Math.max(51, Math.min(84, (int) (this.height * 0.25F)));
        manoW = manoH * 2 / 3;
        muestraH = manoH * 3 / 4;
        muestraW = muestraH * 2 / 3;
        if (!clasica) {
            initCompacta();
            return;
        }
        if (hudAntes != null && this.minecraft != null) {   // la vista clásica va con la barra de ítems
            this.minecraft.options.hideGui = hudAntes;
            hudAntes = null;
        }
        int total = PANEL_W + GAP + COL_W;
        left = Math.max(2, (this.width - total) / 2);
        top = Math.max(14, (this.height - PANEL_H) / 2);
        TableSnapshot s = snap();
        if (s == null) {
            return;
        }
        if (selPlayers < 0) {
            selPlayers = s.cfgPlayers;
            selTarget = s.cfgTarget;
            selFlor = s.cfgFlor;
        }
        int colX = left + PANEL_W + GAP;
        int y = top + 40;
        PlayerView v = s.view;
        for (ActionType a : BUTTONS) {
            if (v == null || !v.isLegal(a)) {
                continue;     // solo lo que se puede hacer ahora: menos ruido
            }
            this.addButton(new Button(colX, y, COL_W, 16, new StringTextComponent(label(a)), btn -> send(a),
                    (btn, ms, mx, my) -> this.renderTooltip(ms, new StringTextComponent("Atajo: " + atajo(a)), mx, my)));
            y += 18;
        }
        if (!s.tableRunning) {
            int bx = left + PANEL_W / 2 - 60;
            int by = top + 58;
            this.addButton(new Button(bx, by, 120, 16, new StringTextComponent(selPlayers == 4 ? "Modo: 2 contra 2" : "Modo: 1 contra 1"),
                    btn -> { selPlayers = selPlayers == 4 ? 2 : 4; refresh(); }));
            this.addButton(new Button(bx, by + 20, 120, 16, new StringTextComponent("A " + selTarget + " puntos"),
                    btn -> { selTarget = selTarget == 30 ? 40 : 30; refresh(); }));
            this.addButton(new Button(bx, by + 40, 120, 16, new StringTextComponent(selFlor ? "Con flor" : "Sin flor"),
                    btn -> { selFlor = !selFlor; refresh(); }));
            this.addButton(new Button(bx, by + 64, 120, 20, new StringTextComponent("Repartir"),
                    btn -> TrucoNetwork.CHANNEL.sendToServer(TableActionPacket.start(pos(), selPlayers, selTarget, selFlor))));
            if (!s.lugar.isEmpty() && s.torneoRonda == 0) {
                this.addButton(new Button(left + PANEL_W + GAP, top + 40, COL_W, 16, new StringTextComponent("Torneo (5 esmeraldas)"),
                        btn -> TrucoNetwork.CHANNEL.sendToServer(TableActionPacket.torneo(pos())),
                        (btn, ms, mx, my) -> this.renderTooltip(ms, new StringTextComponent(
                                "Tres rivales seguidos, partidos a 20. Premio: 20 esmeraldas y la Copa."), mx, my)));
            }
            this.addButton(new Button(bx, by + 88, 59, 14, new StringTextComponent("Apostar"),
                    btn -> TrucoNetwork.CHANNEL.sendToServer(TableActionPacket.bet(pos()))));
            this.addButton(new Button(bx + 61, by + 88, 59, 14, new StringTextComponent("Retirar"),
                    btn -> TrucoNetwork.CHANNEL.sendToServer(TableActionPacket.withdraw(pos()))));
        }
    }

    private static String label(ActionType a) {
        switch (a) {
            case TRUCO: return "Truco";
            case RETRUCO: return "Retruco";
            case VALE_CUATRO: return "Vale cuatro";
            case ENVIDO: return "Envido";
            case REAL_ENVIDO: return "Real envido";
            case FALTA_ENVIDO: return "Falta envido";
            case FLOR: return "Flor";
            case CONTRAFLOR: return "Contraflor";
            case CONTRAFLOR_AL_RESTO: return "Contraflor al resto";
            case CON_FLOR_ME_ACHICO: return "Con flor me achico";
            case QUIERO: return "Quiero";
            case NO_QUIERO: return "No quiero";
            case ME_VOY_AL_MAZO: return "Me voy al mazo";
            default: return a.name();
        }
    }

    private static String atajo(ActionType a) {
        switch (a) {
            case TRUCO: case RETRUCO: case VALE_CUATRO: return "T";
            case ENVIDO: return "E";
            case REAL_ENVIDO: return "R";
            case FALTA_ENVIDO: return "F";
            case FLOR: return "L";
            case CONTRAFLOR: return "C";
            case CONTRAFLOR_AL_RESTO: return "A";
            case CON_FLOR_ME_ACHICO: return "X";
            case QUIERO: return "Q";
            case NO_QUIERO: return "N";
            case ME_VOY_AL_MAZO: return "M";
            default: return "";
        }
    }

    private void send(ActionType a) {
        TableSnapshot s = snap();
        if (s != null && s.view != null && s.view.isLegal(a) && pos() != null) {
            TrucoNetwork.CHANNEL.sendToServer(TableActionPacket.action(pos(), a));
        }
    }

    private void playCard(int index) {
        TableSnapshot s = snap();
        if (s == null || s.view == null || !s.view.isLegal(ActionType.PLAY_CARD)) {
            return;
        }
        if (index >= 0 && index < s.view.hand.size()) {
            TrucoNetwork.CHANNEL.sendToServer(TableActionPacket.card(pos(), s.view.hand.get(index).getId()));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        if (!clasica) {
            renderCompacta(ms, mouseX, mouseY, partialTicks);
            return;
        }
        this.renderBackground(ms);
        TableSnapshot s = snap();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bind(TAPETE);
        this.blit(ms, left, top, 0, 0, PANEL_W, PANEL_H);
        if (s == null) {
            super.render(ms, mouseX, mouseY, partialTicks);
            return;
        }
        drawScoreColumn(ms, s);
        PlayerView v = s.view;
        if (v != null && s.tableRunning) {
            drawMuestra(ms, v);
            drawTable(ms, s, v);
            drawHand(ms, v, mouseX, mouseY);
            drawMyPoints(ms, v);
            drawTantos(ms, s, v);
            drawPot(ms, s, left + PANEL_W + GAP, top + PANEL_H - 30, mouseX, mouseY);
        } else if (s.tableRunning) {
            drawCenteredString(ms, this.font, "Hay una partida en juego en esta mesa.", left + PANEL_W / 2, top + 90, SOFT);
        } else {
            String head = v != null && v.matchOver
                    ? (v.matchWinner == v.team ? "¡Ganaron! " : "Perdieron. ") + "Sentados: " + s.seatedHumans
                    : "Sentados a la mesa: " + s.seatedHumans;
            drawCenteredString(ms, this.font, head, left + PANEL_W / 2, top + 44, SOFT);
            if (s.clandestina) {
                drawCenteredString(ms, this.font, "Mesa clandestina: se apuesta fuerte", left + PANEL_W / 2, top + 33, GOLD);
            } else if (!s.lugar.isEmpty()) {
                drawCenteredString(ms, this.font, s.lugar, left + PANEL_W / 2, top + 22, GOLD);
            }
            if (s.torneoRonda > 0) {
                drawCenteredString(ms, this.font, "Torneo · ronda " + s.torneoRonda + " de 3 · esperando al rival...",
                        left + PANEL_W / 2, top + 33, 0xFFFFFFFF);
            }
            drawPot(ms, s, left + PANEL_W / 2 - 60, top + 164, mouseX, mouseY);
        }
        if (verHistorial) {
            drawLog(ms, s);
        }
        drawCenteredString(ms, this.font, status(s), left + PANEL_W / 2, top - 11, WHITE);
        chico(ms, verHistorial ? "H: ocultar historial" : "H: historial", left + PANEL_W + GAP, top + PANEL_H + 3, 0x80FFFFFF);
        String sena = ClientTrucoState.currentSena();
        if (sena != null) {
            String line = this.font.plainSubstrByWidth(sena, PANEL_W - 20);
            fill(ms, left + 8, top + PANEL_H - 22, left + PANEL_W - 8, top + PANEL_H - 9, 0xAA000000);
            drawCenteredString(ms, this.font, line, left + PANEL_W / 2, top + PANEL_H - 20,
                    ClientTrucoState.senaFromPartner() ? 0xFFF0C8 : 0xB0B0B0);
        }
        super.render(ms, mouseX, mouseY, partialTicks);
    }

    /** Íconos del pozo de apuestas (con cantidad y dueño al pasar el mouse). */
    private void drawPot(MatrixStack ms, TableSnapshot s, int x, int y, int mouseX, int mouseY) {
        if (s.pot.isEmpty()) {
            return;
        }
        this.font.draw(ms, "Pozo:", x, y - 9, GOLD);
        int perRow = 7;
        TableSnapshot.PotEntry hovered = null;
        for (int i = 0; i < s.pot.size() && i < 14; i++) {
            TableSnapshot.PotEntry e = s.pot.get(i);
            int ix = x + (i % perRow) * 17;
            int iy = y + (i / perRow) * 17;
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(e.itemId));
            if (item == null) continue;
            ItemStack stack = new ItemStack(item, Math.max(1, e.count));
            this.itemRenderer.renderGuiItem(stack, ix, iy);
            this.itemRenderer.renderGuiItemDecorations(this.font, stack, ix, iy);
            if (mouseX >= ix && mouseX < ix + 16 && mouseY >= iy && mouseY < iy + 16) {
                hovered = e;
            }
        }
        if (hovered != null) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(hovered.itemId));
            String name = item != null ? new ItemStack(item).getHoverName().getString() : hovered.itemId;
            this.renderTooltip(ms, new StringTextComponent(hovered.count + " x " + name + " — " + hovered.owner), mouseX, mouseY);
        }
    }

    private String status(TableSnapshot s) {
        PlayerView v = s.view;
        if (v == null) {
            return s.tableRunning ? "Partida en curso" : "Elegí la modalidad y repartí";
        }
        if (v.matchOver) {
            return v.matchWinner == v.team ? "¡Ganaron ustedes el partido!" : "Perdieron el partido";
        }
        if (v.roundFinished) {
            return "Fin de la mano: se reparte de nuevo...";
        }
        if (v.pendingCall != null) {
            return v.pendingAwaitingTeam == v.team
                    ? "Te cantaron " + v.pendingCall.getSpeech() + " Respondé."
                    : "Esperando respuesta a " + v.pendingCall.getSpeech();
        }
        if (v.turn == v.seat) {
            return v.isLegal(ActionType.FLOR) && !v.isLegal(ActionType.PLAY_CARD) ? "Tenés flor: cantala" : "Tu turno";
        }
        return "Turno de " + s.nameOf(v.turn);
    }

    /** Anotador con fósforos: cuadrados de 5 (cuatro lados y la diagonal), malas | buenas. */
    private void drawScoreColumn(MatrixStack ms, TableSnapshot s) {
        int x = left + PANEL_W + GAP;
        fill(ms, x, top, x + COL_W, top + 34, 0xC0201810);
        PlayerView v = s.view;
        if (v == null) {
            chico(ms, "Anotador", x + 4, top + 4, 0xFFE8B82A);
            return;
        }
        int mine = v.team;
        anotador(ms, x + 4, top + 4, "Nos", v.score[mine], v.inBuenas[mine], v.targetScore);
        anotador(ms, x + 4, top + 19, "Ellos", v.score[1 - mine], v.inBuenas[1 - mine], v.targetScore);
    }

    private void anotador(MatrixStack ms, int x, int y, String quien, int puntos, boolean buenas, int target) {
        chico(ms, quien, x, y + 3, buenas ? 0xFF9CE89C : 0xFFF0D8A0);
        int mitad = target / 2;
        int cuadros = mitad / 5;
        int cx = x + 24;
        for (int k = 0; k < 2 * cuadros; k++) {
            if (k == cuadros) {
                fill(ms, cx, y - 1, cx + 1, y + 12, 0x90FFFFFF);     // la raya entre malas y buenas
                cx += 4;
            }
            int enEste = Math.max(0, Math.min(5, puntos - k * 5));
            fosforos(ms, cx, y + 1, enEste, k >= cuadros);
            cx += 11;
        }
    }

    /** Un cuadrado del anotador con {@code n} fósforos (0-5). */
    private void fosforos(MatrixStack ms, int x, int y, int n, boolean buenas) {
        int c = buenas ? 0xFF8FD48F : 0xFFF2E3B3;
        int vacio = 0x30FFFFFF;
        int l = 8;
        fill(ms, x, y, x + l, y + 1, n >= 1 ? c : vacio);             // arriba
        fill(ms, x + l - 1, y, x + l, y + l, n >= 2 ? c : vacio);     // derecha
        fill(ms, x, y + l - 1, x + l, y + l, n >= 3 ? c : vacio);     // abajo
        fill(ms, x, y, x + 1, y + l, n >= 4 ? c : vacio);             // izquierda
        if (n >= 5) {
            for (int d = 1; d < l - 1; d++) {
                fill(ms, x + d, y + d, x + d + 1, y + d + 1, c);       // la diagonal que cierra el cuadrado
            }
        }
    }

    /** Texto chiquito (70 %). */
    private void chico(MatrixStack ms, String txt, float x, float y, int color) {
        ms.pushPose();
        ms.translate(x, y, 0);
        ms.scale(0.7F, 0.7F, 1.0F);
        this.font.draw(ms, txt, 0, 0, color);
        ms.popPose();
    }

    private void drawLog(MatrixStack ms, TableSnapshot s) {
        List<String> log = s.log;
        int from = Math.max(0, log.size() - 8);
        fill(ms, left + 8, top + 8, left + PANEL_W - 8, top + 12 + (log.size() - from) * 7 + 4, 0xD0101010);
        ms.pushPose();
        ms.translate(left + 12, top + 12, 0);
        ms.scale(0.7F, 0.7F, 1.0F);
        int y = 0;
        for (int i = from; i < log.size(); i++) {
            String line = this.font.plainSubstrByWidth(log.get(i), (int) ((PANEL_W - 24) / 0.7F));
            this.font.draw(ms, line, 0, y, i == log.size() - 1 ? 0xFFFFFF : 0xB8D0B0);
            y += 10;
        }
        ms.popPose();
    }

    private void drawMuestra(MatrixStack ms, PlayerView v) {
        if (v.muestra == null) {
            return;
        }
        int x = left + 204;
        int y = top + 22;
        chico(ms, "muestra", x + 4, y - 8, 0xA0E8D8B0);
        drawCard(ms, v.muestra, x, y, HAND_W, HAND_H);
    }

    /** Coordenadas (esquina superior izquierda) para las cartas jugadas por posición relativa. */
    private int[] slot(PlayerView v, int rel) {
        int cx = left + PANEL_W / 2;
        int cy = top + 84;
        if (v.players == 2) {
            return rel == 0 ? new int[] {cx - 15, cy + 4} : new int[] {cx - 15, cy - 56};
        }
        switch (rel) {
            case 0: return new int[] {cx - 15, cy + 4};
            case 1: return new int[] {cx - 85, cy - 26};
            case 2: return new int[] {cx - 15, cy - 56};
            default: return new int[] {cx + 40, cy - 26};
        }
    }

    private void drawTable(MatrixStack ms, TableSnapshot s, PlayerView v) {
        for (int seat = 0; seat < v.players; seat++) {
            int rel = v.relativePosition(seat);
            int[] p = slot(v, rel);
            // Nombre (y marca de mano/turno)
            String name = (seat == v.mano ? "» " : "") + (rel == 0 ? "Vos" : s.nameOf(seat));
            int nameY = rel == 0 ? p[1] + TABLE_H + 1 : p[1] - 10;
            if (rel != 0) {
                int color = seat % 2 == v.team ? 0x9CE89C : 0xF0B0A0;
                if (seat == v.turn && !v.roundFinished) {
                    color = GOLD;
                }
                drawCenteredString(ms, this.font, this.font.plainSubstrByWidth(name, 70), p[0] + TABLE_W / 2, nameY, color);
                // Cartas que le quedan (dorsos)
                int remaining = 3;
                for (int t = 0; t < 3; t++) {
                    if (v.table[t][seat] != null) remaining--;
                }
                if (!v.roundFinished) {
                    for (int k = 0; k < remaining; k++) {
                        drawBack(ms, p[0] - 22 + k * 4, p[1] + 4, 14, 21);
                    }
                }
            }
            // Cartas jugadas en cada baza, apenas desplazadas
            for (int t = 0; t < 3; t++) {
                Card c = v.table[t][seat];
                if (c != null) {
                    drawCard(ms, c, p[0] + t * 9, p[1] + t * 2, TABLE_W, TABLE_H);
                }
            }
            // Globo del último canto
            if (seat == s.lastCallSeat && !s.lastCallText.isEmpty() && !v.roundFinished) {
                int w = this.font.width(s.lastCallText) + 6;
                int bx = p[0] + TABLE_W / 2 - w / 2;
                int by = rel == 0 ? p[1] - 12 : p[1] + TABLE_H + 2;
                fill(ms, bx, by, bx + w, by + 11, 0xEEFFF6D0);
                this.font.draw(ms, s.lastCallText, bx + 3, by + 2, 0x3A2A10);
            }
        }
        // Indicadores de bazas
        int ix = left + 12;
        int iy = top + 12;
        for (int t = 0; t < 3; t++) {        // las tres bazas: verde ganada, roja perdida, gris parda
            int r = v.trickResults[t];
            int color = r == -2 ? 0x50FFFFFF : r == -1 ? 0xFFB0B0B0 : r == v.team ? 0xFF4CAF50 : 0xFFD04040;
            fill(ms, ix + t * 9, iy, ix + t * 9 + 6, iy + 6, color);
        }
    }

    private int handX(PlayerView v, int i) {
        if (!clasica) {
            return manoX0() + i * (manoW + 5);
        }
        int n = v.hand.size();
        int total = n * HAND_W + (n - 1) * 6;
        return left + PANEL_W / 2 - total / 2 + i * (HAND_W + 6);
    }

    private int handY() {
        return clasica ? top + PANEL_H - HAND_H - 10 : this.height - manoH - 8;
    }

    /** Vista compacta, de izquierda a derecha: tantos y muestra (104), la mano (112), la botonera (204). */
    private int bloqueX0() {
        int total = 104 + 3 * manoW + 2 * 5 + 12 + 3 * (BOTON_W + 2);
        return Math.max(4, (this.width - total) / 2);
    }

    private int manoX0() {
        return bloqueX0() + 104;
    }

    private int anchoMano() {
        return clasica ? HAND_W : manoW;
    }

    private int altoMano() {
        return clasica ? HAND_H : manoH;
    }

    private void drawHand(MatrixStack ms, PlayerView v, int mouseX, int mouseY) {
        boolean canPlay = v.isLegal(ActionType.PLAY_CARD);
        int cw = anchoMano(), ch = altoMano();
        for (int i = 0; i < v.hand.size(); i++) {
            int x = handX(v, i);
            int y = handY();
            boolean hover = canPlay && mouseX >= x && mouseX < x + cw && mouseY >= y && mouseY < y + ch;
            if (!clasica && mouseX >= x && mouseX < x + cw && mouseY >= y - 4 && mouseY < y + ch) {
                zoom = v.hand.get(i);           // pasando el mouse: la carta se ve grande
                zoomX = x + cw / 2;
                zoomY = this.height;
            }
            if (hover) {
                y -= 4;
            }
            if (v.handIsPieza[i]) {
                fill(ms, x - 2, y - 2, x + cw + 2, y + ch + 2, GOLD);
            }
            drawCard(ms, v.hand.get(i), x, y, cw, ch);
            this.font.draw(ms, String.valueOf(i + 1), x + cw - 7, y + ch - 9, canPlay ? 0x202020 : 0x909090);
        }
    }

    private void drawMyPoints(MatrixStack ms, PlayerView v) {
        int x = left + 12;
        int y = top + PANEL_H - 26;
        chico(ms, "tu envido " + v.envidoPoints, x, y, 0xC0D8E8D0);
        if (v.hasFlor) {
            chico(ms, "tu flor " + v.florPoints, x, y + 8, 0xFFE8B82A);
        }
    }

    /** Los tantos que cantó cada uno (envido o flor), al lado de su lugar en la mesa. */
    private void drawTantos(MatrixStack ms, TableSnapshot s, PlayerView v) {
        for (int seat = 0; seat < v.players && seat < s.tantos.length; seat++) {
            int t = s.tantos[seat];
            if (t < 0) {
                continue;
            }
            int rel = v.relativePosition(seat);
            int[] p = slot(v, rel);
            String num = String.valueOf(t);
            boolean gana = seat == s.tantoGanador;
            int w = Math.max(16, this.font.width(num) + 8);
            int bx = rel == 0 ? p[0] + TABLE_W + 30 : p[0] + TABLE_W + 18;
            int by = rel == 0 ? p[1] + 8 : p[1] + 12;
            int borde = gana ? 0xFFE8B82A : 0xFF8A7A60;
            fill(ms, bx - 1, by - 1, bx + w + 1, by + 13, borde);
            fill(ms, bx, by, bx + w, by + 12, seat % 2 == v.team ? 0xFF1E3A22 : 0xFF3A1E1E);
            drawCenteredString(ms, this.font, num, bx + w / 2, by + 2, gana ? 0xFFE8B82A : 0xFFF0F0F0);
            chico(ms, s.tantosFlor ? "flor" : "envido", bx, by + 14, 0xA0FFFFFF);
        }
    }

    private void drawCard(MatrixStack ms, Card c, int x, int y, int w, int h) {
        RenderSystem.enableBlend();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        bindCartas();
        int id = c.getId();
        blit(ms, x, y, w, h, (id % 10) * CARD_W, (id / 10) * CARD_H, CARD_W, CARD_H, ATLAS_W, ATLAS_H);
        RenderSystem.disableBlend();
    }

    private void drawBack(MatrixStack ms, int x, int y, int w, int h) {
        RenderSystem.enableBlend();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        bindCartas();
        blit(ms, x, y, w, h, 0, 4 * CARD_H, CARD_W, CARD_H, ATLAS_W, ATLAS_H);
        RenderSystem.disableBlend();
    }

    /** Las cartas son de alta resolución: se achican con filtro suave (sin serrucho). */
    private void bindCartas() {
        TableSnapshot s = snap();
        ResourceLocation atlas = s == null ? CARTAS : com.trucoya.barajas.Baraja.de(s.baraja).atlasGui();
        CartasTextura.usar(atlas);   // con mipmaps: nítidas en cualquier tamaño
    }

    // ------------------------------------------------------------------ vista compacta

    private void initCompacta() {
        TableSnapshot s = snap();
        if (s == null) {
            return;
        }
        if (selPlayers < 0) {
            selPlayers = s.cfgPlayers;
            selTarget = s.cfgTarget;
            selFlor = s.cfgFlor;
        }
        PlayerView v = s.view;
        if (v != null && s.tableRunning && !apuntado) {
            apuntado = true;
            mirarLaMesa();
        }
        if (this.minecraft != null && hudAntes == null) {
            hudAntes = this.minecraft.options.hideGui;
            this.minecraft.options.hideGui = true;     // sin la barra de ítems: se ve la mesa limpia
        }
        if (s.tableRunning && v != null) {
            // la botonera: solo lo que se puede hacer ahora, en tres columnas a la derecha de la mano
            int x0 = manoX0() + 3 * manoW + 2 * 5 + 12;
            int fila = 0, col = 0;
            // con la interfaz grande no entran tres columnas: se usan las que entren
            int columnas = Math.max(1, Math.min(3, (this.width - 4 - x0) / (BOTON_W + 2)));
            int legales = 0;
            for (ActionType a : BUTTONS) if (v.isLegal(a)) legales++;
            int filas = Math.max(3, (legales + columnas - 1) / columnas);
            int y0 = this.height - filas * (BOTON_H + 2) - 8;
            for (ActionType a : BUTTONS) {
                if (!v.isLegal(a)) continue;
                int bx = x0 + col * (BOTON_W + 2);
                int by = y0 + fila * (BOTON_H + 2);
                this.addButton(new Button(bx, by, BOTON_W, BOTON_H, new StringTextComponent(label(a)), btn -> send(a),
                        (btn, ms, mx, my) -> this.renderTooltip(ms, new StringTextComponent("Atajo: " + atajo(a)), mx, my)));
                if (++col == columnas) {
                    col = 0;
                    fila++;
                }
            }
            return;
        }
        if (!s.tableRunning) {
            int bx = this.width / 2 - 60;
            int by = this.height / 2 - 34;
            this.addButton(new Button(bx, by, 120, 16, new StringTextComponent(selPlayers == 4 ? "Modo: 2 contra 2" : "Modo: 1 contra 1"),
                    btn -> { selPlayers = selPlayers == 4 ? 2 : 4; refresh(); }));
            this.addButton(new Button(bx, by + 18, 59, 16, new StringTextComponent("A " + selTarget),
                    btn -> { selTarget = selTarget == 30 ? 40 : 30; refresh(); }));
            this.addButton(new Button(bx + 61, by + 18, 59, 16, new StringTextComponent(selFlor ? "Con flor" : "Sin flor"),
                    btn -> { selFlor = !selFlor; refresh(); }));
            this.addButton(new Button(bx, by + 38, 120, 20, new StringTextComponent("Repartir"),
                    btn -> TrucoNetwork.CHANNEL.sendToServer(TableActionPacket.start(pos(), selPlayers, selTarget, selFlor))));
            this.addButton(new Button(bx, by + 60, 59, 14, new StringTextComponent("Apostar"),
                    btn -> TrucoNetwork.CHANNEL.sendToServer(TableActionPacket.bet(pos()))));
            this.addButton(new Button(bx + 61, by + 60, 59, 14, new StringTextComponent("Retirar"),
                    btn -> TrucoNetwork.CHANNEL.sendToServer(TableActionPacket.withdraw(pos()))));
            if (!s.lugar.isEmpty() && s.torneoRonda == 0) {
                this.addButton(new Button(bx, by + 76, 120, 14, new StringTextComponent("Torneo (5 esmeraldas)"),
                        btn -> TrucoNetwork.CHANNEL.sendToServer(TableActionPacket.torneo(pos())),
                        (btn, ms, mx, my) -> this.renderTooltip(ms, new StringTextComponent(
                                "Tres rivales seguidos, partidos a 20. Premio: 20 esmeraldas y la Copa."), mx, my)));
            }
        }
    }

    /** Al abrir: la cámara mira el centro de la mesa (así las cartas del paño quedan a la vista). */
    private void mirarLaMesa() {
        BlockPos p = pos();
        if (p == null || this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        net.minecraft.client.entity.player.ClientPlayerEntity pl = this.minecraft.player;
        net.minecraft.util.math.vector.Vector3d ojo = pl.getEyePosition(1.0F);
        double dx = p.getX() + 0.5D - ojo.x, dy = p.getY() + 1.0D - ojo.y, dz = p.getZ() + 0.5D - ojo.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) -(Math.atan2(dy, h) * 180.0D / Math.PI) + 6.0F;   // un poquito más abajo: la franja tapa el borde
        pl.yRot = pl.yRotO = yaw;
        pl.xRot = pl.xRotO = net.minecraft.util.math.MathHelper.clamp(pitch, -10.0F, 80.0F);
    }

    private void renderCompacta(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        TableSnapshot s = snap();
        int w = this.width, h = this.height;
        if (s == null) {
            super.render(ms, mouseX, mouseY, partialTicks);
            return;
        }
        PlayerView v = s.view;
        if (v != null && s.tableRunning) {
            // franja de abajo, apenas oscura (la mesa del mundo se sigue viendo arriba)
            this.fillGradient(ms, 0, h - manoH - 22, w, h, 0x00000000, 0xC0100C08);
            drawHand(ms, v, mouseX, mouseY);
            // la muestra y tus tantos, a la izquierda de la mano
            int xm = manoX0() - muestraW - 10;
            if (v.muestra != null) {
                chico(ms, "muestra", xm + 1, h - muestraH - 18, 0xC0E8D8B0);
                drawCard(ms, v.muestra, xm, h - muestraH - 9, muestraW, muestraH);
            }
            chico(ms, "envido " + v.envidoPoints, xm - 52, h - 30, 0xD0D8E8D0);
            if (v.hasFlor) {
                chico(ms, "flor " + v.florPoints, xm - 52, h - 21, 0xFFE8B82A);
            }
            // arriba a la izquierda: el anotador, las bazas y los tantos cantados
            fill(ms, 4, 4, 128, 40, 0x90100C08);
            anotador(ms, 8, 8, "Nos", v.score[v.team], v.inBuenas[v.team], v.targetScore);
            anotador(ms, 8, 23, "Ellos", v.score[1 - v.team], v.inBuenas[1 - v.team], v.targetScore);
            for (int t = 0; t < 3; t++) {
                int r = v.trickResults[t];
                int color = r == -2 ? 0x50FFFFFF : r == -1 ? 0xFFB0B0B0 : r == v.team ? 0xFF4CAF50 : 0xFFD04040;
                fill(ms, 8 + t * 9, 44, 14 + t * 9, 50, color);
            }
            int ty = 56;
            for (int seat = 0; seat < v.players && seat < s.tantos.length; seat++) {
                if (s.tantos[seat] < 0) continue;
                String quien = seat == v.seat ? "Vos" : s.nameOf(seat);
                int col = seat == s.tantoGanador ? 0xFFE8B82A : seat % 2 == v.team ? 0xFF9CE89C : 0xFFF0B0A0;
                chico(ms, (s.tantosFlor ? "Flor " : "Envido ") + quien + ": " + s.tantos[seat], 8, ty, col);
                ty += 8;
            }
            drawPot(ms, s, w - 128, 18, mouseX, mouseY);
            mesaEnPantalla(ms, s, v, mouseX, mouseY);
            historialY = ty + 4;
        } else if (s.tableRunning) {
            centradoConFondo(ms, "Hay una partida en juego en esta mesa.", w / 2, h / 2, 0xFFD8E8D0);
        } else {
            fill(ms, w / 2 - 70, h / 2 - 62, w / 2 + 70, h / 2 + 96, 0xB0100C08);
            String head = v != null && v.matchOver
                    ? (v.matchWinner == v.team ? "¡Ganaron!" : "Perdieron.") + "  Sentados: " + s.seatedHumans
                    : "Sentados a la mesa: " + s.seatedHumans;
            drawCenteredString(ms, this.font, head, w / 2, h / 2 - 56, SOFT);
            String sub = s.clandestina ? "Mesa clandestina: se apuesta fuerte" : s.torneoRonda > 0
                    ? "Torneo · ronda " + s.torneoRonda + " de 3" : s.lugar;
            if (!sub.isEmpty()) {
                chico(ms, this.font.plainSubstrByWidth(sub, 190), w / 2 - 64, h / 2 - 45, GOLD);
            }
            drawPot(ms, s, w / 2 - 60, h / 2 + 100, mouseX, mouseY);
        }
        // arriba al centro: de quién es el turno y el último canto
        centradoConFondo(ms, status(s), w / 2, 6, 0xFFFFFFFF);
        if (v != null && s.lastCallSeat >= 0 && !s.lastCallText.isEmpty() && !v.roundFinished) {
            String quien = s.lastCallSeat == v.seat ? "Vos" : s.nameOf(s.lastCallSeat);
            centradoConFondo(ms, quien + ": " + s.lastCallText, w / 2, 20, 0xFFE8B82A);
        }
        String sena = ClientTrucoState.currentSena();
        if (sena != null) {
            centradoConFondo(ms, this.font.plainSubstrByWidth(sena, w - 40), w / 2, h - manoH - 34,
                    ClientTrucoState.senaFromPartner() ? 0xFFFFF0C8 : 0xFFB0B0B0);
        }
        if (verHistorial) {
            List<String> log = s.log;
            int from = Math.max(0, log.size() - 8);
            // a la izquierda, debajo del anotador (el costado derecho es de las bazas)
            int x0 = 4, ancho = Math.min(210, w / 2 - 40);
            int y0 = Math.max(40, historialY);
            fill(ms, x0, y0, x0 + ancho, y0 + 4 + (log.size() - from) * 8 + 4, 0xC0101010);
            int y = y0 + 4;
            for (int i = from; i < log.size(); i++) {
                chico(ms, this.font.plainSubstrByWidth(log.get(i), (int) ((ancho - 8) / 0.7F)), x0 + 4, y,
                        i == log.size() - 1 ? 0xFFFFFFFF : 0xFFB8D0B0);
                y += 8;
            }
        }
        chico(ms, "V: vista grande · H: historial · Esc: cerrar (seguís sentado)", 4, h - 8, 0x70FFFFFF);
        super.render(ms, mouseX, mouseY, partialTicks);
        if (zoom != null) {
            dibujarZoom(ms, zoom, zoomX, zoomY);
            zoom = null;
        }
    }

    // ------------------------------------------------------------------ las bazas, claras, contra el costado derecho

    /** La carta bajo el mouse, para mostrarla grande (se dibuja al final, encima de todo). */
    @javax.annotation.Nullable
    private Card zoom;
    private int zoomX, zoomY;

    /**
     * Las cartas jugadas en esta mano, grandes y derechas en la pantalla (además de verse sobre la mesa del mundo):
     * un grupo por baza, contra el costado derecho, con quién tiró cada carta y quién ganó la baza.
     */
    private void mesaEnPantalla(MatrixStack ms, TableSnapshot s, PlayerView v, int mouseX, int mouseY) {
        int bazas = 0;
        for (int t = 0; t < 3; t++) {
            boolean alguna = false;
            for (int seat = 0; seat < v.players; seat++) alguna |= v.table[t][seat] != null;
            if (alguna || t == v.trickIndex) bazas = t + 1;
        }
        if (bazas == 0) {
            return;
        }
        // recostadas contra el costado derecho (el centro queda libre para ver la carta que tira el rival en la
        // mesa del mundo). Se busca la grilla (1, 2 o 3 bazas por fila) que deja las cartas más grandes; se calcula
        // siempre para las tres bazas, así el tamaño no cambia a mitad de la mano.
        final int sepX = 10, sepY = 6, rotulos = 7 + 9;
        int arriba = 34;
        if (!s.pot.isEmpty()) {
            arriba = Math.max(arriba, 18 + ((Math.min(14, s.pot.size()) + 6) / 7) * 17 + 8);
        }
        int abajo = this.height - manoH - 30;
        for (net.minecraft.client.gui.widget.Widget b : this.buttons) {
            if (b.visible && b.x + b.getWidth() > this.width / 2) abajo = Math.min(abajo, b.y - 6);   // la botonera
        }
        int maxAncho = Math.min(300, (int) (this.width * 0.45F));
        int altoLibre = Math.max(60, abajo - arriba);
        int ch = 0, cols = 3;
        for (int c = 3; c >= 1; c--) {
            int filas = (3 + c - 1) / c;
            int cwAncho = (maxAncho - (c - 1) * sepX - c * (v.players - 1) * 3) / (c * v.players);
            int chAlto = (altoLibre - filas * rotulos - (filas - 1) * sepY) / filas;
            int prueba = Math.min(60, Math.min(cwAncho * 3 / 2, chAlto));
            if (prueba > ch + 2) {          // a igualdad, menos filas
                ch = prueba;
                cols = c;
            }
        }
        ch = Math.max(26, ch);
        int cw = ch * 2 / 3;
        int gw = v.players * (cw + 3) - 3;
        int filasUsadas = (bazas + cols - 1) / cols;
        int anchoUsado = Math.min(bazas, cols) * gw + (Math.min(bazas, cols) - 1) * sepX;
        int anchoGrilla = cols * gw + (cols - 1) * sepX;
        int x0 = this.width - 8 - anchoGrilla;
        int y0 = arriba + 2;
        int altoFila = ch + rotulos + sepY;
        fill(ms, x0 - 6, y0 - 4, x0 + anchoUsado + 6, y0 + filasUsadas * altoFila - sepY + 2, 0x80100C08);
        String[] orden = {"1ª baza", "2ª baza", "3ª baza"};
        for (int t = 0; t < bazas; t++) {
            int gx = x0 + (t % cols) * (gw + sepX);
            int gy = y0 + (t / cols) * altoFila;
            int r = v.trickResults[t];
            int col = r == -2 ? 0xFFE8DCC0 : r == -1 ? 0xFFB0B0B0 : r == v.team ? 0xFF7CE07C : 0xFFF08070;
            String rotulo = orden[t] + (r == -1 ? " · parda" : r >= 0 ? (r == v.team ? " · nuestra" : " · de ellos") : "");
            chico(ms, this.font.plainSubstrByWidth(rotulo, (int) (Math.max(gw, 40) / 0.7F)), gx, gy - 1, col);
            for (int k = 0; k < v.players; k++) {
                // el orden de la ronda empezando por la mano
                int seat = (v.mano + k) % v.players;
                int cx = gx + k * (cw + 3), cy = gy + 7;
                Card c = v.table[t][seat];
                if (c != null) {
                    fill(ms, cx + 1, cy + 2, cx + cw + 1, cy + ch + 2, 0x60000000);   // sombrita
                    drawCard(ms, c, cx, cy, cw, ch);
                    if (mouseX >= cx && mouseX < cx + cw && mouseY >= cy && mouseY < cy + ch) {
                        zoom = c;
                        zoomX = cx + cw / 2;
                        zoomY = cy + ch + 6;
                    }
                } else if (t == v.trickIndex && !v.roundFinished) {
                    int borde = seat == v.turn ? 0xC0E8B82A : 0x50FFFFFF;      // el que tiene que tirar, en dorado
                    fill(ms, cx, cy, cx + cw, cy + 1, borde);
                    fill(ms, cx, cy + ch - 1, cx + cw, cy + ch, borde);
                    fill(ms, cx, cy, cx + 1, cy + ch, borde);
                    fill(ms, cx + cw - 1, cy, cx + cw, cy + ch, borde);
                }
                String quien = seat == v.seat ? "Vos" : s.nameOf(seat);
                quien = this.font.plainSubstrByWidth(quien, (int) ((cw + 2) / 0.6F));
                ms.pushPose();
                ms.translate(cx, cy + ch + 3, 0);
                ms.scale(0.6F, 0.6F, 1.0F);
                this.font.draw(ms, quien, 0, 0, seat % 2 == v.team ? 0xFF9CE89C : 0xFFF0B0A0);
                ms.popPose();
            }
        }
    }

    /** La carta bajo el mouse, grande, para leerla bien (con sombra y borde). */
    private void dibujarZoom(MatrixStack ms, Card c, int cx, int arriba) {
        int ch = Math.min(this.height - 20, Math.max(96, manoH * 2));
        int cw = ch * 2 / 3;
        int x = Math.max(4, Math.min(this.width - cw - 4, cx - cw / 2));
        int y = arriba;
        if (y + ch > this.height - 4) {
            y = Math.max(4, arriba - ch - manoH - 16);       // la de la mano: se muestra arriba
        }
        ms.pushPose();
        ms.translate(0, 0, 400);
        fill(ms, x + 3, y + 4, x + cw + 3, y + ch + 4, 0x80000000);
        fill(ms, x - 1, y - 1, x + cw + 1, y + ch + 1, 0xFF201810);
        drawCard(ms, c, x, y, cw, ch);
        ms.popPose();
    }

    private void centradoConFondo(MatrixStack ms, String txt, int cx, int y, int color) {
        int tw = this.font.width(txt);
        fill(ms, cx - tw / 2 - 4, y - 2, cx + tw / 2 + 4, y + 10, 0x90000000);
        drawCenteredString(ms, this.font, txt, cx, y, color);
    }

    // ------------------------------------------------------------------ entrada

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        TableSnapshot s = snap();
        if (button == 0 && s != null && s.view != null && s.view.isLegal(ActionType.PLAY_CARD)) {
            for (int i = 0; i < s.view.hand.size(); i++) {
                int x = handX(s.view, i);
                int y = handY() - 4;
                if (mouseX >= x && mouseX < x + anchoMano() && mouseY >= y && mouseY < y + altoMano() + 4) {
                    playCard(i);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        TableSnapshot s = snap();
        PlayerView v = s == null ? null : s.view;
        if (v != null) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_1: playCard(0); return true;
                case GLFW.GLFW_KEY_2: playCard(1); return true;
                case GLFW.GLFW_KEY_3: playCard(2); return true;
                case GLFW.GLFW_KEY_T:
                    for (ActionType a : new ActionType[] {ActionType.TRUCO, ActionType.RETRUCO, ActionType.VALE_CUATRO}) {
                        if (v.isLegal(a)) {
                            send(a);
                            break;
                        }
                    }
                    return true;
                case GLFW.GLFW_KEY_E: send(ActionType.ENVIDO); return true;
                case GLFW.GLFW_KEY_R: send(ActionType.REAL_ENVIDO); return true;
                case GLFW.GLFW_KEY_F: send(ActionType.FALTA_ENVIDO); return true;
                case GLFW.GLFW_KEY_L: send(ActionType.FLOR); return true;
                case GLFW.GLFW_KEY_C: send(ActionType.CONTRAFLOR); return true;
                case GLFW.GLFW_KEY_A: send(ActionType.CONTRAFLOR_AL_RESTO); return true;
                case GLFW.GLFW_KEY_X: send(ActionType.CON_FLOR_ME_ACHICO); return true;
                case GLFW.GLFW_KEY_Q: send(ActionType.QUIERO); return true;
                case GLFW.GLFW_KEY_N: send(ActionType.NO_QUIERO); return true;
                case GLFW.GLFW_KEY_M: send(ActionType.ME_VOY_AL_MAZO); return true;
                case GLFW.GLFW_KEY_H: verHistorial = !verHistorial; return true;
                case GLFW.GLFW_KEY_V: clasica = !clasica; refresh(); return true;
                default: break;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_V && v == null) {
            clasica = !clasica;
            refresh();
            return true;
        }
        if (ModKeys.SENAS.matches(keyCode, scanCode) && v != null) {
            this.minecraft.setScreen(new SenasScreen(this));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
