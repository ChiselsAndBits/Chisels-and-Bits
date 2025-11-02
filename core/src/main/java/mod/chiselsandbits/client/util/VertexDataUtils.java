package mod.chiselsandbits.client.util;

import com.communi.suggestu.scena.core.client.models.processing.VertexData;
import com.communi.suggestu.scena.core.util.VectorUtils;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.minecraft.core.Direction;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.*;

public final class VertexDataUtils {

    private VertexDataUtils() {
        throw new IllegalStateException("Tried to instantiate: 'VertexDataUtils', but this is a utility class.");
    }

    @SuppressWarnings("ConstantConditions")
    public static Collection<VertexData> adaptVertices(VertexData[] vertexData, final Direction cullDirection, Vector3f from, Vector3f to) {
        final BiMap<VertexData, Vector2f> projectedVertexPositions = HashBiMap.create();
        Arrays.stream(vertexData).forEach(vertex -> projectedVertexPositions.put(vertex, vertex.projectOntoPlaneOf(cullDirection)));
        final Collection<Vector2f> boxCorners = buildCorners(cullDirection, from, to);
        final Map<Vector2f, VertexData> closestCorners = buildClosestVertices(vertexData, boxCorners, cullDirection);

        final InterpolationHelper interpolationHelper = new InterpolationHelper(
                projectedVertexPositions.get(vertexData[0]).x(),
                projectedVertexPositions.get(vertexData[0]).y(),
                projectedVertexPositions.get(vertexData[1]).x(),
                projectedVertexPositions.get(vertexData[1]).y(),
                projectedVertexPositions.get(vertexData[2]).x(),
                projectedVertexPositions.get(vertexData[2]).y(),
                projectedVertexPositions.get(vertexData[3]).x(),
                projectedVertexPositions.get(vertexData[3]).y()
        );

        final List<VertexData> adaptedVertices = new ArrayList<>();
        for (Vector2f newPosition : boxCorners) {
            interpolationHelper.locate(newPosition.x(), newPosition.y());
            final float u = interpolationHelper.interpolate(
                    vertexData[0].u(),
                    vertexData[1].u(),
                    vertexData[2].u(),
                    vertexData[3].u()
            );
            final float v = interpolationHelper.interpolate(
                    vertexData[0].v(),
                    vertexData[1].v(),
                    vertexData[2].v(),
                    vertexData[3].v()
            );

            final Vector3f position = VectorUtils.unprojectFromPlaneOf(newPosition, from, cullDirection);
            if (!closestCorners.containsKey(newPosition) || closestCorners.get(newPosition) == null) {
                throw new IllegalStateException("Could not find a vertex for the given position: %s".formatted(newPosition));
            }
            final VertexData vertex = new VertexData(position, new Vector2f(u,v), closestCorners.get(newPosition).vertexIndex());
            adaptedVertices.add(vertex);
        }

        adaptedVertices.sort(Comparator.comparing(VertexData::vertexIndex));

        return adaptedVertices;
    }

    private static Map<Vector2f, VertexData> buildClosestVertices(final VertexData[] vertexData, final Collection<Vector2f> boxCorners, Direction cullDirection) {
        final Vector2f center = new Vector2f();
        for (final Vector2f corner : boxCorners) {
            center.add(corner);
        }
        center.mul(1f / boxCorners.size());

        final Vector2f centerOfVertices = new Vector2f();
        for (final VertexData vertex : vertexData) {
            centerOfVertices.add(vertex.projectOntoPlaneOf(cullDirection));
        }
        centerOfVertices.mul(1f / vertexData.length);

        enum Corner {
            TOP_LEFT,
            TOP_RIGHT,
            BOTTOM_LEFT,
            BOTTOM_RIGHT;

            private static Corner closest(final Vector2f vector2f, final Vector2f center) {
                if (vector2f.x() < center.x()) {
                    if (vector2f.y() < center.y()) {
                        return BOTTOM_LEFT;
                    } else {
                        return TOP_LEFT;
                    }
                } else {
                    if (vector2f.y() < center.y()) {
                        return BOTTOM_RIGHT;
                    } else {
                        return TOP_RIGHT;
                    }
                }
            }
        }

        final BiMap<Corner, Vector2f> boxCornersByCorner = HashBiMap.create();
        boxCorners.forEach(corner -> boxCornersByCorner.put(Corner.closest(corner, center), corner));

        final BiMap<Corner, VertexData> vertexByCorner = HashBiMap.create();
        for (VertexData vertexDatum : vertexData) {
            vertexByCorner.put(Corner.closest(vertexDatum.projectOntoPlaneOf(cullDirection), centerOfVertices), vertexDatum);
        }

        final Map<Vector2f, VertexData> closestVertices = new HashMap<>();
        boxCornersByCorner.forEach((corner, vector2f) -> closestVertices.put(vector2f, vertexByCorner.get(corner)));
        return closestVertices;
    }

    private static Collection<Vector2f> buildCorners(final Direction cullDirection, final Vector3f from, final Vector3f to) {
        final Collection<Vector2f> corners = new ArrayList<>();

        final float x1 = from.x();
        final float y1 = from.y();
        final float z1 = from.z();

        final float x2 = to.x();
        final float y2 = to.y();
        final float z2 = to.z();

        switch (cullDirection) {
            case DOWN -> {
                corners.add(new Vector2f(x1, z1));
                corners.add(new Vector2f(x1, z2));
                corners.add(new Vector2f(x2, z2));
                corners.add(new Vector2f(x2, z1));
            }
            case UP -> {
                corners.add(new Vector2f(x1, z1));
                corners.add(new Vector2f(x2, z1));
                corners.add(new Vector2f(x2, z2));
                corners.add(new Vector2f(x1, z2));
            }
            case NORTH -> {
                corners.add(new Vector2f(x1, y1));
                corners.add(new Vector2f(x2, y1));
                corners.add(new Vector2f(x2, y2));
                corners.add(new Vector2f(x1, y2));
            }
            case SOUTH -> {
                corners.add(new Vector2f(x1, y1));
                corners.add(new Vector2f(x1, y2));
                corners.add(new Vector2f(x2, y2));
                corners.add(new Vector2f(x2, y1));
            }
            case WEST -> {
                corners.add(new Vector2f(z1, y1));
                corners.add(new Vector2f(z1, y2));
                corners.add(new Vector2f(z2, y2));
                corners.add(new Vector2f(z2, y1));
            }
            case EAST -> {
                corners.add(new Vector2f(z1, y1));
                corners.add(new Vector2f(z2, y1));
                corners.add(new Vector2f(z2, y2));
                corners.add(new Vector2f(z1, y2));
            }
        }

        return corners;
    }
}
