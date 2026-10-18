# Java Desktop Labs

A collection of small desktop applications built in Java — JavaFX and Swing.
Each app is self-contained, open source, and runs on Windows, macOS and Linux.

**→ [Browse the apps](https://vbforge.github.io/java-desktop-labs/)**

## Apps

| #  | App                                         | Framework | What it does                                                                                                       |
|----|---------------------------------------------|-----------|--------------------------------------------------------------------------------------------------------------------|
| 1  | [PathLab](apps/pathlab/README.md)           | JavaFX    | Grid-based pathfinding visualiser (BFS, Dijkstra, A*).                                                             |
| 2  | [Ledger](apps/ledger/README.md)             | JavaFX    | Personal finance tracker with SQLite storage, CSV-ready table views and a live spending chart.                     |
| 3  | [Pixelforge](apps/pixelforge/README.md)     | JavaFX    | Interactive Mandelbrot explorer with off-thread rendering.                                                         |
| 4  | [Chatter](apps/chatter/README.md)           | JavaFX    | LAN chat client and server over TCP.                                                                               |
| 5  | [Kana Drill](apps/kana-drill/README.md)     | Swing     | Spaced-repetition flashcards for hiragana and katakana.                                                            |
| 6  | [TaskFlow](apps/taskflow/README.md)         | Swing     | Offline Kanban board with custom drag-and-drop, a live drop indicator, and full undo/redo via the command pattern. |
| 7  | [Contacts](apps/contacts/README.md)         | JavaFX    | Address book with switchable SQLite / CSV / JSON storage.                                                          |
| 8  | [Air Distance](apps/air-distance/README.md) | Swing     | Compute remaining distance from a known distance and fraction flown.                                               |
| 9  | [FocusSprint](apps/focus-sprint/README.md)  | JavaFX    | Single-day focus tracker with live retrospective.                                                                  |
| 10 | [DiskScope](apps/diskscope/README.md)       | JavaFX    | Interactive disk space analyzer.                                                                                   |
| 11 | [CalcPad](apps/calcpad/README.md)           | JavaFX    | Keyboard-friendly calculator with BigDecimal precision.                                                            |
| 12 | [ChessMate](apps/chessmate/README.md)       | JavaFX    | Full-featured Chess with a Minimax AI, player-vs-player chess clocks, move history, and material-point scoring.    |
| 13 | [ClickRush](apps/clickrush/README.md)       | JavaFX    | Reflex and clicking speed trainer with local high scores.    |
| 14 | [AudioFlow](apps/audioflow/README.md)       | JavaFX    | Minimal dark-themed local audio player (accepts .mp3 and .wav).    |
| 15 | [HomeShelf](apps/homeshelf/README.md)       | JavaFX    | A sleek media library manager that lets you save your collection as JSON, XML, SQLite, or Binary files.    |
| 16 | [SquareCraft](apps/squarecraft/README.md)       | JavaFX    | Magic Square (3x3) Generator and Solver with a dual-mode JavaFX interface (generate math perfect squares or solve partially filled ones).    |

## Repository layout

```
apps/          one folder per application, each with its own pom.xml
docs/          GitHub Pages site (served from /docs on main)
.github/       CI workflows
```

## Running an app

Each app's README has exact instructions. In general:

**Download a release** — grab the jar from the
[Releases page](https://github.com/vbforge/java-desktop-labs/releases)
and run it with Java 21+:

```bash
java -jar pathlab-1.0.0-win.jar
```

**Build from source** — clone the repo and use Maven:

```bash
cd apps/pathlab
mvn clean javafx:run
```

## License

MIT — see [LICENSE](LICENSE).