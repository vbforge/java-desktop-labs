package com.vbforge.sortscope;

import javafx.animation.Transition;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HeapSort extends AbstractSort {
    private static final Color ROOT_COLOR = Color.web("#f89820");
    private final ArrayList<Transition> transitions;

    public HeapSort(double dx) {
        super(dx);
        this.transitions = new ArrayList<>();
    }

    private void heapify(CNode[] arr, int i, int n) {
        int left = 2 * i + 1;
        int right = 2 * i + 2;
        int max = i;

        comparisons += 2;
        if (left < n && arr[max].getValue() < arr[left].getValue()) max = left;
        if (right < n && arr[max].getValue() < arr[right].getValue()) max = right;

        if (max != i) {
            transitions.add(swap(arr, i, max));
            heapify(arr, max, n);
        }
    }

    private List<CNode> selectSubTree(CNode[] arr, int n) {
        List<CNode> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(arr[i]);
        }
        return list;
    }

    @Override
    public ArrayList<Transition> startSort(CNode[] arr) {
        transitions.clear();
        
        // Build initial max heap
        transitions.add(colorCNode(selectSubTree(arr, arr.length), SELECT_COLOR));
        for (int i = arr.length / 2 - 1; i >= 0; i--) {
            heapify(arr, i, arr.length);
        }
        transitions.add(colorCNode(selectSubTree(arr, arr.length), START_COLOR));

        // Swap root with last element, heapify subarray
        for (int i = arr.length - 1; i > 0; i--) {
            transitions.add(colorCNode(arr, ROOT_COLOR, 0));
            transitions.add(swap(arr, 0, i));
            transitions.add(colorCNode(arr, START_COLOR, i));
            
            transitions.add(colorCNode(selectSubTree(arr, i), SELECT_COLOR));
            heapify(arr, 0, i);
            transitions.add(colorCNode(selectSubTree(arr, i), START_COLOR));
        }
        
        transitions.add(colorCNode(Arrays.asList(arr), SORTED_COLOR));
        return transitions;
    }
}