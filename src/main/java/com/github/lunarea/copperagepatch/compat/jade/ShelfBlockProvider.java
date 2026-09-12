package com.github.lunarea.copperagepatch.compat.jade;

import com.github.smallinger.copperagebackport.block.shelf.ShelfBlock;
import com.github.smallinger.copperagebackport.block.shelf.ShelfBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Level;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Jade component provider for ShelfBlock and ShelfBlockEntity.
 * Displays stored items visually with item elements, names, and stack counts.
 */
public enum ShelfBlockProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    public static final String NBT_ITEMS = "Items";

    @Override
    public ResourceLocation getUid() {
        return CopperAgeJadePlugin.SHELF;
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (accessor == null || tag == null) {
            return;
        }
        BlockEntity be = accessor.getBlockEntity();
        if (be instanceof ShelfBlockEntity shelf) {
            NonNullList<ItemStack> items = shelf.getItems();
            if (items != null) {
                Level level = accessor.getLevel() != null ? accessor.getLevel() : be.getLevel();
                net.minecraft.core.HolderLookup.Provider registries = level != null ? level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
                try {
                    ContainerHelper.saveAllItems(tag, items, true, registries);
                } catch (Throwable ignored) {}
            }
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (accessor == null || tooltip == null) {
            return;
        }
        BlockState state = accessor.getBlockState();
        if (state != null && !(state.getBlock() instanceof ShelfBlock)) {
            return;
        }
        BlockEntity be = accessor.getBlockEntity();
        List<ItemStack> storedItems = new ArrayList<>();

        if (be instanceof ShelfBlockEntity shelf) {
            NonNullList<ItemStack> items = shelf.getItems();
            if (items != null) {
                for (ItemStack stack : items) {
                    if (stack != null && !stack.isEmpty()) {
                        storedItems.add(stack);
                    }
                }
            }
        }
        if (storedItems.isEmpty() && accessor.getServerData() != null && accessor.getServerData().contains(NBT_ITEMS)) {
            Level level = accessor.getLevel() != null ? accessor.getLevel() : (be != null ? be.getLevel() : null);
            net.minecraft.core.HolderLookup.Provider registries = level != null ? level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
            try {
                ListTag list = accessor.getServerData().getList(NBT_ITEMS, 10);
                int size = ShelfBlockEntity.MAX_ITEMS;
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag itemTag = list.getCompound(i);
                    int slot = itemTag.getByte("Slot") & 255;
                    if (slot >= size) {
                        size = slot + 1;
                    }
                }
                NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
                ContainerHelper.loadAllItems(accessor.getServerData(), items, registries);
                for (ItemStack stack : items) {
                    if (stack != null && !stack.isEmpty()) {
                        storedItems.add(stack);
                    }
                }
            } catch (Throwable ignored) {}
        }

        if (storedItems.isEmpty()) {
            tooltip.add(Component.translatableWithFallback("tooltip.copper_age_patch.shelf_empty", "Empty")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatableWithFallback("tooltip.copper_age_patch.shelf_items", "Stored Items:")
                    .withStyle(ChatFormatting.GRAY));

            IElementHelper helper = IElementHelper.get();
            List<IElement> visualElements = new ArrayList<>();
            for (ItemStack stack : storedItems) {
                if (helper != null) {
                    IElement el = helper.item(stack);
                    if (el != null) {
                        visualElements.add(el);
                    }
                }
            }
            if (!visualElements.isEmpty()) {
                tooltip.add(visualElements);
            }

            for (ItemStack stack : storedItems) {
                Component name;
                try {
                    name = stack.getHoverName();
                } catch (Throwable ignored) {
                    name = Component.literal("Unknown Item");
                }
                MutableComponent line = Component.literal("• ").withStyle(ChatFormatting.DARK_GRAY)
                        .append(name.copy().withStyle(ChatFormatting.WHITE));
                if (stack.getCount() > 1) {
                    line.append(Component.literal(" x" + stack.getCount()).withStyle(ChatFormatting.GRAY));
                }
                tooltip.add(line);
            }
        }
    }
}
