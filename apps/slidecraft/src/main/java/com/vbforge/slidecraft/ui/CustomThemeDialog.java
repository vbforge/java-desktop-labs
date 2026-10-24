package com.vbforge.slidecraft.ui;

import com.vbforge.slidecraft.model.Presentation;
import com.vbforge.slidecraft.model.Sign;
import com.vbforge.slidecraft.model.Slide;
import com.vbforge.slidecraft.model.Theme;
import com.vbforge.slidecraft.render.SlideCanvas;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Modal for creating a custom theme, with a live preview. */
public final class CustomThemeDialog extends Dialog<Theme> {

    private final TextField name = new TextField();
    private final ColorPicker background;
    private final ColorPicker text;
    private final ComboBox<String> font = new ComboBox<>();
    private final RadioButton left = new RadioButton("Left");
    private final RadioButton center = new RadioButton("Center");
    private final SlideCanvas preview = new SlideCanvas();

    public CustomThemeDialog(Window owner, Theme initial) {
        initOwner(owner);
        setTitle("Create Custom Theme");
        setHeaderText(null);
        getDialogPane().getStylesheets().add(Styles.dark());

        ButtonType saveType = new ButtonType("Save Theme", ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        background = new ColorPicker(Color.web(initial.backgroundColor()));
        text = new ColorPicker(Color.web(initial.textColor()));

        name.setPromptText("e.g. Ocean Night");
        font.setItems(FXCollections.observableArrayList(fontChoices(initial.fontFamily())));
        font.setValue(initial.fontFamily());
        font.setMaxWidth(Double.MAX_VALUE);
        background.setMaxWidth(Double.MAX_VALUE);
        text.setMaxWidth(Double.MAX_VALUE);

        ToggleGroup alignment = new ToggleGroup();
        left.setToggleGroup(alignment);
        center.setToggleGroup(alignment);
        (initial.alignsLeft() ? left : center).setSelected(true);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        ColumnConstraints labels = new ColumnConstraints();
        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(labels, fields);
        form.addRow(0, fieldLabel("Name"), name);
        form.addRow(1, fieldLabel("Background"), background);
        form.addRow(2, fieldLabel("Text colour"), text);
        form.addRow(3, fieldLabel("Font"), font);
        form.addRow(4, fieldLabel("Alignment"), new HBox(16, left, center));

        preview.setPrefSize(400, 225);
        preview.setMinSize(400, 225);
        VBox box = new VBox(16, form, preview);
        box.setPadding(new Insets(4, 0, 0, 0));
        box.setAlignment(Pos.TOP_LEFT);
        getDialogPane().setContent(box);

        Node saveButton = getDialogPane().lookupButton(saveType);
        saveButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> name.getText() == null || name.getText().isBlank(), name.textProperty()));

        Runnable refresh = this::refreshPreview;
        background.valueProperty().addListener((o, a, b) -> refresh.run());
        text.valueProperty().addListener((o, a, b) -> refresh.run());
        font.valueProperty().addListener((o, a, b) -> refresh.run());
        alignment.selectedToggleProperty().addListener((o, a, b) -> refresh.run());
        refreshPreview();

        setResultConverter(button -> button == saveType ? build() : null);
        javafx.application.Platform.runLater(name::requestFocus);
    }

    private Theme build() {
        return new Theme(
                name.getText().trim(),
                toHex(background.getValue()),
                toHex(text.getValue()),
                font.getValue(),
                left.isSelected() ? Theme.LEFT : Theme.CENTER);
    }

    private void refreshPreview() {
        Theme theme = new Theme("preview", toHex(background.getValue()), toHex(text.getValue()),
                font.getValue(), left.isSelected() ? Theme.LEFT : Theme.CENTER);
        Presentation sample = new Presentation("Preview", theme, new Sign(true, "© Your Name"), List.of());
        preview.show(sample, new Slide("Sample title", "This is how your content will look with this theme.", ""));
    }

    private static Label fieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        return label;
    }

    private static List<String> fontChoices(String current) {
        List<String> families = new ArrayList<>(Font.getFamilies());
        families.sort(String.CASE_INSENSITIVE_ORDER);
        Set<String> ordered = new LinkedHashSet<>();
        ordered.add("System");
        ordered.add("Monospaced");
        ordered.add(current);
        ordered.addAll(families);
        return new ArrayList<>(ordered);
    }

    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x",
                Math.round(c.getRed() * 255), Math.round(c.getGreen() * 255), Math.round(c.getBlue() * 255));
    }
}
