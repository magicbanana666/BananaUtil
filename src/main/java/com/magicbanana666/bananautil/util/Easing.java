package com.magicbanana666.bananautil.util;

import net.minecraft.util.Mth;

public final class Easing
{
    private Easing() {}

    /** Sine ease-in-out: 0 -> 0, 0.5 -> 0.5, 1 -> 1. */
    public static float sine(float progress)
    {
        progress = Mth.clamp(progress, 0F, 1F);
        return ((float)Math.sin(Math.toRadians(-90F + (180F * progress))) / 2F) + 0.5F;
    }

    public static float progress(float current, float total)
    {
        return total <= 0F ? 1F : Mth.clamp(current / total, 0F, 1F);
    }
}
