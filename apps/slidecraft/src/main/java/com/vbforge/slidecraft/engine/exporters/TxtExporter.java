package com.vbforge.slidecraft.engine.exporters;

import com.vbforge.slidecraft.model.Presentation;
import com.vbforge.slidecraft.model.Slide;

/** Plain-text export: human-readable and diff-friendly. */
public final class TxtExporter {

    private TxtExporter() {
    }

    public static String export(Presentation p) {
        StringBuilder out = new StringBuilder();
        out.append("PRESENTATION: ").append(p.title()).append('\n');
        out.append("=".repeat(Math.max(p.title().length() + 14, 20))).append("\n\n");

        int n = 0;
        for (Slide s : p.slides()) {
            n++;
            out.append("--- Slide ").append(n).append(" ---\n");
            out.append(s.title()).append("\n\n");
            out.append(s.content()).append("\n\n");
            if (!s.notes().isBlank()) {
                out.append("Speaker Notes: ").append(s.notes()).append("\n\n");
            }
            if (p.sign().shouldRender()) {
                out.append("Footer: ").append(p.sign().text()).append('\n');
            }
            out.append('\n');
        }
        return out.toString();
    }
}
