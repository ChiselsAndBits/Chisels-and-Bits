package mod.chiselsandbits.utils;

public class SimpleInstanceCache<X, Y> {
	X equalityValue;
	Y cacheInstance;

	public SimpleInstanceCache() {
	}

	public SimpleInstanceCache(
	  final X defaultEquality,
	  final Y defaultValue)

	{
		equalityValue = defaultEquality;
		cacheInstance = defaultValue;
	}

	public boolean needsUpdate(
	  final X testValue)

	{
		try

		{
			if (equalityValue == null != (testValue == null))

			{
				return true;
			}

			return !isEqual(equalityValue, testValue);
		}

		finally

		{
			equalityValue = testValue;
		}
	}

	private boolean isEqual(
	  final X a,
	  final X b)

	{
		return a == b;
	}

	public Y getCached()

	{
		return cacheInstance;
	}

	public void updateCachedValue(
	  final Y value)

	{
		cacheInstance = value;
	}
}
