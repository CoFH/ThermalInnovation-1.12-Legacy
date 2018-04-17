package cofh.thermalinnovation.item;

import cofh.api.item.IInventoryContainerItem;
import cofh.api.item.IToolQuiver;
import cofh.core.gui.container.InventoryContainerItemWrapper;
import cofh.core.init.CoreEnchantments;
import cofh.core.init.CoreProps;
import cofh.core.key.KeyBindingItemMultiMode;
import cofh.core.util.core.IInitializer;
import cofh.core.util.helpers.ChatHelper;
import cofh.core.util.helpers.ItemHelper;
import cofh.core.util.helpers.MathHelper;
import cofh.core.util.helpers.StringHelper;
import cofh.thermalfoundation.init.TFProps;
import cofh.thermalinnovation.ThermalInnovation;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTippedArrow;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.*;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static cofh.core.util.helpers.RecipeHelper.addShapedRecipe;
import static cofh.thermalfoundation.util.TFCrafting.addPotionFillRecipe;

public class ItemQuiver extends ItemMultiPotion implements IInitializer, IToolQuiver {

	public ItemQuiver() {

		super("thermalinnovation");

		register("quiver");
		setUnlocalizedName("quiver");
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
		tooltip.add(StringHelper.getInfoText("info.thermalinnovation.quiver.a.0"));
		tooltip.add(StringHelper.localize("info.thermalinnovation.quiver.a.1"));
		tooltip.add(StringHelper.getNoticeText("info.thermalinnovation.quiver.a.2"));
		tooltip.add(StringHelper.localizeFormat("info.thermalinnovation.quiver.b." + getMode(stack), StringHelper.getKeyName(KeyBindingItemMultiMode.INSTANCE.getKey())));

		if (isCreative(stack)) {
			tooltip.add(StringHelper.localize("info.cofh.arrows") + ": " + StringHelper.localize("info.cofh.infinite"));
		} else {
			tooltip.add(StringHelper.localize("info.cofh.arrows") + ": " + getNumArrows(stack) + " / " + StringHelper.formatNumber(getMaxArrowCount(stack)));
		}
		FluidStack fluid = getFluid(stack);
		if (fluid != null) {
			String color = StringHelper.LIGHT_GRAY;

			if (fluid.getFluid().getRarity() == EnumRarity.UNCOMMON) {
				color = StringHelper.YELLOW;
			} else if (fluid.getFluid().getRarity() == EnumRarity.RARE) {
				color = StringHelper.BRIGHT_BLUE;
			} else if (fluid.getFluid().getRarity() == EnumRarity.EPIC) {
				color = StringHelper.PINK;
			}
			tooltip.add(StringHelper.localize("info.cofh.fluid") + ": " + color + fluid.getFluid().getLocalizedName(fluid) + StringHelper.LIGHT_GRAY);

			if (isCreative(stack)) {
				tooltip.add(StringHelper.localize("info.cofh.infiniteSource"));
			} else {
				tooltip.add(StringHelper.localize("info.cofh.level") + ": " + StringHelper.formatNumber(fluid.amount) + " / " + StringHelper.formatNumber(getCapacity(stack)) + " mB");
			}
			tooltip.add("");
			tooltip.add(StringHelper.localize("info.thermalinnovation.quiver.d"));
			addPotionTooltip(fluid.tag, tooltip);
		} else {
			tooltip.add(StringHelper.localize("info.cofh.fluid") + ": " + StringHelper.localize("info.cofh.empty"));

			if (isCreative(stack)) {
				tooltip.add(StringHelper.localize("info.cofh.infiniteSource"));
			} else {
				tooltip.add(StringHelper.localize("info.cofh.level") + ": 0 / " + StringHelper.formatNumber(getCapacity(stack)) + " mB");
			}
		}
	}

	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {

		if (isInCreativeTab(tab)) {
			for (int metadata : itemList) {
				if (metadata != CREATIVE) {
					items.add(new ItemStack(this, 1, metadata));
				} else {
					if (TFProps.showCreativeItems) {
						items.add(new ItemStack(this, 1, metadata));
					}
				}
			}
		}
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {

		return super.shouldCauseReequipAnimation(oldStack, newStack, slotChanged) && (slotChanged || !ItemHelper.areItemStacksEqualIgnoreTags(oldStack, newStack, ARROWS, CoreProps.FLUID));
	}

	@Override
	public int getItemEnchantability(ItemStack stack) {

		return 10;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {

		ItemStack stack = player.getHeldItem(hand);

		if (player.isSneaking()) {
			addArrows(stack, 1, false);
		} else {
			addArrows(stack, 64, false);
		}
		return new ActionResult<>(EnumActionResult.SUCCESS, stack);
	}

	/* HELPERS */
	public int getNumArrows(ItemStack stack) {

		if (stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}
		return stack.getTagCompound().getInteger(ARROWS);
	}

	public int getMaxArrowCount(ItemStack stack) {

		if (!typeMap.containsKey(ItemHelper.getItemDamage(stack))) {
			return 0;
		}
		int arrows = typeMap.get(ItemHelper.getItemDamage(stack)).arrows;
		int enchant = EnchantmentHelper.getEnchantmentLevel(CoreEnchantments.holding, stack);

		return arrows + arrows * enchant / 2;
	}

	public int addArrows(ItemStack stack, int maxArrows, boolean simulate) {

		if (stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}
		int stored = getNumArrows(stack);
		int toAdd = Math.min(maxArrows, getMaxArrowCount(stack) - stored);

		if (!simulate && !isCreative(stack)) {
			stored += toAdd;
			stack.getTagCompound().setInteger(ARROWS, stored);
		}
		return toAdd;
	}

	public int removeArrows(ItemStack stack, int maxArrows, boolean simulate) {

		if (stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}
		if (isCreative(stack)) {
			return maxArrows;
		}
		int stored = Math.min(stack.getTagCompound().getInteger(ARROWS), getMaxArrowCount(stack));
		int toRemove = Math.min(maxArrows, stored);

		if (!simulate) {
			stored -= toRemove;
			stack.getTagCompound().setInteger(ARROWS, stored);
		}
		return toRemove;
	}

	public static ItemStack findArrows(EntityPlayer player) {

		ItemStack offHand = player.getHeldItemOffhand();
		ItemStack mainHand = player.getHeldItemMainhand();

		if (isArrow(offHand)) {
			return offHand;
		} else if (isArrow(mainHand)) {
			return mainHand;
		}
		for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
			ItemStack stack = player.inventory.getStackInSlot(i);

			if (isArrow(stack)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	public static boolean isArrow(ItemStack stack) {

		return stack.getItem().equals(Items.ARROW);
	}

	public static boolean addToPlayerInventory(EntityPlayer player, ItemStack stack) {

		if (stack.isEmpty() || player == null) {
			return false;
		}
		InventoryPlayer inv = player.inventory;
		for (int i = 0; i < inv.mainInventory.size(); i++) {
			if (inv.mainInventory.get(i).isEmpty()) {
				inv.mainInventory.set(i, stack.copy());
				return true;
			}
		}
		return false;
	}

	@Override
	public int getMaxFluidAmount(ItemStack stack) {

		if (!typeMap.containsKey(ItemHelper.getItemDamage(stack))) {
			return 0;
		}
		int capacity = typeMap.get(ItemHelper.getItemDamage(stack)).capacity;
		int enchant = EnchantmentHelper.getEnchantmentLevel(CoreEnchantments.holding, stack);

		return capacity + capacity * enchant / 2;
	}

	public static boolean onItemPickup(EntityItemPickupEvent event, ItemStack stack) {

		if (!canPlayerAccess(stack, event.getEntityPlayer()) || ((ItemQuiver) stack.getItem()).getMode(stack) <= 0 || isCreative(stack)) {
			return false;
		}
		ItemStack eventItem = event.getItem().getItem();

		if (!(eventItem.getItem() instanceof IInventoryContainerItem) || ((IInventoryContainerItem) eventItem.getItem()).getSizeInventory(stack) <= 0) {
			int count = eventItem.getCount();
			InventoryContainerItemWrapper inv = new InventoryContainerItemWrapper(stack);
			for (int i = 0; i < inv.getSizeInventory(); i++) {
				ItemStack slot = inv.getStackInSlot(i);
				if (ItemHandlerHelper.canItemStacksStackRelaxed(eventItem, slot)) {
					int fill = slot.getMaxStackSize() - slot.getCount();
					if (fill > eventItem.getCount()) {
						slot.setCount(slot.getCount() + eventItem.getCount());
					} else {
						slot.setCount(slot.getMaxStackSize());
					}
					eventItem.splitStack(fill);
				} else if (slot.isEmpty()) {
					inv.setInventorySlotContents(i, eventItem.copy());
					eventItem.setCount(0);
				}
				if (eventItem.isEmpty()) {
					break;
				}
			}
			if (eventItem.getCount() != count) {
				stack.setAnimationsToGo(5);
				EntityPlayer player = event.getEntityPlayer();
				player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.2F, ((MathHelper.RANDOM.nextFloat() - MathHelper.RANDOM.nextFloat()) * 0.7F + 1.0F) * 2.0F);
				inv.markDirty();
			}
		}
		return eventItem.isEmpty();
	}

	/* IModelRegister */
	@Override
	@SideOnly (Side.CLIENT)
	public void registerModels() {

		ModelLoader.setCustomMeshDefinition(this, stack -> new ModelResourceLocation(getRegistryName(), String.format("fill=%s,type=%s", MathHelper.clamp(getFluidAmount(stack) > 0 ? 1 + getScaledFluidStored(stack, 7) : 0, 0, 7), typeMap.get(ItemHelper.getItemDamage(stack)).name)));

		for (Map.Entry<Integer, ItemEntry> entry : itemMap.entrySet()) {
			for (int fill = 0; fill < 8; fill++) {
				ModelBakery.registerItemVariants(this, new ModelResourceLocation(getRegistryName(), String.format("fill=%s,type=%s", fill, entry.getValue().name)));
			}
		}
	}

	/* IMultiModeItem */
	@Override
	public void onModeChange(EntityPlayer player, ItemStack stack) {

		int mode = getMode(stack);
		if (mode == 1) {
			player.world.playSound(null, player.getPosition(), SoundEvents.ENTITY_PLAYER_SPLASH, SoundCategory.PLAYERS, 0.4F, 1.0F);
		} else {
			player.world.playSound(null, player.getPosition(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.2F, 0.6F);
		}
		ChatHelper.sendIndexedChatMessageToPlayer(player, new TextComponentTranslation("info.thermalinnovation.quiver.c." + mode));
	}

	/* IToolQuiver */
	@Override
	public EntityArrow createEntityArrow(World world, ItemStack item, EntityLivingBase shooter) {

		FluidStack fluid = getFluid(item);
		ItemStack arrowStack;

		if (getMode(item) == 1 && fluid != null && fluid.amount >= MB_PER_ARROW) {
			arrowStack = PotionUtils.addPotionToItemStack(new ItemStack(Items.TIPPED_ARROW), PotionUtils.getPotionTypeFromNBT(fluid.tag));
			return ((ItemTippedArrow) arrowStack.getItem()).createArrow(world, arrowStack, shooter);
		}
		arrowStack = new ItemStack(Items.ARROW);
		return ((ItemArrow) arrowStack.getItem()).createArrow(world, arrowStack, shooter);
	}

	@Override
	public boolean isEmpty(ItemStack item, EntityLivingBase shooter) {

		if (isCreative(item) || (shooter instanceof EntityPlayer && ((EntityPlayer) shooter).capabilities.isCreativeMode)) {
			return false;
		}
		return getNumArrows(item) <= 0;
	}

	@Override
	public void onArrowFired(ItemStack item, EntityLivingBase shooter) {

		if (shooter instanceof EntityPlayer) {
			boolean creativeMode = ((EntityPlayer) shooter).capabilities.isCreativeMode;
			removeArrows(item, 1, creativeMode);
			drain(item, MB_PER_ARROW, getMode(item) == 1 && !creativeMode);
		}
	}

	/* IInitializer */
	@Override
	public boolean initialize() {

		config();

		quiverBasic = addEntryItem(0, "standard0", CAPACITY_ARROW[0], CAPACITY_FLUID[0], EnumRarity.COMMON);
		quiverHardened = addEntryItem(1, "standard1", CAPACITY_ARROW[1], CAPACITY_FLUID[1], EnumRarity.COMMON);
		quiverReinforced = addEntryItem(2, "standard2", CAPACITY_ARROW[2], CAPACITY_FLUID[2], EnumRarity.UNCOMMON);
		quiverSignalum = addEntryItem(3, "standard3", CAPACITY_ARROW[3], CAPACITY_FLUID[3], EnumRarity.UNCOMMON);
		quiverResonant = addEntryItem(4, "standard4", CAPACITY_ARROW[4], CAPACITY_FLUID[4], EnumRarity.RARE);

		quiverCreative = addEntryItem(CREATIVE, "creative", CAPACITY_ARROW[4], CAPACITY_FLUID[4], EnumRarity.EPIC);

		ThermalInnovation.proxy.addIModelRegister(this);

		return true;
	}

	@Override
	public boolean register() {

		if (!enable) {
			return false;
		}
		// @formatter:off
		addShapedRecipe(quiverBasic,
				"BB ",
				"XLS",
				"LXS",
				'B', Items.GLASS_BOTTLE,
				'L', Items.LEATHER,
				'S', Items.STRING,
				'X', "ingotCopper"
		);
		// @formatter:on

		addPotionFillRecipe(quiverBasic, quiverBasic, "cofh:potion");
		addPotionFillRecipe(quiverHardened, quiverHardened, "cofh:potion");
		addPotionFillRecipe(quiverReinforced, quiverReinforced, "cofh:potion");
		addPotionFillRecipe(quiverSignalum, quiverSignalum, "cofh:potion");
		addPotionFillRecipe(quiverResonant, quiverResonant, "cofh:potion");
		addPotionFillRecipe(quiverCreative, quiverCreative, "cofh:potion");
		return true;
	}

	private static void config() {

		String category = "Item.Quiver";
		String comment;

		int capacity = CAPACITY_ARROW_BASE;
		comment = "Adjust this value to change the quantity of arrows stored by a Basic Decoctive Quiver. This base value will scale with item level.";
		capacity = ThermalInnovation.CONFIG.getConfiguration().getInt("BaseArrowCapacity", category, capacity, capacity / 5, capacity * 5, comment);

		for (int i = 0; i < CAPACITY_FLUID.length; i++) {
			CAPACITY_ARROW[i] *= capacity;
		}

		capacity = CAPACITY_FLUID_BASE;
		comment = "Adjust this value to change the amount of Fluid (in mB) stored by a Basic Decoctive Quiver. This base value will scale with item level.";
		capacity = ThermalInnovation.CONFIG.getConfiguration().getInt("BaseFluidCapacity", category, capacity, capacity / 5, capacity * 5, comment);

		for (int i = 0; i < CAPACITY_FLUID.length; i++) {
			CAPACITY_FLUID[i] *= capacity;
		}
	}

	/* ENTRY */
	public class TypeEntry {

		public final String name;
		public final int arrows;
		public final int capacity;

		TypeEntry(String name, int arrows, int capacity) {

			this.name = name;
			this.arrows = arrows;
			this.capacity = capacity;
		}
	}

	private void addEntry(int metadata, String name, int arrows, int capacity) {

		typeMap.put(metadata, new TypeEntry(name, arrows, capacity));
	}

	private ItemStack addEntryItem(int metadata, String name, int arrows, int capacity, EnumRarity rarity) {

		addEntry(metadata, name, arrows, capacity);
		return addItem(metadata, name, rarity);
	}

	private static TIntObjectHashMap<TypeEntry> typeMap = new TIntObjectHashMap<>();

	public static final String ARROWS = "Arrows";
	public static final int CAPACITY_ARROW_BASE = 40;
	public static final int CAPACITY_FLUID_BASE = 2000;
	public static final int MB_PER_ARROW = 50;

	public static final int[] CAPACITY_ARROW = { 1, 3, 6, 10, 15 };
	public static final int[] CAPACITY_FLUID = { 1, 3, 6, 10, 15 };

	public static boolean enable = true;

	/* REFERENCES */
	public static ItemStack quiverBasic;
	public static ItemStack quiverHardened;
	public static ItemStack quiverReinforced;
	public static ItemStack quiverSignalum;
	public static ItemStack quiverResonant;

	public static ItemStack quiverCreative;

}
