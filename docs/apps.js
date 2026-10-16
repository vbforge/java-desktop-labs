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
    }
];