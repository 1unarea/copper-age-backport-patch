# Copper Age Backport Patch

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](https://minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.x-orange.svg)](https://neoforged.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.15.x-blue.svg)](https://fabricmc.net/)

An unofficial patch and compatibility addon for [Copper Age Backport](https://github.com/Smallinger/Copper-Age-Backport) by **Smallinger** on Minecraft 1.21.1 NeoForge/Fabric.

## Features

* **Registry Crash Fix:** Resolves duplicate armor material registration on NeoForge 21.1.237 and newer (also Fabric 0.15.0 and newer), preventing rollback to vanilla.
* **Jade Tooltips:** Displays held items, antenna items (such as poppy flowers), oxidation stages, and waxed status on Copper Golems and statues, plus shelf storage contents.
* **Better Combat Integration:** Adds attack animations, sweeps, and weapon hitboxes for all copper tools and weapons.
* **Create Mod Recipes & Copper Nugget Interoperability:** Adds Crushing Wheel recipes and bidirectional ingot/nugget crafting interoperability with Create and other mods.
* **Tag Support:** Populates standard NeoForge and Common item tags for armors, tools, and copper nuggets (`c:nuggets`, `c:nuggets/copper`, `c:copper_nuggets`).
* **Armor Durability Fix:** Adds the missing durability feature to the copper armors. (`Copper helmet - 121`, `copper chestplate - 176`, `copper leggings - 165` and `copper boots - 143`)
* **Copper Armor Trim Support:** Copper armor pieces now display smithing trim overlays correctly in both the inventory (2D) and when worn (3D entity model). Includes a built-in `copper_darker` trim palette for when copper material is used as the trim.
* **Tool Trims Compatibility ([Tool Trims](https://modrinth.com/mod/tool-trims)):** Copper tools (sword, axe, pickaxe, shovel, hoe) are now fully trimmable via the Tool Trims mod. All 200 trim combinations (5 tools × 4 patterns × 10 materials) are supported without custom textures — iron tool trim shapes are reused since the silhouettes match pixel-perfectly.
* **Modern Copper Golem Spawn Egg:** Conditionally updates the Copper Golem spawn egg texture to the modern detailed version when [Vanilla Backport](https://modrinth.com/mod/vanilla-backport) is installed.

## Supported Languages

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

## Requirements

* **Minecraft:** 1.21.1
* **Mod Loader:** NeoForge (21.1.237 or newer) / Fabric (0.15.0 or newer)
* **Required Mod:** [Copper Age Backport](https://modrinth.com/mod/copper-age-backport)

Optional integrations: [Jade](https://modrinth.com/mod/jade), [Better Combat](https://modrinth.com/mod/better-combat), [Create](https://modrinth.com/mod/create), [Tool Trims](https://modrinth.com/mod/tool-trims).

## Credits and License

* Original mod by Smallinger (CC0).
* Patch developed by aegeada under the [MIT License](LICENSE).
