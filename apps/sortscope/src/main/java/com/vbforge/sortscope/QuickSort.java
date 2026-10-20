package com.vbforge.sortscope;

import javafx.animation.Transition;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.Arrays;

public class QuickSort extends AbstractSort {
    private static final Color PIVOT_COLOR = Color.web("#a371f7");
    private final ArrayList<Transition> transitions;

    public QuickSort(double dx) {
        super(dx);
        this.transitions = new ArrayList<>();
    }

    private void quickSort(CNode[] arr, int lo, int hi) {
        if (lo < hi) {
            int q = partition(arr, lo, hi);
            quickSort(arr, lo, q - 1);
            quickSort(arr, q + 1, hi);
        }
    }

    private int partition(CNode[] arr, int lo, int hi) {
        int i = lo;
        transitions.add(colorCNode(arr, PIVOT_COLOR, hi)); 

        for (int j = lo; j < hi; j++) {
            comparisons++;
            transitions.add(colorCNode(arr, SELECT_COLOR, j));
            
            if (arr[j].getValue() < arr[hi].getValue()) {
                transitions.add(swap(arr, i, j));
                transitions.add(colorCNode(arr, START_COLOR, i));
                i++;
            } else {
                transitions.add(colorCNode(arr, START_COLOR, j));
            }
        }
        
        transitions.add(swap(arr, i, hi));
        transitions.add(colorCNode(arr, START_COLOR, i));
        return i;
    }

    @Override
    public ArrayList<Transition> startSort(CNode[] arr) {
        quickSort(arr, 0, arr.length - 1);
        transitions.add(colorCNode(Arrays.asList(arr), SORTED_COLOR));
        return transitions;
    }
}