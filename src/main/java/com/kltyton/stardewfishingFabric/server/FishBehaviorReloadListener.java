package com.kltyton.stardewfishingFabric.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kltyton.stardewfishingFabric.StardewfishingFabric;
import com.kltyton.stardewfishingFabric.common.FishBehavior;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FishBehaviorReloadListener extends SimplePreparableReloadListener<Map<String, JsonObject>> implements IdentifiableResourceReloadListener {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON_INSTANCE = new Gson();
    private static final Identifier LOCATION = Identifier.fromNamespaceAndPath(StardewfishingFabric.MODID, "data.json");
    private static FishBehaviorReloadListener INSTANCE;

    // 存储物品与其对应鱼类行为的映射
    private final Map<Item, FishBehavior> fishBehaviors = new HashMap<>();
    private FishBehavior defaultBehavior;

    private FishBehaviorReloadListener() {
        super();
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath(StardewfishingFabric.MODID, "fish_behavior_reload");
    }

    @Override
    protected @NotNull Map<String, JsonObject> prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        Map<String, JsonObject> objects = new HashMap<>();
        for (Resource resource : pResourceManager.getResourceStack(LOCATION)) {
            try (InputStream inputstream = resource.open();
                 Reader reader = new BufferedReader(new InputStreamReader(inputstream, StandardCharsets.UTF_8))
            ) {
                objects.put(resource.sourcePackId(), GsonHelper.fromJson(GSON_INSTANCE, reader, JsonObject.class));
            } catch (RuntimeException | IOException exception) {
                LOGGER.error("数据包 {} 中鱼行为列表 {} 中的 json 无效", LOCATION, resource.sourcePackId(), exception);
            }
        }
        return objects;
    }

    @Override
    protected void apply(Map<String, JsonObject> jsonObjects, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        for (Map.Entry<String, JsonObject> entry : jsonObjects.entrySet()) {
            FishBehaviorList.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(errorMsg -> LOGGER.warn("无法解码数据包 {} - {} 中的鱼行为列表 {}", LOCATION, entry.getKey(), errorMsg))
                    .ifPresent(behaviorList -> behaviorList.behaviors.forEach((loc, fishBehavior) -> {
                        Item item = BuiltInRegistries.ITEM.getValue(loc);
                        if (behaviorList.replace || !fishBehaviors.containsKey(item)) {
                            fishBehaviors.put(item, fishBehavior);
                        }
                        behaviorList.defaultBehavior.ifPresent(behavior -> defaultBehavior = behavior);
                    }));
        }
    }

    public static FishBehaviorReloadListener create() {
        INSTANCE = new FishBehaviorReloadListener();
        return INSTANCE;
    }

    public static FishBehavior getBehavior(@Nullable ItemStack stack) {
        if (stack == null) return INSTANCE.defaultBehavior;
        return INSTANCE.fishBehaviors.getOrDefault(stack.getItem(), INSTANCE.defaultBehavior);
    }

    private record FishBehaviorList(boolean replace, Map<Identifier, FishBehavior> behaviors, Optional<FishBehavior> defaultBehavior) {
        private static final Codec<FishBehaviorList> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.BOOL.optionalFieldOf("replace", false).forGetter(FishBehaviorList::replace),
                Codec.unboundedMap(Identifier.CODEC, FishBehavior.CODEC).fieldOf("behaviors").forGetter(FishBehaviorList::behaviors),
                FishBehavior.CODEC.optionalFieldOf("defaultBehavior").forGetter(FishBehaviorList::defaultBehavior)
        ).apply(inst, FishBehaviorList::new));
    }
}
