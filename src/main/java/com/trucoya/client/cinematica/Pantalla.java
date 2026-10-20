package com.trucoya.client.cinematica;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;

import java.util.List;

/** Todo lo que se dibuja encima de la imagen: franjas de cine, viñeta, fundidos, carteles y subtítulos. */
@OnlyIn(Dist.CLIENT)
final class Pantalla {

    private static final ResourceLocation VINETA = new ResourceLocation("textures/misc/vignette.png");
    /** Relación de aspecto del cine (2.39 : 1). */
    private static final double ASPECTO = 2.39D;

    private Pantalla() {
    }

    /** Sube de 0 a 1 entre a y b, se queda, y baja entre c y d. */
    static double tramo(double t, double a, double b, double c, double d) {
        if (t <= a || t >= d) {
            return 0.0D;
        }
        if (t < b) {
            return Toma.suave((t - a) / (b - a));
        }
        if (t <= c) {
            return 1.0D;
        }
        return 1.0D - Toma.suave((t - c) / (d - c));
    }

    static double blanco(double t) {
        return Math.max(tramo(t, 12.0D, 13.3D, 13.3D, 14.4D),
                Math.max(tramo(t, 57.82D, 57.95D, 58.05D, 59.2D), tramo(t, 67.0D, 68.3D, 68.3D, 69.8D)));
    }

    static double negro(double t) {
        double inicio = t < 0.9D ? 1.0D - Toma.suave(t / 0.9D) : 0.0D;
        return Math.max(inicio, Math.max(tramo(t, 31.6D, 33.0D, 33.0D, 33.9D), tramo(t, 88.6D, 90.0D, 90.0D, 99.0D)));
    }

