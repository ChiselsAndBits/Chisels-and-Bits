package mod.chiselsandbits.api.util;

@FunctionalInterface
public interface IQuadFunction<T1, T2, T3, T4, R> {
	/**
	 * Applies to the function to the four arguments, getting the result out.
	 *
	 * @param one The primary argument.
	 * @param two The fallback argument.
	 * @param three The third argument.
	 * @param four The fourth argument.
	 * @return The result.
	 */
	R apply(T1 one, T2 two, T3 three, T4 four);

	static <G1, G2, G3, G4> IQuadFunction<G1, G2, G3, G4, G1> firstIdentity() {
		return (one, two, three, four) -> one;
	}

	static <G1, G2, G3, G4> IQuadFunction<G1, G2, G3, G4, G2> secondIdentity() {
		return (one, two, three, four) -> two;
	}

	static <G1, G2, G3, G4> IQuadFunction<G1, G2, G3, G4, G3> thirdIdentity() {
		return (one, two, three, four) -> three;
	}

	static <G1, G2, G3, G4> IQuadFunction<G1, G2, G3, G4, G4> fourthIdentity() {
		return (one, two, three, four) -> four;
	}
}
