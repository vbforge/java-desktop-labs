package com.vbforge.slidecraft.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;

/** Immutable presentation: one theme, one sign, an ordered list of slides. */
@JsonPropertyOrder({"title", "theme", "sign", "slides"})
public record Presentation(String title, Theme theme, Sign sign, List<Slide> slides) {

    public static final String DEFAULT_TITLE = "Untitled Presentation";

    public Presentation {
        title = (title == null || title.isBlank()) ? DEFAULT_TITLE : title.trim();
        theme = theme == null ? Theme.DEFAULT : theme;
        sign = sign == null ? Sign.NONE : sign;
        slides = slides == null ? List.of() : List.copyOf(slides);
    }

    /** A new presentation with the default theme, no sign and one starter slide. */
    public static Presentation blank() {
        return new Presentation(
                DEFAULT_TITLE,
                Theme.DEFAULT,
                Sign.NONE,
                List.of(new Slide("Welcome to SlideCraft", "Write your content. The theme does the rest.", "")));
    }

    public Presentation withTitle(String newTitle) {
        return new Presentation(newTitle, theme, sign, slides);
    }

    public Presentation withTheme(Theme newTheme) {
        return new Presentation(title, newTheme, sign, slides);
    }

    public Presentation withSign(Sign newSign) {
        return new Presentation(title, theme, newSign, slides);
    }

    public Presentation withSlides(List<Slide> newSlides) {
        return new Presentation(title, theme, sign, newSlides);
    }

    public Presentation withSlide(int index, Slide slide) {
        List<Slide> copy = new ArrayList<>(slides);
        copy.set(index, slide);
        return withSlides(copy);
    }

    public Presentation withSlideInserted(int index, Slide slide) {
        List<Slide> copy = new ArrayList<>(slides);
        copy.add(index, slide);
        return withSlides(copy);
    }

    public Presentation withSlideRemoved(int index) {
        List<Slide> copy = new ArrayList<>(slides);
        copy.remove(index);
        return withSlides(copy);
    }
}
