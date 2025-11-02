package mod.chiselsandbits.keys.contexts;

import com.communi.suggestu.scena.core.client.key.IKeyConflictContext;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;

import java.util.function.Supplier;

public enum IsPressingDebugKeyConflictContext implements IKeyConflictContext {
    F3_DEBUG_KEY(() -> InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), 292));

    private final Supplier<Boolean> isKeyPressed;

    IsPressingDebugKeyConflictContext(final Supplier<Boolean> isKeyPressed) {
        this.isKeyPressed = isKeyPressed;
    }

    @Override
    public boolean isActive() {
        return isKeyPressed.get();
    }

    @Override
    public boolean conflicts(final IKeyConflictContext other) {
        return other == this;
    }
}
