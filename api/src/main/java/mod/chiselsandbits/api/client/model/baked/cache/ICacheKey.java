package mod.chiselsandbits.api.client.model.baked.cache;

/**
 * Marker interface for a cache key.
 */
public interface ICacheKey
{

    record IntBased(int value) implements ICacheKey
    {
    }
}
