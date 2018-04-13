package cofh.thermalinnovation.item;

import cofh.api.item.IMultiModeItem;
import cofh.core.init.CoreEnchantments;
import cofh.core.item.IAOEBreakItem;
import cofh.core.key.KeyBindingItemMultiMode;
import cofh.core.util.RayTracer;
import cofh.core.util.core.IInitializer;
import cofh.core.util.helpers.*;
import cofh.thermalfoundation.init.TFProps;
import cofh.thermalfoundation.item.ItemMaterial;
import cofh.thermalinnovation.ThermalInnovation;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static cofh.core.util.helpers.RecipeHelper.addShapedRecipe;

public class ItemSaw extends ItemToolMultiRF implements IInitializer, IMultiModeItem, IAOEBreakItem {

	public ItemSaw() {

		super("thermalinnovation");

		setUnlocalizedName("saw");
		setCreativeTab(ThermalInnovation.tabTools);

		toolClasses.add("axe");
		toolClasses.add("sword");
		toolClasses.add("saw");

		effectiveBlocks.addAll(ItemAxe.EFFECTIVE_ON);
		effectiveBlocks.add(Blocks.WEB);
		effectiveBlocks.add(Blocks.VINE);
		effectiveBlocks.add(Blocks.LEAVES);
		effectiveBlocks.add(Blocks.LEAVES2);

		effectiveMaterials.add(Material.WOOD);
		effectiveMaterials.add(Material.CACTUS);
		effectiveMaterials.add(Material.GOURD);
		effectiveMaterials.add(Material.LEAVES);
		effectiveMaterials.add(Material.PLANTS);
		effectiveMaterials.add(Material.VINE);
		effectiveMaterials.add(Material.WEB);
	}

	@Override
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {

		if (StringHelper.displayShiftForDetail && !StringHelper.isShiftKeyDown()) {
			tooltip.add(StringHelper.shiftForDetails());
		}
		if (!StringHelper.isShiftKeyDown()) {
			return;
		}
		tooltip.add(StringHelper.getInfoText("info.thermalinnovation.saw.a.0"));
		tooltip.add(StringHelper.localize("info.thermalinnovation.saw.c." + getMode(stack)));
		tooltip.add(StringHelper.localizeFormat("info.thermalinnovation.saw.b.0", StringHelper.getKeyName(KeyBindingItemMultiMode.INSTANCE.getKey())));

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
					if (TFProps.showEmptyItems) {
						items.add(setDefaultTag(new ItemStack(this, 1, metadata), 0));
					}
					if (TFProps.showFullItems) {
						items.add(setDefaultTag(new ItemStack(this, 1, metadata), getBaseCapacity(metadata)));
					}
				} else {
					if (TFProps.showCreativeItems) {
						items.add(setDefaultTag(new ItemStack(this, 1, metadata), getBaseCapacity(metadata)));
					}
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
	public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {

		return enchantment.type.canEnchantItem(stack.getItem()) || enchantment.canApply(new ItemStack(Items.IRON_AXE));
	}

	@Override
	public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase entity, EnumHand hand) {

		if (ServerHelper.isClientWorld(entity.world)) {
			return false;
		}
		if (entity instanceof IShearable) {
			IShearable target = (IShearable) entity;
			BlockPos pos = new BlockPos(entity.posX, entity.posY, entity.posZ);
			if (target.isShearable(stack, entity.world, pos)) {
				List<ItemStack> drops = target.onSheared(stack, entity.world, pos, EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, stack));
				entity.attackEntityFrom(DamageSource.causePlayerDamage(player), 1.0F);

				for (ItemStack drop : drops) {
					EntityItem ent = entity.entityDropItem(drop, 1.0F);
					ent.motionY += MathHelper.RANDOM.nextFloat() * 0.05F;
					ent.motionX += (MathHelper.RANDOM.nextFloat() - MathHelper.RANDOM.nextFloat()) * 0.1F;
					ent.motionZ += (MathHelper.RANDOM.nextFloat() - MathHelper.RANDOM.nextFloat()) * 0.1F;
				}
				if (!player.capabilities.isCreativeMode) {
					useEnergy(stack, 1, false);
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, EntityPlayer player) {

		World world = player.world;
		IBlockState state = world.getBlockState(pos);

		if (state.getBlockHardness(world, pos) == 0.0F) {
			return false;
		}
		if (!canHarvestBlock(state, stack)) {
			if (!player.capabilities.isCreativeMode) {
				useEnergy(stack, 1, false);
			}
			return false;
		}
		world.playEvent(2001, pos, Block.getStateId(state));

		float refStrength = state.getPlayerRelativeBlockHardness(player, world, pos);
		if (refStrength != 0.0F) {
			RayTraceResult traceResult = RayTracer.retrace(player, false);

			if (traceResult == null || traceResult.sideHit == null) {
				return false;
			}
			int count = 1;
			int mode = getMode(stack);

			switch (mode) {
				case SINGLE:
					break;
				case TUNNEL2:
					count += breakTunnel2(player, world, pos, traceResult, refStrength);
					break;
				case AREA3:
					count += breakArea3(player, world, pos, traceResult, refStrength);
					break;
				case CUBE3:
					count += breakCube3(player, world, pos, traceResult, refStrength);
					break;
				case AREA5:
					count += breakArea5(player, world, pos, traceResult, refStrength);
					break;
			}
			if (count > 0 && !player.capabilities.isCreativeMode) {
				useEnergy(stack, count, false);
			}
		}
		return false;
	}

	@Override
	public boolean onEntitySwing(EntityLivingBase entityLiving, ItemStack stack) {

		setActive(stack, entityLiving);
		return true;
	}

	@Override
	public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {

		return !oldStack.equals(newStack) && (getEnergyStored(oldStack) > 0 != getEnergyStored(newStack) > 0 || isActive(oldStack) != isActive(newStack));
	}

	@Override
	public int getItemEnchantability(ItemStack stack) {

		return getEnchantability(stack);
	}

	@Override
	public float getDestroySpeed(ItemStack stack, IBlockState state) {

		if (getEnergyStored(stack) < energyPerUse) {
			return 1.0F;
		}
		return (effectiveMaterials.contains(state.getMaterial()) || effectiveBlocks.contains(state)) ? getEfficiency(stack) - MODE_EFF[getMode(stack)] : 1.0F;
	}

	@Override
	public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack) {

		Multimap<String, AttributeModifier> multimap = HashMultimap.create();

		if (slot == EntityEquipmentSlot.MAINHAND) {
			if (getEnergyStored(stack) >= energyPerUse) {
				multimap.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(ATTACK_SPEED_MODIFIER, "Tool modifier", -2.0F, 0));
				multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Tool modifier", getAttackDamage(stack), 0));
			} else {
				multimap.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(ATTACK_SPEED_MODIFIER, "Tool modifier", -3.2F, 0));
				multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Tool modifier", 0, 0));
			}
		}
		return multimap;
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

