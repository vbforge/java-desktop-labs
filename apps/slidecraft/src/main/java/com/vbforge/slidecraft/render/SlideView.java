package com.vbforge.slidecraft.render;

import com.vbforge.slidecraft.model.Presentation;
import com.vbforge.slidecraft.model.Slide;
import com.vbforge.slidecraft.model.Theme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * The single renderer for "what a slide looks like". It always lays out at a fixed logical size of
 * 1920x1080; the editor preview, the fullscreen presentation and PNG/PDF export all use this node
 * (scaled or snapshotted), so background, font, alignment and sign are applied in exactly one place.
 */
public final class SlideView extends StackPane {

    public static final double WIDTH = 1920;
    public static final double HEIGHT = 1080;

    private static final double PADDING = Math.round(WIDTH * 0.066);
    private static final double TITLE_SIZE = Math.round(WIDTH * 0.06);
    private static final double CONTENT_SIZE = Math.round(WIDTH * 0.035);
    private static final double SIGN_SIZE = Math.round(WIDTH * 0.014);

    private final Label titleLabel = new Label();
    private final Label contentLabel = new Label();
    private final Label signLabel = new Label();
    private final VBox textBox = new VBox();

    public SlideView() {
        setMinSize(WIDTH, HEIGHT);
        setPrefSize(WIDTH, HEIGHT);
        setMaxSize(WIDTH, HEIGHT);
        setClip(new Rectangle(WIDTH, HEIGHT));

        titleLabel.setWrapText(true);
        contentLabel.setWrapText(true);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        contentLabel.setMaxWidth(Double.MAX_VALUE);

        textBox.getChildren().addAll(titleLabel, contentLabel);
        textBox.setFillWidth(true);
        textBox.setMaxHeight(HEIGHT - 2 * PADDING);
        StackPane.setAlignment(textBox, Pos.CENTER);
        StackPane.setMargin(textBox, new Insets(PADDING));

        signLabel.setOpacity(0.65);
        signLabel.setMaxWidth(WIDTH * 0.7);
        // StackPane stretches the label up to its max width, so the text itself must be right-aligned
        // inside the label or it would sit at the left of the stretched box.
        signLabel.setAlignment(Pos.CENTER_RIGHT);
        signLabel.setTextAlignment(TextAlignment.RIGHT);
        signLabel.setMouseTransparent(true);
        StackPane.setAlignment(signLabel, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(signLabel, new Insets(0, PADDING, PADDING * 0.3, 0));

        getChildren().addAll(textBox, signLabel);
    }

    /** Re-renders this view for the given slide using the presentation's theme and sign. */
    public void update(Presentation presentation, Slide slide) {
        Theme theme = presentation.theme();

        // Theme values are validated in the Theme record, so they are safe to embed in inline CSS.
        setStyle("-fx-background-color: " + theme.backgroundColor() + ";");
        Color text = Color.web(theme.textColor());

        double fit = fitFactor(slide);
        String family = FontResolver.resolve(theme.fontFamily());
        titleLabel.setFont(Font.font(family, FontWeight.BOLD, TITLE_SIZE * Math.min(1.0, fit + 0.1)));
        contentLabel.setFont(Font.font(family, FontWeight.NORMAL, CONTENT_SIZE * fit));
        signLabel.setFont(Font.font(family, FontWeight.NORMAL, SIGN_SIZE));

        titleLabel.setTextFill(text);
        contentLabel.setTextFill(text);
        signLabel.setTextFill(text);
        // Inline styles outrank any stylesheet rule, so the editor's dark CSS can never recolour slide text.
        String fill = "-fx-text-fill: " + theme.textColor() + ";";
        titleLabel.setStyle(fill);
        contentLabel.setStyle(fill);
        signLabel.setStyle(fill);

        boolean left = theme.alignsLeft();
        TextAlignment alignment = left ? TextAlignment.LEFT : TextAlignment.CENTER;
        Pos labelPos = left ? Pos.CENTER_LEFT : Pos.CENTER;
        titleLabel.setTextAlignment(alignment);
        contentLabel.setTextAlignment(alignment);
        titleLabel.setAlignment(labelPos);
        contentLabel.setAlignment(labelPos);
        textBox.setAlignment(left ? Pos.CENTER_LEFT : Pos.CENTER);
        textBox.setSpacing(CONTENT_SIZE * 0.9);

        titleLabel.setText(slide.title());
        contentLabel.setText(slide.content());
        contentLabel.setLineSpacing(CONTENT_SIZE * 0.2);
        contentLabel.setVisible(!slide.content().isEmpty());
        contentLabel.setManaged(!slide.content().isEmpty());
        titleLabel.setVisible(!slide.title().isEmpty());
        titleLabel.setManaged(!slide.title().isEmpty());

        boolean showSign = presentation.sign().shouldRender();
        signLabel.setText(showSign ? presentation.sign().text() : "");
        signLabel.setVisible(showSign);
        signLabel.setManaged(showSign);
    }

    /** Shrinks text for long slides so content stays on the slide instead of overflowing. */
    private static double fitFactor(Slide slide) {
        long newlines = slide.content().chars().filter(c -> c == '\n').count();
        long weight = slide.content().length() + newlines * 40;
        if (weight > 900) {
            return 0.5;
        }
        if (weight > 600) {
            return 0.65;
        }
        if (weight > 350) {
            return 0.8;
        }
        return 1.0;
    }
}
