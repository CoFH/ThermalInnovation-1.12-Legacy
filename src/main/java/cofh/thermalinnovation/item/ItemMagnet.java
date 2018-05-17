package cofh.thermalinnovation.item;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import cofh.core.init.CoreEnchantments;
import cofh.core.init.CoreProps;
import cofh.core.item.ItemMultiRF;
import cofh.core.key.KeyBindingItemMultiMode;
import cofh.core.util.CoreUtils;
import cofh.core.util.RayTracer;
import cofh.core.util.core.IInitializer;
import cofh.core.util.filter.ItemFilterWrapper;
import cofh.core.util.helpers.*;
import cofh.thermalfoundation.init.TFProps;
import cofh.thermalfoundation.init.TFSounds;
import cofh.thermalinnovation.ThermalInnovation;
import cofh.thermalinnovation.gui.GuiHandler;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static cofh.core.util.helpers.RecipeHelper.*;

@Optional.Interface (iface = "baubles.api.IBauble", modid = "baubles")
public class ItemMagnet extends ItemMultiRF implements IInitializer, IBauble {

	public ItemMagnet() {

		super("thermalinnovation");

		setUnlocalizedName("magnet");
		setCreativeTab(ThermalInnovation.tabTools);

		setHasSubtypes(true);
		setMaxStackSize(1);
		setNoRepair();
	}

	@Override
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {

		if (StringHelper.displayShiftForDetail && !StringHelper.isShiftKeyDown()) {
			tooltip.add(StringHelper.shiftForDetails());
		}
		if (!StringHelper.isShiftKeyDown()) {
			return;
		}
		int radius = getRadius(stack);

		tooltip.add(StringHelper.getInfoText("info.thermalinnovation.magnet.a.0"));
		tooltip.add(StringHelper.localize("info.thermalinnovation.magnet.a.1"));
		tooltip.add(StringHelper.getNoticeText("info.thermalinnovation.magnet.a.2"));
		tooltip.add(StringHelper.localizeFormat("info.thermalinnovation.magnet.b." + getMode(stack), StringHelper.getKeyName(KeyBindingItemMultiMode.INSTANCE.getKey())));
		tooltip.add(StringHelper.ORANGE + StringHelper.localize("info.cofh.radius") + ": " + radius + StringHelper.END);

		if (isCreative(stack)) {
			tooltip.add(StringHelper.localize("info.cofh.charge") + ": 1.21G RF");
		} else {
			tooltip.add(StringHelper.localize("info.cofh.charge") + ": " + StringHelper.getScaledNumber(getEnergyStored(stack)) + " / " + StringHelper.getScaledNumber(getMaxEnergyStored(stack)) + " RF");
		}
	}

	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {

		if (enable && isInCreativeTab(tab)) {
			for (int metadata : itemList) {
				if (metadata != CREATIVE) {
					if (TFProps.showEmptyItems) {
						items.add(EnergyHelper.setDefaultEnergyTag(new ItemStack(this, 1, metadata), 0));
					}
					if (TFProps.showFullItems) {
						items.add(EnergyHelper.setDefaultEnergyTag(new ItemStack(this, 1, metadata), getBaseCapacity(metadata)));
					}
				} else {
					if (TFProps.showCreativeItems) {
						items.add(EnergyHelper.setDefaultEnergyTag(new ItemStack(this, 1, metadata), getBaseCapacity(metadata)));
					}
				}
			}
		}
	}

	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {

		if (world.getTotalWorldTime() % CoreProps.TIME_CONSTANT_QUARTER != 0) {
			return;
		}
		if (!(entity instanceof EntityPlayer) || CoreUtils.isFakePlayer(entity) || entity.isSneaking() || getMode(stack) <= 0) {
			return;
		}
		EntityPlayer player = (EntityPlayer) entity;

		if (getEnergyStored(stack) < ENERGY_PER_ITEM && !player.capabilities.isCreativeMode) {
			return;
		}
		int radius = getRadius(stack);
		int radSq = radius * radius;

		AxisAlignedBB area = new AxisAlignedBB(player.getPosition().add(-radius, -radius, -radius), player.getPosition().add(1 + radius, 1 + radius, 1 + radius));
		List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class, area, EntitySelectors.IS_ALIVE);
		ItemFilterWrapper wrapper = new ItemFilterWrapper(stack, getFilterSize(stack));

