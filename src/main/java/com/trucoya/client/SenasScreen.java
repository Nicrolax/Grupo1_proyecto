package com.trucoya.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.trucoya.core.Sena;
import com.trucoya.network.TableActionPacket;
import com.trucoya.network.TrucoNetwork;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;

/**
 * Menú rápido de señas (tecla G): doce gestos en dos columnas. Al elegir uno, el compañero
 * recibe el aviso; los rivales tienen una chance de descubrirlo.
 */
@OnlyIn(Dist.CLIENT)
public class SenasScreen extends Screen {

    private static final int BOX_W = 150;
    private static final int BOX_H = 18;

    @Nullable
    private final Screen parent;

    public SenasScreen(@Nullable Screen parent) {
        super(new StringTextComponent("Señas"));
        this.parent = parent;
    }

    private int boxX(int i) {
        return this.width / 2 + (i % 2 == 0 ? -BOX_W - 4 : 4);
    }

    private int boxY(int i) {
        int rows = (Sena.values().length + 1) / 2;
        return this.height / 2 - rows * (BOX_H + 3) / 2 + (i / 2) * (BOX_H + 3) + 8;
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(ms);
        Sena[] all = Sena.values();
        drawCenteredString(ms, this.font, "Señas para tu compañero", this.width / 2, boxY(0) - 24, 0xE8B82A);
        drawCenteredString(ms, this.font, "Cuidado: los rivales pueden descubrirlas", this.width / 2, boxY(0) - 13, 0xA0A0A0);
        for (int i = 0; i < all.length; i++) {
            int x = boxX(i);
            int y = boxY(i);
            boolean hover = mouseX >= x && mouseX < x + BOX_W && mouseY >= y && mouseY < y + BOX_H;
            fill(ms, x, y, x + BOX_W, y + BOX_H, hover ? 0xE0806020 : 0xC0201810);
            String meaning = all[i].getMeaning();
            String title = Character.toUpperCase(meaning.charAt(0)) + meaning.substring(1);
            this.font.draw(ms, this.font.plainSubstrByWidth(title, BOX_W - 6), x + 3, y + 1, 0xFFFFFF);
            ms.pushPose();
            ms.translate(x + 3, y + 10, 0);
            ms.scale(0.7F, 0.7F, 1.0F);
            this.font.draw(ms, all[i].getGesture(), 0, 0, 0xC8B89A);
            ms.popPose();
        }
        super.render(ms, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Sena[] all = Sena.values();
            for (int i = 0; i < all.length; i++) {
                int x = boxX(i);
                int y = boxY(i);
                if (mouseX >= x && mouseX < x + BOX_W && mouseY >= y && mouseY < y + BOX_H) {
                    if (ClientTrucoState.tablePos != null) {
                        TrucoNetwork.CHANNEL.sendToServer(TableActionPacket.sena(ClientTrucoState.tablePos, all[i]));
                    }
                    onClose();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || ModKeys.SENAS.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
