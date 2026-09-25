package com.sosea1.powah.client.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import com.sosea1.powah.Powah;
import com.sosea1.powah.common.energy.EnergyPortMode;
import com.sosea1.powah.common.tier.PowahTier;
import com.sosea1.powah.content.cable.TileCable;

/** Renders modern Powah I/O plates only on cable faces connected to external FE handlers. */
public final class CableRenderer extends TileEntitySpecialRenderer<TileCable> {
    private static final EnumFacing[] FACINGS = EnumFacing.values();
    private static final ResourceLocation[][] TEXTURES = createTextures();
    private final CableIndicatorModel model = new CableIndicatorModel();

    @Override
    public void render(TileCable tile, double x, double y, double z, float partialTicks,
                       int destroyStage, float alpha) {
        if (tile == null || tile.getWorld() == null) {
            return;
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5D, y + 1.5D, z + 0.5D);
        GlStateManager.translate(0.0D, -0.125D, 0.0D);
        GlStateManager.scale(1.0F, -1.0F, -1.0F);
        GlStateManager.enableRescaleNormal();

        for (EnumFacing side : FACINGS) {
            EnergyPortMode mode = tile.getSideMode(side);
            if (mode == EnergyPortMode.NONE || !tile.hasExternalEnergySide(side)) {
                continue;
            }
            bindTexture(texture(tile, mode));
            model.renderIndicator(side);
        }

        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
    }

    private static ResourceLocation texture(TileCable tile, EnergyPortMode mode) {
        ResourceLocation texture = TEXTURES[tile.getTier().ordinal()][mode.ordinal()];
        if (texture == null) {
            throw new IllegalArgumentException("No cable texture for " + mode);
        }
        return texture;
    }

    private static ResourceLocation[][] createTextures() {
        ResourceLocation[][] textures = new ResourceLocation[PowahTier.values().length][EnergyPortMode.values().length];
        for (PowahTier tier : PowahTier.normalValues()) {
            String name = tier.name().toLowerCase(java.util.Locale.ENGLISH);
            textures[tier.ordinal()][EnergyPortMode.BOTH.ordinal()] = location(name, "all");
            // Upstream Powah names RECEIVE from the cable perspective as *_out.
            textures[tier.ordinal()][EnergyPortMode.INPUT.ordinal()] = location(name, "out");
            textures[tier.ordinal()][EnergyPortMode.OUTPUT.ordinal()] = location(name, "in");
        }
        return textures;
    }

    private static ResourceLocation location(String tier, String suffix) {
        return new ResourceLocation(Powah.MOD_ID,
                "textures/model/tile/energy_cable_" + tier + "_" + suffix + ".png");
    }
}
