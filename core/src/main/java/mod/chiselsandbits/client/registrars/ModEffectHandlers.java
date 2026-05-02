package mod.chiselsandbits.client.registrars;

import com.communi.suggestu.scena.core.client.effect.IEffectManager;
import mod.chiselsandbits.api.block.entity.IMultiStateBlockEntity;
import mod.chiselsandbits.registrars.ModBlocks;
import mod.chiselsandbits.utils.EffectUtils;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ModEffectHandlers
{
    private ModEffectHandlers()
    {
        throw new IllegalStateException("Can not instantiate an instance of: BlockColors. This is a utility class");
    }

    public static void onClientConstruction()
    {
        IEffectManager.getInstance()
            .setupHitEffects(registrar -> {
                registrar.register(new IEffectManager.IHitEffectHandler()
                                   {
                                       @Nullable
                                       private BlockState getPrimaryState(Level level, BlockPos pos)
                                       {
                                           final BlockEntity blockEntity = level.getBlockEntity(pos);
                                           if (!(blockEntity instanceof IMultiStateBlockEntity multiStateBlockEntity))
                                           {
                                               return null;
                                           }

                                           return multiStateBlockEntity.getStatistics().getPrimaryState().blockState();
                                       }

                                       @Override
                                       public boolean addHitEffects(
                                           @NotNull final BlockState blockState,
                                           @NotNull final Level level,
                                           @NotNull final BlockPos blockPos,
                                           @NotNull final Direction direction,
                                           @NotNull final ParticleEngine particleEngine)
                                       {
                                           final BlockState primaryState = getPrimaryState(level, blockPos);
                                           if (primaryState == null)
                                           {
                                               return false;
                                           }

                                           return EffectUtils.addHitEffects(level, blockPos, direction, primaryState, particleEngine);
                                       }
                                   },
                    ModBlocks.CHISELED_BLOCK.get());
            });

        IEffectManager.getInstance()
            .setupDestroyEffects(registrar -> {
                registrar.register(new IEffectManager.IDestroyEffectHandler()
                                   {
                                       @Nullable
                                       private BlockState getPrimaryState(Level level, BlockPos pos)
                                       {
                                           final BlockEntity blockEntity = level.getBlockEntity(pos);
                                           if (!(blockEntity instanceof IMultiStateBlockEntity multiStateBlockEntity))
                                           {
                                               return null;
                                           }

                                           return multiStateBlockEntity.getStatistics().getPrimaryState().blockState();
                                       }

                                       @Override
                                       public boolean addDestroyEffects(final BlockState blockState, final Level level, final BlockPos blockPos, final ParticleEngine particleEngine)
                                       {
                                           final BlockState primaryState = getPrimaryState(level, blockPos);
                                           if (primaryState == null)
                                           {
                                               return false;
                                           }

                                           EffectUtils.addBlockDestroyEffects(level, blockPos, primaryState, particleEngine, level);
                                           return true;
                                       }
                                   },
                    ModBlocks.CHISELED_BLOCK.get());
            });
    }
}
