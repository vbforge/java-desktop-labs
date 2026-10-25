package com.vbforge.blockforge.ui;

import com.vbforge.blockforge.model.Block;
import com.vbforge.blockforge.model.GameState;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Bottom bar showing turn, current selection and board dimensions.
 */
public final class StatusBar extends HBox {

    private final Label left = new Label();
    private final Label right = new Label();
    private final Region spacer = new Region();

    public StatusBar() {
        getStyleClass().add("status-bar");
        setSpacing(16);
        setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        getChildren().addAll(left, spacer, right);
    }

    /*public void update(GameState state, Block selected) {
        if (state == null) {
            left.setText("");
            right.setText("");
            return;
        }

        int totalMoves = state.maxTurns() * state.playerCount();
        StringBuilder sb = new StringBuilder();
        sb.append("Turn ").append(state.movesUsed() + 1)
          .append(" of ").append(totalMoves)
          .append(" — Player ").append(state.turnIndex() + 1);

        if (selected != null) {
            String kind = selected.isLeaf()
                    ? selected.color().name()
                    : "internal";
            sb.append(" · Selected: level ").append(selected.level())
              .append(" (").append(selected.size()).append('×').append(selected.size())
              .append(' ').append(kind).append(')');
        }
        left.setText(sb.toString());

        int cells = 1 << state.maxDepth();
        right.setText("seed " + state.seed()
                + " · " + cells + "×" + cells
                + " · maxDepth " + state.maxDepth());
    }*/

    public void update(GameState state, Block selected) {
        if (state == null) {
            left.setText("");
            right.setText("");
            return;
        }

        if (state.isGameOver()) {
            int[] scores = state.scores();
            String msg;
            if (state.playerCount() == 1) {
                msg = "Game over — final score " + scores[0];
            } else if (scores[0] == scores[1]) {
                msg = "Game over — it's a tie at " + scores[0];
            } else {
                int winner = scores[0] > scores[1] ? 1 : 2;
                msg = "Game over — Player " + winner + " wins! "
                        + scores[0] + " : " + scores[1];
            }
            left.setText(msg);
        } else {
            int totalMoves = state.maxTurns() * state.playerCount();
            StringBuilder sb = new StringBuilder();
            sb.append("Turn ").append(state.movesUsed() + 1)
                    .append(" of ").append(totalMoves)
                    .append(" — Player ").append(state.turnIndex() + 1);

            if (selected != null) {
                String kind = selected.isLeaf() ? selected.color().name() : "internal";
                sb.append(" · Selected: level ").append(selected.level())
                        .append(" (").append(selected.size()).append('×').append(selected.size())
                        .append(' ').append(kind).append(')');
            }
            left.setText(sb.toString());
        }

        int cells = 1 << state.maxDepth();
        right.setText("seed " + state.seed()
                + " · " + cells + "×" + cells
                + " · maxDepth " + state.maxDepth());
    }

}