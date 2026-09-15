# Changelog

All notable changes to Copper Age Backport Patch are documented here.

---

## 1.0.0

### Highlights
- Official 1.0.0 Stable Release: Transitioning out of beta with full feature parity across NeoForge and Fabric on Minecraft 1.21.1.
- Complete clean-room architecture: Zero-copy implementation under the MIT license, free of restrictive upstream code or external dependencies.

### Added
- **Canonical Creative Mode Spawn Eggs Placement**: Positioned the Copper Golem Spawn Egg directly after the Cod Spawn Egg and before the Cow Spawn Egg on both NeoForge and Fabric loaders, precisely matching the official Minecraft 1.21.9 reference inventory sequence.
- **JEI / EMI / REI Information Pages**: Added detailed in-game recipe and mechanic guide pages for copper equipment, golem oxidation, waxing, and shelf interactions across all 29 supported language localizations.
- **Anvil Repair Support**: Copper armor and copper tools can now be repaired on an Anvil using Copper Ingots (`#c:ingots/copper`).
- **Full Tool Trims Compatibility**: Seamless smithing trimming support for all 5 copper tools (Sword, Axe, Pickaxe, Shovel, Hoe) across 4 patterns and 10 materials when the Tool Trims mod is installed.
- **Native Copper Armor Trims**: Complete model overrides for all 4 copper armor pieces with 10 standard trim materials and high-contrast copper-on-copper rendering via the `copper_darker` palette.
- **Conditional Modern Spawn Egg Icon**: Automatic detection of Vanilla Backport with client configuration override (`AUTO`, `MODERN`, `CLASSIC`).
- **Jade HUD Integrations**: Real-time display of Copper Golem oxidation levels, waxing status, held items, antenna items (such as poppies gifted by Iron Golems), and visual Copper Shelf inventory previews.
- **Create & Better Combat Integration**: Bidirectional nugget/ingot crafting recipes, Crushing Wheel recycling, and weapon attributes with attack sweeping hitboxes.

### Fixed
- **Fabric Spawn Egg Tint Override**: Fixed reflection handling on Fabric's `ColorProviderRegistry` to resolve dynamic proxy interfaces correctly despite bytecode type erasure, ensuring the modern spawn egg renders without color filter distortions.
- **NeoForge Creative Tab Repositioning**: Resolved tab duplicate constraints by dynamically removing upstream end-of-tab entries and rebinding insertion anchors directly to live parent entries.
- **Copper Armor Durability**: Restored canonical durability values based on the standard multiplier of 11 (Helmet: 121, Chestplate: 176, Leggings: 165, Boots: 143), resolving indestructible armor.
- **Copper Axe in Combat Tab**: Restored missing Copper Axe to the Creative Combat tab directly following the Stone Axe.
- **NeoForge Bootstrap Crash**: Eliminated duplicate `ResourceKey[minecraft:armor_material / minecraft:copper]` registry collision crash on NeoForge 21.1.237+ via thread-safe memoized interceptor.

---

## 0.1.4

### Added
- **Creative Mode Spawn Eggs Tab Placement** — Copper Golem spawn egg is now registered into the Creative Mode Spawn Eggs tab on both NeoForge and Fabric loaders. It is ordered immediately before the Iron Golem spawn egg (and directly after Sulfur Cube spawn egg when Vanilla Backport is present, or after Sniffer spawn egg when absent), matching canonical mob category progression.

### Fixed
- **Spawn Egg Texture Tint Filter Removal** — Removed the dark copper color multiplication filter applied to the modern Copper Golem spawn egg by vanilla Minecraft's `SpawnEggItem` color handler. Modern custom icon textures now render with 100% true pixel fidelity, while retaining classic two-tone template coloration when classic mode is selected.
- **Modern Spawn Egg Icon Fidelity** — Updated modern spawn egg icon to match official 16x16 RGBA asset specification.

---

## 0.1.3

### Added
- **Complete Tool Trims Mod Compatibility for Copper Tools** — Full smithing trimming support for all 5 copper tools (Copper Sword, Axe, Pickaxe, Shovel, Hoe) across all 4 Tool Trims patterns (Linear, Tracks, Charge, Frost) and 10 materials (Amethyst, Copper, Diamond, Emerald, Gold, Iron, Lapis, Netherite, Quartz, Redstone). Provides 200 data-driven smithing recipes, item model overrides, clean trim models, and `#tooltrims:trimmable_tools` tag integration.
- **Copper Armor Item Trim Visual Overrides** — Restored vanilla-consistent GUI/inventory item trim overlays for all copper armor pieces (Helmet, Chestplate, Leggings, Boots) across all 10 materials (trim_type 0.1 to 1.0). Includes high-contrast `copper_darker` palette permutation in `assets/minecraft/atlases/blocks.json` for copper trim on copper armor while preserving in-world player armor entity trim rendering.
- **Conditional Modern Copper Golem Spawn Egg Texture** — Dynamically switches the Copper Golem spawn egg texture to the detailed modern design when `vanillabackport` is installed (or when client config is set to `MODERN`), while retaining the classic two-tone dotted vanilla template egg when absent (or `CLASSIC`). Managed via `copper_age_patch.json` client configuration with `AUTO`, `MODERN`, and `CLASSIC` modes, implemented cleanly via built-in resource pack registration across NeoForge and Fabric without bytecode modification.

---

## 0.1.2

### Fixed
- **Copper Armor Durability** — Copper armor pieces now have correct durability values (Helmet: 121, Chestplate: 176, Leggings: 165, Boots: 143), matching the standard tier multiplier of 11. Previously armor was undamageable due to missing `MAX_DAMAGE` component.
- **Copper Axe in Combat Tab** — Copper Axe now appears in the Creative Mode Combat tab immediately after Stone Axe, matching vanilla axe tier ordering (Wood → Stone → Copper → Iron). Previously it was only visible in the Tools & Utilities tab.
- **Fabric Crash Fix** — Resolved `NoClassDefFoundError` on Fabric due to Intermediary mapping incompatibility in `CopperArmorMaterialMixin`.

### Notes
- The separate `copperagebackport_durability_fix` mod is **no longer needed** — this patch now includes durability fix functionality built-in.
- Both NeoForge and Fabric loaders are supported.

---

## 0.1.1

### Added
- **Cross-mod copper nugget interop** — Copper nuggets from Copper Age Backport, Create, and vanilla now share common item tags (`c:nuggets/copper`, `c:copper_nuggets`), enabling crafting recipes across mods.
- **Jade HUD antenna tooltip** — When an Iron Golem gifts a rose to a Copper Golem, the flower placed on its antenna is now shown in the Jade HUD entity tooltip.

### Fixed
- Fabric crash on load due to duplicate `ResourceKey` registration for copper armor material.

---

## 0.1.0

### Added
- Initial release.
- Mixin-based fix for duplicate `ResourceKey[minecraft:armor_material / minecraft:copper]` registration crash on NeoForge.
- Jade HUD integration: copper golem weathering state, waxing status, and held item display.
- Better Combat weapon attribute support for copper sword.
- Dual-loader build (NeoForge + Fabric) from a single source tree.
