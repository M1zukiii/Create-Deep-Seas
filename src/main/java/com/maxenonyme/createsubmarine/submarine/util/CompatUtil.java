package com.maxenonyme.createsubmarine.submarine.util;

import net.neoforged.fml.ModList;

public class CompatUtil {
    private static Boolean sodiumLoaded = null;

    public static boolean isSodiumLoaded() {
        if (sodiumLoaded == null) {
            sodiumLoaded = ModList.get().isLoaded("sodium");
        }
        return sodiumLoaded;
    }
}
