// Every app in the repo gets one entry here.
// The site reads this file and builds the cards automatically.
// Keep this in sync with the folders under /apps.

const APPS = [
    {
        id: "pathlab",
        name: "PathLab",
        framework: "JavaFX",
        tagline: "Grid-based pathfinding visualiser. Draw walls with the mouse, pick an algorithm, and watch BFS, Dijkstra or A* explore the grid and trace the shortest path.",
        tags: ["JavaFX", "Canvas", "Algorithms", "Maven"],
        image: "images/pathlab.png",
        source: "https://github.com/vbforge/java-desktop-labs/tree/main/apps/pathlab",
        release: "https://github.com/vbforge/java-desktop-labs/releases/tag/pathlab-v1.0.0"
    },
    {
        id: "ledger",
        name: "Ledger",
        framework: "JavaFX",
        tagline: "Personal finance tracker with SQLite storage, an editable table view and a live spending chart. Everything stays in a local file — no account, no cloud.",
        tags: ["SQLite", "Charts", "TableView", "Maven"],
        image: "images/ledger.png",
        source: "https://github.com/vbforge/java-desktop-labs/tree/main/apps/ledger",
        release: "https://github.com/vbforge/java-desktop-labs/releases/tag/ledger-v1.0.0"
    },
    {
        id: "pixelforge",
        name: "PixelForge",
        framework: "JavaFX",
        tagline: "Interactive Mandelbrot explorer with cancellable off-thread rendering and half-resolution previews during pan and zoom.",
        tags: ["JavaFX", "Canvas", "Concurrency", "Maven"],
        image: "images/pixelforge.png",
        source: "https://github.com/vbforge/java-desktop-labs/tree/main/apps/pixelforge",
        release: "https://github.com/vbforge/java-desktop-labs/releases/tag/pixelforge-v1.0.0"
    },
    {
        id: "chatter",
        name: "Chatter",
        framework: "JavaFX",
        tagline: "Minimal LAN chat over TCP. One window can host a server, join one, or both — with a text protocol simple enough to test with telnet.",
        tags: ["Sockets", "Multithreading", "JavaFX", "Maven"],
        image: "images/chatter.png",
        source: "https://github.com/vbforge/java-desktop-labs/tree/main/apps/chatter",
        release: "https://github.com/vbforge/java-desktop-labs/releases/tag/chatter-v1.0.0"
    },
    {
        id: "kana-drill",
        name: "Kana Drill",
        framework: "Swing",
        tagline: "Spaced-repetition flashcards for hiragana and katakana. Type the romaji, get a flash of feedback, and see characters you keep forgetting come up more often.",
        tags: ["Swing", "Spaced Repetition", "Custom Painting", "Maven"],
        image: "images/kana-drill.png",
        source: "https://github.com/vbforge/java-desktop-labs/tree/main/apps/kana-drill",
        release: "https://github.com/vbforge/java-desktop-labs/releases/tag/kana-drill-v1.0.0"
    },
    {
        id: "taskflow",
        name: "TaskFlow",
        framework: "Swing",
        tagline: "Offline Kanban board with custom drag-and-drop, a live drop indicator, and full undo/redo via the command pattern. Board lives in a plain text file.",
        tags: ["Swing", "Drag & Drop", "Command Pattern", "Custom Painting"],
        image: "images/taskflow.png",
        source: "https://github.com/vbforge/java-desktop-labs/tree/main/apps/taskflow",
        release: "https://github.com/vbforge/java-desktop-labs/releases/tag/taskflow-v1.0.0"
    },
    {
        id: "contacts",
        name: "Contacts",
        framework: "JavaFX",
        tagline: "Address book with switchable storage — SQLite, CSV or JSON — plus cross-format import and export. The whole thing sits behind a two-method repository interface.",
        tags: ["JavaFX", "FXML", "SQLite", "CSV", "JSON", "Maven"],
        image: "images/contacts.png",
        source: "https://github.com/vbforge/java-desktop-labs/tree/main/apps/contacts",
        release: "https://github.com/vbforge/java-desktop-labs/releases/tag/contacts-v1.0.0"
    },
    {
        id: "air-distance",
        name: "Air Distance",
        framework: "Swing",
        tagline: "Compute remaining distance from a known distance already flown and the fraction of the trip it represents. Pure-math core, Swing front end, full JUnit test coverage.",
        tags: ["Swing", "JUnit 5", "Math", "Maven"],
        image: "images/air-distance.png",
        source: "https://github.com/vbforge/java-desktop-labs/tree/main/apps/air-distance",
        release: "https://github.com/vbforge/java-desktop-labs/releases/tag/air-distance-v1.0.0"
    },
    {
        id: "focus-sprint",
        name: "FocusSprint",
        framework: "JavaFX",
        tagline: "Track your day by section — Code, Read, Sport — with a live retrospective that updates as you work. Sessions stored in SQLite; the retrospective is a query, not a stored number.",
        tags: ["JavaFX", "SQLite", "AnimationTimer", "Maven"],
        image: "images/focus-sprint.png",
        source: "https://github.com/vbforge/java-desktop-labs/tree/main/apps/focus-sprint",
        release: "https://github.com/vbforge/java-desktop-labs/releases/tag/focus-sprint-v1.0.0"
    },
    {
        id: "diskscope",
        name: "DiskScope",
        framework: "JavaFX",
        tagline: "Interactive disk space analyzer with a drill-down donut chart. Scans folders on a background thread, groups the top 7 largest items, and lets you navigate the file system visually.",
        tags: ["JavaFX", "File I/O", "Concurrency", "Maven"],
        image: "images/diskscope.png",
        source: "https://github.com/vbforge/java-desktop-labs/tree/main/apps/diskscope",
        release: "https://github.com/vbforge/java-desktop-labs/releases/tag/diskscope-v1.0.0"
    },
    {
        id: "calcpad",
        name: "CalcPad",
        framework: "JavaFX",
        tagline: "Keyboard-friendly calculator with precise BigDecimal math, full decimal support, and no division-by-zero crashes.",
        tags: ["JavaFX", "Math", "BigDecimal", "Maven"],
        image: "images/calcpad.png",
        source: "https://github.com/vbforge/java-desktop-labs/tree/main/apps/calcpad",
        release: "https://github.com/vbforge/java-desktop-labs/releases/tag/calcpad-v1.0.0"
    },
    {
        id: "chessmate",
        name: "ChessMate",
        framework: "JavaFX",
        tagline: "Full-featured Chess with a Minimax AI, player-vs-player chess clocks, move history, and material-point scoring.",
        tags: ["JavaFX", "Chess", "AI", "Minimax", "Maven"],
        image: "images/chessmate.png",
        source: "https://github.com/vbforge/java-desktop-labs/tree/main/apps/chessmate",
        release: "https://github.com/vbforge/java-desktop-labs/releases/tag/chessmate-v1.0.0"
    },
    {
        id: "clickrush",
        name: "ClickRush",
        framework: "JavaFX",
        tagline: "Test your visual reflexes and raw clicking speed. Features a millisecond-accurate Reaction Time test and a live CPS meter, backed by a local SQLite leaderboard.",
        tags: ["JavaFX", "SQLite", "AnimationTimer", "Maven"],
        image: "images/clickrush.png",
        source: "https://github.com/vbforge/java-desktop-labs/tree/main/apps/clickrush",
        release: "https://github.com/vbforge/java-desktop-labs/releases/tag/clickrush-v1.0.0"
    },
    {
        id: "audioflow",
        name: "AudioFlow",
        framework: "JavaFX",
        tagline: "Minimal dark-themed local audio player (accepts .mp3 and .wav).",
        tags: ["JavaFX", "Media", "File I/O", "Maven"],
        image: "images/audioflow.png",
        source: "https://github.com/vbforge/java-desktop-labs/tree/main/apps/audioflow",
        release: "https://github.com/vbforge/java-desktop-labs/releases/tag/audioflow-v1.0.0"
    },
    {
        id: "homeshelf",
        name: "HomeShelf",
        framework: "JavaFX",
        tagline: "A sleek media library manager that lets you save your collection as JSON, XML, SQLite, or Binary files.",
        tags: ["JavaFX", "SQLite", "Jackson", "File I/O", "Maven"],
        image: "images/homeshelf.png",
        source: "https://github.com/vbforge/java-desktop-labs/tree/main/apps/homeshelf",
        release: "https://github.com/vbforge/java-desktop-labs/releases/tag/homeshelf-v1.0.0"
    },
    {
        id: "squarecraft",
        name: "SquareCraft",
        framework: "JavaFX",
        tagline: "A dual-mode magic square utility. Instantly generate mathematically perfect squares or solve partially filled ones.",
        tags: ["JavaFX", "Math", "JUnit 5", "Maven"],
        image: "images/squarecraft.png",
        source: "https://github.com/vbforge/java-desktop-labs/tree/main/apps/squarecraft",
        release: "https://github.com/vbforge/java-desktop-labs/releases/tag/squarecraft-v1.0.0"
    },
    {
        id: "quizmaster",
        name: "QuizMaster",
        framework: "JavaFX",
        tagline: "Interactive quiz application with SQLite storage. Import your own JSON question banks, play against a smooth 15-second timer, and review your mistakes in the retrospective.",
        tags: ["JavaFX", "SQLite", "Jackson", "JSON", "Maven"],
        image: "images/quizmaster.png",
        source: "https://github.com/vbforge/java-desktop-labs/tree/main/apps/quizmaster",
        release: "https://github.com/vbforge/java-desktop-labs/releases/tag/quizmaster-v1.0.0"
    },
    {
        id: "word-counter",
        name: "Word Counter",
        framework: "Swing",
        tagline: "Analyze text with live updates and visual charts. Filter stop words and see your true vocabulary with a dark-themed JFreeChart.",
        tags: ["Swing", "JFreeChart", "JUnit 5", "Maven"],
        image: "images/word-counter.png",
        source: "https://github.com/vbforge/java-desktop-labs/tree/main/apps/word-counter",
        release: "https://github.com/vbforge/java-desktop-labs/releases/tag/word-counter-v1.0.0"
    },
    {
        id: "atlasdesk",
        name: "AtlasDesk",
        framework: "JavaFX",
        tagline: "Browse countries with instant search, continent filters and flags. Add your own countries from JSON, then see each one on a world map.",
        tags: ["JavaFX", "SQLite", "Jackson", "Maven"],
        image: "images/atlasdesk.png",
        source: "https://github.com/vbforge/java-desktop-labs/tree/main/apps/atlasdesk",
        release: "https://github.com/vbforge/java-desktop-labs/releases/tag/atlasdesk-v1.0.0"
    },
    {
        id: "tic-tac-pro",
        name: "TicTacPro",
        framework: "JavaFX",
        tagline: "Classic Tic-Tac-Toe rebuilt with an Unbeatable Minimax AI, Player vs Player mode, and a sleek dark UI.",
        tags: ["JavaFX", "Game", "Minimax AI", "JUnit 5", "Maven"],
        image: "images/tic-tac-pro.png",
        source: "https://github.com/vbforge/java-desktop-labs/tree/main/apps/tic-tac-pro",
        release: "https://github.com/vbforge/java-desktop-labs/releases/tag/tic-tac-pro-v1.0.0"
    },
    {
        id: "snakebyte",
        name: "SnakeByte",
        framework: "JavaFX",
        tagline: "A modern rebuild of the classic Snake game. Features 5 speed levels, custom keybindings, pause menus, and smooth Canvas rendering.",
        tags: ["JavaFX", "Game", "Canvas", "JUnit 5", "Maven"],
        image: "images/snakebyte.png",
        source: "https://github.com/vbforge/java-desktop-labs/tree/main/apps/snakebyte",
        release: "https://github.com/vbforge/java-desktop-labs/releases/tag/snakebyte-v1.0.0"
    },
    {
        id: "sortscope",
        name: "SortScope",
        framework: "JavaFX",
        tagline: "An interactive educational tool to visualize 6 different sorting algorithms. Features live stats, pause/resume, and built-in Big-O theory.",
        tags: ["JavaFX", "Algorithms", "Education", "JUnit 5", "Maven"],
        image: "images/sortscope.png",
        source: "https://github.com/vbforge/java-desktop-labs/tree/main/apps/sortscope",
        release: "https://github.com/vbforge/java-desktop-labs/releases/tag/sortscope-v1.0.0"
    },
    {
        id: "pongx",
        name: "PongX",
        framework: "JavaFX",
        tagline: "Classic Pong rebuilt for the modern era. Features dynamic ball physics, local multiplayer, and an adjustable AI opponent.",
        tags: ["JavaFX", "Game", "Canvas", "JUnit 5", "Maven"],
        image: "images/pongx.png",
        source: "https://github.com/vbforge/java-desktop-labs/tree/main/apps/pongx",
        release: "https://github.com/vbforge/java-desktop-labs/releases/tag/pongx-v1.0.0"
    }
];