package com.sosea1.powah.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import com.sosea1.powah.content.magmator.TileMagmator;

public final class MagmatorRenderer extends TileEntitySpecialRenderer<TileMagmator> {
    @Override
    public void render(TileMagmator tile, double x, double y, double z, float partialTicks,
                       int destroyStage, float alpha) {
        if (tile == null || tile.getTank() == null) return;
        FluidStack fluid = tile.getTank().getFluid();
        if (fluid == null || fluid.amount <= 0) return;
        ResourceLocation still = fluid.getFluid().getStill(fluid);
        if (still == null) return;
        TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(still.toString());
        int color = fluid.getFluid().getColor(fluid);
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;
        int a = color >>> 24 & 0xFF;
        if (a == 0) a = 255;
        float fill = Math.min(1.0F, fluid.amount / (float) tile.getTank().getCapacity()) * 0.45F;
        double surface = 0.51D + fill;

        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        double min=0.1875D, max=0.8125D;
        buf.pos(min,surface,min).tex(sprite.getMinU(),sprite.getMinV()).color(r,g,b,a).endVertex();
        buf.pos(min,surface,max).tex(sprite.getMinU(),sprite.getMaxV()).color(r,g,b,a).endVertex();
        buf.pos(max,surface,max).tex(sprite.getMaxU(),sprite.getMaxV()).color(r,g,b,a).endVertex();
        buf.pos(max,surface,min).tex(sprite.getMaxU(),sprite.getMinV()).color(r,g,b,a).endVertex();
        tess.draw();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}
