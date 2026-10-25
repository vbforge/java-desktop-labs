package com.vbforge.blockforge.engine;

import com.vbforge.blockforge.model.Block;
import com.vbforge.blockforge.model.GameState;
import com.vbforge.blockforge.model.Move;

/**
 * Stateless move applier.
 *
 * <p>Given a state, a target coordinate and a move, returns either the new state or a rejection.
 * The input {@link GameState#board()} is <strong>never mutated</strong>: the engine deep-copies
 * the board, applies the move to the copy, and returns a new {@code GameState} holding the copy.
 * This makes it safe to store the input state in an undo stack.
 *
 * <p>RNG state is threaded through: the pre-move {@code rngState} is loaded into a fresh
 * {@link JavaUtilRandomCompat}, the move is applied (smash may consume values), and the post-move
 * state is written into the returned {@code GameState}.
 */
public final class MoveEngine {

    private MoveEngine() {}

    /**
     * Applies a move at the given unit coordinate and level.
     *
     * @param state  the current game state (not mutated)
     * @param unitX  x coordinate of the selected point, in unit cells
     * @param unitY  y coordinate of the selected point, in unit cells
     * @param level  desired level of the selection; the closest available level is used
     * @param move   which move to apply
     * @return accepted + new state, or rejected + the input state
     */
    public static MoveResult apply(GameState state, int unitX, int unitY, int level, Move move) {
        // 1. Deep-copy the board so the caller's state is untouched.
        Block board = state.board().copy();

        // 2. Resolve the selected block on the copy.
        Block selected = board.getSelectedBlock(unitX, unitY, level);
        if (selected == null) {
            return new MoveResult(false, state);
        }

        // 3. Load the RNG.
        JavaUtilRandomCompat rng = new JavaUtilRandomCompat(0L);
        rng.setState(state.rngState());

        // 4. Perform the move. Rotations/reflections require children; smash may or may not.
        boolean acted = switch (move) {
            case ROTATE_CW -> {
                if (selected.children().length > 0) {
                    selected.rotate(1);
                    yield true;
                }
                yield false;
            }
            case ROTATE_CCW -> {
                if (selected.children().length > 0) {
                    selected.rotate(0);
                    yield true;
                }
                yield false;
            }
            case REFLECT_X -> {
                if (selected.children().length > 0) {
                    selected.reflect(0);
                    yield true;
                }
                yield false;
            }
            case REFLECT_Y -> {
                if (selected.children().length > 0) {
                    selected.reflect(1);
                    yield true;
                }
                yield false;
            }
            case SMASH -> selected.smash(rng);
        };

        if (!acted) {
            return new MoveResult(false, state);
        }

        // 5. Build the new state.
        int nextTurn = state.playerCount() > 1
                ? (state.turnIndex() + 1) % state.playerCount()
                : state.turnIndex();

        GameState next = new GameState(
                state.seed(),
                rng.state(),
                state.maxDepth(),
                state.playerCount(),
                state.maxTurns(),
                nextTurn,
                state.movesUsed() + 1,
                state.goals(),
                board
        );

        return new MoveResult(true, next);
    }
}