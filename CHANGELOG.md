# Changelog

All notable changes to Copper Age Backport Patch are documented here.

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
