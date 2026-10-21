package com.vbforge.rectsolve;

public final class RectangleSolver {
    private static final double EPSILON = 1e-9;

    private RectangleSolver() {}

    public static double solve(Double a, Double b, Double c, Double d) {
        int unknownCount = 0;
        if (a == null) unknownCount++;
        if (b == null) unknownCount++;
        if (c == null) unknownCount++;
        if (d == null) unknownCount++;

        if (unknownCount != 1) {
            throw new IllegalArgumentException("Exactly one value must be unknown. Found " + unknownCount);
        }

        if (a == null) {
            requireNonZero(d, "d");
            return (b * c) / d;
        } else if (b == null) {
            requireNonZero(c, "c");
            return (a * d) / c;
        } else if (c == null) {
            requireNonZero(b, "b");
            return (a * d) / b;
        } else {
            requireNonZero(a, "a");
            return (b * c) / a;
        }
    }

    public static VerificationResult verify(double a, double b, double c, double d) {
        double left = a * d;
        double right = b * c;
        boolean consistent = Math.abs(left - right) < EPSILON;
        return new VerificationResult(consistent, left, right);
    }

    public record VerificationResult(boolean consistent, double leftProduct, double rightProduct) {}

    private static void requireNonZero(double value, String fieldName) {
        if (value == 0.0) {
            throw new ArithmeticException("Cannot solve: field '" + fieldName + "' is 0 (division by zero).");
        }
    }
}