# Changelog

All notable changes to Copper Age Backport Patch are documented here.

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
