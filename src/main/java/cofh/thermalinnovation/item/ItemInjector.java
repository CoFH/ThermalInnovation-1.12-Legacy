package cofh.thermalinnovation.item;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import cofh.core.init.CoreEnchantments;
import cofh.core.init.CoreProps;
import cofh.core.key.KeyBindingItemMultiMode;
import cofh.core.util.CoreUtils;
import cofh.core.util.core.IInitializer;
import cofh.core.util.helpers.*;
import cofh.thermalfoundation.init.TFProps;
import cofh.thermalinnovation.ThermalInnovation;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.*;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static cofh.core.util.helpers.RecipeHelper.*;
import static cofh.thermalfoundation.util.TFCrafting.addPotionFillRecipe;

@Optional.Interface (iface = "baubles.api.IBauble", modid = "baubles")
public class ItemInjector extends ItemMultiPotion implements IInitializer, IBauble {

	public ItemInjector() {

		super("thermalinnovation");

		setUnlocalizedName("injector");
		setCreativeTab(ThermalInnovation.tabTools);
	}

	@Override
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {

		if (StringHelper.displayShiftForDetail && !StringHelper.isShiftKeyDown()) {
			tooltip.add(StringHelper.shiftForDetails());
		}
		if (!StringHelper.isShiftKeyDown()) {
			return;
		}
		tooltip.add(StringHelper.getInfoText("info.thermalinnovation.injector.a.0"));
		tooltip.add(StringHelper.localize("info.thermalinnovation.injector.a.1"));
		tooltip.add(StringHelper.getNoticeText("info.thermalinnovation.injector.a.2"));
		tooltip.add(StringHelper.localizeFormat("info.thermalinnovation.injector.b." + getMode(stack), StringHelper.getKeyName(KeyBindingItemMultiMode.INSTANCE.getKey())));

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
			tooltip.add(StringHelper.localize("info.thermalinnovation.injector.d"));
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

		if (enable && isInCreativeTab(tab)) {
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
	public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {

		if (ServerHelper.isClientWorld(world) || world.getTotalWorldTime() % CoreProps.TIME_CONSTANT != 0) {
			return;
		}
		if (!(entity instanceof EntityLivingBase) || CoreUtils.isFakePlayer(entity) || getMode(stack) == 0) {
			return;
		}
		EntityLivingBase living = (EntityLivingBase) entity;
		FluidStack fluid = getFluid(stack);
		if (fluid != null && fluid.amount >= MB_PER_CYCLE) {
			boolean used = false;
			for (PotionEffect effect : PotionUtils.getEffectsFromTag(fluid.tag)) {
				PotionEffect active = living.getActivePotionMap().get(effect.getPotion());

				if (active != null && active.getDuration() >= 40) {
					continue;
				}
				if (effect.getPotion().isInstant()) {
					effect.getPotion().affectEntity(null, null, (EntityLivingBase) entity, effect.getAmplifier(), 0.5D);
				} else {
					PotionEffect potion = new PotionEffect(effect.getPotion(), effect.getDuration() / 4, effect.getAmplifier(), effect.getIsAmbient(), false);
					living.addPotionEffect(potion);
				}
				used = true;
			}
			if (used) {
				drain(stack, MB_PER_CYCLE, true);
			}
		}
	}

	@Override
	public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase entity, EnumHand hand) {

		FluidStack fluid = getFluid(stack);
		if (fluid != null && fluid.amount >= MB_PER_USE) {
			if (ServerHelper.isServerWorld(entity.world)) {
				for (PotionEffect effect : PotionUtils.getEffectsFromTag(fluid.tag)) {
					if (effect.getPotion().isInstant()) {
						effect.getPotion().affectEntity(player, player, entity, effect.getAmplifier(), 0.5D);
					} else {
						PotionEffect potion = new PotionEffect(effect.getPotion(), effect.getDuration() / 2, effect.getAmplifier(), effect.getIsAmbient(), true);
						entity.addPotionEffect(potion);
					}
				}
				drain(stack, MB_PER_USE, true);
			}
			player.swingArm(hand);
			return true;
		}
		return false;
	}

	@Override
	public int getItemEnchantability(ItemStack stack) {

		return 10;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {

		ItemStack stack = player.getHeldItem(hand);

		if (player.isSneaking()) {
			if (ServerHelper.isServerWorld(world)) {
				FluidStack fluid = getFluid(stack);
				if (fluid != null && fluid.amount >= MB_PER_USE) {
					for (PotionEffect effect : PotionUtils.getEffectsFromTag(fluid.tag)) {
						if (effect.getPotion().isInstant()) {
							effect.getPotion().affectEntity(null, null, player, effect.getAmplifier(), 1.0D);
						} else {
							PotionEffect potion = new PotionEffect(effect.getPotion(), effect.getDuration(), effect.getAmplifier(), effect.getIsAmbient(), false);
							player.addPotionEffect(potion);
						}
					}
					drain(stack, MB_PER_USE, true);
				}
			}
			player.swingArm(hand);
			return new ActionResult<>(EnumActionResult.FAIL, stack);
		}
		return new ActionResult<>(EnumActionResult.PASS, stack);
	}

	/* HELPERS */
	protected int getMaxFluidAmount(ItemStack stack) {

		if (!typeMap.containsKey(ItemHelper.getItemDamage(stack))) {
			return 0;
		}
		int capacity = typeMap.get(ItemHelper.getItemDamage(stack)).capacity;
		int enchant = EnchantmentHelper.getEnchantmentLevel(CoreEnchantments.holding, stack);

		return capacity + capacity * enchant / 2;
	}

	/* IItemColor */
	public int colorMultiplier(ItemStack stack, int tintIndex) {

		if (tintIndex == TINT_INDEX_0 && ColorHelper.hasColor0(stack)) {
			return ColorHelper.getColor0(stack);
		}
		return super.colorMultiplier(stack, tintIndex);
	}

	@Override
	public int getMaxColorIndex(ItemStack stack) {

		return 1;
	}

	/* IBauble */
	@Override
	public BaubleType getBaubleType(ItemStack stack) {

		return BaubleType.TRINKET;
	}

	@Override
	public void onWornTick(ItemStack stack, EntityLivingBase player) {

		World world = player.world;
		if (ServerHelper.isClientWorld(world) || world.getTotalWorldTime() % CoreProps.TIME_CONSTANT != 0) {
			return;
		}
		if (CoreUtils.isFakePlayer(player) || getMode(stack) == 0) {
			return;
		}
		FluidStack fluid = getFluid(stack);
		if (fluid != null && fluid.amount >= MB_PER_CYCLE) {
			boolean used = false;
			for (PotionEffect effect : PotionUtils.getEffectsFromTag(fluid.tag)) {
				PotionEffect active = player.getActivePotionMap().get(effect.getPotion());
				if (active != null && active.getDuration() >= 40) {
					continue;
				}
				if (effect.getPotion().isInstant()) {
					effect.getPotion().affectEntity(null, null, player, effect.getAmplifier(), 0.5D);
				} else {
					PotionEffect potion = new PotionEffect(effect.getPotion(), effect.getDuration() / 4, effect.getAmplifier(), effect.getIsAmbient(), false);
					player.addPotionEffect(potion);
				}
				used = true;
			}
			if (used) {
				drain(stack, MB_PER_CYCLE, true);
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

		int mode = getMode(stack);
		if (mode == 1) {
			player.world.playSound(null, player.getPosition(), SoundEvents.ENTITY_PLAYER_SPLASH, SoundCategory.PLAYERS, 0.4F, 1.0F);
		} else {
			player.world.playSound(null, player.getPosition(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.2F, 0.6F);
		}
		ChatHelper.sendIndexedChatMessageToPlayer(player, new TextComponentTranslation("info.thermalinnovation.injector.c." + mode));
	}

	/* IModelRegister */
	@Override
	@SideOnly (Side.CLIENT)
	public void registerModels() {

		ModelLoader.setCustomMeshDefinition(this, stack -> new ModelResourceLocation(getRegistryName(), String.format("color0=%s,fill=%s,type=%s", ColorHelper.hasColor0(stack) ? 1 : 0, MathHelper.clamp(getFluidAmount(stack) > 0 ? 1 + getScaledFluidStored(stack, 7) : 0, 0, 7), typeMap.get(ItemHelper.getItemDamage(stack)).name)));

		for (Map.Entry<Integer, ItemEntry> entry : itemMap.entrySet()) {
			for (int color0 = 0; color0 < 2; color0++) {
				for (int fill = 0; fill < 8; fill++) {
					ModelBakery.registerItemVariants(this, new ModelResourceLocation(getRegistryName(), String.format("color0=%s,fill=%s,type=%s", color0, fill, entry.getValue().name)));
				}
			}
		}
	}

	/* IInitializer */
	@Override
	public boolean preInit() {

		ForgeRegistries.ITEMS.register(setRegistryName("injector"));
		ThermalInnovation.proxy.addIModelRegister(this);

		config();

		injectorBasic = addEntryItem(0, "standard0", CAPACITY[0], EnumRarity.COMMON);
		injectorHardened = addEntryItem(1, "standard1", CAPACITY[1], EnumRarity.COMMON);
		injectorReinforced = addEntryItem(2, "standard2", CAPACITY[2], EnumRarity.UNCOMMON);
		injectorSignalum = addEntryItem(3, "standard3", CAPACITY[3], EnumRarity.UNCOMMON);
		injectorResonant = addEntryItem(4, "standard4", CAPACITY[4], EnumRarity.RARE);

		injectorCreative = addEntryItem(CREATIVE, "creative", CAPACITY[4], EnumRarity.EPIC);

		return true;
	}

	@Override
	public boolean initialize() {

		if (!enable) {
			return false;
		}
		// @formatter:off
		addShapedRecipe(injectorBasic,
				"BRB",
				"XBX",
				" I ",
				'B', Items.GLASS_BOTTLE,
				'I', "ingotSilver",
				'R', "dustGlowstone",
				'X', "ingotCopper"
		);
		addShapedUpgradeRecipe(injectorHardened,
				" R ",
				"IXI",
				"R R",
				'I', "ingotInvar",
				'R', "nuggetCopper",
				'X', injectorBasic
		);
		addShapedUpgradeRecipe(injectorReinforced,
				" R ",
				"IXI",
				"R R",
				'I', "ingotElectrum",
				'R', "nuggetInvar",
				'X', injectorHardened
		);
		addShapedUpgradeRecipe(injectorSignalum,
				" R ",
				"IXI",
				"R R",
				'I', "ingotSignalum",
				'R', "nuggetElectrum",
				'X', injectorReinforced
		);
		addShapedUpgradeRecipe(injectorResonant,
				" R ",
				"IXI",
				"R R",
				'I', "ingotEnderium",
				'R', "nuggetSignalum",
				'X', injectorSignalum
		);
		// @formatter:on

		addPotionFillRecipe(injectorBasic, injectorBasic, "cofh:potion");
		addPotionFillRecipe(injectorHardened, injectorHardened, "cofh:potion");
		addPotionFillRecipe(injectorReinforced, injectorReinforced, "cofh:potion");
		addPotionFillRecipe(injectorSignalum, injectorSignalum, "cofh:potion");
		addPotionFillRecipe(injectorResonant, injectorResonant, "cofh:potion");
		addPotionFillRecipe(injectorCreative, injectorCreative, "cofh:potion");

		addColorRecipe(injectorBasic, injectorBasic, "dye");
		addColorRecipe(injectorHardened, injectorHardened, "dye");
		addColorRecipe(injectorReinforced, injectorReinforced, "dye");
		addColorRecipe(injectorSignalum, injectorSignalum, "dye");
		addColorRecipe(injectorResonant, injectorResonant, "dye");

		addColorRemoveRecipe(injectorBasic, injectorBasic);
		addColorRemoveRecipe(injectorHardened, injectorHardened);
		addColorRemoveRecipe(injectorReinforced, injectorReinforced);
		addColorRemoveRecipe(injectorSignalum, injectorSignalum);
		addColorRemoveRecipe(injectorResonant, injectorResonant);
		return true;
	}

	private static void config() {

		String category = "Item.Injector";
		String comment;

		enable = ThermalInnovation.CONFIG.get(category, "Enable", true);

		int capacity = CAPACITY_BASE;
		comment = "Adjust this value to change the amount of Fluid (in mB) stored by a Basic Hypoinfuser. This base value will scale with item level.";
		capacity = ThermalInnovation.CONFIG.getConfiguration().getInt("BaseCapacity", category, capacity, capacity / 5, capacity * 5, comment);

		for (int i = 0; i < CAPACITY.length; i++) {
			CAPACITY[i] *= capacity;
		}
	}

	/* ENTRY */
	public class TypeEntry {

		public final String name;
		public final int capacity;

		TypeEntry(String name, int capacity) {

			this.name = name;
			this.capacity = capacity;
		}
	}

	private void addEntry(int metadata, String name, int capacity) {

		typeMap.put(metadata, new TypeEntry(name, capacity));
	}

	private ItemStack addEntryItem(int metadata, String name, int capacity, EnumRarity rarity) {

		addEntry(metadata, name, capacity);
		return addItem(metadata, name, rarity);
	}

	private static TIntObjectHashMap<TypeEntry> typeMap = new TIntObjectHashMap<>();

	public static final int CAPACITY_BASE = 2000;
	public static final int MB_PER_CYCLE = 50;
	public static final int MB_PER_USE = 250;

	public static final int[] CAPACITY = { 1, 3, 6, 10, 15 };

	public static boolean enable = true;

	/* REFERENCES */
	public static ItemStack injectorBasic;
	public static ItemStack injectorHardened;
	public static ItemStack injectorReinforced;
	public static ItemStack injectorSignalum;
	public static ItemStack injectorResonant;

	public static ItemStack injectorCreative;

}
