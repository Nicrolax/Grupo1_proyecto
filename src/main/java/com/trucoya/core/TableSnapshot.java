package com.trucoya.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Todo lo que el cliente necesita para dibujar la mesa: la vista privada del jugador (si hay
 * partida), los nombres de los asientos, el registro de cantos y la configuración elegida.
 */
public final class TableSnapshot {

    public static final int MAX_LOG = 8;
    public static final int MAX_POT = 24;

    /** Una entrada del pozo de apuestas: ítem (id de registro), cantidad y dueño. */
    public static final class PotEntry {
        public final String itemId;
        public final int count;
        public final String owner;

        public PotEntry(String itemId, int count, String owner) {
            this.itemId = itemId == null ? "" : itemId;
            this.count = count;
            this.owner = owner == null ? "" : owner;
        }
    }

    /** null si no hay partida en curso ni terminada. */
    public final PlayerView view;
    /** Nombres por asiento del motor (vacío si no hay partida). */
    public final List<String> names;
    public final boolean[] isBot;
    public final List<String> log;
    /** Asiento que hizo el último canto y su texto (para el globo dentro del tapete). */
    public final int lastCallSeat;
    public final String lastCallText;
    /** Configuración propuesta para la próxima partida. */
    public final int cfgPlayers;
    public final int cfgTarget;
    public final boolean cfgFlor;
    /** Jugadores humanos sentados a la mesa. */
    public final int seatedHumans;
    /** Hay una partida en juego en la mesa (aunque este jugador no participe). */
    public final boolean tableRunning;
    /** El cliente debe abrir el tapete si estaba cerrado. */
    public final boolean open;
    /** Prestigio Criollo del jugador que recibe la vista ("Nivel 2 · Parroquiano (75 pts)"). */
    public final String myPrestige;
    /** Pozo de apuestas en juego o propuesto. */
    public final List<PotEntry> pot;
    /** Mesa de la guarida clandestina: solo se aceptan apuestas de alto valor. */
    public final boolean clandestina;
    /** Tantos cantados en esta mano por asiento (-1: no cantó). Se ven como en la mesa de verdad. */
    public final int[] tantos;
    /** Los tantos son de flor (si no, de envido). */
    public final boolean tantosFlor;
    /** Asiento que ganó el envido o la flor (-1 si todavía no se resolvió). */
    public final int tantoGanador;
    /** Dónde está la mesa en la historia ("Pulpería de la Sierra · ..."), vacío si no es una pulpería. */
    public final String lugar;
    /** Ronda del torneo en curso (0: no hay torneo). */
    public final int torneoRonda;
    /** La baraja con la que se juega en esta mesa (número de barajas.Baraja). */
    public int baraja;

    public TableSnapshot(PlayerView view, List<String> names, boolean[] isBot, List<String> log,
                         int lastCallSeat, String lastCallText, int cfgPlayers, int cfgTarget, boolean cfgFlor,
                         int seatedHumans, boolean tableRunning, boolean open,
                         String myPrestige, List<PotEntry> pot, boolean clandestina) {
        this(view, names, isBot, log, lastCallSeat, lastCallText, cfgPlayers, cfgTarget, cfgFlor, seatedHumans,
                tableRunning, open, myPrestige, pot, clandestina, new int[] {-1, -1, -1, -1}, false, -1, "", 0);
    }

    public TableSnapshot(PlayerView view, List<String> names, boolean[] isBot, List<String> log,
                         int lastCallSeat, String lastCallText, int cfgPlayers, int cfgTarget, boolean cfgFlor,
                         int seatedHumans, boolean tableRunning, boolean open,
                         String myPrestige, List<PotEntry> pot, boolean clandestina,
                         int[] tantos, boolean tantosFlor, int tantoGanador, String lugar, int torneoRonda) {
        this.view = view;
        this.names = Collections.unmodifiableList(new ArrayList<String>(names));
        this.isBot = isBot.clone();
        List<String> l = new ArrayList<String>(log);
        while (l.size() > MAX_LOG) {
            l.remove(0);
        }
        this.log = Collections.unmodifiableList(l);
        this.lastCallSeat = lastCallSeat;
        this.lastCallText = lastCallText == null ? "" : lastCallText;
        this.cfgPlayers = cfgPlayers;
        this.cfgTarget = cfgTarget;
        this.cfgFlor = cfgFlor;
        this.seatedHumans = seatedHumans;
        this.tableRunning = tableRunning;
        this.open = open;
        this.myPrestige = myPrestige == null ? "" : myPrestige;
        List<PotEntry> p = new ArrayList<PotEntry>(pot == null ? Collections.<PotEntry>emptyList() : pot);
        while (p.size() > MAX_POT) {
            p.remove(p.size() - 1);
        }
        this.pot = Collections.unmodifiableList(p);
        this.clandestina = clandestina;
        this.tantos = new int[4];
        for (int i = 0; i < 4; i++) {
            this.tantos[i] = tantos != null && i < tantos.length ? tantos[i] : -1;
        }
        this.tantosFlor = tantosFlor;
        this.tantoGanador = tantoGanador;
        this.lugar = lugar == null ? "" : lugar;
        this.torneoRonda = Math.max(0, Math.min(9, torneoRonda));
    }

