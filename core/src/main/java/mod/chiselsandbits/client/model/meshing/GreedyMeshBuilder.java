package mod.chiselsandbits.client.model.meshing;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.client.model.baked.chiseled.ChiselRenderType;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class GreedyMeshBuilder {

    private static final int DIMENSIONS = 3;

    private GreedyMeshBuilder() {
        throw new IllegalStateException("Cannot instantiate utility class");
    }

    @FunctionalInterface
    public interface MaterialProvider {
        BlockInformation getMaterial(int x, int y, int z);
    }

    public static GreedyMeshFace[] buildMesh(MaterialProvider data) {
        final List<GreedyMeshFace> faces = new ArrayList<>();
        final int sizePerDimension = StateEntrySize.current().getBitsPerBlockSide();

        final Object2IntMap<BlockInformation> indexByMaterial = new Object2IntOpenHashMap<>();
        final Int2ObjectMap<BlockInformation> materialByIndex = new Int2ObjectOpenHashMap<>();
        final int[] mask = new int[sizePerDimension * sizePerDimension]; // The mask is used to keep track of which faces have been added to the mesh.

        indexByMaterial.put(BlockInformation.AIR, 0);
        materialByIndex.put(0, BlockInformation.AIR);

        class MaterialProcessor {
            final int getMaterialIndex(int x, int y, int z) {
                final BlockInformation blockInformation = data.getMaterial(x, y, z);

                return indexByMaterial.computeIfAbsent(blockInformation, k -> {
                    int index = indexByMaterial.size() + 1;
                    materialByIndex.put(index, blockInformation);
                    return index;
                });
            }
        }

        final MaterialProcessor materialProcessor = new MaterialProcessor();

        for (int dimension = 0; dimension < DIMENSIONS; dimension++) {
            int currentFirstDimensionIter, currentSecondDimensionIter;  // The current iteration position in the x, y, and z dimensions, also known as i, j, and k.
            int width, height; //The current iteration dimensions in the x, y, and z dimensions, also known as l, w, and h.

            //We need to iterate over the other two dimensions.
            final int firstDimensionIter = (dimension + 1) % DIMENSIONS; // The u and v dimensions are used to iterate over the other two dimensions.
            final int secondDimensionIter = (dimension + 2) % DIMENSIONS;

            final int[] dimensionalIterator = new int[]{0, 0, 0};
            final int[] calculatedDimensionsOffset = new int[]{0, 0, 0}; //Dimension offset for the already calculated dimensions.

            calculatedDimensionsOffset[dimension] = 1;
            for (dimensionalIterator[dimension] = -1; dimensionalIterator[dimension] < sizePerDimension; ) {

                //Compute mask
                int maskIndex = 0;
                for (dimensionalIterator[secondDimensionIter] = 0; dimensionalIterator[secondDimensionIter] < sizePerDimension; ++dimensionalIterator[secondDimensionIter]) {
                    for (dimensionalIterator[firstDimensionIter] = 0; dimensionalIterator[firstDimensionIter] < sizePerDimension; ++dimensionalIterator[firstDimensionIter], ++maskIndex) {

                        int currentMaterial = (0 <= dimensionalIterator[dimension] ? materialProcessor.getMaterialIndex(
                                dimensionalIterator[0],
                                dimensionalIterator[1],
                                dimensionalIterator[2]) : 0);
                        int neighborMaterial = (dimensionalIterator[dimension] < sizePerDimension - 1 ? materialProcessor.getMaterialIndex(
                                dimensionalIterator[0] + calculatedDimensionsOffset[0],
                                dimensionalIterator[1] + calculatedDimensionsOffset[1],
                                dimensionalIterator[2] + calculatedDimensionsOffset[2]) : 0);

                        boolean aIsTruthy = currentMaterial != 0;
                        boolean bIsTruthy = neighborMaterial != 0;

                        if (aIsTruthy == bIsTruthy) {
                            mask[maskIndex] = 0;
                        } else if (aIsTruthy) {
                            mask[maskIndex] = currentMaterial;
                        } else {
                            mask[maskIndex] = -neighborMaterial;
                        }
                    }
                }

                //Increment the computed position in the current dimension
                dimensionalIterator[dimension]++;

                //Compute the mesh
                maskIndex = 0;
                for (currentSecondDimensionIter = 0; currentSecondDimensionIter < sizePerDimension; ++currentSecondDimensionIter) {
                    for(currentFirstDimensionIter = 0; currentFirstDimensionIter < sizePerDimension;) {
                        int materialMask = mask[maskIndex];
                        boolean isMaterialMaskTruthy = materialMask != 0;
                        if (isMaterialMaskTruthy) {
                            width = computeWidth(materialMask, mask, maskIndex, currentFirstDimensionIter, sizePerDimension);
                            height = computeHeight(currentSecondDimensionIter, sizePerDimension, width, mask, maskIndex, materialMask);

                            dimensionalIterator[firstDimensionIter] = currentFirstDimensionIter;
                            dimensionalIterator[secondDimensionIter] = currentSecondDimensionIter;

                            final GreedyMeshFace face = generateFace(materialMask,
                                secondDimensionIter,
                                height,
                                firstDimensionIter,
                                width,
                                materialByIndex,
                                dimensionalIterator,
                                dimension,
                                sizePerDimension);
                            faces.add(face);

                            clearMask(height, width, mask, maskIndex, sizePerDimension);

                            currentFirstDimensionIter += width;
                            maskIndex += width;
                        } else {
                            ++currentFirstDimensionIter;
                            ++maskIndex;
                        }
                    }
                }
            }
        }

        return faces.toArray(new GreedyMeshFace[0]);
    }

    private static void clearMask(int height, int width, int[] mask, int maskIndex, int sizePerDimension) {
        for (int x = 0; x < height; x++) {
            for (int y = 0; y < width; y++) {
                mask[maskIndex + y + x * sizePerDimension] = 0;
            }
        }
    }

    @NotNull
    private static GreedyMeshFace generateFace(
        int materialMask,
        int secondDimensionIter,
        int height,
        int firstDimensionIter,
        int width,
        Int2ObjectMap<BlockInformation> materialByIndex,
        int[] dimensionalIterator,
        int dimension,
        float sizePerDimension) {
        int[] deltaVertical = new int[]{0, 0, 0};
        int[] deltaHorizontal = new int[]{0, 0, 0};

        final Direction.Axis axis = Direction.Axis.values()[dimension];

        Direction.AxisDirection axisDirection = Direction.AxisDirection.POSITIVE;
        if (materialMask > 0) {
            deltaHorizontal[secondDimensionIter] = height;
            deltaVertical[firstDimensionIter] = width;
        } else {
            axisDirection = Direction.AxisDirection.NEGATIVE;
            materialMask = -materialMask;
            deltaVertical[secondDimensionIter] = height;
            deltaHorizontal[firstDimensionIter] = width;
        }

        final Direction normalDirection = Direction.fromAxisAndDirection(axis, axisDirection);

        final BlockInformation material = materialByIndex.get(materialMask);
        final Vector3f lowerLeft = new Vector3f(dimensionalIterator[0], dimensionalIterator[1], dimensionalIterator[2]).div(sizePerDimension);
        final Vector3f upperLeft = new Vector3f(dimensionalIterator[0] + deltaVertical[0], dimensionalIterator[1] + deltaVertical[1], dimensionalIterator[2] + deltaVertical[2]).div(sizePerDimension);
        final Vector3f lowerRight = new Vector3f(dimensionalIterator[0] + deltaHorizontal[0], dimensionalIterator[1] + deltaHorizontal[1], dimensionalIterator[2] + deltaHorizontal[2]).div(sizePerDimension);
        final Vector3f upperRight = new Vector3f(dimensionalIterator[0] + deltaVertical[0] + deltaHorizontal[0], dimensionalIterator[1] + deltaVertical[1] + deltaHorizontal[1], dimensionalIterator[2] + deltaVertical[2] + deltaHorizontal[2]).div(sizePerDimension);

        final double axisValue = axis.choose(lowerLeft.x, lowerLeft.y, lowerLeft.z);

        final boolean isEdge = axisValue == 0 || axisValue == sizePerDimension;

        return new GreedyMeshFace(
            material,
            lowerLeft,
            upperLeft,
            lowerRight,
            upperRight,
            normalDirection,
            isEdge
        );
    }

    private static int computeHeight(int j, int sizePerDimension, int width, int[] mask, int maskIndex, int materialMask) {
        int height;
        int k;
        boolean done = false;
        for(height = 1; j + height < sizePerDimension; height++) {
            for(k = 0; k < width; k++) {
                if(mask[maskIndex + k + height * sizePerDimension] != materialMask) {
                    done = true;
                    break;
                }
            }
            if(done) {
                break;
            }
        }
        return height;
    }

    private static int computeWidth(int materialMask, int[] mask, int maskIndex, int i, int sizePerDimension) {
        int width = 1;
        while (i + width < sizePerDimension && materialMask == mask[maskIndex + width]) {
            width++;
        }
        return width;
    }

}
