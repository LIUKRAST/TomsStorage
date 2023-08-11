package com.tom.storagemod.blockEntity;

//import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;

import com.tom.storagemod.Config;
import com.tom.storagemod.TomsStorage;
import com.tom.storagemod.StoredItemStack;
import com.tom.storagemod.TickerUtil.TickableServer;
import com.tom.storagemod.block.AbstractStorageTerminal;
import com.tom.storagemod.block.AbstractStorageTerminal.TerminalPos;
import com.tom.storagemod.energy.CustomEnergyStorage;
import com.tom.storagemod.gui.StorageTerminalMenu;
import com.tom.storagemod.item.WirelessTerminal;
import com.tom.storagemod.network.EnergyPacket;
import com.tom.storagemod.network.NetworkHandler;
import com.tom.storagemod.registry.RegisterBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class StorageTerminalBlockEntity extends BlockEntity implements MenuProvider, TickableServer/*, IHaveGoggleInformation*/ {
    // Energy
    public int energyClient = -1;
    public int consumtionClient = -1;
    private final CustomEnergyStorage battery = new CustomEnergyStorage(Config.maxEnergyCapacity, 1000, 0);
    private LazyOptional<IEnergyStorage> energy = LazyOptional.of(() -> battery);
    // Items
    private IItemHandler itemHandler;
    private Map<StoredItemStack, Long> items = new HashMap<>();
    private int sort;
    private String lastSearch = "";
    private boolean updateItems;
    private int beaconLevel;

    public StorageTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(RegisterBlockEntityTypes.STORAGE_TERMINAL, pos, state);
    }

    public StorageTerminalBlockEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory plInv, Player arg2) {
        return new StorageTerminalMenu(id, plInv, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("ts.storage_terminal");
    }

    public Map<StoredItemStack, Long> getStacks() {
        updateItems = true;
        return items;
    }

    public StoredItemStack pullStack(StoredItemStack stack, long max) {
        if (stack != null && itemHandler != null && max > 0) {
            ItemStack st = stack.getStack();
            StoredItemStack ret = null;
            for (int i = itemHandler.getSlots() - 1; i >= 0; i--) {
                ItemStack s = itemHandler.getStackInSlot(i);
                if (ItemStack.isSame(s, st) && ItemStack.tagMatches(s, st)) {
                    ItemStack pulled = itemHandler.extractItem(i, (int) max, false);
                    if (!pulled.isEmpty()) {
                        if (ret == null) ret = new StoredItemStack(pulled);
                        else ret.grow(pulled.getCount());
                        max -= pulled.getCount();
                        if (max < 1) break;
                    }
                }
            }
            return ret;
        }
        return null;
    }

    public StoredItemStack pushStack(StoredItemStack stack) {
        if (stack != null && itemHandler != null) {
            ItemStack is = ItemHandlerHelper.insertItemStacked(itemHandler, stack.getActualStack(), false);
            if (is.isEmpty()) return null;
            else {
                return new StoredItemStack(is);
            }
        }
        return stack;
    }

    public ItemStack pushStack(ItemStack itemstack) {
        StoredItemStack is = pushStack(new StoredItemStack(itemstack));
        return is == null ? ItemStack.EMPTY : is.getActualStack();
    }

    public void pushOrDrop(ItemStack st) {
        if (st.isEmpty()) return;
        StoredItemStack st0 = pushStack(new StoredItemStack(st));
        if (st0 != null) {
            Containers.dropItemStack(level, worldPosition.getX() + .5f, worldPosition.getY() + .5f, worldPosition.getZ() + .5f, st0.getActualStack());
        }
    }

    @Override
    public void updateServer() {
        // Energy
        pullEnergy();
        battery.consume(getConsumtion());
        if (energyClient != battery.getEnergyStored() || consumtionClient != getConsumtion()) {
            setChanged();
            NetworkHandler.sendToAll(level, worldPosition, new EnergyPacket(worldPosition, getEnergyStored(), getConsumtion()));
            energyClient = battery.getEnergyStored();
            consumtionClient = getConsumtion();
        }

        // Items
        if (updateItems) {
            BlockState st = level.getBlockState(worldPosition);
            Direction d = st.getValue(AbstractStorageTerminal.FACING);
            TerminalPos p = st.getValue(AbstractStorageTerminal.TERMINAL_POS);
            if (p == TerminalPos.UP) d = Direction.UP;
            if (p == TerminalPos.DOWN) d = Direction.DOWN;
            BlockEntity invTile = level.getBlockEntity(worldPosition.relative(d));
            items.clear();
            if (invTile != null) {
                LazyOptional<IItemHandler> lih = invTile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, d.getOpposite());
                itemHandler = lih.orElse(null);
                if (itemHandler != null) {
                    IntStream.range(0, itemHandler.getSlots()).mapToObj(itemHandler::getStackInSlot).filter(s -> !s.isEmpty()).
                            map(StoredItemStack::new).forEach(s -> items.merge(s, s.getQuantity(), (a, b) -> a + b));
                }
            }
            updateItems = false;
        }
        if (level.getGameTime() % 40 == 5) {
            beaconLevel = BlockPos.betweenClosedStream(new AABB(worldPosition).inflate(8)).mapToInt(p -> {
                if (level.isLoaded(p)) {
                    BlockState st = level.getBlockState(p);
                    if (st.is(Blocks.BEACON)) {
                        return InventoryCableConnectorBlockEntity.calcBeaconLevel(level, p.getX(), p.getY(), p.getZ());
                    }
                }
                return 0;
            }).max().orElse(0);
        }
    }

    public boolean canInteractWith(Player player) {
        if (level.getBlockEntity(worldPosition) != this) return false;
        int d = 4;
        int termReach = 0;
        if (player.getMainHandItem().getItem() instanceof WirelessTerminal)
            termReach = Math.max(termReach, ((WirelessTerminal) player.getMainHandItem().getItem()).getRange(player, player.getMainHandItem()));
        if (player.getOffhandItem().getItem() instanceof WirelessTerminal)
            termReach = Math.max(termReach, ((WirelessTerminal) player.getOffhandItem().getItem()).getRange(player, player.getOffhandItem()));
        if (beaconLevel >= Config.wirelessTermBeaconLvl && termReach > 0) {
            if (beaconLevel >= Config.wirelessTermBeaconLvlDim) return true;
            else return player.getLevel() == level;
        }
        d = Math.max(d, termReach);
        return player.getLevel() == level && !(player.distanceToSqr(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D, this.worldPosition.getZ() + 0.5D) > d * 2 * d * 2);
    }

    public int getSorting() {
        return sort;
    }

    public void setSorting(int newC) {
        sort = newC;
    }

    @Override
    public void saveAdditional(CompoundTag compound) {
        compound.putInt("sort", sort);
        compound.putInt("consumtion", getConsumtion());
        battery.writeToNBT(compound);
    }

    @Override
    public void load(CompoundTag compound) {
        sort = compound.getInt("sort");
        battery.readFromNBT(compound);
    }

    public String getLastSearch() {
        return lastSearch;
    }

    public void setLastSearch(String string) {
        lastSearch = string;
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction facing) {
        if (capability == CapabilityEnergy.ENERGY) return energy.cast();
        return super.getCapability(capability, facing);
    }

    private void pullEnergy() {
        for (int i = 0; (i < Direction.values().length) && (battery.getEnergyStored() < battery.getMaxEnergyStored()); i++) {
            Direction facing = Direction.values()[i];
            BlockEntity tileEntity = level.getBlockEntity(worldPosition.relative(facing));
            if (tileEntity != null) {
                tileEntity.getCapability(CapabilityEnergy.ENERGY, facing.getOpposite()).ifPresent(handler -> {
                    if (handler.canExtract()) {
                        int space = battery.getMaxEnergyStored() - battery.getEnergyStored();
                        int received = handler.extractEnergy(Math.min(battery.getMaxReceive(), space), false);
                        battery.modifyEnergyStored(received);
                        setChanged();
                    }
                });
            }
        }
    }

    public boolean hasEnergy() {
        return battery.getEnergyStored() > getConsumtion();
    }

    public int getEnergyStored() {
        return battery.getEnergyStored();
    }

    public int getConsumtion() {
        return Config.baseEnergyConsumption + (int) (Config.energyConsumptionPerItem * items.size());
    }

    /*@Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(componentSpacing.plainCopy().append(new TranslatableComponent("toms_storage.tooltip.consume_energy", getConsumtion())));
        tooltip.add(componentSpacing.plainCopy().append(new TranslatableComponent("toms_storage.tooltip.stored_energy", getEnergyStored())));
        return true;
    }*/
}
