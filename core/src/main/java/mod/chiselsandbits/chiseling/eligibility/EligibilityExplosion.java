package mod.chiselsandbits.chiseling.eligibility;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class EligibilityExplosion implements Explosion
{

    private static final EligibilityExplosion INSTANCE = new EligibilityExplosion();

    public static EligibilityExplosion getInstance() {
        return INSTANCE;
    }

    @Override
    public ServerLevel level()
    {
        return null;
    }

    @Override
    public BlockInteraction getBlockInteraction()
    {
        return BlockInteraction.KEEP;
    }

    @Override
    public @Nullable LivingEntity getIndirectSourceEntity()
    {
        return null;
    }

    @Override
    public @Nullable Entity getDirectSourceEntity()
    {
        return null;
    }

    @Override
    public float radius()
    {
        return 0;
    }

    @Override
    public Vec3 center()
    {
        return Vec3.ZERO;
    }

    @Override
    public boolean canTriggerBlocks()
    {
        return false;
    }

    @Override
    public boolean shouldAffectBlocklikeEntities()
    {
        return false;
    }
}
