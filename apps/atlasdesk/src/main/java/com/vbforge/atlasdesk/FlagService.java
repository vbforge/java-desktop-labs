package com.vbforge.atlasdesk;

import javafx.scene.image.Image;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Loads and caches flag images. Supported flagcdn widths: 20, 40, 80, 160, 320 ... */
public final class FlagService {

    private static final Map<String, Image> CACHE = new ConcurrentHashMap<>();

    private FlagService() {}

    public static Image get(String code, int width) {
        if (code == null || code.isBlank()) return null;
        String c = code.toLowerCase(Locale.ROOT);
        return CACHE.computeIfAbsent(c + "@" + width,
                k -> new Image("https://flagcdn.com/w" + width + "/" + c + ".png", true));
    }
}