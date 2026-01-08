package mod.chiselsandbits.plugins.wiki.exporter.tags;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;
import org.sinytra.wiki.exporter.ExportContext;
import org.sinytra.wiki.exporter.NamespacedModuleConfig;
import org.sinytra.wiki.exporter.platform.services.ExporterModule;
import org.sinytra.wiki.exporter.platform.services.ExporterModuleFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class TagExporterFactory implements ExporterModuleFactory<TagExporterFactory.Config>
{

    public static final String NAME = "tags";

    @Override
    public @Nullable Class<Config> getConfigClass()
    {
        return Config.class;
    }

    @Override
    public String name()
    {
        return NAME;
    }

    @Override
    public ExporterModule create(final ExportContext context, @Nullable TagExporterFactory.Config config)
    {
        if (config == null)
            throw new IllegalStateException();

        return new Exporter(config);
    }

    public record Config(
        Set<String> namespaces,
        Set<String> registries
    ) implements NamespacedModuleConfig {}

    public record Exporter(Config config) implements ExporterModule
    {
        private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

        @Override
        public void run(final Path output) throws Exception
        {
            for (final String registryName : config.registries())
            {
                var registryIdentifier = Identifier.parse(registryName);
                var registryKey = ResourceKey.createRegistryKey(registryIdentifier);
                Registry<?> registry = TagExporterEntrypoint.getServer().registryAccess()
                    .registries()
                    .filter(registryEntry -> registryEntry.key().equals(registryKey))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Could not find registry: " + registryName))
                    .value();

                runFor(output, registry, registryIdentifier);
            }
        }

        private <T> void runFor(final Path output, final Registry<T> registry, final Identifier registryIdentifier) throws IOException
        {
            for (final HolderSet.Named<T> tagHolder : registry.getTags().toList())
            {
                if (!config().namespaces().contains(tagHolder.key().location().getNamespace()))
                    continue;

                write(
                    output.resolve(
                        registryIdentifier.getNamespace()
                    ).resolve(
                        registryIdentifier.getPath()
                    ).resolve(
                        tagHolder.key().location().getNamespace()
                    ).resolve(
                        "%s.json".formatted(tagHolder.key().location().getPath())
                    ),
                    tagHolder.stream()
                        .map(holder -> registry.getKey(holder.value()))
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .toList()
                );
            }
        }

        private static void write(final Path output, final List<String> contents) throws IOException
        {
            if (!Files.exists(output.getParent()))
                Files.createDirectories(output.getParent());
            try(FileOutputStream fileOutputStream = new FileOutputStream(output.toFile());
                OutputStreamWriter writer = new OutputStreamWriter(fileOutputStream);
                JsonWriter jsonWriter = new JsonWriter(writer)) {
                GSON.toJson(contents, List.class, jsonWriter);
            }
        }
    }
}
