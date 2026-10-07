package com.sosea1.powah.common.config;

import java.io.File;
import java.util.Locale;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import com.sosea1.powah.common.ender.EnderNetworkData;
import com.sosea1.powah.common.tier.PowahTier;

/** Forge-backed configuration. Machines consume an immutable snapshot and a monotonic revision. */
public final class PowahConfig {
    private static volatile EnergyConfigSnapshot energy = DefaultEnergyConfig.create();
    private static volatile long revision;
    private static volatile Configuration configuration;
    private static volatile File file;
    private static volatile long lastModified;
    private static volatile long nextPollNanos;

    private static volatile int energizingRange = 4;
    private static volatile int poorAttempts = 8, poorVein = 5, poorMinY = 0, poorMaxY = 64;
    private static volatile int normalAttempts = 6, normalVein = 4, normalMinY = 0, normalMaxY = 20;
    private static volatile int denseAttempts = 3, denseVein = 3, denseMinY = 1, denseMaxY = 8;
    private static volatile int dryAttempts = 9, dryVein = 17, dryMinY = 1, dryMaxY = 64;
    private static volatile double dryTemperature = 0.15D;
    private static volatile int[] allowedDimensions = {0};
    private static volatile boolean playerAerialPearl = true;
    private static volatile boolean lensOfEnder = true;
    private static volatile boolean dimensionalBindingCard = true;

    private PowahConfig() {}

    public static synchronized void initialize(File suggestedFile) {
        file = suggestedFile;
        configuration = new Configuration(suggestedFile);
        reload();
    }

