package com.sosea1.powah.common.config;

import java.io.File;
import net.minecraftforge.common.config.Configuration;

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
        dryTemperature = cfg.getFloat("dryIceMaxBiomeTemperature", "worldgen", 0.15F, -2.0F, 2.0F, "Maximum biome temperature for Dry Ice generation");

        playerAerialPearl = cfg.getBoolean("player_aerial_pearl", "general", true, "Allow Aerial Pearl + zombie-family conversion into Player Aerial Pearl");
        lensOfEnder = cfg.getBoolean("lens_of_ender", "general", true, "Allow Photoelectric Pane + Enderman/Endermite conversion into Lens of Ender");
        dimensionalBindingCard = cfg.getBoolean("dimensional_binding_card", "general", true, "Allow Binding Card + Enderman/Endermite conversion into Dimensional Binding Card");

        EnergyConfigSnapshot base = DefaultEnergyConfig.create();
        energy = new EnergyConfigSnapshot(
                scale(base.energyPerFuelTick(), fuel), energizingCost,
                scale(base.furnator(), capacity, transfer, generation),
                scale(base.magmator(), capacity, transfer, generation),
                scale(base.reactor(), capacity, transfer, generation),
                scale(base.solarPanel(), capacity, transfer, generation),
                scale(base.thermoGenerator(), capacity, transfer, generation),
                scale(base.battery(), capacity, transfer, 1.0D),
                scale(base.energyCell(), capacity, transfer, 1.0D),
                scale(base.discharger(), capacity, transfer, 1.0D),
                scale(base.energyHopper(), capacity, transfer, 1.0D),
                scale(base.playerTransmitter(), capacity, transfer, 1.0D),
                scale(base.energizingRod(), capacity, transfer, 1.0D),
                scale(base.cableTransfer(), transfer),
                scale(base.enderCellTransfer(), transfer),
                scale(base.enderGateTransfer(), transfer),
                scale(base.hopperCharging(), transfer),
                scale(base.playerTransmitterCharging(), transfer),
                base.enderChannels());
        if (cfg.hasChanged()) cfg.save();
        lastModified = file != null && file.isFile() ? file.lastModified() : 0L;
        revision++;
    }

    private static int integer(Configuration c,String key,int def,int min,int max){ return c.getInt(key,"worldgen",def,min,max,key); }
    private static double positive(double value){ return value > 0.0D && Double.isFinite(value) ? value : 1.0D; }
    private static long scale(long value,double factor){ double r=value*factor; return r>=Long.MAX_VALUE?Long.MAX_VALUE:Math.max(0L,(long)r); }
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
    public static int poorAttempts(){ pollFile(); return poorAttempts;} public static int poorVein(){ pollFile(); return poorVein;} public static int poorMinY(){ pollFile(); return Math.min(poorMinY,poorMaxY);} public static int poorMaxY(){ pollFile(); return Math.max(poorMinY,poorMaxY);}
    public static int normalAttempts(){ pollFile(); return normalAttempts;} public static int normalVein(){ pollFile(); return normalVein;} public static int normalMinY(){ pollFile(); return Math.min(normalMinY,normalMaxY);} public static int normalMaxY(){ pollFile(); return Math.max(normalMinY,normalMaxY);}
    public static int denseAttempts(){ pollFile(); return denseAttempts;} public static int denseVein(){ pollFile(); return denseVein;} public static int denseMinY(){ pollFile(); return Math.min(denseMinY,denseMaxY);} public static int denseMaxY(){ pollFile(); return Math.max(denseMinY,denseMaxY);}
    public static int dryAttempts(){ pollFile(); return dryAttempts;} public static int dryVein(){ pollFile(); return dryVein;} public static int dryMinY(){ pollFile(); return Math.min(dryMinY,dryMaxY);} public static int dryMaxY(){ pollFile(); return Math.max(dryMinY,dryMaxY);} public static double dryTemperature(){ pollFile(); return dryTemperature;}
    public static boolean playerAerialPearl(){ pollFile(); return playerAerialPearl;}
    public static boolean lensOfEnder(){ pollFile(); return lensOfEnder;}
    public static boolean dimensionalBindingCard(){ pollFile(); return dimensionalBindingCard;}
}
