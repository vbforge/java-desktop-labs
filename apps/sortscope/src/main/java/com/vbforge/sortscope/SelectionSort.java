package com.vbforge.sortscope;

import javafx.animation.ParallelTransition;
import javafx.animation.Transition;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.Arrays;

public class SelectionSort extends AbstractSort {
    private static final Color MIN_INDX_COLOR = Color.web("#a371f7");

    public SelectionSort(double dx) { super(dx); }

    private ParallelTransition colorCNode(CNode[] arr, int a, int b, Color colorA, Color colorB) {
        ParallelTransition pt = new ParallelTransition();
        pt.getChildren().addAll(colorCNode(arr, colorA, a), colorCNode(arr, colorB, b));
        return pt;
    }

    @Override
    public ArrayList<Transition> startSort(CNode[] arr) {
        ArrayList<Transition> transitions = new ArrayList<>();
        
        for (int i = 0; i < arr.length - 1; i++) {
            int minIndx = i;
            transitions.add(colorCNode(arr, MIN_INDX_COLOR, minIndx));

            for (int j = i + 1; j < arr.length; j++) {
                comparisons++;
                transitions.add(colorCNode(arr, SELECT_COLOR, j));
                
                if (arr[j].getValue() < arr[minIndx].getValue()) {
                    // Reset the old min index to default color, highlight the new one
                    transitions.add(colorCNode(arr, minIndx, j, START_COLOR, MIN_INDX_COLOR));
                    minIndx = j;
                } else {
                    // Reset the compared element back to default color
                    transitions.add(colorCNode(arr, START_COLOR, j));
                }
            }

            if (minIndx != i) {
                transitions.add(swap(arr, i, minIndx));
            }
            transitions.add(colorCNode(arr, START_COLOR, i, minIndx));
        }
        
        transitions.add(colorCNode(Arrays.asList(arr), SORTED_COLOR));
        return transitions;
    }
}