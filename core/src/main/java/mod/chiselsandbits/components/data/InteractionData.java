package mod.chiselsandbits.components.data;

import com.mojang.serialization.Codec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record InteractionData(ItemStack stack) {
	public static final InteractionData EMPTY = new InteractionData(ItemStack.EMPTY);

	public static final Codec<InteractionData> CODEC = ItemStack.OPTIONAL_CODEC.xmap(InteractionData::new, InteractionData::stack);

	public static final StreamCodec<RegistryFriendlyByteBuf, InteractionData> STREAM_CODEC = ItemStack.STREAM_CODEC.map(InteractionData::new, InteractionData::stack);
}
