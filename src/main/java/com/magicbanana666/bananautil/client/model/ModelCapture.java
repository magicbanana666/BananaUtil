package com.magicbanana666.bananautil.client.model;

import com.magicbanana666.bananautil.mixin.client.ModelPartAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * A fake {@link SubmitNodeCollector} that records the boxes of every model an entity renderer submits, with their final
 * transforms, instead of drawing them, so they can be animated or drawn in another way.
 */
public final class ModelCapture implements SubmitNodeCollector
{
    private static final Set<Direction> ALL_FACES = Collections.unmodifiableSet(EnumSet.allOf(Direction.class));

    /** One box, in pixels, with the pose it was drawn with (relative to the capture origin). */
    public record CapturedCube(Matrix4f pose, Matrix3f normal, float minX, float minY, float minZ, float maxX, float maxY, float maxZ)
    {
        public float centerX()
        {
            return (minX + maxX) / 2F;
        }

        public float centerY()
        {
            return (minY + maxY) / 2F;
        }

        public float centerZ()
        {
            return (minZ + maxZ) / 2F;
        }

        public CapturedCube lerp(CapturedCube other, float t)
        {
            return new CapturedCube(new Matrix4f(pose).lerp(other.pose, t), new Matrix3f(normal).lerp(other.normal, t),
                    minX + (other.minX - minX) * t, minY + (other.minY - minY) * t, minZ + (other.minZ - minZ) * t,
                    maxX + (other.maxX - maxX) * t, maxY + (other.maxY - maxY) * t, maxZ + (other.maxZ - maxZ) * t);
        }

        /** A zero sized box at the centre of this one (for boxes that appear / disappear in a transition). */
        public CapturedCube collapsed()
        {
            float x = centerX(), y = centerY(), z = centerZ();
            return new CapturedCube(new Matrix4f(pose), new Matrix3f(normal), x, y, z, x, y, z);
        }

        /** Draws this box with a simple cube UV layout. */
        public void draw(PoseStack.Pose base, VertexConsumer consumer, int light, int overlay, int color, int texU, int texV, int texWidth, int texHeight, float grow)
        {
            float w = maxX - minX, h = maxY - minY, d = maxZ - minZ;
            if(w <= 0F && h <= 0F && d <= 0F)
            {
                return;
            }
            PoseStack stack = new PoseStack();
            stack.last().set(base);
            stack.last().pose().mul(pose);
            stack.last().normal().mul(normal);
            ModelPart.Cube cube = new ModelPart.Cube(texU, texV, minX, minY, minZ, w, h, d, grow, grow, grow, false, texWidth, texHeight, ALL_FACES);
            cube.compile(stack.last(), consumer, light, overlay, color);
        }
    }

    private final List<CapturedCube> cubes = new ArrayList<>();

    public List<CapturedCube> getCubes()
    {
        return cubes;
    }

    public void clear()
    {
        cubes.clear();
    }

    private void capture(ModelPart part, PoseStack poseStack)
    {
        if(!part.visible)
        {
            return;
        }
        ModelPartAccessor accessor = (ModelPartAccessor)(Object)part;
        List<ModelPart.Cube> partCubes = accessor.bananautil$getCubes();
        if(partCubes.isEmpty() && accessor.bananautil$getChildren().isEmpty())
        {
            return;
        }
        poseStack.pushPose();
        part.translateAndRotate(poseStack);
        if(!part.skipDraw)
        {
            for(ModelPart.Cube cube : partCubes)
            {
                float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
                float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
                for(ModelPart.Polygon polygon : cube.polygons)
                {
                    for(ModelPart.Vertex vertex : polygon.vertices())
                    {
                        minX = Math.min(minX, vertex.x());
                        minY = Math.min(minY, vertex.y());
                        minZ = Math.min(minZ, vertex.z());
                        maxX = Math.max(maxX, vertex.x());
                        maxY = Math.max(maxY, vertex.y());
                        maxZ = Math.max(maxZ, vertex.z());
                    }
                }
                if(minX <= maxX)
                {
                    PoseStack.Pose pose = poseStack.last();
                    cubes.add(new CapturedCube(new Matrix4f(pose.pose()), new Matrix3f(pose.normal()), minX, minY, minZ, maxX, maxY, maxZ));
                }
            }
        }
        for(ModelPart child : accessor.bananautil$getChildren().values())
        {
            capture(child, poseStack);
        }
        poseStack.popPose();
    }

    @Override
    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, @Nullable TextureAtlasSprite sprite, int outlineColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay)
    {
        model.setupAnim(state);
        capture(model.root(), poseStack);
    }

    @Override
    public void submitModelPart(ModelPart modelPart, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, @Nullable TextureAtlasSprite sprite, boolean sheeted, boolean hasFoil, int tintedColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay, int outlineColor)
    {
        capture(modelPart, poseStack);
    }

    @Override
    public OrderedSubmitNodeCollector order(int order)
    {
        return this;
    }

    // everything else is not geometry we care about

    @Override
    public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {}

    @Override
    public void submitNameTag(PoseStack poseStack, @Nullable Vec3 nameTagAttachment, int offset, Component name, boolean seeThrough, int lightCoords, double distanceToCameraSq, CameraRenderState camera) {}

    @Override
    public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence string, boolean dropShadow, Font.DisplayMode displayMode, int lightCoords, int color, int backgroundColor, int outlineColor) {}

    @Override
    public void submitFlame(PoseStack poseStack, EntityRenderState renderState, Quaternionf rotation) {}

    @Override
    public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {}

    @Override
    public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState) {}

    @Override
    public void submitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts, int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {}

    @Override
    public void submitBreakingBlockModel(PoseStack poseStack, BlockStateModel model, long seed, int progress) {}

    @Override
    public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int[] tintLayers, List<BakedQuad> quads, ItemStackRenderState.FoilType foilType) {}

    @Override
    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, CustomGeometryRenderer customGeometryRenderer) {}

    @Override
    public void submitParticleGroup(ParticleGroupRenderer particleGroupRenderer) {}
}
