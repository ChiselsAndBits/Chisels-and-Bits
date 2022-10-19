package mod.chiselsandbits.keys;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.api.item.withmode.IWithModeItem;
import mod.chiselsandbits.client.reloading.ClientResourceReloadingManager;
import mod.chiselsandbits.client.screens.ToolModeSelectionScreen;
import mod.chiselsandbits.client.time.TickHandler;
import mod.chiselsandbits.keys.contexts.HoldsSpecificItemInHandKeyConflictContext;
import mod.chiselsandbits.keys.contexts.HoldsWithToolItemInHandKeyConflictContext;
import mod.chiselsandbits.keys.contexts.IsPressingDebugKeyConflictContext;
import mod.chiselsandbits.keys.contexts.SpecificScreenOpenKeyConflictContext;
import mod.chiselsandbits.network.packets.HeldToolModeChangedPacket;
import mod.chiselsandbits.network.packets.RequestChangeTrackerOperationPacket;
import mod.chiselsandbits.platforms.core.client.key.IKeyBindingManager;
import mod.chiselsandbits.platforms.core.client.key.KeyModifier;
import mod.chiselsandbits.utils.ItemStackUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class KeyBindingManager
{
    private static final KeyBindingManager INSTANCE                       = new KeyBindingManager();
    private              KeyMapping        openToolMenuKeybinding         = null;
    private              KeyMapping        cycleToolMenuLeftKeybinding    = null;
    private              KeyMapping        cycleToolMenuRightKeybinding   = null;
    private              KeyMapping        resetMeasuringTapeKeyBinding   = null;
    private              KeyMapping        undoOperationKeyBinding        = null;
    private              KeyMapping        redoOperationKeyBinding        = null;
    private              KeyMapping        scopingKeyBinding              = null;
    private              KeyMapping        resetCachesKeyBinding          = null;
    private              boolean           toolMenuKeyWasDown             = false;
    private              int               toolModeSelectionPlusCoolDown  = 15;
    private              int               toolModeSelectionMinusCoolDown = 15;
    private              long              lastChangeTime                 = -10;
    private boolean initialized = false;

    private KeyBindingManager()
    {
    }

    public static KeyBindingManager getInstance()
    {
        return INSTANCE;
    }

    public void onModInitialization()
    {
        IKeyBindingManager.getInstance().register(openToolMenuKeybinding =
                                                    IKeyBindingManager.getInstance().createNew("mod.chiselsandbits.keys.key.modded-tool.open",
                                                      HoldsWithToolItemInHandKeyConflictContext.getInstance(),
                                                      InputConstants.Type.KEYSYM,
                                                      GLFW.GLFW_KEY_R,
                                                      "mod.chiselsandbits.keys.category"));

        IKeyBindingManager.getInstance().register(cycleToolMenuLeftKeybinding =
                                                    IKeyBindingManager.getInstance().createNew("mod.chiselsandbits.keys.key.modded-tool.cycle.left",
                                                      SpecificScreenOpenKeyConflictContext.RADIAL_TOOL_MENU,
                                                      InputConstants.Type.KEYSYM,
                                                      InputConstants.UNKNOWN.getValue(),
                                                      "mod.chiselsandbits.keys.category"));

        IKeyBindingManager.getInstance().register(cycleToolMenuRightKeybinding =
                                                    IKeyBindingManager.getInstance().createNew("mod.chiselsandbits.keys.key.modded-tool.cycle.right",
                                                      SpecificScreenOpenKeyConflictContext.RADIAL_TOOL_MENU,
                                                      InputConstants.Type.KEYSYM,
                                                      InputConstants.UNKNOWN.getValue(),
                                                      "mod.chiselsandbits.keys.category"));

        IKeyBindingManager.getInstance().register(resetMeasuringTapeKeyBinding =
                                                    IKeyBindingManager.getInstance().createNew("mod.chiselsandbits.keys.key.measuring-tape.reset",
                                                      HoldsSpecificItemInHandKeyConflictContext.MEASURING_TAPE,
                                                      InputConstants.Type.KEYSYM,
                                                      1,
                                                      "mod.chiselsandbits.keys.category"));

        IKeyBindingManager.getInstance().register(undoOperationKeyBinding =
                                                    IKeyBindingManager.getInstance().createNew("mod.chiselsandbits.keys.key.undo",
                                                      HoldsSpecificItemInHandKeyConflictContext.CHANGE_TRACKING_ITEM,
                                                      KeyModifier.CONTROL,
                                                      InputConstants.Type.KEYSYM,
                                                      InputConstants.KEY_Z,
                                                      "mod.chiselsandbits.keys.category"));

        IKeyBindingManager.getInstance().register(redoOperationKeyBinding =
                                                    IKeyBindingManager.getInstance().createNew("mod.chiselsandbits.keys.key.redo",
                                                      HoldsSpecificItemInHandKeyConflictContext.CHANGE_TRACKING_ITEM,
                                                      KeyModifier.CONTROL,
                                                      InputConstants.Type.KEYSYM,
                                                      InputConstants.KEY_Y,
                                                      "mod.chiselsandbits.keys.category"));

        IKeyBindingManager.getInstance().register(scopingKeyBinding =
                                                    IKeyBindingManager.getInstance().createNew("mod.chiselsandbits.keys.key.zoom",
                                                      HoldsSpecificItemInHandKeyConflictContext.CHANGE_TRACKING_ITEM,
                                                      InputConstants.Type.KEYSYM,
                                                      InputConstants.KEY_Z,
                                                      "mod.chiselsandbits.keys.category"));

        IKeyBindingManager.getInstance().register(resetCachesKeyBinding =
                IKeyBindingManager.getInstance().createNew("mod.chiselsandbits.keys.reset-caches",
                        IsPressingDebugKeyConflictContext.F3_DEBUG_KEY,
                        InputConstants.Type.KEYSYM,
                        InputConstants.KEY_C,
                        "mod.chiselsandbits.keys.category"));

        initialized = true;
    }

    public void handleKeyPresses()
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.screen == null)
        {
            boolean toolMenuKeyIsDown = isOpenToolMenuKeyPressed();
            if (toolMenuKeyIsDown && !toolMenuKeyWasDown)
            {
                if (isOpenToolMenuKeyPressed())
                {
                    if (mc.screen == null)
                    {
                        ItemStack inHand = ItemStackUtils.getModeItemStackFromPlayer(mc.player);
                        if (!inHand.isEmpty() && inHand.getItem() instanceof IWithModeItem)
                        {
                            try
                            {
                                mc.setScreen(ToolModeSelectionScreen.create(inHand));
                            }
                            catch (ClassCastException ignored)
                            {
                            }
                        }
                    }
                }
            }
            toolMenuKeyWasDown = toolMenuKeyIsDown;
        }
        else
        {
            toolMenuKeyWasDown = true;
        }

        if (ItemStackUtils.getModeItemStackFromPlayer(Minecraft.getInstance().player).getItem() instanceof IWithModeItem)
        {
            final ItemStack stack = ItemStackUtils.getModeItemStackFromPlayer(Minecraft.getInstance().player);
            final IWithModeItem<?> withModeItem = (IWithModeItem<?>) stack.getItem();

            final List<Object> candidates = Lists.newArrayList(withModeItem.getPossibleModes());
            final Object cur = withModeItem.getMode(stack);
            int workingIndex = candidates.indexOf(cur);

            if (toolModeSelectionPlusCoolDown == 15 && isCycleToolMenuRightKeyPressed())
            {
                workingIndex++;
                if (workingIndex >= candidates.size())
                {
                    workingIndex = 0;
                }

                ChiselsAndBits.getInstance().getNetworkChannel().sendToServer(new HeldToolModeChangedPacket(workingIndex));
                toolModeSelectionPlusCoolDown = 0;
            }
            else if (isCycleToolMenuRightKeyPressed() && toolModeSelectionPlusCoolDown <= 14)
            {
                toolModeSelectionPlusCoolDown++;
            }
            else if (toolModeSelectionMinusCoolDown == 15 && isCycleToolMenuLeftKeyPressed())
            {
                workingIndex--;
                if (workingIndex < 0)
                {
                    workingIndex = candidates.size() - 1;
                }

                ChiselsAndBits.getInstance().getNetworkChannel().sendToServer(new HeldToolModeChangedPacket(workingIndex));
                toolModeSelectionMinusCoolDown = 0;
            }
            else if (isCycleToolMenuLeftKeyPressed() && toolModeSelectionMinusCoolDown <= 14)
            {
                toolModeSelectionMinusCoolDown++;
            }
        }

        if (!isCycleToolMenuRightKeyPressed())
        {
            toolModeSelectionPlusCoolDown = 15;
        }

        if (!isCycleToolMenuLeftKeyPressed())
        {
            toolModeSelectionMinusCoolDown = 15;
        }

        if (isUndoOperationKeyPressed() && (TickHandler.getClientTicks() - lastChangeTime) > 10)
        {
            ChiselsAndBits.getInstance().getNetworkChannel().sendToServer(new RequestChangeTrackerOperationPacket(false));
            lastChangeTime = TickHandler.getClientTicks();
        }

        if (isRedoOperationKeyPressed() && (TickHandler.getClientTicks() - lastChangeTime) > 10)
        {
            ChiselsAndBits.getInstance().getNetworkChannel().sendToServer(new RequestChangeTrackerOperationPacket(true));
            lastChangeTime = TickHandler.getClientTicks();
        }

        if (isResetCachesPressed()) {
            ClientResourceReloadingManager.getInstance().clearCaches();
        }
    }

    public boolean hasBeenInitialized() {
        return initialized;
    }

    public boolean isOpenToolMenuKeyPressed()
    {
        return isKeyDown(getOpenToolMenuKeybinding());
    }

    public boolean isCycleToolMenuRightKeyPressed()
    {
        return isKeyDown(getCycleToolMenuRightKeybinding());
    }

    public boolean isCycleToolMenuLeftKeyPressed()
    {
        return isKeyDown(getCycleToolMenuLeftKeybinding());
    }

    public boolean isUndoOperationKeyPressed()
    {
        return isKeyDown(getUndoOperationKeyBinding());
    }

    public boolean isRedoOperationKeyPressed()
    {
        return isKeyDown(getRedoOperationKeyBinding());
    }

    public boolean isKeyDown(KeyMapping keybinding)
    {
        if (keybinding.isUnbound())
        {
            return false;
        }

        boolean isDown = switch (keybinding.key.getType())
                           {
                               case KEYSYM -> InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), keybinding.key.getValue());
                               case MOUSE -> GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), keybinding.key.getValue()) == GLFW.GLFW_PRESS;
                               default -> false;
                           };
        return (isDown || keybinding.isDown() ) && IKeyBindingManager.getInstance().isKeyConflictOfActive(keybinding) &&
                 IKeyBindingManager.getInstance().isKeyModifierActive(keybinding);
    }

    public KeyMapping getOpenToolMenuKeybinding()
    {
        if (openToolMenuKeybinding == null)
        {
            throw new IllegalStateException("Keybindings have not been initialized.");
        }

        return openToolMenuKeybinding;
    }

    public KeyMapping getCycleToolMenuRightKeybinding()
    {
        if (cycleToolMenuRightKeybinding == null)
        {
            throw new IllegalStateException("Keybindings have not been initialized.");
        }

        return cycleToolMenuRightKeybinding;
    }

    public KeyMapping getCycleToolMenuLeftKeybinding()
    {
        if (cycleToolMenuLeftKeybinding == null)
        {
            throw new IllegalStateException("Keybindings have not been initialized.");
        }

        return cycleToolMenuLeftKeybinding;
    }

    public KeyMapping getUndoOperationKeyBinding()
    {
        if (undoOperationKeyBinding == null)
        {
            throw new IllegalStateException("Keybindings have not been initialized.");
        }

        return undoOperationKeyBinding;
    }

    public KeyMapping getRedoOperationKeyBinding()
    {
        if (redoOperationKeyBinding == null)
        {
            throw new IllegalStateException("Keybindings have not been initialized.");
        }

        return redoOperationKeyBinding;
    }

    public boolean isResetMeasuringTapeKeyPressed() {return isKeyDown(getResetMeasuringTapeKeyBinding());}

    public KeyMapping getResetMeasuringTapeKeyBinding()
    {
        if (resetMeasuringTapeKeyBinding == null)
        {
            throw new IllegalStateException("Keybindings have not been initialized.");
        }

        return resetMeasuringTapeKeyBinding;
    }

    public KeyMapping getScopingKeyBinding()
    {
        if (scopingKeyBinding == null)
        {
            throw new IllegalStateException("Keybindings have not been initialized.");
        }

        return scopingKeyBinding;
    }

    public boolean isScopingKeyPressed() {
        return isKeyDown(getScopingKeyBinding());
    }

    public KeyMapping getResetCachesKeyBinding()
    {
        if (resetCachesKeyBinding == null)
        {
            throw new IllegalStateException("Keybindings have not been initialized.");
        }

        return resetCachesKeyBinding;
    }

    public boolean isResetCachesPressed() {
        return isKeyDown(getResetCachesKeyBinding());
    }
}
