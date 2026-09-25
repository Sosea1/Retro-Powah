package com.sosea1.powah.client.gui;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import com.sosea1.powah.Powah;
import com.sosea1.powah.common.container.ContainerCableConfig;
import com.sosea1.powah.common.energy.EnergyPortMode;
import com.sosea1.powah.common.energy.RedstoneControlMode;

@SideOnly(Side.CLIENT)
public final class GuiCableConfig extends GuiContainer {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Powah.MOD_ID, "textures/gui/container/cable.png");
    private static final ResourceLocation CONTROL_TEXTURE =
            new ResourceLocation(Powah.MOD_ID, "textures/gui/container/button_ov.png");

    private static final int PANEL_W = 160;
    private static final int PANEL_H = 50;
    private static final int CONFIG_X = 8;
    private static final int CONFIG_Y = 20;
    private static final int CONFIG_W = 118;
    private static final int CONFIG_H = 22;
    private static final int REDSTONE_X = 130;
    private static final int REDSTONE_Y = 20;
    private static final int REDSTONE_W = 22;
    private static final int REDSTONE_H = 22;

    private final ContainerCableConfig cable;

    public GuiCableConfig(ContainerCableConfig cable) {
        super(cable);
        this.cable = cable;
        this.xSize = PANEL_W;
        this.ySize = PANEL_H;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawTooltips(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        int left = (width - xSize) / 2;
        int top = (height - ySize) / 2;

        drawRect(left, top, left + PANEL_W, top + PANEL_H, 0xFFC6C6C6);
        drawRect(left, top, left + PANEL_W, top + 1, 0xFFFFFFFF);
        drawRect(left, top, left + 1, top + PANEL_H, 0xFFFFFFFF);
        drawRect(left + PANEL_W - 1, top, left + PANEL_W, top + PANEL_H, 0xFF555555);
        drawRect(left, top + PANEL_H - 1, left + PANEL_W, top + PANEL_H, 0xFF555555);

        drawRect(left + 2, top + 2, left + PANEL_W - 2, top + 16, 0xFF373737);

        boolean modeHover = inside(mouseX, mouseY, left + CONFIG_X, top + CONFIG_Y, CONFIG_W, CONFIG_H);
        drawRect(left + CONFIG_X, top + CONFIG_Y, left + CONFIG_X + CONFIG_W, top + CONFIG_Y + CONFIG_H, modeHover ? 0xFF8B8B8B : 0xFF555555);
        drawRect(left + CONFIG_X + 1, top + CONFIG_Y + 1, left + CONFIG_X + CONFIG_W - 1, top + CONFIG_Y + CONFIG_H - 1, modeHover ? 0xFF737373 : 0xFF373737);

        EnergyPortMode mode = cable.getSyncedMode();
        mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(left + CONFIG_X + 2, top + CONFIG_Y + 1, iconU(mode), 0, 18, 19);

        boolean redstoneHover = inside(mouseX, mouseY, left + REDSTONE_X, top + REDSTONE_Y, REDSTONE_W, REDSTONE_H);
        drawRect(left + REDSTONE_X, top + REDSTONE_Y, left + REDSTONE_X + REDSTONE_W, top + REDSTONE_Y + REDSTONE_H, redstoneHover ? 0xFF8B8B8B : 0xFF555555);
        drawRect(left + REDSTONE_X + 1, top + REDSTONE_Y + 1, left + REDSTONE_X + REDSTONE_W - 1, top + REDSTONE_Y + REDSTONE_H - 1, redstoneHover ? 0xFF737373 : 0xFF373737);

        mc.getTextureManager().bindTexture(CONTROL_TEXTURE);
        int[] uv = redstoneUv(cable.getSyncedRedstoneMode());
        drawTexturedModalRect(left + REDSTONE_X + 3, top + REDSTONE_Y + 3, uv[0], uv[1], 15, 16);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String side = I18n.format("gui.powah.cable.side", cable.getSide().getName());
        int titleWidth = fontRenderer.getStringWidth(side);
        fontRenderer.drawString(side, (xSize - titleWidth) / 2, 5, 0xFFFFFFFF);

        String modeText = I18n.format("gui.powah.cable.mode." + cable.getSyncedMode().name().toLowerCase(Locale.ROOT));
        fontRenderer.drawString(modeText, CONFIG_X + 24, CONFIG_Y + 7, 0xFFE0E0E0);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            int left = (width - xSize) / 2;
            int top = (height - ySize) / 2;
            if (inside(mouseX, mouseY, left + CONFIG_X, top + CONFIG_Y, CONFIG_W, CONFIG_H)) {
                mc.playerController.sendEnchantPacket(cable.windowId, ContainerCableConfig.ACTION_NEXT_MODE);
                return;
            }
            if (inside(mouseX, mouseY, left + REDSTONE_X, top + REDSTONE_Y, REDSTONE_W, REDSTONE_H)) {
                mc.playerController.sendEnchantPacket(cable.windowId, ContainerCableConfig.ACTION_REDSTONE);
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void drawTooltips(int mouseX, int mouseY) {
        int left = (width - xSize) / 2;
        int top = (height - ySize) / 2;
        if (inside(mouseX, mouseY, left + CONFIG_X, top + CONFIG_Y, CONFIG_W, CONFIG_H)) {
            drawHoveringText(Arrays.asList(
                    I18n.format("gui.powah.cable.side", cable.getSide().getName()),
                    I18n.format("gui.powah.cable.mode." + cable.getSyncedMode().name().toLowerCase(Locale.ROOT))),
                    mouseX, mouseY);
            return;
        }
        if (inside(mouseX, mouseY, left + REDSTONE_X, top + REDSTONE_Y, REDSTONE_W, REDSTONE_H)) {
            drawHoveringText(Collections.singletonList(
                    "Redstone: " + cable.getSyncedRedstoneMode().name()), mouseX, mouseY);
        }
    }

    private static int iconU(EnergyPortMode mode) {
        switch (mode) {
            case BOTH: return 153;
            case OUTPUT: return 171;
            case INPUT: return 189;
            case NONE: default: return 207;
        }
    }

    private static int[] redstoneUv(RedstoneControlMode mode) {
        switch (mode) {
            case HIGH: return new int[]{38, 8};
            case LOW: return new int[]{47, 0};
            case IGNORED: default: return new int[]{38, 0};
        }
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseY >= y && mouseX < x + w && mouseY < y + h;
    }
}
