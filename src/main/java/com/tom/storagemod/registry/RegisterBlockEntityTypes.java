package com.tom.storagemod.registry;

import com.tom.storagemod.blockEntity.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegisterEvent;

import static com.tom.storagemod.TomsStorage.RegistryEvents.resource;

public class RegisterBlockEntityTypes {

    public static final BlockEntityType<InventoryConnectorBlockEntity> INVENTORY_CONNECTOR = BlockEntityType.Builder.of(InventoryConnectorBlockEntity::new, RegisterBlocks.INVENTORY_CONNECTOR).build(null);;
    public static final BlockEntityType<StorageTerminalBlockEntity> STORAGE_TERMINAL = BlockEntityType.Builder.of(StorageTerminalBlockEntity::new, RegisterBlocks.STORAGE_TERMINAL).build(null);;
    public static final BlockEntityType<OpenCrateBlockEntity> OPEN_CRATE = BlockEntityType.Builder.of(OpenCrateBlockEntity::new, RegisterBlocks.OPEN_CRATE).build(null);;
    public static final BlockEntityType<PaintedBlockEntity> PAINTED = BlockEntityType.Builder.of(PaintedBlockEntity::new, RegisterBlocks.PAINTED_TRIM, RegisterBlocks.INVENTORY_CABLE_FRAMED).build(null);
    public static final BlockEntityType<InventoryCableConnectorBlockEntity> INVENTORY_CABLE_CONNECTOR = BlockEntityType.Builder.of(InventoryCableConnectorBlockEntity::new, RegisterBlocks.INVENTORY_CABLE_CONNECTOR, RegisterBlocks.INVENTORY_CABLE_CONNECTOR_FRAMED).build(null);;
    public static final BlockEntityType<InventoryCableConnectorFilteredBlockEntity> INVENTORY_CABLE_CONNECTOR_FILTERED = BlockEntityType.Builder.of(InventoryCableConnectorFilteredBlockEntity::new, RegisterBlocks.INVENTORY_CABLE_CONNECTOR_FILTERED).build(null);
    public static final BlockEntityType<InventoryProxyBlockEntity> INVENTORY_PROXY = BlockEntityType.Builder.of(InventoryProxyBlockEntity::new, RegisterBlocks.INVENTORY_PROXY).build(null);
    public static final BlockEntityType<CraftingTerminalBlockEntity> CRAFTING_TERMINAL = BlockEntityType.Builder.of(CraftingTerminalBlockEntity::new, RegisterBlocks.CRAFTING_TERMINAL).build(null);
    public static final BlockEntityType<InventoryHopperBasicBlockEntity> INVENTORY_HOPPER_BASIC = BlockEntityType.Builder.of(InventoryHopperBasicBlockEntity::new, RegisterBlocks.INVENTORY_HOPPER_BASIC).build(null);
    public static final BlockEntityType<LevelEmitterBlockEntity> LEVEL_EMITTER = BlockEntityType.Builder.of(LevelEmitterBlockEntity::new, RegisterBlocks.LEVEL_EMITTER).build(null);
    
    public static void register(RegisterEvent.RegisterHelper<BlockEntityType<?>> helper) {
        helper.register(resource("inventory_connector.tile"), INVENTORY_CONNECTOR);
        helper.register(resource("storage_terminal.tile"), STORAGE_TERMINAL);
        helper.register(resource("open_crate.tile"), OPEN_CRATE);
        helper.register(resource("painted.tile"), PAINTED);
        helper.register(resource("inventory_cable_connector.tile"), INVENTORY_CABLE_CONNECTOR);
        helper.register(resource("inventory_cable_connector_filtered.tile"), INVENTORY_CABLE_CONNECTOR_FILTERED);
        helper.register(resource("inventory_proxy.tile"), INVENTORY_PROXY);
        helper.register(resource("crafting_terminal.tile"), CRAFTING_TERMINAL);
        helper.register(resource("inventoty_hopper_basic.tile"), INVENTORY_HOPPER_BASIC);
        helper.register(resource("level_emitter.tile"), LEVEL_EMITTER);
    }
}
