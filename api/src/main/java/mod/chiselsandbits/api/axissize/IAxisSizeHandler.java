package mod.chiselsandbits.api.axissize;

/**
 * Represents a handler which can indicate the current active maximum and minimum size
 * of the multistate object.
 */
public interface IAxisSizeHandler
{
	static IAxisSizeHandler empty() {
		return new IAxisSizeHandler() {
			@Override
			public int getLowest()

			{
				return 0;
			}

			@Override
			public int getHighest()

			{
				return 0;
			}
		};
	}

	/**
     * The lowest set value on the axis of the size handler.
     *
     * @return The lowest set value.
     */
	int getLowest();

	/**
     * The highest set value on the axis of the size handler.
     *
     * @return The highest set value.
     */
	int getHighest();
}
