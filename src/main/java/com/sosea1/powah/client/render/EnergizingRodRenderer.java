package com.sosea1.powah.client.render;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import com.sosea1.powah.Powah;
import com.sosea1.powah.content.energizing.TileEnergizingRod;

public final class EnergizingRodRenderer extends TileEntitySpecialRenderer<TileEnergizingRod> {
    private static final ResourceLocation BEAM = new ResourceLocation(Powah.MOD_ID, "textures/model/tile/beam.png");

    @Override
    public void render(TileEnergizingRod tile, double x, double y, double z, float partialTicks,
                       int destroyStage, float alpha) {
        if (tile == null || tile.getWorld() == null) return;
        BlockPos orb = tile.getOrbPos();
        if (orb == null || !tile.getWorld().isBlockLoaded(orb)) return;

        double dx = orb.getX() - tile.getPos().getX();
        double dy = orb.getY() - tile.getPos().getY();
        double dz = orb.getZ() - tile.getPos().getZ();
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 0.001D) return;

        double nx = dx / length;
        double ny = dy / length;
        double nz = dz / length;
        float yaw = (float) Math.toDegrees(Math.atan2(nz, nx));
        float pitch = (float) Math.toDegrees(Math.acos(Math.max(-1.0D, Math.min(1.0D, ny))));
        int color = tile.getTier().color();
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;
        double width = 0.12D;
        double scroll = ((tile.getWorld().getTotalWorldTime() + partialTicks) * 0.05D) % 1.0D;

        float oldX = OpenGlHelper.lastBrightnessX;
        float oldY = OpenGlHelper.lastBrightnessY;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5D, y + 0.5D, z + 0.5D);
        GlStateManager.rotate(90.0F - yaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        bindTexture(BEAM);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        double v0 = scroll;
        double v1 = scroll + length * 5.05D;
        quad(buf, -width, 0, 0, -width, length, 0, width, length, 0, width, 0, 0, r, g, b, v0, v1);
        quad(buf, 0, 0, -width, 0, length, -width, 0, length, width, 0, 0, width, r, g, b, v0, v1);
        tess.draw();

        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, oldX, oldY);
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static void quad(BufferBuilder bld,
                             double x1, double y1, double z1, double x2, double y2, double z2,
                             double x3, double y3, double z3, double x4, double y4, double z4,
                             int r, int g, int b, double v0, double v1) {
        bld.pos(x1, y1, z1).tex(0.0D, v0).color(r, g, b, 220).endVertex();
        bld.pos(x2, y2, z2).tex(0.0D, v1).color(r, g, b, 220).endVertex();
        bld.pos(x3, y3, z3).tex(1.0D, v1).color(r, g, b, 220).endVertex();
        bld.pos(x4, y4, z4).tex(1.0D, v0).color(r, g, b, 220).endVertex();
    }

}
