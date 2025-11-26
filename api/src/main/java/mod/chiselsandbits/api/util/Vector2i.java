package mod.chiselsandbits.api.util;

import com.google.common.base.MoreObjects;
import com.mojang.serialization.Codec;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import java.util.stream.IntStream;

public class Vector2i implements Comparable<Vector2i>
{
    public static final Codec<Vector2i> CODEC       =
      Codec.INT_STREAM.comapFlatMap((stream) -> Util.fixedSize(stream, 3).map((componentArray) -> new Vector2i(componentArray[0], componentArray[1])),
        (vector) -> IntStream.of(vector.getX(), vector.getY()));
    /**
     * An immutable vector with zero as all coordinates.
     */
    public static final Vector2i        NULL_VECTOR = new Vector2i(0, 0);
    private             int             x;
    private             int             y;

    public Vector2i(double xIn, double yIn)
    {
        this(Mth.floor(xIn), Mth.floor(yIn));
    }

    public Vector2i(int xIn, int yIn)
    {
        this.x = xIn;
        this.y = yIn;
    }

    public int hashCode()
    {
        return this.getY() * 31 + this.getX();
    }

    public boolean equals(Object p_equals_1_)
    {
        if (this == p_equals_1_)
        {
            return true;
        }
        else if (!(p_equals_1_ instanceof Vector2i))
        {
            return false;
        }
        else
        {
            Vector2i Vector2i = (Vector2i) p_equals_1_;
            if (this.getX() != Vector2i.getX())
            {
                return false;
            }
            else
            {
                return this.getY() == Vector2i.getY();
            }
        }
    }

    /**
     * Gets the X coordinate.
     * @return The x part of the coordinate.
     */
    public int getX()
    {
        return this.x;
    }

    /**
     * Gets the Y coordinate.
     * @return The y part of the coordinate.
     */
    public int getY()
    {
        return this.y;
    }

    /**
     * Sets the Y coordinate.
     * @param yIn The new y part of the coordinate
     */
    protected void setY(int yIn)
    {
        this.y = yIn;
    }

    /**
     * Sets the X coordinate.
     * @param xIn The new x part of the coordinate.
     */
    protected void setX(int xIn)
    {
        this.x = xIn;
    }

    public String toString()
    {
        return MoreObjects.toStringHelper(this).add("x", this.getX()).add("y", this.getY()).toString();
    }

    public int compareTo(Vector2i p_compareTo_1_)
    {
        if (this.getY() == p_compareTo_1_.getY())
        {
            return this.getX() - p_compareTo_1_.getX();
        }
        else
        {
            return this.getY() - p_compareTo_1_.getY();
        }
    }

    /**
     * Offset this BlockPos 1 block up
     */
    public Vector2i up()
    {
        return this.up(1);
    }

    /**
     * Offset this BlockPos n blocks up
     */
    public Vector2i up(int n)
    {
        return this.offset(Direction.UP, n);
    }

    /**
     * Offsets this BlockPos n blocks in the given direction
     */
    public Vector2i offset(Direction facing, int n)
    {
        return n == 0 ? this : new Vector2i(this.getX() + facing.getStepX() * n, this.getY() + facing.getStepY() * n);
    }

    /**
     * Offset this BlockPos 1 block down
     */
    public Vector2i down()
    {
        return this.down(1);
    }

    /**
     * Offset this BlockPos n blocks down
     */
    public Vector2i down(int n)
    {
        return this.offset(Direction.DOWN, n);
    }

    public boolean withinDistance(Vector2i vector, double distance)
    {
        return this.distanceSq((double) vector.getX(), (double) vector.getY(), false) < distance * distance;
    }

    public double distanceSq(double x, double y, boolean useCenter)
    {
        double d0 = useCenter ? 0.5D : 0.0D;
        double d1 = (double) this.getX() + d0 - x;
        double d2 = (double) this.getY() + d0 - y;
        return d1 * d1 + d2 * d2;
    }

    public boolean withinDistance(Position position, double distance)
    {
        return this.distanceSq(position.x(), position.y(), true) < distance * distance;
    }

    /**
     * Calculate squared distance to the given Vector
     */
    public double distanceSq(Vector2i to)
    {
        return this.distanceSq((double) to.getX(), (double) to.getY(), true);
    }

    public int manhattanDistance(Vector2i vector)
    {
        float f = (float) Math.abs(vector.getX() - this.getX());
        float f1 = (float) Math.abs(vector.getY() - this.getY());
        return (int) (f + f1);
    }

    public String getCoordinatesAsString()
    {
        return "" + this.getX() + ", " + this.getY();
    }
}
