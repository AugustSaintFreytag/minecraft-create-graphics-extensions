package net.saint.createrenderfixer.dh;

import java.util.HashSet;
import java.util.Set;

import com.simibubi.create.AllTags;
import com.simibubi.create.content.contraptions.Contraption;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public final class WindmillLODAnalysisUtil {

	// Models

	public record PlaneSize(float width, float height, float depth) {
	}

	// API

	public static PlaneSize getWindmillPlaneSize(Contraption contraption, Direction.Axis rotationAxis, AABB bounds) {
		var basePlaneSize = getPlaneSizeForBounds(bounds, rotationAxis);
		var estimatedBladeWidth = getWindmillBladeWidthForContraption(contraption, rotationAxis);
		var adjustedBladeWidth = getBladeWidthForPlaneSize(basePlaneSize, estimatedBladeWidth);

		return new PlaneSize(basePlaneSize.width(), basePlaneSize.height(), adjustedBladeWidth);
	}

	// Utility

	private static PlaneSize getPlaneSizeForBounds(AABB bounds, Direction.Axis rotationAxis) {
		if (bounds == null || rotationAxis == null) {
			return new PlaneSize(1.0F, 1.0F, 1.0F);
		}

		var sizeAlongX = (float) bounds.getXsize();
		var sizeAlongY = (float) bounds.getYsize();
		var sizeAlongZ = (float) bounds.getZsize();

		return switch (rotationAxis) {
			case X -> new PlaneSize(sizeAlongZ, sizeAlongY, sizeAlongX);
			case Y -> new PlaneSize(sizeAlongX, sizeAlongZ, sizeAlongY);
			case Z -> new PlaneSize(sizeAlongX, sizeAlongY, sizeAlongZ);
		};
	}

	private static float getWindmillBladeWidthForContraption(Contraption contraption, Direction.Axis rotationAxis) {
		if (rotationAxis == null) {
			return 0.0F;
		}

		var sailPositions = getWindmillSailPositionsForContraption(contraption);

		if (sailPositions.isEmpty()) {
			return 0.0F;
		}

		var inPlaneAxes = getInPlaneAxesForRotationAxis(rotationAxis);
		var firstAxis = inPlaneAxes[0];
		var secondAxis = inPlaneAxes[1];
		var widthAlongFirstAxis = getBladeWidthForAxes(sailPositions, firstAxis, secondAxis);
		var widthAlongSecondAxis = getBladeWidthForAxes(sailPositions, secondAxis, firstAxis);

		return Math.max(widthAlongFirstAxis, widthAlongSecondAxis);
	}

	private static float getBladeWidthForPlaneSize(PlaneSize planeSize, float estimatedBladeWidth) {
		if (estimatedBladeWidth <= 0.0F) {
			return planeSize.depth();
		}

		var maximumWidth = Math.min(planeSize.width(), planeSize.height());

		if (estimatedBladeWidth > maximumWidth) {
			return maximumWidth;
		}

		return estimatedBladeWidth;
	}

	private static Set<BlockPos> getWindmillSailPositionsForContraption(Contraption contraption) {
		var sailPositions = new HashSet<BlockPos>();

		if (contraption == null) {
			return sailPositions;
		}

		var blocks = contraption.getBlocks();

		if (blocks == null || blocks.isEmpty()) {
			return sailPositions;
		}

		for (var entry : blocks.entrySet()) {
			var blockState = entry.getValue().state();

			if (!AllTags.AllBlockTags.WINDMILL_SAILS.matches(blockState)) {
				continue;
			}

			sailPositions.add(entry.getKey());
		}

		return sailPositions;
	}

	private static int getBladeWidthForAxes(Set<BlockPos> sailPositions, Direction.Axis bladeLengthAxis, Direction.Axis bladeWidthAxis) {
		var maximumWidth = 0;

		for (var position : sailPositions) {
			var length = getAdjacentSailCountForAxis(sailPositions, position, bladeLengthAxis);
			var width = getAdjacentSailCountForAxis(sailPositions, position, bladeWidthAxis);

			if (length <= width) {
				continue;
			}

			if (width > maximumWidth) {
				maximumWidth = width;
			}
		}

		return maximumWidth;
	}

	private static int getAdjacentSailCountForAxis(Set<BlockPos> sailPositions, BlockPos startPosition, Direction.Axis axis) {
		var count = 1;
		count += getAdjacentSailCountForAxisDirection(sailPositions, startPosition, axis, 1);
		count += getAdjacentSailCountForAxisDirection(sailPositions, startPosition, axis, -1);

		return count;
	}

	private static int getAdjacentSailCountForAxisDirection(Set<BlockPos> sailPositions, BlockPos startPosition, Direction.Axis axis,
			int step) {
		var count = 0;
		var currentPosition = startPosition;

		while (true) {
			currentPosition = getOffsetPositionForAxis(currentPosition, axis, step);

			if (!sailPositions.contains(currentPosition)) {
				break;
			}

			count++;
		}

		return count;
	}

	private static BlockPos getOffsetPositionForAxis(BlockPos position, Direction.Axis axis, int step) {
		if (axis == Direction.Axis.X) {
			return position.offset(step, 0, 0);
		}

		if (axis == Direction.Axis.Y) {
			return position.offset(0, step, 0);
		}

		return position.offset(0, 0, step);
	}

	private static Direction.Axis[] getInPlaneAxesForRotationAxis(Direction.Axis rotationAxis) {
		if (rotationAxis == Direction.Axis.X) {
			return new Direction.Axis[] { Direction.Axis.Y, Direction.Axis.Z };
		}

		if (rotationAxis == Direction.Axis.Y) {
			return new Direction.Axis[] { Direction.Axis.X, Direction.Axis.Z };
		}

		return new Direction.Axis[] { Direction.Axis.X, Direction.Axis.Y };
	}
}