		if (ServerHelper.isClientWorld(world)) {
			for (EntityItem item : items) {
				if (item.getEntityData().getBoolean(CONVEYOR_COMPAT)) {
					continue;
				}
				if (item.getPositionVector().squareDistanceTo(player.getPositionVector()) <= radSq && wrapper.getFilter().matches(item.getItem())) {
					world.spawnParticle(EnumParticleTypes.REDSTONE, item.posX, item.posY, item.posZ, 0, 0, 0, 0);
				}
			}
		} else {
			int itemCount = 0;
			for (EntityItem item : items) {
				if (item.getEntityData().getBoolean(CONVEYOR_COMPAT)) {
					continue;
				}
				if (item.getThrower() == null || !item.getThrower().equals(player.getName()) || item.age >= 2 * CoreProps.TIME_CONSTANT) {
					if (item.getPositionVector().squareDistanceTo(player.getPositionVector()) <= radSq && wrapper.getFilter().matches(item.getItem())) {
						item.setPosition(player.posX, player.posY, player.posZ);
						item.setPickupDelay(0);
						itemCount++;
					}
				}
			}
			if (!player.capabilities.isCreativeMode) {
				extractEnergy(stack, ENERGY_PER_ITEM * itemCount, false);
			}
		}
	}

	@Override
	public int getItemEnchantability(ItemStack stack) {

		return 10;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {

		ItemStack stack = player.getHeldItem(hand);
		if (CoreUtils.isFakePlayer(player)) {
			return new ActionResult<>(EnumActionResult.FAIL, stack);
		}
		if (player.isSneaking()) {
			player.openGui(ThermalInnovation.instance, GuiHandler.MAGNET_FILTER_ID, world, 0, 0, 0);
		} else if (getEnergyStored(stack) >= ENERGY_PER_USE || player.capabilities.isCreativeMode) {
			RayTraceResult traceResult = RayTracer.retrace(player, 64);
			if (traceResult == null || traceResult.typeOfHit != RayTraceResult.Type.BLOCK) {
				return ActionResult.newResult(EnumActionResult.PASS, stack);
			}
			int radius = getRadius(stack);
			int radSq = radius * radius;

			AxisAlignedBB area = new AxisAlignedBB(traceResult.getBlockPos().add(-radius, -radius, -radius), traceResult.getBlockPos().add(1 + radius, 1 + radius, 1 + radius));
			List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class, area, EntitySelectors.IS_ALIVE);
			ItemFilterWrapper wrapper = new ItemFilterWrapper(stack, getFilterSize(stack));

			if (ServerHelper.isClientWorld(world)) {
				for (EntityItem item : items) {
					if (item.getPositionVector().squareDistanceTo(traceResult.hitVec) <= radSq && wrapper.getFilter().matches(item.getItem())) {
						world.spawnParticle(EnumParticleTypes.PORTAL, item.posX, item.posY, item.posZ, 0, 0, 0, 0);
					}
				}
			} else {
				int itemCount = 0;
				for (EntityItem item : items) {
					if (item.getPositionVector().squareDistanceTo(traceResult.hitVec) <= radSq && wrapper.getFilter().matches(item.getItem())) {
						item.setPosition(player.posX, player.posY, player.posZ);
						item.setPickupDelay(0);
						itemCount++;
					}
				}
				if (!player.capabilities.isCreativeMode) {
					extractEnergy(stack, ENERGY_PER_USE + ENERGY_PER_ITEM * itemCount, false);
				}
			}
			player.swingArm(hand);
			stack.setAnimationsToGo(5);
			player.world.playSound(null, player.getPosition(), TFSounds.magnetUse, SoundCategory.PLAYERS, 0.4F, 1.0F);
		}
		return new ActionResult<>(EnumActionResult.SUCCESS, stack);
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer playerIn, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {

		return EnumActionResult.FAIL;
	}

	/* HELPERS */
	@Override
	protected int getCapacity(ItemStack stack) {

		if (!typeMap.containsKey(ItemHelper.getItemDamage(stack))) {
			return 0;
		}
		int capacity = typeMap.get(ItemHelper.getItemDamage(stack)).capacity;
		int enchant = EnchantmentHelper.getEnchantmentLevel(CoreEnchantments.holding, stack);

		return capacity + capacity * enchant / 2;
	}

	@Override
	protected int getReceive(ItemStack stack) {

		if (!typeMap.containsKey(ItemHelper.getItemDamage(stack))) {
			return 0;
		}
		return typeMap.get(ItemHelper.getItemDamage(stack)).recv;
	}

	public int getBaseCapacity(int metadata) {

		if (!typeMap.containsKey(metadata)) {
			return 0;
		}
		return typeMap.get(metadata).capacity;
	}

	public int getRadius(ItemStack stack) {

		if (!typeMap.containsKey(ItemHelper.getItemDamage(stack))) {
			return 0;
		}
		return typeMap.get(ItemHelper.getItemDamage(stack)).level + 4;
	}

	public static int getLevel(ItemStack stack) {

		if (!typeMap.containsKey(ItemHelper.getItemDamage(stack))) {
			return 0;
		}
		return typeMap.get(ItemHelper.getItemDamage(stack)).level;
	}

	public static int getFilterSize(ItemStack stack) {

		return CoreProps.FILTER_SIZE[getLevel(stack)];
	}

	/* IBauble */
	@Override
	public BaubleType getBaubleType(ItemStack stack) {

		return BaubleType.TRINKET;
	}

	@Override
	public void onWornTick(ItemStack stack, EntityLivingBase player) {

		World world = player.world;
		if (world.getTotalWorldTime() % CoreProps.TIME_CONSTANT_QUARTER != 0) {
			return;
		}
		if (!(player instanceof EntityPlayer) || CoreUtils.isFakePlayer(player) || player.isSneaking() || getMode(stack) <= 0) {
			return;
		}
		EntityPlayer castPlayer = (EntityPlayer) player;
		if (getEnergyStored(stack) < ENERGY_PER_ITEM && !castPlayer.capabilities.isCreativeMode) {
			return;
		}
		int radius = getRadius(stack);
		int radSq = radius * radius;

		AxisAlignedBB area = new AxisAlignedBB(player.getPosition().add(-radius, -radius, -radius), player.getPosition().add(1 + radius, 1 + radius, 1 + radius));
		List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class, area, EntitySelectors.IS_ALIVE);
		ItemFilterWrapper wrapper = new ItemFilterWrapper(stack, getFilterSize(stack));

		if (ServerHelper.isClientWorld(world)) {
			for (EntityItem item : items) {
				if (item.getEntityData().getBoolean(CONVEYOR_COMPAT)) {
					continue;
				}
				if (item.getPositionVector().squareDistanceTo(player.getPositionVector()) <= radSq && wrapper.getFilter().matches(item.getItem())) {
					world.spawnParticle(EnumParticleTypes.REDSTONE, item.posX, item.posY, item.posZ, 0, 0, 0, 0);
				}
			}
		} else {
			int itemCount = 0;
			for (EntityItem item : items) {
				if (item.getEntityData().getBoolean(CONVEYOR_COMPAT)) {
					continue;
				}
				if (item.getThrower() == null || !item.getThrower().equals(player.getName()) || item.age >= 2 * CoreProps.TIME_CONSTANT) {
					if (item.getPositionVector().squareDistanceTo(player.getPositionVector()) <= radSq && wrapper.getFilter().matches(item.getItem())) {
						item.setPosition(player.posX, player.posY, player.posZ);
						item.setPickupDelay(0);
						itemCount++;
					}
				}
			}
			if (!castPlayer.capabilities.isCreativeMode) {
				extractEnergy(stack, ENERGY_PER_ITEM * itemCount, false);
			}
		}
	}

	@Override
	public boolean willAutoSync(ItemStack stack, EntityLivingBase player) {

		return true;
	}

	/* IMultiModeItem */
	@Override
	public void onModeChange(EntityPlayer player, ItemStack stack) {

		player.world.playSound(null, player.getPosition(), TFSounds.magnetUse, SoundCategory.PLAYERS, 0.4F, 0.8F + 0.4F * getMode(stack));
		ChatHelper.sendIndexedChatMessageToPlayer(player, new TextComponentTranslation("info.thermalinnovation.magnet.c." + getMode(stack)));
	}

	/* IModelRegister */
	@Override
	@SideOnly (Side.CLIENT)
	public void registerModels() {

		ModelLoader.setCustomMeshDefinition(this, stack -> new ModelResourceLocation(getRegistryName(), String.format("color0=%s,state=%s,type=%s", ColorHelper.hasColor0(stack) ? 1 : 0, this.getEnergyStored(stack) > 0 ? getMode(stack) == 1 ? "active" : "charged" : "drained", typeMap.get(ItemHelper.getItemDamage(stack)).name)));

		String[] states = { "charged", "active", "drained" };

		for (Map.Entry<Integer, ItemEntry> entry : itemMap.entrySet()) {
			for (int color0 = 0; color0 < 2; color0++) {
				for (int state = 0; state < 3; state++) {
					ModelBakery.registerItemVariants(this, new ModelResourceLocation(getRegistryName(), String.format("color0=%s,state=%s,type=%s", color0, states[state], entry.getValue().name)));
				}
			}
		}
	}

	/* IInitializer */
	@Override
	public boolean preInit() {

		ForgeRegistries.ITEMS.register(setRegistryName("magnet"));
		ThermalInnovation.proxy.addIModelRegister(this);

		config();

		magnetBasic = addEntryItem(0, "standard0", 0, EnumRarity.COMMON);
		magnetHardened = addEntryItem(1, "standard1", 1, EnumRarity.COMMON);
		magnetReinforced = addEntryItem(2, "standard2", 2, EnumRarity.UNCOMMON);
		magnetSignalum = addEntryItem(3, "standard3", 3, EnumRarity.UNCOMMON);
		magnetResonant = addEntryItem(4, "standard4", 4, EnumRarity.RARE);

		magnetCreative = addEntryItem(CREATIVE, "creative", CAPACITY[4], 0, 4, EnumRarity.EPIC);

		return true;
	}

	@Override
	public boolean initialize() {

		if (!enable) {
			return false;
		}
		// @formatter:off
		addShapedRecipe(magnetBasic,
				"IRI",
				"XIX",
				" R ",
				'I', "ingotIron",
				'R', "dustRedstone",
				'X', "ingotLead"
		);
		addShapedUpgradeRecipe(magnetHardened,
				" R ",
				"IXI",
				"R R",
				'I', "ingotInvar",
				'R', "nuggetCopper",
				'X', magnetBasic
		);
		addShapedUpgradeRecipe(magnetReinforced,
				" R ",
				"IXI",
				"R R",
				'I', "ingotElectrum",
				'R', "nuggetInvar",
				'X', magnetHardened
		);
		addShapedUpgradeRecipe(magnetSignalum,
				" R ",
				"IXI",
				"R R",
				'I', "ingotSignalum",
				'R', "nuggetElectrum",
				'X', magnetReinforced
		);
		addShapedUpgradeRecipe(magnetResonant,
				" R ",
				"IXI",
				"R R",
				'I', "ingotEnderium",
				'R', "nuggetSignalum",
				'X', magnetSignalum
		);
		// @formatter:on

		addColorRecipe(magnetBasic, magnetBasic, "dye");
		addColorRecipe(magnetHardened, magnetHardened, "dye");
		addColorRecipe(magnetReinforced, magnetReinforced, "dye");
		addColorRecipe(magnetSignalum, magnetSignalum, "dye");
		addColorRecipe(magnetResonant, magnetResonant, "dye");

		addColorRemoveRecipe(magnetBasic, magnetBasic);
		addColorRemoveRecipe(magnetHardened, magnetHardened);
		addColorRemoveRecipe(magnetReinforced, magnetReinforced);
		addColorRemoveRecipe(magnetSignalum, magnetSignalum);
		addColorRemoveRecipe(magnetResonant, magnetResonant);
		return true;
	}

	private static void config() {

		String category = "Item.Magnet";
		String comment;

		enable = ThermalInnovation.CONFIG.get(category, "Enable", true);

		int capacity = CAPACITY_BASE;
		comment = "Adjust this value to change the amount of Energy (in RF) stored by a Basic Fluxomagnet. This base value will scale with item level.";
		capacity = ThermalInnovation.CONFIG.getConfiguration().getInt("BaseCapacity", category, capacity, CAPACITY_MIN, CAPACITY_MAX, comment);

		int xfer = XFER_BASE;
		comment = "Adjust this value to change the amount of Energy (in RF/t) that can be received by a Basic Fluxomagnet. This base value will scale with item level.";
		xfer = ThermalInnovation.CONFIG.getConfiguration().getInt("BaseReceive", category, xfer, XFER_MIN, XFER_MAX, comment);

		for (int i = 0; i < CAPACITY.length; i++) {
			CAPACITY[i] *= capacity;
			XFER[i] *= xfer;
		}
	}

	/* ENTRY */
	public class TypeEntry {

		public final String name;

		public final int capacity;
		public final int recv;
		public final int level;

		TypeEntry(String name, int capacity, int recv, int level) {

			this.name = name;
			this.capacity = capacity;
			this.recv = recv;
			this.level = level;
		}
	}

	private void addEntry(int metadata, String name, int capacity, int xfer, int level) {

		typeMap.put(metadata, new TypeEntry(name, capacity, xfer, level));
	}

	private ItemStack addEntryItem(int metadata, String name, int level, EnumRarity rarity) {

		addEntry(metadata, name, CAPACITY[metadata], XFER[metadata], level);
		return addItem(metadata, name, rarity);
	}

	private ItemStack addEntryItem(int metadata, String name, int capacity, int xfer, int radius, EnumRarity rarity) {

		addEntry(metadata, name, capacity, xfer, radius);
		return addItem(metadata, name, rarity);
	}

	private static TIntObjectHashMap<TypeEntry> typeMap = new TIntObjectHashMap<>();

	public static final String CONVEYOR_COMPAT = "PreventRemoteMovement";

	public static final int CAPACITY_BASE = 40000;
	public static final int XFER_BASE = 1000;
	public static final int ENERGY_PER_ITEM = 25;
	public static final int ENERGY_PER_USE = 250;

	public static final int[] CAPACITY = { 1, 3, 6, 10, 15 };
	public static final int[] XFER = { 1, 4, 9, 16, 25 };

	public static boolean enable = true;

	/* REFERENCES */
	public static ItemStack magnetBasic;
	public static ItemStack magnetHardened;
	public static ItemStack magnetReinforced;
	public static ItemStack magnetSignalum;
	public static ItemStack magnetResonant;

	public static ItemStack magnetCreative;

}
