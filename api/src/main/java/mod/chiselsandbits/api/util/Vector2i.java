package mod.chiselsandbits.api.util;

import java.util.stream.IntStream;

import com.google.common.base.MoreObjects;
import com.mojang.serialization.Codec;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

/**
 * Mutable integer vector with two coordinates and helpers for grid-distance calculations.
 */

public class Vector2i implements Comparable<Vector2i> {
	/**
	 * Codec for a two-coordinate vector represented by an integer stream.
	 */
	public static final Codec<Vector2i> CODEC =
			Codec.INT_STREAM.comapFlatMap((stream) -> Util.fixedSize(stream, 3).map((componentArray) -> new Vector2i(componentArray[0], componentArray[1])),
						(vector) -> IntStream.of(vector.getX(), vector.getY()));
	/**
	 * An immutable vector with zero as all coordinates.
	 */
	public static final Vector2i NULL_VECTOR = new Vector2i(0, 0);
	private int x;
	private int y;

	/**
	 * Creates a vector by flooring each coordinate to the nearest integer not greater than it.
	 *
	 * @param xIn the x coordinate before flooring
	 * @param yIn the y coordinate before flooring
	 */
	public Vector2i(double xIn, double yIn) {
		this(Mth.floor(xIn), Mth.floor(yIn));
	}

	/**
	 * Creates a vector from integer coordinates.
	 *
	 * @param xIn the x coordinate
	 * @param yIn the y coordinate
	 */
	public Vector2i(int xIn, int yIn) {
		this.x = xIn;
		this.y = yIn;
	}

	/**
	 * Returns a hash derived from this vector's coordinates.
	 */
	@Override
	public int hashCode() {
		return this.getY() * 31 + this.getX();
	}

	/**
	 * Compares this vector with another object by x and y coordinates.
	 *
	 * @param p_equals_1_ the object to compare
	 * @return {@code true} when the object is a vector with matching coordinates
	 */
	@Override
	public boolean equals(Object p_equals_1_) {
		if (this == p_equals_1_) {
			return true;
		} else if (!(p_equals_1_ instanceof Vector2i)) {
			return false;
		} else {
			Vector2i Vector2i = (Vector2i) p_equals_1_;

			if (this.getX() != Vector2i.getX()) {
				return false;
			} else {
				return this.getY() == Vector2i.getY();
			}
		}
	}

	/**
	 * Gets the X coordinate.
	 *
	 * @return The x part of the coordinate.
	 */
	public int getX() {
		return this.x;
	}

	/**
	 * Gets the Y coordinate.
	 *
	 * @return The y part of the coordinate.
	 */
	public int getY() {
		return this.y;
	}

	/**
	 * Sets the Y coordinate.
	 *
	 * @param yIn The new y part of the coordinate
	 */
	protected void setY(int yIn) {
		this.y = yIn;
	}

	/**
	 * Sets the X coordinate.
	 *
	 * @param xIn The new x part of the coordinate.
	 */
	protected void setX(int xIn) {
		this.x = xIn;
	}

	/**
	 * Returns a string containing this vector's x and y coordinates.
	 */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("x", this.getX()).add("y", this.getY()).toString();
	}

	/**
	 * Orders vectors by y coordinate, then by x coordinate.
	 *
	 * @param p_compareTo_1_ the vector to compare with
	 * @return a negative value, zero, or a positive value according to the coordinate ordering
	 */
	@Override
	public int compareTo(Vector2i p_compareTo_1_) {
		if (this.getY() == p_compareTo_1_.getY()) {
			return this.getX() - p_compareTo_1_.getX();
		} else {
			return this.getY() - p_compareTo_1_.getY();
		}
	}

	/**
	 * Returns this vector offset by one step upward.
	 *
	 * @return the offset vector
	 */
	public Vector2i up() {
		return this.up(1);
	}

	/**
	 * Offsets this vector {@code n} steps upward.
	 *
	 * @param n number of steps to move
	 * @return the offset vector, or this vector when {@code n} is zero
	 */
	public Vector2i up(int n) {
		return this.offset(Direction.UP, n);
	}

	/**
	 * Offsets this vector {@code n} steps in the given direction.
	 *
	 * @param facing direction of movement
	 * @param n number of steps to move
	 * @return the offset vector, or this vector when {@code n} is zero
	 */
	public Vector2i offset(Direction facing, int n) {
		return n == 0 ? this : new Vector2i(this.getX() + facing.getStepX() * n, this.getY() + facing.getStepY() * n);
	}

	/**
	 * Returns this vector offset by one step downward.
	 *
	 * @return the offset vector
	 */
	public Vector2i down() {
		return this.down(1);
	}

	/**
	 * Returns this vector offset downward by the requested number of steps.
	 *
	 * @param n number of steps to move
	 * @return the offset vector, or this vector when {@code n} is zero
	 */
	public Vector2i down(int n) {
		return this.offset(Direction.DOWN, n);
	}

	/**
	 * Tests whether the squared distance between the coordinates is less than the squared limit.
	 *
	 * @param vector the other vector
	 * @param distance maximum distance
	 * @return {@code true} when the vectors are strictly closer than {@code distance}
	 */
	public boolean withinDistance(Vector2i vector, double distance) {
		return this.distanceSq((double) vector.getX(), (double) vector.getY(), false) < distance * distance;
	}

	/**
	 * Calculates squared distance to a point, optionally measuring from this vector's cell center.
	 *
	 * @param x target x coordinate
	 * @param y target y coordinate
	 * @param useCenter whether to offset this vector's coordinates by one half
	 * @return squared Euclidean distance
	 */
	public double distanceSq(double x, double y, boolean useCenter) {
		double d0 = useCenter ? 0.5D : 0.0D;
		double d1 = (double) this.getX() + d0 - x;
		double d2 = (double) this.getY() + d0 - y;
		return d1 * d1 + d2 * d2;
	}

	/**
	 * Tests whether this vector's cell center is strictly within the given distance of a position.
	 *
	 * @param position the position to measure to
	 * @param distance maximum distance
	 * @return {@code true} when the distance is less than {@code distance}
	 */
	public boolean withinDistance(Position position, double distance) {
		return this.distanceSq(position.x(), position.y(), true) < distance * distance;
	}

	/**
	 * Calculates squared distance from this cell's center to the given vector.
	 *
	 * @param to target vector
	 * @return squared Euclidean distance
	 */
	public double distanceSq(Vector2i to) {
		return this.distanceSq((double) to.getX(), (double) to.getY(), true);
	}

	/**
	 * Calculates the Manhattan distance between this vector and another.
	 *
	 * @param vector the other vector
	 * @return the sum of absolute coordinate differences
	 */
	public int manhattanDistance(Vector2i vector) {
		float f = (float) Math.abs(vector.getX() - this.getX());
		float f1 = (float) Math.abs(vector.getY() - this.getY());
		return (int) (f + f1);
	}

	/**
	 * Returns the coordinates as a comma-separated {@code x, y} string.
	 *
	 * @return the x and y coordinates separated by a comma and space
	 */
	public String getCoordinatesAsString() {
		return "" + this.getX() + ", " + this.getY();
	}
}
