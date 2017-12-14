package cofh.thermalinnovation.gui;

import cofh.core.block.TileCore;
import cofh.core.util.helpers.ItemHelper;
import cofh.thermalinnovation.gui.client.GuiMagnetFilter;
import cofh.thermalinnovation.gui.container.ContainerMagnetFilter;
import cofh.thermalinnovation.init.TIItems;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class GuiHandler implements IGuiHandler {

	public static final int TILE_ID = 0;
	public static final int TILE_CONFIG_ID = 1;
	public static final int MAGNET_FILTER_ID = 17;

	@Override
	public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {

		switch (id) {
			case TILE_ID:
				TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
				if (tile instanceof TileCore) {
					return ((TileCore) tile).getGuiClient(player.inventory);
				}
				return null;
			case TILE_CONFIG_ID:
				tile = world.getTileEntity(new BlockPos(x, y, z));
				if (tile instanceof TileCore) {
					return ((TileCore) tile).getConfigGuiClient(player.inventory);
				}
				return null;
			case MAGNET_FILTER_ID:
				if (ItemHelper.isPlayerHoldingMainhand(TIItems.itemMagnet, player)) {
					return new GuiMagnetFilter(player.inventory, new ContainerMagnetFilter(player.getHeldItemMainhand(), player.inventory));
				}
				return null;
			default:
				return null;
		}
	}

	@Override
	public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {

		switch (id) {
			case TILE_ID:
				TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
				if (tile instanceof TileCore) {
					return ((TileCore) tile).getGuiServer(player.inventory);
				}
				return null;
			case TILE_CONFIG_ID:
				tile = world.getTileEntity(new BlockPos(x, y, z));
				if (tile instanceof TileCore) {
					return ((TileCore) tile).getConfigGuiServer(player.inventory);
				}
				return null;
			case MAGNET_FILTER_ID:
				if (ItemHelper.isPlayerHoldingMainhand(TIItems.itemMagnet, player)) {
					return new ContainerMagnetFilter(player.getHeldItemMainhand(), player.inventory);
				}
				return null;
			default:
				return null;
		}
	}

}
