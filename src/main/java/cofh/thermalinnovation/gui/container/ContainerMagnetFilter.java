package cofh.thermalinnovation.gui.container;

import cofh.api.core.IFilterable;
import cofh.core.gui.container.ContainerCore;
import cofh.core.gui.slot.SlotFalseCopy;
import cofh.core.gui.slot.SlotLocked;
import cofh.core.network.PacketCore;
import cofh.core.util.CoreUtils;
import cofh.core.util.filter.ItemFilterWrapper;
import cofh.core.util.helpers.MathHelper;
import cofh.core.util.helpers.StringHelper;
import cofh.thermalinnovation.item.ItemMagnet;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerMagnetFilter extends ContainerCore implements IFilterable {

	static final String NAME = "item.thermalinnovation.magnet.name";

	protected final ItemFilterWrapper filterWrapper;
	protected final EntityPlayer player;
	protected final int containerIndex;
	protected final int filterIndex;
	protected boolean valid = true;

	public ContainerMagnetFilter(ItemStack stack, InventoryPlayer inventory) {

		player = inventory.player;
		containerIndex = inventory.currentItem;
		filterWrapper = new ItemFilterWrapper(stack, ItemMagnet.getFilterSize(stack));

		filterIndex = ItemMagnet.getLevel(stack);
		int rows = MathHelper.clamp(filterIndex + 1, 1, 3);
		int slots = ItemMagnet.getFilterSize(stack);
		int rowSize = slots / rows;

		int xOffset = 89 - 9 * rowSize;
		int yOffset = filterIndex == 0 ? 26 : 17;

		bindPlayerInventory(inventory);

		for (int i = 0; i < slots; i++) {
			addSlotToContainer(new SlotFalseCopy(filterWrapper, i, xOffset + i % rowSize * 18, yOffset + i / rowSize * 18));
		}
	}

	@Override
	protected void bindPlayerInventory(InventoryPlayer inventoryPlayer) {

		int xOffset = getPlayerInventoryHorizontalOffset();
		int yOffset = getPlayerInventoryVerticalOffset();

		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 9; j++) {
				addSlotToContainer(new Slot(inventoryPlayer, j + i * 9 + 9, xOffset + j * 18, yOffset + i * 18));
			}
		}
		for (int i = 0; i < 9; i++) {
			if (i == inventoryPlayer.currentItem) {
				addSlotToContainer(new SlotLocked(inventoryPlayer, i, xOffset + i * 18, yOffset + 58));
			} else {
				addSlotToContainer(new Slot(inventoryPlayer, i, xOffset + i * 18, yOffset + 58));
			}
		}
	}

	@Override
	protected int getPlayerInventoryVerticalOffset() {

		return filterIndex > 1 ? 120 : 102;
	}

	@Override
	protected int getSizeInventory() {

		return 0;
	}

	@Override
	public void detectAndSendChanges() {

		ItemStack item = player.inventory.mainInventory.get(containerIndex);
		if (item.isEmpty() || item.getItem() != filterWrapper.getFilterItem()) {
			valid = false;
			return;
		}
		super.detectAndSendChanges();
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {

		onSlotChanged();
		if (filterWrapper.getDirty() && !valid) {
			player.inventory.setItemStack(ItemStack.EMPTY);
		}
		return valid;
	}

	/* HELPERS */
	public void onSlotChanged() {

		ItemStack item = player.inventory.mainInventory.get(containerIndex);
		if (valid && !item.isEmpty() && item.getItem() == filterWrapper.getFilterItem()) {
			player.inventory.mainInventory.set(containerIndex, filterWrapper.getFilterStack());
		}
	}

	public boolean getFlag(int flag) {

		return filterWrapper.getFilter().getFlag(flag);
	}

	public String getInventoryName() {

		return filterWrapper.hasCustomName() ? filterWrapper.getName() : StringHelper.localize(NAME);
	}

	public ItemStack getFilterStack() {

		return filterWrapper.getFilterStack();
	}

	/* IFilterable */
	public void setFlag(int flag, boolean value) {

		filterWrapper.getFilter().setFlag(flag, value);
		if (CoreUtils.isClient()) {
			PacketCore.sendFilterPacketToServer(flag, value);
		}
		filterWrapper.markDirty();
	}

}
