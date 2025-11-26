package mod.chiselsandbits.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.change.IChangeTracker;
import mod.chiselsandbits.api.change.IChangeTrackerManager;
import mod.chiselsandbits.api.change.changes.IllegalChangeAttempt;
import mod.chiselsandbits.api.chiseling.eligibility.IEligibilityManager;
import mod.chiselsandbits.api.inventory.bit.IBitInventory;
import mod.chiselsandbits.api.inventory.management.IBitInventoryManager;
import mod.chiselsandbits.api.multistate.mutator.IMutableStateEntryInfo;
import mod.chiselsandbits.api.multistate.mutator.IMutatorFactory;
import mod.chiselsandbits.api.util.IBatchMutation;
import mod.chiselsandbits.api.multistate.mutator.world.IWorldAreaMutator;
import mod.chiselsandbits.api.profiling.IProfilerResult;
import mod.chiselsandbits.api.profiling.IProfilingManager;
import mod.chiselsandbits.api.util.BlockInformationUtils;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.api.variant.state.IStateVariantManager;
import mod.chiselsandbits.network.packets.ExportPatternCommandMessagePacket;
import mod.chiselsandbits.network.packets.ImportPatternCommandMessagePacket;
import mod.chiselsandbits.profiling.ProfilingManager;
import mod.chiselsandbits.utils.CommandUtils;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.profiling.jfr.Environment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.function.Consumer;

public class CommandManager
{
    private static final CommandManager INSTANCE = new CommandManager();
    private static final Logger         LOGGER   = LogManager.getLogger();

    private CommandManager()
    {
    }

    private static final SimpleCommandExceptionType GIVE_NOT_CHISELABLE_EXCEPTION = new SimpleCommandExceptionType(Component.translatable(LocalStrings.CommandGiveErrorBlockStateNotChiselable
                                                                                                                                     .toString()));

    public static CommandManager getInstance()
    {
        return INSTANCE;
    }

