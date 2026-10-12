package com.magicbanana666.bananautil.tabula;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.magicbanana666.bananautil.BananaUtil;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Reads Tabula .tbl files: a zip with a model.json (the project) and a texture png.
 */
public final class TabulaFile
{
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private TabulaFile() {}

    @Nullable
    public static TabulaProject read(Path file)
    {
        try
        {
            return read(Files.readAllBytes(file));
        }
        catch(IOException | JsonParseException e)
        {
            BananaUtil.LOGGER.warn("Could not read Tabula file {}: {}", file, e.toString());
            return null;
        }
    }

    @Nullable
    public static TabulaProject read(byte[] tblBytes) throws IOException
    {
        byte[] json = null;
        byte[] texture = null;
        try(ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(tblBytes)))
        {
            ZipEntry entry;
            while((entry = zip.getNextEntry()) != null)
            {
                String name = entry.getName();
                if(name.equals("model.json"))
                {
                    json = zip.readAllBytes();
                }
                else if(name.toLowerCase().endsWith(".png") && texture == null)
                {
                    texture = zip.readAllBytes();
                }
            }
        }

        if(json == null)
        {
            return null;
        }

        TabulaProject project = GSON.fromJson(new InputStreamReader(new ByteArrayInputStream(json), StandardCharsets.UTF_8), TabulaProject.class);
        if(project == null || project.parts == null)
        {
            return null;
        }
        project.textureBytes = texture;
        return project;
    }

    @Nullable
    public static TabulaProject read(InputStream in) throws IOException
    {
        return read(in.readAllBytes());
    }
}
