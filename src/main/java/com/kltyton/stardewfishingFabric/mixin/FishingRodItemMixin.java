package com.kltyton.stardewfishingFabric.mixin;

import com.kltyton.stardewfishingFabric.StardewfishingFabric;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FishingRodItem.class)
public class FishingRodItemMixin {

    @Redirect(method = "use",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;playSound(DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FF)V",
                    ordinal = 0),
            require = 0)
    private void redirectPlaySoundFirst(Level instance, double x, double y, double z, Holder<SoundEvent> sound, SoundSource category, float volume, float pitch) {
        Player nearestPlayer = null;
        if (Minecraft.getInstance().level != null) {
            nearestPlayer = Minecraft.getInstance().level.getNearestPlayer(x, y, z, 1, false);
        }
        if (nearestPlayer != null && nearestPlayer.fishing instanceof FishingHook fishingHook) {
            boolean isBiting = fishingHook.getEntityData().get(FishingHook.DATA_BITING);
            if (isBiting) {
                instance.playSound(null, x, y, z, Holder.direct(StardewfishingFabric.FISH_HIT), SoundSource.NEUTRAL, 1.0F, 1.0F);
            } else {
                instance.playSound(null, x, y, z, Holder.direct(StardewfishingFabric.PULL_ITEM), SoundSource.NEUTRAL, 1.0F, 1.0F);
            }
        }
    }

    @Redirect(method = "use",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;playSound(DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FF)V",
                    ordinal = 1),
            require = 0)
    private void redirectPlaySoundSecond(Level instance, double x, double y, double z, Holder<SoundEvent> sound, SoundSource category, float volume, float pitch) {
        instance.playSound(null, x, y, z, Holder.direct(StardewfishingFabric.CAST), SoundSource.NEUTRAL, 1.0F, 1.0F);
    }
}