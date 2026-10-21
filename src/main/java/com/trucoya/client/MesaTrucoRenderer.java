package com.trucoya.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.trucoya.barajas.Baraja;
import com.trucoya.tileentity.MesaTrucoTileEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.math.vector.Matrix3f;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Las cartas sobre la mesa, en el mundo: cada uno juega sus cartas delante suyo (de izquierda a derecha, como
 * en el boliche), la muestra queda boca arriba debajo del mazo, en la esquina de quien repartió. Las ve todo el
 * que esté cerca, no solo los que juegan.
 */
@OnlyIn(Dist.CLIENT)
public class MesaTrucoRenderer extends TileEntityRenderer<MesaTrucoTileEntity> {

    /** Medio ancho y media altura de una carta, en bloques (una carta mide 0,21 x 0,315: se lee desde la silla). */
    private static final float HW = 0.105F, HH = 0.1575F;
    /** Distancia del centro de la mesa al centro de las cartas de cada uno, y separación entre bazas. */
    private static final float FONDO = 0.325F, PASO = 0.222F;
    /** Luz mínima sobre las cartas (de 0 a 15): de noche o en un rancho oscuro se siguen leyendo. */
    private static final int LUZ_MINIMA = 11;
    /** Lados de la mesa: norte, este, sur, oeste (hacia donde está sentado cada uno). */
    private static final float[][] LADO = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

    public MesaTrucoRenderer(TileEntityRendererDispatcher d) {
        super(d);
    }

    @Override
    public void render(MesaTrucoTileEntity te, float pt, MatrixStack ms, IRenderTypeBuffer buffers, int luz, int overlay) {
        MesaTrucoTileEntity.VistaMesa v = te.vista;
        if (!v.enJuego || te.getLevel() == null) {
            return;
        }
        int luzMundo = WorldRenderer.getLightColor(te.getLevel(), te.getBlockPos().above());
        int light = net.minecraft.client.renderer.LightTexture.pack(
                Math.max(LUZ_MINIMA, net.minecraft.client.renderer.LightTexture.block(luzMundo)),
                net.minecraft.client.renderer.LightTexture.sky(luzMundo));
        // la misma textura grande del tapete, con mipmaps: nítida de cerca y sin titilar de lejos
        IVertexBuilder b = buffers.getBuffer(RenderType.entityCutoutNoCull(
                CartasTextura.preparar(Baraja.de(v.baraja).atlasGui())));
        // con el Anotador de porotos encima de la mesa, las cartas van apoyadas arriba de la tablita y de los
        // porotos (si no, quedaban tapadas en el medio de la mesa)
        float y0 = te.getLevel().getBlockState(te.getBlockPos().above()).getBlock()
                instanceof com.trucoya.block.AnotadorBlock ? 0.115F : 0.0F;
        ms.pushPose();
        ms.translate(0.0D, y0, 0.0D);
        Matrix4f m = ms.last().pose();
        Matrix3f n = ms.last().normal();
        // las cartas jugadas, delante de cada uno
        for (int s = 0; s < v.jugadores; s++) {
            int silla = Math.floorMod(v.silla[s], 4);
            float lx = LADO[silla][0], lz = LADO[silla][1];
            float fx = -lx, fz = -lz;            // hacia el centro de la mesa
            float rx = -fz, rz = fx;             // la derecha de quien está sentado
            for (int t = 0; t < 3; t++) {
                int id = v.jugadas[t * 4 + s];
                if (id < 0) continue;
                float giro = (((id * 37 + t * 11 + s * 5) % 13) - 6) * 0.018F;   // ninguna queda derechita
                float c = (float) Math.cos(giro), sn = (float) Math.sin(giro);
                float ax = rx * c - fx * sn, az = rz * c - fz * sn;   // eje del ancho, girado
                float bx = fx * c + rx * sn, bz = fz * c + rz * sn;   // eje del alto (hacia el centro), girado
                float cx = 0.5F + lx * FONDO + rx * (t - 1) * PASO;
                float cz = 0.5F + lz * FONDO + rz * (t - 1) * PASO;
                // cada baza un poquito más arriba (y cada asiento a su altura): nunca se pisan dos cartas
                carta(b, m, n, light, cx, 1.002F + t * 0.0016F + s * 0.0004F, cz, ax, az, bx, bz, id % 10, id / 10);
            }
        }
        // la muestra y el mazo, en la esquina a la derecha de quien repartió
        if (v.repartidor >= 0 && v.repartidor < v.jugadores) {
            int silla = Math.floorMod(v.silla[v.repartidor], 4);
            float lx = LADO[silla][0], lz = LADO[silla][1];
            float fx = -lx, fz = -lz;
            float rx = -fz, rz = fx;
            float cx = 0.5F + lx * 0.30F + rx * 0.385F, cz = 0.5F + lz * 0.30F + rz * 0.385F;
            if (v.muestra >= 0) {
                // atravesada debajo del mazo, asomando
                carta(b, m, n, light, cx - rx * 0.05F, 1.0065F, cz - rz * 0.05F, fx, fz, -rx, -rz,
                        v.muestra % 10, v.muestra / 10);
            }
            int quedan = 40 - 3 * v.jugadores - 1;
            int capas = Math.max(2, quedan / 4);
            for (int k = 0; k < capas; k++) {
                carta(b, m, n, light, cx - fx * 0.03F, 1.008F + k * 0.0011F, cz - fz * 0.03F, rx, rz, fx, fz, 0, 4);
            }
        }
        ms.popPose();
    }

    /** Una carta acostada: centro (x, y, z), eje del ancho (ax, az), eje del alto (bx, bz); celda del atlas. */
    private static void carta(IVertexBuilder b, Matrix4f m, Matrix3f n, int light, float x, float y, float z,
                              float ax, float az, float bx, float bz, int col, int fila) {
        float u0 = col / 10.0F, u1 = (col + 1) / 10.0F;
        float v0 = fila / 5.0F, v1 = (fila + 1) / 5.0F;
        // arriba-izquierda, abajo-izquierda, abajo-derecha, arriba-derecha (vistos desde arriba)
        vertice(b, m, n, light, x - ax * HW + bx * HH, y, z - az * HW + bz * HH, u0, v0);
        vertice(b, m, n, light, x - ax * HW - bx * HH, y, z - az * HW - bz * HH, u0, v1);
        vertice(b, m, n, light, x + ax * HW - bx * HH, y, z + az * HW - bz * HH, u1, v1);
        vertice(b, m, n, light, x + ax * HW + bx * HH, y, z + az * HW + bz * HH, u1, v0);
    }

    private static void vertice(IVertexBuilder b, Matrix4f m, Matrix3f n, int light, float x, float y, float z,
                                float u, float v) {
        b.vertex(m, x, y, z).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(n, 0.0F, 1.0F, 0.0F).endVertex();
    }
}
