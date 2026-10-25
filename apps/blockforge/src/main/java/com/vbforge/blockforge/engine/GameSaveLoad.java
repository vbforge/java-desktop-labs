package com.vbforge.blockforge.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vbforge.blockforge.model.BlobGoal;
import com.vbforge.blockforge.model.Block;
import com.vbforge.blockforge.model.BlockColor;
import com.vbforge.blockforge.model.GameState;
import com.vbforge.blockforge.model.Goal;
import com.vbforge.blockforge.model.PerimeterGoal;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes the {@code .blockforge} native format (JSON, UTF-8, atomic write).
 *
 * <p>Serialisation is manual rather than POJO-based, so the exact schema in §4.I of the
 * requirements doc is authoritative and unknown future fields are safely ignored on load.
 *
 * <p>Validation is all-or-nothing: if the file is invalid for any reason, {@link #fromJson(String)}
 * throws {@link GameFormatException} and no partial state is ever produced.
 */
public final class GameSaveLoad {

    public static final int CURRENT_VERSION = 1;
    public static final long MAX_FILE_BYTES = 20L * 1024 * 1024;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private GameSaveLoad() {}

    // ----------------------------------------------------------------- public API

    public static String toJson(GameState state) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("version",     CURRENT_VERSION);
        root.put("seed",        state.seed());
        root.put("rngState",    state.rngState());
        root.put("maxDepth",    state.maxDepth());
        root.put("playerCount", state.playerCount());
        root.put("maxTurns",    state.maxTurns());
        root.put("turnIndex",   state.turnIndex());
        root.put("movesUsed",   state.movesUsed());

        ArrayNode goalsArr = root.putArray("goals");
        for (Goal g : state.goals()) {
            ObjectNode gNode = goalsArr.addObject();
            if (g instanceof BlobGoal) {
                gNode.put("type", "blob");
            } else if (g instanceof PerimeterGoal) {
                gNode.put("type", "perimeter");
            } else {
                throw new IllegalStateException("Unknown goal type: " + g.getClass());
            }
            gNode.put("target", g.targetColor().name());
        }

        root.set("board", boardToJson(state.board()));

        try {
            return MAPPER.writeValueAsString(root) + "\n";
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialise game state", e);
        }
    }

    public static GameState fromJson(String json) throws GameFormatException {
        if (json == null || json.isBlank()) {
            throw new GameFormatException("File is empty");
        }
        JsonNode root;
        try {
            root = MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new GameFormatException("Malformed JSON: " + e.getOriginalMessage(), e);
        }
        if (root == null || !root.isObject()) {
            throw new GameFormatException("Root must be a JSON object");
        }

        int version = requireInt(root, "version");
        if (version != CURRENT_VERSION) {
            throw new GameFormatException("Unsupported version: " + version
                    + " (this build reads version " + CURRENT_VERSION + ")");
        }

        long seed      = requireLong(root, "seed");
        long rngState  = requireLong(root, "rngState");
        int maxDepth   = requireInt(root, "maxDepth");
        int playerCount = requireInt(root, "playerCount");
        int maxTurns   = requireInt(root, "maxTurns");
        int turnIndex  = requireInt(root, "turnIndex");
        int movesUsed  = requireInt(root, "movesUsed");

        if (maxDepth < 1 || maxDepth > 8) {
            throw new GameFormatException("maxDepth must be in [1, 8], got " + maxDepth);
        }
        if (playerCount < 1 || playerCount > 2) {
            throw new GameFormatException("playerCount must be 1 or 2, got " + playerCount);
        }
        if (maxTurns < 1) {
            throw new GameFormatException("maxTurns must be >= 1, got " + maxTurns);
        }

        JsonNode goalsNode = root.get("goals");
        if (goalsNode == null || !goalsNode.isArray()) {
            throw new GameFormatException("'goals' must be an array");
        }
        if (goalsNode.size() != playerCount) {
            throw new GameFormatException("'goals' must have " + playerCount
                    + " entries, got " + goalsNode.size());
        }
        List<Goal> goals = new ArrayList<>(playerCount);
        for (int i = 0; i < goalsNode.size(); i++) {
            goals.add(parseGoal(goalsNode.get(i), i));
        }

        JsonNode boardNode = root.get("board");
        if (boardNode == null || !boardNode.isObject()) {
            throw new GameFormatException("'board' must be an object");
        }
        Block board = boardFromJson(boardNode, 0, 0, 1 << maxDepth, 0, maxDepth);

        try {
            return new GameState(
                    seed, rngState, maxDepth, playerCount, maxTurns,
                    turnIndex, movesUsed, goals, board);
        } catch (IllegalArgumentException e) {
            // Structural fields (e.g. rngState out of range) rejected by GameState.
            throw new GameFormatException(e.getMessage(), e);
        }
    }

    public static GameState read(Path file) throws IOException {
        long size = Files.size(file);
        if (size > MAX_FILE_BYTES) {
            throw new GameFormatException(
                    "File is too large (limit " + (MAX_FILE_BYTES / 1024 / 1024) + " MB)");
        }
        return fromJson(new String(Files.readAllBytes(file), java.nio.charset.StandardCharsets.UTF_8));
    }

    /** Atomically writes the game to {@code file}. */
    public static void write(GameState state, Path file) throws IOException {
        byte[] data = toJson(state).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        writeAtomic(file, data);
    }

    // ----------------------------------------------------------------- serialisation

    private static JsonNode boardToJson(Block b) {
        ObjectNode node = MAPPER.createObjectNode();
        if (b.isLeaf()) {
            node.put("c", b.color().name());
        } else {
            ArrayNode arr = node.putArray("children");
            for (Block child : b.children()) {
                arr.add(boardToJson(child));
            }
        }
        return node;
    }

    // ----------------------------------------------------------------- parsing

    private static Block boardFromJson(JsonNode node, int x, int y, int size, int level, int maxDepth)
            throws GameFormatException {
        if (node == null || !node.isObject()) {
            throw new GameFormatException("board node at level " + level + " is not an object");
        }

        JsonNode cNode = node.get("c");
        JsonNode childrenNode = node.get("children");

        boolean hasColor = cNode != null && !cNode.isNull();
        boolean hasChildrenArray = childrenNode != null && childrenNode.isArray();

        if (hasColor && hasChildrenArray && childrenNode.size() > 0) {
            throw new GameFormatException(
                    "board node at level " + level + " has both 'c' and non-empty 'children'");
        }
        if (!hasColor && !hasChildrenArray) {
            throw new GameFormatException(
                    "board node at level " + level + " has neither 'c' nor 'children'");
        }

        if (hasColor) {
            BlockColor color;
            try {
                color = BlockColor.valueOf(cNode.asText());
            } catch (IllegalArgumentException e) {
                throw new GameFormatException(
                        "board node at level " + level + " has unknown color '" + cNode.asText() + "'");
            }
            return Block.leaf(x, y, size, level, maxDepth, color);
        }

        // Internal node
        if (level >= maxDepth) {
            throw new GameFormatException(
                    "board node at level " + level + " is internal but maxDepth is " + maxDepth);
        }
        if (childrenNode.size() != 4) {
            throw new GameFormatException(
                    "internal board node at level " + level
                            + " must have exactly 4 children, got " + childrenNode.size());
        }

        int half = size / 2;
        Block[] kids = new Block[4];
        kids[Block.UR] = boardFromJson(childrenNode.get(0), x + half, y,        half, level + 1, maxDepth);
        kids[Block.UL] = boardFromJson(childrenNode.get(1), x,        y,        half, level + 1, maxDepth);
        kids[Block.LL] = boardFromJson(childrenNode.get(2), x,        y + half, half, level + 1, maxDepth);
        kids[Block.LR] = boardFromJson(childrenNode.get(3), x + half, y + half, half, level + 1, maxDepth);
        return Block.internal(x, y, size, level, maxDepth, kids);
    }

    private static Goal parseGoal(JsonNode node, int index) throws GameFormatException {
        if (node == null || !node.isObject()) {
            throw new GameFormatException("goals[" + index + "] must be an object");
        }
        JsonNode typeNode = node.get("type");
        if (typeNode == null || !typeNode.isTextual()) {
            throw new GameFormatException("goals[" + index + "].type must be a string");
        }
        JsonNode targetNode = node.get("target");
        if (targetNode == null || !targetNode.isTextual()) {
            throw new GameFormatException("goals[" + index + "].target must be a string");
        }
        BlockColor color;
        try {
            color = BlockColor.valueOf(targetNode.asText());
        } catch (IllegalArgumentException e) {
            throw new GameFormatException(
                    "goals[" + index + "].target: unknown color '" + targetNode.asText() + "'");
        }
        return switch (typeNode.asText()) {
            case "blob" -> new BlobGoal(color);
            case "perimeter" -> new PerimeterGoal(color);
            default -> throw new GameFormatException(
                    "goals[" + index + "].type: unknown goal type '" + typeNode.asText() + "'");
        };
    }

    private static int requireInt(JsonNode root, String field) throws GameFormatException {
        JsonNode n = root.get(field);
        if (n == null || !n.isIntegralNumber()) {
            throw new GameFormatException("Missing or invalid '" + field + "'");
        }
        return n.intValue();
    }

    private static long requireLong(JsonNode root, String field) throws GameFormatException {
        JsonNode n = root.get(field);
        if (n == null || !n.isIntegralNumber()) {
            throw new GameFormatException("Missing or invalid '" + field + "'");
        }
        return n.longValue();
    }

    // ----------------------------------------------------------------- atomic write

    private static void writeAtomic(Path target, byte[] data) throws IOException {
        Path absolute = target.toAbsolutePath();
        Path dir = absolute.getParent();
        Files.createDirectories(dir);
        Path tmp = Files.createTempFile(dir, ".blockforge-", ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(tmp)) {
                out.write(data);
            }
            try {
                Files.move(tmp, absolute,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}