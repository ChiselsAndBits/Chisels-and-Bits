package mod.chiselsandbits.api;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly( Side.CLIENT )
public enum ModKeyBinding
{
	//Misc
	ROTATE_CCW,
	ROTATE_CW,
	UNDO,
	REDO,
	MODE_MENU,
	ADD_TO_CLIPBOARD,
	PICK_BIT,

	//Chisel Modes
	SINGLE,
	SNAP2,
	SNAP4,
	SNAP8,
	LINE,
	PLANE,
	CONNECTED_PLANE,
	CUBE_SMALL,
	CUBE_MEDIUM,
	CUBE_LARGE,
	SAME_MATERIAL,
	DRAWN_REGION,
	CONNECTED_MATERIAL,

	//Positive Pattern Modes
	REPLACE,
	ADDITIVE,
	PLACEMENT,
	IMPOSE,

	//Tape Measure Modes
	BIT,
	BLOCK,
	DISTANCE;
}
