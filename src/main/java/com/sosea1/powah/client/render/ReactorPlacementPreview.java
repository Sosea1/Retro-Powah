package com.sosea1.powah.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import com.sosea1.powah.Powah;
import com.sosea1.powah.content.reactor.ItemReactorBlock;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = Powah.MOD_ID, value = Side.CLIENT)
public final class ReactorPlacementPreview {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Powah.MOD_ID, "textures/misc/reactor_ov.png");

    private ReactorPlacementPreview() {
    }

    @SubscribeEvent
    public static void render(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null || mc.world == null || mc.objectMouseOver == null
                || mc.objectMouseOver.typeOfHit != RayTraceResult.Type.BLOCK) return;

        ItemStack reactor = heldReactor(player);
        if (reactor.isEmpty()) return;

        BlockPos core = ItemReactorBlock.placementPosition(
                mc.world, mc.objectMouseOver.getBlockPos(), mc.objectMouseOver.sideHit);
        boolean enough = player.capabilities.isCreativeMode
                || ItemReactorBlock.countAvailable(player, reactor.getItem()) >= ItemReactorBlock.STRUCTURE_BLOCKS;
        boolean valid = enough && ItemReactorBlock.canFitStructure(mc.world, core)
                && !ItemReactorBlock.hasLivingEntityInStructure(mc.world, core);

        double cameraX = mc.getRenderManager().viewerPosX;
        double cameraY = mc.getRenderManager().viewerPosY;
        double cameraZ = mc.getRenderManager().viewerPosZ;
        double minX = core.getX() - 1.0D - cameraX;
        double maxX = core.getX() + 2.0D - cameraX;
        double y = core.getY() + 0.002D - cameraY;
        double minZ = core.getZ() - 1.0D - cameraZ;
        double maxZ = core.getZ() + 2.0D - cameraZ;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.color(valid ? 0.46F : 0.81F, valid ? 0.88F : 0.02F,
                valid ? 0.59F : 0.05F, 0.72F);
        mc.getTextureManager().bindTexture(TEXTURE);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(minX, y, maxZ).tex(0.0D, 1.0D).endVertex();
        buffer.pos(maxX, y, maxZ).tex(1.0D, 1.0D).endVertex();
        buffer.pos(maxX, y, minZ).tex(1.0D, 0.0D).endVertex();
        buffer.pos(minX, y, minZ).tex(0.0D, 0.0D).endVertex();
        tessellator.draw();

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static ItemStack heldReactor(EntityPlayer player) {
        for (EnumHand hand : EnumHand.values()) {
            ItemStack stack = player.getHeldItem(hand);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemReactorBlock) return stack;
        }
        return ItemStack.EMPTY;
    }
}
