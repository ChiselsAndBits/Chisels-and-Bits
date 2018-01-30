package mod.chiselsandbits.config;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.helpers.LocalStrings;
import mod.chiselsandbits.modes.ChiselMode;
import mod.chiselsandbits.modes.PositivePatternMode;
import mod.chiselsandbits.modes.TapeMeasureModes;
import mod.chiselsandbits.registry.ModRegistry;
import mod.chiselsandbits.render.chiseledblock.tesr.GfxRenderState.UseVBO;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ModConfig extends Configuration
{

	// automatic setting...
	public boolean lowMemoryMode = false;

	// file path...
	final private File myPath;

	// mod settings...
	@Configured( category = "Integration Settings" )
	public boolean ShowBitsInJEI;

	@Configured( category = "Integration Settings" )
	public boolean enableVivecraftCompatibility;

	@Configured( category = "Integration Settings" )
	public boolean enableMCMultipart;

	@Configured( category = "Troubleshooting" )
	public boolean enableAPITestingItem;

	@Configured( category = "Troubleshooting" )
	public boolean logTileErrors;

	@Configured( category = "Client Settings" )
	public boolean enableRightClickModeChange;

	@Configured( category = "Client Settings" )
	private boolean showUsage;

	@Configured( category = "Client Settings" )
	public boolean invertBitBagFullness;

	@Configured( category = "Client Settings" )
	public boolean enableChiselMode_Plane;

	@Configured( category = "Client Settings" )
	public boolean enableChiselMode_SameMaterial;

	@Configured( category = "Client Settings" )
	public boolean enableChiselMode_ConnectedPlane;

	@Configured( category = "Client Settings" )
	public boolean enableChiselMode_ConnectedMaterial;

	@Configured( category = "Client Settings" )
	public boolean enableChiselMode_Line;

	@Configured( category = "Client Settings" )
	public boolean enableChiselMode_SmallCube;

	@Configured( category = "Client Settings" )
	public boolean enableChiselMode_MediumCube;

	@Configured( category = "Client Settings" )
	public boolean enableChiselMode_LargeCube;

	@Configured( category = "Client Settings" )
	public boolean enableChiselMode_DrawnRegion;

	@Configured( category = "Client Settings" )
	public boolean enableChiselMode_Snap2;

	@Configured( category = "Client Settings" )
	public boolean enableChiselMode_Snap4;

	@Configured( category = "Client Settings" )
	public boolean enableChiselMode_Snap8;

	@Configured( category = "Client Settings" )
	public boolean enablePositivePatternMode_Additive;

	@Configured( category = "Client Settings" )
	public boolean enablePositivePatternMode_Impose;

	@Configured( category = "Client Settings" )
	public boolean enablePositivePatternMode_Placement;

	@Configured( category = "Client Settings" )
	public boolean enablePositivePatternMode_Replace;

	@Configured( category = "Client Settings" )
	public boolean enableTapeMeasure_Bit;

	@Configured( category = "Client Settings" )
	public boolean enableTapeMeasure_Block;

	@Configured( category = "Client Settings" )
	public boolean enableTapeMeasure_Distance;

	@Configured( category = "Client Settings" )
	public boolean enableToolbarIcons;

	@Configured( category = "Client Settings" )
	public boolean perChiselMode;

	@Configured( category = "Client Settings" )
	public boolean chatModeNotification;

	@Configured( category = "Client Settings" )
	public boolean itemNameModeDisplay;

	@Configured( category = "Client Settings" )
	public int creativeClipboardSize;

	@Configured( category = "Client Settings" )
	public boolean addBrokenBlocksToCreativeClipboard;

	@Configured( category = "Client Settings" )
	public boolean fluidBitsAreClickThrough;

	@Configured( category = "Client Settings" )
	public boolean persistCreativeClipboard;

	@Configured( category = "Client Settings" )
	public int maxUndoLevel;

	@Configured( category = "Client Settings" )
	public int maxTapeMeasures;

	@Configured( category = "Client Settings" )
	public boolean displayMeasuringTapeInChat;

	@Configured( category = "Client Settings" )
	public float radialMenuVolume;

	@Configured( category = "Client Performance Settings" )
	public static UseVBO useVBO;

	@Configured( category = "Troublershooting" )
	public boolean useGetLightValue;

	@Configured( category = "Troublershooting" )
	public boolean enableFaceLightmapExtraction;

	@Configured( category = "Troublershooting" )
	public boolean disableCustomVertexFormats;

	@Configured( category = "Client Performance Settings" )
	public int maxMillisecondsPerBlock = 10;

	@Configured( category = "Client Performance Settings" )
	public int maxMillisecondsUploadingPerFrame = 15;

	@Configured( category = "Client Performance Settings" )
	public int dynamicModelFaceCount;

	@Configured( category = "Client Performance Settings" )
	public int dynamicModelRange;

	@Configured( category = "Client Performance Settings" )
	public boolean dynamicModelMinimizeLatancy;

	@Configured( category = "Client Performance Settings" )
	public long minimizeLatancyMaxTime;

	@Configured( category = "Client Performance Settings" )
	public int dynamicMaxConcurrentTessalators;

	@Configured( category = "Client Performance Settings" )
	public boolean forceDynamicRenderer;

	@Configured( category = "Client Performance Settings" )
	public boolean defaultToDynamicRenderer;

	@Configured( category = "Client Performance Settings" )
	public boolean dynamicRenderFullChunksOnly;

	@Configured( category = "Client Performance Settings" )
	public boolean enableModelCompression;

	@Configured( category = "Balance Settings" )
	public boolean blacklistTickingBlocks;

	@Configured( category = "Balance Settings" )
	public boolean damageTools;

	@Configured( category = "Crafting" )
	public boolean enablePositivePrintCrafting;

	@Configured( category = "Crafting" )
	public boolean enableStackableCrafting;

	@Configured( category = "Crafting" )
	public boolean enableNegativePrintInversionCrafting;

	@Configured( category = "Crafting" )
	public boolean enableChiselCrafting;

	@Configured( category = "Items" )
	public boolean enableBitTank;

	@Configured( category = "Items" )
	public boolean enableBitBag;

	@Configured( category = "Items" )
	public boolean enableNegativePrint;

	@Configured( category = "Items" )
	public boolean enableMirrorPrint;

	@Configured( category = "Items" )
	public boolean enablePositivePrint;

	@Configured( category = "Items" )
	public boolean enableChisledBits;

	@Configured( category = "Items" )
	public boolean enableStoneChisel;

	@Configured( category = "Items" )
	public boolean enableIronChisel;

	@Configured( category = "Items" )
	public boolean enableGoldChisel;

	@Configured( category = "Items" )
	public boolean enableDiamondChisel;

	@Configured( category = "Items" )
	public boolean enableWoodenWrench;

	@Configured( category = "Items" )
	public boolean enableTapeMeasure;

	@Configured( category = "Items" )
	public boolean enableBitSaw;

	@Configured( category = "Balance Settings" )
	public boolean enableChiselToolHarvestCheck;

	@Configured( category = "Balance Settings" )
	public String enableChiselToolHarvestCheckTools;

	@Configured( category = "Balance Settings" )
	public boolean enableToolHarvestLevels;

	@Configured( category = "Balance Settings" )
	public boolean enableBitLightSource;

	@Configured( category = "Balance Settings" )
	public float bitLightPercentage;

	@Configured( category = "Balance Settings" )
	public boolean compatabilityMode;

	@Configured( category = "Balance Settings" )
	public double maxDrawnRegionSize;

	@Configured( category = "Balance Settings" )
	public int bagStackSize;
	
	@Configured( category = "Balance Settings" )
	public int stoneChiselUses;

	@Configured( category = "Balance Settings" )
	public int ironChiselUses;

	@Configured( category = "Balance Settings" )
	public int diamondChiselUses;

	@Configured( category = "Balance Settings" )
	public int goldChiselUses;

	@Configured( category = "Balance Settings" )
	public int wrenchUses;

	@Configured( category = "Balance Settings" )
	public int diamondSawUses;

	@Configured( category = "Balance Settings" )
	public boolean enableSetBitCommand;

	@Configured( category = "Balance Settings" )
	public boolean fullBlockCrafting;
		
	@Configured( category = "Balance Settings" )
	public boolean requireBagSpace;
	
	@Configured( category = "Balance Settings" )
	public boolean voidExcessBits;

	// in game state
	public boolean replaceingBits = false;

	public boolean deobfuscatedEnvironment()
	{
		final Object deObf = Launch.blackboard.get( "fml.deobfuscatedEnvironment" );
		return Boolean.valueOf( String.valueOf( deObf ) );
	}

	public boolean isEnabled(
			final String className )
	{
		final Property p = get( "Enabled Blocks", className, true );
		final boolean out = p.getBoolean( true );

		if ( hasChanged() )
		{
			save();
		}

		return out;
	}

	private void setDefaults()
	{
		final long mem = Runtime.getRuntime().maxMemory() / ( 1024 * 1024 ); // mb

		logTileErrors = false;
		enableAPITestingItem = deobfuscatedEnvironment();
		lowMemoryMode = mem < 1200;
		enableChiselMode_SameMaterial = !ChiselMode.SAME_MATERIAL.isDisabled;
		enableChiselMode_ConnectedMaterial = !ChiselMode.CONNECTED_MATERIAL.isDisabled;
		enableChiselMode_ConnectedPlane = !ChiselMode.CONNECTED_PLANE.isDisabled;
		enableChiselMode_LargeCube = !ChiselMode.CUBE_LARGE.isDisabled;
		enableChiselMode_MediumCube = !ChiselMode.CUBE_MEDIUM.isDisabled;
		enableChiselMode_SmallCube = !ChiselMode.CUBE_SMALL.isDisabled;
		enableChiselMode_Line = !ChiselMode.LINE.isDisabled;
		enableChiselMode_Plane = !ChiselMode.PLANE.isDisabled;
		enableChiselMode_DrawnRegion = !ChiselMode.DRAWN_REGION.isDisabled;
		enableChiselMode_Snap2 = !ChiselMode.SNAP2.isDisabled;
		enableChiselMode_Snap4 = !ChiselMode.SNAP4.isDisabled;
		enableChiselMode_Snap8 = !ChiselMode.SNAP8.isDisabled;
		enablePositivePatternMode_Additive = !PositivePatternMode.ADDITIVE.isDisabled;
		enablePositivePatternMode_Impose = !PositivePatternMode.IMPOSE.isDisabled;
		enablePositivePatternMode_Placement = !PositivePatternMode.PLACEMENT.isDisabled;
		enablePositivePatternMode_Replace = !PositivePatternMode.REPLACE.isDisabled;
		enableTapeMeasure_Bit = !TapeMeasureModes.BIT.isDisabled;
		enableTapeMeasure_Block = !TapeMeasureModes.BLOCK.isDisabled;
		enableTapeMeasure_Distance = !TapeMeasureModes.DISTANCE.isDisabled;
		displayMeasuringTapeInChat = false;
		perChiselMode = true;
		enableChiselCrafting = true;
		fullBlockCrafting = true;
		radialMenuVolume = 0.1f;
		chatModeNotification = false;
		itemNameModeDisplay = true;
		enableToolbarIcons = true;
		compatabilityMode = true;
		maxDrawnRegionSize = 4;
		bagStackSize = 512;
		requireBagSpace = false;
		voidExcessBits = false;
		maxUndoLevel = 32;
		maxTapeMeasures = 5;

		// Dynamic models..
		dynamicModelFaceCount = 40;
		dynamicModelRange = 128;
		dynamicModelMinimizeLatancy = true;
		minimizeLatancyMaxTime = 100;
		dynamicMaxConcurrentTessalators = 32; // in low memory this acts as 2.
		forceDynamicRenderer = false;
		defaultToDynamicRenderer = false;
		dynamicRenderFullChunksOnly = true;
		useVBO = UseVBO.AUTOMATIC;
		disableCustomVertexFormats = false;
		enableModelCompression = true;
		enableFaceLightmapExtraction = true;
		useGetLightValue = true;

		showUsage = true;
		invertBitBagFullness = false;
		enableSetBitCommand = false;

		damageTools = true;
		blacklistTickingBlocks = true;
		stoneChiselUses = 8384;
		ironChiselUses = 293440;
		diamondChiselUses = 796480;
		goldChiselUses = 1024;
		wrenchUses = 1888;
		diamondSawUses = 7980;

		enablePositivePrintCrafting = true;
		enableStackableCrafting = true;
		enableNegativePrintInversionCrafting = true;

		enableChiselToolHarvestCheck = true;
		enableToolHarvestLevels = true;
		enableChiselToolHarvestCheckTools = "pickaxe,axe,shovel";

		creativeClipboardSize = 32;
		addBrokenBlocksToCreativeClipboard = true;
		fluidBitsAreClickThrough = true;
		persistCreativeClipboard = true;
		enableRightClickModeChange = false;

		enableBitTank = true;
		enableBitLightSource = true;
		bitLightPercentage = 6.25f;
		enableBitBag = true;
		enableNegativePrint = true;
		enablePositivePrint = true;
		enableMirrorPrint = true;
		enableChisledBits = true;
		enableStoneChisel = true;
		enableIronChisel = true;
		enableGoldChisel = true;
		enableDiamondChisel = true;
		enableWoodenWrench = true;
		enableTapeMeasure = true;
		enableBitSaw = true;
		ShowBitsInJEI = false;
		enableVivecraftCompatibility = false;
		enableMCMultipart = true;
	}

	public ModConfig(
			final File path )
	{
		super( path );
		myPath = path;
		ChiselsAndBits.registerWithBus( this );
		setDefaults();
		populateSettings();
		save();
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	void populateSettings()
	{
		final Class<ModConfig> me = ModConfig.class;
		final Set<Property> fields = new HashSet<Property>();

		for ( final Field f : me.getDeclaredFields() )
		{
			final Configured c = f.getAnnotation( Configured.class );
			if ( c != null )
			{
				try
				{
					Property p = null;

					if ( f.getType() == long.class || f.getType() == Long.class )
					{
						final long defaultValue = f.getLong( this );
						p = get( c.category(), f.getName(), (int) defaultValue );
						final long value = p.getInt();
						f.set( this, value );
					}
					else if ( f.getType() == String.class )
					{
						final String defaultValue = (String) f.get( this );
						p = get( c.category(), f.getName(), defaultValue );
						final String value = p.getString();
						f.set( this, value );
					}
					else if ( f.getType() == int.class || f.getType() == Integer.class )
					{
						final int defaultValue = f.getInt( this );
						p = get( c.category(), f.getName(), defaultValue );
						final int value = p.getInt();
						f.set( this, value );
					}
					else if ( f.getType() == float.class || f.getType() == Float.class )
					{
						final float defaultValue = f.getFloat( this );
						p = get( c.category(), f.getName(), defaultValue );
						final float value = (float) p.getDouble();
						f.set( this, value );
					}
					else if ( f.getType() == double.class || f.getType() == Double.class )
					{
						final double defaultValue = f.getDouble( this );
						p = get( c.category(), f.getName(), defaultValue );
						final double value = p.getDouble();
						f.set( this, value );
					}
					else if ( f.getType() == boolean.class || f.getType() == Boolean.class )
					{
						final boolean defaultValue = f.getBoolean( this );
						p = get( c.category(), f.getName(), defaultValue );
						final boolean value = p.getBoolean();
						f.set( this, value );
					}
					else if ( Enum.class.isAssignableFrom( f.getType() ) )
					{
						final Class<? extends Enum> eClass = (Class<? extends Enum>) f.getType();
						final Enum<?> defaultValue = (Enum<?>) f.get( this );

						final EnumSet<?> options = EnumSet.allOf( eClass );
						final String[] values = new String[options.size()];
						int off = 0;
						for ( final Object eo : options )
						{
							final Enum<?> exc = (Enum<?>) eo;
							values[off++] = exc.name();
						}

						p = get( c.category(), f.getName(), defaultValue != null ? defaultValue.toString() : "", "", values );
						final String value = p.getString();

						for ( final Object eo : options )
						{
							final Enum<?> exc = (Enum<?>) eo;
							if ( exc.name().equals( value ) )
							{
								f.set( this, eo );
							}
						}
					}

					if ( p != null )
					{
						p.setLanguageKey( ModRegistry.unlocalizedPrefix + "config." + f.getName() );
						fields.add( p );
					}
				}
				catch ( final IllegalArgumentException e )
				{
					// yar!
					e.printStackTrace();
				}
				catch ( final IllegalAccessException e )
				{
					// yar!
					e.printStackTrace();
				}
			}
		}

		// cleanup the config... remove junk...
		for ( final String s : getCategoryNames() )
		{
			if ( s.equals( "enabled blocks" ) )
			{
				continue;
			}

			final ConfigCategory cc = getCategory( s );
			final List<String> removeThis = new ArrayList<String>();

			for ( final Entry<String, Property> e : cc.entrySet() )
			{
				if ( !fields.contains( e.getValue() ) )
				{
					removeThis.add( e.getKey() );
				}
			}

			for ( final String g : removeThis )
			{
				cc.remove( g );
			}
		}

		sync();
	}

	private void sync()
	{
		// sane bag sizes...
		if ( bagStackSize < 64 )
		{
			bagStackSize = 64;
		}
		else if ( bagStackSize > 999999 )
		{
			bagStackSize = 999999;
		}

		// configure mode enums..
		ChiselMode.SAME_MATERIAL.isDisabled = !enableChiselMode_SameMaterial;
		ChiselMode.CONNECTED_MATERIAL.isDisabled = !enableChiselMode_ConnectedMaterial;
		ChiselMode.CONNECTED_PLANE.isDisabled = !enableChiselMode_ConnectedPlane;
		ChiselMode.CUBE_LARGE.isDisabled = !enableChiselMode_LargeCube;
		ChiselMode.CUBE_MEDIUM.isDisabled = !enableChiselMode_MediumCube;
		ChiselMode.CUBE_SMALL.isDisabled = !enableChiselMode_SmallCube;
		ChiselMode.LINE.isDisabled = !enableChiselMode_Line;
		ChiselMode.PLANE.isDisabled = !enableChiselMode_Plane;
		ChiselMode.DRAWN_REGION.isDisabled = !enableChiselMode_DrawnRegion;
		ChiselMode.SNAP2.isDisabled = !enableChiselMode_Snap2;
		ChiselMode.SNAP4.isDisabled = !enableChiselMode_Snap4;
		ChiselMode.SNAP8.isDisabled = !enableChiselMode_Snap8;
		PositivePatternMode.ADDITIVE.isDisabled = !enablePositivePatternMode_Additive;
		PositivePatternMode.IMPOSE.isDisabled = !enablePositivePatternMode_Impose;
		PositivePatternMode.PLACEMENT.isDisabled = !enablePositivePatternMode_Placement;
		PositivePatternMode.REPLACE.isDisabled = !enablePositivePatternMode_Replace;
		TapeMeasureModes.BIT.isDisabled = !enableTapeMeasure_Bit;
		TapeMeasureModes.BLOCK.isDisabled = !enableTapeMeasure_Block;
		TapeMeasureModes.DISTANCE.isDisabled = !enableTapeMeasure_Distance;
	}

	@SubscribeEvent
	public void onConfigChanged(
			final ConfigChangedEvent.OnConfigChangedEvent eventArgs )
	{
		if ( eventArgs.getModID().equals( ChiselsAndBits.MODID ) )
		{
			populateSettings();
			save();
		}
	}

	@Override
	public void save()
	{
		if ( hasChanged() )
		{
			super.save();
		}
	}

	@Override
	public Property get(
			final String category,
			final String key,
			final String defaultValue,
			final String comment,
			final Property.Type type )
	{
		final Property prop = super.get( category, key, defaultValue, comment, type );

		if ( prop != null && !category.equals( "Client Settings" ) && category.equals( "Client Performance" ) )
		{
			prop.setRequiresMcRestart( true );
		}

		return prop;
	}

	public void helpText(
			final LocalStrings string,
			final List<String> tooltip,
			final String... variables )
	{
		if ( showUsage )
		{
			int varOffset = 0;

			final String[] lines = string.getLocal().split( ";" );
			for ( String a : lines )
			{
				while ( a.contains( "{}" ) && variables.length > varOffset )
				{
					final int offset = a.indexOf( "{}" );
					if ( offset >= 0 )
					{
						final String pre = a.substring( 0, offset );
						final String post = a.substring( offset + 2 );
						a = new StringBuilder( pre ).append( variables[varOffset++] ).append( post ).toString();
					}
				}

				tooltip.add( a );
			}
		}
	}

	public String getFilePath()
	{
		return myPath.getAbsolutePath();
	}

}