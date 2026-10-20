package com.trucoya.client;

import com.trucoya.TrucoYa;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MipmapGenerator;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.Texture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Textura de una baraja (atlas de 1280 x 960, cartas de 128 x 192) con mipmaps y filtro suave.
 * Así las cartas se leen claras en cualquier tamaño: achicadas en el tapete, o sobre la mesa vistas de lejos
 * (sin el "serrucho" ni el titileo de una textura común sin mipmaps).
 */
@OnlyIn(Dist.CLIENT)
public final class CartasTextura extends Texture {

    /** Niveles de mipmap: 1280x960 → 640x480 → 320x240 → 160x120. */
    private static final int NIVELES = 3;
    private static final Set<ResourceLocation> REGISTRADAS = new HashSet<>();

    private final ResourceLocation ubicacion;

    private CartasTextura(ResourceLocation ubicacion) {
        this.ubicacion = ubicacion;
    }

    /** Registra (una sola vez) la textura con mipmaps; llamar antes de usarla para dibujar. */
    public static ResourceLocation preparar(ResourceLocation rl) {
        if (REGISTRADAS.add(rl)) {
            Minecraft.getInstance().getTextureManager().register(rl, new CartasTextura(rl));
        }
        return rl;
    }

    /** Prepara y deja la textura activa (para dibujar en pantallas). */
    public static void usar(ResourceLocation rl) {
        Minecraft.getInstance().getTextureManager().bind(preparar(rl));
    }

    @Override
    public void load(IResourceManager recursos) throws IOException {
        try (IResource r = recursos.getResource(ubicacion); NativeImage imagen = NativeImage.read(r.getInputStream())) {
            NativeImage[] niveles = MipmapGenerator.generateMipLevels(imagen, NIVELES);
            TextureUtil.prepareImage(this.getId(), NIVELES, imagen.getWidth(), imagen.getHeight());
            for (int i = 0; i < niveles.length; i++) {
                niveles[i].upload(i, 0, 0, 0, 0, niveles[i].getWidth(), niveles[i].getHeight(), true, false, true, false);
            }
            for (int i = 1; i < niveles.length; i++) {
                niveles[i].close();
            }
        } catch (IOException e) {
            TrucoYa.LOGGER.warn("Truco Ya: no se pudo cargar la baraja {}", ubicacion, e);
            throw e;
        }
    }
}
