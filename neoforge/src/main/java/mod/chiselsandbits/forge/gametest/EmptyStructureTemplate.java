package mod.chiselsandbits.forge.gametest;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

/**
 * Provides an all-air structure template for the game tests, built in memory.
 * A structure file in the resources is not used, because the build's resource processing corrupts binary NBT files.
 */
final class EmptyStructureTemplate {
	private static final int SIZE = 32;

	private EmptyStructureTemplate() {
		throw new IllegalStateException("Can not instantiate an instance of: EmptyStructureTemplate. This is a utility class");
	}

	static void register(final MinecraftServer server, final Identifier id) {
		final ListTag size = new ListTag();

		for (int i = 0; i < 3; i++) {
			size.add(IntTag.valueOf(SIZE));
		}

		final CompoundTag air = new CompoundTag();
		air.putString("Name", "minecraft:air");
		final ListTag palette = new ListTag();
		palette.add(air);

		final CompoundTag tag = new CompoundTag();
		tag.putInt("DataVersion", SharedConstants.getCurrentVersion().dataVersion().version());
		tag.put("size", size);
		tag.put("palette", palette);
		tag.put("blocks", new ListTag());
		tag.put("entities", new ListTag());

		server.getStructureManager().getOrCreate(id).load(server.registryAccess().lookupOrThrow(Registries.BLOCK), tag);
	}
}
