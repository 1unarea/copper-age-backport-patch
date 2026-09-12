# Copper Age Backport Patch

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](https://minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.x-orange.svg)](https://neoforged.net/)

An unofficial patch and compatibility addon for [Copper Age Backport](https://github.com/Smallinger/Copper-Age-Backport) by **Smallinger** on Minecraft 1.21.1 NeoForge.

## Features

* **Registry Crash Fix:** Resolves the duplicate armor material registry crash on NeoForge 21.1.237 and newer, allowing the game to load without rolling back to vanilla.
* **Jade Tooltips:** Displays held items, oxidation stages, and waxed status on Copper Golems and statues, plus visual inventories for shelves.
* **Better Combat Integration:** Configures native attack animations, sweeps, and hitboxes for all copper tools and weapons.
* **Create Mod Recipes:** Adds Crushing Wheel recipes to recycle copper equipment into copper ingots and nuggets.
* **Tags and Translations:** Populates standard NeoForge/Common item tags and provides built-in translations for over 20 languages.

## Requirements

* **Minecraft:** 1.21.1
* **Mod Loader:** NeoForge (21.1.237 or newer)
* **Required Mod:** [Copper Age Backport](https://github.com/Smallinger/Copper-Age-Backport)

Optional integrations: [Jade](https://modrinth.com/mod/jade), [Better Combat](https://modrinth.com/mod/better-combat), [Create](https://modrinth.com/mod/create).

## Building from Source

```bash
git clone https://github.com/aegeada/copper-age-backport-patch.git
cd copper-age-backport-patch
./gradlew build
```

## Credits and License

* Original mod by Smallinger (CC0).
* Patch developed by aegeada under the [MIT License](LICENSE).
