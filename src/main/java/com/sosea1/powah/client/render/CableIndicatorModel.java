package com.sosea1.powah.client.render;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.util.EnumFacing;

public final class CableIndicatorModel extends ModelBase {
    private static final float SCALE = 1.0F / 16.0F;

    private final ModelRenderer north;
    private final ModelRenderer northPlate;
    private final ModelRenderer south;
    private final ModelRenderer southPlate;
    private final ModelRenderer west;
    private final ModelRenderer westPlate;
    private final ModelRenderer east;
    private final ModelRenderer eastPlate;
    private final ModelRenderer down;
    private final ModelRenderer downPlate;
    private final ModelRenderer up;
    private final ModelRenderer upPlate;

    public CableIndicatorModel() {
        textureWidth = 64;
        textureHeight = 32;

        north = part(0, 10, -1.5F, -1.5F, -7.75F, 3, 3, 6);
        northPlate = part(0, 20, -2.5F, -2.5F, -8.2F, 5, 5, 1);
        south = part(0, 0, -1.5F, -1.5F, 1.75F, 3, 3, 6);
        southPlate = part(0, 20, -2.5F, -2.5F, 7.2F, 5, 5, 1);

        west = part(19, 0, -7.75F, -1.5F, -1.5F, 6, 3, 3);
        westPlate = part(13, 20, -8.2F, -2.5F, -2.5F, 1, 5, 5);
        east = part(19, 7, 1.75F, -1.5F, -1.5F, 6, 3, 3);
        eastPlate = part(13, 20, 7.2F, -2.5F, -2.5F, 1, 5, 5);

        down = part(38, 10, -1.5F, -7.75F, -1.5F, 3, 6, 3);
        downPlate = part(26, 20, -2.5F, -8.2F, -2.5F, 5, 1, 5);
        up = part(38, 0, -1.5F, 1.75F, -1.5F, 3, 6, 3);
        upPlate = part(26, 20, -2.5F, 7.2F, -2.5F, 5, 1, 5);
    }

    private ModelRenderer part(int u, int v, float x, float y, float z, int width, int height, int depth) {
        ModelRenderer part = new ModelRenderer(this, u, v);
        part.setRotationPoint(0.0F, 14.0F, 0.0F);
        part.addBox(x, y, z, width, height, depth);
        return part;
    }

    public void renderIndicator(EnumFacing side) {
        getIndicator(side).render(SCALE);
        getPlate(side).render(SCALE);
    }

    private ModelRenderer getIndicator(EnumFacing side) {
        // Same flips as modern Powah: entity-style rendering is mirrored on Y/Z.
        switch (side) {
            case DOWN: return up;
            case UP: return down;
            case NORTH: return south;
            case SOUTH: return north;
            case WEST: return west;
            case EAST: return east;
            default: throw new IllegalArgumentException("Unknown cable side " + side);
        }
    }

    private ModelRenderer getPlate(EnumFacing side) {
        switch (side) {
            case DOWN: return upPlate;
            case UP: return downPlate;
            case NORTH: return southPlate;
            case SOUTH: return northPlate;
            case WEST: return westPlate;
            case EAST: return eastPlate;
            default: throw new IllegalArgumentException("Unknown cable side " + side);
        }
    }
}
