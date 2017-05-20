package mod.chiselsandbits.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.lwjgl.opengl.GL11;

import com.google.common.base.Stopwatch;

import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.core.ClientSide;
import mod.chiselsandbits.helpers.ChiselToolType;
import mod.chiselsandbits.helpers.DeprecationHelper;
import mod.chiselsandbits.helpers.LocalStrings;
import mod.chiselsandbits.modes.IToolMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraftforge.client.model.ModelLoader;

public class ChiselsAndBitsMenu extends GuiScreen
{

	private final float TIME_SCALE = 0.01f;
	public static final ChiselsAndBitsMenu instance = new ChiselsAndBitsMenu();

	private float visibility = 0.0f;
	private Stopwatch lastChange = Stopwatch.createStarted();
	public IToolMode switchTo = null;
	public ButtonAction doAction = null;
	public boolean actionUsed = false;

	private float clampVis(
			final float f )
	{
		return Math.max( 0.0f, Math.min( 1.0f, f ) );
	}

	public void raiseVisibility()
	{
		visibility = clampVis( visibility + lastChange.elapsed( TimeUnit.MILLISECONDS ) * TIME_SCALE );
		lastChange = Stopwatch.createStarted();
	}

	public void decreaseVisibility()
	{
		visibility = clampVis( visibility - lastChange.elapsed( TimeUnit.MILLISECONDS ) * TIME_SCALE );
		lastChange = Stopwatch.createStarted();
	}

	public boolean isVisible()
	{
		return visibility > 0.001;
	}

	public void configure(
			final int scaledWidth,
			final int scaledHeight )
	{
		mc = Minecraft.getMinecraft();
		fontRendererObj = mc.fontRendererObj;
		width = scaledWidth;
		height = scaledHeight;
	}

	private static class MenuButton
	{

		public double x1, x2;
		public double y1, y2;
		public boolean highlighted;

		public final ButtonAction action;
		public TextureAtlasSprite icon;
		public int color;
		public String name;
		public EnumFacing textSide;

		public MenuButton(
				final String name,
				final ButtonAction action,
				final double x,
				final double y,
				final TextureAtlasSprite ico,
				final EnumFacing textSide )
		{
			this.name = name;
			this.action = action;
			x1 = x;
			x2 = x + 18;
			y1 = y;
			y2 = y + 18;
			icon = ico;
			color = 0xffffff;
			this.textSide = textSide;
		}

		public MenuButton(
				final String name,
				final ButtonAction action,
				final double x,
				final double y,
				final int col,
				final EnumFacing textSide )
		{
			this.name = name;
			this.action = action;
			x1 = x;
			x2 = x + 18;
			y1 = y;
			y2 = y + 18;
			color = col;
			this.textSide = textSide;
		}

	};

	static class MenuRegion
	{

		public final IToolMode mode;
		public double x1, x2;
		public double y1, y2;
		public boolean highlighted;

		public MenuRegion(
				final IToolMode mode )
		{
			this.mode = mode;
		}

	};

