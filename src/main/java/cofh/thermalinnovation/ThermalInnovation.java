package cofh.thermalinnovation;

import cofh.CoFHCore;
import cofh.core.init.CoreProps;
import cofh.core.util.ConfigHandler;
import cofh.thermalfoundation.ThermalFoundation;
import cofh.thermalinnovation.gui.GuiHandler;
import cofh.thermalinnovation.init.TIBlocks;
import cofh.thermalinnovation.init.TIItems;
import cofh.thermalinnovation.init.TIProps;
import cofh.thermalinnovation.proxy.Proxy;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod (modid = ThermalInnovation.MOD_ID, name = ThermalInnovation.MOD_NAME, version = ThermalInnovation.VERSION, dependencies = ThermalInnovation.DEPENDENCIES, updateJSON = ThermalInnovation.UPDATE_URL, certificateFingerprint = "8a6abf2cb9e141b866580d369ba6548732eff25f")
public class ThermalInnovation {

	public static final String MOD_ID = "thermalinnovation";
	public static final String MOD_NAME = "Thermal Innovation";

	public static final String VERSION = "0.2.0";
	public static final String VERSION_MAX = "1.0.0";
	public static final String VERSION_GROUP = "required-after:" + MOD_ID + "@[" + VERSION + "," + VERSION_MAX + ");";
	public static final String UPDATE_URL = "https://raw.github.com/cofh/version/master/" + MOD_ID + "_update.json";

	public static final String DEPENDENCIES = CoFHCore.VERSION_GROUP + ThermalFoundation.VERSION_GROUP + "after:thermalexpansion";
	public static final String MOD_GUI_FACTORY = "cofh.thermalinnovation.gui.GuiConfigTIFactory";

	@Instance (MOD_ID)
	public static ThermalInnovation instance;

	@SidedProxy (clientSide = "cofh.thermalinnovation.proxy.ProxyClient", serverSide = "cofh.thermalinnovation.proxy.Proxy")
	public static Proxy proxy;

	public static final Logger LOG = LogManager.getLogger(MOD_ID);
	public static final ConfigHandler CONFIG = new ConfigHandler(VERSION);
	public static final ConfigHandler CONFIG_CLIENT = new ConfigHandler(VERSION);
	public static final GuiHandler GUI_HANDLER = new GuiHandler();

	public static CreativeTabs tabCommon;       // Blocks and general stuff.
	public static CreativeTabs tabItems;        // Non-usable items.
	public static CreativeTabs tabUtils;        // Usable items, non-tiered.
	public static CreativeTabs tabTools;        // Usable items, tiered.                (Unified Tabs Only)

	public ThermalInnovation() {

		super();
	}

	/* INIT */
	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {

		CONFIG.setConfiguration(new Configuration(new File(CoreProps.configDir, "/cofh/" + MOD_ID + "/common.cfg"), true));
		CONFIG_CLIENT.setConfiguration(new Configuration(new File(CoreProps.configDir, "/cofh/" + MOD_ID + "/client.cfg"), true));

		TIProps.preInit();

		TIBlocks.preInit();
		TIItems.preInit();

		/* Register Handlers */
		registerHandlers();

		proxy.preInit(event);
	}

	@EventHandler
	public void initialize(FMLInitializationEvent event) {

		proxy.initialize(event);
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {

		proxy.postInit(event);
	}

	@EventHandler
	public void loadComplete(FMLLoadCompleteEvent event) {

		CONFIG.cleanUp(false, true);
		CONFIG_CLIENT.cleanUp(false, true);

		LOG.info(MOD_NAME + ": Load Complete.");
	}

	/* HELPERS */
	private void registerHandlers() {

		NetworkRegistry.INSTANCE.registerGuiHandler(instance, GUI_HANDLER);
	}

}