    public void register(final CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext pContext)
    {
        dispatcher.register(
          Commands.literal("candb")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("fill")
                    .then(Commands.argument("start", Vec3Argument.vec3(false))
                            .then(Commands.argument("end", Vec3Argument.vec3(false))
                                    .then(Commands.argument("state", BlockStateArgument.block(pContext))
                                            .executes(this::runFillCommand)
                                    )
                                    .then(Commands.literal("random")
                                            .executes(this::runFillCommand)
                                    )
                            )
                    )
            )
            .then(Commands.literal("clear")
                    .then(Commands.argument("start", Vec3Argument.vec3(false))
                            .then(Commands.argument("end", Vec3Argument.vec3(false))
                                    .executes(this::runClearCommand)
                            )
                    )
            )
            .then(Commands.literal("stats")
                    .then(Commands.argument("start", Vec3Argument.vec3(false))
                            .then(Commands.argument("end", Vec3Argument.vec3(false))
                                    .executes(this::runStatsCommand)
                            )
                    )
            )
            .then(Commands.literal("give")
                    .then(Commands.argument("target", EntityArgument.player())
                            .then(Commands.argument("state", BlockStateArgument.block(pContext))
                                    .then(Commands.argument("count", IntegerArgumentType.integer(0, 64 * 64))
                                            .executes(this::runGiveCommand)
                                    )
                                    .executes(this::runGiveCommand)
                            )
                    )
            )
            .then(Commands.literal("profiling")
                    .then(Commands.literal("start")
                            .executes(this::startProfiling)
                    )
                    .then(Commands.literal("stop")
                            .executes(this::stopProfiling)
                    )
            )
            .then(Commands.literal("undo")
              .then(Commands.argument("target", EntityArgument.player())
                .executes(this::undoFor)
              )
            )
            .then(Commands.literal("redo")
              .then(Commands.argument("target", EntityArgument.player())
                .executes(this::redoFor)
              )
            )
            .then(Commands.literal("save")
              .then(Commands.argument("target", BlockPosArgument.blockPos())
                  .then(Commands.argument("name", StringArgumentType.string())
                      .executes(this::savePatternOf)
                  )
              )
            )
            .then(Commands.literal("load")
              .then(Commands.argument("name", StringArgumentType.string())
                .executes(this::loadPatternOf)
              )
            )
        );
    }

    private int runFillCommand(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        final Vec3 start = Vec3Argument.getVec3(context, "start");
        final Vec3 end = Vec3Argument.getVec3(context, "end");

        if (start.equals(end)) {
            context.getSource().sendFailure(Component.literal("Start and end are the same. This is not supported"));
            return -1;
        }

        final IWorldAreaMutator mutator = IMutatorFactory.getInstance().covering(
          context.getSource().getLevel(),
          start,
          end
        );

        if (CommandUtils.hasArgument(context, "state"))
        {
            final BlockState state = BlockStateArgument.getBlock(context, "state").getState();
            try (final IBatchMutation ignored = context.getSource().getEntity() != null ? mutator.batch(IChangeTrackerManager.getInstance().getChangeTracker(context.getSource().getPlayerOrException())) :
                                                  mutator.batch())
            {
                final long bitCount = mutator.mutableStream().count();
                final BlockInformation information = new BlockInformation(state, IStateVariantManager.getInstance().getStateVariant(state, Optional.empty()));
                final ReportingBlockInformationOverrider reporter = new ReportingBlockInformationOverrider(context.getSource(), bitCount) {
                    @Override
                    protected BlockInformation getNextInformation() {
                        return information;
                    }
                };

                context.getSource().sendSystemMessage(LocalStrings.CommandToFill.getText(bitCount));
                mutator.mutableStream().forEach(reporter);
                context.getSource().sendSystemMessage(LocalStrings.CommandProcessingBlockUpdates.getText());
            }
        }
        else
        {
            try (final IBatchMutation ignored = context.getSource().getEntity() != null ? mutator.batch(IChangeTrackerManager.getInstance().getChangeTracker(context.getSource().getPlayerOrException())) :
                                                                                                                                                                                                            mutator.batch())
            {
                final long bitCount = mutator.mutableStream().count();
                final ReportingBlockInformationOverrider reporter = new ReportingBlockInformationOverrider(context.getSource(), bitCount) {
                    BlockInformation[] lookup = null;
                    long total = bitCount;
                    int index = 0;

                    @Override
                    protected BlockInformation getNextInformation() {
                        if (lookup == null || index == lookup.length) {
                            final int count = (int) Math.min(total, Integer.MAX_VALUE);
                            lookup = BlockInformationUtils.getRandomSupportedInformation(context.getSource().getLevel().getRandom(), count);
                            total -= count;
                            index = 0;
                        }


                        final BlockInformation blockInformation = lookup[index];
                        index++;

                        return blockInformation;
                    }
                };

                context.getSource().sendSystemMessage(LocalStrings.CommandToFill.getText(bitCount));
                mutator.mutableStream().forEach(reporter);
                context.getSource().sendSystemMessage(LocalStrings.CommandProcessingBlockUpdates.getText());
            }
        }

        context.getSource().sendSuccess(() -> Component.translatable(LocalStrings.CommandFillCompleted.toString()), true);
        return 0;
    }

    private int runClearCommand(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        final Vec3 start = Vec3Argument.getVec3(context, "start");
        final Vec3 end = Vec3Argument.getVec3(context, "end");

        final IWorldAreaMutator mutator = IMutatorFactory.getInstance().covering(
          context.getSource().getLevel(),
          start,
          end
        );

        try (final IBatchMutation ignored = context.getSource().getEntity() != null ? mutator.batch(IChangeTrackerManager.getInstance().getChangeTracker(context.getSource().getPlayerOrException())) :
                                                                                                                                                                                                       mutator.batch())
        {
            mutator.mutableStream().forEach(
              IMutableStateEntryInfo::clear
            );
        }

        return 0;
    }

    private int runStatsCommand(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        final Vec3 start = Vec3Argument.getVec3(context, "start");
        final Vec3 end = Vec3Argument.getVec3(context, "end");

        final IWorldAreaMutator mutator = IMutatorFactory.getInstance().covering(
          context.getSource().getLevel(),
          start,
          end
        );

        context.getSource().sendSuccess(() -> Component.literal("Collected the following statistics for the requested area:"), true);
        context.getSource().sendSuccess(() -> Component.literal("----------------------------------------------------------"), true);
        context.getSource().sendSuccess(() -> Component.literal("BlockStates:"), true);
        context.getSource().sendSuccess(() -> Component.literal("############"), true);
        mutator.createSnapshot().getStatics()
          .getStateCounts().forEach((state, count) -> {
            context.getSource().sendSuccess(() -> Component.literal(" > ").append(state.blockState().getBlock().getName()).append(Component.literal(": " + count)), true);
        });

        return 0;
    }

    private int runGiveCommand(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        final Player target = EntityArgument.getPlayer(context, "target");
        final BlockState state = BlockStateArgument.getBlock(context, "state").getState();
        final BlockInformation information = new BlockInformation(state, IStateVariantManager.getInstance().getStateVariant(state, Optional.empty()));

        if (!IEligibilityManager.getInstance().canBeChiseled(information))
            throw GIVE_NOT_CHISELABLE_EXCEPTION.create();

        final int count = CommandUtils.hasArgument(context, "count") ? IntegerArgumentType.getInteger(context, "count") : 4096;

        final IBitInventory inventory = IBitInventoryManager.getInstance().create(target);

        final int insertionCount = Math.min(inventory.getMaxInsertAmount(new BlockInformation(state, Optional.empty())), count);

        inventory.insertOrDiscard(new BlockInformation(state, Optional.empty()), insertionCount);

        return 0;
    }

    private int startProfiling(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        if (ProfilingManager.getInstance().hasProfiler()) {
            context.getSource().sendFailure(Component.literal("Already profiling!"));
            return 1;
        }

        ProfilingManager.getInstance().setProfiler(
          IProfilingManager.getInstance().startProfiling(Environment.from(context.getSource().getServer()))
        );

        return 0;
    }

    private int stopProfiling(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        if (!ProfilingManager.getInstance().hasProfiler()) {
            context.getSource().sendFailure(Component.literal("Not yet profiling!"));
            return 1;
        }

        final IProfilerResult result = IProfilingManager.getInstance().stopProfiling(ProfilingManager.getInstance().getProfiler());
        result.writeAsResponse(line -> context.getSource().sendSuccess(() -> Component.literal(line), true));

        return 0;
    }

    private int redoFor(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        final Player target = EntityArgument.getPlayer(context, "target");
        final IChangeTracker tracker = IChangeTrackerManager.getInstance().getChangeTracker(target);
        if (tracker.getChanges().isEmpty())
        {
            context.getSource().sendFailure(Component.literal("No changes available to redo"));
            return 1;
        }

        try
        {
            tracker.redo(target);
            return 0;
        }
        catch (IllegalChangeAttempt e)
        {
            context.getSource().sendFailure(Component.literal("Can not redo"));
            return 2;
        }
    }

    private int undoFor(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        final Player target = EntityArgument.getPlayer(context, "target");
        final IChangeTracker tracker = IChangeTrackerManager.getInstance().getChangeTracker(target);
        if (tracker.getChanges().isEmpty())
        {
            context.getSource().sendFailure(Component.literal("No changes available to undo"));
            return 1;
        }

        try
        {
            tracker.undo(target);
            return 0;
        }
        catch (IllegalChangeAttempt e)
        {
            context.getSource().sendFailure(Component.literal("Can not undo"));
            return 2;
        }
    }

    private int savePatternOf(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        final BlockPos target = BlockPosArgument.getLoadedBlockPos(context, "target");
        final String name = StringArgumentType.getString(context, "name");

        ChiselsAndBits.getInstance().getNetworkChannel().sendToPlayer(
          new ExportPatternCommandMessagePacket(target, name),
          context.getSource().getPlayerOrException()
        );

        return 0;
    }

    private int loadPatternOf(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        final String name = StringArgumentType.getString(context, "name");

        ChiselsAndBits.getInstance().getNetworkChannel().sendToPlayer(
          new ImportPatternCommandMessagePacket(name),
          context.getSource().getPlayerOrException()
        );

        return 0;
    }

    private static abstract class ReportingBlockInformationOverrider implements Consumer<IMutableStateEntryInfo>
    {
        private final CommandSourceStack source;
        private final long count;
        private final long reportEvery;

        protected int index = 0;

        public ReportingBlockInformationOverrider(CommandSourceStack source, long count) {
            this.source = source;
            this.count = count;

            this.reportEvery = Math.max(1, count / 10);
        }

        @Override
        public void accept(final IMutableStateEntryInfo iMutableStateEntryInfo)
        {
            iMutableStateEntryInfo.overrideState(getNextInformation());
            if (shouldReport())
            {
                doReport();
            }
        }

        protected boolean shouldReport()
        {
            return index++ % reportEvery == 0;
        }

        protected void doReport()
        {
            final double procent = (double) index / (double) count * 100;
            final String formatted = String.format("%.0f", procent);

            source.sendSystemMessage(LocalStrings.CommandFillingInProgress.getText(formatted, count));
        }

        protected abstract BlockInformation getNextInformation();
    }
}
