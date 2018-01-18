package cofh.thermalinnovation.item;

import cofh.api.item.IMultiModeItem;
import cofh.core.init.CoreEnchantments;
import cofh.core.item.IAOEBreakItem;
import cofh.core.item.ItemMultiRF;
import cofh.core.key.KeyBindingItemMultiMode;
import cofh.core.util.RayTracer;
import cofh.core.util.core.IInitializer;
import cofh.core.util.helpers.*;
import cofh.thermalinnovation.ThermalInnovation;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.THashSet;
import gnu.trove.set.hash.TLinkedHashSet;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ItemDrill extends ItemMultiRF implements IInitializer, IMultiModeItem, IAOEBreakItem {

	protected final TLinkedHashSet<String> toolClasses = new TLinkedHashSet<>();
	protected final Set<String> immutableClasses = java.util.Collections.unmodifiableSet(toolClasses);

	protected THashSet<Block> effectiveBlocks = new THashSet<>();
	protected THashSet<Material> effectiveMaterials = new THashSet<>();

	public ItemDrill() {

		super("thermalinnovation");

		setUnlocalizedName("drill");
		setCreativeTab(ThermalInnovation.tabCommon);

		setHasSubtypes(true);
		setMaxStackSize(1);
		setNoRepair();

		toolClasses.add("pickaxe");
		toolClasses.add("shovel");
		toolClasses.add("drill");

		effectiveBlocks.addAll(ItemPickaxe.EFFECTIVE_ON);
		effectiveBlocks.addAll(ItemSpade.EFFECTIVE_ON);

		effectiveMaterials.add(Material.IRON);
		effectiveMaterials.add(Material.ANVIL);
		effectiveMaterials.add(Material.ROCK);
		effectiveMaterials.add(Material.ICE);
		effectiveMaterials.add(Material.PACKED_ICE);
		effectiveMaterials.add(Material.GLASS);
		effectiveMaterials.add(Material.REDSTONE_LIGHT);

		effectiveMaterials.add(Material.GROUND);
		effectiveMaterials.add(Material.GRASS);
		effectiveMaterials.add(Material.SAND);
		effectiveMaterials.add(Material.SNOW);
		effectiveMaterials.add(Material.CRAFTED_SNOW);
		effectiveMaterials.add(Material.CLAY);
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

		tooltip.add(StringHelper.getInfoText("info.thermalinnovation.drill.a.0"));
		tooltip.add(StringHelper.localize("info.cofh.area") + ": " + radius + "x" + radius);

		if (getNumModes(stack) > 1) {
			tooltip.add(StringHelper.localizeFormat("info.thermalinnovation.drill.b.0", StringHelper.getKeyName(KeyBindingItemMultiMode.INSTANCE.getKey())));
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
					items.add(EnergyHelper.setDefaultEnergyTag(new ItemStack(this, 1, metadata), 0));
					items.add(EnergyHelper.setDefaultEnergyTag(new ItemStack(this, 1, metadata), getBaseCapacity(metadata)));
				} else {
					items.add(EnergyHelper.setDefaultEnergyTag(new ItemStack(this, 1, metadata), getBaseCapacity(metadata)));
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

		return enchantment.type.canEnchantItem(stack.getItem()) || enchantment.canApply(new ItemStack(Items.IRON_PICKAXE)) || enchantment.canApply(new ItemStack(Items.IRON_SHOVEL));
	}

	@Override
	public boolean canHarvestBlock(IBlockState state, ItemStack stack) {

		return getHarvestLevel(stack) >= state.getBlock().getHarvestLevel(state) && getDestroySpeed(stack, state) > 1.0F;
	}

	@Override
	public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {

		useEnergy(stack, 2, true);
		return true;
	}

	@Override
	public boolean isEnchantable(ItemStack stack) {

		return true;
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
			BlockPos adjPos;
			IBlockState adjState;
			float strength;
			int count = 0;

			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			int radius = getMode(stack);

			switch (traceResult.sideHit) {
				case DOWN:
				case UP:
					for (int i = x - radius; i <= x + radius; i++) {
						for (int k = z - radius; k <= z + radius; k++) {
							adjPos = new BlockPos(i, y, k);
							adjState = world.getBlockState(adjPos);
							strength = adjState.getPlayerRelativeBlockHardness(player, world, adjPos);
							if (strength > 0F && refStrength / strength <= 10F) {
								if (harvestBlock(world, adjPos, player)) {
									count++;
								}
							}
						}
					}
					break;
				case NORTH:
				case SOUTH:
					for (int i = x - radius; i <= x + radius; i++) {
						for (int j = y - radius; j <= y + radius; j++) {
							adjPos = new BlockPos(i, j, z);
							adjState = world.getBlockState(adjPos);
							strength = adjState.getPlayerRelativeBlockHardness(player, world, adjPos);
							if (strength > 0F && refStrength / strength <= 10F) {
								if (harvestBlock(world, adjPos, player)) {
									count++;
								}
							}
						}
					}
					break;
				case WEST:
				case EAST:
					for (int j = y - radius; j <= y + radius; j++) {
						for (int k = z - radius; k <= z + radius; k++) {
							adjPos = new BlockPos(x, j, k);
							adjState = world.getBlockState(adjPos);
							strength = adjState.getPlayerRelativeBlockHardness(player, world, adjPos);
							if (strength > 0F && refStrength / strength <= 10F) {
								if (harvestBlock(world, adjPos, player)) {
									count++;
								}
							}
						}
					}
					break;
			}
			if (count > 0 && !player.capabilities.isCreativeMode) {
				useEnergy(stack, count, false);
			}
		}
		return true;
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
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {

		return !oldStack.equals(newStack) && (slotChanged || getEnergyStored(oldStack) > 0 != getEnergyStored(newStack) > 0 || isActive(oldStack) != isActive(newStack));
	}

	@Override
	public int getHarvestLevel(ItemStack stack, String toolClass, @Nullable EntityPlayer player, @Nullable IBlockState blockState) {

		if (!getToolClasses(stack).contains(toolClass)) {
			return -1;
		}
		return getHarvestLevel(stack);
	}

	@Override
	public int getItemEnchantability(ItemStack stack) {

		return getEnchantability(stack);
	}

	@Override
	public float getDestroySpeed(ItemStack stack, IBlockState state) {

		if (getEnergyStored(stack) < ENERGY_PER_USE) {
			return 1.0F;
		}
		return (effectiveMaterials.contains(state.getMaterial()) || effectiveBlocks.contains(state)) ? getEfficiency(stack) - 2.0F * getMode(stack) : 1.0F;
	}

	@Override
	public Set<String> getToolClasses(ItemStack stack) {

		return toolClasses.isEmpty() ? super.getToolClasses(stack) : immutableClasses;
	}

	@Override
	public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack) {

		Multimap<String, AttributeModifier> multimap = HashMultimap.create();

		if (slot == EntityEquipmentSlot.MAINHAND) {
			if (getEnergyStored(stack) >= ENERGY_PER_USE) {
				multimap.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(ATTACK_SPEED_MODIFIER, "Tool modifier", -2.8F, 0));
				multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Tool modifier", getAttackDamage(stack), 0));
			} else {
				multimap.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(ATTACK_SPEED_MODIFIER, "Tool modifier", -2.8F, 0));
				multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Tool modifier", 1, 0));
			}
		}
		return multimap;
	}

	/* HELPERS */
	protected boolean harvestBlock(World world, BlockPos pos, EntityPlayer player) {

		if (world.isAirBlock(pos)) {
			return false;
		}
		EntityPlayerMP playerMP = null;
		if (player instanceof EntityPlayerMP) {
			playerMP = (EntityPlayerMP) player;
		}
		IBlockState state = world.getBlockState(pos);
		Block block = state.getBlock();

		if (!ForgeHooks.canHarvestBlock(block, player, world, pos)) {
			return false;
		}
		// send the blockbreak event
		int xpToDrop = 0;
		if (playerMP != null) {
			xpToDrop = ForgeHooks.onBlockBreakEvent(world, playerMP.interactionManager.getGameType(), playerMP, pos);
			if (xpToDrop == -1) {
				return false;
			}
		}
		// Creative Mode
		if (player.capabilities.isCreativeMode) {
			if (!world.isRemote) {
				if (block.removedByPlayer(state, world, pos, player, false)) {
					block.onBlockDestroyedByPlayer(world, pos, state);
				}
				// always send block update to client
				playerMP.connection.sendPacket(new SPacketBlockChange(world, pos));
			} else {
				if (block.removedByPlayer(state, world, pos, player, false)) {
					block.onBlockDestroyedByPlayer(world, pos, state);
				}
				Minecraft.getMinecraft().getConnection().sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, Minecraft.getMinecraft().objectMouseOver.sideHit));
			}
		}
		// Otherwise
		if (!world.isRemote) {
			if (block.removedByPlayer(state, world, pos, player, true)) {
				block.onBlockDestroyedByPlayer(world, pos, state);
				block.harvestBlock(world, player, pos, state, world.getTileEntity(pos), player.getHeldItemMainhand());
				if (xpToDrop > 0) {
					block.dropXpOnBlockBreak(world, pos, xpToDrop);
				}
			}
			// always send block update to client
			playerMP.connection.sendPacket(new SPacketBlockChange(world, pos));
		} else {
			if (block.removedByPlayer(state, world, pos, player, true)) {
				block.onBlockDestroyedByPlayer(world, pos, state);
			}
			Minecraft.getMinecraft().getConnection().sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, Minecraft.getMinecraft().objectMouseOver.sideHit));
		}
		return true;
	}

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

	public int getEnchantability(ItemStack stack) {

		if (!typeMap.containsKey(ItemHelper.getItemDamage(stack))) {
			return 0;
		}
		return typeMap.get(ItemHelper.getItemDamage(stack)).enchantability;
	}

	public int getMaxRadius(int metadata) {

		if (!typeMap.containsKey(metadata)) {
			return 0;
		}
		return typeMap.get(metadata).maxRadius;
	}

	public int getHarvestLevel(ItemStack stack) {

		if (!typeMap.containsKey(ItemHelper.getItemDamage(stack))) {
			return -1;
		}
		return typeMap.get(ItemHelper.getItemDamage(stack)).harvestLevel;
	}

	public float getAttackDamage(ItemStack stack) {

		if (!typeMap.containsKey(ItemHelper.getItemDamage(stack))) {
			return 0;
		}
		if (getEnergyStored(stack) < ENERGY_PER_USE * 2) {
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

	public boolean isActive(ItemStack stack) {

		return stack.getTagCompound() != null && stack.getTagCompound().hasKey("Active");
	}

	public void setActive(ItemStack stack, EntityLivingBase living) {

		stack.getTagCompound().setLong("Active", living.world.getTotalWorldTime() + 10);
	}

	/* IModelRegister */
	//	@Override
	//	@SideOnly (Side.CLIENT)
	//	public void registerModels() {
	//
	//		ModelLoader.setCustomMeshDefinition(this, stack -> new ModelResourceLocation(getRegistryName(), String.format("type=%s,water=%s", typeMap.get(ItemHelper.getItemDamage(stack)).name, this.getEnergyStored(stack) > 0 ? isActive(stack) ? "tipped" : "level" : "empty")));
	//
	//		String[] waterStates = { "level", "tipped", "empty" };
	//
	//		for (Map.Entry<Integer, ItemEntry> entry : itemMap.entrySet()) {
	//			for (int i = 0; i < 3; i++) {
	//				ModelBakery.registerItemVariants(this, new ModelResourceLocation(getRegistryName(), String.format("type=%s,water=%s", entry.getValue().name, waterStates[i])));
	//			}
	//		}
	//	}

	/* IMultiModeItem */
	@Override
	public int getNumModes(ItemStack stack) {

		return getMaxRadius(ItemHelper.getItemDamage(stack)) + 1;
	}

	@Override
	public void onModeChange(EntityPlayer player, ItemStack stack) {

		player.world.playSound(null, player.getPosition(), SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.PLAYERS, 0.6F, 1.0F - 0.1F * getMode(stack));

		int radius = getMode(stack) * 2 + 1;
		ChatHelper.sendIndexedChatMessageToPlayer(player, new TextComponentString(StringHelper.localize("info.cofh.area") + ": " + radius + "x" + radius));
	}

	/* IAOEBreakItem */
	@Override
	public ImmutableList<BlockPos> getAOEBlocks(ItemStack stack, BlockPos pos, EntityPlayer player) {

		ArrayList<BlockPos> area = new ArrayList<>();
		World world = player.getEntityWorld();
		int radius = getMode(stack);

		RayTraceResult traceResult = RayTracer.retrace(player, false);
		if (traceResult == null || traceResult.sideHit == null || !canHarvestBlock(world.getBlockState(pos), stack)) {
			return ImmutableList.copyOf(area);
		}
		BlockPos harvestPos;

		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();

		switch (traceResult.sideHit) {
			case DOWN:
			case UP:
				for (int i = x - radius; i <= x + radius; i++) {
					for (int k = z - radius; k <= z + radius; k++) {
						if (i == x && k == z) {
							continue;
						}
						harvestPos = new BlockPos(i, y, k);
						if (canHarvestBlock(world.getBlockState(harvestPos), stack)) {
							area.add(harvestPos);
						}
					}
				}
				break;
			case NORTH:
			case SOUTH:
				for (int i = x - radius; i <= x + radius; i++) {
					for (int j = y - radius; j <= y + radius; j++) {
						if (i == x && j == y) {
							continue;
						}
						harvestPos = new BlockPos(i, j, z);
						if (canHarvestBlock(world.getBlockState(harvestPos), stack)) {
							area.add(harvestPos);
						}
					}
				}
				break;
			case WEST:
			case EAST:
				for (int j = y - radius; j <= y + radius; j++) {
					for (int k = z - radius; k <= z + radius; k++) {
						if (j == y && k == z) {
							continue;
						}
						harvestPos = new BlockPos(x, j, k);
						if (canHarvestBlock(world.getBlockState(harvestPos), stack)) {
							area.add(harvestPos);
						}
					}
				}
				break;
		}
		return ImmutableList.copyOf(area);
	}

	/* IInitializer */
	@Override
	public boolean initialize() {

		config();

		drillBasic = addEntryItem(0, "standard0", EnumRarity.COMMON);
		drillHardened = addEntryItem(1, "standard1", EnumRarity.COMMON);
		drillReinforced = addEntryItem(2, "standard2", EnumRarity.UNCOMMON);
		drillSignalum = addEntryItem(3, "standard3", EnumRarity.UNCOMMON);
		drillResonant = addEntryItem(4, "standard4", EnumRarity.RARE);

		drillCreative = addEntryItem(CREATIVE, "creative", HARVEST_LEVEL[4], EFFICIENCY[4], ATTACK_DAMAGE[4], ENCHANTABILITY[4], CAPACITY[4], 0, MAX_RADIUS[4], EnumRarity.EPIC);

		ThermalInnovation.proxy.addIModelRegister(this);

		return true;
	}

	@Override
	public boolean register() {

		if (!enable) {
			return false;
		}
		// @formatter:off

//		addShapedRecipe(drillBasic,
//				" R ",
//				"IXI",
//				"RYR",
//				'I', "ingotLead",
//				'R', "dustRedstone",
//				'X', "ingotCopper",
//				'Y', "dustSulfur"
//		);

		// @formatter:on

		return true;
	}

	private static void config() {

		String category = "Item.Drill";
		String comment;

		enable = ThermalInnovation.CONFIG.get(category, "Enable", true);

		int capacity = CAPACITY_BASE;
		comment = "Adjust this value to change the amount of Energy (in RF) stored by a Basic Flux Drill. This base value will scale with item level.";
		capacity = ThermalInnovation.CONFIG.getConfiguration().getInt("BaseCapacity", category, capacity, capacity / 5, capacity * 5, comment);

		int xfer = XFER_BASE;
		comment = "Adjust this value to change the amount of Energy (in RF/t) that can be received by a Basic Flux Drill. This base value will scale with item level.";
		xfer = ThermalInnovation.CONFIG.getConfiguration().getInt("BaseReceive", category, xfer, xfer / 10, xfer * 10, comment);

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
		public final int maxRadius;

		TypeEntry(String name, int harvestLevel, float efficiency, float attackDamage, int enchantability, int capacity, int recv, int maxRadius) {

			this.name = name;
			this.harvestLevel = harvestLevel;
			this.efficiency = efficiency;
			this.attackDamage = attackDamage;
			this.enchantability = enchantability;
			this.capacity = capacity;
			this.recv = recv;
			this.maxRadius = maxRadius;
		}
	}

	private void addEntry(int metadata, String name, int harvestLevel, float efficiency, float attackDamage, int enchantability, int capacity, int xfer, int maxRadius) {

		typeMap.put(metadata, new TypeEntry(name, harvestLevel, efficiency, attackDamage, enchantability, capacity, xfer, maxRadius));
	}

	private ItemStack addEntryItem(int metadata, String name, EnumRarity rarity) {

		addEntry(metadata, name, HARVEST_LEVEL[metadata], EFFICIENCY[metadata], ATTACK_DAMAGE[metadata], ENCHANTABILITY[metadata], CAPACITY[metadata], XFER[metadata], MAX_RADIUS[metadata]);
		return addItem(metadata, name, rarity);
	}

	private ItemStack addEntryItem(int metadata, String name, int harvestLevel, float efficiency, float attackDamage, int enchantability, int capacity, int xfer, int maxRadius, EnumRarity rarity) {

		addEntry(metadata, name, harvestLevel, efficiency, attackDamage, enchantability, capacity, xfer, maxRadius);
		return addItem(metadata, name, rarity);
	}

	private static TIntObjectHashMap<TypeEntry> typeMap = new TIntObjectHashMap<>();

	public static final int CAPACITY_BASE = 20000;
	public static final int XFER_BASE = 1000;
	public static final int ENERGY_PER_USE = 200;

	public static final int[] HARVEST_LEVEL = { 2, 2, 3, 3, 4 };
	public static final float[] EFFICIENCY = { 6.0F, 7.0F, 8.0F, 9.0F, 10.0F };
	public static final float[] ATTACK_DAMAGE = { 2.0F, 2.5F, 3.0F, 3.5F, 4.0F };
	public static final int[] ENCHANTABILITY = { 10, 10, 15, 15, 20 };

	public static final int[] CAPACITY = { 1, 3, 6, 10, 15 };
	public static final int[] XFER = { 1, 4, 9, 16, 25 };
	public static final int[] MAX_RADIUS = { 0, 1, 1, 2, 2 };

	public static boolean enable = true;

	/* REFERENCES */
	public static ItemStack drillBasic;
	public static ItemStack drillHardened;
	public static ItemStack drillReinforced;
	public static ItemStack drillSignalum;
	public static ItemStack drillResonant;

	public static ItemStack drillCreative;

}
