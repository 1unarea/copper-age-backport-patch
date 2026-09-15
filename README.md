# Copper Age Backport Patch

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](https://minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.x-orange.svg)](https://neoforged.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.15.x-blue.svg)](https://fabricmc.net/)
[![Modrinth](https://img.shields.io/badge/Modrinth-4Gz003Sy-00AF5C.svg)](https://modrinth.com/mod/copper-age-backport-patch)
[![CurseForge](https://img.shields.io/badge/CurseForge-1696121-F16436.svg)](https://www.curseforge.com/minecraft/mc-mods/copper-age-backport-patch)

An unofficial patch, bugfix, and compatibility addon for [Copper Age Backport](https://github.com/Smallinger/Copper-Age-Backport) (by Smallinger) on Minecraft 1.21.1 for NeoForge and Fabric mod loaders.

---

## Overview

Copper Age Backport brings official modern copper equipment and the Copper Golem to Minecraft 1.21.1.

Important loader distinction:
* On **Fabric**, Copper Age Backport runs completely fine standalone and suffers from no startup or registry crashes. On Fabric, this patch serves purely as a gameplay, balance, visual, and mod compatibility enhancement.
* On **NeoForge** (versions 21.1.237 and newer), Copper Age Backport has a critical registry collision bug that crashes the game on startup or aborts world loading with a safe mode warning. This patch intercepts and fixes that crash on NeoForge.

Across both loaders, this patch restores canonical armor durability, enables copper anvil repairs, corrects creative inventory ordering, implements proper vanilla armor trim rendering, adds optional compatibility for the Tool Trims mod, enhances the Copper Golem spawn egg texture, provides in-game JEI / EMI / REI information pages, and integrates with popular mods such as Jade, Better Combat, and Create.

---

## Feature Breakdown

### 1. NeoForge Registry Duplicate Key Crash Fix
* The Problem: NeoForge 21.1.237 and newer enforces strict registry validation during bootstrap. Upstream Copper Age Backport registers `ResourceKey[minecraft:armor_material / minecraft:copper]` multiple times across separate item initializations. On NeoForge, this triggers an `IllegalStateException: Duplicate key` error, causing the game to crash or forcing world loading into Safe Mode.
* Important Note: Fabric does not enforce this restriction and runs Copper Age Backport standalone without issues. This crash fix is active only on NeoForge.
* The Fix: A thread-safe `MemoizedSupplier` interceptor implemented via Mixin ensures the armor material registration executes exactly once during startup on NeoForge, completely eliminating the crash.

### 2. Canonical Copper Armor Durability Fix
* The Problem: In upstream Copper Age Backport, copper armor items omit the canonical `MAX_DAMAGE` data component introduced in modern Minecraft (1.20.5+ / 1.21). As a result, copper armor pieces are completely indestructible and never lose durability when taking damage.
* The Fix: Restores balanced durability values following standard vanilla tier progression using the canonical tier multiplier of 11:
  * Copper Helmet: 121 max durability
  * Copper Chestplate: 176 max durability
  * Copper Leggings: 165 max durability
  * Copper Boots: 143 max durability
* Note: Separate third-party durability fix mods are obsolete and no longer needed.

### 3. Anvil Repair with Copper Ingots
* Full anvil repair functionality for all copper equipment:
  * Copper Helmet, Chestplate, Leggings, and Boots can be repaired on an Anvil using Copper Ingots (`#c:ingots/copper`).
  * Copper Sword, Axe, Pickaxe, Shovel, and Hoe can be repaired on an Anvil using Copper Ingots (`#c:ingots/copper`).
* Restores natural gear maintenance without requiring creative workarounds or item replacement.

### 4. Creative Inventory Tab Ordering
* **Copper Axe in Combat Tab**: In vanilla Minecraft, all axes appear in both the Tools & Utilities tab and the Combat tab. Upstream omitted the Copper Axe from the Combat tab. It is now positioned in the Creative Combat tab directly after Stone Axe and before Iron Axe (Wooden -> Stone -> Copper -> Iron).
* **Copper Golem Spawn Egg Placement**: Upstream placed the spawn egg at the very end of the creative tab. It is now repositioned in the Creative Spawn Eggs tab directly after the Cod Spawn Egg and before the Cow Spawn Egg, exactly matching vanilla Minecraft 1.21.9 release order.

### 5. Smithing Trim Integration: Armor vs. Tools
It is important to distinguish between how armor trims and tool trims work in this mod:

#### 5.1 Copper Armor Trims (Native Vanilla Feature - No Other Mods Required)
* Copper armor pieces (Helmet, Chestplate, Leggings, Boots) can be trimmed at a Smithing Table out of the box using standard vanilla armor trim smithing templates.
* Adds model override definitions across all 4 armor pieces for all 10 standard trim materials (`trim_type` predicates from 0.1 to 1.0), with 44 discrete item models.
* Includes a custom `copper_darker` palette permutation in `assets/minecraft/atlases/blocks.json` so copper trims applied to copper armor maintain high visual contrast, matching vanilla gold-on-gold and iron-on-iron conventions.
* Preserves in-world player 3D entity armor trim rendering.

#### 5.2 Tool Trims Compatibility (Optional Mod Integration - Requires Tool Trims)
* Note: This mod does NOT add standalone tool trimming on its own. It is specifically an integration layer for the [Tool Trims](https://modrinth.com/mod/tool-trims) mod.
* If Tool Trims is installed, all 5 copper tools (Copper Sword, Copper Axe, Copper Pickaxe, Copper Shovel, and Copper Hoe) become trimmable at a Smithing Table.
* Supports all 4 Tool Trims patterns (Linear, Tracks, Charge, Frost) and all 10 trim materials (Amethyst, Copper, Diamond, Emerald, Gold, Iron, Lapis, Netherite, Quartz, Redstone), providing 200 data-driven smithing recipes.
* Reuses Tool Trims colorized palettes based on iron tool silhouettes, avoiding redundant texture assets.
* Z-Fighting Elimination: Trim models utilize Tool Trims layer architecture (layer0 mapped to `tooltrims:item/trim_bases/iron_<tool>_<pattern>` overlay) to eliminate coplanar z-fighting artifacts in first-person and world rendering.
* Copper tools are dynamically enrolled into `#tooltrims:trimmable_tools` as well as standard vanilla tool tags (`#minecraft:swords`, `#minecraft:axes`, etc.).

#### 5.3 High-Priority Built-In Resource Pack
* To ensure copper equipment models with trim overrides are not masked by upstream plain models due to pack load order, models are packaged into an always-enabled built-in resource pack (`copper_trims`).
* Registered via `ResourceManagerHelper.registerBuiltinResourcePack` (ALWAYS_ENABLED) on Fabric and `AddPackFindersEvent` (Pack.Position.TOP) on NeoForge.

### 6. Conditional Modern Copper Golem Spawn Egg & Tint Handling
* Conditionally replaces the classic dotted spawn egg icon with the modern detailed Copper Golem spawn egg texture when [Vanilla Backport](https://modrinth.com/mod/vanilla-backport) is installed.
* Provides clean, native texture rendering across both NeoForge and Fabric loaders without color filter distortions or tint registry bugs.
* Configurable via `config/copper_age_patch.json`:
  * `"AUTO"` (default): Uses the modern texture if Vanilla Backport is installed, classic otherwise.
  * `"MODERN"`: Forces the modern detailed texture.
  * `"CLASSIC"`: Forces the classic vanilla dotted texture.

### 7. In-Game Guide Pages (JEI / EMI / REI)
* Built-in recipe and information pages for Just Enough Items (JEI), EMI, and Roughly Enough Items (REI) covering:
  * Copper Golem creation and antenna items (holding flowers or decorative blocks).
  * Oxidation stages (Unaffected, Exposed, Weathered, Oxidized) and waxing with Honeycomb.
  * De-oxidation mechanics using an axe or lightning strikes via lightning rods.
  * Copper Shelf interactions and storage mechanics.
  * Anvil repair recipes with Copper Ingots.
  * Tool Trims smithing template guides.

### 8. Jade HUD Integration
* Antenna Items: Detects and displays items placed on the Copper Golem antenna (such as the poppy flower gifted by an Iron Golem).
* Weathering & Waxing: Displays the current oxidation stage (Unaffected, Exposed, Weathered, Oxidized) and whether the golem or statue is waxed.
* Held Items: Displays the item currently held by the Copper Golem.
* Shelf Inventories: Provides full visual inventory previews when looking at Copper Shelves via [Jade](https://modrinth.com/mod/jade).

### 9. Create Mod & Tag Interoperability
* Enrolls copper nuggets from all sources into common tags: `#c:nuggets`, `#c:nuggets/copper`, and `#c:copper_nuggets`.
* Bidirectional crafting recipes: 9 copper nuggets craft 1 copper ingot, and 1 copper ingot crafts 9 copper nuggets.
* Adds Crushing Wheel recipes to recycle copper equipment into copper ingots and nuggets with [Create](https://modrinth.com/mod/create).

### 10. Better Combat Integration
* Configures native attack animations, weapon attributes, range, and sweeping hitboxes for copper weapons with [Better Combat](https://modrinth.com/mod/better-combat).

### 11. Multilingual Localization
Full in-game translations for all supported language variants:
* Arabic (`ar_sa`)
* Azerbaijani (`az_az`)
* Chinese Simplified (`zh_cn`)
* Chinese Traditional (`zh_hk`, `zh_tw`)
* Czech (`cs_cz`)
* English (`en_us`, `en_gb`)
* French (`fr_fr`, `fr_ca`)
* German (`de_de`)
* Hungarian (`hu_hu`)
* Italian (`it_it`)
* Japanese (`ja_jp`)
* Korean (`ko_kr`)
* Polish (`pl_pl`)
* Portuguese (`pt_br`, `pt_pt`)
* Russian (`ru_ru`)
* Spanish (`es_es`, `es_mx`, `es_ar`, `es_cl`, `es_ec`, `es_uy`, `es_ve`)
* Thai (`th_th`)
* Turkish (`tr_tr`)
* Vietnamese (`vi_vn`)

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
| [Tool Trims](https://modrinth.com/mod/tool-trims) | NeoForge & Fabric | Optional | Enables smithing trims on copper tools (optional integration) |
| [Vanilla Backport](https://modrinth.com/mod/vanilla-backport) | NeoForge & Fabric | Optional | Modern spawn egg texture trigger |
| [JEI](https://modrinth.com/mod/jei) / [EMI](https://modrinth.com/mod/emi) / [REI](https://modrinth.com/mod/rei) | NeoForge & Fabric | Optional | In-game information and mechanic guide pages |
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
# - build/libs/copper_age_patch-neoforge-1.21.1-1.0.0.jar
# - build/libs/copper_age_patch-fabric-1.21.1-1.0.0.jar
```

---

## License & Attribution

* Original Mod: [Copper Age Backport](https://github.com/Smallinger/Copper-Age-Backport) by Smallinger (CC0).
* Patch: Developed by 1unarea under the [MIT License](LICENSE).
* Clean-room implementation: No proprietary or external code was copied.
