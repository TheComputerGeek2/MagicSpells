package com.nisovin.magicspells.variables.meta;

import java.time.Instant;

import com.nisovin.magicspells.variables.variabletypes.MetaVariable;

public class TimestampDaysVariable extends MetaVariable {

    @Override
    public double getValue(String player) {
        Instant instant = Instant.now();
        long mins = instant.getEpochSecond();
        return Math.floor(mins / 60 / 60 / 24);
    }

}
