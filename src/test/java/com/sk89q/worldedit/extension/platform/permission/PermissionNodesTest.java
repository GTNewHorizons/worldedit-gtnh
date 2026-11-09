package com.sk89q.worldedit.extension.platform.permission;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.sk89q.worldedit.command.brigadier.CommandRunner;

public class PermissionNodesTest {

    @Test
    public void aCommandDeclaresEachOfItsAlternativeNodes() {
        CommandRunner.run("worldedit.test.first|worldedit.test.second", c -> {});

        assertTrue(
            PermissionNodes.all()
                .contains("worldedit.test.first"));
        assertTrue(
            PermissionNodes.all()
                .contains("worldedit.test.second"));
    }

    @Test
    public void nodesCheckedOutsideCommandsAreKnownUpFront() {
        assertTrue(
            PermissionNodes.all()
                .contains(OverridePermissions.NO_LIMITS));
        assertTrue(
            PermissionNodes.all()
                .contains("worldedit.history.undo.other"));
    }
}
