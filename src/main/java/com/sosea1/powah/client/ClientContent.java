package com.sosea1.powah.client;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderSnowball;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import com.sosea1.powah.content.special.EntityChargedSnowball;
import com.sosea1.powah.client.render.ReactorItemRenderer;
import com.sosea1.powah.content.reactor.ItemReactorBlock;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import com.sosea1.powah.Powah;
import com.sosea1.powah.registry.ModContent;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = Powah.MOD_ID, value = Side.CLIENT)
public final class ClientContent {
    private static final ReactorItemRenderer REACTOR_ITEM_RENDERER = new ReactorItemRenderer();

    private ClientContent() {
    }


    @SubscribeEvent
    public static void stitchTextures(TextureStitchEvent.Pre event) {
        // FurnatorRenderer samples the lit face directly from the block atlas.
        // It is intentionally not referenced by the static block model, so Forge would not
        // discover/stitch it automatically. Register it explicitly before atlas baking.
        event.getMap().registerSprite(new net.minecraft.util.ResourceLocation(Powah.MOD_ID,
                "block/furnator_lit"));
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(EntityChargedSnowball.class,
                manager -> new RenderSnowball<EntityChargedSnowball>(manager, ModContent.chargedSnowball(),
                        Minecraft.getMinecraft().getRenderItem()));
        for (Block block : ModContent.blocks()) {
            Item item = Item.getItemFromBlock(block);
            ModelLoader.setCustomModelResourceLocation(item, 0,
                    new ModelResourceLocation(block.getRegistryName(), "inventory"));
            if (item instanceof ItemReactorBlock) {
                item.setTileEntityItemStackRenderer(REACTOR_ITEM_RENDERER);
            }
        }
        for (Item item : ModContent.items()) {
            ModelLoader.setCustomModelResourceLocation(item, 0,
                    new ModelResourceLocation(item.getRegistryName(), "inventory"));
        }
    }
}
