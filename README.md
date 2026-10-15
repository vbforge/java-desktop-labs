# Java Desktop Labs

A collection of small desktop applications built in Java — JavaFX and Swing.
Each app is self-contained, open source, and runs on Windows, macOS and Linux.

**→ [Browse the apps](https://vbforge.github.io/java-desktop-labs/)**

## Apps

| App                     | Framework | What it does                                          |
|-------------------------|-----------|-------------------------------------------------------|
| [PathLab](apps/pathlab) | JavaFX    | Grid-based pathfinding visualiser (BFS, Dijkstra, A*). |
| [Ledger](apps/ledger) | JavaFX    | Personal finance tracker with SQLite storage, CSV-ready table views and a live spending chart. |
| [Ledger](apps/pixelforge) | JavaFX    | Interactive Mandelbrot explorer with off-thread rendering. |

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