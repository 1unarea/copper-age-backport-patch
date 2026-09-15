# Copper Age Backport Patch

An essential bugfix, visual overhaul, and compatibility patch for [Copper Age Backport](https://modrinth.com/mod/backport-copper-age) on Minecraft 1.21.1 (NeoForge & Fabric).

---

## Important Loader Information

* **Fabric**: Copper Age Backport works completely fine standalone and does not crash on Fabric. On Fabric, this mod serves as a balance, visual, and compatibility enhancement.
* **NeoForge**: Copper Age Backport crashes on modern NeoForge (21.1.237+) during startup due to duplicate armor material registry keys. This patch fixes that crash completely.

---

## Features

### Crash Fix & Canonical Durability
* **NeoForge Startup Crash Fix**: Fixes duplicate armor material registration on NeoForge 21.1.237+.
* **Armor Durability Restored**: Upstream copper armor has infinite durability. This patch restores balanced, vanilla-aligned durability values based on multiplier 11 (Helmet: 121, Chestplate: 176, Leggings: 165, Boots: 143).
* **Anvil Repair**: Copper armor and copper tools can now be repaired on an Anvil using Copper Ingots.

### Creative Inventory Organization
* **Combat Tab**: Adds the Copper Axe to the Combat creative tab right after the Stone Axe, matching vanilla tier progression.
* **Spawn Eggs Tab**: Positions the Copper Golem Spawn Egg directly after the Cod Spawn Egg and before the Cow Spawn Egg, matching official 1.21.9 release order.

### Trims: Armor vs. Tools
* **Copper Armor Trims (Native Vanilla Feature)**: Fully supports all 10 vanilla trim materials at the Smithing Table out of the box. No other mods required. Includes high-contrast darker copper trim on copper armor.
* **Tool Trims Compatibility (Optional Integration)**: If the [Tool Trims](https://modrinth.com/mod/tool-trims) mod is installed, all 5 copper tools (Sword, Axe, Pickaxe, Shovel, Hoe) can also be trimmed with all 4 patterns and 10 materials.

### Visuals & Spawn Egg Icon
* **Modern Copper Golem Spawn Egg**: Automatically switches to the modern detailed egg texture if [Vanilla Backport](https://modrinth.com/mod/vanilla-backport) is installed.
* **Clean Rendering**: Fixed tint filter distortions on Fabric so the spawn egg renders with accurate colors on all loaders.
* **Configurable**: Choose between AUTO, MODERN, or CLASSIC in `config/copper_age_patch.json`.

### In-Game Information (JEI / EMI / REI)
* Built-in guide pages explaining Copper Golem oxidation stages, waxing, antenna items, lightning rod de-oxidation, copper shelf storage, and anvil repairs.

### Mod Integrations
* **Jade HUD**: Shows Copper Golem oxidation stage, waxed status, held items, antenna items (such as poppies from Iron Golems), and visual previews of Copper Shelf containers.
* **Create**: Adds copper equipment crushing wheel recipes and bidirectional copper nugget/ingot crafting recipes.
* **Better Combat**: Native weapon attributes, attack animations, and sweeping hitboxes for copper weapons.

---

## Dependencies & Recommendations

* **Required**: [Copper Age Backport](https://modrinth.com/mod/backport-copper-age)
* **Required (Fabric only)**: [Fabric API](https://modrinth.com/mod/fabric-api)
* **Optional Integrations**:
  * [Tool Trims](https://modrinth.com/mod/tool-trims) (enables trimming copper tools)
  * [Vanilla Backport](https://modrinth.com/mod/vanilla-backport) (modern spawn egg texture)
  * [JEI](https://modrinth.com/mod/jei) / [EMI](https://modrinth.com/mod/emi) / [REI](https://modrinth.com/mod/rei) (in-game guide pages)
  * [Jade](https://modrinth.com/mod/jade) (HUD tooltips)
  * [Create](https://modrinth.com/mod/create) (recycling recipes)
  * [Better Combat](https://modrinth.com/mod/better-combat) (combat animations)
