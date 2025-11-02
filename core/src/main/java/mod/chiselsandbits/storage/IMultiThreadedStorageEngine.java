package mod.chiselsandbits.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.concurrent.CompletableFuture;

/**
 * Represents a multithreaded storage engine, which can process data for IO purposes.
 */
public interface IMultiThreadedStorageEngine<TPayload>
{

    /**
     * Encodes the given payload off-thread.
     *
     * @param payload The payload to encode.
     * @param valueOutput The value output to write to.
     * @return The off-thread encode task.
     */
    CompletableFuture<Void> encodeAsync(TPayload payload, final ValueOutput valueOutput);

    /**
     * Decodes the given nbt data off-thread.
     *
     * @param valueInput The value input to read from.
     * @return The off-thread decode task.
     */
    CompletableFuture<TPayload> decodeAsync(final ValueInput valueInput);
}
