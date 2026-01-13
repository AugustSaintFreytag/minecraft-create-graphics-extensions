package net.saint.createrenderfixer.dh;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.seibel.distanthorizons.api.enums.rendering.EDhApiBlockMaterial;
import com.seibel.distanthorizons.api.objects.math.DhApiVec3d;
import com.seibel.distanthorizons.api.objects.render.DhApiRenderableBox;

import net.minecraft.core.Direction;
import net.saint.createrenderfixer.Mod;

public final class WindmillLODTransformUtil {

	// Models

	private record BladeLengths(float widthLength, float heightLength) {
	}

	private record BladeSegmentCounts(int widthSegmentCount, int heightSegmentCount) {
	}

	// API

	public static List<DhApiRenderableBox> makeWindmillCrossBoxes(WindmillLODEntry entry, float rotationAngle, float bladeThickness,
			Color bladeColor, EDhApiBlockMaterial bladeMaterial) {
		if (entry == null) {
			return List.of();
		}

		var bladeGeometry = entry.bladeGeometry;
		var bladeLengths = new BladeLengths(bladeGeometry.widthLength(), bladeGeometry.heightLength());
		var segmentCounts = new BladeSegmentCounts(bladeGeometry.widthSegmentCount(), bladeGeometry.heightSegmentCount());
		var bladeWidth = bladeGeometry.bladeWidth();
		var thicknessScale = getThicknessScaleForRotationAngle(rotationAngle);
		var baseBoxes = getCrossBoxesForAxis(entry.rotationAxis, bladeLengths, segmentCounts, bladeWidth, bladeThickness, thicknessScale,
				bladeColor, bladeMaterial);

		return rotateBoxesForAxis(baseBoxes, entry.rotationAxis, rotationAngle);
	}

	// Utility

	private static List<DhApiRenderableBox> getCrossBoxesForAxis(Direction.Axis rotationAxis, BladeLengths bladeLengths,
			BladeSegmentCounts segmentCounts, float bladeWidth, float bladeThickness, float thicknessScale, Color bladeColor,
			EDhApiBlockMaterial bladeMaterial) {
		return switch (rotationAxis) {
			case X -> getCrossBoxesForXAxis(bladeLengths, segmentCounts, bladeWidth, bladeThickness, thicknessScale, bladeColor,
					bladeMaterial);
			case Y -> getCrossBoxesForYAxis(bladeLengths, segmentCounts, bladeWidth, bladeThickness, thicknessScale, bladeColor,
					bladeMaterial);
			case Z -> getCrossBoxesForZAxis(bladeLengths, segmentCounts, bladeWidth, bladeThickness, thicknessScale, bladeColor,
					bladeMaterial);
		};
	}

	private static List<DhApiRenderableBox> getCrossBoxesForXAxis(BladeLengths bladeLengths, BladeSegmentCounts segmentCounts,
			float bladeWidth, float bladeThickness, float thicknessScale, Color bladeColor, EDhApiBlockMaterial bladeMaterial) {
		var boxes = new ArrayList<DhApiRenderableBox>();
		addBladeSegmentsForAxis(boxes, Direction.Axis.Z, Direction.Axis.X, bladeLengths.widthLength(), segmentCounts.widthSegmentCount(),
				bladeWidth, bladeThickness, thicknessScale, bladeColor, bladeMaterial);
		addBladeSegmentsForAxis(boxes, Direction.Axis.Y, Direction.Axis.X, bladeLengths.heightLength(), segmentCounts.heightSegmentCount(),
				bladeWidth, bladeThickness, thicknessScale, bladeColor, bladeMaterial);

		return boxes;
	}

	private static List<DhApiRenderableBox> getCrossBoxesForYAxis(BladeLengths bladeLengths, BladeSegmentCounts segmentCounts,
			float bladeWidth, float bladeThickness, float thicknessScale, Color bladeColor, EDhApiBlockMaterial bladeMaterial) {
		var boxes = new ArrayList<DhApiRenderableBox>();
		addBladeSegmentsForAxis(boxes, Direction.Axis.X, Direction.Axis.Y, bladeLengths.widthLength(), segmentCounts.widthSegmentCount(),
				bladeWidth, bladeThickness, thicknessScale, bladeColor, bladeMaterial);
		addBladeSegmentsForAxis(boxes, Direction.Axis.Z, Direction.Axis.Y, bladeLengths.heightLength(), segmentCounts.heightSegmentCount(),
				bladeWidth, bladeThickness, thicknessScale, bladeColor, bladeMaterial);

		return boxes;
	}

