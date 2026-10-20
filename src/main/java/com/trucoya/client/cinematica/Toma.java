package com.trucoya.client.cinematica;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.DoubleFunction;

/**
 * Un plano de cámara entre {@link #t0} y {@link #t1} (segundos de la cinemática).
 * Las coordenadas son locales a su {@link Marco}: el presente (centrado en el Vizcacha, mirando al jugador)
 * o el recuerdo (el escenario de la batalla).
 */
@OnlyIn(Dist.CLIENT)
abstract class Toma {

    enum Marco { PRESENTE, RECUERDO }

    /** Pose de cámara: dónde está, a dónde mira, apertura y giro. */
    static final class Pose {
        final Vector3d pos;
        final Vector3d mira;
        final double fov;
        final double roll;

        Pose(Vector3d pos, Vector3d mira, double fov, double roll) {
            this.pos = pos;
            this.mira = mira;
            this.fov = fov;
            this.roll = roll;
        }
    }

    final double t0;
    final double t1;
    final Marco marco;

    Toma(double t0, double t1, Marco marco) {
        this.t0 = t0;
        this.t1 = t1;
        this.marco = marco;
    }

    abstract Pose pose(double t);

    double u(double t) {
        return MathHelper.clamp((t - t0) / (t1 - t0), 0.0D, 1.0D);
    }

    // ------------------------------------------------------------------ fábricas

    /**
     * Un movimiento de grúa o travelling: la cámara recorre {@code pos} y la mirada {@code mira}
     * (curvas Catmull-Rom), con aceleración y frenado suaves.
     */
    static Toma camino(Marco marco, double t0, double t1, Vector3d[] pos, Vector3d[] mira,
                       double fov0, double fov1, double temblor) {
        return new Toma(t0, t1, marco) {
            @Override
            Pose pose(double t) {
                double u = suave(u(t));
                Vector3d p = catmull(pos, u).add(temblor(t, temblor));
                return new Pose(p, catmull(mira, u), fov0 + (fov1 - fov0) * u, temblor * 18.0D * Math.sin(t * 1.3D));
            }
        };
    }

    /** Un plano calculado a mano (cámaras que siguen a los jinetes). */
    static Toma libre(Marco marco, double t0, double t1, DoubleFunction<Pose> f) {
        return new Toma(t0, t1, marco) {
            @Override
            Pose pose(double t) {
                return f.apply(t);
            }
        };
    }

    // ------------------------------------------------------------------ matemática

    /** Aceleración y frenado sinusoidal (el "ease in-out" del cine). */
    static double suave(double u) {
        return 0.5D - 0.5D * Math.cos(Math.PI * MathHelper.clamp(u, 0.0D, 1.0D));
    }

    static Vector3d v(double x, double y, double z) {
        return new Vector3d(x, y, z);
    }

    /** Curva Catmull-Rom que pasa por todos los puntos (los extremos se repiten). */
    static Vector3d catmull(Vector3d[] p, double u) {
        if (p.length == 1) {
            return p[0];
        }
        double s = MathHelper.clamp(u, 0.0D, 1.0D) * (p.length - 1);
        int i = Math.min((int) Math.floor(s), p.length - 2);
        double f = s - i;
        Vector3d p0 = p[Math.max(0, i - 1)];
        Vector3d p1 = p[i];
        Vector3d p2 = p[i + 1];
        Vector3d p3 = p[Math.min(p.length - 1, i + 2)];
        double f2 = f * f;
        double f3 = f2 * f;
        return new Vector3d(
                cr(p0.x, p1.x, p2.x, p3.x, f, f2, f3),
                cr(p0.y, p1.y, p2.y, p3.y, f, f2, f3),
                cr(p0.z, p1.z, p2.z, p3.z, f, f2, f3));
    }

    private static double cr(double a, double b, double c, double d, double f, double f2, double f3) {
        return 0.5D * (2.0D * b + (-a + c) * f + (2.0D * a - 5.0D * b + 4.0D * c - d) * f2 + (-a + 3.0D * b - 3.0D * c + d) * f3);
    }

    /** Cámara en mano: suma de senos desfasados (no se repite a simple vista). */
    static Vector3d temblor(double t, double a) {
        if (a <= 0.0D) {
            return Vector3d.ZERO;
        }
        return new Vector3d(
                a * (0.6D * Math.sin(t * 1.7D) + 0.3D * Math.sin(t * 3.9D + 1.0D) + 0.1D * Math.sin(t * 9.1D + 2.0D)),
                a * (0.6D * Math.sin(t * 1.3D + 4.0D) + 0.3D * Math.sin(t * 4.7D + 3.0D) + 0.1D * Math.sin(t * 11.3D)),
                a * (0.6D * Math.sin(t * 1.1D + 2.0D) + 0.3D * Math.sin(t * 3.3D + 5.0D) + 0.1D * Math.sin(t * 8.3D + 1.0D)));
    }

    static Vector3d lerp(Vector3d a, Vector3d b, double u) {
        return new Vector3d(a.x + (b.x - a.x) * u, a.y + (b.y - a.y) * u, a.z + (b.z - a.z) * u);
    }
}