    /** Dibuja la capa de cine de la cinemática en curso. */
    static void dibujar(Cinematica c) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            return;   // con el menú de pausa abierto no se tapa nada
        }
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        double t = c.t;
        MatrixStack ms = new MatrixStack();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        vineta(mc, w, h, c.peliculaVieja() ? 1.0F : 0.55F);
        if (c.peliculaVieja()) {      // parpadeo de película vieja
            double p = 0.035D + 0.03D * Math.sin(t * 37.0D) * Math.sin(t * 11.3D + 1.0D);
            AbstractGui.fill(ms, 0, 0, w, h, argb(p, 0x000000));
        }
        double barra = barra(w, h) * Math.min(1.0D, Toma.suave(t / 1.2D));
        franjas(ms, w, h, barra);

        // carteles
        FontRenderer font = mc.font;
        for (Guion.Cartel k : c.guion.carteles) {
            double a = tramo(t, k.t, k.t + 1.1D, k.fin - 1.1D, k.fin);
            if (a > 0.01D) {
                boolean grande = k.grande;
                float escala = grande ? Math.max(2.5F, h / 75.0F) : Math.max(2.0F, h / 100.0F);
                double y = grande ? h * 0.40D : h * 0.30D;
                espaciado(ms, font, k.titulo, w / 2.0D, y, escala, escala * 1.4F, argb(a, 0xEBD7A4));
                float sub = Math.max(1.0F, escala * 0.42F);
                double ySub = y + 9 * escala + 6;
                int ancho = (int) (font.width(k.sub) * sub);
                int linea = argb(a * 0.8D, 0xBFA978);
                AbstractGui.fill(ms, (int) (w / 2.0D - ancho / 2.0D - 34), (int) (ySub + 4 * sub), (int) (w / 2.0D - ancho / 2.0D - 8), (int) (ySub + 4 * sub) + 1, linea);
                AbstractGui.fill(ms, (int) (w / 2.0D + ancho / 2.0D + 8), (int) (ySub + 4 * sub), (int) (w / 2.0D + ancho / 2.0D + 34), (int) (ySub + 4 * sub) + 1, linea);
                centrado(ms, font, k.sub, w / 2.0D, ySub, sub, argb(a * 0.9D, 0xD8C69C));
            }
        }

        // subtítulos (en la franja de abajo)
        for (Guion.Linea l : c.guion.lineas) {
            double a = tramo(t, l.t - 0.15D, l.t + 0.15D, l.fin - 0.25D, l.fin);
            if (a > 0.01D) {
                List<IReorderingProcessor> renglones = font.split(new StringTextComponent(l.texto), (int) (w * 0.72D));
                double alto = renglones.size() * 11.0D;
                double y0 = barra >= alto + 6 ? h - barra / 2.0D - alto / 2.0D + 1 : h - alto - 14;
                for (int i = 0; i < renglones.size(); i++) {
                    IReorderingProcessor r = renglones.get(i);
                    font.drawShadow(ms, r, (float) (w / 2.0D - font.width(r) / 2.0D), (float) (y0 + i * 11), argb(a, 0xF3E7C9));
                }
            }
        }

        // fundidos
        double bl = c.blanco(t);
        if (bl > 0.003D) {
            AbstractGui.fill(ms, 0, 0, w, h, argb(bl, 0xFFF8EA));
        }
        if (c.destello > 0.003D) {
            AbstractGui.fill(ms, 0, 0, w, h, argb(Math.min(0.75D, c.destello), 0xF4F1FF));
        }
        double ng = c.negro(t);
        if (ng > 0.003D) {
            AbstractGui.fill(ms, 0, 0, w, h, argb(ng, 0x000000));
        }
        // después de los créditos, el subtítulo va sobre el negro
        if (c.esFinal && t > Cinematica.TITULOS) {
            for (Guion.Linea l : c.guion.lineas) {
                double a = tramo(t, l.t - 0.15D, l.t + 0.15D, l.fin - 0.25D, l.fin);
                if (l.t > Cinematica.TITULOS && a > 0.01D) {
                    List<IReorderingProcessor> renglones = font.split(new StringTextComponent(l.texto), (int) (w * 0.72D));
                    double y0 = h * 0.5D - renglones.size() * 5.5D;
                    for (int i = 0; i < renglones.size(); i++) {
                        IReorderingProcessor r = renglones.get(i);
                        font.drawShadow(ms, r, (float) (w / 2.0D - font.width(r) / 2.0D), (float) (y0 + i * 11), argb(a, 0xC8B89A));
                    }
                }
            }
        }

        // cómo saltarla
        if (t < 7.0D || c.salto > 0.0D) {
            double a = c.salto > 0.0D ? 1.0D : tramo(t, 1.0D, 2.0D, 5.5D, 7.0D);
            if (a > 0.01D) {
                String tecla = mc.options.keyJump.getTranslatedKeyMessage().getString();
                String txt = "Mantené " + tecla + " para saltar";
                float s = 0.75F;
                double x = w - font.width(txt) * s - 10;
                double y = Math.max(6, barra / 2.0D - 4);
                ms.pushPose();
                ms.translate(x, y, 0);
                ms.scale(s, s, 1.0F);
                font.drawShadow(ms, txt, 0, 0, argb(a * 0.75D, 0xCFC6B0));
                ms.popPose();
                if (c.salto > 0.0D) {
                    int largo = (int) (font.width(txt) * s);
                    AbstractGui.fill(ms, (int) x, (int) y + 9, (int) x + largo, (int) y + 10, argb(0.35D, 0xFFFFFF));
                    AbstractGui.fill(ms, (int) x, (int) y + 9, (int) x + (int) (largo * Math.min(1.0D, c.salto / Cinematica.SALTO)), (int) y + 10, argb(0.9D, 0xEBD7A4));
                }
            }
        }
        RenderSystem.enableDepthTest();
    }

    /** La capa de cine de una escena de la Salamanca (franjas, carteles, subtítulos, fogonazos y fundidos). */
    static void dibujarEscena(EscenaViva c) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            return;
        }
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        double t = c.t;
        MatrixStack ms = new MatrixStack();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        vineta(mc, w, h, 0.6F);
        double barra = barra(w, h) * Math.min(1.0D, Toma.suave(t / 1.0D));
        franjas(ms, w, h, barra);
        if (c.destello > 0.003D) {
            AbstractGui.fill(ms, 0, 0, w, h, argb(Math.min(0.85D, c.destello), 0xF4F1FF));
        }
        double ng = c.negro(t);
        if (ng > 0.003D) {
            AbstractGui.fill(ms, 0, 0, w, h, argb(ng, 0x000000));
        }
        FontRenderer font = mc.font;
        for (Guion.Cartel k : c.guion.carteles) {
            double a = tramo(t, k.t, k.t + 1.1D, k.fin - 1.1D, k.fin);
            if (a > 0.01D) {
                float escala = Math.max(2.0F, h / 100.0F);
                double y = h * 0.36D;
                espaciado(ms, font, k.titulo, w / 2.0D, y, escala, escala * 1.4F, argb(a, 0xEBD7A4));
                float sub = Math.max(1.0F, escala * 0.42F);
                centrado(ms, font, k.sub, w / 2.0D, y + 9 * escala + 6, sub, argb(a * 0.9D, 0xD8C69C));
            }
        }
        for (Guion.Linea l : c.guion.lineas) {
            double a = tramo(t, l.t - 0.15D, l.t + 0.15D, l.fin - 0.25D, l.fin);
            if (a > 0.01D) {
                List<IReorderingProcessor> renglones = font.split(new StringTextComponent(l.texto), (int) (w * 0.72D));
                double alto = renglones.size() * 11.0D;
                double y0 = barra >= alto + 6 ? h - barra / 2.0D - alto / 2.0D + 1 : h - alto - 14;
                for (int i = 0; i < renglones.size(); i++) {
                    IReorderingProcessor r = renglones.get(i);
                    font.drawShadow(ms, r, (float) (w / 2.0D - font.width(r) / 2.0D), (float) (y0 + i * 11), argb(a, 0xF3E7C9));
                }
            }
        }
        RenderSystem.enableDepthTest();
    }

    /** Al terminar: la imagen vuelve de negro y las franjas se retiran. */
    static void dibujarSalida(double s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            return;
        }
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        MatrixStack ms = new MatrixStack();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        franjas(ms, w, h, barra(w, h) * (1.0D - Toma.suave(s / 1.3D)));
        double ng = 1.0D - Toma.suave(s / 1.1D);
        if (ng > 0.003D) {
            AbstractGui.fill(ms, 0, 0, w, h, argb(ng, 0x000000));
        }
        RenderSystem.enableDepthTest();
    }

    static double barra(int w, int h) {
        return Math.max(h * 0.08D, (h - w / ASPECTO) / 2.0D);
    }

    static void franjas(MatrixStack ms, int w, int h, double barra) {
        if (barra < 0.5D) {
            return;
        }
        int b = (int) Math.ceil(barra);
        AbstractGui.fill(ms, 0, 0, w, b, 0xFF000000);
        AbstractGui.fill(ms, 0, h - b, w, h, 0xFF000000);
    }

    static void vineta(Minecraft mc, int w, int h, float fuerza) {
        mc.getTextureManager().bind(VINETA);
        RenderSystem.enableTexture();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.color4f(fuerza, fuerza, fuerza, 1.0F);
        Tessellator tes = Tessellator.getInstance();
        BufferBuilder b = tes.getBuilder();
        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        b.vertex(0.0D, h, -90.0D).uv(0.0F, 1.0F).endVertex();
        b.vertex(w, h, -90.0D).uv(1.0F, 1.0F).endVertex();
        b.vertex(w, 0.0D, -90.0D).uv(1.0F, 0.0F).endVertex();
        b.vertex(0.0D, 0.0D, -90.0D).uv(0.0F, 0.0F).endVertex();
        tes.end();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
    }

    static void centrado(MatrixStack ms, FontRenderer font, String s, double cx, double y, float escala, int color) {
        ms.pushPose();
        ms.translate(cx - font.width(s) * escala / 2.0D, y, 0);
        ms.scale(escala, escala, 1.0F);
        font.drawShadow(ms, s, 0, 0, color);
        ms.popPose();
    }

    /** Título con letras separadas (como en los afiches de cine). */
    static void espaciado(MatrixStack ms, FontRenderer font, String s, double cx, double y, float escala, float extra, int color) {
        double ancho = 0;
        for (int i = 0; i < s.length(); i++) {
            ancho += font.width(String.valueOf(s.charAt(i))) * escala + (i < s.length() - 1 ? extra : 0);
        }
        double x = cx - ancho / 2.0D;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            ms.pushPose();
            ms.translate(x, y, 0);
            ms.scale(escala, escala, 1.0F);
            font.drawShadow(ms, ch, 0, 0, color);
            ms.popPose();
            x += font.width(ch) * escala + extra;
        }
    }

    static int argb(double a, int rgb) {
        int al = MathHelper.clamp((int) Math.round(a * 255.0D), 5, 255);
        return (al << 24) | (rgb & 0xFFFFFF);
    }
}
