package com.kltyton.stardewfishingFabric.client.util;


import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.joml.Matrix3x2fStack;

public class RenderUtil {

    public static void blitF(GuiGraphics guiGraphics, Identifier texture, float x, float y, int uOffset, int vOffset, int uWidth, int vHeight) {
        blitF(guiGraphics, texture, x, y, uOffset, vOffset, uWidth, vHeight, 1.0f);
    }

    public static void blitF(GuiGraphics guiGraphics, Identifier texture, float x, float y, int uOffset, int vOffset, int uWidth, int vHeight, float alpha) {
        Matrix3x2fStack pose = guiGraphics.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        int argb = ARGB.color((int)(alpha * 255), 255, 255, 255);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, uOffset, vOffset, uWidth, vHeight, 256, 256, argb);
        pose.popMatrix();
    }

    public static void fillF(GuiGraphics guiGraphics, float pMinX, float pMinY, float pMaxX, float pMaxY, float pZ, int pColor) {
        if (pMinX > pMaxX) { float t = pMinX; pMinX = pMaxX; pMaxX = t; }
        if (pMinY > pMaxY) { float t = pMinY; pMinY = pMaxY; pMaxY = t; }
        guiGraphics.fill((int) pMinX, (int) pMinY, (int) pMaxX, (int) pMaxY, pColor);
    }

    public static void drawRotatedAround(GuiGraphics guiGraphics, float radians, float pivotX, float pivotY, Runnable runnable) {
        Matrix3x2fStack pose = guiGraphics.pose();
        pose.pushMatrix();
        pose.translate(pivotX, pivotY);
        pose.rotate(radians);
        pose.translate(-pivotX, -pivotY);
        runnable.run();
        pose.popMatrix();
    }

    public static void drawWithBlend(Runnable runnable) {
        // GUI_TEXTURED pipeline handles blending by default in 1.21.11
        runnable.run();
    }

    public static void drawWithShake(GuiGraphics guiGraphics, Shake shake, float partialTick, boolean doShake, Runnable runnable) {
        Matrix3x2fStack pose = guiGraphics.pose();
        if (doShake) {
            pose.pushMatrix();
            pose.translate(shake.getXOffset(partialTick), shake.getYOffset(partialTick));
        }
        runnable.run();
        if (doShake) {
            pose.popMatrix();
        }
    }
}