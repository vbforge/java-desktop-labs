package com.vbforge.sortscope;

import javafx.animation.ParallelTransition;
import javafx.animation.Transition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MergeSort extends AbstractSort {
    private CNode[] tmp;

    public MergeSort(double dx) { super(dx); }

    private ArrayList<Transition> merge(CNode[] arr, int p, int q, int r) {
        ArrayList<Transition> transitions = new ArrayList<>();
        List<CNode> tmpList = new ArrayList<>();

        for (int i = p; i <= r; i++) {
            tmp[i] = arr[i];
            tmpList.add(tmp[i]);
        }

        int i = p, j = q + 1, k = p;
        while (i <= q && j <= r) {
            comparisons++;
            if (tmp[i].getValue() <= tmp[j].getValue()) {
                arr[k++] = tmp[i++];
            } else {
                arr[k++] = tmp[j++];
            }
            moves++; // Count the write
        }
        while (i <= q) {
            arr[k++] = tmp[i++];
            moves++; // Count the write
        }
        while (j <= r) {
            arr[k++] = tmp[j++];
            moves++; // Count the write
        }

        transitions.add(colorCNode(tmpList, SELECT_COLOR));
        ParallelTransition pt = new ParallelTransition(); 

        for (int x = p; x <= r; x++) {
            for (int y = p; y <= r; y++) {
                if (tmp[x] == arr[y]) {
                    pt.getChildren().add(tmp[x].moveX(dx * (y - x)));
                    break;
                }
            }
        }
        transitions.add(pt);
        transitions.add(colorCNode(tmpList, START_COLOR));
        return transitions;
    }

    private ArrayList<Transition> mergeSort(CNode[] arr, int p, int r) {
        ArrayList<Transition> transitions = new ArrayList<>();
        if (p < r) {
            int q = (p + r) / 2;
            transitions.addAll(mergeSort(arr, p, q));
            transitions.addAll(mergeSort(arr, q + 1, r));
            transitions.addAll(merge(arr, p, q, r));
        }
        return transitions;
    }

    @Override
    public ArrayList<Transition> startSort(CNode[] arr) {
        this.tmp = new CNode[arr.length];
        ArrayList<Transition> transitions = new ArrayList<>(mergeSort(arr, 0, arr.length - 1));
        transitions.add(colorCNode(Arrays.asList(arr), SORTED_COLOR));
        return transitions;
    }
}