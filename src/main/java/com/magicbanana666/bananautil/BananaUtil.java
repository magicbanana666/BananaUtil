package com.magicbanana666.bananautil;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * BananaUtil: the small shared library behind Hats Reinvented (Tabula models, head locating, easing, GUI entity rendering).
 */
@Mod(BananaUtil.MOD_ID)
public final class BananaUtil
{
    public static final String MOD_ID = "bananautil";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BananaUtil(IEventBus modBus, ModContainer container)
    {
    }
}
