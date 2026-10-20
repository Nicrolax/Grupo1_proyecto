package com.trucoya.core;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Instantánea de lo que un jugador puede ver. Es lo único que el servidor envía al cliente
 * para dibujar el tapete y la botonera: no contiene las cartas ocultas de los rivales ni del
 * compañero, lo que impide trampas por inspección de paquetes.
 *
 * Se serializa con {@link #write(DataOutput)} / {@link #read(DataInput)} (sin dependencias de
 * Minecraft; en Forge se usa sobre un {@code ByteBufOutputStream}).
 */
public final class PlayerView {

    public final int seat;
    public final int team;
    public final int players;
    public final int targetScore;
    public final int[] score;
    public final String[] stageLabel;
    public final boolean[] inBuenas;

    public final Card muestra;
    public final List<Card> hand;
    /** Para resaltar bordes en la GUI: true si la carta de la mano es Pieza activa. */
    public final boolean[] handIsPieza;
    public final int envidoPoints;
    public final boolean hasFlor;
    public final int florPoints;

    /** Cartas jugadas en la mesa: [baza][asiento], null si no se jugó. */
    public final Card[][] table;
    /** Resultado de cada baza: equipo ganador, -1 parda, -2 sin jugar. */
    public final int[] trickResults;
    public final int trickIndex;

    public final int dealer;
    public final int mano;
    public final int turn;
    public final int trucoLevel;
    public final ActionType pendingCall;
    public final int pendingAwaitingTeam;

    public final List<ActionType> legalActions;
    public final boolean roundFinished;
    public final boolean matchOver;
    public final int matchWinner;

    PlayerView(TrucoMatch m, int seat) {
        this.seat = seat;
        this.team = seat % 2;
        this.players = m.getConfig().getPlayers();
        this.targetScore = m.getConfig().getTargetScore();
        this.score = new int[] {m.getScore(0), m.getScore(1)};
        this.stageLabel = new String[] {m.stageLabel(0), m.stageLabel(1)};
        this.inBuenas = new boolean[] {m.isInBuenas(0), m.isInBuenas(1)};

        this.muestra = m.getMuestra();
        this.hand = Collections.unmodifiableList(new ArrayList<Card>(m.getHand(seat)));
        CardRules rules = m.getRules();
        this.handIsPieza = new boolean[hand.size()];
        if (rules != null) {
            for (int i = 0; i < hand.size(); i++) {
                handIsPieza[i] = rules.isPieza(hand.get(i));
            }
            List<Card> dealt = m.getDealtHand(seat);
            this.envidoPoints = dealt.isEmpty() ? 0 : rules.envidoPoints(dealt);
            this.hasFlor = m.getConfig().isFlorEnabled() && rules.hasFlor(dealt);
            this.florPoints = hasFlor ? rules.florPoints(dealt) : 0;
        } else {
            this.envidoPoints = 0;
            this.hasFlor = false;
            this.florPoints = 0;
        }

        this.table = new Card[3][players];
        this.trickResults = new int[3];
        for (int t = 0; t < 3; t++) {
            trickResults[t] = m.getTrickResult(t);
            for (int s = 0; s < players; s++) {
                table[t][s] = m.getTrickCard(t, s);
            }
        }
        this.trickIndex = m.getTrickIndex();
        this.dealer = m.getDealer();
        this.mano = m.getMano();
        this.turn = m.getTurn();
        this.trucoLevel = m.getTrucoLevel();
        this.pendingCall = m.getPendingCall();
        this.pendingAwaitingTeam = m.getPendingAwaitingTeam();
        this.legalActions = m.legalActions(seat);
        this.roundFinished = m.isRoundFinished();
        this.matchOver = m.isOver();
        this.matchWinner = m.getWinner();
    }

    // ------------------------------------------------------------------ serialización

    private static final int MAGIC = 0x54590001; // "TY" v1