    public static synchronized void reload() {
        Configuration cfg = configuration;
        if (cfg == null) {
            energy = DefaultEnergyConfig.create();
            playerAerialPearl = true;
            lensOfEnder = true;
            dimensionalBindingCard = true;
            revision++;
            return;
        }
        cfg.load();
        double generation = positive(cfg.getFloat("generationMultiplier", "energy", 1.0F, 0.01F, 1000.0F, "Generator/reactor production multiplier"));
        double capacity = positive(cfg.getFloat("capacityMultiplier", "energy", 1.0F, 0.01F, 1000.0F, "Internal capacity multiplier"));
        double transfer = positive(cfg.getFloat("transferMultiplier", "energy", 1.0F, 0.01F, 1000.0F, "FE transfer multiplier"));
        double fuel = positive(cfg.getFloat("fuelEnergyMultiplier", "energy", 1.0F, 0.01F, 1000.0F, "Fuel energy multiplier"));
        double energizingCost = positive(cfg.getFloat("recipeEnergyMultiplier", "energizing", 1.0F, 0.01F, 1000.0F, "Energizing recipe FE cost multiplier"));
        energizingRange = cfg.getInt("rodRange", "energizing", 4, 1, 32, "Maximum Energizing Rod link range");

        poorAttempts = integer(cfg,"poorAttempts",8,0,128); poorVein=integer(cfg,"poorVeinSize",5,1,64); poorMinY=integer(cfg,"poorMinY",0,0,255); poorMaxY=integer(cfg,"poorMaxY",64,0,255);
        normalAttempts = integer(cfg,"normalAttempts",6,0,128); normalVein=integer(cfg,"normalVeinSize",4,1,64); normalMinY=integer(cfg,"normalMinY",0,0,255); normalMaxY=integer(cfg,"normalMaxY",20,0,255);
        denseAttempts = integer(cfg,"denseAttempts",3,0,128); denseVein=integer(cfg,"denseVeinSize",3,1,64); denseMinY=integer(cfg,"denseMinY",1,0,255); denseMaxY=integer(cfg,"denseMaxY",8,0,255);
        dryAttempts = integer(cfg,"dryIceAttempts",9,0,128); dryVein=integer(cfg,"dryIceVeinSize",17,1,64); dryMinY=integer(cfg,"dryIceMinY",1,0,255); dryMaxY=integer(cfg,"dryIceMaxY",64,0,255);
        dryTemperature = cfg.getFloat("dryIceMaxBiomeTemperature", "worldgen", 0.15F, -2.0F, 2.0F, "Maximum biome temperature for Dry Ice generation in biomes without COLD/SNOWY tags");
        allowedDimensions = cfg.get("worldgen", "allowedDimensions", new int[]{0},
                "Surface dimensions allowed to generate Uraninite and Dry Ice").getIntList();

        playerAerialPearl = cfg.getBoolean("player_aerial_pearl", "general", true, "Allow Aerial Pearl + zombie-family conversion into Player Aerial Pearl");
        lensOfEnder = cfg.getBoolean("lens_of_ender", "general", true, "Allow Photoelectric Pane + Enderman/Endermite conversion into Lens of Ender");
        dimensionalBindingCard = cfg.getBoolean("dimensional_binding_card", "general", true, "Allow Binding Card + Enderman/Endermite conversion into Dimensional Binding Card");

        EnergyConfigSnapshot base = DefaultEnergyConfig.create();
        energy = new EnergyConfigSnapshot(
                scale(base.energyPerFuelTick(), fuel), energizingCost,
                scale(profile(cfg, "furnator", base.furnator(), true), capacity, transfer, generation),
                scale(profile(cfg, "magmator", base.magmator(), true), capacity, transfer, generation),
                scale(profile(cfg, "reactor", base.reactor(), true), capacity, transfer, generation),
                scale(profile(cfg, "solar_panel", base.solarPanel(), true), capacity, transfer, generation),
                scale(profile(cfg, "thermo_generator", base.thermoGenerator(), true), capacity, transfer, generation),
                scale(profile(cfg, "battery", base.battery(), false), capacity, transfer, 1.0D),
                scale(profile(cfg, "energy_cell", base.energyCell(), false), capacity, transfer, 1.0D),
                scale(profile(cfg, "discharger", base.discharger(), false), capacity, transfer, 1.0D),
                scale(profile(cfg, "energy_hopper", base.energyHopper(), false), capacity, transfer, 1.0D),
                scale(profile(cfg, "player_transmitter", base.playerTransmitter(), false), capacity, transfer, 1.0D),
                scale(profile(cfg, "energizing_rod", base.energizingRod(), false), capacity, transfer, 1.0D),
                scale(tiers(cfg, "energy.cable.transfer", base.cableTransfer()), transfer),
                scale(tiers(cfg, "energy.ender_cell.transfer", base.enderCellTransfer()), transfer),
                scale(tiers(cfg, "energy.ender_gate.transfer", base.enderGateTransfer()), transfer),
                scale(tiers(cfg, "energy.energy_hopper.charging", base.hopperCharging()), transfer),
                scale(tiers(cfg, "energy.player_transmitter.charging", base.playerTransmitterCharging()), transfer),
                tiers(cfg, "energy.ender.channels", base.enderChannels(), 1L, EnderNetworkData.MAX_CHANNELS));
        if (cfg.hasChanged()) cfg.save();
        lastModified = file != null && file.isFile() ? file.lastModified() : 0L;
        revision++;
    }

    private static int integer(Configuration c,String key,int def,int min,int max){ return c.getInt(key,"worldgen",def,min,max,key); }
    private static EnergyConfigSnapshot.Profile profile(Configuration cfg, String machine,
                                                        EnergyConfigSnapshot.Profile defaults, boolean generator) {
        String category = "energy." + machine;
        return new EnergyConfigSnapshot.Profile(
                tiers(cfg, category + ".capacity", defaults.capacity()),
                tiers(cfg, category + ".transfer", defaults.transfer()),
                generator ? tiers(cfg, category + ".generation", defaults.generation()) : defaults.generation());
    }

    private static TieredLongValues tiers(Configuration cfg, String category, TieredLongValues defaults) {
        return tiers(cfg, category, defaults, 0L, Long.MAX_VALUE);
    }

