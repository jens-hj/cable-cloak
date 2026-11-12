package dev.jensh.cablecloak.test;

import com.nightbeam.omnivault.network.NetworkContent;
import com.nightbeam.omnivault.network.block.CableBlock;
import dev.jensh.cablecloak.CloakBlockEntity;
import java.nio.file.Path;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Builds a row of cables in every cloak state, checks the client received the cloaks, and takes screenshots
 * (build/run/clientGameTest/screenshots) for a visual check and the Modrinth gallery.
 */
@SuppressWarnings("UnstableApiUsage")
public class CloakClientGameTest implements FabricClientGameTest {
	/** A row of cables: bare, bare frame, then a facade per entry. */
	private static final BlockState[] LOOKS = {
			null,
			null,
			Blocks.STONE_BRICKS.defaultBlockState(),
			Blocks.GRASS_BLOCK.defaultBlockState(),
			Blocks.OAK_LEAVES.defaultBlockState(),
			Blocks.GLASS.defaultBlockState(),
			Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH),
			Blocks.BOOKSHELF.defaultBlockState(),
	};

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			sp.getConnection().waitForChunksRender();
			for (String cmd : new String[]{"time set 6000", "gamerule advance_time false", "weather clear",
					"gamerule advance_weather false", "gamerule spawn_mobs false", "gamemode spectator @a"}) {
				sp.getServer().runCommand(cmd);
			}
			BlockPos o = sp.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().getFirst().blockPosition());
			BlockPos start = o.offset(-LOOKS.length + 1, 1, -5);

			sp.getServer().runOnServer(server -> build(server, start));
			// A second row behind: one long cable, half of it cloaked in stone bricks to show it disappearing.
			sp.getServer().runOnServer(server -> buildRun(server, start.offset(0, 0, -3)));

			context.runOnClient(mc -> {
				mc.options.fov().set(60);
				mc.options.bobView().set(false);
			});
			tp(sp, o.getX() + 0.5, o.getY() + 3.2, o.getZ() + 2.5, 25);
			context.waitTicks(40);
			sp.getConnection().waitForChunksRender();

			context.runOnClient(mc -> {
				for (int i = 0; i < LOOKS.length; i++) {
					BlockPos pos = start.offset(i * 2, 0, 0);
					if (!(mc.level.getBlockEntity(pos) instanceof CloakBlockEntity cloak)) {
						throw new AssertionError("client has no cloak block entity at " + pos);
					}
					if (cloak.hasFrame() != (i > 0) || !java.util.Objects.equals(cloak.facade(), LOOKS[i])) {
						throw new AssertionError("client cloak at " + pos + " is " + cloak.facade() + ", expected " + LOOKS[i]);
					}
				}
			});

			context.getInput().pressKey(options -> options.keyToggleGui);
			Path out = Path.of("gallery");
			shoot(context, out, "row");
			tp(sp, o.getX() + 0.5, o.getY() + 6, o.getZ() - 1, 55);
			context.waitTicks(20);
			sp.getConnection().waitForChunksRender();
			shoot(context, out, "above");
		}
	}

	private static void build(MinecraftServer server, BlockPos start) {
		ServerLevel level = server.overworld();
		for (int i = 0; i < LOOKS.length; i++) {
			BlockPos pos = start.offset(i * 2, 0, 0);
			BlockState cable = NetworkContent.NEXUS_CABLE.get().defaultBlockState();
			if (i == LOOKS.length - 1) {
				cable = cable.setValue(CableBlock.COLOR, CableBlock.CableColor.of(DyeColor.RED));
			}
			level.setBlock(pos, cable, Block.UPDATE_ALL);
			level.setBlock(pos.below(), Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_ALL);
			if (i > 0) {
				cloak(level, pos, LOOKS[i]);
			}
		}
	}

	private static void buildRun(MinecraftServer server, BlockPos start) {
		ServerLevel level = server.overworld();
		int length = LOOKS.length * 2 - 1;
		for (int i = 0; i < length; i++) {
			BlockPos pos = start.offset(i, 0, 0);
			level.setBlock(pos, NetworkContent.INVENTORY_CABLE.get().defaultBlockState(), Block.UPDATE_ALL);
		}
		for (int i = 0; i < length; i++) {
			BlockPos pos = start.offset(i, 0, 0);
			level.setBlock(pos, level.getBlockState(pos), Block.UPDATE_ALL);
			if (i >= length / 2) {
				cloak(level, pos, Blocks.STONE_BRICKS.defaultBlockState());
			}
		}
	}

	private static void cloak(ServerLevel level, BlockPos pos, @Nullable BlockState look) {
		if (!(level.getBlockEntity(pos) instanceof CloakBlockEntity cloak)) {
			throw new AssertionError("no cloak block entity at " + pos);
		}
		cloak.setFrame(true);
		if (look != null) {
			cloak.setFacade(look, ItemStack.EMPTY);
		}
	}

	private static void tp(TestSingleplayerContext sp, double x, double eyeY, double z, float pitch) {
		sp.getServer().runCommand(String.format(Locale.ROOT, "tp @a %.2f %.2f %.2f 180 %.1f", x, eyeY - 1.62, z, pitch));
	}

	private static void shoot(ClientGameTestContext context, Path out, String name) {
		context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix().withSize(1920, 1080).withDestinationDir(out));
	}
}
