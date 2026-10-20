package com.trucoya.network;

import com.trucoya.core.ActionType;
import com.trucoya.core.Sena;
import com.trucoya.tileentity.MesaTrucoTileEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Cliente → servidor: intención del jugador en una mesa (botón, carta, seña, repartir, abrir).
 * El servidor valida todo; un paquete inválido o fuera de lugar simplemente se ignora.
 */
public class TableActionPacket {

    public static final byte ACTION = 0;
    public static final byte CARD = 1;
    public static final byte START = 2;
    public static final byte SENA = 3;
    public static final byte OPEN = 4;
    public static final byte BET = 5;
    public static final byte WITHDRAW = 6;
    public static final byte TORNEO = 7;

    private final BlockPos pos;
    private final byte kind;
    private final int a;
    private final int b;
    private final int c;

    public TableActionPacket(BlockPos pos, byte kind, int a, int b, int c) {
        this.pos = pos;
        this.kind = kind;
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public static TableActionPacket action(BlockPos pos, ActionType action) {
        return new TableActionPacket(pos, ACTION, action.ordinal(), 0, 0);
    }

    public static TableActionPacket card(BlockPos pos, int cardId) {
        return new TableActionPacket(pos, CARD, cardId, 0, 0);
    }

    public static TableActionPacket start(BlockPos pos, int players, int target, boolean flor) {
        return new TableActionPacket(pos, START, players, target, flor ? 1 : 0);
    }

    public static TableActionPacket sena(BlockPos pos, Sena sena) {
        return new TableActionPacket(pos, SENA, sena.ordinal(), 0, 0);
    }

    public static TableActionPacket open(BlockPos pos) {
        return new TableActionPacket(pos, OPEN, 0, 0, 0);
    }

    public static TableActionPacket bet(BlockPos pos) {
        return new TableActionPacket(pos, BET, 0, 0, 0);
    }

    public static TableActionPacket withdraw(BlockPos pos) {
        return new TableActionPacket(pos, WITHDRAW, 0, 0, 0);
    }

    public static TableActionPacket torneo(BlockPos pos) {
        return new TableActionPacket(pos, TORNEO, 0, 0, 0);
    }

    public static void encode(TableActionPacket msg, PacketBuffer buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeByte(msg.kind);
        buf.writeVarInt(msg.a);
        buf.writeVarInt(msg.b);
        buf.writeVarInt(msg.c);
    }

    public static TableActionPacket decode(PacketBuffer buf) {
        return new TableActionPacket(buf.readBlockPos(), buf.readByte(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    @SuppressWarnings("deprecation")
    public static void handle(TableActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            World world = player.level;
            if (!world.hasChunkAt(msg.pos)) {
                return;
            }
            TileEntity te = world.getBlockEntity(msg.pos);
            if (!(te instanceof MesaTrucoTileEntity)) {
                return;
            }
            MesaTrucoTileEntity table = (MesaTrucoTileEntity) te;
            switch (msg.kind) {
                case ACTION: {
                    ActionType[] all = ActionType.values();
                    if (msg.a >= 0 && msg.a < all.length) {
                        table.handleAction(player, all[msg.a]);
                    }
                    break;
                }
                case CARD:
                    if (msg.a >= 0 && msg.a < 40) {
                        table.handleCard(player, msg.a);
                    }
                    break;
                case START:
                    table.requestStart(player, msg.a, msg.b, msg.c != 0);
                    break;
                case SENA:
                    table.handleSena(player, Sena.byOrdinal(msg.a));
                    break;
                case OPEN:
                    if (table.isSeatedHere(player)) {
                        table.openFor(player);
                    }
                    break;
                case BET:
                    table.handleBet(player);
                    break;
                case WITHDRAW:
                    table.handleWithdraw(player);
                    break;
                case TORNEO:
                    table.iniciarTorneo(player);
                    break;
                default:
                    break;
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
