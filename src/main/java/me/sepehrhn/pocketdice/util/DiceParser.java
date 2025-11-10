// File: src/main/java/me/sepehrhn/pocketdice/util/DiceParser.java
package me.sepehrhn.pocketdice.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses dice notation like "2d6" or "d20" (if shorthand allowed).
 * Validates lower bounds: N >= 1, M >= 2.
 */
public final class DiceParser {

    private static final Pattern FULL = Pattern.compile("^\\s*(\\d+)[dD](\\d+)\\s*$");
    private static final Pattern SHORTHAND = Pattern.compile("^\\s*[dD](\\d+)\\s*$");

    private DiceParser() {}

    public static DiceSpec parse(String input, boolean allowShorthand) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Please provide a notation like 1d6 or d8.");
        }

        Matcher m = FULL.matcher(input);
        if (m.matches()) {
            int dice = parseInt(m.group(1), "Number of dice");
            int faces = parseInt(m.group(2), "Number of faces");
            validate(dice, faces);
            return new DiceSpec(dice, faces);
        }

        if (allowShorthand) {
            Matcher s = SHORTHAND.matcher(input);
            if (s.matches()) {
                int faces = parseInt(s.group(1), "Number of faces");
                validate(1, faces);
                return new DiceSpec(1, faces);
            }
        }

        throw new IllegalArgumentException("Invalid notation. Use NdM like 2d6"
                + (allowShorthand ? " or d8" : "") + ".");
    }

    private static int parseInt(String s, String field) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(field + " must be a number.");
        }
    }

    private static void validate(int dice, int faces) {
        if (dice < 1) {
            throw new IllegalArgumentException("Number of dice (N) must be >= 1.");
        }
        if (faces < 2) {
            throw new IllegalArgumentException("Number of faces (M) must be >= 2.");
        }
    }

    public record DiceSpec(int dice, int faces) {}
}