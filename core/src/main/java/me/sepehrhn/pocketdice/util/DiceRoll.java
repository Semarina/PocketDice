package me.sepehrhn.pocketdice.util;

import java.util.List;

public record DiceRoll(DiceParser.DiceSpec spec, List<Integer> rolls, int total) {}
