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
- CraftTweaker can add and remove Energizing recipes.
- The One Probe and HWYLA show machine information in the world.
- Patchouli provides the in-game guidebook.

## Build

Use JDK 25 and the included Gradle wrapper. Run `gradlew.bat build` on Windows or `./gradlew build` on Linux and macOS. The playable jar is written to `build/libs/powah-0.1.0-beta.jar`.

## Credits

- [owmii](https://github.com/owmii/Powah) created the original Powah! mod.
- [Technici4n](https://github.com/Technici4n/Powah) and contributors maintain modern Powah!.
- Cyn and SammySemicolon contributed textures to Powah!.
- The Gradle project uses parts of [Cleanroom ForgeDevEnv](https://github.com/CleanroomMC/ForgeDevEnv).

## License

Retro Powah follows the original Powah! [LGPL-3.0 terms](https://www.gnu.org/licenses/lgpl-3.0.html), together with the [GNU GPL-3.0 terms](https://www.gnu.org/licenses/gpl-3.0.html) referenced by LGPL-3.0. The retained Cleanroom ForgeDevEnv build scaffold is under its [MIT license](https://github.com/CleanroomMC/ForgeDevEnv/blob/master/LICENSE), copyright © 2022 CleanroomMC.