	private static List<DhApiRenderableBox> getCrossBoxesForZAxis(BladeLengths bladeLengths, BladeSegmentCounts segmentCounts,
			float bladeWidth, float bladeThickness, float thicknessScale, Color bladeColor, EDhApiBlockMaterial bladeMaterial) {
		var boxes = new ArrayList<DhApiRenderableBox>();
		addBladeSegmentsForAxis(boxes, Direction.Axis.X, Direction.Axis.Z, bladeLengths.widthLength(), segmentCounts.widthSegmentCount(),
				bladeWidth, bladeThickness, thicknessScale, bladeColor, bladeMaterial);
		addBladeSegmentsForAxis(boxes, Direction.Axis.Y, Direction.Axis.Z, bladeLengths.heightLength(), segmentCounts.heightSegmentCount(),
				bladeWidth, bladeThickness, thicknessScale, bladeColor, bladeMaterial);

		return boxes;
	}

	private static void addBladeSegmentsForAxis(List<DhApiRenderableBox> boxes, Direction.Axis bladeAxis, Direction.Axis rotationAxis,
			float bladeLength, int segmentCount, float bladeWidth, float bladeThickness, float thicknessScale, Color bladeColor,
			EDhApiBlockMaterial bladeMaterial) {
		var segmentLength = bladeLength / segmentCount;
		var halfSegmentLength = segmentLength / 2.0F;
		var startOffset = -bladeLength / 2.0F + halfSegmentLength;
		var halfBladeWidth = bladeWidth / 2.0F;
		var halfBladeThickness = bladeThickness / 2.0F;
		var halfThicknessX = halfBladeThickness;
		var halfThicknessY = halfBladeThickness;
		var halfThicknessZ = halfBladeThickness;

		var widthAxis = getInPlaneWidthAxisForBlade(rotationAxis, bladeAxis);

		switch (widthAxis) {
			case X -> halfThicknessX = halfBladeWidth;
			case Y -> halfThicknessY = halfBladeWidth;
			case Z -> halfThicknessZ = halfBladeWidth;
		}

		switch (widthAxis) {
			case X -> halfThicknessX *= thicknessScale;
			case Y -> halfThicknessY *= thicknessScale;
			case Z -> halfThicknessZ *= thicknessScale;
		}

		for (var index = 0; index < segmentCount; index++) {
			var offset = startOffset + segmentLength * index;
			var minimumX = -halfThicknessX;
			var maximumX = halfThicknessX;
			var minimumY = -halfThicknessY;
			var maximumY = halfThicknessY;
			var minimumZ = -halfThicknessZ;
			var maximumZ = halfThicknessZ;

			switch (bladeAxis) {
				case X -> {
					minimumX = offset - halfSegmentLength;
					maximumX = offset + halfSegmentLength;
				}
				case Y -> {
					minimumY = offset - halfSegmentLength;
					maximumY = offset + halfSegmentLength;
				}
				case Z -> {
					minimumZ = offset - halfSegmentLength;
					maximumZ = offset + halfSegmentLength;
				}
			}

			boxes.add(createBox(minimumX, minimumY, minimumZ, maximumX, maximumY, maximumZ, bladeColor, bladeMaterial));
		}
	}

	private static Direction.Axis getInPlaneWidthAxisForBlade(Direction.Axis rotationAxis, Direction.Axis bladeAxis) {
		for (var axis : Direction.Axis.values()) {
			if (axis != rotationAxis && axis != bladeAxis) {
				return axis;
			}
		}

		return bladeAxis;
	}

