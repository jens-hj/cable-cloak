package dev.jensh.cablecloak;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Lives on every cloakable cable. Empty until a frame is put on; then it holds the frame, the block state the
 * facade copies, and the block item that went into it (dropped again when the facade comes off).
 */
public class CloakBlockEntity extends BlockEntity {
	private boolean frame;
	private @Nullable BlockState facade;
	private ItemStack facadeItem = ItemStack.EMPTY;

	private @Nullable VoxelShape facadeShape;
	private @Nullable BlockState shapeForCable;
	private @Nullable VoxelShape shapeCache;

	public CloakBlockEntity(BlockPos pos, BlockState state) {
		super(CableCloak.CLOAK_BE, pos, state);
	}

	public boolean hasFrame() {
		return frame;
	}

	public @Nullable BlockState facade() {
		return facade;
	}

	public ItemStack facadeItem() {
		return facadeItem;
	}

	public void setFrame(boolean frame) {
		this.frame = frame;
		if (!frame) {
			facade = null;
			facadeItem = ItemStack.EMPTY;
		}
		changed();
	}

	/** Sets the copied look. {@code item} is what the player paid and gets back; empty in creative. */
	public void setFacade(@Nullable BlockState facade, ItemStack item) {
		this.facade = facade;
		this.facadeItem = facade == null ? ItemStack.EMPTY : item.copyWithCount(Math.min(item.getCount(), 1));
		changed();
	}

	/** The shape of the cable with its facade on, or {@code cableShape} when there is none. */
	public VoxelShape shape(BlockState cable, VoxelShape cableShape) {
		if (!frame) {
			return cableShape;
		}
		if (facadeShape == null) {
			facadeShape = facade == null
					? Shapes.block()
					: facade.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty());
		}
		if (shapeCache == null || shapeForCable != cable) {
			shapeCache = Shapes.or(cableShape, facadeShape);
			shapeForCable = cable;
		}
		return shapeCache;
	}

	private void invalidate() {
		facadeShape = null;
		shapeCache = null;
		shapeForCable = null;
	}

	private void changed() {
		invalidate();
		setChanged();
		if (level != null) {
			BlockState state = getBlockState();
			level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		frame = input.getBooleanOr("frame", false);
		facade = frame ? input.read("facade", BlockState.CODEC).orElse(null) : null;
		facadeItem = facade != null ? input.read("facade_item", ItemStack.CODEC).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
		invalidate();
		if (level != null && level.isClientSide()) {
			// Redraw the chunk section with the new look.
			BlockState state = getBlockState();
			level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_IMMEDIATE);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (frame) {
			output.putBoolean("frame", true);
			output.storeNullable("facade", BlockState.CODEC, facade);
			if (!facadeItem.isEmpty()) {
				output.store("facade_item", ItemStack.CODEC, facadeItem);
			}
		}
	}

	@Override
	public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level != null && frame) {
			Block.popResource(level, pos, new ItemStack(CableCloak.CLOAK_FRAME));
			if (!facadeItem.isEmpty()) {
				Block.popResource(level, pos, facadeItem.copy());
			}
		}
	}

	@Override
	public @Nullable Object getRenderData() {
		return frame ? new CloakRenderData(facade, facade != null && facade.isSolidRender()) : null;
	}

	/** What the client model needs: the copied look (null for a bare frame) and whether it hides the cable. */
	public record CloakRenderData(@Nullable BlockState facade, boolean hidesCable) {
	}
}
