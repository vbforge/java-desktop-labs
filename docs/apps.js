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
    }
];