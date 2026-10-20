package com.vbforge.sortscope;

import javafx.animation.FillTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.Transition;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSort {
    public static final Color START_COLOR = Color.web("#6ea8d8");
    public static final Color SELECT_COLOR = Color.web("#f89820");
    public static final Color SORTED_COLOR = Color.web("#3fb950");

    public int comparisons = 0;
    public int moves = 0; // Renamed from swaps

    protected double dx;

    public AbstractSort(double dx) {
        this.dx = dx;
    }

    protected ParallelTransition colorCNode(CNode[] arr, Color color, int... indices) {
        ParallelTransition pt = new ParallelTransition();
        for (int i : indices) {
            FillTransition ft = new FillTransition(Duration.millis(100), arr[i], (Color) arr[i].getFill(), color);
            pt.getChildren().add(ft);
        }
        return pt;
    }

    protected ParallelTransition colorCNode(List<CNode> list, Color color) {
        ParallelTransition pt = new ParallelTransition();
        for (CNode c : list) {
            FillTransition ft = new FillTransition(Duration.millis(100), c, (Color) c.getFill(), color);
            pt.getChildren().add(ft);
        }
        return pt;
    }

    protected ParallelTransition swap(CNode[] arr, int i, int j) {
        ParallelTransition pt = new ParallelTransition();
        int dxFactor = j - i;
        pt.getChildren().addAll(arr[i].moveX(dx * dxFactor), arr[j].moveX(-dx * dxFactor));

        CNode tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
        
        moves++; // Increment moves here
        return pt;
    }

    public abstract ArrayList<Transition> startSort(CNode[] arr);
}