package mod.chiselsandbits.utils;

public final class ModelDataGeneration {
	private long generation;

	public synchronized long begin() {
		return ++generation;
	}

	public synchronized boolean isCurrent(final long candidate) {
		return candidate == generation;
	}

	public synchronized boolean publishIfCurrent(final long candidate, final Runnable publish) {
		if (candidate != generation) {
			return false;
		}

		publish.run();
		return true;
	}
}
