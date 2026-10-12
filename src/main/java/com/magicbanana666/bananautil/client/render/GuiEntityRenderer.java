package com.magicbanana666.bananautil.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.function.Consumer;

/**
 * Draws a living entity into a GUI rectangle (26.1 renders these as picture-in-picture).
 */
public final class GuiEntityRenderer
{
    private GuiEntityRenderer() {}

    /**
     * @param size     pixels per block (the entity is scaled so 1 block = size GUI pixels)
     * @param bodyYaw  degrees the entity is turned (0 = facing the viewer)
     * @param pitch    degrees the camera looks down at the entity
     * @param tweak    lets callers adjust the extracted render state, may be null
     */
    public static void render(GuiGraphicsExtractor graphics, LivingEntity entity, int x0, int y0, int x1, int y1, float size, float bodyYaw, float pitch, float partialTick, Consumer<EntityRenderState> tweak)
    {
        render(graphics, entity, x0, y0, x1, y1, size, bodyYaw, pitch, partialTick, 0F, tweak);
    }

    /**
     * @param offsetY blocks to move the entity down inside the rectangle (EG to make room for something on its head)
     */
    public static void render(GuiGraphicsExtractor graphics, LivingEntity entity, int x0, int y0, int x1, int y1, float size, float bodyYaw, float pitch, float partialTick, float offsetY, Consumer<EntityRenderState> tweak)
    {
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderer<? super LivingEntity, ?> renderer = dispatcher.getRenderer(entity);
        EntityRenderState state = renderer.createRenderState(entity, partialTick);
        state.shadowPieces.clear();
        state.outlineColor = 0;
        state.nameTag = null;
        state.scoreText = null;
        state.leashStates = null;

        Quaternionf rotation = new Quaternionf().rotateZ((float)Math.PI);
        Quaternionf cameraRotation = new Quaternionf().rotateX((float)Math.toRadians(pitch));
        rotation.mul(cameraRotation);

        if(state instanceof LivingEntityRenderState living)
        {
            living.bodyRot = 180F + bodyYaw;
            living.yRot = 0F;
            living.xRot = 0F;
            living.boundingBoxWidth = living.boundingBoxWidth / living.scale;
            living.boundingBoxHeight = living.boundingBoxHeight / living.scale;
            living.scale = 1F;
        }
        if(tweak != null)
        {
            tweak.accept(state);
        }

        Vector3f translation = new Vector3f(0F, state.boundingBoxHeight / 2F + offsetY, 0F);
        graphics.entity(state, size, translation, rotation, cameraRotation, x0, y0, x1, y1);
    }

    /** Pixels-per-block so an entity of the given size fits in a box of the given GUI size. */
    public static float fitScale(LivingEntity entity, float boxWidth, float boxHeight)
    {
        float w = Math.max(0.1F, entity.getBbWidth());
        float h = Math.max(0.1F, entity.getBbHeight());
        return Math.min(boxWidth / (w * 1.4F), boxHeight / (h * 1.15F));
    }
}
