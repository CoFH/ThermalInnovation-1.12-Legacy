package cofh.thermalinnovation.item;

import cofh.api.item.IMultiModeItem;
import cofh.core.init.CoreEnchantments;
import cofh.core.item.ItemMultiRF;
import cofh.core.key.KeyBindingItemMultiMode;
import cofh.core.util.core.IInitializer;
import cofh.core.util.helpers.*;
import cofh.thermalinnovation.ThermalInnovation;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class ItemPump extends ItemMultiRF implements IInitializer, IMultiModeItem {

	public ItemPump() {

		super("thermalinnovation");

		setUnlocalizedName("pump");
		setCreativeTab(ThermalInnovation.tabCommon);

		setHasSubtypes(true);
		setMaxStackSize(1);
		setNoRepair();
	}

	@Override
	public ItemStack setDefaultTag(ItemStack stack, int energy) {

		EnergyHelper.setDefaultEnergyTag(stack, energy);
		stack.getTagCompound().setInteger("Mode", getNumModes(stack) - 1);

		return stack;
	}

	@Override
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {

		if (StringHelper.displayShiftForDetail && !StringHelper.isShiftKeyDown()) {
			tooltip.add(StringHelper.shiftForDetails());
		}
		if (!StringHelper.isShiftKeyDown()) {
			return;
		}
		int radius = getMode(stack) * 2 + 1;

		tooltip.add(StringHelper.getInfoText("info.thermalinnovation.pump.a.0"));

		if (radius > 1) {
			tooltip.add(StringHelper.localize("info.cofh.area") + ": " + radius + "x" + radius);
		}
		if (getNumModes(stack) > 1) {
			tooltip.add(StringHelper.localizeFormat("info.thermalinnovation.pump.b.0", StringHelper.getKeyName(KeyBindingItemMultiMode.INSTANCE.getKey())));
		}
		if (ItemHelper.getItemDamage(stack) == CREATIVE) {
			tooltip.add(StringHelper.localize("info.cofh.charge") + ": 1.21G RF");
		} else {
			tooltip.add(StringHelper.localize("info.cofh.charge") + ": " + StringHelper.getScaledNumber(getEnergyStored(stack)) + " / " + StringHelper.getScaledNumber(getMaxEnergyStored(stack)) + " RF");
		}
	}

	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {

		if (isInCreativeTab(tab)) {
			for (int metadata : itemList) {
				if (metadata != CREATIVE) {
					items.add(setDefaultTag(new ItemStack(this, 1, metadata), 0));
					items.add(setDefaultTag(new ItemStack(this, 1, metadata), getBaseCapacity(metadata)));
				} else {
					items.add(setDefaultTag(new ItemStack(this, 1, metadata), getBaseCapacity(metadata)));
				}
			}
		}
	}

	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean isSelected) {

		if (!isActive(stack)) {
			return;
		}
		long activeTime = stack.getTagCompound().getLong("Active");

		if (entity.world.getTotalWorldTime() > activeTime) {
			stack.getTagCompound().removeTag("Active");
		}
	}

	@Override
	public int getItemEnchantability(ItemStack stack) {

		return 10;
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

	protected int useEnergy(ItemStack stack, int count, boolean simulate) {

		if (ItemHelper.getItemDamage(stack) == CREATIVE) {
			return 0;
		}
		int unbreakingLevel = MathHelper.clamp(EnchantmentHelper.getEnchantmentLevel(Enchantments.UNBREAKING, stack), 0, 10);
		if (MathHelper.RANDOM.nextInt(2 + unbreakingLevel) >= 2) {
			return 0;
		}
		return extractEnergy(stack, count * ENERGY_PER_USE, simulate);
	}

	public int getBaseCapacity(int metadata) {

		if (!typeMap.containsKey(metadata)) {
			return 0;
		}
		return typeMap.get(metadata).capacity;
	}

	public boolean isActive(ItemStack stack) {

		return stack.getTagCompound() != null && stack.getTagCompound().hasKey("Active");
	}

	public void setActive(ItemStack stack, EntityLivingBase living) {

		stack.getTagCompound().setLong("Active", living.world.getTotalWorldTime() + 20);
	}

	/* IModelRegister */
	@Override
	@SideOnly (Side.CLIENT)
	public void registerModels() {

		ModelLoader.setCustomMeshDefinition(this, stack -> new ModelResourceLocation(getRegistryName(), String.format("state=%s,type=%s", this.getEnergyStored(stack) > 0 ? isActive(stack) ? "active" : "charged" : "drained", typeMap.get(ItemHelper.getItemDamage(stack)).name)));

		String[] states = { "charged", "active", "drained" };

		for (Map.Entry<Integer, ItemEntry> entry : itemMap.entrySet()) {
			for (int i = 0; i < 3; i++) {
				ModelBakery.registerItemVariants(this, new ModelResourceLocation(getRegistryName(), String.format("state=%s,type=%s", states[i], entry.getValue().name)));
			}
		}
	}

	/* IMultiModeItem */
	@Override
	public void onModeChange(EntityPlayer player, ItemStack stack) {

		player.world.playSound(null, player.getPosition(), SoundEvents.ITEM_BUCKET_FILL, SoundCategory.PLAYERS, 0.4F, 0.8F + 0.4F * getMode(stack));
		ChatHelper.sendIndexedChatMessageToPlayer(player, new TextComponentTranslation("info.thermalinnovation.pump.c." + getMode(stack)));
	}

	/* IEnchantableItem */
	@Override
	public boolean canEnchant(ItemStack stack, Enchantment enchantment) {

		return ItemHelper.getItemDamage(stack) != CREATIVE && enchantment == CoreEnchantments.holding;
	}

	/* IInitializer */
	@Override
	public boolean initialize() {

		config();

		pumpBasic = addEntryItem(0, "standard0", 0, EnumRarity.COMMON);
		pumpHardened = addEntryItem(1, "standard1", 1, EnumRarity.COMMON);
		pumpReinforced = addEntryItem(2, "standard2", 2, EnumRarity.UNCOMMON);
		pumpSignalum = addEntryItem(3, "standard3", 3, EnumRarity.UNCOMMON);
		pumpResonant = addEntryItem(4, "standard4", 4, EnumRarity.RARE);

		pumpCreative = addEntryItem(CREATIVE, "creative", CAPACITY[4], 0, 4, EnumRarity.EPIC);

		ThermalInnovation.proxy.addIModelRegister(this);

		return true;
	}

	@Override
	public boolean register() {

		if (!enable) {
			return false;
		}
		// @formatter:off

//		addShapedRecipe(pumpBasic,
//				"R R",
//				"IRI",
//				"XIX",
//				'I', "ingotIron",
//				'R', "dustRedstone",
//				'X', "ingotLead"
//		);

		// @formatter:on

		return true;
	}

	private static void config() {

		String category = "Item.Pump";
		String comment;

		enable = ThermalInnovation.CONFIG.get(category, "Enable", true);

		int capacity = CAPACITY_BASE;
		comment = "Adjust this value to change the amount of Energy (in RF) stored by a Basic FluiVac. This base value will scale with item level.";
		capacity = ThermalInnovation.CONFIG.getConfiguration().getInt("BaseCapacity", category, capacity, capacity / 5, capacity * 5, comment);

		int xfer = XFER_BASE;
		comment = "Adjust this value to change the amount of Energy (in RF/t) that can be received by a Basic FluiVac. This base value will scale with item level.";
		xfer = ThermalInnovation.CONFIG.getConfiguration().getInt("BaseReceive", category, xfer, xfer / 10, xfer * 10, comment);

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

	public static final int CAPACITY_BASE = 40000;
	public static final int XFER_BASE = 1000;
	public static final int ENERGY_PER_USE = 100;

	public static final int[] CAPACITY = { 1, 3, 6, 10, 15 };
	public static final int[] XFER = { 1, 4, 9, 16, 25 };

	public static boolean enable = true;

	/* REFERENCES */
	public static ItemStack pumpBasic;
	public static ItemStack pumpHardened;
	public static ItemStack pumpReinforced;
	public static ItemStack pumpSignalum;
	public static ItemStack pumpResonant;

	public static ItemStack pumpCreative;

}
