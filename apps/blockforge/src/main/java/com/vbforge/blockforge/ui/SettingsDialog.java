package com.vbforge.blockforge.ui;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
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
import javafx.stage.Window;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Modal for starting a new game.
 *
 * <p>Fields: max depth (2–6), player count (1 or 2), turns per player (1–50), and seed
 * (blank = random; otherwise decimal or {@code 0x} hex).
 *
 * <p>The OK button is disabled while any field is invalid.
 */
public final class SettingsDialog extends Dialog<SettingsDialog.Settings> {

    private static final String STYLESHEET = "/com/vbforge/blockforge/vbforge-dark.css";

    public record Settings(long seed, int maxDepth, int playerCount, int maxTurns) {}

    private final TextField depthField = new TextField();
    private final RadioButton oneP = new RadioButton("1 player");
    private final RadioButton twoP = new RadioButton("2 players");
    private final TextField turnsField = new TextField();
    private final TextField seedField = new TextField();
    private final Label hint = new Label();

    public SettingsDialog(long defaultSeed, int defaultDepth, int defaultPlayers, int defaultTurns) {
        initOwner((Window) null);
        setTitle("New Game");
        setHeaderText(null);
        getDialogPane().getStylesheets().add(
                Objects.requireNonNull(
                        SettingsDialog.class.getResource(STYLESHEET),
                        "Missing stylesheet " + STYLESHEET).toExternalForm());

        ButtonType startType = new ButtonType("Start Game", ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(startType, ButtonType.CANCEL);

        depthField.setText(String.valueOf(defaultDepth));
        turnsField.setText(String.valueOf(defaultTurns));
        seedField.setText(Long.toString(defaultSeed));
        seedField.setPromptText("blank = random");

        ToggleGroup players = new ToggleGroup();
        oneP.setToggleGroup(players);
        twoP.setToggleGroup(players);
        (defaultPlayers == 1 ? oneP : twoP).setSelected(true);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        ColumnConstraints labels = new ColumnConstraints();
        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(labels, fields);
        form.addRow(0, fieldLabel("Max depth (2–6)"), depthField);
        form.addRow(1, fieldLabel("Players"), new HBox(16, oneP, twoP));
        form.addRow(2, fieldLabel("Turns per player"), turnsField);
        form.addRow(3, fieldLabel("Seed"), seedField);

        hint.getStyleClass().add("field-label");
        hint.setText("Seed: decimal (e.g. 2) or hex (0x2a). Blank = random.");

        VBox box = new VBox(12, form, hint);
        box.setPadding(new Insets(4, 0, 0, 0));
        getDialogPane().setContent(box);

        Node okBtn = getDialogPane().lookupButton(startType);
        okBtn.disableProperty().bind(Bindings.createBooleanBinding(
                () -> !isValid(),
                depthField.textProperty(),
                turnsField.textProperty(),
                seedField.textProperty(),
                players.selectedToggleProperty()));

        setResultConverter(button -> button == startType ? build() : null);
        Platform.runLater(depthField::requestFocus);
    }

    private boolean isValid() {
        try {
            parseDepth();
            parseTurns();
            parseSeed();
            return oneP.isSelected() || twoP.isSelected();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private Settings build() {
        return new Settings(
                parseSeed(),
                parseDepth(),
                oneP.isSelected() ? 1 : 2,
                parseTurns());
    }

    private int parseDepth() {
        int d = Integer.parseInt(depthField.getText().trim());
        if (d < 2 || d > 6) {
            throw new IllegalArgumentException("depth out of range");
        }
        return d;
    }

    private int parseTurns() {
        int t = Integer.parseInt(turnsField.getText().trim());
        if (t < 1 || t > 50) {
            throw new IllegalArgumentException("turns out of range");
        }
        return t;
    }

    private long parseSeed() {
        String s = seedField.getText().trim();
        if (s.isEmpty()) {
            return ThreadLocalRandom.current().nextLong();
        }
        if (s.startsWith("0x") || s.startsWith("0X")) {
            return Long.parseUnsignedLong(s.substring(2), 16);
        }
        return Long.parseLong(s);
    }

    private static Label fieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        return label;
    }
}