# Retro Powah

Retro Powah brings the modern [Powah!](https://github.com/Technici4n/Powah) mod to Minecraft 1.12.2. This is an unofficial backport of its content and gameplay, not a port of the older 1.16.5 codebase.

## Requirements

- Minecraft 1.12.2 with Forge or Cleanroom.

## Features

- Tiered energy generation with Furnators, Magmators, Solar Panels, Thermo Generators, and multiblock Reactors.
- Energy storage and transfer through Batteries, Energy Cells, Cables, Ender Cells, and Ender Gates.
- Energizing Orb recipes with Energizing Rods, plus Energy Hoppers, Player Transmitters, and other utility blocks.
- Ores, materials, crafting recipes, and configuration for machine tiers and energy rates.

## Optional integrations

- JEI shows recipes and machine information.
- CraftTweaker can add, remove, and clear Energizing recipes, fuels, coolants, and heat sources.
- Baubles accessories can be charged by batteries and Player Transmitters.
- The One Probe and HWYLA show machine information in the world.
- Patchouli provides the in-game guidebook.

## Configuration

`config/powah.cfg` contains per-tier capacities, transfer rates, generation, charging rates, and Ender channel counts. Global energy multipliers apply on top of these values. Defaults follow modern Powah; Ender networks support up to 12 channels. For example, `energy.reactor.generation` controls reactor production and `energy.cable.transfer` controls each cable output, not the total throughput of the network.

On dedicated servers, distribute the same `powah.cfg` to clients so item tooltips match server limits. Tier settings are not synchronized automatically; machine behavior uses the server configuration.

World generation defaults to dimension `0`. Add other surface dimensions to `worldgen.allowedDimensions` to opt in. Nether, End, and void biomes remain excluded. Dry Ice uses Forge's `COLD`/`SNOWY` biome tags, with a temperature fallback. Changes affect newly generated terrain only.

## CraftTweaker

Startup scripts in `scripts/*.zs` can adjust these registries:

```zenscript
mods.powah.MagmaticFluid.add(<liquid:lava>, 10000); // FE per 100 mB
mods.powah.Coolant.add(<liquid:water>, 0); // temperature
mods.powah.HeatSource.add("minecraft:lava", 1000); // block and temperature
mods.powah.SolidCoolant.add(<minecraft:snowball>, 12.0, -3); // amount and temperature
mods.powah.ReactorFuel.add(<powah:uraninite>, 100.0, 700); // fuel amount and temperature
```

Each registry also has `remove(input)` and `clear()`. Heat sources accept either a block item or its registry name. `mods.powah.Energizing` provides `addExact`, `addOre`, `remove`, and `clear`; adding an existing recipe ID replaces that recipe. Item registrations apply to the item type, not individual metadata or NBT variants. Restart the game after script changes; live reload does not rebuild JEI or undo earlier actions.

## 1.12.2 adaptations

The guidebook uses optional Patchouli instead of GuideME. Cables additionally support legacy FE consumers that pull energy. The raw Uraninite storage block and its reversible crafting recipes are Retro Powah additions. Dense Uraninite generates near the bottom of the world because 1.12.2 has no negative-Y terrain.

## Build

Use JDK 25 and the included Gradle wrapper. Run `gradlew.bat build` on Windows or `./gradlew build` on Linux and macOS. The playable jar is written to `build/libs/powah-0.1.0-beta.jar`.

## Credits

- [owmii](https://github.com/owmii/Powah) created the original Powah! mod.
- [Technici4n](https://github.com/Technici4n/Powah) and contributors maintain modern Powah!.
- Cyn and SammySemicolon contributed textures to Powah!.
- The Gradle project uses parts of [Cleanroom ForgeDevEnv](https://github.com/CleanroomMC/ForgeDevEnv).

## License

Retro Powah follows the original Powah! [LGPL-3.0 terms](LICENSE), together with the [GNU GPL-3.0 terms](COPYING) incorporated by LGPL-3.0. Powah source and assets retain their original authorship: owmii, Technici4n and contributors; texture credits include Cyn and SammySemicolon. This repository is an unofficial, modified backport maintained by Sosea1. License texts and this README are included in distributed jars under `META-INF/powah`.

The retained Cleanroom ForgeDevEnv build scaffold is under the following original MIT notice:

```text
MIT License

Copyright (c) 2022 CleanroomMC

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
