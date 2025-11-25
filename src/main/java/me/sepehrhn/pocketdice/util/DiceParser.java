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
            throw new DiceParseException(ParseError.MISSING_NOTATION);
        }

        Matcher m = FULL.matcher(input);
        if (m.matches()) {
            int dice = parseInt(m.group(1), true);
            int faces = parseInt(m.group(2), false);
            validate(dice, faces);
            return new DiceSpec(dice, faces);
        }

        if (allowShorthand) {
            Matcher s = SHORTHAND.matcher(input);
            if (s.matches()) {
                int faces = parseInt(s.group(1), false);
                validate(1, faces);
                return new DiceSpec(1, faces);
            }
        }

        throw new DiceParseException(ParseError.INVALID_NOTATION, allowShorthand);
    }

    private static int parseInt(String s, boolean diceField) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            throw new DiceParseException(diceField ? ParseError.DICE_NAN : ParseError.FACES_NAN);
        }
    }

    private static void validate(int dice, int faces) {
        if (dice < 1) {
            throw new DiceParseException(ParseError.DICE_TOO_LOW);
        }
        if (faces < 2) {
            throw new DiceParseException(ParseError.FACES_TOO_LOW);
        }
    }

    public enum ParseError {
        MISSING_NOTATION,
        INVALID_NOTATION,
        DICE_NAN,
        FACES_NAN,
        DICE_TOO_LOW,
        FACES_TOO_LOW
    }

    public static class DiceParseException extends IllegalArgumentException {
        private final ParseError error;
        private final boolean shorthandAllowed;

        public DiceParseException(ParseError error) {
            this(error, false);
        }

        public DiceParseException(ParseError error, boolean shorthandAllowed) {
            this.error = error;
            this.shorthandAllowed = shorthandAllowed;
        }

        public ParseError getError() {
            return error;
        }

        public boolean isShorthandAllowed() {
            return shorthandAllowed;
        }
    }

    public record DiceSpec(int dice, int faces) {}
}
