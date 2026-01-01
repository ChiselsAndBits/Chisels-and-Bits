package mod.chiselsandbits.client.tooltip;

import com.communi.suggestu.scena.core.client.event.IClientEvents;
import com.communi.suggestu.scena.core.client.rendering.IRenderingManager;
import com.communi.suggestu.scena.core.client.tooltip.IClientTooltipComponentConverter;
import com.mojang.blaze3d.platform.Lighting;
import mod.chiselsandbits.api.item.multistate.IMultiStateItem;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ChiseledBlockTooltipHandler {

    public static void configure() {
        IRenderingManager.getInstance().registerClientTooltipComponentConverter(
                registrar -> registrar.registerConvert(Payload.class, new Converter())
        );

        IClientEvents.getInstance().getGatherTooltipComponentsEvent().register((itemStack, screenWidth, screenHeight, maxWidth, tooltipElements) -> {
            if (itemStack.getItem() instanceof IMultiStateItem multiStateItem) {
                tooltipElements.add(new Payload(multiStateItem.createItemStack(itemStack).createSnapshot()));
            }

            return true;
        });
    }

    public record Payload(IMultiStateSnapshot snapshot) implements TooltipComponent {}

    public record Client(ItemStack component) implements ClientTooltipComponent {

        @Override
        public int getHeight() {
            return 40;
        }

        @Override
        public int getWidth(@NotNull Font font) {
            return 40;
        }

        @Override
        public void renderImage(@NotNull Font font, int x, int y, @NotNull GuiGraphics graphics) {
            // Render the chiseled block preview here.
            BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(component, Minecraft.getInstance().level, Minecraft.getInstance().player, 0);
            graphics.pose().pushPose();
            graphics.pose().translate((float)(x + 20), (float)(y + 20), (float)(150));

            try {
                graphics.pose().scale(24.0F, -24.0F, 24.0F);
                boolean $$8 = !model.usesBlockLight();
                if ($$8) {
                    Lighting.setupForFlatItems();
                }

                Minecraft.getInstance().getItemRenderer().render(component, ItemDisplayContext.GUI, false, graphics.pose(), graphics.bufferSource(), 15728880, OverlayTexture.NO_OVERLAY, model);
                graphics.flush();
                if ($$8) {
                    Lighting.setupFor3DItems();
                }
            } catch (Throwable $$9) {
                CrashReport $$10 = CrashReport.forThrowable($$9, "Rendering item");
                CrashReportCategory $$11 = $$10.addCategory("Item being rendered");
                $$11.setDetail("Item Type", () -> String.valueOf(component.getItem()));
                $$11.setDetail("Item Components", () -> String.valueOf(component.getComponents()));
                $$11.setDetail("Item Foil", () -> String.valueOf(component.hasFoil()));
                throw new ReportedException($$10);
            }

            graphics.pose().popPose();
        }
    }

    public record Converter() implements IClientTooltipComponentConverter {

        @Override
        public ClientTooltipComponent convert(TooltipComponent component) {
            if (!(component instanceof Payload payload)) {
                return null;
            }

            return new Client(payload.snapshot().toItemStack().toBlockStack());
        }
    }
}
