# Java Desktop Labs

A collection of small desktop applications built in Java — JavaFX and Swing.
Each app is self-contained, open source, and runs on Windows, macOS and Linux.

**→ [Browse the apps](https://vbforge.github.io/java-desktop-labs/)**

## Apps

| App                                         | Framework | What it does                                                                                                       |
|---------------------------------------------|-----------|--------------------------------------------------------------------------------------------------------------------|
| [PathLab](apps/pathlab/README.md)           | JavaFX    | Grid-based pathfinding visualiser (BFS, Dijkstra, A*).                                                             |
| [Ledger](apps/ledger/README.md)             | JavaFX    | Personal finance tracker with SQLite storage, CSV-ready table views and a live spending chart.                     |
| [Pixelforge](apps/pixelforge/README.md)     | JavaFX    | Interactive Mandelbrot explorer with off-thread rendering.                                                         |
| [Chatter](apps/chatter/README.md)           | JavaFX    | LAN chat client and server over TCP.                                                                               |
| [Kana Drill](apps/kana-drill/README.md)     | Swing     | Spaced-repetition flashcards for hiragana and katakana.                                                            |
| [TaskFlow](apps/taskflow/README.md)         | Swing     | Offline Kanban board with custom drag-and-drop, a live drop indicator, and full undo/redo via the command pattern. |
| [Contacts](apps/contacts/README.md)         | JavaFX    | Address book with switchable SQLite / CSV / JSON storage.                                                          |
| [Air Distance](apps/air-distance/README.md) | Swing     | Compute remaining distance from a known distance and fraction flown.                                               |

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