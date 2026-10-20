package com.vbforge.sortscope;

import javafx.animation.Transition;
import java.util.ArrayList;
import java.util.Arrays;

public class BubbleSort extends AbstractSort {
    public BubbleSort(double dx) { super(dx); }

    @Override
    public ArrayList<Transition> startSort(CNode[] arr) {
        ArrayList<Transition> transitions = new ArrayList<>();
        boolean swapped;
        
        for (int i = 0; i < arr.length - 1; i++) {
            swapped = false;
            for (int j = 0; j < arr.length - 1 - i; j++) {
                comparisons++;
                
                // Highlight comparing elements
                transitions.add(colorCNode(arr, SELECT_COLOR, j, j + 1));
                
                // Check and swap
                if (arr[j].getValue() > arr[j + 1].getValue()) {
                    transitions.add(swap(arr, j, j + 1));
                    swapped = true;
                }
                
                // Reset colors
                transitions.add(colorCNode(arr, START_COLOR, j, j + 1));
            }
            
            // Mark the last sorted element
            transitions.add(colorCNode(arr, SORTED_COLOR, arr.length - 1 - i));

            // Early exit if no swaps were made (array is sorted)
            if (!swapped) break;
        }
        
        transitions.add(colorCNode(Arrays.asList(arr), SORTED_COLOR));
        return transitions;
    }
}