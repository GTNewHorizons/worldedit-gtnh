package com.sk89q.worldedit.regions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.operation.RunContext;
import com.sk89q.worldedit.function.visitor.RegionVisitor;

public class RegionCountTest {

    private final RunContext oneCandidate = new RunContext() {

        @Override
        public boolean shouldContinue() {
            return false;
        }
    };

    @Test
    public void countYieldsEvenBeforeReachingTheFirstSelectedBlock() throws Exception {
        EllipsoidRegion ellipsoid = new EllipsoidRegion(new Vector(0, 128, 0), new Vector(100000, 127, 100000));
        CylinderRegion cylinder = new CylinderRegion(new Vector(0, 128, 0), new Vector2D(100000, 100000), 1, 255);
        for (Region shape : Arrays.asList(ellipsoid, cylinder)) {
            RegionVisitor scan = RegionCount.create(shape, pos -> {
                fail("An exterior candidate must yield without reading a world block");
                return false;
            });
            assertSame(scan, scan.resume(oneCandidate));
            assertSame(scan, scan.resume(oneCandidate));
            assertEquals(0, scan.getAffected());
        }
    }

    @Test
    public void slicedCountsVisitExactlyThePositionsOfEachBuiltInShape() throws Exception {
        Polygonal2DRegion polygon = new Polygonal2DRegion(
            null,
            Arrays.asList(new BlockVector2D(-2, -1), new BlockVector2D(3, 0), new BlockVector2D(1, 4)),
            7,
            9);
        ConvexPolyhedralRegion convex = new ConvexPolyhedralRegion((com.sk89q.worldedit.world.World) null);
        convex.addVertex(new Vector(-2, 4, -2));
        convex.addVertex(new Vector(3, 4, -2));
        convex.addVertex(new Vector(-2, 4, 3));
        convex.addVertex(new Vector(-1, 7, -1));
        CylinderRegion cylinder = new CylinderRegion(new Vector(15, 3, -16), new Vector2D(3, 4), 2, 6);
        EllipsoidRegion ellipsoid = new EllipsoidRegion(new Vector(0, 0, 0), new Vector(2, 4, 2));

        for (Region shape : Arrays.asList(ellipsoid, convex, polygon, cylinder)) {
            Map<Vector, Integer> expected = new HashMap<>();
            for (Vector pos : shape) {
                expected.merge(pos, 1, Integer::sum);
            }
            Map<Vector, Integer> actual = new HashMap<>();
            RegionVisitor scan = RegionCount.create(shape, pos -> {
                actual.merge(pos, 1, Integer::sum);
                return true;
            });
            Operation next = scan;
            while (next != null) {
                next = next.resume(oneCandidate);
            }
            assertEquals(expected, actual);
        }
    }

    @Test
    public void compositeSnapshotsKeepRepeatedVisitsAndIgnoreLaterChildChanges() throws Exception {
        BlockVector origin = new BlockVector(0, 0, 0);
        CuboidRegion child = new CuboidRegion(origin, origin);
        RegionIntersection selection = new RegionIntersection(child, new RegionIntersection(child));
        Map<Vector, Integer> visited = new HashMap<>();
        RegionVisitor scan = RegionCount.create(selection, pos -> {
            visited.merge(pos, 1, Integer::sum);
            return true;
        });
        child.setPos2(new Vector(3, 0, 0));
        Operations.complete(scan);
        assertEquals(Collections.singletonMap(origin, 2), visited);
        assertEquals(2, scan.getAffected());
    }
}
