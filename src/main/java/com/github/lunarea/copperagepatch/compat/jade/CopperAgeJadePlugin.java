package com.github.lunarea.copperagepatch.compat.jade;

import com.github.smallinger.copperagebackport.block.CopperGolemStatueBlock;
import com.github.smallinger.copperagebackport.block.shelf.ShelfBlock;
import com.github.smallinger.copperagebackport.entity.CopperGolemEntity;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade plugin providing HUD tooltip compatibility for Copper Age Backport entities and blocks.
 * Discovered dynamically by Jade via @WailaPlugin annotation.
 */
@WailaPlugin
public class CopperAgeJadePlugin implements IWailaPlugin {

    public static final String ID = "copper_age_patch:jade";

    public static final ResourceLocation COPPER_GOLEM = ResourceLocation.fromNamespaceAndPath("copper_age_patch", "copper_golem");
    public static final ResourceLocation COPPER_GOLEM_STATUE = ResourceLocation.fromNamespaceAndPath("copper_age_patch", "copper_golem_statue");
    public static final ResourceLocation SHELF = ResourceLocation.fromNamespaceAndPath("copper_age_patch", "shelf");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(CopperGolemEntityProvider.INSTANCE, CopperGolemEntity.class);
        registration.registerBlockDataProvider(CopperGolemStatueBlockProvider.INSTANCE, CopperGolemStatueBlock.class);
        registration.registerBlockDataProvider(ShelfBlockProvider.INSTANCE, ShelfBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(CopperGolemEntityProvider.INSTANCE, CopperGolemEntity.class);
        registration.registerBlockComponent(CopperGolemStatueBlockProvider.INSTANCE, CopperGolemStatueBlock.class);
        registration.registerBlockComponent(ShelfBlockProvider.INSTANCE, ShelfBlock.class);
    }
}