    /** ¿Hay una partida en juego (no terminada)? */
    public boolean isRunning() {
        return view != null && !view.matchOver;
    }

    public String nameOf(int seat) {
        return seat >= 0 && seat < names.size() ? names.get(seat) : "?";
    }

    public void write(DataOutput out) throws IOException {
        out.writeBoolean(view != null);
        if (view != null) {
            view.write(out);
        }
        out.writeByte(names.size());
        for (int i = 0; i < names.size(); i++) {
            out.writeUTF(clip(names.get(i), 40));
            out.writeBoolean(i < isBot.length && isBot[i]);
        }
        out.writeByte(log.size());
        for (String s : log) {
            out.writeUTF(clip(s, 120));
        }
        out.writeByte(lastCallSeat);
        out.writeUTF(clip(lastCallText, 60));
        out.writeByte(cfgPlayers);
        out.writeByte(cfgTarget);
        out.writeBoolean(cfgFlor);
        out.writeByte(seatedHumans);
        out.writeBoolean(tableRunning);
        out.writeBoolean(open);
        out.writeUTF(clip(myPrestige, 80));
        out.writeByte(pot.size());
        for (PotEntry e : pot) {
            out.writeUTF(clip(e.itemId, 100));
            out.writeShort(e.count);
            out.writeUTF(clip(e.owner, 40));
        }
        out.writeBoolean(clandestina);
        for (int i = 0; i < 4; i++) {
            out.writeByte(tantos[i]);
        }
        out.writeBoolean(tantosFlor);
        out.writeByte(tantoGanador);
        out.writeUTF(clip(lugar, 100));
        out.writeByte(torneoRonda);
        out.writeByte(baraja);
    }

    public static TableSnapshot read(DataInput in) throws IOException {
        PlayerView v = in.readBoolean() ? PlayerView.read(in) : null;
        int n = in.readByte();
        if (n < 0 || n > 4) {
            throw new IOException("Cantidad de nombres inválida");
        }
        List<String> names = new ArrayList<String>(n);
        boolean[] bots = new boolean[n];
        for (int i = 0; i < n; i++) {
            names.add(in.readUTF());
            bots[i] = in.readBoolean();
        }
        int ln = in.readByte();
        if (ln < 0 || ln > MAX_LOG) {
            throw new IOException("Registro inválido");
        }
        List<String> log = new ArrayList<String>(ln);
        for (int i = 0; i < ln; i++) {
            log.add(in.readUTF());
        }
        int lastSeat = in.readByte();
        String lastText = in.readUTF();
        int p = in.readByte();
        int t = in.readByte();
        boolean f = in.readBoolean();
        int seated = in.readByte();
        boolean running = in.readBoolean();
        boolean open = in.readBoolean();
        String prestige = in.readUTF();
        int pn = in.readByte();
        if (pn < 0 || pn > MAX_POT) {
            throw new IOException("Pozo inválido");
        }
        List<PotEntry> pot = new ArrayList<PotEntry>(pn);
        for (int i = 0; i < pn; i++) {
            pot.add(new PotEntry(in.readUTF(), in.readShort(), in.readUTF()));
        }
        boolean clandestina = in.readBoolean();
        int[] tantos = new int[4];
        for (int i = 0; i < 4; i++) {
            int x = in.readByte();
            tantos[i] = x >= 0 && x <= 40 ? x : -1;
        }
        boolean tantosFlor = in.readBoolean();
        int ganador = in.readByte();
        String lugar = in.readUTF();
        int torneo = in.readByte();
        TableSnapshot snap = new TableSnapshot(v, names, bots, log, lastSeat, lastText,
                p == 4 ? 4 : 2, t == 40 ? 40 : 30, f, seated, running, open, prestige, pot, clandestina,
                tantos, tantosFlor, ganador >= 0 && ganador < 4 ? ganador : -1, lugar, torneo);
        snap.baraja = in.readByte();
        return snap;
    }

    public byte[] toBytes() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(256);
            write(new DataOutputStream(bos));
            return bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static TableSnapshot fromBytes(byte[] data) throws IOException {
        return read(new DataInputStream(new ByteArrayInputStream(data)));
    }

    private static String clip(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
