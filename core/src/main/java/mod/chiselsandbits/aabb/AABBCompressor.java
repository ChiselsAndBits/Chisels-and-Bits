package mod.chiselsandbits.aabb;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import mod.chiselsandbits.api.axissize.CollisionType;
import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import mod.chiselsandbits.api.multistate.accessor.IStateEntryInfo;
import mod.chiselsandbits.api.multistate.accessor.sortable.IPositionMutator;
import mod.chiselsandbits.utils.AABBUtils;
import mod.chiselsandbits.utils.DirectionUtils;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class AABBCompressor
{

    private AABBCompressor()
    {
        throw new IllegalStateException("Can not instantiate an instance of: AABBCompressor. This is a utility class");
    }

    public static List<AABB> compressStates(
      final IAreaAccessor accessor,
      final CollisionType sizeType)
    {
        final BuildingState state = new BuildingState();

        //X == REGION
        //Y == FACE

        //noinspection Convert2Lambda We need this to be pre-compiled
        accessor.forEachWithPositionMutator(
          IPositionMutator.xyz(),
          new Consumer<>()
          {
              @Override
              public void accept(final IStateEntryInfo stateEntryInfo)
              {
                  if (state.getRegionBuildingAxisValue() != stateEntryInfo.getStartPoint().x())
                  {
                      state.setCurrentBox(null, null);
                  }
                  state.setRegionBuildingAxisValue(stateEntryInfo.getStartPoint().x());

                  if (state.getFaceBuildingAxisValue() != stateEntryInfo.getStartPoint().y())
                  {
                      state.setCurrentBox(null, null);
                  }
                  state.setFaceBuildingAxisValue(stateEntryInfo.getStartPoint().y());

                  final Optional<Vec3> previousCenterPoint = state.getLastCenter();
                  final Vec3 centerPoint = stateEntryInfo.getCenterPoint();
                  state.onNextEntry(centerPoint);

                  final Optional<Direction> stepDirection = previousCenterPoint.flatMap(
                    d -> DirectionUtils.getDirectionVectorBetweenIfAligned(centerPoint, d)
                  );

                  final Optional<AABB> potentialEntryData = buildBoundingBox(stateEntryInfo, sizeType);

                  if (potentialEntryData.isEmpty())
                  {
                      state.setCurrentBox(null, centerPoint);
                      return;
                  }
                  final AABB entryData = potentialEntryData.get();

                  if (state.getCurrentBox() != null)
                  {
                      if (stepDirection
                        .map(direction -> AABBUtils.areBoxesNeighbors(state.getCurrentBox(), entryData, direction))
                        .filter(b -> b)
                        .isPresent())
                      {
                          state.expandCurrentBoxToInclude(entryData, centerPoint);

                          if (attemptMergeWithNeighbors(state, centerPoint, state.getCurrentBox()))
                          {
                              return;
                          }

                          return;
                      }
                  }

                  if (attemptMergeWithNeighbors(state, centerPoint, entryData))
                  {
                      return;
                  }

                  state.setCurrentBox(potentialEntryData.get(), centerPoint);
              }
          }
        );
        return Lists.newArrayList(state.getBoxes());
    }

    private static boolean attemptMergeWithNeighbors(final BuildingState state, final Vec3 centerPoint, final AABB entryData)
    {
        for (final Direction offsetDirection :Direction.values())
        {
            final Vec3 neighborCenter = centerPoint.add(Vec3.atLowerCornerOf(offsetDirection.getUnitVec3i()).multiply(
              StateEntrySize.current().getSizePerBit(),
              StateEntrySize.current().getSizePerBit(),
              StateEntrySize.current().getSizePerBit()
              ));
            final Optional<AABB> potentialNeighborBox = state.getBoxFor(neighborCenter);

            if (potentialNeighborBox.isPresent()) {
                final AABB neighborBox = potentialNeighborBox.get();
                if (AABBUtils.areBoxesNeighbors(entryData, neighborBox, offsetDirection)) {
                    state.expandBoxAt(neighborCenter, entryData, centerPoint);
                    return true;
                }
            }
        }
        return false;
    }

    public static Optional<AABB> buildBoundingBox(
      final IStateEntryInfo stateEntryInfo,
      final CollisionType sizeType) {
        if (!sizeType.isValidFor(stateEntryInfo))
            return Optional.empty();

        return Optional.of(stateEntryInfo.getBoundingBox());
    }

    private static final class BuildingState {
        private double regionBuildingAxis = Double.NEGATIVE_INFINITY;
        private double faceBuildingAxis = Double.NEGATIVE_INFINITY;

        private Vec3 lastCenterPoint = null;
        private AABB currentBox;

        private final Map<Vec3, AABB>      boxAssignments   = Maps.newHashMap();
        private final Multimap<AABB, Vec3> stateAssignments = HashMultimap.create();

        public double getRegionBuildingAxisValue()
        {
            return regionBuildingAxis;
        }

        public void setRegionBuildingAxisValue(final double regionBuildingAxis)
        {
            this.regionBuildingAxis = regionBuildingAxis;
        }

        public double getFaceBuildingAxisValue()
        {
            return faceBuildingAxis;
        }

        public void setFaceBuildingAxisValue(final double faceBuildingAxis)
        {
            this.faceBuildingAxis = faceBuildingAxis;
        }

        public AABB getCurrentBox()
        {
            return currentBox;
        }

        public void setCurrentBox(final AABB currentBox, final Vec3 centerPoint)
        {
            this.currentBox = currentBox;
            if (currentBox != null) {
                boxAssignments.put(centerPoint, currentBox);
                stateAssignments.put(currentBox, centerPoint);
            }
        }

        public Optional<AABB> getBoxFor(final Vec3 target) {
            return Optional.ofNullable(boxAssignments.get(target));
        }

        public Optional<Vec3> getLastCenter() {
            return Optional.ofNullable(lastCenterPoint);
        }

        public void onNextEntry(final Vec3 lastCenterPoint) {
            this.lastCenterPoint = lastCenterPoint;
        }

        public void expandCurrentBoxToInclude(final AABB entryData, final Vec3 centerPoint)
        {
            final AABB current = this.getCurrentBox();
            if (current == null)
                throw new IllegalStateException("Can not expand current box, if current is not set.");

            final AABB expanded = current.minmax(entryData);

            final Collection<Vec3> currentlyAssignedToCurrent = stateAssignments.removeAll(current);

            currentlyAssignedToCurrent.forEach(v -> boxAssignments.put(v, expanded));
            stateAssignments.putAll(expanded, currentlyAssignedToCurrent);

            boxAssignments.put(centerPoint, expanded);
            stateAssignments.put(expanded, centerPoint);

            this.currentBox = expanded;
        }

        public Collection<AABB> getBoxes()
        {
            return stateAssignments.keySet();
        }

        public void expandBoxAt(final Vec3 neighborCenter, final AABB entryData, final Vec3 centerPoint)
        {
            final AABB current = boxAssignments.get(neighborCenter);
            if (current == null)
                throw new IllegalStateException(String.format("Can not expand box at: %s, if current is not set.", neighborCenter));

            final AABB expanded = current.minmax(entryData);

            final Collection<Vec3> currentlyAssignedToCurrent = stateAssignments.removeAll(current);

            currentlyAssignedToCurrent.forEach(v -> boxAssignments.put(v, expanded));
            stateAssignments.putAll(expanded, currentlyAssignedToCurrent);

            boxAssignments.put(centerPoint, expanded);
            stateAssignments.put(expanded, centerPoint);
        }
    }
}
