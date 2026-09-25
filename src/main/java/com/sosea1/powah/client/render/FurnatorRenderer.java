package com.sosea1.powah.client.render;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import com.sosea1.powah.content.furnator.BlockFurnator;
import com.sosea1.powah.content.furnator.TileFurnator;

public final class FurnatorRenderer extends TileEntitySpecialRenderer<TileFurnator> {
    @Override
    public void render(TileFurnator tile, double x, double y, double z, float partialTicks,
                       int destroyStage, float alpha) {
        if (tile == null || tile.getWorld() == null || !tile.isBurning()) return;
        IBlockState state = tile.getWorld().getBlockState(tile.getPos());
        if (!(state.getBlock() instanceof BlockFurnator)) return;
        EnumFacing facing = state.getValue(BlockFurnator.FACING);
        TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("powah:block/furnator_lit");
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        float oldX = OpenGlHelper.lastBrightnessX;
        float oldY = OpenGlHelper.lastBrightnessY;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        face(buf, facing, sprite);
        tess.draw();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, oldX, oldY);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static void face(BufferBuilder b, EnumFacing f, TextureAtlasSprite s) {
        double e = 0.001D;
        double u0=s.getMinU(), u1=s.getMaxU(), v0=s.getMinV(), v1=s.getMaxV();
        if (f == EnumFacing.NORTH) {
            vertex(b,e,e,-e,u0,v1); vertex(b,e,1-e,-e,u0,v0); vertex(b,1-e,1-e,-e,u1,v0); vertex(b,1-e,e,-e,u1,v1);
        } else if (f == EnumFacing.SOUTH) {
            vertex(b,1-e,e,1+e,u0,v1); vertex(b,1-e,1-e,1+e,u0,v0); vertex(b,e,1-e,1+e,u1,v0); vertex(b,e,e,1+e,u1,v1);
        } else if (f == EnumFacing.WEST) {
            vertex(b,-e,e,1-e,u0,v1); vertex(b,-e,1-e,1-e,u0,v0); vertex(b,-e,1-e,e,u1,v0); vertex(b,-e,e,e,u1,v1);
        } else {
            vertex(b,1+e,e,e,u0,v1); vertex(b,1+e,1-e,e,u0,v0); vertex(b,1+e,1-e,1-e,u1,v0); vertex(b,1+e,e,1-e,u1,v1);
        }
    }

    private static void vertex(BufferBuilder b, double x,double y,double z,double u,double v) {
        b.pos(x,y,z).tex(u,v).color(255,255,255,255).endVertex();
    }
}
