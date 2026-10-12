// File: src/test/java/me/sepehrhn/pocketdice/util/DiceParserTest.java
package me.sepehrhn.pocketdice.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DiceParserTest {

    @Test
    void validInputs() {
        var a = DiceParser.parse("1d6");
        assertEquals(1, a.dice());
        assertEquals(6, a.faces());

        var b = DiceParser.parse("2d20");
        assertEquals(2, b.dice());
        assertEquals(20, b.faces());

        var c = DiceParser.parse("d8");
        assertEquals(1, c.dice());
        assertEquals(8, c.faces());
    }

    @Test
    void invalidInputs() {
        assertThrows(DiceParser.DiceParseException.class, () -> DiceParser.parse("0d6"));
        assertThrows(DiceParser.DiceParseException.class, () -> DiceParser.parse("2d1"));
        assertThrows(DiceParser.DiceParseException.class, () -> DiceParser.parse("-1d6"));
        assertThrows(DiceParser.DiceParseException.class, () -> DiceParser.parse("2dx"));
        assertThrows(DiceParser.DiceParseException.class, () -> DiceParser.parse("2d"));
    }
}
