package com.modfast;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Modfast implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("modfast");

    @Override
    public void onInitialize() {
        LOGGER.info("Fastboot initialized! Your game will launch like a bullet.");
    }
}
