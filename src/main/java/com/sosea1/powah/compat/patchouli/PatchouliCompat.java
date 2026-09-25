package com.sosea1.powah.compat.patchouli;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import com.sosea1.powah.Powah;

public final class PatchouliCompat {
    public static final ResourceLocation BOOK_ID = new ResourceLocation(Powah.MOD_ID, "manual");

    private PatchouliCompat() {}

    public static boolean isLoaded() {
        return Loader.isModLoaded("patchouli");
    }

    private static java.lang.reflect.Method clientOpenMethod;
    private static java.lang.reflect.Method serverOpenMethod;
    private static Object apiInstance;
    private static boolean initialized;

    private static boolean init() {
        if (!initialized) {
            initialized = true;
            try {
                Class<?> apiClass = Class.forName("vazkii.patchouli.api.PatchouliAPI");
                apiInstance = apiClass.getField("instance").get(null);
                clientOpenMethod = apiClass.getMethod("openBookGUI", ResourceLocation.class);
                serverOpenMethod = apiClass.getMethod("openBookGUI", EntityPlayerMP.class, ResourceLocation.class);
            } catch (Throwable t) {
                Powah.logger().warn("Failed to initialize Patchouli API reflection integration", t);
            }
        }
        return apiInstance != null;
    }

    public static void openBookClient() {
        if (!isLoaded() || !init() || clientOpenMethod == null) return;
        try {
            clientOpenMethod.invoke(apiInstance, BOOK_ID);
        } catch (Throwable t) {
            Powah.logger().warn("Failed to open Patchouli book on client", t);
        }
    }

    public static void openBookServer(EntityPlayerMP player) {
        if (!isLoaded() || !init() || serverOpenMethod == null || player == null) return;
        try {
            serverOpenMethod.invoke(apiInstance, player, BOOK_ID);
        } catch (Throwable t) {
            Powah.logger().warn("Failed to open Patchouli book on server for player {}", player.getName(), t);
        }
    }
}
