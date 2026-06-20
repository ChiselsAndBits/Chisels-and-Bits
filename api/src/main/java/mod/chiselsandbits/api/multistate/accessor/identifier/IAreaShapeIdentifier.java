package mod.chiselsandbits.api.multistate.accessor.identifier;

import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;

/**
 * Marker interface that uniquely identifies the shape of the contents that can be accessed
 * via a given {@link IAreaAccessor}.
 */
public interface IAreaShapeIdentifier {
	/**
     * The dummy instance of the identifier, in-case it is not relevant in the current context,
     * but needs to be supplied.
     */
	IAreaShapeIdentifier DUMMY = new IAreaShapeIdentifier() {
		@Override
		public int hashCode()

		{
			return 0;
		}

		@Override
		public boolean equals(final Object obj)

		{
			return obj == this;
		}
	}

	;
}
