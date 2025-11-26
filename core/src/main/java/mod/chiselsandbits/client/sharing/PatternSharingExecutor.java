package mod.chiselsandbits.client.sharing;

import com.communi.suggestu.scena.core.client.models.processing.VertexData;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Either;
import mod.chiselsandbits.api.client.sharing.PatternIOException;
import mod.chiselsandbits.api.config.IClientConfiguration;
import mod.chiselsandbits.api.item.multistate.IMultiStateItemStack;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.client.util.BakedQuadUtils;
import mod.chiselsandbits.utils.CompressionUtils;
import mod.chiselsandbits.utils.FileUtils;
import mod.chiselsandbits.utils.TextureUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.AtlasIds;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.zip.DataFormatException;

public final class PatternSharingExecutor
{

    private PatternSharingExecutor()
    {
        throw new IllegalStateException("Can not instantiate an instance of: PatternSharingExecutor. This is a utility class");
    }

    static void doSavePattern(final IMultiStateItemStack multiStateItemStack, final String patternName, HolderLookup.Provider provider)
    {
        try
        {
            savePattern(multiStateItemStack, patternName, provider);
        }
        catch (PatternIOException e)
        {
            Minecraft.getInstance().getChatListener().handleSystemMessage(e.getErrorMessage(), false);
        }
    }

    private static void savePattern(final IMultiStateItemStack multiStateItemStack, final String patternName, HolderLookup.Provider provider) throws PatternIOException
    {
        final byte[] textureAtlasData = getBlockTextureAtlasData();
        final Model model = getModelQuadData(multiStateItemStack);
        final byte[] chiselData = getChiselData(multiStateItemStack, provider);

        final String dataString = toDataString(textureAtlasData, model, chiselData);
        writePatternDataToDisk(patternName, dataString);
    }

    private static void writePatternDataToDisk(final String patternName, final String dataString) throws PatternIOException
    {
        final String encodedString = Base64.getEncoder().encodeToString(dataString.getBytes());
        final Path targetPath = Paths.get(IClientConfiguration.getInstance().getPatternExportPath().get(), patternName + ".cbsbp");
        try
        {
            final byte[] uncompressedData = encodedString.getBytes();
            final byte[] compressedData = CompressionUtils.compress(uncompressedData);

            Files.write(FileUtils.ensureFileWritable(targetPath), compressedData, StandardOpenOption.WRITE);
        }
        catch (IOException e)
        {
            throw new PatternIOException(
              LocalStrings.PatternExportFailedCouldNotWriteFile.getText(),
              "Could not write pattern data file to disk",
              e
            );
        }
    }

    private static String toDataString(final byte[] textureAtlasData, final Model model, final byte[] chiselData)
    {
        final Gson gson = new GsonBuilder().create();

        final JsonObject returnObject = new JsonObject();

        returnObject.addProperty("version", "1.0");
        returnObject.addProperty("textureAtlas", Base64.getEncoder().encodeToString(textureAtlasData));
        returnObject.add("modelData", gson.toJsonTree(model));
        returnObject.addProperty("chiselData", Base64.getEncoder().encodeToString(chiselData));

        return gson.toJson(returnObject);
    }

    private static byte[] getChiselData(final IMultiStateItemStack multiStateItemStack, HolderLookup.Provider provider) throws PatternIOException
    {
        final RegistryOps<Tag> registryOps = RegistryOps.create(NbtOps.INSTANCE, provider);
        final Tag payload = IMultiStateSnapshot.CODEC.encodeStart(
                registryOps, multiStateItemStack.createSnapshot()
        ).getOrThrow();

        final ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();

        try
        {
            NbtIo.writeAnyTag(payload, dataOutput);
        }
        catch (IOException e)
        {
            throw new PatternIOException(
              LocalStrings.PatternExportFailedCouldNotWriteChiselData.getText(),
              "Failed to write chisel data to memory.",
              e
            );
        }

        return dataOutput.toByteArray();
    }

    private static byte[] getBlockTextureAtlasData() throws PatternIOException
    {
        try(NativeImage currentAtlas = TextureUtils.downloadTexture(
            Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getTexture()
        )) {
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            final WritableByteChannel channel = Channels.newChannel(byteArrayOutputStream);
            try
            {
                if (currentAtlas.writeToChannel(channel)) {
                    return byteArrayOutputStream.toByteArray();
                }

                throw new PatternIOException(
                    LocalStrings.PatternExportFailedGenericAtlasWriteFailure.getText(),
                    "Failed to process the in-memory block atlas texture."
                );
            }
            catch (IOException e)
            {
                throw new PatternIOException(
                    LocalStrings.PatternExportFailedCouldNotWriteAtlas.getText(),
                    "Failed to write the current block texture atlas to disk.",
                    e
                );
            }
        }
    }

