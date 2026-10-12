package com.magicbanana666.bananautil.client.head;

import com.magicbanana666.bananautil.mixin.client.ModelPartAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Finds the head of any entity model and where the top of that head is.
 * <p>
 * Models are trees of named {@link ModelPart}s, so this looks the head up by name / {@link HeadedModel} and measures
 * its biggest cube, which also works for modded mobs without any per-mob data.
 */
public final class HeadLocator
{
    /** A player head is 8 pixels wide; hats are modelled for that size. */
    public static final float PLAYER_HEAD_SIZE = 8F;

    private static final Map<Model<?>, Optional<Head>> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    private HeadLocator() {}

    /**
     * @param path   the parts from the model root down to (and including) the head
     * @param topX   centre of the top of the head, in head space, in pixels (model units)
     * @param topY   top of the head (smallest y, model y points down)
     * @param topZ   centre of the top of the head
     * @param width  largest horizontal size of the skull in pixels
     */
    public record Head(List<ModelPart> path, float topX, float topY, float topZ, float width)
    {
        /** Applies the root -> head transforms (uses the parts' current animated pose). */
        public void translateToHead(PoseStack poseStack)
        {
            for(ModelPart part : path)
            {
                part.translateAndRotate(poseStack);
            }
        }

        /** Moves from the head joint to the centre of the top face of the skull. */
        public void translateToTop(PoseStack poseStack)
        {
            poseStack.translate(topX / 16F, topY / 16F, topZ / 16F);
        }

        /** The head size relative to a player head. */
        public float scale()
        {
            return width / PLAYER_HEAD_SIZE;
        }

        public ModelPart headPart()
        {
            return path.get(path.size() - 1);
        }
    }

    @Nullable
    public static Head find(Model<?> model)
    {
        return CACHE.computeIfAbsent(model, m -> Optional.ofNullable(locate(m))).orElse(null);
    }

    public static void clearCache()
    {
        CACHE.clear();
    }

    @Nullable
    private static Head locate(Model<?> model)
    {
        ModelPart root = model.root();
        ModelPart head = null;
        if(model instanceof HeadedModel headed)
        {
            head = headed.getHead();
        }
        if(head == null)
        {
            head = findByName(root, true);
        }
        if(head == null)
        {
            head = findByName(root, false);
        }
        if(head == null)
        {
            return null;
        }

        List<ModelPart> path = new ArrayList<>();
        if(!findPath(root, head, path))
        {
            return null;
        }

        //measure the biggest cube of the head (in head space)
        Box best = new Box();
        measure(head, new Matrix4f(), best, true);
        if(!best.found)
        {
            return null;
        }
        return new Head(Collections.unmodifiableList(path), (best.minX + best.maxX) / 2F, best.minY, (best.minZ + best.maxZ) / 2F, Math.max(best.maxX - best.minX, best.maxZ - best.minZ));
    }

    @Nullable
    private static ModelPart findByName(ModelPart root, boolean exact)
    {
        Deque<ModelPart> queue = new ArrayDeque<>();
        queue.add(root);
        while(!queue.isEmpty())
        {
            ModelPart part = queue.poll();
            for(Map.Entry<String, ModelPart> e : children(part).entrySet())
            {
                String name = e.getKey().toLowerCase(Locale.ROOT);
                boolean match = exact ? name.equals("head") : name.contains("head") && !name.contains("wear");
                if(match && hasCubes(e.getValue()))
                {
                    return e.getValue();
                }
                queue.add(e.getValue());
            }
        }
        return null;
    }

    private static boolean hasCubes(ModelPart part)
    {
        if(!cubes(part).isEmpty())
        {
            return true;
        }
        for(ModelPart child : children(part).values())
        {
            if(hasCubes(child))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean findPath(ModelPart current, ModelPart target, List<ModelPart> path)
    {
        for(ModelPart child : children(current).values())
        {
            path.add(child);
            if(child == target || findPath(child, target, path))
            {
                return true;
            }
            path.remove(path.size() - 1);
        }
        return false;
    }

    private static void measure(ModelPart part, Matrix4f transform, Box best, boolean isHead)
    {
        for(ModelPart.Cube cube : cubes(part))
        {
            float volume = (cube.maxX - cube.minX) * (cube.maxY - cube.minY) * (cube.maxZ - cube.minZ);
            if(volume > best.volume)
            {
                best.set(cube, transform, volume);
            }
        }
        for(Map.Entry<String, ModelPart> e : children(part).entrySet())
        {
            String name = e.getKey().toLowerCase(Locale.ROOT);
            if(name.equals("hat") || name.contains("ear") || name.contains("horn"))
            {
                continue; //the humanoid hat overlay and decorations don't define the skull
            }
            ModelPart child = e.getValue();
            //same as ModelPart.translateAndRotate, but in pixel space
            PartPose pose = child.getInitialPose();
            Matrix4f m = new Matrix4f(transform)
                    .translate(pose.x(), pose.y(), pose.z())
                    .rotateZYX(pose.zRot(), pose.yRot(), pose.xRot())
                    .scale(pose.xScale(), pose.yScale(), pose.zScale());
            measure(child, m, best, false);
        }
    }

    @SuppressWarnings("DataFlowIssue")
    private static List<ModelPart.Cube> cubes(ModelPart part)
    {
        return ((ModelPartAccessor)(Object)part).bananautil$getCubes();
    }

    @SuppressWarnings("DataFlowIssue")
    private static Map<String, ModelPart> children(ModelPart part)
    {
        return ((ModelPartAccessor)(Object)part).bananautil$getChildren();
    }

    private static final class Box
    {
        boolean found;
        float volume = -1F;
        float minX, minY, minZ, maxX, maxY, maxZ;

        void set(ModelPart.Cube cube, Matrix4f transform, float volume)
        {
            this.found = true;
            this.volume = volume;
            minX = minY = minZ = Float.MAX_VALUE;
            maxX = maxY = maxZ = -Float.MAX_VALUE;
            Vector3f v = new Vector3f();
            for(int i = 0; i < 8; i++)
            {
                v.set((i & 1) == 0 ? cube.minX : cube.maxX, (i & 2) == 0 ? cube.minY : cube.maxY, (i & 4) == 0 ? cube.minZ : cube.maxZ);
                transform.transformPosition(v);
                minX = Math.min(minX, v.x);
                minY = Math.min(minY, v.y);
                minZ = Math.min(minZ, v.z);
                maxX = Math.max(maxX, v.x);
                maxY = Math.max(maxY, v.y);
                maxZ = Math.max(maxZ, v.z);
            }
        }
    }
}