	public int getEnchantability(ItemStack stack) {

		if (!typeMap.containsKey(ItemHelper.getItemDamage(stack))) {
			return 0;
		}
		return typeMap.get(ItemHelper.getItemDamage(stack)).enchantability;
	}

	public int getHarvestLevel(ItemStack stack) {

		if (!typeMap.containsKey(ItemHelper.getItemDamage(stack)) || getEnergyStored(stack) <= energyPerUse) {
			return -1;
		}
		return typeMap.get(ItemHelper.getItemDamage(stack)).harvestLevel;
	}

	public float getAttackDamage(ItemStack stack) {

		if (!typeMap.containsKey(ItemHelper.getItemDamage(stack))) {
			return 0;
		}
		if (getEnergyStored(stack) < energyPerUse * 2) {
			return 0;
		}
		return typeMap.get(ItemHelper.getItemDamage(stack)).attackDamage;
	}

	public float getEfficiency(ItemStack stack) {

		if (!typeMap.containsKey(ItemHelper.getItemDamage(stack))) {
			return 0;
		}
		return typeMap.get(ItemHelper.getItemDamage(stack)).efficiency;
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
	public int getNumModes(ItemStack stack) {

		if (!typeMap.containsKey(ItemHelper.getItemDamage(stack))) {
			return 0;
		}
		return typeMap.get(ItemHelper.getItemDamage(stack)).numModes;
	}

	@Override
	public void onModeChange(EntityPlayer player, ItemStack stack) {

		player.world.playSound(null, player.getPosition(), SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.PLAYERS, 0.6F, 1.0F - 0.1F * getMode(stack));
		ChatHelper.sendIndexedChatMessageToPlayer(player, new TextComponentTranslation("info.thermalinnovation.saw.c." + getMode(stack)));
	}

	/* IEnchantableItem */
	@Override
	public boolean canEnchant(ItemStack stack, Enchantment enchantment) {

		return super.canEnchant(stack, enchantment) || enchantment == CoreEnchantments.insight || enchantment == CoreEnchantments.smelting || enchantment == CoreEnchantments.vorpal;
	}

	/* IAOEBreakItem */
	@Override
	public ImmutableList<BlockPos> getAOEBlocks(ItemStack stack, BlockPos pos, EntityPlayer player) {

		ArrayList<BlockPos> area = new ArrayList<>();
		World world = player.getEntityWorld();
		int mode = getMode(stack);

		RayTraceResult traceResult = RayTracer.retrace(player, false);
		if (traceResult == null || traceResult.sideHit == null || !canHarvestBlock(world.getBlockState(pos), stack)) {
			return ImmutableList.copyOf(area);
		}
		switch (mode) {
			case SINGLE:
				break;
			case TUNNEL2:
				getAOEBlocksTunnel2(stack, world, pos, traceResult, area);
				break;
			case AREA3:
				getAOEBlocksArea3(stack, world, pos, traceResult, area);
				break;
			case CUBE3:
				getAOEBlocksCube3(stack, world, pos, traceResult, area);
				break;
			case AREA5:
				getAOEBlocksArea5(stack, world, pos, traceResult, area);
				break;
		}
		return ImmutableList.copyOf(area);
	}

	/* IInitializer */
	@Override
	public boolean initialize() {

		config();

		sawBasic = addEntryItem(0, "standard0", EnumRarity.COMMON);
		sawHardened = addEntryItem(1, "standard1", EnumRarity.COMMON);
		sawReinforced = addEntryItem(2, "standard2", EnumRarity.UNCOMMON);
		sawSignalum = addEntryItem(3, "standard3", EnumRarity.UNCOMMON);
		sawResonant = addEntryItem(4, "standard4", EnumRarity.RARE);

		sawCreative = addEntryItem(CREATIVE, "creative", HARVEST_LEVEL[4], EFFICIENCY[4], ATTACK_DAMAGE[4], ENCHANTABILITY[4], CAPACITY[4], 0, NUM_MODES[4], EnumRarity.EPIC);

		ThermalInnovation.proxy.addIModelRegister(this);

		return true;
	}

	@Override
	public boolean register() {

		if (!enable) {
			return false;
		}
		// @formatter:off
		addShapedRecipe(sawBasic,
				" X ",
				"ICI",
				"YGY",
				'C', ItemMaterial.partToolCasing,
				'G', "gearLead",
				'I', "ingotLead",
				'X', ItemMaterial.partSawBlade,
				'Y', "ingotTin"
		);
		// @formatter:on
		return true;
	}

	private static void config() {

		String category = "Item.Saw";
		String comment;

		enable = ThermalInnovation.CONFIG.get(category, "Enable", true);

		int capacity = CAPACITY_BASE;
		comment = "Adjust this value to change the amount of Energy (in RF) stored by a Basic Fluxsaw. This base value will scale with item level.";
		capacity = ThermalInnovation.CONFIG.getConfiguration().getInt("BaseCapacity", category, capacity, CAPACITY_MIN, CAPACITY_MAX, comment);

		int xfer = XFER_BASE;
		comment = "Adjust this value to change the amount of Energy (in RF/t) that can be received by a Basic Fluxsaw. This base value will scale with item level.";
		xfer = ThermalInnovation.CONFIG.getConfiguration().getInt("BaseReceive", category, xfer, XFER_MIN, XFER_MAX, comment);

		for (int i = 0; i < CAPACITY.length; i++) {
			CAPACITY[i] *= capacity;
			XFER[i] *= xfer;
		}
	}

	/* ENTRY */
	public class TypeEntry {

		public final String name;

		public final int harvestLevel;
		public final float efficiency;
		public final float attackDamage;
		public final int enchantability;

		public final int capacity;
		public final int recv;
		public final int numModes;

		TypeEntry(String name, int harvestLevel, float efficiency, float attackDamage, int enchantability, int capacity, int recv, int numModes) {

			this.name = name;
			this.harvestLevel = harvestLevel;
			this.efficiency = efficiency;
			this.attackDamage = attackDamage;
			this.enchantability = enchantability;
			this.capacity = capacity;
			this.recv = recv;
			this.numModes = numModes;
		}
	}

	private void addEntry(int metadata, String name, int harvestLevel, float efficiency, float attackDamage, int enchantability, int capacity, int xfer, int numModes) {

		typeMap.put(metadata, new TypeEntry(name, harvestLevel, efficiency, attackDamage, enchantability, capacity, xfer, numModes));
	}

	private ItemStack addEntryItem(int metadata, String name, EnumRarity rarity) {

		addEntry(metadata, name, HARVEST_LEVEL[metadata], EFFICIENCY[metadata], ATTACK_DAMAGE[metadata], ENCHANTABILITY[metadata], CAPACITY[metadata], XFER[metadata], NUM_MODES[metadata]);
		return addItem(metadata, name, rarity);
	}

	private ItemStack addEntryItem(int metadata, String name, int harvestLevel, float efficiency, float attackDamage, int enchantability, int capacity, int xfer, int numModes, EnumRarity rarity) {

		addEntry(metadata, name, harvestLevel, efficiency, attackDamage, enchantability, capacity, xfer, numModes);
		return addItem(metadata, name, rarity);
	}

	private static TIntObjectHashMap<TypeEntry> typeMap = new TIntObjectHashMap<>();

	public static final int[] HARVEST_LEVEL = { 2, 2, 3, 3, 4 };
	public static final float[] EFFICIENCY = { 6.0F, 7.5F, 9.0F, 10.5F, 12.0F };
	public static final float[] ATTACK_DAMAGE = { 4.0F, 4.5F, 5.0F, 5.5F, 6.0F };
	public static final int[] ENCHANTABILITY = { 10, 10, 15, 15, 20 };

	public static final int[] CAPACITY = { 1, 3, 6, 10, 15 };
	public static final int[] XFER = { 1, 4, 9, 16, 25 };
	public static final int[] NUM_MODES = { 2, 3, 3, 4, 5 };
	public static final float[] MODE_EFF = { 0, 2.0F, 4.0F, 8.0F, 8.0F };

	public static boolean enable = true;

	/* REFERENCES */
	public static ItemStack sawBasic;
	public static ItemStack sawHardened;
	public static ItemStack sawReinforced;
	public static ItemStack sawSignalum;
	public static ItemStack sawResonant;

	public static ItemStack sawCreative;

}
