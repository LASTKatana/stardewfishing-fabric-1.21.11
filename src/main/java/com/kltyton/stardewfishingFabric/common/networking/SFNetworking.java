package com.kltyton.stardewfishingFabric.common.networking;

import com.kltyton.stardewfishingFabric.StardewfishingFabric;
import com.kltyton.stardewfishingFabric.client.ClientEvents;
import com.kltyton.stardewfishingFabric.common.FishBehavior;
import com.kltyton.stardewfishingFabric.common.FishingDataStorage;
import com.kltyton.stardewfishingFabric.common.FishingHookLogic;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;

public class SFNetworking {

    // -------------------------------------------------------------------------
    // StartMinigame  S2C  payload
    // -------------------------------------------------------------------------
    public record StartMinigamePayload(FishBehavior behavior) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<StartMinigamePayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(StardewfishingFabric.MODID, "start_minigame"));

        public static final StreamCodec<FriendlyByteBuf, StartMinigamePayload> CODEC =
                StreamCodec.of(
                        (buf, payload) -> payload.behavior().writeToBuffer(buf),
                        buf -> new StartMinigamePayload(new FishBehavior(buf))
                );

        @Override
        public CustomPacketPayload.Type<StartMinigamePayload> type() {
            return TYPE;
        }
    }

    // -------------------------------------------------------------------------
    // CompleteMinigame  C2S  payload
    // -------------------------------------------------------------------------
    public record CompleteMinigamePayload(boolean success, double accuracy) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<CompleteMinigamePayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(StardewfishingFabric.MODID, "complete_minigame"));

        public static final StreamCodec<FriendlyByteBuf, CompleteMinigamePayload> CODEC =
                StreamCodec.of(
                        (buf, payload) -> {
                            buf.writeBoolean(payload.success());
                            if (payload.success()) buf.writeDouble(payload.accuracy());
                        },
                        buf -> {
                            boolean success = buf.readBoolean();
                            double accuracy = success ? buf.readDouble() : -1;
                            return new CompleteMinigamePayload(success, accuracy);
                        }
                );

        @Override
        public CustomPacketPayload.Type<CompleteMinigamePayload> type() {
            return TYPE;
        }
    }

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------
    public static void register() {
        // Register payload types
        PayloadTypeRegistry.playS2C().register(StartMinigamePayload.TYPE, StartMinigamePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CompleteMinigamePayload.TYPE, CompleteMinigamePayload.CODEC);

        // Client receives StartMinigame from server
        ClientPlayNetworking.registerGlobalReceiver(StartMinigamePayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientEvents.openFishingScreen(payload.behavior()))
        );

        // Server receives CompleteMinigame from client
        ServerPlayNetworking.registerGlobalReceiver(CompleteMinigamePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            FishingHook hook = FishingDataStorage.getHookForPlayer(player);
            ItemStack items = FishingDataStorage.getItemsForPlayer(player);
            context.server().execute(() ->
                    FishingHookLogic.endMinigame(player, payload.success(), payload.accuracy(), hook, items)
            );
        });
    }

    // -------------------------------------------------------------------------
    // Send helpers
    // -------------------------------------------------------------------------

    // Server → Client: send StartMinigame
    public static void sendToPlayer(ServerPlayer player, FishBehavior behavior) {
        ServerPlayNetworking.send(player, new StartMinigamePayload(behavior));
    }

    // Client → Server: send CompleteMinigame
    public static void sendToServer(boolean success, double accuracy) {
        ClientPlayNetworking.send(new CompleteMinigamePayload(success, accuracy));
    }
}
