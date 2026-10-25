package com.vbforge.blockforge.engine;

import com.vbforge.blockforge.model.GameState;

/**
 * Result of an attempted move.
 *
 * <p>If rejected, {@link #stateAfter()} is the same instance as the input state, so callers can
 * cheaply detect "nothing happened" via {@code result.stateAfter() == original}.
 */
public record MoveResult(boolean accepted, GameState stateAfter) {}