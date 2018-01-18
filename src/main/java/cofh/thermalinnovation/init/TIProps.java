package cofh.thermalinnovation.init;

import cofh.core.gui.CreativeTabCore;
import cofh.thermalinnovation.ThermalInnovation;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TIProps {

	private TIProps() {

	}

	public static void preInit() {

		configCommon();
		configClient();
	}

	/* HELPERS */
	private static void configCommon() {

		String category;
		String comment;
	}

	private static void configClient() {

		/* CREATIVE TABS */
		ThermalInnovation.tabCommon = new CreativeTabCore("thermalinnovation") {

			@Override
			@SideOnly (Side.CLIENT)
			public ItemStack getIconItemStack() {

				ItemStack iconStack = new ItemStack(TIItems.itemMagnet, 1, 1);
				iconStack.setTagCompound(new NBTTagCompound());
				iconStack.getTagCompound().setBoolean("CreativeTab", true);

				return iconStack;
			}

		};
	}

	/* INTERFACE */
	public static boolean showArmorCharge = true;
	public static boolean showToolCharge = true;

}
