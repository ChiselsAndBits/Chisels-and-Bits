package mod.chiselsandbits.api.util;

/**
 * Function that accepts four arguments and produces a result.
 *
 * @param <T1> type of the first input
 * @param <T2> type of the second input
 * @param <T3> type of the third input
 * @param <T4> type of the fourth input
 * @param <R> result type
 */

@FunctionalInterface
public interface IQuadFunction<T1, T2, T3, T4, R> {
	/**
	 * Applies this function to four arguments.
	 *
	 * @param one the first argument
	 * @param two the second argument
	 * @param three the third argument
	 * @param four the fourth argument
	 * @return the function result
	 */
	R apply(T1 one, T2 two, T3 three, T4 four);

	/**
	 * Creates a four-argument function that returns its first argument.
	 *
	 * @param <G1> type of the first argument and function result
	 * @param <G2> type of the second argument
	 * @param <G3> type of the third argument
	 * @param <G4> type of the fourth argument
	 * @return a function that returns its first argument
	 */
	static <G1, G2, G3, G4> IQuadFunction<G1, G2, G3, G4, G1> firstIdentity() {
		return (one, two, three, four) -> one;
	}

	/**
	 * Creates a four-argument function that returns its second argument.
	 *
	 * @param <G1> type of the first argument
	 * @param <G2> type of the second argument and function result
	 * @param <G3> type of the third argument
	 * @param <G4> type of the fourth argument
	 * @return a function that returns its second argument
	 */
	static <G1, G2, G3, G4> IQuadFunction<G1, G2, G3, G4, G2> secondIdentity() {
		return (one, two, three, four) -> two;
	}

	/**
	 * Creates a four-argument function that returns its third argument.
	 *
	 * @param <G1> type of the first argument
	 * @param <G2> type of the second argument
	 * @param <G3> type of the third argument and function result
	 * @param <G4> type of the fourth argument
	 * @return a function that returns its third argument
	 */
	static <G1, G2, G3, G4> IQuadFunction<G1, G2, G3, G4, G3> thirdIdentity() {
		return (one, two, three, four) -> three;
	}

	/**
	 * Creates a four-argument function that returns its fourth argument.
	 *
	 * @param <G1> type of the first argument
	 * @param <G2> type of the second argument
	 * @param <G3> type of the third argument
	 * @param <G4> type of the fourth argument and function result
	 * @return a function that returns its fourth argument
	 */
	static <G1, G2, G3, G4> IQuadFunction<G1, G2, G3, G4, G4> fourthIdentity() {
		return (one, two, three, four) -> four;
	}
}
