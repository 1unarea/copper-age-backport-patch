package com.github.lunarea.copperagepatch.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * JEI / EMI / REI compatibility plugin for Copper Age Backport Patch.
 *
 * Provides comprehensive in-game info pages covering:
 * - Copper Golem construction, weathering stages, waxing/scraping, and interactions
 * - Copper Golem Statue posing and waxing
 * - Copper Shelves storage and redstone comparator output
 * - Copper Armor vanilla smithing trims, durability, and anvil repair
 * - Copper Tools trimming with Tool Trims mod, stats, and anvil repair
 * - Copper Chests weathering mechanics
 * - Copper Nugget crafting and Create mod Crushing Wheel recycling
 * - Copper Horse Armor dungeon loot and stats
 *
 * Supported automatically by JEI, EMI (via JEI plugin adapter), and REI.
 */
@JeiPlugin
public class CopperAgeJeiPlugin implements IModPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(CopperAgeJeiPlugin.class);

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("copper_age_patch", "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        LOGGER.info("[CopperAgeBackportPatch] Registering JEI/EMI/REI information pages...");

        // 1. Copper Golem
        registerInfo(registration, "gui.copper_age_patch.jei.info.copper_golem",
                "minecraft:copper_golem_spawn_egg",
                "copperagebackport:copper_golem_spawn_egg",
                "minecraft:copper_block",
                "minecraft:exposed_copper",
                "minecraft:weathered_copper",
                "minecraft:oxidized_copper",
                "minecraft:waxed_copper_block",
                "minecraft:carved_pumpkin",
                "minecraft:lightning_rod"
        );

        // 2. Copper Golem Statue
        registerInfo(registration, "gui.copper_age_patch.jei.info.copper_golem_statue",
                "minecraft:copper_golem_statue",
                "copperagebackport:copper_golem_statue",
                "minecraft:exposed_copper_golem_statue",
                "copperagebackport:exposed_copper_golem_statue",
                "minecraft:weathered_copper_golem_statue",
                "copperagebackport:weathered_copper_golem_statue",
                "minecraft:oxidized_copper_golem_statue",
                "copperagebackport:oxidized_copper_golem_statue",
                "minecraft:waxed_copper_golem_statue",
                "copperagebackport:waxed_copper_golem_statue",
                "minecraft:waxed_exposed_copper_golem_statue",
                "copperagebackport:waxed_exposed_copper_golem_statue",
                "minecraft:waxed_weathered_copper_golem_statue",
                "copperagebackport:waxed_weathered_copper_golem_statue",
                "minecraft:waxed_oxidized_copper_golem_statue",
                "copperagebackport:waxed_oxidized_copper_golem_statue"
        );

        // 3. Copper Shelves
        registerInfo(registration, "gui.copper_age_patch.jei.info.shelf",
                "minecraft:oak_shelf", "copperagebackport:oak_shelf",
                "minecraft:spruce_shelf", "copperagebackport:spruce_shelf",
                "minecraft:birch_shelf", "copperagebackport:birch_shelf",
                "minecraft:jungle_shelf", "copperagebackport:jungle_shelf",
                "minecraft:acacia_shelf", "copperagebackport:acacia_shelf",
                "minecraft:dark_oak_shelf", "copperagebackport:dark_oak_shelf",
                "minecraft:mangrove_shelf", "copperagebackport:mangrove_shelf",
                "minecraft:cherry_shelf", "copperagebackport:cherry_shelf",
                "minecraft:bamboo_shelf", "copperagebackport:bamboo_shelf",
                "minecraft:crimson_shelf", "copperagebackport:crimson_shelf",
                "minecraft:warped_shelf", "copperagebackport:warped_shelf",
                "minecraft:pale_oak_shelf", "copperagebackport:pale_oak_shelf"
        );

        // 4. Copper Armor
        registerInfo(registration, "gui.copper_age_patch.jei.info.copper_armor",
                "minecraft:copper_helmet", "copperagebackport:copper_helmet",
                "minecraft:copper_chestplate", "copperagebackport:copper_chestplate",
                "minecraft:copper_leggings", "copperagebackport:copper_leggings",
                "minecraft:copper_boots", "copperagebackport:copper_boots"
        );

        // 5. Copper Tools
        registerInfo(registration, "gui.copper_age_patch.jei.info.copper_tools",
                "minecraft:copper_sword", "copperagebackport:copper_sword",
                "minecraft:copper_axe", "copperagebackport:copper_axe",
                "minecraft:copper_pickaxe", "copperagebackport:copper_pickaxe",
                "minecraft:copper_shovel", "copperagebackport:copper_shovel",
                "minecraft:copper_hoe", "copperagebackport:copper_hoe"
        );

        // 6. Copper Chest
        registerInfo(registration, "gui.copper_age_patch.jei.info.copper_chest",
                "minecraft:copper_chest", "copperagebackport:copper_chest",
                "minecraft:exposed_copper_chest", "copperagebackport:exposed_copper_chest",
                "minecraft:weathered_copper_chest", "copperagebackport:weathered_copper_chest",
                "minecraft:oxidized_copper_chest", "copperagebackport:oxidized_copper_chest",
                "minecraft:waxed_copper_chest", "copperagebackport:waxed_copper_chest",
                "minecraft:waxed_exposed_copper_chest", "copperagebackport:waxed_exposed_copper_chest",
                "minecraft:waxed_weathered_copper_chest", "copperagebackport:waxed_weathered_copper_chest",
                "minecraft:waxed_oxidized_copper_chest", "copperagebackport:waxed_oxidized_copper_chest"
        );

        // 7. Copper Recycling & Nuggets
        registerInfo(registration, "gui.copper_age_patch.jei.info.copper_recycling",
                "minecraft:copper_nugget", "copperagebackport:copper_nugget"
        );

        // 8. Copper Horse Armor
        registerInfo(registration, "gui.copper_age_patch.jei.info.copper_horse_armor",
                "minecraft:copper_horse_armor", "copperagebackport:copper_horse_armor"
        );
    }

    private void registerInfo(IRecipeRegistration registration, String translationKey, String... itemIds) {
        Set<Item> resolvedItems = new LinkedHashSet<>();
        for (String idStr : itemIds) {
            ResourceLocation id = ResourceLocation.parse(idStr);
            if (BuiltInRegistries.ITEM.containsKey(id)) {
                Item item = BuiltInRegistries.ITEM.get(id);
                if (item != null && item != Items.AIR) {
                    resolvedItems.add(item);
                }
            }
        }

        if (resolvedItems.isEmpty()) {
            return;
        }

        List<ItemStack> stacks = new ArrayList<>(resolvedItems.size());
        for (Item item : resolvedItems) {
            stacks.add(new ItemStack(item));
        }

        registration.addIngredientInfo(stacks, VanillaTypes.ITEM_STACK, Component.translatable(translationKey));
    }
}
