package com.vbforge.sortscope;

import javafx.animation.TranslateTransition;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class CNode extends Rectangle {
    private final int value;

    public CNode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public TranslateTransition moveX(double x) {
        TranslateTransition t = new TranslateTransition();
        t.setNode(this);
        t.setDuration(Duration.millis(100));
        t.setByX(x);
        return t;
    }
}