    private static Model getModelQuadData(final IMultiStateItemStack multiStateItemStack)
    {
        final ItemStack blockStack = multiStateItemStack.toBlockStack();
        if (!blockStack.has(DataComponents.ITEM_MODEL))
            throw new IllegalStateException("BlockStage does not have model for export!");

        final Identifier itemModelLocation = blockStack.get(DataComponents.ITEM_MODEL);
        if (itemModelLocation == null)
            throw new IllegalStateException("BlockStage has empty model location for export!");

        final ItemModel model = Minecraft.getInstance().getModelManager().getItemModel(itemModelLocation);
        final ItemStackRenderState state = new ItemStackRenderState();
        model.update(
            state,
            blockStack,
            Minecraft.getInstance().getItemModelResolver(),
            ItemDisplayContext.NONE,
            null,
            null,
            0
        );

        final List<QuadData> quads = Arrays.stream(state.layers)
            .map(ItemStackRenderState.LayerRenderState::prepareQuadList)
            .flatMap(List::stream)
            .map(QuadData::new)
            .toList();

        return new Model(quads);
    }

    static Either<IMultiStateItemStack, PatternIOException> doImportPattern(final String patternName, HolderLookup.Provider provider) {
        try
        {
            return Either.left(importPattern(patternName, provider));
        }
        catch (PatternIOException e)
        {
            return Either.right(e);
        }
    }

    private static IMultiStateItemStack importPattern(final String patternName, HolderLookup.Provider provider) throws PatternIOException
    {
        final String patternData = loadPatternDataFromDisk(patternName);
        return getChiselData(patternData, provider);
    }

    private static String loadPatternDataFromDisk(final String patternName) throws PatternIOException {
        final Path targetPath = Paths.get(IClientConfiguration.getInstance().getPatternExportPath().get(), patternName + ".cbsbp");
        if (!Files.exists(targetPath)) {
            throw new PatternIOException(
              LocalStrings.PatternImportFailedFileNotFound.getText(),
              "Failed to find the pattern file at the specified path."
            );
        }

        final byte[] compressedData;
        try
        {
            compressedData = Files.readAllBytes(targetPath);
        }
        catch (IOException e)
        {
            throw new PatternIOException(
              LocalStrings.PatternImportFailedCouldNotReadFile.getText(),
              "Failed to read the pattern file from disk.",
              e
            );
        }

        final byte[] uncompressedData;
        try
        {
            uncompressedData = CompressionUtils.decompress(compressedData);
        }
        catch (IOException e)
        {
            throw new PatternIOException(
              LocalStrings.PatternImportFailedCouldNotDecompressFile.getText(),
              "Could not decompress the pattern file from disk.",
              e
            );
        }
        catch (DataFormatException e)
        {
            throw new PatternIOException(
              LocalStrings.PatternImportFailedCompressedDataInWrongFormat.getText(),
              "Could not decompress the pattern file from disk, the data is stored in an unknown format.",
              e
            );
        }

        final byte[] decodedData = Base64.getDecoder().decode(uncompressedData);
        return new String(decodedData);
    }

    private static IMultiStateItemStack getChiselData(final String dataString, HolderLookup.Provider provider) throws PatternIOException
    {
        final Gson gson = new GsonBuilder().create();

        final JsonObject jsonObject = gson.fromJson(dataString, JsonObject.class);
        final String version = jsonObject.get("version").getAsString();

        if (version.equals("1.0")) {
            return getVersion1ChiselData(jsonObject, provider);
        }

        throw new PatternIOException(
          LocalStrings.PatternImportFailedUnknownVersion.getText(),
          "The pattern file is stored in an unknown version."
        );
    }

    private static IMultiStateItemStack getVersion1ChiselData(final JsonObject dataObject, HolderLookup.Provider provider) throws PatternIOException
    {
        final String encodedChiselString = dataObject.get("chiselData").getAsString();
        final byte[] chiselData = Base64.getDecoder().decode(encodedChiselString);

        final ByteArrayDataInput byteArrayDataInput = ByteStreams.newDataInput(chiselData);
        final Tag tag;
        try
        {
            tag = NbtIo.readAnyTag(byteArrayDataInput, NbtAccounter.unlimitedHeap());
        }
        catch (IOException e)
        {
            throw new PatternIOException(
              LocalStrings.PatternImportFailedInvalidChiselData.getText(),
              "The chisel data is stored in an invalid format."
            );
        }

        RegistryOps<Tag> registryOps = RegistryOps.create(NbtOps.INSTANCE, provider);
        final IMultiStateSnapshot snapshot = IMultiStateSnapshot.CODEC.parse(registryOps, tag).getOrThrow();

        return snapshot.toItemStack();
    }

    private record Model(List<QuadData> quads) {}

    private record QuadData(VertexData[] vertices, int tintIndex, Direction direction, boolean shade) {
        private QuadData(BakedQuad quad)
        {
            this(BakedQuadUtils.getVertexData(quad), quad.tintIndex(), quad.direction(), quad.shade());
        }
    }
}
