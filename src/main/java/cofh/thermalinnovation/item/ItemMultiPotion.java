package cofh.thermalinnovation.item;

import cofh.api.fluid.IFluidContainerItem;
import cofh.api.item.IColorableItem;
import cofh.api.item.IMultiModeItem;
import cofh.core.init.CoreEnchantments;
import cofh.core.init.CoreProps;
import cofh.core.item.IEnchantableItem;
import cofh.core.item.ItemMulti;
import cofh.core.util.capabilities.FluidContainerItemWrapper;
import cofh.core.util.helpers.ItemHelper;
import cofh.core.util.helpers.MathHelper;
import cofh.core.util.helpers.StringHelper;
import cofh.thermalfoundation.fluid.FluidPotion;
import cofh.thermalfoundation.init.TFFluids;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public abstract class ItemMultiPotion extends ItemMulti implements IColorableItem, IEnchantableItem, IFluidContainerItem, IMultiModeItem {

	public ItemMultiPotion(String modName) {

		super(modName);

		setHasSubtypes(true);
		setMaxStackSize(1);
		setNoRepair();
	}

	@Override
	public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {

		return !EnumEnchantmentType.BREAKABLE.equals(enchantment.type) && super.canApplyAtEnchantingTable(stack, enchantment);
	}

	@Override
	public boolean isDamageable() {

		return true;
	}

	@Override
	public boolean isEnchantable(ItemStack stack) {

		return true;
	}

	@Override
	public boolean isFull3D() {

		return true;
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {

		return super.shouldCauseReequipAnimation(oldStack, newStack, slotChanged) && (slotChanged || !ItemHelper.areItemStacksEqualIgnoreTags(oldStack, newStack, CoreProps.FLUID));
	}

	@Override
	public int getRGBDurabilityForDisplay(ItemStack stack) {

		return colorMultiplier(stack, 1);
	}

	@Override
	public double getDurabilityForDisplay(ItemStack stack) {

		return MathHelper.clamp(1.0D - ((double) getFluidAmount(stack) / (double) getCapacity(stack)), 0.0D, 1.0D);
	}

	@Override
	public boolean showDurabilityBar(ItemStack stack) {

		return !isCreative(stack) && getFluidAmount(stack) > 0;
	}

	/* HELPERS */
	protected abstract int getMaxFluidAmount(ItemStack stack);

	public int getFluidAmount(ItemStack stack) {

		FluidStack fluid = getFluid(stack);
		return fluid == null ? 0 : fluid.amount;
	}

	public int getScaledFluidStored(ItemStack stack, int scale) {

		return MathHelper.round((long) getFluidAmount(stack) * scale / getCapacity(stack));
	}

	@SideOnly (Side.CLIENT)
	protected static void addPotionTooltip(NBTTagCompound potionTag, List<String> lores) {

		List<PotionEffect> list = PotionUtils.getEffectsFromTag(potionTag);
		if (list.isEmpty()) {
			String s = StringHelper.localize("effect.none").trim();
			lores.add(TextFormatting.GRAY + s);
		} else {
			for (PotionEffect potioneffect : list) {
				String s1 = StringHelper.localize(potioneffect.getEffectName()).trim();
				Potion potion = potioneffect.getPotion();
				if (potioneffect.getAmplifier() > 0) {
					s1 = s1 + " " + StringHelper.localize("potion.potency." + potioneffect.getAmplifier()).trim();
				}
				if (potion.isBadEffect()) {
					lores.add(TextFormatting.RED + s1);
				} else {
					lores.add(TextFormatting.BLUE + s1);
				}
			}
		}
	}

	/* IItemColor */
	public int colorMultiplier(ItemStack stack, int tintIndex) {

		FluidStack fluid = getFluid(stack);
		if (fluid != null && tintIndex == TINT_INDEX_FLUID) {
			return FluidPotion.getPotionColor(fluid);
		}
		return 0xFFFFFF;
	}

	/* IEnchantableItem */
	@Override
	public boolean canEnchant(ItemStack stack, Enchantment enchantment) {

		return !isCreative(stack) && enchantment == CoreEnchantments.holding;
	}

	/* IFluidContainerItem */
	@Override
	public FluidStack getFluid(ItemStack container) {

		if (container.getTagCompound() == null) {
			container.setTagCompound(new NBTTagCompound());
		}
		if (!container.getTagCompound().hasKey(CoreProps.FLUID)) {
			return null;
		}
		return FluidStack.loadFluidStackFromNBT(container.getTagCompound().getCompoundTag(CoreProps.FLUID));
	}

	@Override
	public int getCapacity(ItemStack stack) {

		return getMaxFluidAmount(stack);
	}

	@Override
	public int fill(ItemStack container, FluidStack resource, boolean doFill) {

		if (container.getTagCompound() == null) {
			container.setTagCompound(new NBTTagCompound());
		}
		if (resource == null || resource.amount <= 0 || !TFFluids.isPotion(resource)) {
			return 0;
		}
		int capacity = getCapacity(container);

		if (isCreative(container)) {
			if (doFill) {
				NBTTagCompound fluidTag = resource.writeToNBT(new NBTTagCompound());
				fluidTag.setInteger(CoreProps.AMOUNT, capacity - Fluid.BUCKET_VOLUME);
				container.getTagCompound().setTag(CoreProps.FLUID, fluidTag);
			}
			return resource.amount;
		}
		if (!doFill) {
			if (!container.getTagCompound().hasKey(CoreProps.FLUID)) {
				return Math.min(capacity, resource.amount);
			}
			FluidStack stack = FluidStack.loadFluidStackFromNBT(container.getTagCompound().getCompoundTag(CoreProps.FLUID));

			if (stack == null) {
				return Math.min(capacity, resource.amount);
			}
			if (!stack.isFluidEqual(resource)) {
				return 0;
			}
			return Math.min(capacity - stack.amount, resource.amount);
		}
		if (!container.getTagCompound().hasKey(CoreProps.FLUID)) {
			NBTTagCompound fluidTag = resource.writeToNBT(new NBTTagCompound());

			if (capacity < resource.amount) {
				fluidTag.setInteger(CoreProps.AMOUNT, capacity);
				container.getTagCompound().setTag(CoreProps.FLUID, fluidTag);
				return capacity;
			}
			fluidTag.setInteger(CoreProps.AMOUNT, resource.amount);
			container.getTagCompound().setTag(CoreProps.FLUID, fluidTag);
			return resource.amount;
		}
		NBTTagCompound fluidTag = container.getTagCompound().getCompoundTag(CoreProps.FLUID);
		FluidStack stack = FluidStack.loadFluidStackFromNBT(fluidTag);

		if (stack == null || !stack.isFluidEqual(resource)) {
			return 0;
		}
		int filled = capacity - stack.amount;

		if (resource.amount < filled) {
			stack.amount += resource.amount;
			filled = resource.amount;
		} else {
			stack.amount = capacity;
		}
		container.getTagCompound().setTag(CoreProps.FLUID, stack.writeToNBT(fluidTag));
		return filled;
	}

	@Override
	public FluidStack drain(ItemStack container, int maxDrain, boolean doDrain) {

		if (container.getTagCompound() == null) {
			container.setTagCompound(new NBTTagCompound());
		}
		if (!container.getTagCompound().hasKey(CoreProps.FLUID) || maxDrain == 0) {
			return null;
		}
		FluidStack stack = FluidStack.loadFluidStackFromNBT(container.getTagCompound().getCompoundTag(CoreProps.FLUID));

		if (stack == null) {
			return null;
		}
		boolean creative = isCreative(container);
		int drained = creative ? maxDrain : Math.min(stack.amount, maxDrain);

		if (doDrain && !creative) {
			if (maxDrain >= stack.amount) {
				container.getTagCompound().removeTag(CoreProps.FLUID);
				return stack;
			}
			NBTTagCompound fluidTag = container.getTagCompound().getCompoundTag(CoreProps.FLUID);
			fluidTag.setInteger(CoreProps.AMOUNT, fluidTag.getInteger(CoreProps.AMOUNT) - drained);
			container.getTagCompound().setTag(CoreProps.FLUID, fluidTag);
		}
		stack.amount = drained;
		return stack;
	}

	/* CAPABILITIES */
	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt) {

		return new FluidContainerItemWrapper(stack, this);
	}

	public static final int TINT_INDEX_FLUID = 1;
	public static final int TINT_INDEX_0 = 2;
	public static final int TINT_INDEX_1 = 3;

}
