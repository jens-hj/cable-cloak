package dev.jensh.cablecloak;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Put on a cable to give it an empty frame, ready to take the look of a block. */
public class CloakFrameItem extends Item {
	public CloakFrameItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		if (!CloakSupport.isCloakable(level.getBlockState(context.getClickedPos()))) {
			return InteractionResult.PASS;
		}
		CloakBlockEntity cloak = CloakSupport.cloakAt(level, context.getClickedPos());
		if (cloak == null || cloak.hasFrame()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			cloak.setFrame(true);
			Player player = context.getPlayer();
			if (player == null || !player.getAbilities().instabuild) {
				context.getItemInHand().shrink(1);
			}
			level.playSound(null, context.getClickedPos(), SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
		}
		return InteractionResult.SUCCESS;
	}
}