	@Override
	public void drawScreen(
			final int mouseX,
			final int mouseY,
			final float partialTicks )
	{
		final ChiselToolType tool = ClientSide.instance.getHeldToolType( EnumHand.MAIN_HAND );

		if ( tool == null )
		{
			return;
		}

		GlStateManager.pushMatrix();
		GlStateManager.translate( 0.0F, 0.0F, 200.0F );

		final int start = (int) ( visibility * 98 ) << 24;
		final int end = (int) ( visibility * 128 ) << 24;

		drawGradientRect( 0, 0, width, height, start, end );

		GlStateManager.disableTexture2D();
		GlStateManager.enableBlend();
		GlStateManager.disableAlpha();
		GlStateManager.tryBlendFuncSeparate( GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0 );
		GlStateManager.shadeModel( GL11.GL_SMOOTH );
		final Tessellator tessellator = Tessellator.getInstance();
		final VertexBuffer renderBuffer = tessellator.getBuffer();

		renderBuffer.begin( GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR );

		final double vecX = mouseX - width / 2;
		final double vecY = mouseY - height / 2;
		double radians = Math.atan2( vecY, vecX );

		final double ring_inner_edge = 20;
		final double ring_outer_edge = 50;
		final double text_distnace = 65;
		final double quarterCircle = Math.PI / 2.0;

		if ( radians < -quarterCircle )
		{
			radians = radians + Math.PI * 2;
		}

		final double middle_x = width / 2;
		final double middle_y = height / 2;

		final ArrayList<MenuRegion> modes = new ArrayList<MenuRegion>();
		final ArrayList<MenuButton> btns = new ArrayList<MenuButton>();

		if ( tool == ChiselToolType.BIT )
		{
			if ( ChiselsAndBits.getConfig().replaceingBits )
			{
				btns.add( new MenuButton( LocalStrings.BitOptionReplace.toString(), ButtonAction.REPLACE_TOGGLE, text_distnace, -44, ClientSide.swapIcon, EnumFacing.EAST ) );
			}
			else
			{
				btns.add( new MenuButton( LocalStrings.BitOptionPlace.toString(), ButtonAction.REPLACE_TOGGLE, text_distnace, -44, ClientSide.placeIcon, EnumFacing.EAST ) );
			}
		}

		btns.add( new MenuButton( "mod.chiselsandbits.other.undo", ButtonAction.UNDO, text_distnace, -20, ClientSide.undoIcon, EnumFacing.EAST ) );
		btns.add( new MenuButton( "mod.chiselsandbits.other.redo", ButtonAction.REDO, text_distnace, 4, ClientSide.redoIcon, EnumFacing.EAST ) );

		if ( tool == ChiselToolType.TAPEMEASURE )
		{
			final int colorSize = EnumDyeColor.values().length / 4 * 24 - 4;
			double underring = -ring_outer_edge - 34;
			double bntPos = -colorSize;
			final int bntSize = 24;
			EnumFacing textSide = EnumFacing.UP;
			for ( final EnumDyeColor color : EnumDyeColor.values() )
			{
				final ButtonAction action = ButtonAction.valueOf( color.name() );
				if ( bntPos > colorSize )
				{
					underring = ring_outer_edge;
					bntPos = -colorSize;
					textSide = EnumFacing.DOWN;
				}

				btns.add( new MenuButton( "chiselsandbits.color." + color.getUnlocalizedName(), action, bntPos, underring, color.getMapColor().colorValue, textSide ) );
				bntPos += bntSize;
			}
		}

		for ( final IToolMode mode : tool.getAvailableModes() )
		{
			if ( !mode.isDisabled() )
			{
				modes.add( new MenuRegion( mode ) );
			}
		}

		switchTo = null;
		doAction = null;

		if ( !modes.isEmpty() )
		{
			final int totalModes = Math.max( 3, modes.size() );
			int currentMode = 0;
			final double fragment = Math.PI * 0.005;
			final double fragment2 = Math.PI * 0.0025;
			final double perObject = 2.0 * Math.PI / totalModes;

			for ( final MenuRegion mnuRgn : modes )
			{
				final double begin_rad = currentMode * perObject - quarterCircle;
				final double end_rad = ( currentMode + 1 ) * perObject - quarterCircle;

				mnuRgn.x1 = Math.cos( begin_rad );
				mnuRgn.x2 = Math.cos( end_rad );
				mnuRgn.y1 = Math.sin( begin_rad );
				mnuRgn.y2 = Math.sin( end_rad );

				final double x1m1 = Math.cos( begin_rad + fragment ) * ring_inner_edge;
				final double x2m1 = Math.cos( end_rad - fragment ) * ring_inner_edge;
				final double y1m1 = Math.sin( begin_rad + fragment ) * ring_inner_edge;
				final double y2m1 = Math.sin( end_rad - fragment ) * ring_inner_edge;

				final double x1m2 = Math.cos( begin_rad + fragment2 ) * ring_outer_edge;
				final double x2m2 = Math.cos( end_rad - fragment2 ) * ring_outer_edge;
				final double y1m2 = Math.sin( begin_rad + fragment2 ) * ring_outer_edge;
				final double y2m2 = Math.sin( end_rad - fragment2 ) * ring_outer_edge;

				final float a = 0.5f;
				float f = 0f;

				final boolean quad = inTriangle(
						x1m1, y1m1,
						x2m2, y2m2,
						x2m1, y2m1,
						vecX, vecY ) || inTriangle(
								x1m1, y1m1,
								x1m2, y1m2,
								x2m2, y2m2,
								vecX, vecY );

				if ( begin_rad <= radians && radians <= end_rad && quad )
				{
					f = 1;
					mnuRgn.highlighted = true;
					switchTo = mnuRgn.mode;
				}

				renderBuffer.pos( middle_x + x1m1, middle_y + y1m1, zLevel ).color( f, f, f, a ).endVertex();
				renderBuffer.pos( middle_x + x2m1, middle_y + y2m1, zLevel ).color( f, f, f, a ).endVertex();
				renderBuffer.pos( middle_x + x2m2, middle_y + y2m2, zLevel ).color( f, f, f, a ).endVertex();
				renderBuffer.pos( middle_x + x1m2, middle_y + y1m2, zLevel ).color( f, f, f, a ).endVertex();

				currentMode++;
			}
		}

		for ( final MenuButton btn : btns )
		{
			final float a = 0.5f;
			float f = 0f;

			if ( btn.x1 <= vecX && btn.x2 >= vecX && btn.y1 <= vecY && btn.y2 >= vecY )
			{
				f = 1;
				btn.highlighted = true;
				doAction = btn.action;
			}

			renderBuffer.pos( middle_x + btn.x1, middle_y + btn.y1, zLevel ).color( f, f, f, a ).endVertex();
			renderBuffer.pos( middle_x + btn.x1, middle_y + btn.y2, zLevel ).color( f, f, f, a ).endVertex();
			renderBuffer.pos( middle_x + btn.x2, middle_y + btn.y2, zLevel ).color( f, f, f, a ).endVertex();
			renderBuffer.pos( middle_x + btn.x2, middle_y + btn.y1, zLevel ).color( f, f, f, a ).endVertex();
		}

		tessellator.draw();

		GlStateManager.shadeModel( GL11.GL_FLAT );

		GlStateManager.translate( 0.0F, 0.0F, 5.0F );
		GlStateManager.enableTexture2D();
		GlStateManager.color( 1, 1, 1, 1.0f );
		GlStateManager.disableBlend();
		GlStateManager.enableAlpha();
		GlStateManager.bindTexture( Minecraft.getMinecraft().getTextureMapBlocks().getGlTextureId() );

		renderBuffer.begin( GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR );

		for ( final MenuRegion mnuRgn : modes )
		{
			final double x = ( mnuRgn.x1 + mnuRgn.x2 ) * 0.5 * ( ring_outer_edge * 0.6 + 0.4 * ring_inner_edge );
			final double y = ( mnuRgn.y1 + mnuRgn.y2 ) * 0.5 * ( ring_outer_edge * 0.6 + 0.4 * ring_inner_edge );

			final SpriteIconPositioning sip = ClientSide.instance.getIconForMode( mnuRgn.mode );

			final double scalex = 15 * sip.width * 0.5;
			final double scaley = 15 * sip.height * 0.5;
			final double x1 = x - scalex;
			final double x2 = x + scalex;
			final double y1 = y - scaley;
			final double y2 = y + scaley;

			final TextureAtlasSprite sprite = sip.sprite;

			final float f = 1.0f;
			final float a = 1.0f;

			final double u1 = sip.left * 16.0;
			final double u2 = ( sip.left + sip.width ) * 16.0;
			final double v1 = sip.top * 16.0;
			final double v2 = ( sip.top + sip.height ) * 16.0;

			renderBuffer.pos( middle_x + x1, middle_y + y1, zLevel ).tex( sprite.getInterpolatedU( u1 ), sprite.getInterpolatedV( v1 ) ).color( f, f, f, a ).endVertex();
			renderBuffer.pos( middle_x + x1, middle_y + y2, zLevel ).tex( sprite.getInterpolatedU( u1 ), sprite.getInterpolatedV( v2 ) ).color( f, f, f, a ).endVertex();
			renderBuffer.pos( middle_x + x2, middle_y + y2, zLevel ).tex( sprite.getInterpolatedU( u2 ), sprite.getInterpolatedV( v2 ) ).color( f, f, f, a ).endVertex();
			renderBuffer.pos( middle_x + x2, middle_y + y1, zLevel ).tex( sprite.getInterpolatedU( u2 ), sprite.getInterpolatedV( v1 ) ).color( f, f, f, a ).endVertex();
		}

		for ( final MenuButton btn : btns )
		{
			final float f = switchTo == null ? 1.0f : 0.5f;
			final float a = 1.0f;

			final double u1 = 0;
			final double u2 = 16;
			final double v1 = 0;
			final double v2 = 16;

			final TextureAtlasSprite sprite = btn.icon == null ? Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite( ModelLoader.White.LOCATION.toString() ) : btn.icon;

			final double btnx1 = btn.x1 + 1;
			final double btnx2 = btn.x2 - 1;
			final double btny1 = btn.y1 + 1;
			final double btny2 = btn.y2 - 1;

			final float red = f * ( ( btn.color >> 16 & 0xff ) / 255.0f );
			final float green = f * ( ( btn.color >> 8 & 0xff ) / 255.0f );
			final float blue = f * ( ( btn.color & 0xff ) / 255.0f );

			renderBuffer.pos( middle_x + btnx1, middle_y + btny1, zLevel ).tex( sprite.getInterpolatedU( u1 ), sprite.getInterpolatedV( v1 ) ).color( red, green, blue, a ).endVertex();
			renderBuffer.pos( middle_x + btnx1, middle_y + btny2, zLevel ).tex( sprite.getInterpolatedU( u1 ), sprite.getInterpolatedV( v2 ) ).color( red, green, blue, a ).endVertex();
			renderBuffer.pos( middle_x + btnx2, middle_y + btny2, zLevel ).tex( sprite.getInterpolatedU( u2 ), sprite.getInterpolatedV( v2 ) ).color( red, green, blue, a ).endVertex();
			renderBuffer.pos( middle_x + btnx2, middle_y + btny1, zLevel ).tex( sprite.getInterpolatedU( u2 ), sprite.getInterpolatedV( v1 ) ).color( red, green, blue, a ).endVertex();
		}

		tessellator.draw();

		for ( final MenuRegion mnuRgn : modes )
		{
			if ( mnuRgn.highlighted )
			{
				final double x = ( mnuRgn.x1 + mnuRgn.x2 ) * 0.5;
				final double y = ( mnuRgn.y1 + mnuRgn.y2 ) * 0.5;

				int fixed_x = (int) ( x * text_distnace );
				final int fixed_y = (int) ( y * text_distnace );
				final String text = mnuRgn.mode.getName().getLocal();

				if ( x <= -0.2 )
				{
					fixed_x -= fontRendererObj.getStringWidth( text );
				}
				else if ( -0.2 <= x && x <= 0.2 )
				{
					fixed_x -= fontRendererObj.getStringWidth( text ) / 2;
				}

				fontRendererObj.drawStringWithShadow( text, (int) middle_x + fixed_x, (int) middle_y + fixed_y, 0xffffffff );
			}
		}

		for ( final MenuButton btn : btns )
		{
			if ( btn.highlighted )
			{
				final String text = DeprecationHelper.translateToLocal( btn.name );

				if ( btn.textSide == EnumFacing.EAST )
				{
					fontRendererObj.drawStringWithShadow( text, (int) ( middle_x + btn.x2 + 8 ), (int) ( middle_y + btn.y1 + 6 ), 0xffffffff );
				}
				else if ( btn.textSide == EnumFacing.UP )
				{
					fontRendererObj.drawStringWithShadow( text, (int) ( middle_x + ( btn.x1 + btn.x2 ) * 0.5 - fontRendererObj.getStringWidth( text ) * 0.5 ), (int) ( middle_y + btn.y1 - 14 ), 0xffffffff );
				}
				else if ( btn.textSide == EnumFacing.DOWN )
				{
					fontRendererObj.drawStringWithShadow( text, (int) ( middle_x + ( btn.x1 + btn.x2 ) * 0.5 - fontRendererObj.getStringWidth( text ) * 0.5 ), (int) ( middle_y + btn.y1 + 24 ), 0xffffffff );
				}

			}
		}

		GlStateManager.popMatrix();
	}

	private boolean inTriangle(
			final double x1,
			final double y1,
			final double x2,
			final double y2,
			final double x3,
			final double y3,
			final double x,
			final double y )
	{
		final double ab = ( x1 - x ) * ( y2 - y ) - ( x2 - x ) * ( y1 - y );
		final double bc = ( x2 - x ) * ( y3 - y ) - ( x3 - x ) * ( y2 - y );
		final double ca = ( x3 - x ) * ( y1 - y ) - ( x1 - x ) * ( y3 - y );
		return sign( ab ) == sign( bc ) && sign( bc ) == sign( ca );
	}

	private int sign(
			final double n )
	{
		return n > 0 ? 1 : -1;
	}

    /**
     * Called when the mouse is clicked. Args : mouseX, mouseY, clickedButton
     */
    protected void mouseClicked(
    		int mouseX,
    		int mouseY,
    		int mouseButton ) throws IOException
    {
        if ( mouseButton == 0 && ChiselsAndBits.getConfig().enableVivecraftCompatibility )
        {
            this.mc.displayGuiScreen( (GuiScreen) null );

            if ( this.mc.currentScreen == null )
            {
                this.mc.setIngameFocus();
            }
        }
    }
}
