package cofh.thermalinnovation.init;

import cofh.core.util.core.IInitializer;
import cofh.thermalfoundation.init.TFProps;
import cofh.thermalinnovation.item.*;
import net.minecraft.item.ItemStack;
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
		itemSaw = new ItemSaw();
		//      itemLaser = new ItemLaser();
		//		itemPump = new ItemPump();
		itemMagnet = new ItemMagnet();
		itemInjector = new ItemInjector();
		itemQuiver = new ItemQuiver();

		initList.add(itemDrill);
		initList.add(itemSaw);
		//      initList.add(itemLaser);
		//		initList.add(itemPump);
		initList.add(itemMagnet);
		initList.add(itemInjector);
		initList.add(itemQuiver);

		for (IInitializer init : initList) {
			init.preInit();
		}
		for (int i = 0; i < 5; i++) {
			ItemStack iconStack = new ItemStack(itemDrill, 1, i);
			TFProps.toolList.add(iconStack.copy());

			iconStack = new ItemStack(itemSaw, 1, i);
			TFProps.toolList.add(iconStack.copy());

			iconStack = new ItemStack(itemMagnet, 1, i);
			TFProps.toolList.add(iconStack.copy());

			iconStack = new ItemStack(itemInjector, 1, i);
			TFProps.toolList.add(iconStack.copy());

			iconStack = new ItemStack(itemQuiver, 1, i);
			TFProps.toolList.add(iconStack.copy());
		}
		MinecraftForge.EVENT_BUS.register(INSTANCE);
	}

	/* EVENT HANDLING */
	@SubscribeEvent
	public void registerRecipes(RegistryEvent.Register<IRecipe> event) {

		for (IInitializer init : initList) {
			init.initialize();
		}
	}

	private static ArrayList<IInitializer> initList = new ArrayList<>();

	/* REFERENCES */
	public static ItemDrill itemDrill;
	public static ItemSaw itemSaw;
	//  public static ItemLaser itemLaser;
	//	public static ItemPump itemPump;
	public static ItemMagnet itemMagnet;
	public static ItemInjector itemInjector;
	public static ItemQuiver itemQuiver;

}
