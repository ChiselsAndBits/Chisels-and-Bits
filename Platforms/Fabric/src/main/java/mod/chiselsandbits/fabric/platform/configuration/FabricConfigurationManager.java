package mod.chiselsandbits.fabric.platform.configuration;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.*;
import mod.chiselsandbits.platforms.core.config.ConfigurationType;
import mod.chiselsandbits.platforms.core.config.IConfigurationBuilder;
import mod.chiselsandbits.platforms.core.config.IConfigurationManager;
import mod.chiselsandbits.platforms.core.dist.Dist;
import mod.chiselsandbits.platforms.core.dist.DistExecutor;
import mod.chiselsandbits.platforms.core.util.constants.Constants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class FabricConfigurationManager implements IConfigurationManager
{
    private static final Gson GSON = new GsonBuilder()
      .create();

    private static final ResourceLocation CONFIG_SYNC_CHANNEL_ID = new ResourceLocation(Constants.MOD_ID, "config_sync");
    private static final FabricConfigurationManager INSTANCE = new FabricConfigurationManager();

    public static FabricConfigurationManager getInstance()
    {
        return INSTANCE;
    }

    private final Map<String, FabricConfigurationSpec> syncedSources = Maps.newHashMap();
    private final List<FabricConfigurationSpec> noneSyncedSources = Lists.newArrayList();

    private FabricConfigurationManager()
    {
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
            ClientPlayNetworking.registerGlobalReceiver(CONFIG_SYNC_CHANNEL_ID, new ClientPlayNetworking.PlayChannelHandler() {
                @Override
                public void receive(
                  final Minecraft minecraft, final ClientPacketListener clientPacketListener, final FriendlyByteBuf friendlyByteBuf, final PacketSender packetSender)
                {
                    final JsonElement jsonElement = GSON.fromJson(friendlyByteBuf.readUtf(Integer.MAX_VALUE / 4), JsonElement.class);
                    if (!jsonElement.isJsonObject())
                        throw new JsonParseException("The synced configs must be send in an object!");

                    final JsonObject jsonObject = jsonElement.getAsJsonObject();

                    syncedSources.forEach((key, spec) -> {
                        spec.reset();
                        if (jsonObject.has(key)) {
                            final JsonElement specData = jsonObject.get(key);
                            if (!specData.isJsonObject())
                                throw new JsonParseException("A single synced config must be send in an object!");

                            spec.loadFrom(specData.getAsJsonObject());
                        }
                    });
                }
            });
        });

        ServerPlayConnectionEvents.JOIN.register((listener, packetSender, minecraftServer) -> syncTo(listener.getPlayer()));
    }

    public void syncTo(final ServerPlayer serverPlayer) {
        final JsonObject targetObject = new JsonObject();
        syncedSources.forEach((key, spec) -> {
            final JsonObject specObject = spec.getSource().getConfig();
            targetObject.add(key, specObject);
        });

        final String payload = GSON.toJson(targetObject);
        final FriendlyByteBuf buffer = PacketByteBufs.create();

        buffer.writeUtf(payload);

        ServerPlayNetworking.send(serverPlayer, CONFIG_SYNC_CHANNEL_ID, buffer);
    }

    @Override
    public IConfigurationBuilder createBuilder(
      final ConfigurationType type, final String name)
    {

        final JsonObject localConfig = doesLocalConfigExist(name) ? loadLocalConfig(name) : new JsonObject();
        final FabricConfigurationSource source = new FabricConfigurationSource(name, localConfig);

        return new FabricConfigurationBuilder(source, fabricConfigurationSpec -> {
            if (type == ConfigurationType.SYNCED)
            {
                syncedSources.put(name, fabricConfigurationSpec);
            }
            else
            {
                noneSyncedSources.add(fabricConfigurationSpec);
            }

            fabricConfigurationSpec.forceGetAll();
            fabricConfigurationSpec.writeAll();

            saveLocalConfig(name, source.getConfig());
        });
    }

    private JsonObject loadLocalConfig(final String name) {
        try
        {
            final Path configPath = Path.of("./", "configs", name + ".json");

            final FileReader fileReader = new FileReader(configPath.toAbsolutePath().toFile().getAbsolutePath());

            final JsonElement containedElement = GSON.fromJson(fileReader, JsonElement.class);
            if (!containedElement.isJsonObject())
                throw new IllegalStateException("Config file: " + name + " is not a json object!");

            fileReader.close();

            return containedElement.getAsJsonObject();
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Failed to open and read configuration file: " + name, e);
        }
    }

    private boolean doesLocalConfigExist(final String name) {
        final Path configPath = Path.of("./", "configs", name + ".json");
        return Files.exists(configPath);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void saveLocalConfig(final String name, final JsonObject config) {
        try
        {
            final Path configPath = Path.of("./", "configs", name + ".json");
            if (Files.exists(configPath))
                Files.delete(configPath);

            configPath.toFile().getParentFile().mkdirs();
            Files.createFile(configPath);

            final FileWriter fileWriter = new FileWriter(configPath.toAbsolutePath().toFile().getAbsolutePath());
            GSON.toJson(config, fileWriter);

            fileWriter.close();
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Failed to open and read configuration file: " + name, e);
        }
    }
}