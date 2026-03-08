package com.kltyton.stardewfishingFabric.client;

import com.kltyton.stardewfishingFabric.StardewfishingFabric;
import com.kltyton.stardewfishingFabric.client.util.Animation;
import com.kltyton.stardewfishingFabric.client.util.RenderUtil;
import com.kltyton.stardewfishingFabric.client.util.Shake;
import com.kltyton.stardewfishingFabric.common.FishBehavior;
import com.kltyton.stardewfishingFabric.common.networking.SFNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

public class FishingScreen extends Screen {
    private static final Component TITLE = Component.literal("钓鱼小游戏");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(StardewfishingFabric.MODID, "textures/minigame.png");

    private static final int GUI_WIDTH = 38;
    private static final int GUI_HEIGHT = 152;
    private static final int HIT_WIDTH = 73;
    private static final int HIT_HEIGHT = 29;
    private static final int PERFECT_WIDTH = 41;
    private static final int PERFECT_HEIGHT = 12;

    private static final float ALPHA_PER_TICK = 1F / 10;
    private static final float HANDLE_ROT_FAST = Mth.PI / 3;
    private static final float HANDLE_ROT_SLOW = Mth.PI / -7F;

    private static final int REEL_FAST_LENGTH = 30;
    private static final int REEL_SLOW_LENGTH = 20;
    private static final int CREAK_LENGTH = 6;

    private int leftPos, topPos;
    private final FishingMinigame minigame;
    public Status status = Status.HIT_TEXT;
    public double accuracy = -1;
    private boolean mouseDown = false;
    private int animationTimer = 0;

    private final Animation textSize = new Animation(0);
    private final Animation progressBar;
    private final Animation bobberPos = new Animation(0);
    private final Animation bobberAlpha = new Animation(1);
    private final Animation fishPos = new Animation(0);
    private final Animation handleRot = new Animation(0);

    private final Shake shake = new Shake(0.75F, 1);

    public int reelSoundTimer = -1;
    private int creakSoundTimer = 0;

