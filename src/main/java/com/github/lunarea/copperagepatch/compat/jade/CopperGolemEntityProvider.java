package com.github.lunarea.copperagepatch.compat.jade;

import com.github.smallinger.copperagebackport.entity.CopperGolemEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WeatheringCopper;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElementHelper;

import java.lang.reflect.Field;

/**
 * Jade component provider for CopperGolemEntity.
 * Displays held items (icon + name + stack count), weathering state, and waxed status.
 */
public enum CopperGolemEntityProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    INSTANCE;

    public static final String NBT_WAXED = "Waxed";
    public static final String NBT_WEATHER_STATE = "WeatherState";
    public static final String NBT_HELD_ITEM = "HeldItem";

    private static final Field NEXT_WEATHERING_TICK_FIELD;
    static {
        Field f = null;
        try {
            f = CopperGolemEntity.class.getDeclaredField("nextWeatheringTick");
            f.setAccessible(true);
        } catch (Throwable ignored) {}
        NEXT_WEATHERING_TICK_FIELD = f;
    }

    @Override
    public ResourceLocation getUid() {
        return CopperAgeJadePlugin.COPPER_GOLEM;
    }

    @Override
    public void appendServerData(CompoundTag tag, EntityAccessor accessor) {
        if (accessor == null || tag == null) {
            return;
        }
        Entity entity = accessor.getEntity();
        if (entity instanceof CopperGolemEntity golem) {
            boolean isWaxed = isGolemWaxed(golem);
            tag.putBoolean(NBT_WAXED, isWaxed);
            try {
                if (golem.getWeatherState() != null) {
                    tag.putInt(NBT_WEATHER_STATE, golem.getWeatherState().ordinal());
                }
            } catch (Throwable ignored) {}
            Level level = accessor.getLevel() != null ? accessor.getLevel() : golem.level();
            net.minecraft.core.HolderLookup.Provider registries = level != null ? level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
            try {
                ItemStack held = golem.getMainHandItem();
                if (held != null && !held.isEmpty()) {
                    tag.put(NBT_HELD_ITEM, held.saveOptional(registries));
                }
            } catch (Throwable ignored) {}
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        if (accessor == null || tooltip == null) {
            return;
        }
        Entity entity = accessor.getEntity();
        if (!(entity instanceof CopperGolemEntity golem)) {
            return;
        }

        // 1. Held Item Display (Element icon + item name + count)
        ItemStack heldItem = null;
        try {
            heldItem = golem.getMainHandItem();
        } catch (Throwable ignored) {}

        Level level = accessor.getLevel() != null ? accessor.getLevel() : golem.level();
        net.minecraft.core.HolderLookup.Provider registries = level != null ? level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
        if ((heldItem == null || heldItem.isEmpty()) && accessor.getServerData() != null && accessor.getServerData().contains(NBT_HELD_ITEM)) {
            try {
                heldItem = ItemStack.parseOptional(registries, accessor.getServerData().getCompound(NBT_HELD_ITEM));
            } catch (Throwable ignored) {}
        }

        if (heldItem != null && !heldItem.isEmpty()) {
            IElementHelper helper = IElementHelper.get();
            snownee.jade.api.ui.IElement itemElement = helper != null ? helper.item(heldItem) : null;
            Component name;
            try {
                name = heldItem.getHoverName();
            } catch (Throwable ignored) {
                name = Component.literal("Unknown Item");
            }
            MutableComponent itemDesc = Component.empty()
                    .append(name.copy().withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" x" + heldItem.getCount()).withStyle(ChatFormatting.GRAY));
            if (itemElement != null) {
                tooltip.add(itemElement);
                tooltip.append(itemDesc);
            } else {
                tooltip.add(itemDesc);
            }
        }

        // 2. Weathering / Oxidation Stage
        WeatheringCopper.WeatherState weatherState = null;
        try {
            weatherState = golem.getWeatherState();
        } catch (Throwable ignored) {}

        if (weatherState == null && accessor.getServerData() != null && accessor.getServerData().contains(NBT_WEATHER_STATE)) {
            int ordinal = accessor.getServerData().getInt(NBT_WEATHER_STATE);
            WeatheringCopper.WeatherState[] values = WeatheringCopper.WeatherState.values();
            if (ordinal >= 0 && ordinal < values.length) {
                weatherState = values[ordinal];
            }
        }
        tooltip.add(Component.translatableWithFallback("tooltip.copper_age_patch.weathering", "Weathering: ")
                .withStyle(ChatFormatting.GRAY)
                .append(formatWeatherState(weatherState)));

        // 3. Waxed Status
        boolean isWaxed = false;
        if (accessor.getServerData() != null && accessor.getServerData().contains(NBT_WAXED)) {
            isWaxed = accessor.getServerData().getBoolean(NBT_WAXED);
        } else {
            isWaxed = isGolemWaxed(golem);
        }

        if (isWaxed) {
            tooltip.add(Component.translatableWithFallback("tooltip.copper_age_patch.waxed", "Waxed")
                    .withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatableWithFallback("tooltip.copper_age_patch.not_waxed", "Not Waxed")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /**
     * Inspects whether a CopperGolemEntity is waxed.
     * In Copper Age Backport, waxed golems have nextWeatheringTick == -2L.
     */
    public static boolean isGolemWaxed(CopperGolemEntity golem) {
        if (golem == null) {
            return false;
        }
        if (NEXT_WEATHERING_TICK_FIELD != null) {
            try {
                long tick = NEXT_WEATHERING_TICK_FIELD.getLong(golem);
                return tick == -2L;
            } catch (Throwable ignored) {}
        }
        try {
            CompoundTag nbt = new CompoundTag();
            golem.addAdditionalSaveData(nbt);
            if (nbt.contains("next_weather_age")) {
                return nbt.getLong("next_weather_age") == -2L;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    /**
     * Formats weathering state with clean, identifiable styling.
     */
    public static Component formatWeatherState(WeatheringCopper.WeatherState state) {
        if (state == null) {
            state = WeatheringCopper.WeatherState.UNAFFECTED;
        }
        return switch (state) {
            case UNAFFECTED -> Component.translatableWithFallback("tooltip.copper_age_patch.weathering.unaffected", "Unoxidized")
                    .withStyle(ChatFormatting.GREEN);
            case EXPOSED -> Component.translatableWithFallback("tooltip.copper_age_patch.weathering.exposed", "Exposed")
                    .withStyle(ChatFormatting.YELLOW);
            case WEATHERED -> Component.translatableWithFallback("tooltip.copper_age_patch.weathering.weathered", "Weathered")
                    .withStyle(ChatFormatting.AQUA);
            case OXIDIZED -> Component.translatableWithFallback("tooltip.copper_age_patch.weathering.oxidized", "Oxidized")
                    .withStyle(ChatFormatting.DARK_AQUA);
        };
    }
}
