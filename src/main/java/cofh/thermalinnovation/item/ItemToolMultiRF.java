package cofh.thermalinnovation.item;

import cofh.core.item.ItemMultiRF;
import cofh.core.util.helpers.EnergyHelper;
import cofh.core.util.helpers.ItemHelper;
import cofh.core.util.helpers.MathHelper;
import gnu.trove.set.hash.THashSet;
import gnu.trove.set.hash.TLinkedHashSet;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Set;

public abstract class ItemToolMultiRF extends ItemMultiRF {

	protected final TLinkedHashSet<String> toolClasses = new TLinkedHashSet<>();
	protected final Set<String> immutableClasses = java.util.Collections.unmodifiableSet(toolClasses);

	protected THashSet<Block> effectiveBlocks = new THashSet<>();
	protected THashSet<Material> effectiveMaterials = new THashSet<>();

	protected int energyPerUse = 200;

	public ItemToolMultiRF(String modName) {

		super(modName);

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
	public boolean canHarvestBlock(IBlockState state, ItemStack stack) {

		return getHarvestLevel(stack) >= state.getBlock().getHarvestLevel(state) && getDestroySpeed(stack, state) > 1.0F;
	}

	@Override
	public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {

		useEnergy(stack, 2, true);
		return true;
	}

	@Override
	public int getHarvestLevel(ItemStack stack, String toolClass, @Nullable EntityPlayer player, @Nullable IBlockState blockState) {

		if (!getToolClasses(stack).contains(toolClass)) {
			return -1;
		}
		return getHarvestLevel(stack);
	}

	@Override
	public Set<String> getToolClasses(ItemStack stack) {

		return toolClasses.isEmpty() ? super.getToolClasses(stack) : immutableClasses;
	}

	/* HELPERS */
	protected int getHarvestLevel(ItemStack stack) {

		return -1;
	}

	protected int useEnergy(ItemStack stack, int count, boolean simulate) {

		if (ItemHelper.getItemDamage(stack) == CREATIVE) {
			return 0;
		}
		int unbreakingLevel = MathHelper.clamp(EnchantmentHelper.getEnchantmentLevel(Enchantments.UNBREAKING, stack), 0, 10);
		if (MathHelper.RANDOM.nextInt(2 + unbreakingLevel) >= 2) {
			return 0;
		}
		return extractEnergy(stack, count * energyPerUse, simulate);
	}

	protected boolean isActive(ItemStack stack) {

		return stack.getTagCompound() != null && stack.getTagCompound().hasKey("Active");
	}

	protected void setActive(ItemStack stack, EntityLivingBase living) {

		stack.getTagCompound().setLong("Active", living.world.getTotalWorldTime() + 20);
	}

	protected int breakTunnel2(EntityPlayer player, World world, BlockPos pos, RayTraceResult traceResult, float refStrength) {

		BlockPos adjPos;
		IBlockState adjState;
		float strength;

		switch (traceResult.sideHit) {
			case DOWN:
			case UP:
				break;
			default:
				adjPos = new BlockPos(pos.down());
				adjState = world.getBlockState(adjPos);
				strength = adjState.getPlayerRelativeBlockHardness(player, world, adjPos);
				if (strength > 0F && refStrength / strength <= 10F) {
					if (harvestBlock(world, adjPos, player)) {
						return 1;
					}
				}
		}
		return 0;
	}

	protected int breakArea3(EntityPlayer player, World world, BlockPos pos, RayTraceResult traceResult, float refStrength) {

		BlockPos adjPos;
		IBlockState adjState;
		float strength;
		int count = 0;

		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		int radius = 1;

		switch (traceResult.sideHit) {
			case DOWN:
			case UP:
				for (int i = x - radius; i <= x + radius; i++) {
					for (int k = z - radius; k <= z + radius; k++) {
						if (i == x && k == z) {
							continue;
						}
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
						if (i == x && j == y) {
							continue;
						}
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
						if (j == y && k == z) {
							continue;
						}
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
		return count;
	}

	protected int breakCube3(EntityPlayer player, World world, BlockPos pos, RayTraceResult traceResult, float refStrength) {

		BlockPos adjPos;
		IBlockState adjState;
		float strength;
		int count = 0;

		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		int radius = 1;
		int depth = 2;
		int depth_min = depth;
		int depth_max = 0;

		switch (traceResult.sideHit) {
			case DOWN:
				depth_min = 0;
				depth_max = depth;
			case UP:
				for (int i = x - radius; i <= x + radius; i++) {
					for (int j = y - depth_min; j <= y + depth_max; j++) {
						for (int k = z - radius; k <= z + radius; k++) {
							if (i == x && j == y && k == z) {
								continue;
							}
							adjPos = new BlockPos(i, j, k);
							adjState = world.getBlockState(adjPos);
							strength = adjState.getPlayerRelativeBlockHardness(player, world, adjPos);
							if (strength > 0F && refStrength / strength <= 10F) {
								if (harvestBlock(world, adjPos, player)) {
									count++;
								}
							}
						}
					}
				}
				break;
			case NORTH:
				depth_min = 0;
				depth_max = depth;
			case SOUTH:
				for (int i = x - radius; i <= x + radius; i++) {
					for (int j = y - radius; j <= y + radius; j++) {
						for (int k = z - depth_min; k <= z + depth_max; k++) {
							if (i == x && j == y && k == z) {
								continue;
							}
							adjPos = new BlockPos(i, j, k);
							adjState = world.getBlockState(adjPos);
							strength = adjState.getPlayerRelativeBlockHardness(player, world, adjPos);
							if (strength > 0F && refStrength / strength <= 10F) {
								if (harvestBlock(world, adjPos, player)) {
									count++;
								}
							}
						}
					}
				}
				break;
			case WEST:
				depth_min = 0;
				depth_max = depth;
			case EAST:
				for (int i = x - depth_min; i <= x + depth_max; i++) {
					for (int j = y - radius; j <= y + radius; j++) {
						for (int k = z - radius; k <= z + radius; k++) {
							if (i == x && j == y && k == z) {
								continue;
							}
							adjPos = new BlockPos(i, j, k);
							adjState = world.getBlockState(adjPos);
							strength = adjState.getPlayerRelativeBlockHardness(player, world, adjPos);
							if (strength > 0F && refStrength / strength <= 10F) {
								if (harvestBlock(world, adjPos, player)) {
									count++;
								}
							}
						}
					}
				}
				break;
		}
		return count;
	}

	protected int breakArea5(EntityPlayer player, World world, BlockPos pos, RayTraceResult traceResult, float refStrength) {

		BlockPos adjPos;
		IBlockState adjState;
		float strength;
		int count = 0;

		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		int radius = 2;

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
				int posY = y;
				y += 1;     // Offset for 5x5
				for (int i = x - radius; i <= x + radius; i++) {
					for (int j = y - radius; j <= y + radius; j++) {
						if (i == x && j == posY) {
							continue;
						}
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
				posY = y;
				y += 1;     // Offset for 5x5
				for (int j = y - radius; j <= y + radius; j++) {
					for (int k = z - radius; k <= z + radius; k++) {
						if (j == posY && k == z) {
							continue;
						}
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
		return count;
	}

	protected void getAOEBlocksTunnel2(ItemStack stack, World world, BlockPos pos, RayTraceResult traceResult, ArrayList<BlockPos> area) {

		BlockPos harvestPos;

		switch (traceResult.sideHit) {
			case DOWN:
			case UP:
				break;
			default:
				harvestPos = new BlockPos(pos.down());
				if (canHarvestBlock(world.getBlockState(harvestPos), stack)) {
					area.add(harvestPos);
				}
				break;
		}
	}

	protected void getAOEBlocksArea3(ItemStack stack, World world, BlockPos pos, RayTraceResult traceResult, ArrayList<BlockPos> area) {

		BlockPos harvestPos;

		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		int radius = 1;

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
	}

	protected void getAOEBlocksCube3(ItemStack stack, World world, BlockPos pos, RayTraceResult traceResult, ArrayList<BlockPos> area) {

		BlockPos harvestPos;

		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		int radius = 1;
		int depth = 2;
		int depth_min = depth;
		int depth_max = 0;

		switch (traceResult.sideHit) {
			case DOWN:
				depth_min = 0;
				depth_max = depth;
			case UP:
				for (int i = x - radius; i <= x + radius; i++) {
					for (int j = y - depth_min; j <= y + depth_max; j++) {
						for (int k = z - radius; k <= z + radius; k++) {
							if (i == x && j == y && k == z) {
								continue;
							}
							harvestPos = new BlockPos(i, j, k);
							if (canHarvestBlock(world.getBlockState(harvestPos), stack)) {
								area.add(harvestPos);
							}
						}
					}
				}
				break;
			case NORTH:
				depth_min = 0;
				depth_max = depth;
			case SOUTH:
				for (int i = x - radius; i <= x + radius; i++) {
					for (int j = y - radius; j <= y + radius; j++) {
						for (int k = z - depth_min; k <= z + depth_max; k++) {
							if (i == x && j == y && k == z) {
								continue;
							}
							harvestPos = new BlockPos(i, j, k);
							if (canHarvestBlock(world.getBlockState(harvestPos), stack)) {
								area.add(harvestPos);
							}
						}
					}
				}
				break;
			case WEST:
				depth_min = 0;
				depth_max = depth;
			case EAST:
				for (int i = x - depth_min; i <= x + depth_max; i++) {
					for (int j = y - radius; j <= y + radius; j++) {
						for (int k = z - radius; k <= z + radius; k++) {
							if (i == x && j == y && k == z) {
								continue;
							}
							harvestPos = new BlockPos(i, j, k);
							if (canHarvestBlock(world.getBlockState(harvestPos), stack)) {
								area.add(harvestPos);
							}
						}
					}
				}
				break;
		}
	}

	protected void getAOEBlocksArea5(ItemStack stack, World world, BlockPos pos, RayTraceResult traceResult, ArrayList<BlockPos> area) {

		BlockPos harvestPos;

		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		int radius = 2;

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
				int posY = y;
				y += 1;     // Offset for 5x5
				for (int i = x - radius; i <= x + radius; i++) {
					for (int j = y - radius; j <= y + radius; j++) {
						if (i == x && j == posY) {
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
				posY = y;
				y += 1;     // Offset for 5x5
				for (int j = y - radius; j <= y + radius; j++) {
					for (int k = z - radius; k <= z + radius; k++) {
						if (j == posY && k == z) {
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
	}

	public static final int CAPACITY_BASE = 40000;
	public static final int XFER_BASE = 1000;

	public static final int SINGLE = 0;
	public static final int TUNNEL2 = 1;
	public static final int AREA3 = 2;
	public static final int CUBE3 = 3;
	public static final int AREA5 = 4;

}