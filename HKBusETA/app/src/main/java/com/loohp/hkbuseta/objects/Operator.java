/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.loohp.hkbuseta.objects;

import androidx.compose.runtime.Immutable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Immutable
public final class Operator implements Comparable<Operator> {

    private static final Map<String, Operator> VALUES = new ConcurrentHashMap<>();
    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    public static final Operator KMB = createBuiltIn("kmb", "^[0-9A-Z]{16}$");
    public static final Operator CTB = createBuiltIn("ctb", "^[0-9]{6}$");
    public static final Operator NLB = createBuiltIn("nlb", "^[0-9]{1,4}$");
    public static final Operator MTR_BUS = createBuiltIn("mtr-bus", "^[A-Z]?[0-9]{1,3}[A-Z]?-[A-Z][0-9]{3}$");
    public static final Operator GMB = createBuiltIn("gmb", "^[0-9]{8}$");
    public static final Operator LRT = createBuiltIn("lightRail", "^LR[0-9]+$");
    public static final Operator MTR = createBuiltIn("mtr", "^[A-Z]{3}$");

    private static Operator createBuiltIn(String name, String stopIdPattern) {
        return VALUES.computeIfAbsent(name.toLowerCase(), k -> new Operator(k, COUNTER.getAndIncrement(), Pattern.compile(stopIdPattern), true));
    }

    public static Operator valueOf(String name) {
        return VALUES.computeIfAbsent(name.toLowerCase(), k -> new Operator(k, COUNTER.getAndIncrement(), null, false));
    }

    public static Operator[] values() {
        return VALUES.values().stream().sorted().toArray(Operator[]::new);
    }

    private final String name;
    private final int ordinal;
    private final Pattern stopIdPattern;
    private final boolean builtIn;

    private Operator(String name, int ordinal, Pattern stopIdPattern, boolean builtIn) {
        this.name = name;
        this.ordinal = ordinal;
        this.stopIdPattern = stopIdPattern;
        this.builtIn = builtIn;
    }

    public String name() {
        return name;
    }

    public int ordinal() {
        return ordinal;
    }

    public boolean matchStopIdPattern(String stopId) {
        return stopIdPattern != null && stopIdPattern.matcher(stopId).matches();
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public int compareTo(Operator other) {
        return this.ordinal - other.ordinal;
    }

}