    public void write(DataOutput out) throws IOException {
        out.writeInt(MAGIC);
        out.writeByte(seat);
        out.writeByte(players);
        out.writeByte(targetScore);
        for (int t = 0; t < 2; t++) {
            out.writeByte(score[t]);
            out.writeUTF(stageLabel[t]);
            out.writeBoolean(inBuenas[t]);
        }
        writeCard(out, muestra);
        out.writeByte(hand.size());
        for (int i = 0; i < hand.size(); i++) {
            writeCard(out, hand.get(i));
            out.writeBoolean(handIsPieza[i]);
        }
        out.writeByte(envidoPoints);
        out.writeBoolean(hasFlor);
        out.writeByte(florPoints);
        for (int t = 0; t < 3; t++) {
            out.writeByte(trickResults[t]);
            for (int s = 0; s < players; s++) {
                writeCard(out, table[t][s]);
            }
        }
        out.writeByte(trickIndex);
        out.writeByte(dealer);
        out.writeByte(mano);
        out.writeByte(turn);
        out.writeByte(trucoLevel);
        out.writeByte(pendingCall == null ? -1 : pendingCall.ordinal());
        out.writeByte(pendingAwaitingTeam);
        out.writeByte(legalActions.size());
        for (ActionType a : legalActions) {
            out.writeByte(a.ordinal());
        }
        out.writeBoolean(roundFinished);
        out.writeBoolean(matchOver);
        out.writeByte(matchWinner);
    }

    public static PlayerView read(DataInput in) throws IOException {
        return new PlayerView(in);
    }

    private PlayerView(DataInput in) throws IOException {
        if (in.readInt() != MAGIC) {
            throw new IOException("Versión de vista incompatible");
        }
        seat = checkRange(in.readByte(), 0, 3);
        team = seat % 2;
        players = in.readByte();
        if (players != 2 && players != 4) {
            throw new IOException("Cantidad de jugadores inválida");
        }
        targetScore = in.readByte();
        score = new int[2];
        stageLabel = new String[2];
        inBuenas = new boolean[2];
        for (int t = 0; t < 2; t++) {
            score[t] = in.readByte();
            stageLabel[t] = in.readUTF();
            inBuenas[t] = in.readBoolean();
        }
        muestra = readCard(in);
        int hs = checkRange(in.readByte(), 0, 3);
        List<Card> h = new ArrayList<Card>(hs);
        handIsPieza = new boolean[hs];
        for (int i = 0; i < hs; i++) {
            Card c = readCard(in);
            if (c == null) {
                throw new IOException("Carta nula en la mano");
            }
            h.add(c);
            handIsPieza[i] = in.readBoolean();
        }
        hand = Collections.unmodifiableList(h);
        envidoPoints = in.readByte();
        hasFlor = in.readBoolean();
        florPoints = in.readByte();
        table = new Card[3][players];
        trickResults = new int[3];
        for (int t = 0; t < 3; t++) {
            trickResults[t] = in.readByte();
            for (int s = 0; s < players; s++) {
                table[t][s] = readCard(in);
            }
        }
        trickIndex = in.readByte();
        dealer = in.readByte();
        mano = in.readByte();
        turn = in.readByte();
        trucoLevel = in.readByte();
        int pc = in.readByte();
        pendingCall = pc < 0 ? null : actionAt(pc);
        pendingAwaitingTeam = in.readByte();
        int la = checkRange(in.readByte(), 0, ActionType.values().length);
        List<ActionType> legal = new ArrayList<ActionType>(la);
        for (int i = 0; i < la; i++) {
            legal.add(actionAt(in.readByte()));
        }
        legalActions = Collections.unmodifiableList(legal);
        roundFinished = in.readBoolean();
        matchOver = in.readBoolean();
        matchWinner = in.readByte();
    }

    private static int checkRange(int v, int min, int max) throws IOException {
        if (v < min || v > max) {
            throw new IOException("Valor fuera de rango: " + v);
        }
        return v;
    }

    private static ActionType actionAt(int ordinal) throws IOException {
        ActionType[] all = ActionType.values();
        if (ordinal < 0 || ordinal >= all.length) {
            throw new IOException("Acción inválida: " + ordinal);
        }
        return all[ordinal];
    }

    static void writeCard(DataOutput out, Card c) throws IOException {
        out.writeByte(c == null ? -1 : c.getId());
    }

    static Card readCard(DataInput in) throws IOException {
        int id = in.readByte();
        if (id == -1) {
            return null;
        }
        if (id < 0 || id >= Card.DECK_SIZE) {
            throw new IOException("Carta inválida: " + id);
        }
        return Card.byId(id);
    }

    public boolean isLegal(ActionType a) {
        return legalActions.contains(a);
    }

    /** Posición relativa de un asiento para dibujar: 0 abajo (yo), luego en sentido horario. */
    public int relativePosition(int otherSeat) {
        return ((otherSeat - seat) % players + players) % players;
    }
}
