package com.tom.storagemod;

import java.util.List;

import com.tom.storagemod.registry.RegisterBlocks;
import com.tom.storagemod.registry.RegisterMenuTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import com.tom.storagemod.gui.GuiCraftingTerminal;
import com.tom.storagemod.gui.GuiFiltered;
import com.tom.storagemod.gui.GuiInventoryLink;
import com.tom.storagemod.gui.GuiLevelEmitter;
import com.tom.storagemod.gui.GuiStorageTerminal;
import com.tom.storagemod.item.WirelessTerminalItem;
import com.tom.storagemod.model.BakedPaintedModel;
import com.tom.storagemod.blockEntity.PaintedBlockEntity;
import net.minecraftforge.registries.ForgeRegistries;

public class StorageModClient {

	public static void clientSetup() {
		MenuScreens.register(RegisterMenuTypes.STORAGE_TERMINAL, GuiStorageTerminal::new);
		MenuScreens.register(RegisterMenuTypes.CRAFTING_TERMINAL, GuiCraftingTerminal::new);
		MenuScreens.register(RegisterMenuTypes.FILTERED, GuiFiltered::new);
		MenuScreens.register(RegisterMenuTypes.LEVEL_EMITTER, GuiLevelEmitter::new);
		MenuScreens.register(RegisterMenuTypes.INVENTORY_LINK, GuiInventoryLink::new);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(StorageModClient::bakeModels);
		ItemBlockRenderTypes.setRenderLayer(RegisterBlocks.PAINTED_TRIM, e -> true);
		ItemBlockRenderTypes.setRenderLayer(RegisterBlocks.INVENTORY_CABLE_FRAMED, e -> true);
		ItemBlockRenderTypes.setRenderLayer(RegisterBlocks.INVENTORY_PROXY, e -> true);
		ItemBlockRenderTypes.setRenderLayer(RegisterBlocks.INVENTORY_CABLE_CONNECTOR_FRAMED, e -> true);
		ItemBlockRenderTypes.setRenderLayer(RegisterBlocks.LEVEL_EMITTER, RenderType.cutout());
		BlockColors colors = Minecraft.getInstance().getBlockColors();
		colors.register((state, world, pos, tintIndex) -> {
			if (world != null) {
				try {
					BlockState mimicBlock = ((PaintedBlockEntity)world.getBlockEntity(pos)).getPaintedBlockState();
					return colors.getColor(mimicBlock, world, pos, tintIndex);
				} catch (Exception var8) {
					return - 1;
				}
			}
			return -1;
		}, RegisterBlocks.PAINTED_TRIM, RegisterBlocks.INVENTORY_CABLE_FRAMED, RegisterBlocks.INVENTORY_PROXY, RegisterBlocks.INVENTORY_CABLE_CONNECTOR_FRAMED);
		MinecraftForge.EVENT_BUS.addListener(StorageModClient::renderWorldLastEvent);
	}

	private static void bakeModels(ModelEvent.BakingCompleted event) {
		bindPaintedModel(event, RegisterBlocks.PAINTED_TRIM);
		bindPaintedModel(event, RegisterBlocks.INVENTORY_CABLE_FRAMED);
		bindPaintedModel(event, RegisterBlocks.INVENTORY_PROXY);
		bindPaintedModel(event, RegisterBlocks.INVENTORY_CABLE_CONNECTOR_FRAMED);
	}

	private static void bindPaintedModel(ModelEvent.BakingCompleted event, Block block) {
		ResourceLocation baseLoc = ForgeRegistries.BLOCKS.getKey(block);
		block.getStateDefinition().getPossibleStates().forEach(st -> {
			ModelResourceLocation resLoc = BlockModelShaper.stateToModelLocation(baseLoc, st);
			event.getModels().put(resLoc, new BakedPaintedModel(block, event.getModels().get(resLoc)));
		});
	}

	private static void renderWorldLastEvent(RenderLevelLastEvent evt) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if( player == null )
			return;

		if(!WirelessTerminalItem.isPlayerHolding(player))
			return;

		BlockHitResult lookingAt = (BlockHitResult) player.pick(Config.wirelessRange, 0f, true);
		BlockState state = mc.level.getBlockState(lookingAt.getBlockPos());
		if(state.is(StorageTags.REMOTE_ACTIVATE)) {
			BlockPos pos = lookingAt.getBlockPos();
			Vec3 renderPos = mc.gameRenderer.getMainCamera().getPosition();
			PoseStack ms = evt.getPoseStack();
			VertexConsumer buf = mc.renderBuffers().bufferSource().getBuffer(RenderType.lines());
			drawShape(ms, buf, state.getOcclusionShape(player.level, pos), pos.getX() - renderPos.x, pos.getY() - renderPos.y, pos.getZ() - renderPos.z, 1, 1, 1, 0.4f);
			mc.renderBuffers().bufferSource().endBatch(RenderType.lines());
		}
	}

	private static void drawShape(PoseStack matrices, VertexConsumer vertexConsumer, VoxelShape voxelShape, double d, double e, double f, float g, float h, float i, float j) {
		PoseStack.Pose entry = matrices.last();
		voxelShape.forAllEdges((k, l, m, n, o, p) -> {
			float q = (float)(n - k);
			float r = (float)(o - l);
			float s = (float)(p - m);
			float t = Mth.sqrt(q * q + r * r + s * s);
			q /= t;
			r /= t;
			s /= t;
			vertexConsumer.vertex(entry.pose(), (float)(k + d), (float)(l + e), (float)(m + f)).color(g, h, i, j).normal(entry.normal(), q, r, s).endVertex();
			vertexConsumer.vertex(entry.pose(), (float)(n + d), (float)(o + e), (float)(p + f)).color(g, h, i, j).normal(entry.normal(), q, r, s).endVertex();
		});
	}

	public static void tooltip(String key, List<Component> tooltip, Object... args) {
		tooltip(key, true, tooltip, args);
	}

	public static void tooltip(String key, boolean addShift, List<Component> tooltip, Object... args) {
		if(Screen.hasShiftDown()) {
			String[] sp = I18n.get("tooltip.toms_storage." + key, args).split("\\\\");
			for (int i = 0; i < sp.length; i++) {
				tooltip.add(Component.literal(sp[i]));
			}
		} else if(addShift) {
			tooltip.add(Component.translatable("tooltip.toms_storage.hold_shift_for_info").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
		}
	}
}
