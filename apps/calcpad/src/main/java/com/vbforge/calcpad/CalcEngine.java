package com.vbforge.calcpad;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class CalcEngine {

    private BigDecimal current = BigDecimal.ZERO;
    private BigDecimal accumulator = BigDecimal.ZERO;
    private String pendingOp = "";
    private boolean startNewNumber = true;
    private boolean errorState = false;

    private String displayText = "0";
    private String expressionText = "";

    public void inputDigit(String digit) {
        if (errorState) clear();
        
        if (startNewNumber) {
            displayText = digit;
            startNewNumber = false;
        } else {
            if (displayText.equals("0") && !digit.equals(".")) {
                displayText = digit;
            } else {
                displayText += digit;
            }
        }
        current = new BigDecimal(displayText);
    }

    public void inputOperator(String op) {
        if (errorState) return;
        
        if (!pendingOp.isEmpty() && !startNewNumber) {
            calculate();
        } else {
            accumulator = current;
        }
        
        pendingOp = op;
        startNewNumber = true;
        expressionText = format(accumulator) + " " + op;
    }

    public void calculate() {
        if (errorState || pendingOp.isEmpty()) return;

        try {
            switch (pendingOp) {
                case "+" -> accumulator = accumulator.add(current);
                case "-" -> accumulator = accumulator.subtract(current);
                case "*" -> accumulator = accumulator.multiply(current);
                case "/" -> {
                    if (current.compareTo(BigDecimal.ZERO) == 0) {
                        errorState = true;
                        displayText = "Error";
                        expressionText = "Cannot divide by zero";
                        return;
                    }
                    accumulator = accumulator.divide(current, MathContext.DECIMAL64);
                }
                case "%" -> {
                    if (current.compareTo(BigDecimal.ZERO) == 0) {
                        errorState = true;
                        displayText = "Error";
                        expressionText = "Cannot divide by zero";
                        return;
                    }
                    accumulator = accumulator.remainder(current);
                }
            }
            displayText = format(accumulator);
            current = accumulator;
            pendingOp = "";
            startNewNumber = true;
            expressionText = "";
        } catch (ArithmeticException e) {
            errorState = true;
            displayText = "Error";
            expressionText = "Math error";
        }
    }

    public void backspace() {
        if (errorState) { clear(); return; }
        if (startNewNumber) return;
        
        if (displayText.length() > 1) {
            displayText = displayText.substring(0, displayText.length() - 1);
        } else {
            displayText = "0";
        }
        current = new BigDecimal(displayText);
    }

    public void clear() {
        current = BigDecimal.ZERO;
        accumulator = BigDecimal.ZERO;
        pendingOp = "";
        startNewNumber = true;
        errorState = false;
        displayText = "0";
        expressionText = "";
    }

    public String getDisplayText() { return displayText; }
    public String getExpressionText() { return expressionText; }

    private String format(BigDecimal val) {
        // Strip trailing zeros for clean display (e.g., 5.00 becomes 5)
        return val.stripTrailingZeros().toPlainString();
    }
}