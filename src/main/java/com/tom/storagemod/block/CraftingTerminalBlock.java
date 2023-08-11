package com.tom.storagemod.block;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import com.tom.storagemod.StorageModClient;
import com.tom.storagemod.blockEntity.CraftingTerminalBlockEntity;

public class CraftingTerminalBlock extends AbstractStorageTerminal {

	public CraftingTerminalBlock(BlockBehaviour.Properties settings) {
		super(settings);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CraftingTerminalBlockEntity(pos, state);
	}

	@Override
	public void appendHoverText(ItemStack stack, BlockGetter worldIn, List<Component> tooltip,
			TooltipFlag flagIn) {
		StorageModClient.tooltip("crafting_terminal", tooltip);
	}
}