    public FishingScreen(FishBehavior behavior) {
        super(TITLE);
        this.minigame = new FishingMinigame(this, behavior);
        this.progressBar = new Animation(minigame.getProgress());
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        Matrix3x2fStack poseStack = pGuiGraphics.pose();

        if (!isPauseScreen()) {
            float scale = textSize.getInterpolated(pPartialTick) * 1.5F;
            float x = (width - HIT_WIDTH * scale) / 2;
            float y = (height - HIT_HEIGHT * scale) / 3;

            poseStack.pushMatrix();
            poseStack.scale(scale, scale);
            RenderUtil.blitF(pGuiGraphics, TEXTURE, x * (1 / scale), y * (1 / scale), 71, 0, HIT_WIDTH, HIT_HEIGHT);
            poseStack.popMatrix();
        } else {
            // Dark overlay instead of renderBackground() to avoid "blur once per frame" crash
            pGuiGraphics.fill(0, 0, width, height, 0x80000000);

            RenderUtil.drawWithShake(pGuiGraphics, shake, pPartialTick, status == Status.SUCCESS || status == Status.FAILURE, () -> {
                RenderUtil.drawWithBlend(() -> {
                    pGuiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, GUI_WIDTH, GUI_HEIGHT, 256, 256);

                    float bobberY = 4 - 36 + (142 - bobberPos.getInterpolated(pPartialTick));
                    RenderUtil.blitF(pGuiGraphics, TEXTURE, leftPos + 18, topPos + bobberY, 38, 0, 9, 36, bobberAlpha.getInterpolated(pPartialTick));
                });

                RenderUtil.drawWithShake(pGuiGraphics, shake, pPartialTick, minigame.isBobberOnFish() && status == Status.MINIGAME, () -> {
                    float fishY = 4 - 16 + (142 - fishPos.getInterpolated(pPartialTick));
                    RenderUtil.blitF(pGuiGraphics, TEXTURE, leftPos + 14, topPos + fishY, 55, 0, 16, 15);
                });

                float progress = progressBar.getInterpolated(pPartialTick);
                int color = Mth.hsvToRgb(progress / 3.0F, 1.0F, 1.0F) | 0xFF000000;
                RenderUtil.fillF(pGuiGraphics, leftPos + 33, topPos + 148, leftPos + 37, topPos + 148 - progress * 145, 0, color);

                RenderUtil.drawRotatedAround(pGuiGraphics, handleRot.getInterpolated(pPartialTick), leftPos + 6.5F, topPos + 130.5F,
                        () -> pGuiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + 5, topPos + 129, 47, 0, 8, 3, 256, 256));

                if (status == Status.SUCCESS && accuracy == 1) {
                    float scale = textSize.getInterpolated(pPartialTick);
                    float x = leftPos + 2 + (PERFECT_WIDTH - PERFECT_WIDTH * scale) / 2;
                    float y = topPos - PERFECT_HEIGHT * scale;

                    poseStack.pushMatrix();
                    poseStack.scale(scale, scale);
                    RenderUtil.blitF(pGuiGraphics, TEXTURE, x * (1 / scale), y * (1 / scale), 144, 0, PERFECT_WIDTH, PERFECT_HEIGHT);
                    poseStack.popMatrix();
                }
            });
        }
    }


    public void renderBlurredBackground(float partialTick) {
        // Intentionally empty - prevent "can only blur once per frame" crash in 1.21.11
    }

    @Override
    protected void init() {
        leftPos = (width - GUI_WIDTH) / 2;
        topPos = (height - GUI_HEIGHT) / 2;
    }

    @Override
    public void tick() {
        long window = GLFW.glfwGetCurrentContext();
        boolean currentMouseDown =
                GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS ||
                        GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_2) == GLFW.GLFW_PRESS;
        if (currentMouseDown && !mouseDown) {
            playSound(StardewfishingFabric.REEL_CREAK);
        }
        mouseDown = currentMouseDown;

        shake.tick();

        switch (status) {
            case HIT_TEXT -> {
                if (animationTimer < 20) {
                    if (++animationTimer == 20) {
                        status = Status.MINIGAME;
                    } else if (animationTimer <= 5) {
                        textSize.addValue(0.2F);
                    } else if (animationTimer <= 15) {
                        textSize.addValue(-0.013F);
                    } else {
                        textSize.addValue(-0.16F);
                    }
                }
            }
            case MINIGAME -> {
                minigame.tick(mouseDown);
                boolean onFish = minigame.isBobberOnFish();
                progressBar.setValue(minigame.getProgress());
                bobberPos.setValue(minigame.getBobberPos());
                bobberAlpha.addValue(onFish ? ALPHA_PER_TICK : -ALPHA_PER_TICK, 0.4F, 1);
                fishPos.setValue(minigame.getFishPos());
                handleRot.addValue(onFish ? HANDLE_ROT_FAST : HANDLE_ROT_SLOW);

                if (reelSoundTimer == -1 || --reelSoundTimer == 0) {
                    reelSoundTimer = onFish ? REEL_FAST_LENGTH : REEL_SLOW_LENGTH;
                    playSound(onFish ? StardewfishingFabric.REEL_FAST : StardewfishingFabric.REEL_SLOW);
                }

                if (creakSoundTimer > 0) creakSoundTimer--;
                if (mouseDown && creakSoundTimer == 0) {
                    creakSoundTimer = CREAK_LENGTH;
                    playSound(StardewfishingFabric.REEL_CREAK);
                }
            }
            case SUCCESS, FAILURE -> {
                if (--animationTimer == 0) {
                    onClose();
                } else if (animationTimer >= 15) {
                    textSize.addValue(0.2F);
                } else if (animationTimer >= 5) {
                    textSize.addValue(-0.013F);
                } else {
                    textSize.addValue(-0.16F);
                }
            }
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        SFNetworking.sendToServer(status == Status.SUCCESS, accuracy);
        stopReelingSounds();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return status == Status.MINIGAME;
    }

    @Override
    public boolean isPauseScreen() {
        return status != Status.HIT_TEXT;
    }

    public void setResult(boolean success, double accuracy) {
        status = success ? Status.SUCCESS : Status.FAILURE;
        this.accuracy = accuracy;
        animationTimer = 20;
        textSize.reset(0.0F);
        progressBar.freeze();
        bobberPos.freeze();
        bobberAlpha.freeze();
        fishPos.freeze();
        handleRot.freeze();
        playSound(success ? StardewfishingFabric.COMPLETE : StardewfishingFabric.FISH_ESCAPE);
        shake.setValues(2.0F, 1);
    }

    public void playSound(SoundEvent soundEvent) {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(soundEvent, 1.0F));
    }

    public void stopReelingSounds() {
        minecraft.getSoundManager().stop(StardewfishingFabric.REEL_FAST.location(), null);
        minecraft.getSoundManager().stop(StardewfishingFabric.REEL_SLOW.location(), null);
    }

    public enum Status {
        HIT_TEXT, MINIGAME, SUCCESS, FAILURE
    }
}