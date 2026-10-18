# SquareCraft

> Magic Square Generator and Solver with a dual-mode JavaFX interface.

![SquareCraft screenshot](../../docs/images/squarecraft.png)

![Java](https://img.shields.io/badge/Java-21-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-21-blue)
![JUnit 5](https://img.shields.io/badge/Tests-JUnit%205-3fb950)
![License](https://img.shields.io/badge/license-MIT-green)

A math utility for 3x3 magic squares. Enter a partially filled grid and the
app will calculate the missing numbers to find the "Magic Constant". Or,
switch to the Generator tab to instantly create mathematically perfect
magic squares using the Siam method.

## Features

- **Solver Mode**: Fill in known numbers, and the app calculates the rest.
- **Generator Mode**: Create valid magic squares starting from any number.
- **Verification**: Toggle a detailed breakdown of the math for every row, column, and diagonal.
- **Built-in Help**: Explains the algorithms in plain English.
- **Strict Validation**: Prevents invalid characters from being entered.
- **Unit Tested**: Core math engine is verified with JUnit 5.

## Tech

Java 21 · JavaFX 21 · JUnit 5 · Maven

## The Engine

The UI is completely separated from the logic. The `MagicSquareEngine` class
is a pure Java class with no JavaFX dependencies, making it easily testable
with JUnit 5.

The **Solver** works iteratively:
1. Finds any complete line to determine the Magic Constant.
2. Finds lines with 1 missing number and subtracts the known sum from the constant.
3. Repeats until complete.

The **Generator** uses the Siam method:
1. Place the starting number in the top-middle cell.
2. Move up-right for the next number, wrapping around edges.
3. If the target cell is occupied, move down one cell instead.

## Run it

**From a release** (needs Java 21+ only):

```bash
java -jar squarecraft-1.0.0-win.jar
```

**From source** (needs JDK 21 and Maven):

```bash
cd apps/squarecraft
mvn clean javafx:run
```

**Run the tests:**

```bash
mvn test
```

## What I learned

- **Separating the engine makes JUnit testing trivial.** By putting the math
  logic into `MagicSquareEngine` (with no JavaFX dependencies), I could write
  tests that run in milliseconds without needing to spin up a UI thread.

- **`TextFormatter` is the correct way to restrict input.** Instead of checking
  input on submission, using `setTextFormatter` allows you to filter out
  invalid characters before they even appear in the text field. This prevents
  the user from typing letters or symbols into the number grid.

- **Customizing `TabPane` in JavaFX is surprisingly tricky.** The default
  `.tab-header-background` has a bright grey line that clashes with dark
  themes. It requires deep CSS targeting (`.custom-tabs .tab-header-area 
  .tab-header-background`) to strip out the default borders and apply the
  dark theme properly.

- **Modal windows need explicit sizing.** When building custom dialogs,
  `Scene` sizing is strictly enforced. If you add a lot of text without
  increasing the `Scene` dimensions or setting a `setMaxWidth` on the text
  label, the text gets cut off.

## License

MIT — see [LICENSE](../../LICENSE).