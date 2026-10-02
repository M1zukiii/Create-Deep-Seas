package com.maxenonyme.createsubmarine.submarine.client;

import com.maxenonyme.createsubmarine.submarine.config.SubmarineConfig;
import foundry.veil.Veil;

public final class VeilLights {
    private VeilLights() {
    }

    public static boolean usable() {
        if (Veil.IRIS)
            return false;
        return !SubmarineConfig.CLIENT_SPEC.isLoaded() || SubmarineConfig.VEIL_LIGHTS.get();
    }

    public static boolean usableForAlarm() {
        if (!usable())
            return false;
        return !SubmarineConfig.CLIENT_SPEC.isLoaded() || !SubmarineConfig.PHOTOSENSITIVE_MODE.get();
    }
}
