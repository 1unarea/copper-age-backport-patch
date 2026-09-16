# Copper Age Backport Patch

An unofficial patch and compatibility addon for [Copper Age Backport](https://modrinth.com/mod/backport-copper-age) on Minecraft 1.21.1 (NeoForge and Fabric).

---

## Important Loader Note

* **Fabric:** Copper Age Backport runs completely fine on Fabric on its own and does not crash. On Fabric, this mod serves as a gameplay, balance, and mod compatibility addon.
* **NeoForge:** On NeoForge 21.1.237 and newer, Copper Age Backport suffers from a startup registry collision crash. This patch resolves that crash on NeoForge.

---

## Features

* **Pale Oak Shelf Crafting Recipe**
  * Adds the missing shaped crafting recipe for the Pale Oak Shelf (`minecraft:pale_oak_shelf`) using Stripped Pale Oak Logs (`minecraft:stripped_pale_oak_log`), restoring recipe parity with all other wooden shelf types in Copper Age Backport.

* **Armor Trims (Vanilla Smithing Table)**
  * Copper armor pieces (Helmet, Chestplate, Leggings, Boots) can be trimmed with vanilla armor trim templates without needing any other mod.
  * Includes a custom high-contrast copper_darker palette for copper trims on copper armor.

* **Tool Trims Mod Compatibility (Optional Integration)**
  * Adds full compatibility with the [Tool Trims](https://modrinth.com/mod/tool-trims) mod.
  * Note: Tool trimming is NOT a standalone feature; it requires the Tool Trims mod to be installed. When installed, all 5 copper tools (Sword, Axe, Pickaxe, Shovel, Hoe) can be trimmed across 4 patterns and 10 materials.

* **Armor Durability Restoration & Anvil Repair**
  * Restores balanced durability values (Helmet: 121, Chestplate: 176, Leggings: 165, Boots: 143) based on tier multiplier 11. Upstream omitted durability, making armor unbreakable.
  * Copper armor and copper tools can now be repaired on an Anvil using Copper Ingots.

* **Creative Inventory Organization**
  * **Combat Tab:** Copper Axe appears in the Creative Combat tab directly after Stone Axe, matching vanilla weapon tier progression.
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

* **NeoForge Registry Crash Fix**
  * Fixes the duplicate ResourceKey / armor material registration crash on NeoForge 21.1.237+.

* **20+ Supported Languages**
  * Built-in translations for over 20 languages including English, Turkish, German, French, Spanish, Russian, Chinese, Japanese, and Korean.

---

## Requirements & Compatibility

| Mod | Platform | Requirement | Purpose |
| :--- | :--- | :--- | :--- |
| [Copper Age Backport](https://modrinth.com/mod/backport-copper-age) | NeoForge & Fabric | Required | The base mod being patched |
| [Fabric API](https://modrinth.com/mod/fabric-api) | Fabric | Required | Standard Fabric framework |
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
