package com.sosea1.powah.client.render;

import java.util.Locale;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;
import com.sosea1.powah.Powah;
import com.sosea1.powah.common.tier.PowahTier;
import com.sosea1.powah.content.reactor.BlockReactor;
import com.sosea1.powah.content.reactor.TileReactor;

public final class ReactorRenderer extends TileEntitySpecialRenderer<TileReactor> {
    private static final ResourceLocation BASE = texture("reactor");
    private static final ResourceLocation RUNNING = texture("reactor_on");
    private static final ResourceLocation FILLED = texture("reactor_filled");
    private static final ResourceLocation[] PART_TEXTURES = tierTextures("reactor_block_");
    private static final ResourceLocation[] TIER_TEXTURES = tierTextures("reactor_");
    private final ReactorRenderModel model = new ReactorRenderModel();

    @Override
    public void render(TileReactor tile, double x, double y, double z, float partialTicks,
                       int destroyStage, float alpha) {
        if (tile == null) return;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5D, y + 0.5D, z + 0.5D);
        GlStateManager.scale(1.0F, -1.0F, -1.0F);
        PowahTier renderTier = renderTier(tile);

        if (!tile.isBuilt()) {
            bindTexture(partTexture(renderTier));
            model.renderPart();
            GlStateManager.popMatrix();
            return;
        }

        // The model origin is the bottom-center of the assembled structure.
        GlStateManager.translate(0.0D, -1.0D, 0.0D);
        enableLayerState();
        bindTexture(BASE);
        model.renderAssembled();

        if (tile.isRunning()) {
            bindTexture(RUNNING);
            model.renderAssembled();
        }
        if (tile.getFuel() > 0.0D) {
            bindTexture(FILLED);
            model.renderAssembled();
        }
        if (renderTier != PowahTier.STARTER) {
            bindTexture(TIER_TEXTURES[renderTier.ordinal()]);
            model.renderAssembled();
        }

        disableLayerState();
        GlStateManager.popMatrix();
    }

    private static void enableLayerState() {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
    }

    private static void disableLayerState() {
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static ResourceLocation partTexture(PowahTier tier) {
        return PART_TEXTURES[tier.ordinal()];
    }

    private static PowahTier renderTier(TileReactor tile) {
        if (tile.getWorld() != null) {
            net.minecraft.block.Block block = tile.getWorld().getBlockState(tile.getPos()).getBlock();
            if (block instanceof BlockReactor) {
                return ((BlockReactor) block).getTier();
            }
        }
        return tile.getTier();
    }

    private static ResourceLocation[] tierTextures(String prefix) {
        ResourceLocation[] textures = new ResourceLocation[PowahTier.values().length];
        for (PowahTier tier : PowahTier.normalValues()) {
            textures[tier.ordinal()] = texture(prefix + tierName(tier));
        }
        return textures;
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(Powah.MOD_ID, "textures/model/tile/" + name + ".png");
    }

    private static String tierName(PowahTier tier) {
        return tier.name().toLowerCase(Locale.ROOT);
    }

}
