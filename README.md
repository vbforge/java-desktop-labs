# Java Desktop Labs

A collection of small desktop applications built in Java — JavaFX and Swing.
Each app is self-contained, open source, and runs on Windows, macOS and Linux.
Covering algorithms, databases, networking, games, productivity tools, etc.

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
| 17 | [QuizMaster](apps/quizmaster/README.md)       | JavaFX    | Interactive quiz application with SQLite and JSON import.    |
| 18 | [Word Counter](apps/word-counter/README.md)       | Swing    | A small desktop application that analyzes text and reports word statistics (with live updates).    |
| 19 | [AtlasDesk](apps/atlasdesk/README.md)       | JavaFX    | Country database with instant search, cards, flags, square km and country's position on the World Map.    |
| 20 | [TicTacPro](apps/tic-tac-pro/README.md)       | JavaFX    | Tic-Tac-Toe with Minimax AI and dark theme.    |
| 21 | [SnakeByte](apps/snakebyte/README.md)       | JavaFX    | Classic Snake game with JavaFX and an AnimationTimer.    |
| 22 | [SortScope](apps/sortscope/README.md)       | JavaFX    |  Visual sorting algorithms with real-time statistics.    |
| 23 | [PongX](apps/pongx/README.md)       | JavaFX    |  Classic Pong rebuilt with JavaFX and an AI opponent.    |
| 24 | [TeamCraft](apps/teamcraft/README.md)       | JavaFX    |  Employee management system with SQLite and CSV export.    |
| 25 | [TempTrack](apps/temptrack/README.md)       | JavaFX    |  Live weather dashboard with 7-day forecast and interactive charts.    |
| 26 | [RectSolve](apps/rectsolve/README.md)       | JavaFX    |  Solve for the missing area of a 4-way split rectangle.    |
| 27 | [DietaryTrack](apps/dietarytrack/README.md)       | JavaFX    |  Daily dietary and habit tracker with SQLite and CSV export.    |
| 28 | [SudokuForge](apps/sudokuforge/README.md)       | JavaFX    |  Sudoku generator and animated backtracking solver.    |
| 29 | [FitApp](apps/fitapp/README.md)       | JavaFX    |  Local-first fitness and workout tracker. Java 21, JavaFX 21, SQLite, JUnit 5, Maven.    |

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