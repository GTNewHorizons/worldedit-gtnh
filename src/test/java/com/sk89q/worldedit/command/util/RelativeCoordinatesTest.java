package com.sk89q.worldedit.command.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class RelativeCoordinatesTest {

    private static void assertInvalid(String input) {
        try {
            RelativeCoordinates.parse(input, 1, 2, 3);
            fail("expected " + input + " to be rejected");
        } catch (IllegalArgumentException expected) {}
    }

    @Test
    public void exactCoordinatesWithCommasOrSpaces() {
        assertArrayEquals(new int[] { 10, 64, -20 }, RelativeCoordinates.parse("10,64,-20", 1, 2, 3));
        assertArrayEquals(new int[] { 10, 64, -20 }, RelativeCoordinates.parse("10 64 -20", 1, 2, 3));
        assertArrayEquals(new int[] { 10, 64, -20 }, RelativeCoordinates.parse(" 10, 64 ,-20 ", 1, 2, 3));
    }

    @Test
    public void tildeIsRelativeToTheOrigin() {
        assertArrayEquals(new int[] { 100, 69, 297 }, RelativeCoordinates.parse("~,~5,~-3", 100, 64, 300));
        assertArrayEquals(new int[] { 100, 69, 297 }, RelativeCoordinates.parse("~ ~+5 ~-3", 100, 64, 300));
    }

    @Test
    public void exactAndRelativeMix() {
        assertArrayEquals(new int[] { 7, 64, 290 }, RelativeCoordinates.parse("7 ~ ~-10", 100, 64, 300));
    }

    @Test
    public void twoCoordinatesForChunks() {
        assertArrayEquals(new int[] { 4, -2 }, RelativeCoordinates.parse("~1 -2", 3, 9));
    }

    @Test
    public void badInputIsRejected() {
        assertInvalid("1,2");
        assertInvalid("1,2,3,4");
        assertInvalid("a,2,3");
        assertInvalid("~a,2,3");
        assertInvalid("1.5,2,3");
        assertInvalid("^,^,^");
        assertInvalid("~~,2,3");
        assertInvalid("99999999999,2,3");
        assertInvalid("");
    }
}
