# Copper Age Backport Patch

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](https://minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.x-orange.svg)](https://neoforged.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.15.x-blue.svg)](https://fabricmc.net/)
[![Modrinth](https://img.shields.io/badge/Modrinth-4Gz003Sy-00AF5C.svg)](https://modrinth.com/mod/copper-age-backport-patch)

An unofficial patch, bugfix, and compatibility addon for [Copper Age Backport](https://github.com/Smallinger/Copper-Age-Backport) (by Smallinger) on Minecraft 1.21.1 for NeoForge and Fabric mod loaders.

---

## Overview

Copper Age Backport brings official modern copper equipment and the Copper Golem to Minecraft 1.21.1. While the mod runs standalone without issues on Fabric, modern NeoForge versions suffer from a registry crash on startup. In addition, across both loaders, the upstream mod lacks canonical armor durability properties, creative combat tab positioning, smithing trims, and integrations with modern 1.21.1 ecosystem mods.

Copper Age Backport Patch is a clean-room companion mod that resolves these issues from a single unified codebase supporting both NeoForge and Fabric.

---

## Feature Breakdown

### 1. NeoForge Registry Duplicate Key Crash Fix
* **The Problem:** NeoForge 21.1.237 and newer enforces strict registry validation during bootstrap. Upstream registers `ResourceKey[minecraft:armor_material / minecraft:copper]` multiple times across separate item initializations. This triggers an `IllegalStateException: Duplicate key` on NeoForge that crashes the game or causes world load to fail with a safe mode prompt. (Note: Fabric does not enforce this restriction and runs standalone).
* **The Fix:** A thread-safe `MemoizedSupplier` interceptor implemented via Mixin guarantees that the armor material registration executes exactly once during startup, preventing collisions on NeoForge.

### 2. Canonical Copper Armor Durability Fix
* **The Problem:** In upstream Copper Age Backport, copper armor items omit the canonical `MAX_DAMAGE` data component, causing armor pieces to have infinite durability and never take damage.
* **The Fix:** Restores balanced durability values following standard vanilla tier progression using the canonical tier multiplier of 11:
  * Copper Helmet: 121 max durability
  * Copper Chestplate: 176 max durability
  * Copper Leggings: 165 max durability
  * Copper Boots: 143 max durability
* *Note: The third-party copperagebackport_durability_fix mod is obsolete and no longer needed.*

### 3. Creative Combat Tab Placement
* In vanilla Minecraft, axes appear in both the Tools & Utilities tab and the Combat tab. Upstream omitted the Copper Axe from the Combat tab.
* Copper Axe is placed in the Creative Combat tab directly after Stone Axe and before Iron Axe, maintaining tier progression (Wooden -> Stone -> Copper -> Iron).

### 4. Complete Smithing Trim Integration
#### 4.1 Copper Armor Trims (Inventory 2D + Player 3D)
* Adds model override definitions across all 4 armor pieces for all 10 standard trim materials (trim_type predicates from 0.1 to 1.0).
* Provides 44 discrete item models with layered rendering (layer0 for base armor, layer1 for colored trim overlay).
* Introduces a custom `copper_darker` palette permutation in `assets/minecraft/atlases/blocks.json` so copper trims applied to copper armor maintain high visual contrast, matching vanilla gold-on-gold and iron-on-iron conventions.
* Preserves in-world player 3D entity armor trim rendering.

#### 4.2 Tool Trims Mod Compatibility
* Enables Smithing Table trimming for all 5 copper tools: Copper Sword, Copper Axe, Copper Pickaxe, Copper Shovel, and Copper Hoe with [Tool Trims](https://modrinth.com/mod/tool-trims).
* Supports all 4 Tool Trims patterns (Linear, Tracks, Charge, Frost) and all 10 trim materials (Amethyst, Copper, Diamond, Emerald, Gold, Iron, Lapis, Netherite, Quartz, Redstone), providing 200 data-driven smithing recipes.
* Reuses Tool Trims colorized palettes based on iron tool silhouettes, avoiding redundant texture assets.
* **Z-Fighting Elimination:** Trim models utilize Tool Trims layer architecture (layer0 mapped to `tooltrims:item/trim_bases/iron_<tool>_<pattern>` overlay) to eliminate coplanar z-fighting artifacts in hand or world rendering.
* Copper tools are dynamically enrolled into `#tooltrims:trimmable_tools` as well as standard vanilla tool tags (`#minecraft:swords`, `#minecraft:axes`, etc.).

#### 4.3 High-Priority Built-In Resource Pack
* To ensure copper equipment models with trim overrides are not masked by upstream plain models due to pack load order, models are packaged into an always-enabled built-in resource pack (`copper_trims`).
* Registered via `ResourceManagerHelper.registerBuiltinResourcePack` (ALWAYS_ENABLED) on Fabric and `AddPackFindersEvent` (Pack.Position.TOP) on NeoForge.

### 5. Conditional Modern Copper Golem Spawn Egg
* Conditionally replaces the classic dotted spawn egg icon with the modern detailed Copper Golem spawn egg texture when [Vanilla Backport](https://modrinth.com/mod/vanilla-backport) is installed.
* Configurable via `config/copper_age_patch.json`:
  * `"AUTO"` (default): Uses the modern texture if Vanilla Backport is installed, classic otherwise.
  * `"MODERN"`: Forces the modern detailed texture.
  * `"CLASSIC"`: Forces the classic vanilla dotted texture.

### 6. Jade HUD Integration
* **Antenna Items:** Detects and displays items placed on the Copper Golem antenna (such as the poppy flower gifted by an Iron Golem).
* **Weathering & Waxing:** Displays the current oxidation stage (Unaffected, Exposed, Weathered, Oxidized) and whether the golem or statue is waxed.
* **Held Items:** Displays the item currently held by the Copper Golem.
* **Shelf Inventories:** Provides full visual inventory previews when looking at Copper Shelves via [Jade](https://modrinth.com/mod/jade).

### 7. Create Mod & Tag Interoperability
* Enrolls copper nuggets from all sources into common tags: `#c:nuggets`, `#c:nuggets/copper`, and `#c:copper_nuggets`.
* Bidirectional crafting recipes: 9 copper nuggets craft 1 copper ingot, and 1 copper ingot crafts 9 copper nuggets.
* Adds Crushing Wheel recipes to recycle copper equipment into copper ingots and nuggets with [Create](https://modrinth.com/mod/create).

### 8. Better Combat Integration
* Configures native attack animations, weapon attributes, range, and sweeping hitboxes for copper weapons with [Better Combat](https://modrinth.com/mod/better-combat).

### 9. Multilingual Localization
Full in-game translations for over 20 languages:
Arabic, Azerbaijani, Chinese (Simplified & Traditional), Czech, English (US & UK), French, German, Hungarian, Italian, Japanese, Korean, Polish, Portuguese (Brazil & Portugal), Russian, Spanish, Thai, Turkish, and Vietnamese.

---

## Configuration

A client-side configuration file is located at `config/copper_age_patch.json`:

```json
{
  "spawnEggTextureMode": "AUTO"
}
```

| Value | Behavior |
| :--- | :--- |
| `"AUTO"` | Modern texture if Vanilla Backport is present; classic dotted egg otherwise. (Default) |
| `"MODERN"` | Always use the modern detailed spawn egg texture. |
| `"CLASSIC"` | Always use the classic vanilla dotted spawn egg texture. |

---

## Compatibility & Dependencies

| Mod | Platform | Requirement | Purpose |
| :--- | :--- | :--- | :--- |
| [Copper Age Backport](https://modrinth.com/mod/backport-copper-age) | NeoForge & Fabric | Required | The base mod being patched |
| [Fabric API](https://modrinth.com/mod/fabric-api) | Fabric | Required | Core framework on Fabric |
| [Tool Trims](https://modrinth.com/mod/tool-trims) | NeoForge & Fabric | Optional | Enables smithing trims on copper tools |
| [Vanilla Backport](https://modrinth.com/mod/vanilla-backport) | NeoForge & Fabric | Optional | Modern spawn egg texture trigger |
| [Jade](https://modrinth.com/mod/jade) | NeoForge & Fabric | Optional | HUD tooltips for golems, statues, and shelves |
| [Better Combat](https://modrinth.com/mod/better-combat) | NeoForge & Fabric | Optional | Custom combat animations and weapon stats |
| [Create](https://modrinth.com/mod/create) | NeoForge & Fabric | Optional | Crushing recipes and nugget recycling |

---

## Building From Source

This project uses Gradle with Java 21:

```bash
# Clone the repository
git clone https://github.com/1unarea/copper-age-backport-patch.git
cd copper-age-backport-patch

# Run all verification tests
./gradlew test

# Assemble NeoForge and Fabric jars
./gradlew assemble

# Generated artifacts:
# - build/libs/copper_age_patch-neoforge-1.21.1-0.1.3.jar
# - build/libs/copper_age_patch-fabric-1.21.1-0.1.3.jar
```

---

## License & Attribution

* Original Mod: [Copper Age Backport](https://github.com/Smallinger/Copper-Age-Backport) by Smallinger (CC0).
* Patch: Developed by 1unarea under the [MIT License](LICENSE).
* Clean-room implementation: No proprietary or external code was copied.
