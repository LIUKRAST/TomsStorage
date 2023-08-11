package com.tom.storagemod.block;

import java.util.List;

import com.tom.storagemod.registry.RegisterBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import com.tom.storagemod.TomsStorage;
import com.tom.storagemod.StorageModClient;

public class TrimBlock extends Block implements ITrim, IPaintable {

	public TrimBlock(BlockBehaviour.Properties settings) {
		super(settings);
	}

	@Override
	public void appendHoverText(ItemStack stack, BlockGetter worldIn, List<Component> tooltip,
			TooltipFlag flagIn) {
		tooltip.add(Component.translatable("tooltip.toms_storage.paintable"));
		StorageModClient.tooltip("trim", tooltip);
	}

	//TODO: Make customizable
	@Override
	public boolean paint(Level world, BlockPos pos, BlockState to) {
		world.setBlockAndUpdate(pos, RegisterBlocks.PAINTED_TRIM.defaultBlockState());
		return RegisterBlocks.PAINTED_TRIM.paint(world, pos, to);
	}
}
