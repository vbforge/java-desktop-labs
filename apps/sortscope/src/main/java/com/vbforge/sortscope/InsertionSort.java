package com.vbforge.sortscope;

import javafx.animation.ParallelTransition;
import javafx.animation.Transition;
import java.util.ArrayList;
import java.util.Arrays;

public class InsertionSort extends AbstractSort {
    public InsertionSort(double dx) { super(dx); }

    @Override
    public ArrayList<Transition> startSort(CNode[] arr) {
        ArrayList<Transition> transitions = new ArrayList<>();
        
        for (int i = 1; i < arr.length; i++) {
            int j = i - 1;
            CNode key = arr[i];
            
            transitions.add(colorCNode(arr, SELECT_COLOR, i));
            ParallelTransition pt = new ParallelTransition();

            while (j >= 0) {
                comparisons++;
                if (arr[j].getValue() > key.getValue()) {
                    pt.getChildren().add(arr[j].moveX(dx));
                    arr[j + 1] = arr[j];
                    j--;
                    moves++; // FIX 1: Changed 'swaps' to 'moves'
                } else {
                    break;
                }
            }
            arr[j + 1] = key;
            pt.getChildren().add(key.moveX(dx * (j + 1 - i)));
            moves++; // FIX 2: Count the final move of the key into its new slot
            
            transitions.add(pt);
            transitions.add(colorCNode(arr, START_COLOR, j + 1));
        }
        
        transitions.add(colorCNode(Arrays.asList(arr), SORTED_COLOR));
        return transitions;
    }
}