    private static TieredLongValues tiers(Configuration cfg, String category, TieredLongValues defaults,
                                         long min, long max) {
        long[] values = defaults.copyValues();
        for (PowahTier tier : PowahTier.values()) {
            if (!tier.isNormal()) continue;
            long fallback = defaults.get(tier);
            Property property = cfg.get(category, tier.name().toLowerCase(Locale.ROOT),
                    Long.toString(fallback), "Per-tier value before global multipliers; range " + min + ".." + max,
                    Property.Type.INTEGER);
            values[tier.index()] = Math.max(min, Math.min(max, property.getLong(fallback)));
        }
        return new TieredLongValues(values[0], values[1], values[2], values[3], values[4], values[5], values[6]);
    }
    private static double positive(double value){ return value > 0.0D && Double.isFinite(value) ? value : 1.0D; }
    private static long scale(long value,double factor){
        if (factor == 1.0D) return value;
        double result = value * factor;
        return result >= Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(0L, (long) result);
    }
    private static TieredLongValues scale(TieredLongValues values,double factor){
        long[] v=values.copyValues(); for(int i=0;i<v.length;i++) v[i]=scale(v[i],factor);
        return new TieredLongValues(v[0],v[1],v[2],v[3],v[4],v[5],v[6]);
    }
    private static EnergyConfigSnapshot.Profile scale(EnergyConfigSnapshot.Profile profile,double capacity,double transfer,double generation){
        return new EnergyConfigSnapshot.Profile(scale(profile.capacity(),capacity),scale(profile.transfer(),transfer),scale(profile.generation(),generation));
    }

    /** Poll at most once per five seconds; no per-tick disk IO. */
    private static void pollFile() {
        File current=file; if(current==null) return;
        long now=System.nanoTime(); if(now<nextPollNanos) return; nextPollNanos=now+5_000_000_000L;
        long modified=current.isFile()?current.lastModified():0L; if(modified!=lastModified) reload();
    }
    public static EnergyConfigSnapshot energy(){ pollFile(); return energy; }
    public static long revision(){ pollFile(); return revision; }
    public static int energizingRange(){ pollFile(); return energizingRange; }
    public static int[] worldgenDimensions(){ pollFile(); return allowedDimensions.clone(); }
    public static int poorAttempts(){ pollFile(); return poorAttempts;} public static int poorVein(){ pollFile(); return poorVein;} public static int poorMinY(){ pollFile(); return Math.min(poorMinY,poorMaxY);} public static int poorMaxY(){ pollFile(); return Math.max(poorMinY,poorMaxY);}
    public static int normalAttempts(){ pollFile(); return normalAttempts;} public static int normalVein(){ pollFile(); return normalVein;} public static int normalMinY(){ pollFile(); return Math.min(normalMinY,normalMaxY);} public static int normalMaxY(){ pollFile(); return Math.max(normalMinY,normalMaxY);}
    public static int denseAttempts(){ pollFile(); return denseAttempts;} public static int denseVein(){ pollFile(); return denseVein;} public static int denseMinY(){ pollFile(); return Math.min(denseMinY,denseMaxY);} public static int denseMaxY(){ pollFile(); return Math.max(denseMinY,denseMaxY);}
    public static int dryAttempts(){ pollFile(); return dryAttempts;} public static int dryVein(){ pollFile(); return dryVein;} public static int dryMinY(){ pollFile(); return Math.min(dryMinY,dryMaxY);} public static int dryMaxY(){ pollFile(); return Math.max(dryMinY,dryMaxY);} public static double dryTemperature(){ pollFile(); return dryTemperature;}
    public static boolean playerAerialPearl(){ pollFile(); return playerAerialPearl;}
    public static boolean lensOfEnder(){ pollFile(); return lensOfEnder;}
    public static boolean dimensionalBindingCard(){ pollFile(); return dimensionalBindingCard;}
}
