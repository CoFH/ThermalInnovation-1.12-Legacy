package cofh.thermalinnovation.item;

import cofh.api.fluid.IFluidContainerItem;
import cofh.api.item.IMultiModeItem;
import cofh.api.item.INBTCopyIngredient;
import cofh.core.init.CoreEnchantments;
import cofh.core.init.CoreProps;
import cofh.core.item.IEnchantableItem;
import cofh.core.item.ItemMulti;
import cofh.core.key.KeyBindingItemMultiMode;
import cofh.core.util.CoreUtils;
import cofh.core.util.RayTracer;
import cofh.core.util.capabilities.FluidContainerItemWrapper;
import cofh.core.util.core.IInitializer;
import cofh.core.util.helpers.*;
import cofh.thermalfoundation.fluid.BlockFluidGlowstone;
import cofh.thermalfoundation.init.TFFluids;
import cofh.thermalinnovation.ThermalInnovation;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockGlowstone;
import net.minecraft.block.SoundType;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class ItemIlluminator extends ItemMulti implements IInitializer, IMultiModeItem, IFluidContainerItem, IEnchantableItem, INBTCopyIngredient {

    public ItemIlluminator() {

        super("thermalinnovation");

        setUnlocalizedName("illuminator");
        setCreativeTab(ThermalInnovation.tabCommon);

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
        tooltip.add(StringHelper.getInfoText("info.thermalinnovation.illuminator.a.0"));
        tooltip.add(StringHelper.localizeFormat("info.thermalinnovation.illuminator.a.1", getRange(stack)));
        tooltip.add(StringHelper.localizeFormat("info.thermalinnovation.illuminator.b." + getMode(stack), StringHelper.getKeyName(KeyBindingItemMultiMode.INSTANCE.getKey())));

        FluidStack fluid = getFluid(stack);
        if (fluid != null) {
            tooltip.add(StringHelper.localize("info.cofh.fluid") + ": " + StringHelper.YELLOW + fluid.getFluid().getLocalizedName(fluid) + StringHelper.LIGHT_GRAY);

            if (ItemHelper.getItemDamage(stack) == CREATIVE) {
                tooltip.add(StringHelper.localize("info.cofh.infiniteSource"));
            } else {
                tooltip.add(StringHelper.localize("info.cofh.level") + ": " + StringHelper.formatNumber(fluid.amount) + " / " + StringHelper.formatNumber(getCapacity(stack)) + " mB");
            }
        } else {
            tooltip.add(StringHelper.localize("info.cofh.fluid") + ": " + StringHelper.localize("info.cofh.empty"));

            if (ItemHelper.getItemDamage(stack) == CREATIVE) {
                tooltip.add(StringHelper.localize("info.cofh.infiniteSource"));
            } else {
                tooltip.add(StringHelper.localize("info.cofh.level") + ": 0 / " + StringHelper.formatNumber(getCapacity(stack)) + " mB");
            }
        }
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {

        if (ServerHelper.isClientWorld(world) || world.getTotalWorldTime() % CoreProps.TIME_CONSTANT_EIGHTH != 0) {
            return;
        }
        if (!(entity instanceof EntityLivingBase) || CoreUtils.isFakePlayer(entity) || getMode(stack) == 0) {
            return;
        }
        if(getFluidAmount(stack) < MB_PER_SOURCE) {
            return;
        }

        int range = getRange(stack);
        BlockPos pos = entity.getPosition();

        for(int x = -range; x < range; x++) {
            for(int y = -range; y < range; y++) {
                for(int z = -range; z < range; z++) {

                    BlockPos target = pos.add(x, y, z);
                    if(!world.isAirBlock(target))
                        continue;

                    int light = world.getLightFromNeighbors(target);
                    if(light <= minLight && canPlaceLight(entity, target, world)) {
                        world.setBlockState(target, LIGHT.getDefaultState());
                        SoundType sound = LIGHT.getSoundType();
                        world.playSound(null, target, sound.getPlaceSound(), SoundCategory.BLOCKS, sound.getVolume(), sound.getPitch());
                        drain(stack, MB_PER_SOURCE, true);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {

        if (CoreUtils.isFakePlayer(player)) {
            return EnumActionResult.FAIL;
        }

        ItemStack stack = player.getHeldItem(hand);

        if(world.getBlockState(pos).getBlock() instanceof BlockGlowstone) {
            if(getFluidAmount(stack) + 100 >= getCapacity(stack)) {
                return EnumActionResult.FAIL;
            }

            if(ServerHelper.isServerWorld(world)) {
                world.destroyBlock(pos, false);
                fill(stack, new FluidStack(TFFluids.fluidGlowstone, MB_PER_SOURCE), true);
            }
        } else {
            return EnumActionResult.PASS;
        }

        return EnumActionResult.SUCCESS;
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {

        if (isInCreativeTab(tab)) {
            for (int metadata : itemList) {
                if (metadata != CREATIVE) {
                    items.add(new ItemStack(this, 1, metadata));
                    items.add(FluidHelper.setDefaultFluidTag(new ItemStack(this, 1, metadata), new FluidStack(TFFluids.fluidGlowstone, typeMap.get(metadata).capacity)));
                } else {
                    items.add(FluidHelper.setDefaultFluidTag(new ItemStack(this, 1, metadata), new FluidStack(TFFluids.fluidGlowstone, typeMap.get(metadata).capacity)));
                }
            }
        }
    }

    @Override
    public boolean isFull3D() {

        return true;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {

        return ItemHelper.getItemDamage(stack) != CREATIVE;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {

        return super.shouldCauseReequipAnimation(oldStack, newStack, slotChanged) && (slotChanged || !ItemHelper.areItemStacksEqualIgnoreTags(oldStack, newStack, "Fluid"));
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {

        return 0xFEE803;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {

        return 1.0D - (getFluidAmount(stack) / (double) getCapacity(stack));
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {

        return ItemHelper.getItemDamage(stack) != CREATIVE;
    }

    @Override
    public int getItemEnchantability(ItemStack stack) {

        return 10;
    }

    /* IEnchantableItem */
    @Override
    public boolean canEnchant(ItemStack stack, Enchantment enchantment) {

        return enchantment == CoreEnchantments.holding;
    }

    /* IMultiModeItem */
    @Override
    public void onModeChange(EntityPlayer player, ItemStack stack) {

        int mode = getMode(stack);
        if (mode == 1) {
            player.world.playSound(null, player.getPosition(), SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.PLAYERS, 0.4F, 1.0F);
        } else {
            player.world.playSound(null, player.getPosition(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.2F, 0.6F);
        }
        ChatHelper.sendIndexedChatMessageToPlayer(player, new TextComponentTranslation("info.thermalinnovation.illuminator.c." + mode));
    }

    /* HELPERS */
    public int getFluidAmount(ItemStack stack) {

        FluidStack fluid = getFluid(stack);
        return fluid == null ? 0 : fluid.amount;
    }

    public boolean canPlaceLight(Entity user, BlockPos pos, World world) {

        boolean ret = false;
        for(EnumFacing facing : EnumFacing.values()) {
            if(!world.isAirBlock(pos.offset(facing)))
                ret = true;
        }

        RayTraceResult res = world.rayTraceBlocks(user.getPositionVector(), new Vec3d(pos), false, true, false);
        if(res != null) {
            ret = false;
        }

        return ret;
    }

    public int getRange(ItemStack stack) {

        if(!typeMap.containsKey(ItemHelper.getItemDamage(stack))) {
            return 0;
        }

        return typeMap.get(ItemHelper.getItemDamage(stack)).range;
    }

    /* CAPABILITIES */
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt) {

        return new FluidContainerItemWrapper(stack, this);
    }

    /* IFluidContainerItem */
    @Override
    public FluidStack getFluid(ItemStack container) {

        if (container.getTagCompound() == null) {
            container.setTagCompound(new NBTTagCompound());
        }
        if (!container.getTagCompound().hasKey("Fluid")) {
            return null;
        }
        return FluidStack.loadFluidStackFromNBT(container.getTagCompound().getCompoundTag("Fluid"));
    }

    @Override
    public int getCapacity(ItemStack stack) {

        if (!typeMap.containsKey(ItemHelper.getItemDamage(stack))) {
            return 0;
        }
        int capacity = typeMap.get(ItemHelper.getItemDamage(stack)).capacity;
        int enchant = EnchantmentHelper.getEnchantmentLevel(CoreEnchantments.holding, stack);

        return capacity + capacity * enchant / 2;
    }

    @Override
    public int fill(ItemStack container, FluidStack resource, boolean doFill) {

        if (container.getTagCompound() == null) {
            container.setTagCompound(new NBTTagCompound());
        }
        if (resource == null || resource.getFluid() != TFFluids.fluidGlowstone) {
            return 0;
        }
        int capacity = getCapacity(container);

        if (ItemHelper.getItemDamage(container) == CREATIVE) {
            if (doFill) {
                NBTTagCompound fluidTag = resource.writeToNBT(new NBTTagCompound());
                fluidTag.setInteger("Amount", capacity - Fluid.BUCKET_VOLUME);
                container.getTagCompound().setTag("Fluid", fluidTag);
            }
            return resource.amount;
        }
        if (!doFill) {
            if (!container.getTagCompound().hasKey("Fluid")) {
                return Math.min(capacity, resource.amount);
            }
            FluidStack stack = FluidStack.loadFluidStackFromNBT(container.getTagCompound().getCompoundTag("Fluid"));

            if (stack == null) {
                return Math.min(capacity, resource.amount);
            }
            if (!stack.isFluidEqual(resource)) {
                return 0;
            }
            return Math.min(capacity - stack.amount, resource.amount);
        }
        if (!container.getTagCompound().hasKey("Fluid")) {
            NBTTagCompound fluidTag = resource.writeToNBT(new NBTTagCompound());

            if (capacity < resource.amount) {
                fluidTag.setInteger("Amount", capacity);
                container.getTagCompound().setTag("Fluid", fluidTag);
                return capacity;
            }
            fluidTag.setInteger("Amount", resource.amount);
            container.getTagCompound().setTag("Fluid", fluidTag);
            return resource.amount;
        }
        NBTTagCompound fluidTag = container.getTagCompound().getCompoundTag("Fluid");
        FluidStack stack = FluidStack.loadFluidStackFromNBT(fluidTag);

        if (!stack.isFluidEqual(resource)) {
            return 0;
        }
        int filled = capacity - stack.amount;

        if (resource.amount < filled) {
            stack.amount += resource.amount;
            filled = resource.amount;
        } else {
            stack.amount = capacity;
        }
        container.getTagCompound().setTag("Fluid", stack.writeToNBT(fluidTag));
        return filled;
    }

    @Override
    public FluidStack drain(ItemStack container, int maxDrain, boolean doDrain) {

        if (container.getTagCompound() == null) {
            container.setTagCompound(new NBTTagCompound());
        }
        if (!container.getTagCompound().hasKey("Fluid") || maxDrain == 0) {
            return null;
        }
        FluidStack stack = FluidStack.loadFluidStackFromNBT(container.getTagCompound().getCompoundTag("Fluid"));

        if (stack == null) {
            return null;
        }
        boolean creative = ItemHelper.getItemDamage(container) == CREATIVE;
        int drained = creative ? maxDrain : Math.min(stack.amount, maxDrain);

        if (doDrain && !creative) {
            if (maxDrain >= stack.amount) {
                container.getTagCompound().removeTag("Fluid");
                return stack;
            }
            NBTTagCompound fluidTag = container.getTagCompound().getCompoundTag("Fluid");
            fluidTag.setInteger("Amount", fluidTag.getInteger("Amount") - drained);
            container.getTagCompound().setTag("Fluid", fluidTag);
        }
        stack.amount = drained;
        return stack;
    }

    /* IModelRegister */
    @Override
    @SideOnly(Side.CLIENT)
    public void registerModels() {

        ModelLoader.setCustomMeshDefinition(this, stack -> new ModelResourceLocation(getRegistryName(), String.format("mode=%s,tank=%s,type=%s", getMode(stack), this.getFluidAmount(stack) > 0 ? "filled" : "empty", typeMap.get(ItemHelper.getItemDamage(stack)).name)));

        String[] tankStates = { "filled", "empty" };

        for (Map.Entry<Integer, ItemEntry> entry : itemMap.entrySet()) {
            for (int i = 0; i < 2; i++) {
                for(int j = 0; j < 2; j++) {
                    ModelBakery.registerItemVariants(this, new ModelResourceLocation(getRegistryName(), String.format("mode=%s,tank=%s,type=%s", j, tankStates[i], entry.getValue().name)));
                }
            }
        }
    }

    /* IInitializer */
    @Override
    public boolean initialize() {

        config();

        illuminatorBasic = addEntryItem(0, "standard0", CAPACITY[0], RANGE[0], EnumRarity.COMMON);
        illuminatorHardened = addEntryItem(1, "standard1", CAPACITY[1], RANGE[1], EnumRarity.COMMON);
        illuminatorReinforced = addEntryItem(2, "standard2", CAPACITY[2], RANGE[2], EnumRarity.UNCOMMON);
        illuminatorSignalum = addEntryItem(3, "standard3", CAPACITY[3], RANGE[3], EnumRarity.UNCOMMON);
        illuminatorResonant = addEntryItem(4, "standard4", CAPACITY[4], RANGE[4], EnumRarity.RARE);

        illuminatorCreative = addEntryItem(CREATIVE, "creative", CAPACITY[4], RANGE[4], EnumRarity.EPIC);

        ThermalInnovation.proxy.addIModelRegister(this);

        return true;
    }

    @Override
    public boolean register() {

        if (!enable) {
            return false;
        }
        // @formatter:off

        // @formatter:on

        return true;
    }

    private static void config() {

        String category = "Item.Illuminator";
        String comment;

        int capacity = CAPACITY_BASE;
        comment = "Adjust this value to change the amount of Fluid (in mB) stored by a Basic Portable Illuminator. This base value will scale with item level.";
        capacity = ThermalInnovation.CONFIG.getConfiguration().getInt("BaseCapacity", category, capacity, capacity / 5, capacity * 5, comment);

        comment = "Adjust this value to change the minimum light level the Portable Illuminator will place light sources at.";
        minLight = ThermalInnovation.CONFIG.getConfiguration().getInt("MinLight", category, minLight, 0, 15, comment);

        for (int i = 0; i < CAPACITY.length; i++) {
            CAPACITY[i] *= capacity;
        }
    }

    /* ENTRY */
    public class TypeEntry {

        public final String name;
        public final int capacity;
        public final int range;

        TypeEntry(String name, int capacity, int range) {

            this.name = name;
            this.capacity = capacity;
            this.range = range;
        }
    }

    private void addEntry(int metadata, String name, int capacity, int range) {

        typeMap.put(metadata, new TypeEntry(name, capacity, range));
    }

    private ItemStack addEntryItem(int metadata, String name, int capacity, int range, EnumRarity rarity) {

        addEntry(metadata, name, capacity, range);
        return addItem(metadata, name, rarity);
    }

    private static TIntObjectHashMap<TypeEntry> typeMap = new TIntObjectHashMap<>();

    public static final int CAPACITY_BASE = 1000;
    public static final int MB_PER_SOURCE = 100;
    public static final int CREATIVE = 32000;

    public static final int[] CAPACITY = { 1, 3, 6, 10, 15 };
    public static final int[] RANGE = { 4, 6, 8, 10, 12 };

    public static final Block LIGHT = Blocks.GLOWSTONE;

    public static int minLight = 8;
    public static boolean enable = true;

    /* REFERENCES */
    public static ItemStack illuminatorBasic;
    public static ItemStack illuminatorHardened;
    public static ItemStack illuminatorReinforced;
    public static ItemStack illuminatorSignalum;
    public static ItemStack illuminatorResonant;

    public static ItemStack illuminatorCreative;
}
