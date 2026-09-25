package com.sosea1.powah.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandler;
import com.sosea1.powah.Powah;
import com.sosea1.powah.content.energizing.TileEnergizingOrb;

public final class EnergizingOrbRenderer extends TileEntitySpecialRenderer<TileEnergizingOrb> {
    private static final ResourceLocation ENERGY_CHARGE =
            new ResourceLocation(Powah.MOD_ID, "textures/model/tile/energy_charge.png");
    private final EnergizingOrbRenderModel model = new EnergizingOrbRenderModel();

    @Override
    public void render(TileEnergizingOrb tile, double x, double y, double z, float partialTicks,
                       int destroyStage, float alpha) {
        if (tile == null) return;
        renderItems(tile, x, y, z, partialTicks);
        renderOrb(tile, x, y, z);
    }

    private void renderItems(TileEnergizingOrb tile, double x, double y, double z, float partialTicks) {
        IItemHandler inventory = tile.getInventory();
        ItemStack output = inventory.getStackInSlot(TileEnergizingOrb.OUTPUT_SLOT);
        int visibleCount = output.isEmpty() ? countInputs(inventory) : 1;
        if (visibleCount <= 0) return;

        float ticks = ((tile.getWorld() == null ? 0L : tile.getWorld().getTotalWorldTime()) + partialTicks) / 200.0F;
        float spin = -ticks * 360.0F;
        EnumFacing up = tile.getOrbUp();
        double cx = x + 0.5D + up.getXOffset() * 0.10D;
        double cy = y + 0.5D + up.getYOffset() * 0.10D;
        double cz = z + 0.5D + up.getZOffset() * 0.10D;

        if (!output.isEmpty()) {
            renderStack(output, 0, visibleCount, spin, cx, cy, cz);
            return;
        }

        int index = 0;
        for (int slot = TileEnergizingOrb.FIRST_INPUT_SLOT; slot <= TileEnergizingOrb.LAST_INPUT_SLOT; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                renderStack(stack, index++, visibleCount, spin, cx, cy, cz);
            }
        }
    }

    private static int countInputs(IItemHandler inventory) {
        int count = 0;
        for (int slot = TileEnergizingOrb.FIRST_INPUT_SLOT; slot <= TileEnergizingOrb.LAST_INPUT_SLOT; slot++) {
            if (!inventory.getStackInSlot(slot).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static void renderStack(ItemStack stack, int index, int count, float spin,
                                    double cx, double cy, double cz) {
        double ox = 0.0D;
        double oz = 0.0D;
        if (count > 1) {
            double angle = Math.PI * 2.0D * index / count;
            ox = Math.cos(angle) * 0.12D;
            oz = Math.sin(angle) * 0.12D;
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(cx + ox, cy, cz + oz);
        GlStateManager.rotate(spin, 0.0F, 1.0F, 0.0F);
        GlStateManager.scale(0.35F, 0.35F, 0.35F);
        Minecraft.getMinecraft().getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.FIXED);
        GlStateManager.popMatrix();
    }

    private void renderOrb(TileEnergizingOrb tile, double x, double y, double z) {
        GlStateManager.pushMatrix();
        EnumFacing up = tile.getOrbUp();
        GlStateManager.translate(x + 0.5D, y + 0.5D, z + 0.5D);
        orientLocalUp(up);
        GlStateManager.translate(0.0D, 0.10D, 0.0D);
        GlStateManager.scale(1.8F, 1.8F, 1.8F);

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableAlpha();
        GlStateManager.depthMask(false);
        GlStateManager.disableCull();

        bindTexture(ENERGY_CHARGE);
        model.render();

        GlStateManager.enableCull();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static void orientLocalUp(EnumFacing up) {
        switch (up) {
            case DOWN:
                GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
                break;
            case NORTH:
                GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
                break;
            case SOUTH:
                GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                break;
            case EAST:
                GlStateManager.rotate(-90.0F, 0.0F, 0.0F, 1.0F);
                break;
            case WEST:
                GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
                break;
            case UP:
            default:
                break;
        }
    }
}
