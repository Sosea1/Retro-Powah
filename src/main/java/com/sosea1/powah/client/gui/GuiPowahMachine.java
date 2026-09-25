package com.sosea1.powah.client.gui;

import java.io.IOException;
import java.util.Collections;
import java.util.Locale;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import com.sosea1.powah.Powah;
import com.sosea1.powah.common.container.ContainerPowahMachine;
import com.sosea1.powah.common.energy.EnergyPortMode;
import com.sosea1.powah.common.energy.RedstoneControlMode;
import com.sosea1.powah.content.discharger.TileEnergyDischarger;
import com.sosea1.powah.content.ender.AbstractEnderTile;
import com.sosea1.powah.content.ender.TileEnderCell;
import com.sosea1.powah.content.ender.TileEnderGate;
import com.sosea1.powah.content.energycell.TileEnergyCell;
import com.sosea1.powah.content.furnator.TileFurnator;
import com.sosea1.powah.content.magmator.TileMagmator;
import com.sosea1.powah.content.reactor.TileReactor;
import com.sosea1.powah.content.transmitter.TilePlayerTransmitter;
import com.sosea1.powah.content.thermo.TileThermoGenerator;

@SideOnly(Side.CLIENT)
public final class GuiPowahMachine extends GuiContainer {
    private static final ResourceLocation CONTROL_TEXTURE =
            new ResourceLocation(Powah.MOD_ID, "textures/gui/container/button_ov.png");

    private final ContainerPowahMachine machine;
    private final InventoryPlayer playerInventory;
    private final Profile profile;

