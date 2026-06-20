package mod.chiselsandbits.client.logic;

import mod.chiselsandbits.block.entities.ChiseledBlockEntity;

import net.minecraft.world.level.chunk.LevelChunk;

public class ChiseledBlockModelUpdateHandler {
	public static void updateAllModelDataInChunk(LevelChunk chunk)

	{
		chunk.getBlockEntities()
				.values()
				.forEach(blockEntity ->

				{
					if (blockEntity instanceof ChiseledBlockEntity chiseledBlockEntity) {
						chiseledBlockEntity.updateModelData();
					}
				}
				);
	}
}
