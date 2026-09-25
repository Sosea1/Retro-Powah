package com.sosea1.powah.client.render;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

/** The assembled reactor model measures 48x64x48 pixels (3x4x3 blocks). */
public final class ReactorRenderModel extends ModelBase {
    private static final float SCALE = 1.0F / 16.0F;

    private final ModelRenderer assembled;
    private final ModelRenderer part;

    public ReactorRenderModel() {
        textureWidth = 256;
        textureHeight = 128;
        assembled = new ModelRenderer(this, 0, 0);
        assembled.setTextureSize(256, 128);
        assembled.mirror = true;
        assembled.addBox(-24.0F, -32.0F, -24.0F, 48, 64, 48);
        assembled.setRotationPoint(0.0F, -8.0F, 0.0F);

        part = new ModelRenderer(this, 0, 0);
        part.setTextureSize(64, 32);
        part.mirror = true;
        part.addBox(-8.0F, -8.0F, -8.0F, 16, 16, 16);
    }

    public void renderAssembled() {
        assembled.render(SCALE);
    }

    public void renderPart() {
        part.render(SCALE);
    }
}