    public GuiPowahMachine(ContainerPowahMachine machine, InventoryPlayer playerInventory) {
        super(machine);
        this.machine = machine;
        this.playerInventory = playerInventory;
        this.profile = Profile.forTile(machine.getTile());
        this.xSize = 176;
        this.ySize = profile.height;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
        drawControlTooltip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        int left = (width - xSize) / 2;
        int top = (height - ySize) / 2;

        mc.getTextureManager().bindTexture(profile.texture);
        drawTexturedModalRect(left, top, 0, 0, 176, profile.height);
        drawControlButtons(left, top);

        if (machine.getTile() instanceof TileEnderCell) {
            // The original atlas has no frame for the explicit 1.12 extender slot.
            drawSlotFrame(left - 25, top + 49);
        }

        double energyRatio = ratio(machine.getSyncedEnergy(), machine.getSyncedCapacity());
        switch (profile.kind) {
            case ENERGY_CELL:
                drawHorizontalGauge(left + 31, top + 6, profile.texture, 0, 141, 139, 37, energyRatio);
                break;
            case ENDER_CELL:
                drawHorizontalGauge(left + 31, top + 6, profile.texture, 0, 164, 139, 37, energyRatio);
                break;
            case PLAYER_TRANSMITTER:
                drawHorizontalGauge(left + 31, top + 6, profile.texture, 0, 141, 139, 37, energyRatio);
                if ((machine.getSyncedFlags() & 1) != 0) {
                    blit(profile.texture, left + 9, top + 5, 176, 0, 6, 6);
                }
                break;
            case DISCHARGER:
                drawHorizontalGauge(left + 6, top + 6, profile.texture, 0, 166, 164, 37, energyRatio);
                break;
            case FURNATOR:
                drawVerticalGauge(left + 5, top + 5, profile.texture, 176, 0, 14, 39, energyRatio);
                drawVerticalGauge(left + 110, top + 18, profile.texture, 190, 0, 4, 16,
                        Math.min(1.0D, machine.getSyncedAux() / 1600.0D));
                if ((machine.getSyncedFlags() & 1) != 0) {
                    blit(profile.texture, left + 94, top + 43, 176, 39, 11, 18);
                }
                break;
            case MAGMATOR:
                drawVerticalGauge(left + 5, top + 5, Profile.FURNATOR.texture, 176, 0, 14, 39, energyRatio);
                if ((machine.getSyncedFlags() & 1) != 0) {
                    blit(profile.texture, left + 83, top + 29, 176, 39, 11, 18);
                }
                drawFluidTank(left + 157, top + 5, 14, 65);
                break;
            case THERMO:
                drawVerticalGauge(left + 5, top + 5, profile.texture, 176, 0, 14, 39, energyRatio);
                drawFluidTank(left + 157, top + 5, 14, 65);
                break;
            case REACTOR:
                drawVerticalGauge(left + 5, top + 5, profile.texture, 176, 0, 14, 39, energyRatio);
                drawVerticalGauge(left + 103, top + 13, profile.texture, 190, 16, 5, 48,
                        Math.min(1.0D, machine.getSyncedReactorFuel() / 1000.0D));
                drawVerticalGauge(left + 51, top + 6, profile.texture, 190, 0, 5, 16,
                        Math.min(1.0D, machine.getSyncedReactorCarbon() / 1600.0D));
                drawVerticalGauge(left + 51, top + 52, profile.texture, 195, 0, 5, 16,
                        Math.min(1.0D, machine.getSyncedReactorRedstone() / 162.0D));
                drawVerticalGauge(left + 140, top + 52, profile.texture, 200, 0, 5, 16,
                        Math.min(1.0D, machine.getSyncedReactorSolidCoolant() / 712.0D));
                drawVerticalGauge(left + 114, top + 28, profile.texture, 205, 0, 4, 18,
                        Math.min(1.0D, machine.getSyncedReactorTemperature() / 1000.0D));
                break;
            case WIDE:
            default:
                drawHorizontalGauge(left + 6, top + 6, profile.texture, 0, 141, 164, 37, energyRatio);
                break;
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = machine.getTile().getBlockType() == null ? "Powah Machine"
                : I18n.format(machine.getTile().getBlockType().getTranslationKey() + ".name");
        if (title == null || title.isEmpty()) title = "Powah Machine";

        int text = 0xFF474747;
        if (profile.kind == Kind.ENERGY_CELL || profile.kind == Kind.ENDER_CELL
                || profile.kind == Kind.PLAYER_TRANSMITTER) {
            fontRenderer.drawString(compactEnergy(), 38, 13, text);
            fontRenderer.drawString(format(machine.getTile().getEnergyBuffer().maxExtract()) + " FE/t", 38, 27, text);
        } else if (profile.kind == Kind.WIDE || profile.kind == Kind.DISCHARGER) {
            fontRenderer.drawString(compactEnergy(), 12, 13, text);
            fontRenderer.drawString(format(machine.getTile().getEnergyBuffer().maxExtract()) + " FE/t", 12, 27, text);
        } else if (profile.kind == Kind.THERMO) {
            fontRenderer.drawString(format(machine.getSyncedAux()) + " FE/t", 34, 10, text);
        }

        if (machine.getTile() instanceof AbstractEnderTile) {
            AbstractEnderTile ender = (AbstractEnderTile) machine.getTile();
            String owner = ender.getOwnerName();
            if (owner != null && !owner.isEmpty()) {
                String ownedTitle = title + " · " + owner;
                fontRenderer.drawString(fontRenderer.trimStringToWidth(ownedTitle, xSize - 4), 2, -10, 0xFFE0E0E0);
            }
            int maxChannels = Math.max(1, machine.getSyncedFlags());
            for (int channel = 0; channel < maxChannels; channel++) {
                drawChannelButton(5 + channel * 14, 55,
                        channel + 1, channel == Math.max(0, (int) machine.getSyncedAux() - 1));
            }
        }
        if (machine.getTile() instanceof TileReactor) {
            fontRenderer.drawString(format(machine.getSyncedAux()) + " FE/t", 62, 8, text);
        }

        // Title outside the art avoids covering modern gauge labels/slots.
        if (!(machine.getTile() instanceof AbstractEnderTile)
                || ((AbstractEnderTile) machine.getTile()).getOwnerName().isEmpty()) {
            fontRenderer.drawString(title, 2, -10, 0xFFE0E0E0);
        }
    }

    private void drawControlButtons(int left, int top) {
        mc.getTextureManager().bindTexture(CONTROL_TEXTURE);

        if (machine.hasConfigurableSides()) {
            drawTexturedModalRect(left + xSize, top + 4, 0, 0, 23, 25);
            for (EnumFacing side : EnumFacing.values()) {
                int[] pos = sideButtonPos(left, top, side);
                drawTexturedModalRect(pos[0], pos[1], modeIconU(machine.getSyncedSideMode(side)), 16, 5, 5);
            }
            drawTexturedModalRect(left + xSize + 14, top + 8, 23, 16, 5, 5);
        }

        drawTexturedModalRect(left + xSize, top + 30, 23, 0, 15, 16);
        int[] uv = redstoneUv(machine.getSyncedRedstoneMode());
        drawTexturedModalRect(left + xSize + 2, top + 34, uv[0], uv[1], 9, 8);

        if (profile.kind == Kind.REACTOR) {
            blit(profile.texture, left - 15, top + 6, 209, 0, 15, 16);
            blit(profile.texture, left - 11, top + 10, 224, reactorAutoModeOn() ? 8 : 0, 8, 8);
        }
    }

    private void drawControlTooltip(int mouseX, int mouseY) {
        int left = (width - xSize) / 2;
        int top = (height - ySize) / 2;

        // Energy gauge tooltips.
        if (profile.kind == Kind.FURNATOR || profile.kind == Kind.MAGMATOR
                || profile.kind == Kind.THERMO || profile.kind == Kind.REACTOR) {
            if (inside(mouseX, mouseY, left + 5, top + 5, 14, 39)) {
                long energy = machine.getSyncedEnergy();
                long capacity = machine.getSyncedCapacity();
                double pct = capacity > 0 ? (energy * 100.0 / capacity) : 0.0;
                drawHoveringText(java.util.Arrays.asList(
                        I18n.format("gui.powah.energy"),
                        format(energy) + " / " + format(capacity) + " FE " + String.format(Locale.ROOT, "(%.1f%%)", pct)),
                        mouseX, mouseY);
                return;
            }
        } else if (profile.kind == Kind.ENERGY_CELL || profile.kind == Kind.ENDER_CELL
                || profile.kind == Kind.PLAYER_TRANSMITTER) {
            if (inside(mouseX, mouseY, left + 31, top + 6, 139, 37)) {
                long energy = machine.getSyncedEnergy();
                long capacity = machine.getSyncedCapacity();
                double pct = capacity > 0 ? (energy * 100.0 / capacity) : 0.0;
                drawHoveringText(java.util.Arrays.asList(
                        I18n.format("gui.powah.energy"),
                        format(energy) + " / " + format(capacity) + " FE " + String.format(Locale.ROOT, "(%.1f%%)", pct)),
                        mouseX, mouseY);
                return;
            }
        } else if (profile.kind == Kind.WIDE || profile.kind == Kind.DISCHARGER) {
            if (inside(mouseX, mouseY, left + 6, top + 6, 164, 37)) {
                long energy = machine.getSyncedEnergy();
                long capacity = machine.getSyncedCapacity();
                double pct = capacity > 0 ? (energy * 100.0 / capacity) : 0.0;
                drawHoveringText(java.util.Arrays.asList(
                        I18n.format("gui.powah.energy"),
                        format(energy) + " / " + format(capacity) + " FE " + String.format(Locale.ROOT, "(%.1f%%)", pct)),
                        mouseX, mouseY);
                return;
            }
        }

        // Fluid tank tooltip.
        if (profile.kind == Kind.MAGMATOR || profile.kind == Kind.THERMO) {
            if (inside(mouseX, mouseY, left + 157, top + 5, 14, 65)) {
                int amount = machine.getSyncedFluidAmount();
                int capacity = machine.getSyncedFluidCapacity();
                String fluidName = profile.kind == Kind.MAGMATOR ? I18n.format("gui.powah.fluid.lava") : I18n.format("gui.powah.fluid.coolant");
                drawHoveringText(java.util.Arrays.asList(
                        fluidName,
                        format(amount) + " / " + format(capacity) + " mB"),
                        mouseX, mouseY);
                return;
            }
        }

        // Generator status and burn progress.
        if (profile.kind == Kind.FURNATOR) {
            if (inside(mouseX, mouseY, left + 94, top + 43, 11, 18) || inside(mouseX, mouseY, left + 110, top + 18, 4, 16)) {
                long aux = machine.getSyncedAux();
                drawHoveringText(Collections.singletonList(
                        aux > 0 ? (I18n.format("gui.powah.fuel.remaining") + ": " + aux + " ticks") : I18n.format("gui.powah.status.idle")),
                        mouseX, mouseY);
                return;
            }
        } else if (profile.kind == Kind.MAGMATOR) {
            if (inside(mouseX, mouseY, left + 83, top + 29, 11, 18)) {
                boolean active = (machine.getSyncedFlags() & 1) != 0;
                drawHoveringText(Collections.singletonList(
                        active ? (I18n.format("gui.powah.status.generating") + ": " + format(machine.getTile().getEnergyBuffer().maxExtract()) + " FE/t")
                               : I18n.format("gui.powah.status.idle")),
                        mouseX, mouseY);
                return;
            }
        }

        // Reactor gauges.
        if (profile.kind == Kind.REACTOR) {
            if (inside(mouseX, mouseY, left - 15, top + 6, 15, 16)) {
                drawHoveringText(java.util.Arrays.asList(
                        I18n.format("info.powah.gen.mode") + ": " + I18n.format(reactorAutoModeOn() ? "info.lollipop.on" : "info.lollipop.off"),
                        I18n.format("info.powah.gen.mode.desc")), mouseX, mouseY);
                return;
            }
            if (inside(mouseX, mouseY, left + 103, top + 13, 5, 48)) {
                drawHoveringText(Collections.singletonList(I18n.format("gui.powah.reactor.fuel") + ": " + machine.getSyncedReactorFuel() + " / 1000"), mouseX, mouseY);
                return;
            }
            if (inside(mouseX, mouseY, left + 51, top + 6, 5, 16)) {
                drawHoveringText(Collections.singletonList(I18n.format("gui.powah.reactor.carbon") + ": " + machine.getSyncedReactorCarbon() + " / 1600"), mouseX, mouseY);
                return;
            }
            if (inside(mouseX, mouseY, left + 51, top + 52, 5, 16)) {
                drawHoveringText(Collections.singletonList(I18n.format("gui.powah.reactor.redstone") + ": " + machine.getSyncedReactorRedstone() + " / 162"), mouseX, mouseY);
                return;
            }
            if (inside(mouseX, mouseY, left + 140, top + 52, 5, 16)) {
                drawHoveringText(Collections.singletonList(I18n.format("gui.powah.reactor.solid_coolant") + ": " + machine.getSyncedReactorSolidCoolant() + " / 712"), mouseX, mouseY);
                return;
            }
            if (inside(mouseX, mouseY, left + 114, top + 28, 4, 18)) {
                drawHoveringText(Collections.singletonList(I18n.format("gui.powah.reactor.temp") + ": " + machine.getSyncedReactorTemperature() + "°C / 1000°C"), mouseX, mouseY);
                return;
            }
        }

        // Redstone mode.
        if (inside(mouseX, mouseY, left + xSize, top + 30, 15, 16)) {
            drawHoveringText(Collections.singletonList(
                    "Redstone: " + machine.getSyncedRedstoneMode().name()), mouseX, mouseY);
            return;
        }

        // Ender channel controls and capacity extender.
        if (machine.getTile() instanceof AbstractEnderTile) {
            int maxChannels = Math.max(1, machine.getSyncedFlags());
            for (int channel = 0; channel < maxChannels; channel++) {
                if (inside(mouseX, mouseY, left + 5 + channel * 14, top + 55, 12, 12)) {
                    drawHoveringText(Collections.singletonList(
                            I18n.format("gui.powah.ender.channel.button", channel + 1)), mouseX, mouseY);
                    return;
                }
            }
            if (machine.getTile() instanceof TileEnderCell
                    && inside(mouseX, mouseY, left - 25, top + 49, 22, 22)) {
                drawHoveringText(java.util.Arrays.asList(
                        I18n.format("gui.powah.ender.extender"),
                        I18n.format("gui.powah.ender.extender.desc")), mouseX, mouseY);
                return;
            }
        }

        // Side configuration.
        if (!machine.hasConfigurableSides()) return;
        if (inside(mouseX, mouseY, left + xSize + 14, top + 8, 5, 5)) {
            drawHoveringText(Collections.singletonList("Configure all sides"), mouseX, mouseY);
            return;
        }
        for (EnumFacing side : EnumFacing.values()) {
            int[] pos = sideButtonPos(left, top, side);
            if (inside(mouseX, mouseY, pos[0], pos[1], 5, 5)) {
                drawHoveringText(Collections.singletonList(
                        side.getName() + ": " + machine.getSyncedSideMode(side).name()), mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            int left = (width - xSize) / 2;
            int top = (height - ySize) / 2;
            if (profile.kind == Kind.REACTOR && inside(mouseX, mouseY, left - 15, top + 6, 15, 16)) {
                mc.playerController.sendEnchantPacket(machine.windowId, ContainerPowahMachine.ACTION_REACTOR_GEN_MODE);
                return;
            }
            if (machine.getTile() instanceof AbstractEnderTile) {
                int maxChannels = Math.max(1, machine.getSyncedFlags());
                for (int channel = 0; channel < maxChannels; channel++) {
                    if (inside(mouseX, mouseY, left + 5 + channel * 14, top + 55, 12, 12)) {
                        mc.playerController.sendEnchantPacket(machine.windowId,
                                ContainerPowahMachine.ACTION_ENDER_CHANNEL_BASE + channel);
                        return;
                    }
                }
            }
            if (inside(mouseX, mouseY, left + xSize, top + 30, 15, 16)) {
                mc.playerController.sendEnchantPacket(machine.windowId, ContainerPowahMachine.ACTION_REDSTONE);
                return;
            }
            if (machine.hasConfigurableSides()) {
                if (inside(mouseX, mouseY, left + xSize + 14, top + 8, 5, 5)) {
                    mc.playerController.sendEnchantPacket(machine.windowId, ContainerPowahMachine.ACTION_SIDE_ALL);
                    return;
                }
                for (EnumFacing side : EnumFacing.values()) {
                    int[] pos = sideButtonPos(left, top, side);
                    if (inside(mouseX, mouseY, pos[0], pos[1], 5, 5)) {
                        mc.playerController.sendEnchantPacket(machine.windowId,
                                ContainerPowahMachine.ACTION_SIDE_BASE + side.ordinal());
                        return;
                    }
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }


    private void drawChannelButton(int x, int y, int channel, boolean selected) {
        blit(profile.texture, x, y, selected ? 176 : 188, 0, 12, 12);
        String label = Integer.toString(channel);
        int labelX = x + (12 - fontRenderer.getStringWidth(label)) / 2;
        fontRenderer.drawString(label, labelX, y + 2, selected ? 0xFFFFFF : 0xA7C9C9, false);
    }

    private void drawSlotFrame(int x, int y) {
        drawRect(x, y, x + 22, y + 22, 0xFF07171A);
        drawRect(x + 1, y + 1, x + 21, y + 21, 0xFF18363A);
        drawRect(x + 3, y + 3, x + 19, y + 19, 0xFF081113);
    }

    private boolean reactorAutoModeOn() {
        return (machine.getSyncedFlags() & 2) != 0;
    }

    private int[] sideButtonPos(int left, int top, EnumFacing side) {
        int x = left + xSize + 8;
        int y = top + 14;
        switch (side) {
            case DOWN: y += 6; break;
            case UP: y -= 6; break;
            case SOUTH: x += 6; y += 6; break;
            case WEST: x -= 6; break;
            case EAST: x += 6; break;
            case NORTH: break;
            default: break;
        }
        return new int[]{x, y};
    }

    private static int modeIconU(EnergyPortMode mode) {
        switch (mode) {
            case BOTH: return 28;
            case OUTPUT: return 33;
            case INPUT: return 38;
            case NONE: default: return 43;
        }
    }

    private static int[] redstoneUv(RedstoneControlMode mode) {
        switch (mode) {
            case HIGH: return new int[]{38, 8};
            case LOW: return new int[]{47, 0};
            case IGNORED: default: return new int[]{38, 0};
        }
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseY >= y && mouseX < x + w && mouseY < y + h;
    }

    private String compactEnergy() {
        return format(machine.getSyncedEnergy()) + "/" + format(machine.getSyncedCapacity()) + " FE";
    }

    private void drawFluidTank(int x, int y, int w, int h) {
        int cap = machine.getSyncedFluidCapacity();
        if (cap <= 0) return;
        int amount = Math.max(0, Math.min(cap, machine.getSyncedFluidAmount()));
        int filled = (int) Math.round((amount / (double) cap) * h);
        if (filled > 0) {
            int color = (profile.kind == Kind.MAGMATOR) ? 0xFFD44800 : 0xCC3F76E4;
            drawRect(x + 1, y + h - filled, x + w - 1, y + h, color);
        }
    }


    private void drawHorizontalGauge(int x, int y, ResourceLocation texture,
                                     int u, int v, int w, int h, double ratio) {
        int width = Math.max(0, Math.min(w, (int) Math.round(w * ratio)));
        if (width > 0) blit(texture, x, y, u, v, width, h);
    }

    private void drawVerticalGauge(int x, int y, ResourceLocation texture,
                                   int u, int v, int w, int h, double ratio) {
        int height = Math.max(0, Math.min(h, (int) Math.round(h * ratio)));
        if (height <= 0) return;
        int dy = h - height;
        blit(texture, x, y + dy, u, v + dy, w, height);
    }

    private void blit(ResourceLocation texture, int x, int y, int u, int v, int w, int h) {
        mc.getTextureManager().bindTexture(texture);
        drawTexturedModalRect(x, y, u, v, w, h);
    }

    private static double ratio(long value, long capacity) {
        if (capacity <= 0L || value <= 0L) return 0.0D;
        return Math.max(0.0D, Math.min(1.0D, value / (double) capacity));
    }

    private static String format(long value) {
        if (value >= 1_000_000_000L) return String.format(Locale.ROOT, "%.2fG", value / 1_000_000_000.0D);
        if (value >= 1_000_000L) return String.format(Locale.ROOT, "%.2fM", value / 1_000_000.0D);
        if (value >= 1_000L) return String.format(Locale.ROOT, "%.2fk", value / 1_000.0D);
        return Long.toString(value);
    }

    private enum Kind { WIDE, ENERGY_CELL, ENDER_CELL, PLAYER_TRANSMITTER, DISCHARGER, FURNATOR, MAGMATOR, THERMO, REACTOR }

    private static final class Profile {
        private static final Profile WIDE = new Profile(Kind.WIDE, "wide_energy", 141);
        private static final Profile ENERGY_CELL = new Profile(Kind.ENERGY_CELL, "energy_cell", 141);
        private static final Profile ENDER_CELL = new Profile(Kind.ENDER_CELL, "ender_cell", 164);
        private static final Profile PLAYER_TRANSMITTER = new Profile(Kind.PLAYER_TRANSMITTER, "player_transmitter", 141);
        private static final Profile DISCHARGER = new Profile(Kind.DISCHARGER, "discharger", 166);
        private static final Profile FURNATOR = new Profile(Kind.FURNATOR, "furnator", 166);
        private static final Profile MAGMATOR = new Profile(Kind.MAGMATOR, "magmator", 166);
        private static final Profile THERMO = new Profile(Kind.THERMO, "thermo", 166);
        private static final Profile REACTOR = new Profile(Kind.REACTOR, "reactor", 166);

        private final Kind kind;
        private final ResourceLocation texture;
        private final int height;

        private Profile(Kind kind, String texture, int height) {
            this.kind = kind;
            this.texture = new ResourceLocation(Powah.MOD_ID, "textures/gui/container/" + texture + ".png");
            this.height = height;
        }

        private static Profile forTile(Object tile) {
            if (tile instanceof TileEnergyCell) return ENERGY_CELL;
            if (tile instanceof TileEnderCell || tile instanceof TileEnderGate) return ENDER_CELL;
            if (tile instanceof TilePlayerTransmitter) return PLAYER_TRANSMITTER;
            if (tile instanceof TileEnergyDischarger) return DISCHARGER;
            if (tile instanceof TileFurnator) return FURNATOR;
            if (tile instanceof TileMagmator) return MAGMATOR;
            if (tile instanceof TileThermoGenerator) return THERMO;
            if (tile instanceof TileReactor) return REACTOR;
            return WIDE;
        }
    }
}