	private static float getThicknessScaleForRotationAngle(float rotationAngle) {
		var radians = Math.toRadians(rotationAngle);
		var weight = (Math.cos(radians * 4.0) + 1.0) / 2.0;
		var maximumThicknessScale = Mod.CONFIG.windmillBladeRotationThicknessScaleMaximum;
		var scale = 1.0 + (maximumThicknessScale - 1.0) * weight;

		return (float) scale;
	}

	private static DhApiRenderableBox createBox(double minimumX, double minimumY, double minimumZ, double maximumX, double maximumY,
			double maximumZ, Color bladeColor, EDhApiBlockMaterial bladeMaterial) {
		var minimumPos = new DhApiVec3d(minimumX, minimumY, minimumZ);
		var maximumPos = new DhApiVec3d(maximumX, maximumY, maximumZ);

		return new DhApiRenderableBox(minimumPos, maximumPos, bladeColor, bladeMaterial);
	}

	private static List<DhApiRenderableBox> rotateBoxesForAxis(List<DhApiRenderableBox> boxes, Direction.Axis rotationAxis,
			float rotationAngle) {
		if (boxes == null || boxes.isEmpty()) {
			return List.of();
		}

		var radians = Math.toRadians(rotationAngle);
		var sin = Math.sin(radians);
		var cos = Math.cos(radians);
		var rotated = new ArrayList<DhApiRenderableBox>(boxes.size());

		for (var box : boxes) {
			rotated.add(getRotatedBoxForAxis(box, rotationAxis, sin, cos));
		}

		return rotated;
	}

	private static DhApiRenderableBox getRotatedBoxForAxis(DhApiRenderableBox box, Direction.Axis rotationAxis, double sin, double cos) {
		var minimumX = box.minPos.x;
		var minimumY = box.minPos.y;
		var minimumZ = box.minPos.z;
		var maximumX = box.maxPos.x;
		var maximumY = box.maxPos.y;
		var maximumZ = box.maxPos.z;

		var rotatedMinimumX = Double.POSITIVE_INFINITY;
		var rotatedMinimumY = Double.POSITIVE_INFINITY;
		var rotatedMinimumZ = Double.POSITIVE_INFINITY;
		var rotatedMaximumX = Double.NEGATIVE_INFINITY;
		var rotatedMaximumY = Double.NEGATIVE_INFINITY;
		var rotatedMaximumZ = Double.NEGATIVE_INFINITY;

		for (var x : new double[] { minimumX, maximumX }) {
			for (var y : new double[] { minimumY, maximumY }) {
				for (var z : new double[] { minimumZ, maximumZ }) {
					var rotatedPoint = rotatePointForAxis(rotationAxis, x, y, z, sin, cos);

					rotatedMinimumX = Math.min(rotatedMinimumX, rotatedPoint.x);
					rotatedMinimumY = Math.min(rotatedMinimumY, rotatedPoint.y);
					rotatedMinimumZ = Math.min(rotatedMinimumZ, rotatedPoint.z);
					rotatedMaximumX = Math.max(rotatedMaximumX, rotatedPoint.x);
					rotatedMaximumY = Math.max(rotatedMaximumY, rotatedPoint.y);
					rotatedMaximumZ = Math.max(rotatedMaximumZ, rotatedPoint.z);
				}
			}
		}

		var minimumPos = new DhApiVec3d(rotatedMinimumX, rotatedMinimumY, rotatedMinimumZ);
		var maximumPos = new DhApiVec3d(rotatedMaximumX, rotatedMaximumY, rotatedMaximumZ);

		var color = box.color;
		var material = EDhApiBlockMaterial.getFromIndex(box.material);

		return new DhApiRenderableBox(minimumPos, maximumPos, color, material);
	}

	private static DhApiVec3d rotatePointForAxis(Direction.Axis rotationAxis, double x, double y, double z, double sin, double cos) {
		return switch (rotationAxis) {
			case X -> new DhApiVec3d(x, y * cos - z * sin, y * sin + z * cos);
			case Y -> new DhApiVec3d(x * cos + z * sin, y, -x * sin + z * cos);
			case Z -> new DhApiVec3d(x * cos - y * sin, x * sin + y * cos, z);
		};
	}
}
