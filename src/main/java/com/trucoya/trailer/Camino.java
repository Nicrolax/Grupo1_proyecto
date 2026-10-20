package com.trucoya.trailer;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.List;

/**
 * El recorrido de la cámara del modo tráiler: pasa por las marcas (en orden) con curvas suaves, y la mirada
 * también se curva entre hacia dónde miraba cada marca. Lo usan el cliente (la cámara) y el servidor (que lleva
 * al jugador por el mismo camino, así el mundo se carga donde está la cámara).
 */
public final class Camino {

    /** Una marca: donde estaban los ojos y hacia dónde miraban. */
    public static final class Marca {
        public final double x, y, z;
        public final float yaw, pitch;

        public Marca(double x, double y, double z, float yaw, float pitch) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        Vector3d pos() {
            return new Vector3d(x, y, z);
        }

        /** Un punto a 12 bloques hacia donde mira. */
        Vector3d mira() {
            return pos().add(Vector3d.directionFromRotation(pitch, yaw).scale(12.0D));
        }
    }

    public static final int MAXIMO = 16;

    public final List<Marca> marcas;
    /** Segundos que tarda en ir de una marca a la siguiente. */
    public final float segundos;

    public Camino(List<Marca> marcas, float segundos) {
        this.marcas = marcas;
        this.segundos = segundos;
    }

    public double duracion() {
        return Math.max(1, marcas.size() - 1) * segundos;
    }

    /** Cuántos tramos van recorridos a los {@code t} segundos (con arranque y frenado suaves en las puntas). */
    private double u(double t) {
        int tramos = Math.max(1, marcas.size() - 1);
        double f = MathHelper.clamp(t / duracion(), 0.0D, 1.0D);
        // arranca de quieta, acelera, va pareja y frena hasta quedar quieta al final
        double borde = Math.min(0.5D, 1.0D / tramos);
        double v = 1.0D / (1.0D - borde);
        double g;
        if (f < borde) {
            g = v * f * f / (2.0D * borde);
        } else if (f > 1.0D - borde) {
            g = 1.0D - v * (1.0D - f) * (1.0D - f) / (2.0D * borde);
        } else {
            g = v * (f - borde / 2.0D);
        }
        return MathHelper.clamp(g, 0.0D, 1.0D) * tramos;
    }

    public Vector3d posicion(double t) {
        List<Vector3d> p = new ArrayList<>();
        for (Marca m : marcas) p.add(m.pos());
        return curva(p, u(t));
    }

    public Vector3d mira(double t) {
        List<Vector3d> p = new ArrayList<>();
        for (Marca m : marcas) p.add(m.mira());
        return curva(p, u(t));
    }

    /** Catmull-Rom por los puntos, con u entre 0 y n-1. */
    private static Vector3d curva(List<Vector3d> p, double u) {
        int n = p.size();
        if (n == 1) {
            return p.get(0);
        }
        int i = MathHelper.clamp((int) Math.floor(u), 0, n - 2);
        double f = u - i;
        Vector3d p0 = p.get(Math.max(0, i - 1)), p1 = p.get(i), p2 = p.get(i + 1), p3 = p.get(Math.min(n - 1, i + 2));
        double f2 = f * f, f3 = f2 * f;
        return new Vector3d(cr(p0.x, p1.x, p2.x, p3.x, f, f2, f3), cr(p0.y, p1.y, p2.y, p3.y, f, f2, f3),
                cr(p0.z, p1.z, p2.z, p3.z, f, f2, f3));
    }

    private static double cr(double a, double b, double c, double d, double f, double f2, double f3) {
        return 0.5D * (2 * b + (-a + c) * f + (2 * a - 5 * b + 4 * c - d) * f2 + (-a + 3 * b - 3 * c + d) * f3);
    }

    // ------------------------------------------------------------------ red

    public void escribir(PacketBuffer b) {
        b.writeFloat(segundos);
        b.writeVarInt(marcas.size());
        for (Marca m : marcas) {
            b.writeDouble(m.x);
            b.writeDouble(m.y);
            b.writeDouble(m.z);
            b.writeFloat(m.yaw);
            b.writeFloat(m.pitch);
        }
    }

    public static Camino leer(PacketBuffer b) {
        float seg = MathHelper.clamp(b.readFloat(), 1.0F, 60.0F);
        int n = Math.min(b.readVarInt(), MAXIMO);
        List<Marca> l = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            l.add(new Marca(b.readDouble(), b.readDouble(), b.readDouble(), b.readFloat(), b.readFloat()));
        }
        return new Camino(l, seg);
    }
}
