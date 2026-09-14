# Copper Age Backport Patch

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](https://minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.x-orange.svg)](https://neoforged.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.15.x-blue.svg)](https://fabricmc.net/)
[![Modrinth](https://img.shields.io/badge/Modrinth-4Gz003Sy-00AF5C.svg)](https://modrinth.com/mod/copper-age-backport-patch)

An unofficial patch, bugfix, and comprehensive compatibility addon for **[Copper Age Backport](https://github.com/Smallinger/Copper-Age-Backport)** (by Smallinger) on **Minecraft 1.21.1** for both **NeoForge** and **Fabric** loaders.

---

## 📖 Overview

While **Copper Age Backport** brings official modern copper equipment and the Copper Golem to Minecraft 1.21.1, it suffers from several registry crashes on modern loader versions, missing durability properties, missing creative tab placements, and lack of trim/mod integrations.

**Copper Age Backport Patch** is a clean-room, zero-copy companion mod that resolves these issues without altering original assets or requiring external fix mods. It builds from a single unified codebase supporting both NeoForge and Fabric simultaneously.

---

## ✨ Comprehensive Feature Breakdown

### 1. 🛡️ Registry Duplicate Key Crash Fix
* **The Problem:** NeoForge 21.1.237+ and Fabric 0.15.0+ enforce strict registry validation. Upstream registers `ResourceKey[minecraft:armor_material / minecraft:copper]` multiple times across separate item initializations, triggering an `IllegalStateException: Duplicate key` that crashes the game or forces a fallback to vanilla.
* **The Fix:** An idempotent, concurrency-safe `MemoizedSupplier` interceptor via Mixin ensures the armor material registration occurs exactly once, allowing seamless world loading.

### 2. 🔨 Canonical Copper Armor Durability Fix
* **The Problem:** In the upstream mod, copper armor items lacked a canonical `MAX_DAMAGE` data component, rendering them effectively infinite/undamageable.
* **The Fix:** Restores balanced durability values following standard vanilla tier progression using the tier multiplier of `11`:
  * **Copper Helmet:** `121` max durability
  * **Copper Chestplate:** `176` max durability
  * **Copper Leggings:** `165` max durability
  * **Copper Boots:** `143` max durability
* *Note: The third-party `copperagebackport_durability_fix` mod is completely obsolete and no longer needed.*

### 3. ⚔️ Creative Combat Tab Placement
* In vanilla Minecraft, all axes appear in both *Tools & Utilities* and *Combat* tabs. Upstream omitted the Copper Axe from the Combat tab.
* Copper Axe is now properly placed in the Creative **Combat** tab immediately following the Stone Axe and preceding the Iron Axe, preserving natural progression (`Wooden -> Stone -> Copper -> Iron`).

### 4. 🎨 Complete Smithing Trim Integration
#### 4.1 Copper Armor Trims (Inventory 2D + Player 3D)
* Adds model override definitions across all 4 armor pieces for all 10 standard trim materials (`trim_type` predicates from `0.1` to `1.0`).
* Provides 44 discrete item models with layered rendering (`layer0` for base armor, `layer1` for colored trim overlay).
* Introduces a custom `copper_darker` palette permutation in `assets/minecraft/atlases/blocks.json` so copper trims applied to copper armor maintain high visual contrast, matching vanilla gold-on-gold and iron-on-iron conventions.
* Preserves in-world player 3D entity armor trim rendering.

#### 4.2 Tool Trims Mod Compatibility ([Tool Trims](https://modrinth.com/mod/tool-trims))
* Enables Smithing Table trimming for all 5 copper tools: **Copper Sword, Copper Axe, Copper Pickaxe, Copper Shovel, and Copper Hoe**.
* Supports all **4 Tool Trims patterns** (*Linear, Tracks, Charge, Frost*) and all **10 trim materials** (*Amethyst, Copper, Diamond, Emerald, Gold, Iron, Lapis, Netherite, Quartz, Redstone*), totaling **200 data-driven smithing recipes**.
* Reuses Tool Trims' colorized iron palettes—since iron and copper tool silhouettes match pixel-perfectly, no bloated redundant textures are added.
* **Z-Fighting Elimination:** Trim models utilize Tool Trims' layer architecture (`layer0` mapped to `tooltrims:item/trim_bases/iron_<tool>_<pattern>` overlay) to eliminate any coplanar z-fighting artifacts in 2D hand or 3D world rendering.
* Copper tools are dynamically enrolled into `#tooltrims:trimmable_tools` as well as standard vanilla tool tags (`#minecraft:swords`, `#minecraft:axes`, etc.).

#### 4.3 High-Priority Built-In Resource Pack
* To prevent upstream `copperagebackport`'s plain, override-less item models from masking trimmed models due to alphabetical resource pack sorting, all copper equipment models are packaged into an always-enabled built-in resource pack (`copper_trims`).
* Registered via `ResourceManagerHelper.registerBuiltinResourcePack` (`ALWAYS_ENABLED`) on Fabric and `AddPackFindersEvent` (`Pack.Position.TOP`) on NeoForge.

### 5. 🥚 Conditional Modern Copper Golem Spawn Egg
* Conditionally replaces the classic dotted spawn egg icon with the modern, detailed Copper Golem spawn egg texture when **[Vanilla Backport](https://modrinth.com/mod/vanilla-backport)** is installed.
* Fully configurable via `config/copper_age_patch.json` with three modes:
  * `"AUTO"` (default): Automatically uses the modern texture if Vanilla Backport is present, or classic if absent.
  * `"MODERN"`: Forces the modern detailed texture.
  * `"CLASSIC"`: Forces the classic two-tone dotted vanilla texture.

### 6. 🔍 Jade HUD Integration ([Jade](https://modrinth.com/mod/jade))
* **Antenna Items:** Detects and displays items placed on the Copper Golem's antenna (such as the poppy flower gifted by an Iron Golem).
* **Weathering & Waxing:** Displays the current oxidation stage (*Unaffected, Exposed, Weathered, Oxidized*) and whether the golem/statue is waxed with honeycomb.
* **Held Items:** Displays the item currently held in the Copper Golem's hands.
* **Shelf Inventories:** Provides full visual inventory previews when hovering over Copper Shelves.

### 7. ⚙️ Create Mod & Tag Interoperability ([Create](https://modrinth.com/mod/create))
* Enrolls copper nuggets from all sources into common Conventional tags: `#c:nuggets`, `#c:nuggets/copper`, and `#c:copper_nuggets`.
* Bidirectional crafting recipes: 9 copper nuggets craft 1 copper ingot, and 1 copper ingot crafts 9 copper nuggets.
* Adds Create Crushing Wheel recipes to crush copper equipment down into copper ingots and nuggets.

### 8. 🗡️ Better Combat Integration ([Better Combat](https://modrinth.com/mod/better-combat))
* Configures native attack animations, weapon attributes, range, and sweeping hitboxes for copper swords and axes.

### 9. 🌍 Multilingual Localization
Full in-game translations for over 20 languages:
Arabic, Azerbaijani, Chinese (Simplified & Traditional), Czech, English (US & UK), French, German, Hungarian, Italian, Japanese, Korean, Polish, Portuguese (Brazil & Portugal), Russian, Spanish, Thai, Turkish, and Vietnamese.

---

## 🔧 Configuration

A client-side configuration file is located at `config/copper_age_patch.json`:

```json
{
  "spawnEggTextureMode": "AUTO"
}
```

| Value | Behavior |
| :--- | :--- |
| `"AUTO"` | Modern texture if Vanilla Backport is present; classic dotted egg otherwise. *(Default)* |
| `"MODERN"` | Always use the modern detailed spawn egg texture. |
| `"CLASSIC"` | Always use the classic vanilla dotted spawn egg texture. |

---

## 📦 Compatibility & Dependencies

| Mod | Platform | Requirement | Purpose |
| :--- | :--- | :--- | :--- |
| **[Copper Age Backport](https://modrinth.com/mod/backport-copper-age)** | NeoForge & Fabric | **Required** | The base mod being patched |
| **[Fabric API](https://modrinth.com/mod/fabric-api)** | Fabric | **Required** | Core framework on Fabric |
| **[Tool Trims](https://modrinth.com/mod/tool-trims)** | NeoForge & Fabric | Optional | Enables smithing trims on copper tools |
| **[Vanilla Backport](https://modrinth.com/mod/vanilla-backport)** | NeoForge & Fabric | Optional | Triggers modern spawn egg texture in AUTO mode |
| **[Jade](https://modrinth.com/mod/jade)** | NeoForge & Fabric | Optional | HUD tooltips for golems, statues, and shelves |
| **[Better Combat](https://modrinth.com/mod/better-combat)** | NeoForge & Fabric | Optional | Custom combat animations and sweep attacks |
| **[Create](https://modrinth.com/mod/create)** | NeoForge & Fabric | Optional | Crushing recipes & nugget recycling |

---

## 🛠️ Building From Source

This project uses Gradle with Java 21:

```bash
# Clone the repository
git clone https://github.com/1unarea/copper-age-backport-patch.git
cd copper-age-backport-patch

# Run all unit and verification tests (107 tests)
./gradlew test

# Assemble both NeoForge and Fabric jars
./gradlew assemble

# Generated artifacts:
# - build/libs/copper_age_patch-neoforge-1.21.1-0.1.3.jar
# - build/libs/copper_age_patch-fabric-1.21.1-0.1.3.jar
```

---

## 📄 License & Attribution

* **Original Mod:** [Copper Age Backport](https://github.com/Smallinger/Copper-Age-Backport) by **Smallinger** (licensed under CC0).
* **Patch:** Developed by **1unarea** under the [MIT License](LICENSE).
* Clean-room implementation: Zero external copyrighted or GPL code was copied or decompiled.
