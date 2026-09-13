package com.github.lunarea.copperagepatch.compat.jade;

import com.github.smallinger.copperagebackport.block.CopperGolemStatueBlock;
import com.github.smallinger.copperagebackport.block.WaxedCopperGolemStatueBlock;
import com.github.smallinger.copperagebackport.block.entity.CopperGolemStatueBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.WeatheringCopper;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.lang.reflect.Field;

/**
 * Jade component provider for CopperGolemStatueBlock and CopperGolemStatueBlockEntity.
 * Displays weathering state, waxed status, and custom name if present.
 */
public enum CopperGolemStatueBlockProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    public static final String NBT_CUSTOM_NAME = "CustomName";

    private static final Field STATUE_CUSTOM_NAME_FIELD;
    static {
        Field f = null;
        try {
            f = CopperGolemStatueBlockEntity.class.getDeclaredField("customName");
            f.setAccessible(true);
        } catch (Throwable ignored) {}
        STATUE_CUSTOM_NAME_FIELD = f;
    }

    @Override
    public ResourceLocation getUid() {
        return CopperAgeJadePlugin.COPPER_GOLEM_STATUE;
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (accessor == null || tag == null) {
            return;
        }
        BlockEntity be = accessor.getBlockEntity();
        if (be instanceof CopperGolemStatueBlockEntity statueBE) {
            Component customName = extractCustomName(statueBE, accessor);
            if (customName != null && !customName.getString().isBlank()) {
                net.minecraft.core.HolderLookup.Provider registries = net.minecraft.core.RegistryAccess.EMPTY;
                try {
                    Level level = accessor.getLevel() != null ? accessor.getLevel() : be.getLevel();
                    if (level != null && level.registryAccess() != null) {
                        registries = level.registryAccess();
                    }
                } catch (Throwable ignored) {}
                try {
                    String json = Component.Serializer.toJson(customName, registries);
                    tag.putString(NBT_CUSTOM_NAME, json);
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
        if (state == null) {
            return;
        }
        Block block = state.getBlock();
        if (!(block instanceof CopperGolemStatueBlock statueBlock)) {
            return;
        }

        // 1. Weathering Stage
        WeatheringCopper.WeatherState weather = statueBlock.getWeatheringState();
        tooltip.add(Component.translatableWithFallback("tooltip.copper_age_patch.weathering", "Weathering: ")
                .withStyle(ChatFormatting.GRAY)
                .append(CopperGolemEntityProvider.formatWeatherState(weather)));

        // 2. Waxed Status
        boolean isWaxed = (statueBlock instanceof WaxedCopperGolemStatueBlock);
        if (isWaxed) {
            tooltip.add(Component.translatableWithFallback("tooltip.copper_age_patch.waxed", "Waxed")
                    .withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatableWithFallback("tooltip.copper_age_patch.not_waxed", "Not Waxed")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        // 3. Custom Name (if named)
        Component customName = null;
        BlockEntity be = accessor.getBlockEntity();
        if (be instanceof CopperGolemStatueBlockEntity statueBE) {
            customName = extractCustomName(statueBE, accessor);
        }
        if (customName == null && accessor.getServerData() != null && accessor.getServerData().contains(NBT_CUSTOM_NAME)) {
            net.minecraft.core.HolderLookup.Provider registries = net.minecraft.core.RegistryAccess.EMPTY;
            try {
                Level level = accessor.getLevel() != null ? accessor.getLevel() : (be != null ? be.getLevel() : null);
                if (level != null && level.registryAccess() != null) {
                    registries = level.registryAccess();
                }
            } catch (Throwable ignored) {}
            try {
                String json = accessor.getServerData().getString(NBT_CUSTOM_NAME);
                customName = Component.Serializer.fromJson(json, registries);
            } catch (Throwable ignored) {}
        }

        if (customName != null && !customName.getString().isBlank()) {
            tooltip.add(Component.translatableWithFallback("tooltip.copper_age_patch.statue_name", "Name: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(customName.copy().withStyle(ChatFormatting.WHITE)));
        }
    }

    /**
     * Extracts custom name from a CopperGolemStatueBlockEntity.
     */
    public static Component extractCustomName(CopperGolemStatueBlockEntity statueBE, BlockAccessor accessor) {
        if (statueBE == null) {
            return null;
        }
        // 1. DataComponents (1.21+)
        try {
            if (statueBE.components() != null) {
                Component name = statueBE.components().get(DataComponents.CUSTOM_NAME);
                if (name != null) {
                    return name;
                }
            }
        } catch (Throwable ignored) {}

        // 2. Direct field reflection
        if (STATUE_CUSTOM_NAME_FIELD != null) {
            try {
                Object val = STATUE_CUSTOM_NAME_FIELD.get(statueBE);
                if (val instanceof Component comp) {
                    return comp;
                }
            } catch (Throwable ignored) {}
        }

        return null;
    }
}
