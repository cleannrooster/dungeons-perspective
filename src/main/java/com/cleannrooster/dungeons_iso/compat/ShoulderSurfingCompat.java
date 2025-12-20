package com.cleannrooster.dungeons_iso.compat;

import com.github.exopandora.shouldersurfing.api.client.ShoulderSurfing;
import com.github.exopandora.shouldersurfing.api.model.Perspective;

public class ShoulderSurfingCompat {
    private static Perspective previousPerspective;

    public static void onModEnabled() {
        previousPerspective = Perspective.current();
        ShoulderSurfing.getInstance().changePerspective(Perspective.THIRD_PERSON_BACK);
    }

    public static void onModDisabled() {
        ShoulderSurfing.getInstance().changePerspective(previousPerspective);
        previousPerspective = null;
    }
}
