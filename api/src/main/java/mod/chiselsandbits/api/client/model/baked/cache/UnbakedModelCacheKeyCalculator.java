package mod.chiselsandbits.api.client.model.baked.cache;

import net.minecraft.client.resources.model.UnbakedModel;

public interface UnbakedModelCacheKeyCalculator<T extends UnbakedModel>
{

    /**
     * Calculate the cache key for the given model.
     *
     * @param model the model to calculate the key for.
     * @param randomSeed the random seed to use.
     * @return the key.
     */
    ICacheKey calculate(final T model, long randomSeed);
}
