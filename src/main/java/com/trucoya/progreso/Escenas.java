package com.trucoya.progreso;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.trucoya.TrucoYa;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Las escenas de la Salamanca (visita, llegada, amanecer, derrota, viaje): duración y "marcas" de tiempo.
 * Las escribe tools/generar_salamanca_voces.py junto con las voces, y las leen igual el servidor (que mueve a
 * los personajes y cambia el mundo) y el cliente (cámara y subtítulos): así nunca se desfasan.
 */
public final class Escenas {

    public static final int VISITA = 0, VIAJE = 1, LLEGADA = 2, AMANECER = 3, DERROTA = 4;
    public static final String[] NOMBRES = {"visita", "viaje", "llegada", "amanecer", "derrota"};

    public static final class Info {
        public final double duracion;
        private final Map<String, Double> marcas;

        Info(double duracion, Map<String, Double> marcas) {
            this.duracion = duracion;
            this.marcas = marcas;
        }

        public double marca(String clave, double porDefecto) {
            Double v = marcas.get(clave);
            return v == null ? porDefecto : v;
        }

        /** En ticks del servidor. */
        public int tick(String clave, double porDefecto) {
            return (int) Math.round(marca(clave, porDefecto) * 20.0D);
        }

        public int ticks() {
            return (int) Math.round(duracion * 20.0D);
        }
    }

    private static final Map<Integer, Info> CACHE = new ConcurrentHashMap<>();

    private Escenas() {
    }

    public static Info de(int tipo) {
        return CACHE.computeIfAbsent(tipo, Escenas::leer);
    }

    private static Info leer(int tipo) {
        String ruta = "/assets/" + TrucoYa.MOD_ID + "/cinematica/escena_" + NOMBRES[tipo] + ".json";
        Map<String, Double> marcas = new HashMap<>();
        double duracion = new double[] {70, 7, 42, 46, 13}[tipo];
        try (InputStream in = Escenas.class.getResourceAsStream(ruta)) {
            if (in != null) {
                try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    JsonObject o = new JsonParser().parse(r).getAsJsonObject();
                    if (o.has("duracion")) duracion = o.get("duracion").getAsDouble();
                    if (o.has("marcas")) {
                        for (Map.Entry<String, com.google.gson.JsonElement> e : o.getAsJsonObject("marcas").entrySet()) {
                            marcas.put(e.getKey(), e.getValue().getAsDouble());
                        }
                    }
                }
            } else {
                TrucoYa.LOGGER.warn("Truco Ya: falta {}", ruta);
            }
        } catch (Exception e) {
            TrucoYa.LOGGER.warn("Truco Ya: no se pudo leer {}", ruta, e);
        }
        return new Info(duracion, marcas);
    }
}
