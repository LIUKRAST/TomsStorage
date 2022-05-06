package com.tom.storagemod.gui;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import com.tom.storagemod.StorageMod;
import com.tom.storagemod.network.IDataReceiver;
import com.tom.storagemod.network.NetworkHandler;
import com.tom.storagemod.tile.TileEntityInventoryCableConnector;
import com.tom.storagemod.util.RemoteConnections;

public class ContainerInventoryLink extends AbstractContainerMenu implements IDataReceiver {
	private TileEntityInventoryCableConnector te;
	private Inventory pinv;
	private boolean sentList;

	public ContainerInventoryLink(int id, Inventory playerInv) {
		this(id, playerInv, null);
	}

	public ContainerInventoryLink(int id, Inventory playerInv, TileEntityInventoryCableConnector tile) {
		super(StorageMod.inventoryLink, id);
		this.te = tile;
		this.pinv = playerInv;
	}

	@Override
	public void receive(CompoundTag tag) {
		if(pinv.player.isSpectator() || te == null)return;
		if(tag.contains("remote")) {
			te.setRemote(tag.getBoolean("remote"));
			sentList = false;
			return;
		}
		UUID id = null;
		if(tag.contains("id"))id = tag.getUUID("id");
		if(id == null) {
			UUID chn = RemoteConnections.get(pinv.player.level).makeChannel(tag, pinv.player.getUUID());
			te.setChannel(chn);
		} else if(tag.getBoolean("select")) {
			te.setChannel(id);
		} else if(tag.contains(RemoteConnections.PUBLIC_TAG)) {
			RemoteConnections.get(pinv.player.level).editChannel(id, tag.getBoolean(RemoteConnections.PUBLIC_TAG), pinv.player.getUUID());
		} else {
			RemoteConnections.get(pinv.player.level).removeChannel(id, pinv.player.getUUID());
		}
		sentList = false;
	}

	@Override
	public boolean stillValid(Player p_38874_) {
		return te != null ? te.stillValid(p_38874_) : true;
	}

	@Override
	public void broadcastChanges() {
		if(te == null)return;
		if(!sentList) {
			CompoundTag mainTag = new CompoundTag();
			UUID chn = te.getChannel();
			if(chn != null)
				mainTag.putUUID("selected", chn);

			mainTag.put("list", RemoteConnections.get(pinv.player.level).listChannels(pinv.player));
			mainTag.putInt("lvl", te.getBeaconLevel());
			mainTag.putBoolean("remote", te.isRemote());

			NetworkHandler.sendTo((ServerPlayer) pinv.player, mainTag);
			sentList = true;
		}
		super.broadcastChanges();
	}
}