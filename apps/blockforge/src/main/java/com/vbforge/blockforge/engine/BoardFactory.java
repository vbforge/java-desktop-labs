package com.vbforge.blockforge.engine;

import com.vbforge.blockforge.model.BlobGoal;
import com.vbforge.blockforge.model.Block;
import com.vbforge.blockforge.model.BlockColor;
import com.vbforge.blockforge.model.GameState;
import com.vbforge.blockforge.model.Goal;
import com.vbforge.blockforge.model.PerimeterGoal;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates a fresh game from a seed. The whole session — board and goals — comes from a single
 * RNG, matching the original assignment's generation order.
 */
public final class BoardFactory {

    private BoardFactory() {}

    /**
     * Creates a new game.
     *
     * <p>RNG consumption order (matches the assignment so seeds produce identical outcomes):
     * <ol>
     *   <li>Board: {@code new Block(0, maxDepth, rng)} — consumes per the subdivision policy.</li>
     *   <li>Player 1: {@code nextDouble()} (goal type), then {@code nextInt(4)} (color).</li>
     *   <li>Player 2: {@code nextDouble()}, then {@code nextInt(4)} retried until distinct color.</li>
     * </ol>
     *
     * @throws IllegalArgumentException if any of the parameters is out of range
     */
    public static GameState newGame(long seed, int maxDepth, int playerCount, int maxTurns) {
        if (maxDepth < 1 || maxDepth > 8) {
            throw new IllegalArgumentException("maxDepth must be in [1, 8], got " + maxDepth);
        }
        if (playerCount < 1 || playerCount > 2) {
            throw new IllegalArgumentException("playerCount must be 1 or 2, got " + playerCount);
        }
        if (maxTurns < 1) {
            throw new IllegalArgumentException("maxTurns must be >= 1, got " + maxTurns);
        }

        JavaUtilRandomCompat rng = new JavaUtilRandomCompat(seed);

        Block board = new Block(0, maxDepth, rng);
        board.updateSizeAndPosition(1 << maxDepth, 0, 0);

        List<Goal> goals = new ArrayList<>(playerCount);

        // Player 1: matching assignment order — nextDouble() first, then nextInt().
        double p1Type = rng.nextDouble();
        int p1Color = rng.nextInt(BlockColor.values().length);
        goals.add(p1Type < 0.5
                ? new BlobGoal(BlockColor.values()[p1Color])
                : new PerimeterGoal(BlockColor.values()[p1Color]));

        if (playerCount == 2) {
            double p2Type = rng.nextDouble();
            int p2Color = rng.nextInt(BlockColor.values().length);
            while (p2Color == p1Color) {
                p2Color = rng.nextInt(BlockColor.values().length);
            }
            goals.add(p2Type < 0.5
                    ? new BlobGoal(BlockColor.values()[p2Color])
                    : new PerimeterGoal(BlockColor.values()[p2Color]));
        }

        return new GameState(
                seed,
                rng.state(),
                maxDepth,
                playerCount,
                maxTurns,
                0,
                0,
                goals,
                board
        );
    }
}