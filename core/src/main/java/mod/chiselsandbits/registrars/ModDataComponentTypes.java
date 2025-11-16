package mod.chiselsandbits.registrars;

import com.communi.suggestu.scena.core.registries.deferred.IRegistrar;
import com.communi.suggestu.scena.core.registries.deferred.IRegistryObject;
import com.mojang.serialization.Codec;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.chiseling.mode.IChiselMode;
import mod.chiselsandbits.api.modification.operation.IModificationOperation;
import mod.chiselsandbits.api.pattern.placement.IPatternPlacementType;
import mod.chiselsandbits.api.serialization.CBStreamCodecs;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.api.util.constants.NbtConstants;
import mod.chiselsandbits.components.data.InteractionData;
import mod.chiselsandbits.components.data.MultiStateItemStackData;
import mod.chiselsandbits.components.data.SlottedBitInventoryData;
import mod.chiselsandbits.measures.MeasuringMode;
import mod.chiselsandbits.registries.RegistryManager;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModDataComponentTypes {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final IRegistrar<DataComponentType<?>> REGISTRAR = IRegistrar.create(Registries.DATA_COMPONENT_TYPE, Constants.MOD_ID);

    public static IRegistryObject<DataComponentType<SlottedBitInventoryData>> SLOTTED_BIT_INVENTORY_DATA = REGISTRAR.register(
            NbtConstants.SLOTTED_BIT_INVENTORY_DATA,
            () -> DataComponentType.<SlottedBitInventoryData>builder()
                    .persistent(SlottedBitInventoryData.CODEC)
                    .networkSynchronized(SlottedBitInventoryData.STREAM_CODEC)
                    .build()
    );

    public static IRegistryObject<DataComponentType<BlockInformation>> BLOCK_INFORMATION = REGISTRAR.register(
            NbtConstants.BLOCK_INFORMATION,
            () -> DataComponentType.<BlockInformation>builder()
                    .persistent(BlockInformation.CODEC)
                    .networkSynchronized(BlockInformation.STREAM_CODEC)
                    .build()
    );

    public static IRegistryObject<DataComponentType<IChiselMode>> CHISEL_MODE = REGISTRAR.register(
            NbtConstants.CHISEL_MODE,
            () -> DataComponentType.<IChiselMode>builder()
                    .persistent(RegistryManager.getInstance().getChiselModeRegistry().byNameCodec())
                    .networkSynchronized(RegistryManager.getInstance().getChiselModeRegistry().byNameStreamCodec())
                    .build()
    );

    public static IRegistryObject<DataComponentType<MultiStateItemStackData>> MULTI_STATE_ITEM_STACK_DATA = REGISTRAR.register(
            NbtConstants.MULTI_STATE_ITEM_STACK_DATA,
            () -> DataComponentType.<MultiStateItemStackData>builder()
                    .persistent(MultiStateItemStackData.CODEC)
                    .networkSynchronized(MultiStateItemStackData.STREAM_CODEC)
                    .build()
    );

    public static IRegistryObject<DataComponentType<Integer>> COUNT = REGISTRAR.register(
            NbtConstants.COUNT,
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
                    .build()
    );

    public static IRegistryObject<DataComponentType<Long>> HIGHLIGHT_START_TIME = REGISTRAR.register(
            NbtConstants.HIGHLIGHT_START_TIME,
            () -> DataComponentType.<Long>builder()
                    .persistent(Codec.LONG)
                    .networkSynchronized(ByteBufCodecs.VAR_LONG)
                    .build()
    );

    public static IRegistryObject<DataComponentType<Component>> HIGHLIGHT = REGISTRAR.register(
            NbtConstants.HIGHLIGHT,
            () -> DataComponentType.<Component>builder()
                    .persistent(ComponentSerialization.CODEC)
                    .networkSynchronized(ComponentSerialization.STREAM_CODEC)
                    .build()
    );

    public static IRegistryObject<DataComponentType<Component>> CHISEL_ERROR = REGISTRAR.register(
            NbtConstants.CHISEL_ERROR,
            () -> DataComponentType.<Component>builder()
                    .persistent(ComponentSerialization.CODEC)
                    .networkSynchronized(ComponentSerialization.STREAM_CODEC)
                    .build()
    );

    public static IRegistryObject<DataComponentType<Boolean>> IS_DEFAULT_INSTANCE = REGISTRAR.register(
            NbtConstants.DEFAULT_INSTANCE_INDICATOR,
            () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build()
    );

    public static IRegistryObject<DataComponentType<MeasuringMode>> MEASURING_MODE = REGISTRAR.register(
            NbtConstants.MEASURING_MODE,
            () -> DataComponentType.<MeasuringMode>builder()
                    .persistent(MeasuringMode.CODEC)
                    .networkSynchronized(MeasuringMode.STREAM_CODEC)
                    .build()
    );

    public static IRegistryObject<DataComponentType<Vec3>> START = REGISTRAR.register(
            NbtConstants.START,
            () -> DataComponentType.<Vec3>builder()
                    .persistent(Vec3.CODEC)
                    .networkSynchronized(CBStreamCodecs.VEC_3)
                    .build()
    );

    public static IRegistryObject<DataComponentType<Vec3>> END = REGISTRAR.register(
            NbtConstants.END,
            () -> DataComponentType.<Vec3>builder()
                    .persistent(Vec3.CODEC)
                    .networkSynchronized(CBStreamCodecs.VEC_3)
                    .build()
    );

    public static IRegistryObject<DataComponentType<IPatternPlacementType>> PATTERN_PLACEMENT_TYPE = REGISTRAR.register(
            NbtConstants.PATTERN_PLACEMENT_TYPE,
            () -> DataComponentType.<IPatternPlacementType>builder()
                    .persistent(RegistryManager.getInstance().getPatternPlacementTypeRegistry().byNameCodec())
                    .networkSynchronized(RegistryManager.getInstance().getPatternPlacementTypeRegistry().byNameStreamCodec())
                    .build()
    );

    public static IRegistryObject<DataComponentType<InteractionData>> INTERACTION_TARGET = REGISTRAR.register(
            NbtConstants.INTERACTION_TARGET,
            () -> DataComponentType.<InteractionData>builder()
                    .persistent(InteractionData.CODEC)
                    .networkSynchronized(InteractionData.STREAM_CODEC)
                    .build()
    );

    public static IRegistryObject<DataComponentType<Boolean>> IS_SIMULATING = REGISTRAR.register(
            NbtConstants.IS_SIMULATING,
            () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build()
    );

    public static IRegistryObject<DataComponentType<IModificationOperation>> MODIFICATION_OPERATION = REGISTRAR.register(
            NbtConstants.MODIFICATION_OPERATION,
            () -> DataComponentType.<IModificationOperation>builder()
                    .persistent(RegistryManager.getInstance().getModificationOperationRegistry().byNameCodec())
                    .networkSynchronized(RegistryManager.getInstance().getModificationOperationRegistry().byNameStreamCodec())
                    .build()
    );

    public static IRegistryObject<DataComponentType<Boolean>> BAG_PREFERRED_PICK_UP = REGISTRAR.register(
        NbtConstants.IS_PREFERRED_PICKUP,
        () -> DataComponentType.<Boolean>builder()
            .persistent(Codec.BOOL)
            .networkSynchronized(ByteBufCodecs.BOOL)
            .build()
    );

    public static IRegistryObject<DataComponentType<Boolean>> BAG_FILTERED_PICK_UP = REGISTRAR.register(
        NbtConstants.IS_FILTERED_PICKUP,
        () -> DataComponentType.<Boolean>builder()
            .persistent(Codec.BOOL)
            .networkSynchronized(ByteBufCodecs.BOOL)
            .build()
    );

    public static void onModConstruction() {
        LOGGER.info("Loaded data component types.");
    }
}
