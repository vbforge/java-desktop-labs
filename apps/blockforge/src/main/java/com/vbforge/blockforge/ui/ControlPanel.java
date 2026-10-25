package com.vbforge.blockforge.ui;

import com.vbforge.blockforge.model.BlobGoal;
import com.vbforge.blockforge.model.Block;
import com.vbforge.blockforge.model.GameState;
import com.vbforge.blockforge.model.Goal;
import com.vbforge.blockforge.model.Move;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Right-side control panel: turn header, per-player goal cards, five move buttons,
 * and undo/redo buttons. Emits move / undo / redo events via {@link #setCallbacks}.
 */
public final class ControlPanel extends VBox {

    private final Label turnLabel = new Label();
    private final VBox goalCards = new VBox(6);
    private final Button rotateCW = new Button("Rotate CW   [1]");
    private final Button rotateCCW = new Button("Rotate CCW  [2]");
    private final Button reflectX = new Button("Reflect X   [3]");
    private final Button reflectY = new Button("Reflect Y   [4]");
    private final Button smash = new Button("Smash        [5]");
    private final Button undoBtn = new Button("Undo  [U]");
    private final Button redoBtn = new Button("Redo  [R]");

    private Consumer<Move> onMove = m -> {};
    private Runnable onUndo = () -> {};
    private Runnable onRedo = () -> {};

    public ControlPanel() {
        getStyleClass().addAll("side-panel", "right");
        setPrefWidth(280);
        setMinWidth(240);
        setSpacing(12);
        setPadding(new Insets(16));

        turnLabel.getStyleClass().add("goal-title");

        for (Button b : new Button[] { rotateCW, rotateCCW, reflectX, reflectY, smash, undoBtn, redoBtn }) {
            b.setMaxWidth(Double.MAX_VALUE);
        }

        rotateCW.setOnAction(e -> onMove.accept(Move.ROTATE_CW));
        rotateCCW.setOnAction(e -> onMove.accept(Move.ROTATE_CCW));
        reflectX.setOnAction(e -> onMove.accept(Move.REFLECT_X));
        reflectY.setOnAction(e -> onMove.accept(Move.REFLECT_Y));
        smash.setOnAction(e -> onMove.accept(Move.SMASH));
        undoBtn.setOnAction(e -> onUndo.run());
        redoBtn.setOnAction(e -> onRedo.run());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox undoRedo = new HBox(8, undoBtn, redoBtn);
        HBox.setHgrow(undoBtn, Priority.ALWAYS);
        HBox.setHgrow(redoBtn, Priority.ALWAYS);

        getChildren().addAll(
                sectionTitle("TURN"),
                turnLabel,
                sectionTitle("GOALS"),
                goalCards,
                sectionTitle("MOVES"),
                rotateCW, rotateCCW, reflectX, reflectY, smash,
                new Separator(),
                undoRedo,
                spacer
        );
    }

    public void setCallbacks(Consumer<Move> onMove, Runnable onUndo, Runnable onRedo) {
        this.onMove = onMove;
        this.onUndo = onUndo;
        this.onRedo = onRedo;
    }

    /*public void update(GameState state, Block selected, boolean canUndo, boolean canRedo) {
        if (state == null) return;

        int totalMoves = state.maxTurns() * state.playerCount();
        turnLabel.setText("Turn " + (state.movesUsed() + 1) + " of " + totalMoves
                + " — Player " + (state.turnIndex() + 1));

        goalCards.getChildren().clear();
        int[] scores = state.scores();
        for (int i = 0; i < state.playerCount(); i++) {
            goalCards.getChildren().add(
                    buildGoalCard(i, state.goals().get(i), scores[i], i == state.turnIndex()));
        }

        boolean hasSelection = selected != null;
        boolean canRotate = hasSelection && !selected.isLeaf();
        boolean canSmash = hasSelection
                && selected.level() > 0
                && selected.level() < state.maxDepth();
        rotateCW.setDisable(!canRotate);
        rotateCCW.setDisable(!canRotate);
        reflectX.setDisable(!canRotate);
        reflectY.setDisable(!canRotate);
        smash.setDisable(!canSmash);

        undoBtn.setDisable(!canUndo);
        redoBtn.setDisable(!canRedo);
    }*/

    public void update(GameState state, Block selected, boolean canUndo, boolean canRedo) {
        if (state == null) return;

        int totalMoves = state.maxTurns() * state.playerCount();
        turnLabel.setText("Turn " + Math.min(state.movesUsed() + 1, totalMoves)
                + " of " + totalMoves + " — Player " + (state.turnIndex() + 1));

        goalCards.getChildren().clear();
        int[] scores = state.scores();
        for (int i = 0; i < state.playerCount(); i++) {
            goalCards.getChildren().add(
                    buildGoalCard(i, state.goals().get(i), scores[i], i == state.turnIndex()));
        }

        boolean gameOver = state.isGameOver();
        boolean hasSelection = selected != null && !gameOver;
        boolean canRotate = hasSelection && !selected.isLeaf();
        boolean canSmash = hasSelection
                && selected.level() > 0
                && selected.level() < state.maxDepth();

        rotateCW.setDisable(!canRotate);
        rotateCCW.setDisable(!canRotate);
        reflectX.setDisable(!canRotate);
        reflectY.setDisable(!canRotate);
        smash.setDisable(!canSmash);

        undoBtn.setDisable(!canUndo);
        redoBtn.setDisable(!canRedo);
    }

    private Region buildGoalCard(int index, Goal goal, int score, boolean active) {
        VBox card = new VBox(6);
        card.getStyleClass().add("goal-card");
        if (active) card.getStyleClass().add("active");
        card.setPadding(new Insets(10));

        String type = (goal instanceof BlobGoal) ? "Blob" : "Perimeter";
        Label title = new Label("Player " + (index + 1) + " · " + type);
        title.getStyleClass().add("goal-title");

        Region swatch = new Region();
        swatch.setMinSize(12, 12);
        swatch.setPrefSize(12, 12);
        swatch.setMaxSize(12, 12);
        swatch.setStyle("-fx-background-color: " + goal.targetColor().hex()
                + "; -fx-border-color: rgba(255,255,255,0.25); -fx-border-width: 1;");

        Label target = new Label(goal.targetColor().name());
        HBox targetRow = new HBox(8, swatch, target);
        targetRow.setAlignment(Pos.CENTER_LEFT);

        Label scoreLabel = new Label("Score: " + score);
        scoreLabel.getStyleClass().add("goal-score");

        card.getChildren().addAll(title, targetRow, scoreLabel);
        return card;
    }

    private static Label sectionTitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("section-title");
        return l;
    }
}