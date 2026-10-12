package com.magicbanana666.bananautil.client.model;

import com.magicbanana666.bananautil.tabula.TabulaProject;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A Tabula project baked into vanilla {@link ModelPart}s.
 * Rendering is deferred in 26.1 (submit now, draw later), so part visibility is fixed at bake time:
 * bake one model per set of hidden part names instead of toggling parts per render.
 */
public final class BakedTabulaModel
{
    private static final Set<Direction> ALL_FACES = Collections.unmodifiableSet(EnumSet.allOf(Direction.class));

    public final List<ModelPart> roots;
    public final float scaleX;
    public final float scaleY;
    public final float scaleZ;

    private BakedTabulaModel(List<ModelPart> roots, float scaleX, float scaleY, float scaleZ)
    {
        this.roots = roots;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.scaleZ = scaleZ;
    }

    public static BakedTabulaModel bake(TabulaProject project)
    {
        return bake(project, Collections.emptySet());
    }

    /**
     * @param hiddenPartNames names of parts (and so their children) that should not render
     */
    public static BakedTabulaModel bake(TabulaProject project, Set<String> hiddenPartNames)
    {
        List<ModelPart> roots = new ArrayList<>();
        for(TabulaProject.Part part : project.parts)
        {
            roots.add(bakePart(project, part, hiddenPartNames));
        }
        return new BakedTabulaModel(roots, project.scaleX, project.scaleY, project.scaleZ);
    }

    private static ModelPart bakePart(TabulaProject project, TabulaProject.Part part, Set<String> hidden)
    {
        float texW = part.matchProject ? project.texWidth : part.texWidth;
        float texH = part.matchProject ? project.texHeight : part.texHeight;

        List<ModelPart.Cube> cubes = new ArrayList<>();
        for(TabulaProject.Box box : part.boxes)
        {
            cubes.add(new ModelPart.Cube(part.texOffX + box.texOffX, part.texOffY + box.texOffY,
                    box.posX, box.posY, box.posZ, box.dimX, box.dimY, box.dimZ,
                    box.expandX, box.expandY, box.expandZ, part.mirror, texW, texH, ALL_FACES));
        }

        Map<String, ModelPart> children = new HashMap<>();
        for(int i = 0; i < part.children.size(); i++)
        {
            TabulaProject.Part child = part.children.get(i);
            children.put(i + ":" + child.name, bakePart(project, child, hidden));
        }

        ModelPart modelPart = new ModelPart(cubes, children);
        PartPose pose = PartPose.offsetAndRotation(part.rotPX, part.rotPY, part.rotPZ,
                (float)Math.toRadians(part.rotAX), (float)Math.toRadians(part.rotAY), (float)Math.toRadians(part.rotAZ));
        modelPart.setInitialPose(pose);
        modelPart.loadPose(pose);
        modelPart.visible = part.showModel && !hidden.contains(part.name);
        return modelPart;
    }

    /**
     * Submits the model at the current pose. The pose is expected to be in model space (1 unit = 1 block, y down),
     * exactly like where an entity model part would render.
     */
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType, int light, int overlay, int tint, boolean foil, int outlineColor)
    {
        boolean scaled = scaleX != 1F || scaleY != 1F || scaleZ != 1F;
        if(scaled)
        {
            poseStack.pushPose();
            poseStack.scale(scaleX, scaleY, scaleZ);
        }
        for(ModelPart root : roots)
        {
            collector.submitModelPart(root, poseStack, renderType, light, overlay, null, false, foil, tint, null, outlineColor);
        }
        if(scaled)
        {
            poseStack.popPose();
        }
    }
}
