package com.trucoya.client;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

/** Teclas configurables (Opciones → Controles → Truco Ya). */
@OnlyIn(Dist.CLIENT)
public final class ModKeys {

    public static final String CATEGORY = "key.categories.trucoya";
    public static final KeyBinding SENAS = new KeyBinding("key.trucoya.senas", GLFW.GLFW_KEY_G, CATEGORY);
    public static final KeyBinding TAPETE = new KeyBinding("key.trucoya.tapete", GLFW.GLFW_KEY_J, CATEGORY);

    private ModKeys() {
    }
}
