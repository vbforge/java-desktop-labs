package com.vbforge.airdistance;

/**
 * Given a distance already travelled and the fraction of the total trip
 * that distance represents, computes the remaining distance.
 *
 * <p>Derivation:
 * <pre>
 *   fraction  = numerator / denominator
 *   total     = knownDistance / fraction
 *   remaining = total - knownDistance
 * </pre>
 *
 * <p>Which simplifies to:
 * <pre>
 *   remaining = knownDistance * (denominator - numerator) / numerator
 * </pre>
 *
 * <p>The simplified form avoids an intermediate division and is what this
 * class implements. Both forms are algebraically identical.
 *
 * <p>The class is a stateless utility — no fields, no instances — because
 * there is no state to hold. Every method is a pure function of its
 * arguments, which is what makes it trivially testable without a UI.
 */
public final class AirDistanceCalculator {

    private AirDistanceCalculator() {
        // utility class
    }

    /**
     * @param knownDistance distance already travelled, in km
     * @param numerator     numerator of the fraction of the trip already flown
     * @param denominator   denominator of the fraction of the trip already flown
     * @return remaining distance to travel, in km
     * @throws ArithmeticException if {@code numerator} or {@code denominator} is zero
     */
    public static double remainingDistance(double knownDistance,
                                           double numerator,
                                           double denominator) {
        if (denominator == 0.0) {
            throw new ArithmeticException("Denominator cannot be zero.");
        }
        if (numerator == 0.0) {
            throw new ArithmeticException(
                    "Numerator cannot be zero: a known distance greater than zero "
                    + "can't correspond to 0/" + formatNumber(denominator)
                    + " of the trip.");
        }
        return knownDistance * (denominator - numerator) / numerator;
    }

    /** Renders an integer-valued double without a trailing ".0". */
    private static String formatNumber(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value)) {
            return String.format("%.0f", value);
        }
        return String.valueOf(value);
    }
}