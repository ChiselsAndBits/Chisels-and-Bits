package mod.chiselsandbits.client.tool.mode.icon;

import com.google.common.collect.Maps;
import mod.chiselsandbits.api.client.tool.mode.icon.ISelectedToolModeIconRenderer;
import mod.chiselsandbits.api.client.tool.mode.icon.ISelectedToolModeIconRendererRegistry;
import mod.chiselsandbits.api.config.IClientConfiguration;
import net.minecraft.resources.Identifier;

import java.util.Map;

public class SelectedToolModeRendererRegistry implements ISelectedToolModeIconRendererRegistry
{
    private static final SelectedToolModeRendererRegistry INSTANCE = new SelectedToolModeRendererRegistry();

    public static SelectedToolModeRendererRegistry getInstance()
    {
        return INSTANCE;
    }

    private final Map<Identifier, ISelectedToolModeIconRenderer> rendererMap = Maps.newConcurrentMap();

    private SelectedToolModeRendererRegistry()
    {
       this.register(
          new RootGroupTopLeftSelectedToolModeIconRenderer(),
          new NoopSelectedToolModeIconRenderer()
        );
    }

    @Override
    public ISelectedToolModeIconRenderer getCurrent()
    {
        return rendererMap.getOrDefault(Identifier.parse(IClientConfiguration.getInstance().getToolModeRenderer().get()),
          rendererMap.get(RootGroupTopLeftSelectedToolModeIconRenderer.ID));
    }

    @Override
    public ISelectedToolModeIconRendererRegistry register(final ISelectedToolModeIconRenderer... renderers)
    {
        for (final ISelectedToolModeIconRenderer renderer : renderers)
        {
            if (rendererMap.put(renderer.getId(), renderer) != null)
                throw new IllegalArgumentException("The renderer id: " + renderer.getId() + " is already in use!");
        }
        return this;
    }
}
