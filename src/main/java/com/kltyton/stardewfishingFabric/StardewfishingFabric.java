package com.kltyton.stardewfishingFabric;

import com.kltyton.stardewfishingFabric.common.CommonEvents;
import com.kltyton.stardewfishingFabric.common.networking.C2SCompleteMinigamePacket;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class StardewfishingFabric implements ModInitializer {
    /*
        常量定义
    */
    public static final String MODID = "stardew_fishing";
    public static final ResourceKey<Registry<SoundEvent>> SOUND_EVENT_REGISTRY_KEY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(MODID, "sound_events"));
    public static final TagKey<Item> STARTS_MINIGAME = TagKey.create(BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath(MODID, "starts_minigame"));
    public static final Registry<SoundEvent> SOUND_EVENTS = FabricRegistryBuilder.createSimple(SOUND_EVENT_REGISTRY_KEY).buildAndRegister();
    /*
        声音事件定义
    */
    public static final SoundEvent CAST = registerSound("cast");
    public static final SoundEvent COMPLETE = registerSound("complete");
    public static final SoundEvent DWOP = registerSound("dwop");
    public static final SoundEvent FISH_ESCAPE = registerSound("fish_escape");
    public static final SoundEvent FISH_BITE = registerSound("fish_bite");
    public static final SoundEvent FISH_HIT = registerSound("fish_hit");
    public static final SoundEvent PULL_ITEM = registerSound("pull_item");
    public static final SoundEvent REEL_CREAK = registerSound("reel_creak");
    public static final SoundEvent REEL_FAST = registerSound("reel_fast");
    public static final SoundEvent REEL_SLOW = registerSound("reel_slow");

    @Override
    public void onInitialize() {
        C2SCompleteMinigamePacket.register();
        CommonEvents.initialize();
        // 注册声音事件到注册表
        registerSoundEvent("cast", CAST);
        registerSoundEvent("complete", COMPLETE);
        registerSoundEvent("dwop", DWOP);
        registerSoundEvent("fish_escape", FISH_ESCAPE);
        registerSoundEvent("fish_bite", FISH_BITE);
        registerSoundEvent("fish_hit", FISH_HIT);
        registerSoundEvent("pull_item", PULL_ITEM);
        registerSoundEvent("reel_creak", REEL_CREAK);
        registerSoundEvent("reel_fast", REEL_FAST);
        registerSoundEvent("reel_slow", REEL_SLOW);
    }

    // 创建新的声音事件
    private static SoundEvent registerSound(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(MODID, name);
        return SoundEvent.createVariableRangeEvent(id);
    }

    private static void registerSoundEvent(String name, SoundEvent soundEvent) {
        Registry.register(SOUND_EVENTS, Identifier.fromNamespaceAndPath(MODID, name), soundEvent);
    }
}
