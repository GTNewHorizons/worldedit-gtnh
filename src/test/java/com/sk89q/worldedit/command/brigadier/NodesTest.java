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

import static com.sk89q.worldedit.command.brigadier.Nodes.integer;
import static com.sk89q.worldedit.command.brigadier.Nodes.literal;
import static com.sk89q.worldedit.command.brigadier.Nodes.word;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.util.Arrays;

import net.minecraft.command.ICommandSender;

import org.junit.Before;
import org.junit.Test;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

/**
 * Runs the helpers through the real Brigadier parser: flags and aliases rely on how it treats redirects and siblings.
 */
public class NodesTest {

    private final CommandDispatcher<ICommandSender> dispatcher = new CommandDispatcher<>();
    private final ICommandSender sender = mock(ICommandSender.class);
    private CommandCall lastCall;

    private final Command<ICommandSender> record = ctx -> {
        lastCall = new CommandCall(null, ctx, null);
        return Command.SINGLE_SUCCESS;
    };

    @Before
    public void setUp() {
        // /paste [-ao] [-m <mask>] [leave] [count]
        Nodes.registerWithFlags(
            dispatcher,
            literal("/paste").executes(record)
                .then(
                    word("leave").executes(record)
                        .then(integer("count").executes(record))),
            Flags.of("ao")
                .value('m', "mask"),
            "/p");

        // /need [-h] <pattern>
        Nodes.registerWithFlags(
            dispatcher,
            literal("/need").executes(Nodes.USAGE)
                .then(word("pattern").executes(record)),
            "h");

        // /num [-h] <amount>
        Nodes.registerWithFlags(
            dispatcher,
            literal("/num").executes(Nodes.USAGE)
                .then(integer("amount").executes(record)),
            "h");

        // /group sub [-h] <value>, also /g s
        com.mojang.brigadier.tree.LiteralCommandNode<ICommandSender> sub = Nodes.withFlags(
            literal("sub").executes(Nodes.USAGE)
                .then(word("value").executes(record)),
            "h");
        Nodes.register(
            dispatcher,
            literal("/group").executes(Nodes.USAGE)
                .then(sub)
                .then(Nodes.alias("s", sub)),
            "/g");
    }

    private void run(String input) throws CommandSyntaxException {
        lastCall = null;
        dispatcher.execute(input, sender);
    }

    @Test
    public void flagAsTheLastWordIsSeen() throws Exception {
        run("/paste -a");

        assertTrue(lastCall.flag('a'));
        assertFalse(lastCall.flag('o'));
    }

    @Test
    public void flagAsTheLastWordIsNotReadAsAnArgument() throws Exception {
        run("/paste -a");

        assertFalse(lastCall.has("leave"));
    }

    @Test
    public void flagsCanBeCombinedOrSeparate() throws Exception {
        run("/paste -ao");
        assertTrue(lastCall.flag('a') && lastCall.flag('o'));

        run("/paste -a -o");
        assertTrue(lastCall.flag('a') && lastCall.flag('o'));
    }

    @Test
    public void flagsComeWithArguments() throws Exception {
        run("/paste -a stone 3");

        assertTrue(lastCall.flag('a'));
        assertEquals("stone", lastCall.string("leave"));
        assertEquals(3, lastCall.integer("count"));
    }

    @Test
    public void noFlagsIsNoFlags() throws Exception {
        run("/paste stone");

        assertFalse(lastCall.flag('a'));
        assertEquals("stone", lastCall.string("leave"));
    }

    @Test
    public void valueFlagAsTheLastWordsIsSeen() throws Exception {
        run("/paste -m !air");

        assertEquals("!air", lastCall.flagValue('m'));
    }

    @Test
    public void valueFlagComesWithOtherFlagsAndArguments() throws Exception {
        run("/paste -m !air -a stone");

        assertEquals("!air", lastCall.flagValue('m'));
        assertTrue(lastCall.flag('a'));
        assertEquals("stone", lastCall.string("leave"));
    }

    @Test
    public void flagInFrontOfARequiredArgument() throws Exception {
        run("/need -h stone");

        assertTrue(lastCall.flag('h'));
        assertEquals("stone", lastCall.string("pattern"));
    }

    @Test
    public void unknownFlagIsAnArgument() throws Exception {
        run("/need -x");

        assertEquals("-x", lastCall.string("pattern"));
    }

    @Test
    public void quotedValueMayHoldSpaces() throws Exception {
        run("/need \"stone >dirt\"");

        assertEquals("stone >dirt", lastCall.string("pattern"));
    }

    @Test
    public void negativeNumberIsNotAFlag() throws Exception {
        run("/num -5");

        assertEquals(-5, lastCall.integer("amount"));
        assertFalse(lastCall.flag('h'));
    }

    @Test
    public void numbersCanBeFormulas() throws Exception {
        run("/num 2*8");
        assertEquals(16, lastCall.integer("amount"));

        run("/num \"2 * 8\"");
        assertEquals(16, lastCall.integer("amount"));
    }

    @Test
    public void wholeNumbersCutOffDecimals() throws Exception {
        run("/num 5/2");
        assertEquals(2, lastCall.integer("amount"));

        run("/num 2.5");
        assertEquals(2, lastCall.integer("amount"));
    }

    @Test(expected = CommandSyntaxException.class)
    public void wordIsNotANumber() throws Exception {
        run("/num stone");
    }

    @Test
    public void formulaIsOnlyCalculatedWhenTheCommandAsksForIt() throws Exception {
        // Parsing also happens on Tab and before permissions, so a bad result may only surface when the value is read
        run("/num 1/0");

        try {
            lastCall.integer("amount");
            fail("Expected a failure");
        } catch (CommandFailure expected) {
            assertTrue(
                expected.getMessage()
                    .contains("1/0"));
        }
    }

    @Test
    public void limitsAlsoApplyToFormulas() throws Exception {
        Nodes.register(dispatcher, literal("/light").then(integer("level", 0, 15).executes(record)));

        run("/light 3*5");
        assertEquals(15, lastCall.integer("level"));

        run("/light 3*6");
        try {
            lastCall.integer("level");
            fail("Expected a failure");
        } catch (CommandFailure expected) {
            assertEquals("'3*6' must be at most 15.", expected.getMessage());
        }
    }

    @Test
    public void aliasTakesTheSameArgumentsAndFlags() throws Exception {
        run("/p");
        assertFalse(lastCall.flag('a'));

        run("/p -a stone 3");
        assertTrue(lastCall.flag('a'));
        assertEquals(3, lastCall.integer("count"));
    }

    @Test
    public void aliasesWorkInsideGroups() throws Exception {
        run("/g s -h stone");

        assertTrue(lastCall.flag('h'));
        assertEquals("stone", lastCall.string("value"));
    }

    @Test
    public void usageListsFlagsAndOptionalArguments() {
        assertEquals(
            Arrays.asList("/paste [-ao] [-m <mask>] [<leave> [<count>]]"),
            Nodes.usageLines(
                "/paste",
                dispatcher.getRoot()
                    .getChild("/paste")));
        assertEquals(
            Arrays.asList("/need [-h] <pattern>"),
            Nodes.usageLines(
                "/need",
                dispatcher.getRoot()
                    .getChild("/need")));
    }
}
