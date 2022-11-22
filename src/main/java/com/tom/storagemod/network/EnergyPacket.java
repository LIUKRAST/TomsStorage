package com.tom.storagemod.network;

import com.tom.storagemod.tile.TileEntityStorageTerminal;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EnergyPacket {
	private final BlockPos pos;
	private final int currentEnergy;
	private final int consumption;

	public EnergyPacket(FriendlyByteBuf buf) {
		pos = buf.readBlockPos();
		currentEnergy = buf.readInt();
		consumption = buf.readInt();
	}

	public EnergyPacket(BlockPos pos, int currentEnergy, int consumption) {
		this.pos = pos;
		this.currentEnergy = currentEnergy;
		this.consumption = consumption;
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeBlockPos(pos);
		buf.writeInt(currentEnergy);
		buf.writeInt(consumption);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		if(ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT)
			UpdateSolarPanelClient.handle(this, ctx);
		ctx.get().setPacketHandled(true);
	}

	public static class UpdateSolarPanelClient {
		public static void handle(EnergyPacket packet, Supplier<NetworkEvent.Context> ctx) {
			var level = Minecraft.getInstance().level;
			if(level != null && level.isLoaded(packet.pos)) {
				if(level.getBlockEntity(packet.pos) instanceof TileEntityStorageTerminal terminal) {
					terminal.energyClient = packet.currentEnergy;
					terminal.consumtionClient = packet.consumption;
				}
			}
		}
	}
}
