package com.sosea1.powah.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import com.sosea1.powah.Powah;
import com.sosea1.powah.common.tier.PowahTier;
import com.sosea1.powah.content.reactor.BlockReactor;
import com.sosea1.powah.content.reactor.ItemReactorBlock;

/** 1.12 TEISR equivalent of modern Powah's special reactor item renderer. */
public final class ReactorItemRenderer extends TileEntityItemStackRenderer {
    private static final ResourceLocation[] TEXTURES = createTextures();
    private final ReactorRenderModel model = new ReactorRenderModel();

    @Override
    public void renderByItem(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemReactorBlock)) {
            return;
        }

        Block block = ((ItemReactorBlock) stack.getItem()).getBlock();
        if (!(block instanceof BlockReactor)) {
            return;
        }

        PowahTier tier = ((BlockReactor) block).getTier();
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.5D, 0.5D, 0.5D);
        GlStateManager.scale(1.0F, -1.0F, -1.0F);
        GlStateManager.enableRescaleNormal();
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURES[tier.ordinal()]);
        model.renderPart();
        GlStateManager.disableRescaleNormal();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
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
