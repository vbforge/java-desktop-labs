# Guide: Adding a New Sorting Algorithm to SortScope

This document provides a step-by-step guide on how to integrate a new sorting algorithm into the SortScope application. 

Because of the app's decoupled architecture, you do not need to touch the UI code to write the algorithm. The algorithm simply mutates a `CNode[]` array and returns a list of JavaFX `Transition` objects, which the UI will handle automatically.

## Architecture Overview

Before adding a new algorithm, understand these core components:
- **`CNode`**: A custom class extending `javafx.scene.shape.Rectangle`. It holds an integer `value` and has a `moveX(double x)` method to animate movement.
- **`AbstractSort`**: The base class. It provides helper methods for generating animations (`colorCNode`, `swap`) and tracks the `comparisons` and `moves` statistics.
- **`SortScopeApp`**: The UI layer. It listens for the "Sort" button, instantiates the selected algorithm, and plays the returned transitions.

---

## Step 1: Create the Algorithm Class

Create a new Java class in `src/main/java/com/vbforge/sortscope/`. 
For this example, we will add **Shell Sort**.

```java
package com.vbforge.sortscope;

import javafx.animation.Transition;
import java.util.ArrayList;
import java.util.Arrays;

public class ShellSort extends AbstractSort {
    
    // Constructor must pass 'dx' to the parent AbstractSort
    public ShellSort(double dx) {
        super(dx);
    }

    @Override
    public ArrayList<Transition> startSort(CNode[] arr) {
        ArrayList<Transition> transitions = new ArrayList<>();
        int n = arr.length;

        // Start with a big gap, then reduce the gap
        for (int gap = n / 2; gap > 0; gap /= 2) {
            
            for (int i = gap; i < n; i += 1) {
                // Save the current element
                CNode temp = arr[i];
                int j;
                
                // Compare elements up to 'gap' distance apart
                for (j = i; j >= gap; j -= gap) {
                    comparisons++; // Track comparison
                    
                    transitions.add(colorCNode(arr, SELECT_COLOR, j - gap, j));
                    
                    if (arr[j - gap].getValue() > temp.getValue()) {
                        // Shift element to the right
                        arr[j] = arr[j - gap];
                        transitions.add(arr[j].moveX(dx * gap));
                        moves++; // Track the move
                    } else {
                        break;
                    }
                }
                
                // Put temp in its correct location
                if (j != i) {
                    arr[j] = temp;
                    transitions.add(temp.moveX(dx * (i - j)));
                    moves++;
                }
                
                transitions.add(colorCNode(arr, START_COLOR));
            }
        }

        // At the end, color the whole array green
        transitions.add(colorCNode(Arrays.asList(arr), SORTED_COLOR));
        return transitions;
    }
}
```

### Important Rules for Step 1:
1. **Always call `super(dx)`** in the constructor.
2. **Mutate the `arr` array directly.** Do not create a new array and return it. The UI is visually attached to the specific `CNode` objects inside the original `arr`.
3. **Return `ArrayList<Transition>`**. Every time you compare, swap, or shift, add the corresponding animation to this list.
4. **Increment `comparisons` and `moves`.** Use the inherited variables to keep the stats panel accurate. Use the `swap()` helper method when swapping, or manually increment `moves` when shifting/overwriting.
5. **Color your elements.** Use `colorCNode(arr, SELECT_COLOR, index)` to highlight active elements, and reset them to `START_COLOR` when done.

---

## Step 2: Register the Algorithm in the UI

Open `SortScopeApp.java` and find the `buildSidebar()` method. Add your algorithm's name to both ComboBoxes:

```java
    // In buildSidebar()
    algoCombo = new ComboBox<>();
    algoCombo.getItems().addAll(
        "Bubble Sort", 
        "Selection Sort", 
        "Insertion Sort", 
        "Merge Sort", 
        "Quick Sort", 
        "Heap Sort",
        "Shell Sort" // <--- Add here
    );

    // ... further down in the same method ...

    theoryCombo = new ComboBox<>();
    theoryCombo.getItems().addAll(
        "Bubble Sort", 
        "Selection Sort", 
        "Insertion Sort", 
        "Merge Sort", 
        "Quick Sort", 
        "Heap Sort",
        "Shell Sort" // <--- Add here
    );
```

### Update the Switch Statement
Still in `SortScopeApp.java`, scroll down to the `startSort()` method and add your algorithm to the `switch` block:

```java
    AbstractSort sorter = switch (algoCombo.getValue()) {
        case "Bubble Sort" -> new BubbleSort(dx);
        case "Selection Sort" -> new SelectionSort(dx);
        case "Insertion Sort" -> new InsertionSort(dx);
        case "Merge Sort" -> new MergeSort(dx);
        case "Quick Sort" -> new QuickSort(dx);
        case "Heap Sort" -> new HeapSort(dx);
        case "Shell Sort" -> new ShellSort(dx); // <--- Add here
        default -> new BubbleSort(dx);
    };
```

---

## Step 3: Add the Theory Text

Open `SortScopeApp.java` and find the `initTheoryData()` method. Add an entry to the `theoryMap` for your new algorithm:

```java
    private void initTheoryData() {
        // ... existing theories ...

        theoryMap.put("Shell Sort", 
            "SHELL SORT\n\n" +
            "How it works:\n" +
            "Shell Sort is an optimization of Insertion Sort. It starts by sorting pairs of elements far apart from each other, then progressively reduces the gap between elements to be sorted. By the time the gap reaches 1, the array is almost sorted, making the final Insertion Sort pass very fast.\n\n" +
            "Complexity:\n" +
            "• Time: O(n log n) average, O(n²) worst\n" +
            "• Space: O(1)\n\n" +
            "Fun Fact:\n" +
            "The exact time complexity of Shell Sort depends heavily on the sequence of gaps chosen. The gap sequence n/2, n/4, ... 1 used in this app is the original sequence proposed by Donald Shell in 1959.");
    }
```

---

## Step 4: Write a Unit Test

Open `src/test/java/com/vbforge/sortscope/SortEngineTest.java` and add a new test method to verify the algorithm works:

```java
    @Test
    void testShellSort() {
        // Create an array of 50 random elements
        CNode[] arr = createArray(50, 48L); 
        
        // Run the algorithm (ignoring the transitions for testing)
        new ShellSort(10).startSort(arr);
        
        // Assert the array is sorted in ascending order
        assertSorted(arr);
    }
```

---

## Step 5: Verify & Release

1. Run the app using `mvn clean javafx:run`.
2. Select your new algorithm from the dropdown.
3. Click **Sort** and verify that the animation runs smoothly, the elements end up perfectly sorted left-to-right, and the stats panel correctly shows comparisons and moves.
4. Run `mvn test` to ensure all unit tests pass.
5. Update the `README.md` to include the new algorithm in the features list.

---

### Why this process is so clean:
You'll notice we didn't have to change the layout, the JavaFX animation controller, 
the timer, or the stats panel. By enforcing that every algorithm extends `AbstractSort` and 
returns a list of `Transition`s, the UI remains entirely agnostic to *how* the algorithm works. 
**It just plays the tape!**