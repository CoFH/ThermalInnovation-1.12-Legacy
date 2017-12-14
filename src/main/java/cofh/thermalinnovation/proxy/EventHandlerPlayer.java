package cofh.thermalinnovation.proxy;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly (Side.CLIENT)
public class EventHandlerPlayer {

	public static final EventHandlerPlayer INSTANCE = new EventHandlerPlayer();

	@SubscribeEvent
	public void onLivingRender(RenderLivingEvent.Pre event) {

		//		if (!(event.getEntity() instanceof EntityPlayer) || !(event.getRenderer() instanceof RenderPlayer)) {
		//			return;
		//		}
		//		EntityPlayer player = (EntityPlayer) event.getEntity();
		//		if (player.isSwingInProgress && !player.getHeldItem(player.swingingHand).isEmpty()) {
		//			ItemStack stack = player.getHeldItem(player.swingingHand);
		//			if (stack.getItem() instanceof IChargeAnimationItem && ((IChargeAnimationItem) stack.getItem()).useChargeAnimation(stack)) {
		//				RenderPlayer renderer = (RenderPlayer) event.getRenderer();
		//				if (getHandSide(player, player.swingingHand) == EnumHandSide.RIGHT) {
		//					renderer.getMainModel().rightArmPose = ModelBiped.ArmPose.BOW_AND_ARROW;
		//				} else {
		//					renderer.getMainModel().leftArmPose = ModelBiped.ArmPose.BOW_AND_ARROW;
		//				}
		//				player.swingProgress = 0;
		//			}
		//		}
	}

	@SubscribeEvent
	public void onClientTick(TickEvent.PlayerTickEvent event) {

		//		if (event.phase == Phase.START || event.player == null) {
		//			return;
		//		}
		//		EntityPlayer player = event.player;
		//		if (player.isSwingInProgress && !player.getHeldItem(player.swingingHand).isEmpty()) {
		//			ItemStack stack = player.getHeldItem(player.swingingHand);
		//			if (stack.getItem() instanceof IChargeAnimationItem && ((IChargeAnimationItem) stack.getItem()).useChargeAnimation(stack)) {
		//				if (player.getActiveItemStack() != stack) {
		//					doAnimation = true;
		//					player.setActiveHand(player.swingingHand);
		//					doAnimation = false;
		//				}
		//				player.swingProgress = 0;
		//			}
		//		}
	}

	@SubscribeEvent
	public void onUseItem(LivingEntityUseItemEvent.Start event) {

		if (doAnimation) {
			event.setDuration(1);
		}
	}

	private boolean doAnimation;

	/* HELPERS */
	public static EnumHandSide getHandSide(EntityLivingBase player, EnumHand hand) {

		if (hand == EnumHand.MAIN_HAND) {
			return player.getPrimaryHand();
		} else {
			return player.getPrimaryHand() == EnumHandSide.RIGHT ? EnumHandSide.LEFT : EnumHandSide.RIGHT;
		}
	}

}
