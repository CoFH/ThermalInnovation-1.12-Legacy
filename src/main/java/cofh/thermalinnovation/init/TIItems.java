package cofh.thermalinnovation.init;

import cofh.core.util.core.IInitializer;
import cofh.thermalinnovation.item.ItemDrill;
import cofh.thermalinnovation.item.ItemMagnet;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;

public class TIItems {

	public static final TIItems INSTANCE = new TIItems();

	private TIItems() {

	}

	public static void preInit() {

		itemDrill = new ItemDrill();
		itemMagnet = new ItemMagnet();

		initList.add(itemDrill);
		initList.add(itemMagnet);

		for (IInitializer init : initList) {
			init.initialize();
		}
		MinecraftForge.EVENT_BUS.register(INSTANCE);
	}

	/* EVENT HANDLING */
	@SubscribeEvent
	public void registerRecipes(RegistryEvent.Register<IRecipe> event) {

		for (IInitializer init : initList) {
			init.register();
		}
	}

	private static ArrayList<IInitializer> initList = new ArrayList<>();

	/* REFERENCES */
	public static ItemDrill itemDrill;
	public static ItemMagnet itemMagnet;

}
