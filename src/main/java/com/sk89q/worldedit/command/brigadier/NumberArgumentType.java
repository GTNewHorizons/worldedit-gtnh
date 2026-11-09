/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.command.brigadier;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.internal.expression.runtime.EvaluationException;

/**
 * A number, or a math formula that gives one, such as {@code 2*8}.
 *
 * <p>
 * Parsing only checks the syntax and never calculates anything. Brigadier parses on every press of Tab and before any
 * permission is checked, and formulas have no time limit, so calculating here would let anyone hang the server. The
 * value is calculated by {@link Input#value()}, which commands reach through {@link CommandCall}.
 * </p>
 */
final class NumberArgumentType implements ArgumentType<NumberArgumentType.Input> {

    private static final SimpleCommandExceptionType NOT_A_NUMBER = new SimpleCommandExceptionType(
        new LiteralMessage("Expected a number or a math formula"));

    private final double min;
    private final double max;

    NumberArgumentType(double min, double max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public Input parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        String text = TokenArgumentType.token()
            .parse(reader);
        try {
            return new Input(text, Double.parseDouble(text), null, min, max);
        } catch (NumberFormatException notPlain) {
            try {
                return new Input(text, 0, Expression.compile(text), min, max);
            } catch (ExpressionException notFormula) {
                reader.setCursor(start);
                throw NOT_A_NUMBER.createWithContext(reader);
            }
        }
    }

    /**
     * What was typed for a number argument.
     */
    static final class Input {

        private final String text;
        private final double plainValue;
        private final Expression formula;
        private final double min;
        private final double max;

        private Input(String text, double plainValue, Expression formula, double min, double max) {
            this.text = text;
            this.plainValue = plainValue;
            this.formula = formula;
            this.min = min;
            this.max = max;
        }

        /**
         * Calculates the value and checks that it is allowed.
         *
         * @throws CommandFailure if the formula has no usable result, or the number is out of range
         */
        double value() {
            double value = plainValue;
            if (formula != null) {
                try {
                    value = formula.evaluate();
                } catch (EvaluationException e) {
                    throw new CommandFailure("'" + text + "' could not be calculated (error: " + e.getMessage() + ")");
                }
            }
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                throw new CommandFailure("'" + text + "' does not give a usable number.");
            }
            if (value < min) {
                throw new CommandFailure("'" + text + "' must be at least " + format(min) + ".");
            }
            if (value > max) {
                throw new CommandFailure("'" + text + "' must be at most " + format(max) + ".");
            }
            return value;
        }

        private static String format(double limit) {
            return limit == Math.rint(limit) ? String.valueOf((long) limit) : String.valueOf(limit);
        }
    }
}
