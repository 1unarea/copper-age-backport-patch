# Copper Age Backport Patch

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](https://minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.x-orange.svg)](https://neoforged.net/)

**Copper Age Backport Patch** is an unofficial patch and compatibility addon for [Copper Age Backport](https://github.com/Smallinger/Copper-Age-Backport) by **Smallinger** on Minecraft 1.21.1 NeoForge.

It fixes the startup registry crash on modern NeoForge builds and integrates the Copper Age items and mobs with popular mods like Jade, Better Combat, and Create.

---

## Features

### 1. NeoForge 21.1.250+ Crash Fix
On NeoForge 21.1.237 and newer, duplicate registry entries cause a strict crash (`IllegalStateException: Adding duplicate key 'ResourceKey[minecraft:armor_material / minecraft:copper]'`), forcing the loader to roll back to vanilla state.
* Fix: Intercepts `CopperArmorMaterial.createCopper()` with a memoized supplier to ensure the material is registered exactly once.

### 2. Jade HUD Integration
* Copper Golem Tooltip: Displays the item currently held in the golem's hand (icon, name, count), its weathering stage (Unaffected, Exposed, Weathered, Oxidized), and whether it is waxed.
* Copper Golem Statue Tooltip: Displays oxidation stage, waxed status, and custom name (if named).
* Shelf Tooltip: Visually renders stored items, stack quantities, and item titles.

### 3. Better Combat Compatibility
* Provides weapon attribute configs (`bettercombat:weapon_attributes`) for all copper equipment:
  * Copper Sword, Axe, Pickaxe, Shovel, and Hoe.
* Enables fluid combo animations, attack sweeps, and weapon hitboxes.

### 4. Create Mod Recycling Recipes
* Adds crushing recipes for the Create Crushing Wheel:
  * Crush any copper armor piece (helmet, chestplate, leggings, boots) or tool into copper nuggets and ingots.

### 5. Common and NeoForge Tags
* Automatically populates standard tags:
  * `c:armors/helmets`, `c:armors/chestplates`, `c:armors/leggings`, `c:armors/boots`
  * `c:tools/swords`, `c:tools/axes`, `c:tools/pickaxes`, `c:tools/shovels`, `c:tools/hoes`
  * Corresponding `minecraft:tools/*` tags.

### 6. Multi-Language Support (20+ Languages)
Full in-game localizations included out of the box:
* English (US and UK)
* Turkish and Azerbaijani Turkish
* Spanish (Spain, Mexico, Argentina, Chile, Colombia, Ecuador, Venezuela, Uruguay)
* Portuguese (Brazil and Portugal)
* German, French, Italian, Polish, Czech, Hungarian
* Russian, Ukrainian
* Chinese (Simplified and Traditional), Japanese, Korean
* Vietnamese, Thai, Arabic

---

## Requirements

* Minecraft: 1.21.1
* Mod Loader: NeoForge (21.1.237 or newer)
* Target Mod: [Copper Age Backport](https://github.com/Smallinger/Copper-Age-Backport) (0.1.4+) (Required)

### Optional Compatibilities:
* [Jade](https://modrinth.com/mod/jade)
* [Better Combat](https://modrinth.com/mod/better-combat)
* [Create](https://modrinth.com/mod/create)

---

## Building from Source

1. Clone this repository:
   ```bash
   git clone https://github.com/aegeada/copper-age-backport-patch.git
   cd copper-age-backport-patch
   ```

2. Place required compile-time libraries into the `libs/` directory (e.g. `copperagebackport-neoforge-1.21.1-0.1.4.jar` and `Jade-1.21.1-NeoForge-15.10.6.jar`).

3. Build the project using Gradle:
   ```bash
   ./gradlew build
   ```
   The compiled `.jar` artifact will be in `build/libs/`.

---

## Credits and License

* Original Mod: [Copper Age Backport](https://github.com/Smallinger/Copper-Age-Backport) by **Smallinger** (licensed under MIT / CC0).
* **Copper Age Backport Patch** is developed by **aegeada** and licensed under the [MIT License](LICENSE).
