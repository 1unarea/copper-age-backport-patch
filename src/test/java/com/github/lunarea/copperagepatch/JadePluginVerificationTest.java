package com.github.lunarea.copperagepatch;

import com.github.lunarea.copperagepatch.compat.jade.CopperAgeJadePlugin;
import com.github.lunarea.copperagepatch.compat.jade.CopperGolemEntityProvider;
import com.github.lunarea.copperagepatch.compat.jade.CopperGolemStatueBlockProvider;
import com.github.lunarea.copperagepatch.compat.jade.ShelfBlockProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.WeatheringCopper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.github.smallinger.copperagebackport.block.CopperGolemStatueBlock;
import com.github.smallinger.copperagebackport.block.WaxedCopperGolemStatueBlock;
import com.github.smallinger.copperagebackport.block.WeatheringCopperGolemStatueBlock;
import com.github.smallinger.copperagebackport.entity.CopperGolemEntity;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JadePluginVerificationTest {

    static {
        try {
            net.minecraft.SharedConstants.tryDetectVersion();
        } catch (Throwable ignored) {}
        try {
            java.lang.reflect.Field f = net.minecraft.server.Bootstrap.class.getDeclaredField("isBootstrapped");
            f.setAccessible(true);
            f.setBoolean(null, true);
        } catch (Throwable ignored) {}
    }

    @Test
    @DisplayName("Verify CopperAgeJadePlugin is annotated with @WailaPlugin and implements IWailaPlugin")
    void testPluginClassAnnotationsAndInterfaces() {
        Class<CopperAgeJadePlugin> clazz = CopperAgeJadePlugin.class;

        // Verify @WailaPlugin annotation is present on the class
        assertTrue(clazz.isAnnotationPresent(WailaPlugin.class),
                "CopperAgeJadePlugin must be annotated with @WailaPlugin");

        // Verify implements IWailaPlugin
        assertTrue(IWailaPlugin.class.isAssignableFrom(clazz),
                "CopperAgeJadePlugin must implement snownee.jade.api.IWailaPlugin");

        // Verify ID constant
        assertEquals("copper_age_patch:jade", CopperAgeJadePlugin.ID);
    }

    @Test
    @DisplayName("Verify Jade registration IDs are valid, non-null, and use copper_age_patch namespace")
    void testRegistrationIDs() {
        ResourceLocation golemUid = CopperAgeJadePlugin.COPPER_GOLEM;
        ResourceLocation statueUid = CopperAgeJadePlugin.COPPER_GOLEM_STATUE;
        ResourceLocation shelfUid = CopperAgeJadePlugin.SHELF;

        assertNotNull(golemUid, "COPPER_GOLEM UID must not be null");
        assertNotNull(statueUid, "COPPER_GOLEM_STATUE UID must not be null");
        assertNotNull(shelfUid, "SHELF UID must not be null");

        assertEquals("copper_age_patch", golemUid.getNamespace());
        assertEquals("copper_golem", golemUid.getPath());

        assertEquals("copper_age_patch", statueUid.getNamespace());
        assertEquals("copper_golem_statue", statueUid.getPath());

        assertEquals("copper_age_patch", shelfUid.getNamespace());
        assertEquals("shelf", shelfUid.getPath());

        // Verify providers return identical UIDs
        assertEquals(golemUid, CopperGolemEntityProvider.INSTANCE.getUid());
        assertEquals(statueUid, CopperGolemStatueBlockProvider.INSTANCE.getUid());
        assertEquals(shelfUid, ShelfBlockProvider.INSTANCE.getUid());
    }

    @Test
    @DisplayName("Verify providers implement correct Jade interfaces")
    void testProviderInterfaces() {
        // Golem provider
        assertTrue(CopperGolemEntityProvider.INSTANCE instanceof IEntityComponentProvider,
                "CopperGolemEntityProvider must implement IEntityComponentProvider");
        assertTrue(CopperGolemEntityProvider.INSTANCE instanceof IServerDataProvider<?>,
                "CopperGolemEntityProvider must implement IServerDataProvider");

        // Statue provider
        assertTrue(CopperGolemStatueBlockProvider.INSTANCE instanceof IBlockComponentProvider,
                "CopperGolemStatueBlockProvider must implement IBlockComponentProvider");
        assertTrue(CopperGolemStatueBlockProvider.INSTANCE instanceof IServerDataProvider<?>,
                "CopperGolemStatueBlockProvider must implement IServerDataProvider");

        // Shelf provider
        assertTrue(ShelfBlockProvider.INSTANCE instanceof IBlockComponentProvider,
                "ShelfBlockProvider must implement IBlockComponentProvider");
        assertTrue(ShelfBlockProvider.INSTANCE instanceof IServerDataProvider<?>,
                "ShelfBlockProvider must implement IServerDataProvider");
    }

    @Test
    @DisplayName("Verify weathering stage formatting cleanly formats all 4 stages and handles null")
    void testWeatheringFormatting() {
        Component unaffected = CopperGolemEntityProvider.formatWeatherState(WeatheringCopper.WeatherState.UNAFFECTED);
        assertNotNull(unaffected);
        assertEquals("Unoxidized", unaffected.getString());
        assertEquals(net.minecraft.network.chat.TextColor.fromLegacyFormat(ChatFormatting.GREEN), unaffected.getStyle().getColor());

        Component exposed = CopperGolemEntityProvider.formatWeatherState(WeatheringCopper.WeatherState.EXPOSED);
        assertNotNull(exposed);
        assertEquals("Exposed", exposed.getString());
        assertEquals(net.minecraft.network.chat.TextColor.fromLegacyFormat(ChatFormatting.YELLOW), exposed.getStyle().getColor());

        Component weathered = CopperGolemEntityProvider.formatWeatherState(WeatheringCopper.WeatherState.WEATHERED);
        assertNotNull(weathered);
        assertEquals("Weathered", weathered.getString());
        assertEquals(net.minecraft.network.chat.TextColor.fromLegacyFormat(ChatFormatting.AQUA), weathered.getStyle().getColor());

        Component oxidized = CopperGolemEntityProvider.formatWeatherState(WeatheringCopper.WeatherState.OXIDIZED);
        assertNotNull(oxidized);
        assertEquals("Oxidized", oxidized.getString());
        assertEquals(net.minecraft.network.chat.TextColor.fromLegacyFormat(ChatFormatting.DARK_AQUA), oxidized.getStyle().getColor());

        // Null safety
        Component nullState = CopperGolemEntityProvider.formatWeatherState(null);
        assertNotNull(nullState);
        assertEquals("Unoxidized", nullState.getString());
    }

    @Test
    @DisplayName("Verify register and registerClient register all components and data providers")
    void testRegistrationInvocations() {
        CopperAgeJadePlugin plugin = new CopperAgeJadePlugin();

        List<String> registeredCommon = new ArrayList<>();
        IWailaCommonRegistration commonRegistration = (IWailaCommonRegistration) Proxy.newProxyInstance(
                IWailaCommonRegistration.class.getClassLoader(),
                new Class<?>[]{IWailaCommonRegistration.class},
                (proxy, method, args) -> {
                    registeredCommon.add(method.getName() + ":" + (args.length > 1 ? args[1] : args[0]));
                    return null;
                }
        );

        List<String> registeredClient = new ArrayList<>();
        IWailaClientRegistration clientRegistration = (IWailaClientRegistration) Proxy.newProxyInstance(
                IWailaClientRegistration.class.getClassLoader(),
                new Class<?>[]{IWailaClientRegistration.class},
                (proxy, method, args) -> {
                    registeredClient.add(method.getName() + ":" + (args.length > 1 ? args[1] : args[0]));
                    return null;
                }
        );

        assertDoesNotThrow(() -> plugin.register(commonRegistration));
        assertDoesNotThrow(() -> plugin.registerClient(clientRegistration));

        // Verify common registration registered entity and block providers
        assertTrue(registeredCommon.stream().anyMatch(s -> s.startsWith("registerEntityDataProvider")),
                "Common registration must include registerEntityDataProvider");
        assertTrue(registeredCommon.stream().anyMatch(s -> s.startsWith("registerBlockDataProvider")),
                "Common registration must include registerBlockDataProvider");

        // Verify client registration registered entity and block components
        assertTrue(registeredClient.stream().anyMatch(s -> s.startsWith("registerEntityComponent")),
                "Client registration must include registerEntityComponent");
        assertTrue(registeredClient.stream().anyMatch(s -> s.startsWith("registerBlockComponent")),
                "Client registration must include registerBlockComponent");
    }

    static class TestTooltip implements ITooltip {
        final List<Component> components = new ArrayList<>();
        final List<snownee.jade.api.ui.IElement> elements = new ArrayList<>();

        @Override public void clear() { components.clear(); elements.clear(); }
        @Override public int size() { return components.size(); }
        @Override public boolean isEmpty() { return components.isEmpty() && elements.isEmpty(); }
        @Override public void add(Component component) { components.add(component); }
        @Override public void add(snownee.jade.api.ui.IElement element) { elements.add(element); }
        @Override public void add(List<snownee.jade.api.ui.IElement> elementList) { elements.addAll(elementList); }
        @Override public void append(Component component) { components.add(component); }
        @Override public void append(snownee.jade.api.ui.IElement element) { elements.add(element); }
        @Override public void append(int i, List<snownee.jade.api.ui.IElement> elementList) { elements.addAll(elementList); }
        @Override public void add(int i, snownee.jade.api.ui.IElement element) { elements.add(i, element); }
        @Override public void append(int i, snownee.jade.api.ui.IElement element) { elements.add(i, element); }
        @Override public boolean remove(ResourceLocation id) { return false; }
        @Override public boolean replace(ResourceLocation id, java.util.function.UnaryOperator<List<List<snownee.jade.api.ui.IElement>>> op) { return false; }
        @Override public boolean replace(ResourceLocation id, Component component) { return false; }
        @Override public List<snownee.jade.api.ui.IElement> get(ResourceLocation id) { return List.of(); }
        @Override public List<snownee.jade.api.ui.IElement> get(int i, snownee.jade.api.ui.IElement.Align align) { return List.of(); }
        @Override public String getMessage() { return ""; }
        @Override public String getMessage(ResourceLocation id) { return ""; }
        @Override public void setLineMargin(int i, snownee.jade.api.ui.ScreenDirection dir, int m) {}

        public String getJoinedText() {
            StringBuilder sb = new StringBuilder();
            for (Component c : components) {
                sb.append(c.getString()).append(" ");
            }
            return sb.toString().trim();
        }
    }

    @Test
    @DisplayName("Verify CopperGolemEntityProvider tooltip logic handles server data and edge cases")
    void testGolemTooltipWithServerData() {
        TestTooltip tooltip = new TestTooltip();

        net.minecraft.nbt.CompoundTag serverData = new net.minecraft.nbt.CompoundTag();
        serverData.putBoolean("Waxed", true);
        serverData.putInt("WeatherState", WeatheringCopper.WeatherState.OXIDIZED.ordinal());

        // 1. Non-CopperGolemEntity (null entity) produces empty tooltip
        snownee.jade.api.EntityAccessor nonGolemAccessor = (snownee.jade.api.EntityAccessor) Proxy.newProxyInstance(
                snownee.jade.api.EntityAccessor.class.getClassLoader(),
                new Class<?>[]{snownee.jade.api.EntityAccessor.class},
                (proxy, method, args) -> {
                    if ("getServerData".equals(method.getName())) return serverData;
                    if ("getEntity".equals(method.getName())) return null;
                    return null;
                }
        );
        CopperGolemEntityProvider.INSTANCE.appendTooltip(tooltip, nonGolemAccessor, null);
        assertTrue(tooltip.isEmpty(), "Null entity must produce empty tooltip");

        // 2. Null accessor / tooltip robustness
        assertDoesNotThrow(() -> CopperGolemEntityProvider.INSTANCE.appendTooltip(null, nonGolemAccessor, null));
        assertDoesNotThrow(() -> CopperGolemEntityProvider.INSTANCE.appendTooltip(tooltip, null, null));
        assertDoesNotThrow(() -> CopperGolemEntityProvider.INSTANCE.appendServerData(null, null));
        assertDoesNotThrow(() -> CopperGolemEntityProvider.INSTANCE.appendServerData(new net.minecraft.nbt.CompoundTag(), null));

        // 3. Non-golem server data does not add unexpected keys
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        CopperGolemEntityProvider.INSTANCE.appendServerData(tag, nonGolemAccessor);
        assertTrue(tag.isEmpty(), "Non-golem entity must not write to server tag");
    }

    @Test
    @DisplayName("Verify CopperGolemEntityProvider waxing detection and reflection metadata")
    void testGolemWaxingDetection() throws Exception {
        // Verify CopperGolemEntity declares nextWeatheringTick long field
        java.lang.reflect.Field nextWeatheringTick = com.github.smallinger.copperagebackport.entity.CopperGolemEntity.class.getDeclaredField("nextWeatheringTick");
        assertNotNull(nextWeatheringTick, "CopperGolemEntity must have nextWeatheringTick field");
        assertEquals(long.class, nextWeatheringTick.getType(), "nextWeatheringTick field must be long");
        assertTrue(java.lang.reflect.Modifier.isPrivate(nextWeatheringTick.getModifiers()), "nextWeatheringTick must be private");

        // Verify CopperGolemEntity declares getWeatherState method
        java.lang.reflect.Method getWeatherState = com.github.smallinger.copperagebackport.entity.CopperGolemEntity.class.getDeclaredMethod("getWeatherState");
        assertNotNull(getWeatherState, "CopperGolemEntity must have getWeatherState method");
        assertEquals(WeatheringCopper.WeatherState.class, getWeatherState.getReturnType(), "getWeatherState must return WeatherState");

        // Verify null golem check
        assertFalse(CopperGolemEntityProvider.isGolemWaxed(null), "Null golem must safely return false");

        // Verify allocated golem check for waxed (-1L) and unwaxed (> 0)
        CopperGolemEntity waxedGolem = createMockGolemEntity(null, true);
        assertTrue(CopperGolemEntityProvider.isGolemWaxed(waxedGolem), "Golem with nextWeatheringTick = -1L must be detected as waxed");

        CopperGolemEntity unwaxedGolem = createMockGolemEntity(null, false);
        assertFalse(CopperGolemEntityProvider.isGolemWaxed(unwaxedGolem), "Golem with nextWeatheringTick > 0 must be detected as unwaxed");

        // Verify NBT constants
        assertEquals("Waxed", CopperGolemEntityProvider.NBT_WAXED);
        assertEquals("WeatherState", CopperGolemEntityProvider.NBT_WEATHER_STATE);
        assertEquals("HeldItem", CopperGolemEntityProvider.NBT_HELD_ITEM);
        assertEquals("AntennaItem", CopperGolemEntityProvider.NBT_ANTENNA_ITEM);
        assertEquals(net.minecraft.world.item.ItemStack.EMPTY, CopperGolemEntityProvider.getAntennaItem(null));
    }

    @Test
    @DisplayName("Verify Statue and Shelf tooltip logic handles empty and invalid state gracefully")
    void testStatueAndShelfEmptyState() {
        TestTooltip tooltip = new TestTooltip();

        snownee.jade.api.BlockAccessor blockAccessor = (snownee.jade.api.BlockAccessor) Proxy.newProxyInstance(
                snownee.jade.api.BlockAccessor.class.getClassLoader(),
                new Class<?>[]{snownee.jade.api.BlockAccessor.class},
                (proxy, method, args) -> {
                    if ("getServerData".equals(method.getName())) return new net.minecraft.nbt.CompoundTag();
                    if ("getBlockState".equals(method.getName())) return null;
                    if ("getBlockEntity".equals(method.getName())) return null;
                    return null;
                }
        );

        assertDoesNotThrow(() -> CopperGolemStatueBlockProvider.INSTANCE.appendTooltip(tooltip, blockAccessor, null));
        assertDoesNotThrow(() -> ShelfBlockProvider.INSTANCE.appendTooltip(tooltip, blockAccessor, null));

        // Shelf with empty server data produces "Empty"
        assertTrue(tooltip.getJoinedText().contains("Empty"), "Empty shelf must produce 'Empty' message");
    }

    @Test
    @DisplayName("Verify ShelfBlockProvider null level safety when serverData contains items")
    void testShelfTooltipNullLevelSafetyWithServerData() {
        TestTooltip tooltip = new TestTooltip();
        net.minecraft.nbt.CompoundTag serverData = new net.minecraft.nbt.CompoundTag();
        serverData.put(ShelfBlockProvider.NBT_ITEMS, new net.minecraft.nbt.ListTag());

        snownee.jade.api.BlockAccessor nullLevelAccessor = (snownee.jade.api.BlockAccessor) Proxy.newProxyInstance(
                snownee.jade.api.BlockAccessor.class.getClassLoader(),
                new Class<?>[]{snownee.jade.api.BlockAccessor.class},
                (proxy, method, args) -> {
                    if ("getServerData".equals(method.getName())) return serverData;
                    if ("getBlockEntity".equals(method.getName())) return null;
                    if ("getLevel".equals(method.getName())) return null;
                    if ("getBlockState".equals(method.getName())) return null;
                    return null;
                }
        );

        // Before fix, this would throw NullPointerException on accessor.getLevel().registryAccess()
        assertDoesNotThrow(() -> ShelfBlockProvider.INSTANCE.appendTooltip(tooltip, nullLevelAccessor, null),
                "ShelfBlockProvider must not throw NullPointerException when accessor.getLevel() is null");
        assertTrue(tooltip.getJoinedText().contains("Empty"));
    }

    private static sun.misc.Unsafe getUnsafe() {
        try {
            java.lang.reflect.Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (sun.misc.Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static net.minecraft.world.level.block.state.BlockState createMockBlockState(net.minecraft.world.level.block.Block block) {
        try {
            sun.misc.Unsafe unsafe = getUnsafe();
            net.minecraft.world.level.block.state.BlockState state = (net.minecraft.world.level.block.state.BlockState) unsafe.allocateInstance(net.minecraft.world.level.block.state.BlockState.class);
            java.lang.reflect.Field ownerField = net.minecraft.world.level.block.state.StateHolder.class.getDeclaredField("owner");
            ownerField.setAccessible(true);
            ownerField.set(state, block);
            return state;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static CopperGolemStatueBlock createMockStatueBlock(WeatheringCopper.WeatherState weatherState, boolean waxed) {
        try {
            sun.misc.Unsafe unsafe = getUnsafe();
            Class<?> clazz = waxed ? WaxedCopperGolemStatueBlock.class : WeatheringCopperGolemStatueBlock.class;
            CopperGolemStatueBlock block = (CopperGolemStatueBlock) unsafe.allocateInstance(clazz);
            java.lang.reflect.Field weatherField = CopperGolemStatueBlock.class.getDeclaredField("weatheringState");
            weatherField.setAccessible(true);
            weatherField.set(block, weatherState);
            return block;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static CopperGolemEntity createMockGolemEntity(WeatheringCopper.WeatherState weatherState, boolean waxed) {
        try {
            sun.misc.Unsafe unsafe = getUnsafe();
            CopperGolemEntity golem = (CopperGolemEntity) unsafe.allocateInstance(CopperGolemEntity.class);
            java.lang.reflect.Field tickField = CopperGolemEntity.class.getDeclaredField("nextWeatheringTick");
            tickField.setAccessible(true);
            tickField.setLong(golem, waxed ? -2L : 1000L);
            return golem;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Verify CopperGolemStatueBlockProvider deep test: state, weathering, waxing, and custom name with null level")
    void testStatueDeepVerificationWithNullLevel() {
        // 1. Waxed Oxidized Statue with Custom Name
        TestTooltip tooltip = new TestTooltip();
        net.minecraft.nbt.CompoundTag serverData = new net.minecraft.nbt.CompoundTag();
        serverData.putString(CopperGolemStatueBlockProvider.NBT_CUSTOM_NAME, "{\"text\":\"Test Statue\"}");

        CopperGolemStatueBlock waxedStatue = createMockStatueBlock(WeatheringCopper.WeatherState.OXIDIZED, true);
        net.minecraft.world.level.block.state.BlockState state = createMockBlockState(waxedStatue);

        snownee.jade.api.BlockAccessor accessor = (snownee.jade.api.BlockAccessor) Proxy.newProxyInstance(
                snownee.jade.api.BlockAccessor.class.getClassLoader(),
                new Class<?>[]{snownee.jade.api.BlockAccessor.class},
                (proxy, method, args) -> {
                    if ("getServerData".equals(method.getName())) return serverData;
                    if ("getBlockEntity".equals(method.getName())) return null;
                    if ("getLevel".equals(method.getName())) return null;
                    if ("getBlockState".equals(method.getName())) return state;
                    return null;
                }
        );

        assertDoesNotThrow(() -> CopperGolemStatueBlockProvider.INSTANCE.appendTooltip(tooltip, accessor, null));
        String text = tooltip.getJoinedText();
        assertTrue(text.contains("Oxidized"), "Statue tooltip must contain weathering state 'Oxidized': " + text);
        assertTrue(text.contains("Waxed"), "Statue tooltip must contain 'Waxed': " + text);
        assertTrue(text.contains("Test Statue"), "Statue tooltip must contain custom name 'Test Statue': " + text);

        // 2. Unwaxed Exposed Statue without Custom Name
        TestTooltip tooltip2 = new TestTooltip();
        CopperGolemStatueBlock unwaxedStatue = createMockStatueBlock(WeatheringCopper.WeatherState.EXPOSED, false);
        net.minecraft.world.level.block.state.BlockState state2 = createMockBlockState(unwaxedStatue);

        snownee.jade.api.BlockAccessor accessor2 = (snownee.jade.api.BlockAccessor) Proxy.newProxyInstance(
                snownee.jade.api.BlockAccessor.class.getClassLoader(),
                new Class<?>[]{snownee.jade.api.BlockAccessor.class},
                (proxy, method, args) -> {
                    if ("getServerData".equals(method.getName())) return new net.minecraft.nbt.CompoundTag();
                    if ("getBlockEntity".equals(method.getName())) return null;
                    if ("getLevel".equals(method.getName())) return null;
                    if ("getBlockState".equals(method.getName())) return state2;
                    return null;
                }
        );

        assertDoesNotThrow(() -> CopperGolemStatueBlockProvider.INSTANCE.appendTooltip(tooltip2, accessor2, null));
        String text2 = tooltip2.getJoinedText();
        assertTrue(text2.contains("Exposed"), "Statue tooltip must contain weathering state 'Exposed': " + text2);
        assertTrue(text2.contains("Not Waxed"), "Statue tooltip must contain 'Not Waxed': " + text2);
        assertFalse(text2.contains("Name:"), "Statue without name must not show name line: " + text2);
    }

    @Test
    @DisplayName("Verify CopperGolemEntityProvider deep test: held item, weathering, and waxed status with null level")
    void testGolemDeepVerificationWithNullLevel() {
        TestTooltip tooltip = new TestTooltip();
        net.minecraft.nbt.CompoundTag serverData = new net.minecraft.nbt.CompoundTag();
        serverData.putBoolean(CopperGolemEntityProvider.NBT_WAXED, true);
        serverData.putInt(CopperGolemEntityProvider.NBT_WEATHER_STATE, WeatheringCopper.WeatherState.WEATHERED.ordinal());
        serverData.put(CopperGolemEntityProvider.NBT_HELD_ITEM, new net.minecraft.nbt.CompoundTag());

        CopperGolemEntity golem = createMockGolemEntity(WeatheringCopper.WeatherState.WEATHERED, true);

        snownee.jade.api.EntityAccessor accessor = (snownee.jade.api.EntityAccessor) Proxy.newProxyInstance(
                snownee.jade.api.EntityAccessor.class.getClassLoader(),
                new Class<?>[]{snownee.jade.api.EntityAccessor.class},
                (proxy, method, args) -> {
                    if ("getServerData".equals(method.getName())) return serverData;
                    if ("getEntity".equals(method.getName())) return golem;
                    if ("getLevel".equals(method.getName())) return null;
                    return null;
                }
        );

        assertDoesNotThrow(() -> CopperGolemEntityProvider.INSTANCE.appendTooltip(tooltip, accessor, null));
        String text = tooltip.getJoinedText();
        assertTrue(text.contains("Weathered"), "Golem tooltip must contain weathering state 'Weathered': " + text);
        assertTrue(text.contains("Waxed"), "Golem tooltip must contain 'Waxed': " + text);
    }

    @Test
    @DisplayName("Verify ShelfBlockProvider ignores non-shelf blocks and handles dynamic slot count")
    void testShelfNonShelfBlockAndDynamicSlots() {
        // 1. Non-shelf block must NOT add "Empty" tooltip
        TestTooltip tooltip = new TestTooltip();
        CopperGolemStatueBlock statueBlock = createMockStatueBlock(WeatheringCopper.WeatherState.UNAFFECTED, false);
        net.minecraft.world.level.block.state.BlockState nonShelfState = createMockBlockState(statueBlock);

        snownee.jade.api.BlockAccessor nonShelfAccessor = (snownee.jade.api.BlockAccessor) Proxy.newProxyInstance(
                snownee.jade.api.BlockAccessor.class.getClassLoader(),
                new Class<?>[]{snownee.jade.api.BlockAccessor.class},
                (proxy, method, args) -> {
                    if ("getBlockState".equals(method.getName())) return nonShelfState;
                    if ("getServerData".equals(method.getName())) return new net.minecraft.nbt.CompoundTag();
                    return null;
                }
        );

        ShelfBlockProvider.INSTANCE.appendTooltip(tooltip, nonShelfAccessor, null);
        assertTrue(tooltip.isEmpty(), "ShelfBlockProvider must ignore non-shelf blocks and not produce tooltips");

        // 2. Shelf with 4 slots (exceeding default 3) in serverData parses safely without crashing
        TestTooltip shelfTooltip = new TestTooltip();
        net.minecraft.nbt.CompoundTag serverData = new net.minecraft.nbt.CompoundTag();
        net.minecraft.nbt.ListTag itemsList = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < 4; i++) {
            net.minecraft.nbt.CompoundTag itemTag = new net.minecraft.nbt.CompoundTag();
            itemTag.putByte("Slot", (byte) i);
            itemsList.add(itemTag);
        }
        serverData.put(ShelfBlockProvider.NBT_ITEMS, itemsList);

        snownee.jade.api.BlockAccessor shelfAccessor = (snownee.jade.api.BlockAccessor) Proxy.newProxyInstance(
                snownee.jade.api.BlockAccessor.class.getClassLoader(),
                new Class<?>[]{snownee.jade.api.BlockAccessor.class},
                (proxy, method, args) -> {
                    if ("getServerData".equals(method.getName())) return serverData;
                    if ("getBlockState".equals(method.getName())) return null;
                    if ("getBlockEntity".equals(method.getName())) return null;
                    if ("getLevel".equals(method.getName())) return null;
                    return null;
                }
        );

        assertDoesNotThrow(() -> ShelfBlockProvider.INSTANCE.appendTooltip(shelfTooltip, shelfAccessor, null));
    }

    private static net.minecraft.world.item.ItemStack createMockItemStack(int count) {
        try {
            sun.misc.Unsafe unsafe = getUnsafe();
            net.minecraft.world.item.ItemStack stack = (net.minecraft.world.item.ItemStack) unsafe.allocateInstance(net.minecraft.world.item.ItemStack.class);
            java.lang.reflect.Field countField = net.minecraft.world.item.ItemStack.class.getDeclaredField("count");
            countField.setAccessible(true);
            countField.setInt(stack, count);

            net.minecraft.world.item.Item item = (net.minecraft.world.item.Item) unsafe.allocateInstance(net.minecraft.world.item.Item.class);
            java.lang.reflect.Field itemField = net.minecraft.world.item.ItemStack.class.getDeclaredField("item");
            itemField.setAccessible(true);
            itemField.set(stack, item);

            net.minecraft.core.component.PatchedDataComponentMap components = new net.minecraft.core.component.PatchedDataComponentMap(net.minecraft.core.component.DataComponentMap.EMPTY);
            java.lang.reflect.Field compField = net.minecraft.world.item.ItemStack.class.getDeclaredField("components");
            compField.setAccessible(true);
            compField.set(stack, components);

            return stack;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setGolemArmorSlot(CopperGolemEntity golem, int slotIndex, net.minecraft.world.item.ItemStack stack) {
        try {
            java.lang.reflect.Field armorsField = net.minecraft.world.entity.Mob.class.getDeclaredField("armorItems");
            armorsField.setAccessible(true);
            net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> armors = net.minecraft.core.NonNullList.withSize(4, net.minecraft.world.item.ItemStack.EMPTY);
            armors.set(slotIndex, stack);
            armorsField.set(golem, armors);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setGolemHandSlot(CopperGolemEntity golem, int slotIndex, net.minecraft.world.item.ItemStack stack) {
        try {
            java.lang.reflect.Field handsField = net.minecraft.world.entity.Mob.class.getDeclaredField("handItems");
            handsField.setAccessible(true);
            net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> hands = net.minecraft.core.NonNullList.withSize(2, net.minecraft.world.item.ItemStack.EMPTY);
            hands.set(slotIndex, stack);
            handsField.set(golem, hands);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Verify CopperGolemEntityProvider antenna item detection, null safety, and tooltip output")
    void testGolemAntennaItemDeepVerification() {
        // 1. Antenna item detection on entity (EQUIPMENT_SLOT_ANTENNA = HEAD slot)
        CopperGolemEntity golem = createMockGolemEntity(WeatheringCopper.WeatherState.UNAFFECTED, false);
        net.minecraft.world.item.ItemStack antennaStack = createMockItemStack(1);
        setGolemArmorSlot(golem, 3, antennaStack); // slot 3 = HEAD / EQUIPMENT_SLOT_ANTENNA

        net.minecraft.world.item.ItemStack detected = CopperGolemEntityProvider.getAntennaItem(golem);
        assertNotNull(detected);
        assertFalse(detected.isEmpty(), "Antenna item must be detected on golem");
        assertEquals(antennaStack, detected);

        // 2. appendServerData execution with equipped golem and null level
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        snownee.jade.api.EntityAccessor accessor = (snownee.jade.api.EntityAccessor) Proxy.newProxyInstance(
                snownee.jade.api.EntityAccessor.class.getClassLoader(),
                new Class<?>[]{snownee.jade.api.EntityAccessor.class},
                (proxy, method, args) -> {
                    if ("getEntity".equals(method.getName())) return golem;
                    if ("getLevel".equals(method.getName())) return null;
                    if ("getServerData".equals(method.getName())) return new net.minecraft.nbt.CompoundTag();
                    return null;
                }
        );
        assertDoesNotThrow(() -> CopperGolemEntityProvider.INSTANCE.appendServerData(tag, accessor));

        // 3. appendTooltip renders Antenna line when antenna item is present
        TestTooltip tooltip = new TestTooltip();
        CopperGolemEntityProvider.INSTANCE.appendTooltip(tooltip, accessor, null);
        String text = tooltip.getJoinedText();
        assertTrue(text.contains("Antenna:"), "Tooltip must contain 'Antenna:': " + text);

        // 4. Antenna item with count > 1 shows quantity
        CopperGolemEntity multiGolem = createMockGolemEntity(WeatheringCopper.WeatherState.UNAFFECTED, false);
        setGolemArmorSlot(multiGolem, 3, createMockItemStack(3));
        TestTooltip multiTooltip = new TestTooltip();
        snownee.jade.api.EntityAccessor multiAccessor = (snownee.jade.api.EntityAccessor) Proxy.newProxyInstance(
                snownee.jade.api.EntityAccessor.class.getClassLoader(),
                new Class<?>[]{snownee.jade.api.EntityAccessor.class},
                (proxy, method, args) -> {
                    if ("getEntity".equals(method.getName())) return multiGolem;
                    if ("getLevel".equals(method.getName())) return null;
                    if ("getServerData".equals(method.getName())) return new net.minecraft.nbt.CompoundTag();
                    return null;
                }
        );
        CopperGolemEntityProvider.INSTANCE.appendTooltip(multiTooltip, multiAccessor, null);
        String multiText = multiTooltip.getJoinedText();
        assertTrue(multiText.contains("Antenna:"), "Tooltip must contain 'Antenna:': " + multiText);
        assertTrue(multiText.contains("x3"), "Tooltip must contain 'x3' count: " + multiText);

        // 5. Golem without antenna item does NOT show Antenna: in tooltip
        CopperGolemEntity noAntennaGolem = createMockGolemEntity(WeatheringCopper.WeatherState.UNAFFECTED, false);
        setGolemArmorSlot(noAntennaGolem, 3, net.minecraft.world.item.ItemStack.EMPTY);
        TestTooltip noAntennaTooltip = new TestTooltip();
        snownee.jade.api.EntityAccessor noAntennaAccessor = (snownee.jade.api.EntityAccessor) Proxy.newProxyInstance(
                snownee.jade.api.EntityAccessor.class.getClassLoader(),
                new Class<?>[]{snownee.jade.api.EntityAccessor.class},
                (proxy, method, args) -> {
                    if ("getEntity".equals(method.getName())) return noAntennaGolem;
                    if ("getLevel".equals(method.getName())) return null;
                    if ("getServerData".equals(method.getName())) return new net.minecraft.nbt.CompoundTag();
                    return null;
                }
        );
        CopperGolemEntityProvider.INSTANCE.appendTooltip(noAntennaTooltip, noAntennaAccessor, null);
        String noAntennaText = noAntennaTooltip.getJoinedText();
        assertFalse(noAntennaText.contains("Antenna:"), "Empty antenna slot must not render Antenna tooltip: " + noAntennaText);

        // 6. Both held item and antenna item simultaneously present
        CopperGolemEntity bothGolem = createMockGolemEntity(WeatheringCopper.WeatherState.UNAFFECTED, false);
        setGolemHandSlot(bothGolem, 0, createMockItemStack(2));
        setGolemArmorSlot(bothGolem, 3, createMockItemStack(1));
        TestTooltip bothTooltip = new TestTooltip();
        snownee.jade.api.EntityAccessor bothAccessor = (snownee.jade.api.EntityAccessor) Proxy.newProxyInstance(
                snownee.jade.api.EntityAccessor.class.getClassLoader(),
                new Class<?>[]{snownee.jade.api.EntityAccessor.class},
                (proxy, method, args) -> {
                    if ("getEntity".equals(method.getName())) return bothGolem;
                    if ("getLevel".equals(method.getName())) return null;
                    if ("getServerData".equals(method.getName())) return new net.minecraft.nbt.CompoundTag();
                    return null;
                }
        );
        CopperGolemEntityProvider.INSTANCE.appendTooltip(bothTooltip, bothAccessor, null);
        String bothText = bothTooltip.getJoinedText();
        assertTrue(bothText.contains("x2"), "Must contain held item count: " + bothText);
        assertTrue(bothText.contains("Antenna:"), "Must contain antenna line: " + bothText);

        // 7. Null safety with null level and empty server data
        assertDoesNotThrow(() -> CopperGolemEntityProvider.INSTANCE.appendTooltip(bothTooltip, bothAccessor, null));
        assertDoesNotThrow(() -> CopperGolemEntityProvider.getAntennaItem(null));
    }
}
