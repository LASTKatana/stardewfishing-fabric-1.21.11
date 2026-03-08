package com.kltyton.stardewfishingFabric.common.networking;

/**
 * Packet logic has been moved into SFNetworking.CompleteMinigamePayload.
 * This class is kept only so existing imports do not break.
 * You can safely delete it once you update all call sites.
 */
public class C2SCompleteMinigamePacket {

    public static void register() {
        // Registration is now handled by SFNetworking.register()
        // Call SFNetworking.register() from your ModInitializer instead.
    }
}
