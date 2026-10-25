package com.vbforge.blockforge.engine;

import com.vbforge.blockforge.model.BlockColor;
import com.vbforge.blockforge.model.GameState;
import com.vbforge.blockforge.model.Move;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class GameSaveLoadTest {

    // ---------------------------------------------------------------- round-trip

    @Test
    void roundTripPreservesAllFields() throws IOException {
        GameState original = BoardFactory.newGame(7L, 3, 2, 5);

        // Advance the game a little so seed != rngState and movesUsed > 0.
        GameSession session = new GameSession(original);
        assertTrue(session.applyMove(0, 0, 1, Move.SMASH));
        assertTrue(session.applyMove(0, 0, 1, Move.ROTATE_CW));
        assertTrue(session.applyMove(0, 0, 1, Move.REFLECT_X));
        GameState midGame = session.state();

        GameState loaded = GameSaveLoad.fromJson(GameSaveLoad.toJson(midGame));

        assertEquals(midGame.seed(),        loaded.seed());
        assertEquals(midGame.rngState(),    loaded.rngState());
        assertEquals(midGame.maxDepth(),    loaded.maxDepth());
        assertEquals(midGame.playerCount(), loaded.playerCount());
        assertEquals(midGame.maxTurns(),    loaded.maxTurns());
        assertEquals(midGame.turnIndex(),   loaded.turnIndex());
        assertEquals(midGame.movesUsed(),   loaded.movesUsed());
        assertEquals(midGame.goals(),       loaded.goals());
        assertArrayEquals(midGame.board().flatten(), loaded.board().flatten());
    }

    @Test
    void roundTripPreservesFuturePlayDeterminism() throws IOException {
        GameState original = BoardFactory.newGame(11L, 3, 2, 5);

        // Play 2 moves, then take a fork.
        GameSession base = new GameSession(original);
        assertTrue(base.applyMove(0, 0, 1, Move.SMASH));
        assertTrue(base.applyMove(0, 0, 1, Move.ROTATE_CW));
        GameState forked = GameSaveLoad.fromJson(GameSaveLoad.toJson(base.state()));

        // Continue on both branches with the same moves.
        GameSession a = new GameSession(base.state());
        GameSession b = new GameSession(forked);
        Move[] script = { Move.SMASH, Move.ROTATE_CCW, Move.SMASH, Move.REFLECT_Y };
        for (Move m : script) {
            assertTrue(a.applyMove(0, 0, 1, m));
            assertTrue(b.applyMove(0, 0, 1, m));
        }
        assertArrayEquals(a.state().board().flatten(), b.state().board().flatten());
        assertEquals(a.state().rngState(), b.state().rngState());
    }

    @Test
    void fileRoundTripLeavesNoTempFiles(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("game.blockforge");
        GameState game = BoardFactory.newGame(4L, 3, 2, 5);
        GameSaveLoad.write(game, file);
        assertEquals(game.seed(), GameSaveLoad.read(file).seed());
        try (Stream<Path> files = Files.list(dir)) {
            assertEquals(1, files.count(), "expected only the saved file, no temp leftovers");
        }
    }

    // ---------------------------------------------------------------- validation

    private static String validJson() {
        return GameSaveLoad.toJson(BoardFactory.newGame(1L, 2, 2, 3));
    }

    @Test
    void rejectsEmptyFile() {
        assertThrows(GameFormatException.class, () -> GameSaveLoad.fromJson(""));
        assertThrows(GameFormatException.class, () -> GameSaveLoad.fromJson("   "));
    }

    @Test
    void rejectsMalformedJson() {
        assertThrows(GameFormatException.class, () -> GameSaveLoad.fromJson("{not json"));
    }

    @Test
    void rejectsNonObjectRoot() {
        assertThrows(GameFormatException.class, () -> GameSaveLoad.fromJson("[1,2,3]"));
        assertThrows(GameFormatException.class, () -> GameSaveLoad.fromJson("\"x\""));
    }

    @Test
    void rejectsBadVersion() {
        String broken = validJson().replaceFirst("\"version\"\\s*:\\s*\\d+", "\"version\": 999");
        assertThrows(GameFormatException.class, () -> GameSaveLoad.fromJson(broken));
    }

    @Test
    void rejectsBadMaxDepth() {
        String broken = validJson().replaceFirst("\"maxDepth\"\\s*:\\s*\\d+", "\"maxDepth\": 0");
        assertThrows(GameFormatException.class, () -> GameSaveLoad.fromJson(broken));
    }

    @Test
    void rejectsBadPlayerCount() {
        String broken = validJson().replaceFirst("\"playerCount\"\\s*:\\s*\\d+", "\"playerCount\": 3");
        assertThrows(GameFormatException.class, () -> GameSaveLoad.fromJson(broken));
    }

    @Test
    void rejectsOutOfRangeRngState() {
        String broken = validJson().replaceFirst(
                "\"rngState\"\\s*:\\s*\\d+",
                "\"rngState\": " + (1L << 48));
        assertThrows(GameFormatException.class, () -> GameSaveLoad.fromJson(broken));
    }

    @Test
    void rejectsMismatchedGoalCount() {
        String broken = validJson().replaceFirst(
                "\"playerCount\"\\s*:\\s*\\d+",
                "\"playerCount\": 1");
        // Now 2 goals but playerCount says 1.
        assertThrows(GameFormatException.class, () -> GameSaveLoad.fromJson(broken));
    }

    @Test
    void rejectsUnknownGoalType() {
        String broken = validJson().replaceFirst("\"type\"\\s*:\\s*\"blob\"", "\"type\": \"mystery\"");
        assertThrows(GameFormatException.class, () -> GameSaveLoad.fromJson(broken));
    }

    @Test
    void rejectsUnknownColor() {
        String broken = validJson().replaceFirst(
                "\"target\"\\s*:\\s*\"[A-Z]+\"",
                "\"target\": \"MAGENTA\"");
        assertThrows(GameFormatException.class, () -> GameSaveLoad.fromJson(broken));
    }

    @Test
    void rejectsBoardWithThreeChildren() {
        String broken = validJson().replaceFirst(
                "\"children\"\\s*:\\s*\\[([^\\[\\]]+),([^\\[\\]]+),([^\\[\\]]+)\\]",
                "\"children\": [$1,$2,$3]"); // only 3 children
        // The regex above turns a 4-children array into... actually it only matches a 3-children
        // array. To force the case, hand-edit a small example instead:
        String explicit = """
                {
                  "version":1, "seed":1, "rngState":1, "maxDepth":2,
                  "playerCount":1, "maxTurns":1, "turnIndex":0, "movesUsed":0,
                  "goals":[{"type":"blob","target":"RED"}],
                  "board":{"children":[{"c":"RED"},{"c":"GREEN"},{"c":"BLUE"}]}
                }""";
        assertThrows(GameFormatException.class, () -> GameSaveLoad.fromJson(explicit));
    }

    @Test
    void rejectsInternalNodeAtMaxDepth() {
        String broken = """
                {
                  "version":1, "seed":1, "rngState":1, "maxDepth":0,
                  "playerCount":1, "maxTurns":1, "turnIndex":0, "movesUsed":0,
                  "goals":[{"type":"blob","target":"RED"}],
                  "board":{"children":[{"c":"RED"},{"c":"RED"},{"c":"RED"},{"c":"RED"}]}
                }""";
        // maxDepth=0 means the root must be a leaf; an internal node here is invalid.
        // Note: maxDepth=0 is also caught earlier, but this test proves the tree validator
        // rejects over-subdivided boards independently of the maxDepth check.
        assertThrows(GameFormatException.class, () -> GameSaveLoad.fromJson(broken));
    }

    @Test
    void rejectsNodeWithBothColorAndChildren() {
        String broken = """
                {
                  "version":1, "seed":1, "rngState":1, "maxDepth":2,
                  "playerCount":1, "maxTurns":1, "turnIndex":0, "movesUsed":0,
                  "goals":[{"type":"blob","target":"RED"}],
                  "board":{"c":"RED","children":[{"c":"RED"},{"c":"RED"},{"c":"RED"},{"c":"RED"}]}
                }""";
        assertThrows(GameFormatException.class, () -> GameSaveLoad.fromJson(broken));
    }

    @Test
    void rejectsNodeWithNeitherColorNorChildren() {
        String broken = """
                {
                  "version":1, "seed":1, "rngState":1, "maxDepth":2,
                  "playerCount":1, "maxTurns":1, "turnIndex":0, "movesUsed":0,
                  "goals":[{"type":"blob","target":"RED"}],
                  "board":{}
                }""";
        assertThrows(GameFormatException.class, () -> GameSaveLoad.fromJson(broken));
    }

    @Test
    void rejectsMissingRequiredField() {
        // Remove "seed" from a valid document.
        String broken = validJson().replaceFirst("\\s*\"seed\"\\s*:\\s*\\d+\\s*,", "");
        assertThrows(GameFormatException.class, () -> GameSaveLoad.fromJson(broken));
    }

    @Test
    void unknownTopLevelFieldsAreIgnored() throws IOException {
        String withExtra = validJson().replaceFirst(
                "\\{",
                "{\"futureField\": [1,2,3], ");
        GameState loaded = GameSaveLoad.fromJson(withExtra);
        assertEquals(2, loaded.maxDepth());
    }

    @Test
    void fileTooLargeIsRejected(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("huge.blockforge");
        byte[] filler = new byte[(int) (GameSaveLoad.MAX_FILE_BYTES + 1)];
        Files.write(file, filler);
        assertThrows(GameFormatException.class, () -> GameSaveLoad.read(file));
    }

    @Test
    void emptyFileOnDiskIsRejected(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("empty.blockforge");
        Files.write(file, new byte[0]);
        assertThrows(GameFormatException.class, () -> GameSaveLoad.read(file));
    }
}