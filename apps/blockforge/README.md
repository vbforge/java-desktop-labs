# BlockForge

Mondrian quad-tree puzzle game (JavaFX 21, Java 21). Two players (or one, solo) take turns
smashing, rotating and reflecting coloured blocks to chase a randomly-assigned goal.

Rebuilt from the classic "A Game of Blocks" assignment as a proper desktop app — same rules,
same seeds, same boards — with a modern JavaFX UI, snapshot-based undo/redo, and portable
save files.

## Run / build

Requires JDK 21 and Maven 3.9+.

```bash
mvn javafx:run              # run from source
mvn verify                  # compile + unit tests + fat jar
java -jar target/blockforge-1.0.0-all.jar
```

The fat jar contains the JavaFX natives of the OS it was built on, so build on each target OS.
For a native installer (needs the JDK's `jpackage`):

```bash
jpackage --type app-image --name BlockForge --input target \
  --main-jar blockforge-1.0.0-all.jar --main-class com.vbforge.blockforge.Launcher
```

### Tests

`mvn verify` runs all JUnit 5 tests. The suite covers the model, engine, and save/load layer;
it does not start JavaFX, so it runs headless on any machine.

Reference tests pin the assignment's published examples:

- seed 2, maxDepth 2 → the exact `RRGG / RRGG / YYRB / YYYB` grid
- seed 123, maxDepth 4 → blob GREEN = 65, perimeter GREEN = 24

## Features

- **Quad-tree board** generated from a single `long` seed with the original
  `exp(-0.25 * level)` subdivision policy. Same seed + same moves ⇒ byte-identical board.
- **Two goal types**: largest 4-neighbour blob of a target colour, or highest target-colour
  count on the perimeter (corners double).
- **Five moves**: rotate CW, rotate CCW, reflect X, reflect Y, smash. Rotations and reflections
  recurse into every child; smash replaces the block's children with four fresh ones.
- **Full-time-travel undo/redo**: every snapshot restores both the board *and* the RNG state,
  so undo of a smash followed by redo reproduces the exact same board.
- **Save/load** in a portable `.blockforge` JSON format (includes the RNG state, so mid-game
  resume is exact).
- **Keyboard-driven**: arrows to change selection level, `1`–`5` for moves, `U`/`R` for
  undo/redo, `Ctrl+N`/`O`/`S` for file ops, `F11` for fullscreen.
- **1-player and 2-player** modes. Two-player games guarantee distinct goal colours.
- **Game-over dialog** with final scores, tie handling, and re-entry on new move.

## Keyboard shortcuts

| Key | Action |
|-----|--------|
| `↑` / `↓` | Grow / shrink selection |
| `1` / `2` | Rotate CW / CCW |
| `3` / `4` | Reflect X / Reflect Y |
| `5` | Smash |
| `U` / `Ctrl+Z` | Undo |
| `R` / `Ctrl+Y` | Redo |
| `Ctrl+N` | New Game |
| `Ctrl+O` | Open |
| `Ctrl+S` / `Ctrl+Shift+S` | Save / Save As |
| `F11` | Fullscreen |

## File format

`.blockforge` is JSON, UTF-8, atomic write (temp file + move).

```json
{
  "version": 1,
  "seed": 2,
  "rngState": 1234567890123,
  "maxDepth": 3,
  "playerCount": 2,
  "maxTurns": 5,
  "turnIndex": 1,
  "movesUsed": 4,
  "goals": [
    { "type": "blob",      "target": "GREEN" },
    { "type": "perimeter", "target": "RED"   }
  ],
  "board": { "children": [
    { "c": "RED" },
    { "children": [ { "c": "GREEN" }, { "c": "RED" }, { "c": "BLUE" }, { "c": "YELLOW" } ] },
    { "c": "BLUE" },
    { "c": "YELLOW" }
  ]}
}
```

Validation on load is strict: version, `maxDepth ∈ [1, 8]`, `playerCount ∈ {1, 2}`,
`goals.length == playerCount`, each goal type ∈ `{blob, perimeter}`, each target colour ∈
`{RED, GREEN, BLUE, YELLOW}`, and the board tree must be internally consistent (internal
nodes have exactly 4 children, leaves have exactly one colour, internal nodes never at max
depth). An invalid file is rejected as a whole; no partial state is ever produced.

`samples/hello-2x2.blockforge` is a minimal valid file you can load to verify the format.

## Design notes

- **`JavaUtilRandomCompat`** replicates `java.util.Random`'s LCG exactly, so seeding the app
  with the same `long` as the assignment produces the same board. State is one `long`, making
  snapshot/restore trivial.
- **`Block` is mutable** by design. Undo/redo is provided by `GameSession` snapshots at the
  game-state level, not by making `Block` immutable.
- **Scoring** operates on a flattened `BlockColor[2^maxDepth][2^maxDepth]` grid. Blob uses a
  4-neighbour flood fill; perimeter is a straight edge scan with corner cells counted twice.
- **The renderer** (`BoardRenderer`) paints leaves and grid lines directly onto a JavaFX
  `Canvas`, scaled from unit-cell coordinates to pixels. Board renders crisply at any window
  size and on HiDPI.

## Out of scope for v1

- AI player
- Custom board palettes
- Online multiplayer
- Variable-size or non-square boards
- Localization