package com.sosea1.powah.client.gui;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import com.sosea1.powah.Powah;
import com.sosea1.powah.content.energizing.TileEnergizingOrb;
import com.sosea1.powah.content.energizing.TileEnergizingRod;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = Powah.MOD_ID, value = Side.CLIENT)
public final class PowahHudOverlay extends Gui {
    private static final PowahHudOverlay DRAW = new PowahHudOverlay();
    private static final ResourceLocation ENERGY_BAR =
            new ResourceLocation(Powah.MOD_ID, "textures/gui/ov_energy.png");
    private PowahHudOverlay() {
    }

    @SubscribeEvent
    public static void render(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null || mc.player == null || mc.world == null
                || mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != RayTraceResult.Type.BLOCK) return;

        TileEntity tile = mc.world.getTileEntity(mc.objectMouseOver.getBlockPos());
        if (tile instanceof TileEnergizingRod) {
            TileEnergizingRod rod = (TileEnergizingRod) tile;
            renderRod(event, mc, rod);
        } else if (tile instanceof TileEnergizingOrb) {
            TileEnergizingOrb orb = (TileEnergizingOrb) tile;
            if (!orb.containsRecipe() || orb.getRequiredEnergy() <= 0L) return;
            renderOrb(event, mc, orb);
        }
    }

    private static void renderRod(RenderGameOverlayEvent.Post event, Minecraft mc, TileEnergizingRod rod) {
        long stored = rod.getEnergyBuffer().energy();
        long capacity = rod.getEnergyBuffer().capacity();
        int width = event.getResolution().getScaledWidth();
        int height = event.getResolution().getScaledHeight();
        int x = width / 2 - 37;
        int y = height - 80;
        double ratio = capacity <= 0L ? 0.0D : Math.max(0.0D, Math.min(1.0D, stored / (double) capacity));
        mc.getTextureManager().bindTexture(ENERGY_BAR);
        DRAW.drawTexturedModalRect(x - 1, y, 0, 0, 74, 9);
        int fill = (int) Math.round(72.0D * ratio);
        if (fill > 0) DRAW.drawTexturedModalRect(x, y + 1, 0, 9, fill, 7);

        FontRenderer font = mc.fontRenderer;
        String text = I18n.format("gui.powah.hud.stored", format(stored), formatCompact(capacity));
        font.drawStringWithShadow(text, width / 2.0F - font.getStringWidth(text) / 2.0F, y + 13, 0xFFFFFF);
    }

    private static void renderOrb(RenderGameOverlayEvent.Post event, Minecraft mc, TileEnergizingOrb orb) {
        long stored = orb.getProgress();
        long capacity = orb.getRequiredEnergy();
        int width = event.getResolution().getScaledWidth();
        int height = event.getResolution().getScaledHeight();
        int percent = capacity <= 0L ? 0 : (int) Math.max(0L, Math.min(100L, stored * 100L / capacity));
        FontRenderer font = mc.fontRenderer;
        String percentText = percent + "%";
        String energyText = I18n.format("gui.powah.hud.energy", format(stored), formatCompact(capacity));
        font.drawStringWithShadow(percentText,
                width / 2.0F - font.getStringWidth(percentText) / 2.0F, height - 90, 0x55FF55);
        font.drawStringWithShadow(energyText,
                width / 2.0F - font.getStringWidth(energyText) / 2.0F, height - 75, 0xFFFFFF);
    }

    private static String format(long value) {
        return String.format(Locale.ROOT, "%,d", Math.max(0L, value));
    }

    private static String formatCompact(long value) {
        long safe = Math.max(0L, value);
        if (safe >= 1_000_000_000L) return compact(safe, 1_000_000_000L, "B");
        if (safe >= 1_000_000L) return compact(safe, 1_000_000L, "M");
        if (safe >= 1_000L) return compact(safe, 1_000L, "k");
        return Long.toString(safe);
    }

    private static String compact(long value, long divisor, String suffix) {
        double scaled = value / (double) divisor;
        return (scaled == Math.rint(scaled) ? Long.toString((long) scaled)
                : String.format(Locale.ROOT, "%.1f", scaled)) + suffix;
    }
}
