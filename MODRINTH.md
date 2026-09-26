# Copper Age Backport Patch

An unofficial patch and compatibility addon for [Copper Age Backport](https://modrinth.com/mod/backport-copper-age) on Minecraft 1.21.1 (NeoForge and Fabric).

---

## Important Loader Note

* **Fabric:** Copper Age Backport runs completely fine on Fabric on its own and does not crash. On Fabric, this mod serves as a gameplay, balance, and mod compatibility addon.
* **NeoForge:** On NeoForge 21.1.237 and newer, Copper Age Backport suffers from a startup registry collision crash. This patch resolves that crash on NeoForge.

---

## Features

* **Lightning Rod Parity (Weathering, Channeling & Redstone)**
  * Full multi-loader parity across both Fabric and NeoForge for all 7 Copper Age Backport lightning rod variants (Exposed, Weathered, Oxidized, Waxed, Waxed Exposed, Waxed Weathered, Waxed Oxidized).
  * Restores natural thunderstorm lightning redirection within 128 blocks by dynamically registering all 168 blockstates of CAB lightning rods into `PoiTypes.TYPE_BY_STATE`.
  * Enables Channeling Tridents to reliably summon lightning strikes on all 8 rod variants during thunderstorms via robust `minecraft:any_of` block state matching (`data/minecraft/enchantment/channeling.json`).
  * Emits an 8-tick redstone pulse and electric spark particles when struck by lightning.
  * Struck unwaxed weathered lightning rods de-oxidize by one stage while waxed variants remain protected. Struck rods redirect de-oxidation to attached copper blocks with realistic random-walk copper cleaning.
  * Integrates with `WeatheringCopper` and `HoneycombItem` BiMaps for full axe scraping and honeycomb waxing support.

* **Farmer's Delight Integration (Copper Knife)**
  * Seamlessly integrates a Copper Knife under the `farmersdelight` namespace (`farmersdelight:copper_knife`) so it appears natively in tooltips, recipe viewers, and tags.
  * Balanced durability of 190 (CAB copper tool tier, naturally positioned between Flint [131] and Iron [250]).
  * Authentic knife balance: 2.5 Attack Damage and 2.0 Attack Speed (`+0.5f` damage bonus, `-2.0f` attack speed modifier), preserving straw/grass harvesting and cutting board slicing.
  * Shaped crafting recipe with 1 copper ingot placed vertically over 1 stick.
  * Exclusively added to the Farmer's Delight creative tab directly after the Flint Knife and before the Iron Knife.

* **Shield Expansion Integration (Copper Shield)**
  * Seamlessly integrates a Copper Shield under the `shieldexp` namespace (`shieldexp:copper_shield`) so it appears natively as a Shield Expansion shield.
  * Balanced durability of 120 (positioned between Wooden Shield [55] and Iron Shield [165]).
  * Custom 3D block model geometry based on Shield Expansion standards, ensuring full 3D rendering in GUI, hands, and blocking across both NeoForge and Fabric.
  * Full Shield Expansion stats: parry mechanics, 25 cooldown ticks, 0.70 speed factor, 5 parry ticks, and 2 stamina.
  * Shaped crafting recipe with 1 stick surrounded by 8 copper ingots.
  * Placed in the Creative Combat tab directly after the Wooden Shield and before the Iron Shield.

* **Armor Trims (Vanilla Smithing Table)**
  * Copper armor pieces (Helmet, Chestplate, Leggings, Boots) can be trimmed at a Smithing Table with vanilla armor trim templates without needing any other mod.
  * Includes a custom high-contrast copper_darker palette for copper trim on copper armor to maintain visual clarity.

* **Pale Oak Shelf Crafting Recipe**
  * Adds the missing shaped crafting recipe for the Pale Oak Shelf (`minecraft:pale_oak_shelf`) using Stripped Pale Oak Logs (`minecraft:stripped_pale_oak_log`), restoring recipe parity with all other wooden shelf types in Copper Age Backport.

* **Tool Trims Mod Compatibility (Optional Integration)**
  * Adds full compatibility with the [Tool Trims](https://modrinth.com/mod/tool-trims) mod.
  * Note: Tool trimming is NOT a standalone feature; it requires the Tool Trims mod to be installed. When installed, all 5 copper tools (Sword, Axe, Pickaxe, Shovel, Hoe) can be trimmed across 4 patterns and 10 materials.

* **Armor Durability Restoration & Anvil Repair**
  * Restores balanced durability values (Helmet: 121, Chestplate: 176, Leggings: 165, Boots: 143) based on tier multiplier 11. Upstream omitted durability, making armor unbreakable.
  * Copper armor and copper tools can now be repaired on an Anvil using Copper Ingots.

* **Creative Inventory Organization**
  * **Combat Tab:** Copper Axe appears in the Creative Combat tab directly after Stone Axe, matching vanilla weapon tier progression. Copper Shield appears directly after Wooden Shield.
  * **Spawn Eggs Tab:** Positions the Copper Golem Spawn Egg directly after the Cod Spawn Egg and before the Cow Spawn Egg, matching official 1.21.9 release order.

* **Modern Copper Golem Spawn Egg & Visual Fixes**
  * Automatically uses the modern detailed spawn egg texture when [Vanilla Backport](https://modrinth.com/mod/vanilla-backport) is installed, or keeps the classic dotted egg when absent.
  * Choose between `AUTO`, `MODERN`, or `CLASSIC` in `config/copper_age_patch.json`.
  * Fixed tint filter distortions on Fabric so the spawn egg renders with accurate colors on all loaders.

* **In-Game Information (JEI / EMI / REI)**
  * Built-in guide pages explaining Copper Golem oxidation stages, waxing, antenna items, lightning rod de-oxidation, shelf storage, and anvil repairs.

* **Jade HUD Tooltips**
  * Displays held items, antenna flowers (e.g. poppy gifted by an Iron Golem), oxidation stages, and waxed status on Copper Golems, plus inventory previews for shelves via [Jade](https://modrinth.com/mod/jade).

* **Create Mod Interoperability**
  * Unifies copper nuggets under standard common tags (`#c:nuggets/copper`, `#c:copper_nuggets`) with bidirectional crafting and Crushing Wheel recycling with [Create](https://modrinth.com/mod/create).

* **Better Combat Integration**
  * Native attack animations, weapon attributes, and sweep hitboxes with [Better Combat](https://modrinth.com/mod/better-combat).

* **Vanilla Copper Translation Restoration & Localization Fixes**
  * Restores authentic vanilla translations across 30 languages for 42 copper blocks and subtitles that were previously overwritten in English by Copper Age Backport.
  * Corrects Turkish typos and unidiomatic terminology in Copper Age Backport.

* **NeoForge Registry Crash Fix**
  * Fixes the duplicate ResourceKey / armor material registration crash on NeoForge 21.1.237+.

* **25+ Supported Languages**
  * Complete, authentic translations across all 25 supported languages for mod items (including Copper Knife and Copper Shield across `farmersdelight` and `shieldexp`), blocks, and subtitles.

---

## Requirements & Compatibility

| Mod | Platform | Requirement | Purpose |
| :--- | :--- | :--- | :--- |
| [Copper Age Backport](https://modrinth.com/mod/backport-copper-age) | NeoForge & Fabric | Required | The base mod being patched |
| [Fabric API](https://modrinth.com/mod/fabric-api) | Fabric | Required | Standard Fabric framework |
| [Farmer's Delight](https://modrinth.com/mod/farmers-delight) | NeoForge & Fabric | Optional | Adds Copper Knife with cutting board support and straw harvesting |
| [Shield Expansion](https://modrinth.com/mod/shield-expansion) | NeoForge & Fabric | Optional | Adds Copper Shield with 3D model, parry, and blocking mechanics |
| [Tool Trims](https://modrinth.com/mod/tool-trims) | NeoForge & Fabric | Optional | Enables smithing trims on copper tools |
| [Vanilla Backport](https://modrinth.com/mod/vanilla-backport) | NeoForge & Fabric | Optional | Modern spawn egg texture trigger |
| [JEI](https://modrinth.com/mod/jei) / [EMI](https://modrinth.com/mod/emi) / [REI](https://modrinth.com/mod/rei) | NeoForge & Fabric | Optional | In-game recipe and mechanic guide pages |
| [Jade](https://modrinth.com/mod/jade) | NeoForge & Fabric | Optional | In-game tooltips for golem and shelves |
| [Better Combat](https://modrinth.com/mod/better-combat) | NeoForge & Fabric | Optional | Combat animations and weapon stats |
| [Create](https://modrinth.com/mod/create) | NeoForge & Fabric | Optional | Crushing recipes and nugget recycling |

---

## Credits and License

* Original Copper Age Backport by Smallinger (CC0).
* Patch developed by 1unarea under the MIT License.
