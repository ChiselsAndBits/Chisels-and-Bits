package mod.chiselsandbits.api;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly( Side.CLIENT )
public enum ModKeyBinding
{
	//Misc
	ROTATE_CCW( Type.MISC ),
	ROTATE_CW( Type.MISC ),
	UNDO( Type.MISC ),
	REDO( Type.MISC ),
	MODE_MENU( Type.MISC ),
	ADD_TO_CLIPBOARD( Type.MISC ),
	PICK_BIT( Type.MISC ),

	//Chisel Modes
	SINGLE( Type.CHISEL_MODE ),
	SNAP2( Type.CHISEL_MODE ),
	SNAP4( Type.CHISEL_MODE ),
	SNAP8( Type.CHISEL_MODE ),
	LINE( Type.CHISEL_MODE ),
	PLANE( Type.CHISEL_MODE ),
	CONNECTED_PLANE( Type.CHISEL_MODE ),
	CUBE_SMALL( Type.CHISEL_MODE ),
	CUBE_MEDIUM( Type.CHISEL_MODE ),
	CUBE_LARGE( Type.CHISEL_MODE ),
	SAME_MATERIAL( Type.CHISEL_MODE ),
	DRAWN_REGION( Type.CHISEL_MODE ),
	CONNECTED_MATERIAL( Type.CHISEL_MODE ),

	//Positive Pattern Modes
	REPLACE( Type.POSITIVE_PATTERN_MODE ),
	ADDITIVE( Type.POSITIVE_PATTERN_MODE ),
	PLACEMENT( Type.POSITIVE_PATTERN_MODE ),
	IMPOSE( Type.POSITIVE_PATTERN_MODE ),

	//Tape Measure Modes
	BIT( Type.TAPE_MEASURES_MODE ),
	BLOCK( Type.TAPE_MEASURES_MODE ),
	DISTANCE( Type.TAPE_MEASURES_MODE );

	public static enum Type
	{
		MISC,
		CHISEL_MODE,
		POSITIVE_PATTERN_MODE,
		TAPE_MEASURES_MODE;
	}

	public final Type type;

	private ModKeyBinding(
			Type type)
	{
		this.type = type;
	}

	public int getOffsetOrdinal()
	{
		int offset;

		switch ( type )
		{
			case CHISEL_MODE:
				offset = 7;
				break;
			case POSITIVE_PATTERN_MODE:
				offset = 20;
				break;
			case TAPE_MEASURES_MODE:
				offset = 24;
				break;
			default:
				offset = 0;
		}
		return ordinal() + offset;
	}

}
