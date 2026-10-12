package com.magicbanana666.bananautil.tabula;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A Tabula (projVersion 5) model project, as stored in the model.json of a .tbl file.
 * Plain data so it can be read on a dedicated server too; client baking lives in client.model.TabulaModelBaker.
 */
public class TabulaProject
{
    public String author = "";
    public int projVersion = 5;
    public List<String> notes = new ArrayList<>();
    public float scaleX = 1F;
    public float scaleY = 1F;
    public float scaleZ = 1F;
    public int texWidth = 64;
    public int texHeight = 32;
    public String textureFile;
    public List<Part> parts = new ArrayList<>();
    public String identifier;
    public String name = "";

    /** The raw PNG of the texture, if the .tbl contained one. */
    public transient byte[] textureBytes;

    public List<Part> getAllParts()
    {
        List<Part> all = new ArrayList<>();
        for(Part part : parts)
        {
            part.visit(all::add);
        }
        return all;
    }

    public static class Part
    {
        public List<String> notes = new ArrayList<>();
        public int texWidth = 64;
        public int texHeight = 32;
        public boolean matchProject = true;
        public int texOffX;
        public int texOffY;
        public float rotPX;
        public float rotPY;
        public float rotPZ;
        public float rotAX;
        public float rotAY;
        public float rotAZ;
        public boolean mirror;
        public boolean showModel = true;
        public List<Box> boxes = new ArrayList<>();
        public List<Part> children = new ArrayList<>();
        public String identifier;
        public String name = "";

        public void visit(Consumer<Part> consumer)
        {
            consumer.accept(this);
            for(Part child : children)
            {
                child.visit(consumer);
            }
        }
    }

    public static class Box
    {
        public float posX;
        public float posY;
        public float posZ;
        public float dimX;
        public float dimY;
        public float dimZ;
        public float expandX;
        public float expandY;
        public float expandZ;
        public int texOffX;
        public int texOffY;
        public String identifier;
        public String name = "";
    }
}
