package com.sosea1.powah.client.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;
import com.sosea1.powah.Powah;
import com.sosea1.powah.common.tier.PowahTier;
import com.sosea1.powah.content.reactor.TileReactorPart;

/** Renders individual shell cubes only while a reactor is still being assembled. */
public final class ReactorPartRenderer extends TileEntitySpecialRenderer<TileReactorPart> {
    private static final ResourceLocation[] TEXTURES = createTextures();
    private final ReactorRenderModel model = new ReactorRenderModel();

    @Override
    public void render(TileReactorPart tile, double x, double y, double z, float partialTicks,
                       int destroyStage, float alpha) {
        if (tile == null || tile.isReactorBuilt()) return;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5D, y + 0.5D, z + 0.5D);
        GlStateManager.scale(1.0F, -1.0F, -1.0F);
        bindTexture(TEXTURES[tile.getTier().ordinal()]);
        model.renderPart();
        GlStateManager.popMatrix();
    }
    private static ResourceLocation[] createTextures() {
        ResourceLocation[] textures = new ResourceLocation[PowahTier.values().length];
        for (PowahTier tier : PowahTier.normalValues()) {
            textures[tier.ordinal()] = new ResourceLocation(Powah.MOD_ID, "textures/model/tile/reactor_block_"
                    + tier.name().toLowerCase(java.util.Locale.ROOT) + ".png");
        }
        return textures;
    }
}
