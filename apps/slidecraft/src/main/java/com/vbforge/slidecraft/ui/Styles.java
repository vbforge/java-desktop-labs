package com.vbforge.slidecraft.ui;

import java.util.Objects;

/** Locates the editor stylesheet. */
final class Styles {

    private static final String DARK = "/com/vbforge/slidecraft/vbforge-dark.css";

    private Styles() {
    }

    static String dark() {
        return Objects.requireNonNull(Styles.class.getResource(DARK), "Missing stylesheet " + DARK).toExternalForm();
    }
}
