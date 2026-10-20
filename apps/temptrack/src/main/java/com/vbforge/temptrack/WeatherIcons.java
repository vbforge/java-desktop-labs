package com.vbforge.temptrack;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

/**
 * Vector weather icons drawn with JavaFX shapes.
 *
 * Why not emoji? JavaFX does not render colour emoji reliably (they come out as
 * flat gray/black glyphs or empty boxes depending on the OS/font), so we draw
 * simple line icons instead. Colours come from temptrack.css (.wx-* classes),
 * so the icons always match the app palette.
 */
public final class WeatherIcons {

    private static final double GRID = 24;

    private static final String SUN = "wx-sun";
    private static final String CLOUD = "wx-cloud";
    private static final String WATER = "wx-water";
    private static final String SNOW = "wx-snow";

    private static final String P_CLOUD = "M18 10h-1.26A8 8 0 1 0 9 20h9a5 5 0 0 0 0-10z";
    private static final String P_CLOUD_TOP = "M20 16.58A5 5 0 0 0 18 7h-1.26A8 8 0 1 0 4 15.25";
    private static final String P_CLOUD_BOLT_TOP = "M19 16.9A5 5 0 0 0 18 7h-1.26a8 8 0 1 0-11.62 9";
    private static final String P_CLOUD_SNOW_TOP = "M20 17.58A5 5 0 0 0 18 8h-1.26A8 8 0 1 0 4 16.25";
    private static final String P_RAYS =
            "M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2"
                    + "M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42";

    private WeatherIcons() {}

    // ---------- public API ----------

    /** Icon for an Open-Meteo / WMO weather code. */
    public static Node forCode(int code, double size) {
        return switch (code) {
            case 0, 1 -> sun(size);
            case 2 -> partlyCloudy(size);
            case 3 -> cloud(size);
            case 45, 48 -> fog(size);
            case 51, 53, 55, 56, 57 -> drizzle(size);
            case 61, 63, 65, 66, 67, 80, 81, 82 -> rain(size);
            case 71, 73, 75, 77, 85, 86 -> snow(size);
            case 95, 96, 99 -> storm(size);
            default -> cloud(size);
        };
    }

    /** Human-readable text for a weather code. */
    public static String describe(int code) {
        return switch (code) {
            case 0 -> "Clear sky";
            case 1 -> "Mainly clear";
            case 2 -> "Partly cloudy";
            case 3 -> "Overcast";
            case 45, 48 -> "Fog";
            case 51, 53, 55 -> "Drizzle";
            case 56, 57 -> "Freezing drizzle";
            case 61, 63, 65 -> "Rain";
            case 66, 67 -> "Freezing rain";
            case 71, 73, 75 -> "Snowfall";
            case 77 -> "Snow grains";
            case 80, 81, 82 -> "Rain showers";
            case 85, 86 -> "Snow showers";
            case 95 -> "Thunderstorm";
            case 96, 99 -> "Thunderstorm with hail";
            default -> "Unknown";
        };
    }

    public static Node sun(double size) {
        return box(size, circle(12, 12, 5, SUN), path(P_RAYS, SUN));
    }

    public static Node partlyCloudy(double size) {
        SVGPath cloud = path(P_CLOUD, CLOUD);
        cloud.getTransforms().addAll(new Translate(6.2, 7.6), new Scale(0.74, 0.74));
        return box(size,
                circle(7.5, 7.5, 3, SUN),
                path("M7.5 1v1.2M1 7.5h1.2M2.9 2.9l.85.85M12.1 2.9l-.85.85M2.9 12.1l.85-.85", SUN),
                cloud);
    }

    public static Node cloud(double size) {
        return box(size, path(P_CLOUD, CLOUD));
    }

    public static Node fog(double size) {
        return box(size, path("M5 8h14M3 12h18M5 16h14M8 20h8", CLOUD));
    }

    public static Node drizzle(double size) {
        return box(size,
                path(P_CLOUD_TOP, CLOUD),
                path("M8 19v2M8 13v2M16 19v2M16 13v2M12 21v2M12 15v2", WATER));
    }

    public static Node rain(double size) {
        return box(size,
                path(P_CLOUD_TOP, CLOUD),
                path("M16 13v8M8 13v8M12 15v8", WATER));
    }

    public static Node snow(double size) {
        return box(size,
                path(P_CLOUD_SNOW_TOP, CLOUD),
                circle(8, 16, 0.5, SNOW), circle(8, 20, 0.5, SNOW),
                circle(12, 18, 0.5, SNOW), circle(12, 22, 0.5, SNOW),
                circle(16, 16, 0.5, SNOW), circle(16, 20, 0.5, SNOW));
    }

    public static Node storm(double size) {
        return box(size,
                path(P_CLOUD_BOLT_TOP, CLOUD),
                path("M13 11l-4 6h6l-4 6", SUN));
    }

    public static Node wind(double size) {
        return box(size, path(
                "M9.59 4.59A2 2 0 1 1 11 8H2m10.59 11.41A2 2 0 1 0 14 16H2"
                        + "m15.73-8.27A2.5 2.5 0 1 1 19.5 12H2", CLOUD));
    }

    public static Node humidity(double size) {
        return box(size, path("M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z", WATER));
    }

    // ---------- internals ----------

    private static <T extends Shape> T style(T shape, String cssClass) {
        shape.setFill(null);
        shape.setStrokeWidth(2);
        shape.setStrokeLineCap(StrokeLineCap.ROUND);
        shape.setStrokeLineJoin(StrokeLineJoin.ROUND);
        shape.getStyleClass().add(cssClass); // stroke colour comes from CSS
        return shape;
    }

    private static SVGPath path(String d, String cssClass) {
        SVGPath p = new SVGPath();
        p.setContent(d);
        return style(p, cssClass);
    }

    private static Circle circle(double cx, double cy, double r, String cssClass) {
        return style(new Circle(cx, cy, r), cssClass);
    }

    /** Scales the 24x24 drawing to the requested size inside a fixed-size pane (so layout stays stable). */
    private static Node box(double size, Node... parts) {
        Group g = new Group(parts);
        double k = size / GRID;
        g.getTransforms().add(new Scale(k, k, 0, 0));

        Pane pane = new Pane(g);
        pane.setMinSize(size, size);
        pane.setPrefSize(size, size);
        pane.setMaxSize(size, size);
        pane.setMouseTransparent(true);
        return pane;
    }
}
