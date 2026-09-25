package com.sosea1.powah.client.render;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

/** Backport of Powah's 5x5x5 Energizing Orb cube and its 20x10 texture layout. */
public final class EnergizingOrbRenderModel extends ModelBase {
    private final ModelRenderer cube;

    public EnergizingOrbRenderModel() {
        textureWidth = 20;
        textureHeight = 10;
        cube = new ModelRenderer(this, 0, 0);
        cube.setTextureSize(20, 10);
        cube.mirror = true;
        cube.addBox(-2.5F, -2.5F, -2.5F, 5, 5, 5);
    }

    public void render() {
        cube.render(1.0F / 16.0F);
    }
}
