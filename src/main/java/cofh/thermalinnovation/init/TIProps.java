package cofh.thermalinnovation.init;

import cofh.core.gui.CreativeTabCore;
import cofh.thermalfoundation.ThermalFoundation;
import cofh.thermalfoundation.init.TFProps;
import cofh.thermalinnovation.ThermalInnovation;
import net.minecraft.item.ItemStack;
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
		if (TFProps.useUnifiedTabs) {
			ThermalInnovation.tabCommon = ThermalFoundation.tabCommon;
			ThermalInnovation.tabItems = ThermalFoundation.tabItems;
			ThermalInnovation.tabUtils = ThermalFoundation.tabUtils;

			TFProps.initToolTab();
			ThermalInnovation.tabTools = ThermalFoundation.tabTools;
		} else {
			ThermalInnovation.tabCommon = new CreativeTabCore("thermalinnovation") {

				@Override
				@SideOnly (Side.CLIENT)
				public ItemStack getTabIconItem() {

					return new ItemStack(TIItems.itemDrill, 1, 1);
				}
			};
			ThermalInnovation.tabItems = ThermalInnovation.tabCommon;
			ThermalInnovation.tabUtils = ThermalInnovation.tabCommon;
			ThermalInnovation.tabTools = ThermalInnovation.tabCommon;
		}
	}

}
