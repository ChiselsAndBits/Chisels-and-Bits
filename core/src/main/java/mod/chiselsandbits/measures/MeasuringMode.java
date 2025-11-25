package mod.chiselsandbits.measures;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import mod.chiselsandbits.api.measuring.IMeasuringMode;
import mod.chiselsandbits.api.measuring.IMeasuringType;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.client.icon.IconManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static net.minecraft.world.item.DyeColor.*;

public enum MeasuringMode implements IMeasuringMode
{
    WHITE_BIT(WHITE, MeasuringType.BIT, Component.translatable(Constants.MOD_ID + ".measuring.types.bit.white"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    ORANGE_BIT(ORANGE, MeasuringType.BIT, Component.translatable(Constants.MOD_ID + ".measuring.types.bit.orange"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    MAGENTA_BIT(MAGENTA, MeasuringType.BIT, Component.translatable(Constants.MOD_ID + ".measuring.types.bit.magenta"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    LIGHT_BLUE_BIT(LIGHT_BLUE, MeasuringType.BIT, Component.translatable(Constants.MOD_ID + ".measuring.types.bit.light-blue"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    YELLOW_BIT(YELLOW, MeasuringType.BIT, Component.translatable(Constants.MOD_ID + ".measuring.types.bit.yellow"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    LIME_BIT(LIME, MeasuringType.BIT, Component.translatable(Constants.MOD_ID + ".measuring.types.bit.lime"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    PINK_BIT(PINK, MeasuringType.BIT, Component.translatable(Constants.MOD_ID + ".measuring.types.bit.pink"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    GRAY_BIT(GRAY, MeasuringType.BIT, Component.translatable(Constants.MOD_ID + ".measuring.types.bit.gray"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    LIGHT_GRAY_BIT(LIGHT_GRAY, MeasuringType.BIT, Component.translatable(Constants.MOD_ID + ".measuring.types.bit.light-gray"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    CYAN_BIT(CYAN, MeasuringType.BIT, Component.translatable(Constants.MOD_ID + ".measuring.types.bit.cyan-gray"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    PURPLE_BIT(PURPLE, MeasuringType.BIT, Component.translatable(Constants.MOD_ID + ".measuring.types.bit.purple"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    BLUE_BIT(BLUE, MeasuringType.BIT, Component.translatable(Constants.MOD_ID + ".measuring.types.bit.blue"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    BROWN_BIT(BROWN, MeasuringType.BIT, Component.translatable(Constants.MOD_ID + ".measuring.types.bit.brown"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    GREEN_BIT(GREEN, MeasuringType.BIT, Component.translatable(Constants.MOD_ID + ".measuring.types.bit.green"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    RED_BIT(RED, MeasuringType.BIT, Component.translatable(Constants.MOD_ID + ".measuring.types.bit.red"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    BLACK_BIT(BLACK, MeasuringType.BIT, Component.translatable(Constants.MOD_ID + ".measuring.types.bit.black"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    WHITE_BLOCK(WHITE, MeasuringType.BLOCK, Component.translatable(Constants.MOD_ID + ".measuring.types.block.white"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    ORANGE_BLOCK(ORANGE, MeasuringType.BLOCK, Component.translatable(Constants.MOD_ID + ".measuring.types.block.orange"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    MAGENTA_BLOCK(MAGENTA, MeasuringType.BLOCK, Component.translatable(Constants.MOD_ID + ".measuring.types.block.magenta"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    LIGHT_BLUE_BLOCK(LIGHT_BLUE, MeasuringType.BLOCK, Component.translatable(Constants.MOD_ID + ".measuring.types.block.light-blue"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    YELLOW_BLOCK(YELLOW, MeasuringType.BLOCK, Component.translatable(Constants.MOD_ID + ".measuring.types.block.yellow"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    LIME_BLOCK(LIME, MeasuringType.BLOCK, Component.translatable(Constants.MOD_ID + ".measuring.types.block.lime"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    PINK_BLOCK(PINK, MeasuringType.BLOCK, Component.translatable(Constants.MOD_ID + ".measuring.types.block.pink"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    GRAY_BLOCK(GRAY, MeasuringType.BLOCK, Component.translatable(Constants.MOD_ID + ".measuring.types.block.gray"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    LIGHT_GRAY_BLOCK(LIGHT_GRAY, MeasuringType.BLOCK, Component.translatable(Constants.MOD_ID + ".measuring.types.block.light-gray"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    CYAN_BLOCK(CYAN, MeasuringType.BLOCK, Component.translatable(Constants.MOD_ID + ".measuring.types.block.cyan-gray"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    PURPLE_BLOCK(PURPLE, MeasuringType.BLOCK, Component.translatable(Constants.MOD_ID + ".measuring.types.block.purple"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    BLUE_BLOCK(BLUE, MeasuringType.BLOCK, Component.translatable(Constants.MOD_ID + ".measuring.types.block.blue"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    BROWN_BLOCK(BROWN, MeasuringType.BLOCK, Component.translatable(Constants.MOD_ID + ".measuring.types.block.brown"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    GREEN_BLOCK(GREEN, MeasuringType.BLOCK, Component.translatable(Constants.MOD_ID + ".measuring.types.block.green"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    RED_BLOCK(RED, MeasuringType.BLOCK, Component.translatable(Constants.MOD_ID + ".measuring.types.block.red"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    BLACK_BLOCK(BLACK, MeasuringType.BLOCK, Component.translatable(Constants.MOD_ID + ".measuring.types.block.black"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    WHITE_DISTANCE(WHITE, MeasuringType.DISTANCE, Component.translatable(Constants.MOD_ID + ".measuring.types.distance.white"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    ORANGE_DISTANCE(ORANGE, MeasuringType.DISTANCE, Component.translatable(Constants.MOD_ID + ".measuring.types.distance.orange"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    MAGENTA_DISTANCE(MAGENTA, MeasuringType.DISTANCE, Component.translatable(Constants.MOD_ID + ".measuring.types.distance.magenta"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    LIGHT_BLUE_DISTANCE(LIGHT_BLUE, MeasuringType.DISTANCE, Component.translatable(Constants.MOD_ID + ".measuring.types.distance.light-blue"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    YELLOW_DISTANCE(YELLOW, MeasuringType.DISTANCE, Component.translatable(Constants.MOD_ID + ".measuring.types.distance.yellow"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    LIME_DISTANCE(LIME, MeasuringType.DISTANCE, Component.translatable(Constants.MOD_ID + ".measuring.types.distance.lime"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    PINK_DISTANCE(PINK, MeasuringType.DISTANCE, Component.translatable(Constants.MOD_ID + ".measuring.types.distance.pink"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    GRAY_DISTANCE(GRAY, MeasuringType.DISTANCE, Component.translatable(Constants.MOD_ID + ".measuring.types.distance.gray"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    LIGHT_GRAY_DISTANCE(LIGHT_GRAY, MeasuringType.DISTANCE, Component.translatable(Constants.MOD_ID + ".measuring.types.distance.light-gray"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    CYAN_DISTANCE(CYAN, MeasuringType.DISTANCE, Component.translatable(Constants.MOD_ID + ".measuring.types.distance.cyan-gray"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    PURPLE_DISTANCE(PURPLE, MeasuringType.DISTANCE, Component.translatable(Constants.MOD_ID + ".measuring.types.distance.purple"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    BLUE_DISTANCE(BLUE, MeasuringType.DISTANCE, Component.translatable(Constants.MOD_ID + ".measuring.types.distance.blue"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    BROWN_DISTANCE(BROWN, MeasuringType.DISTANCE, Component.translatable(Constants.MOD_ID + ".measuring.types.distance.brown"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    GREEN_DISTANCE(GREEN, MeasuringType.DISTANCE, Component.translatable(Constants.MOD_ID + ".measuring.types.distance.green"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    RED_DISTANCE(RED, MeasuringType.DISTANCE, Component.translatable(Constants.MOD_ID + ".measuring.types.distance.red"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white")),
    BLACK_DISTANCE(BLACK, MeasuringType.DISTANCE, Component.translatable(Constants.MOD_ID + ".measuring.types.distance.black"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white"));

    public static final Codec<MeasuringMode>                CODEC        = Codec.STRING.xmap(MeasuringMode::valueOf, Enum::name);
    public static final StreamCodec<ByteBuf, MeasuringMode> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(i -> MeasuringMode.values()[i], Enum::ordinal);

    private final DyeColor           color;
    private final MeasuringType      type;
    private final Component          displayName;
    private final ResourceLocation icon;

    MeasuringMode(final DyeColor color, final MeasuringType type, final Component displayName, final ResourceLocation icon)
    {
        this.color = color;
        this.type = type;
        this.displayName = displayName;
        this.icon = icon;
    }

    @Override
    public ResourceLocation getIcon()
    {
        return icon;
    }

    @Override
    public @NotNull Optional<IMeasuringType> getGroup()
    {
        return Optional.of(getType());
    }

    @Override
    public boolean shouldRenderDisplayNameInMenu()
    {
        return false;
    }

    @Override
    public @NotNull Vec3 getColorVector()
    {
        final int color = getColor().getTextColor();

        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        return new Vec3(r, g, b);
    }

    @Override
    public Component getDisplayName()
    {
        return this.displayName;
    }

    @Override
    public DyeColor getColor()
    {
        return this.color;
    }

    @Override
    public MeasuringType getType()
    {
        return this.type;
    }
}
