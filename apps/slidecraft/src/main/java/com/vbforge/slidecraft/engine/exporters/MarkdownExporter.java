package com.vbforge.slidecraft.engine.exporters;

import com.vbforge.slidecraft.model.Presentation;
import com.vbforge.slidecraft.model.Slide;

/** Markdown export: one {@code ##} section per slide, notes as a blockquote, sign in italics. */
public final class MarkdownExporter {

    private MarkdownExporter() {
    }

    public static String export(Presentation p) {
        StringBuilder md = new StringBuilder();
        md.append("# ").append(p.title()).append("\n\n");

        int n = 0;
        for (Slide s : p.slides()) {
            n++;
            md.append("## Slide ").append(n).append(": ")
                    .append(s.title().isBlank() ? "Untitled" : s.title()).append("\n\n");
            md.append(s.content()).append("\n\n");
            if (!s.notes().isBlank()) {
                String[] lines = s.notes().split("\\R", -1);
                md.append("> **Speaker Notes:** ").append(lines[0]).append('\n');
                for (int i = 1; i < lines.length; i++) {
                    md.append("> ").append(lines[i]).append('\n');
                }
                md.append('\n');
            }
            if (p.sign().shouldRender()) {
                md.append('*').append(p.sign().text()).append("*\n\n");
            }
            md.append("---\n\n");
        }
        return md.toString();
    }
}
