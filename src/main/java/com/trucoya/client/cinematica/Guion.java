package com.trucoya.client.cinematica;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.trucoya.TrucoYa;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Subtítulos y carteles de la cinemática. Los tiempos los escribe {@code tools/generar_cinematica.py}
 * (que también genera la narración), así la voz y el texto nunca se desfasan.
 */
@OnlyIn(Dist.CLIENT)
final class Guion {

    static final class Linea {
        final double t, fin;
        final String texto;
        final String sonido;
        /** Quién habla (escenas de la Salamanca): vizcacha, mandinga, ledesma; vacío si no importa. */
        final String quien;

        Linea(double t, double fin, String texto, String sonido) {
            this(t, fin, texto, sonido, "");
        }

        Linea(double t, double fin, String texto, String sonido, String quien) {
            this.t = t;
            this.fin = fin;
            this.texto = texto;
            this.sonido = sonido;
            this.quien = quien;
        }
    }

    static final class Cartel {
        final double t, fin;
        final String titulo, sub;
        final boolean grande;

        Cartel(double t, double fin, String titulo, String sub) {
            this(t, fin, titulo, sub, fin > 80.0D);
        }

        Cartel(double t, double fin, String titulo, String sub, boolean grande) {
            this.t = t;
            this.fin = fin;
            this.titulo = titulo;
            this.sub = sub;
            this.grande = grande;
        }
    }

    final List<Linea> lineas = new ArrayList<>();
    final List<Cartel> carteles = new ArrayList<>();
    double grito = 41.6D;

    double duracion = 90.0D;

    static Guion cargar() {
        return cargar("vizcacha");
    }

    static Guion cargar(String nombre) {
        Guion g = new Guion();
        try {
            IResource r = Minecraft.getInstance().getResourceManager()
                    .getResource(new ResourceLocation(TrucoYa.MOD_ID, "cinematica/" + nombre + ".json"));
            try (Reader in = new InputStreamReader(r.getInputStream(), StandardCharsets.UTF_8)) {
                JsonObject o = new JsonParser().parse(in).getAsJsonObject();
                JsonArray subs = o.getAsJsonArray("subtitulos");
                for (JsonElement e : subs) {
                    JsonObject s = e.getAsJsonObject();
                    g.lineas.add(new Linea(s.get("t").getAsDouble(), s.get("fin").getAsDouble(),
                            s.get("texto").getAsString(), s.get("sonido").getAsString(),
                            s.has("quien") ? s.get("quien").getAsString() : ""));
                }
                for (JsonElement e : o.has("carteles") ? o.getAsJsonArray("carteles") : new JsonArray()) {
                    JsonObject c = e.getAsJsonObject();
                    double fin = c.get("fin").getAsDouble();
                    g.carteles.add(new Cartel(c.get("t").getAsDouble(), fin, c.get("titulo").getAsString(),
                            c.get("sub").getAsString(), c.has("grande") ? c.get("grande").getAsBoolean() : fin > 80.0D));
                }
                if (o.has("duracion")) {
                    g.duracion = o.get("duracion").getAsDouble();
                }
                if (o.has("grito")) {
                    g.grito = o.get("grito").getAsDouble();
                }
            }
        } catch (Exception e) {
            TrucoYa.LOGGER.warn("Truco Ya: no se pudo leer el guion de la cinemática; se usan los carteles básicos", e);
            g.lineas.clear();
            g.carteles.clear();
            g.carteles.add(new Cartel(14.2D, 19.5D, "PASO DE LAS ÁNIMAS", "Invierno del cuarenta"));
            g.carteles.add(new Cartel(83.6D, 88.6D, "EL ÚLTIMO TRUCO", "del Viejo Vizcacha"));
        }
        return g;
    }
}
