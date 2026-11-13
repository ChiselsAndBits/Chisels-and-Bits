package mod.chiselsandbits.registrars;

import com.communi.suggestu.scena.core.client.models.data.IModelDataKey;
import com.communi.suggestu.scena.core.client.rendering.terrain.TerrainGeometry;
import mod.chiselsandbits.client.model.block.ChiseledTerrainRenderData;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelInformation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ModModelProperties {
	private static final Logger LOGGER = LogManager.getLogger();
	public static IModelDataKey<ChiseledBlockModelInformation> MODEL = IModelDataKey.create();
	public static IModelDataKey<ChiseledBlockModelInformation> RECTANGLE_MODEL = IModelDataKey.create();
	public static IModelDataKey<TerrainGeometry> TERRAIN_GEOMETRY = IModelDataKey.create();
	public static IModelDataKey<ChiseledTerrainRenderData> TERRAIN_RENDER_DATA = IModelDataKey.create();

	private ModModelProperties() {
		throw new IllegalStateException("Can not instantiate an instance of: ModModelProperties.This is a utility class");
	}

	public static void onModConstruction() {
		LOGGER.info("Loaded model property configuration.");
	}
}
