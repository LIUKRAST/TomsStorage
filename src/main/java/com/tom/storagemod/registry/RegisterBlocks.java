package com.tom.storagemod.registry;

import com.tom.storagemod.TomsStorage;
import com.tom.storagemod.block.*;
import com.tom.storagemod.item.BlockPaintedItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.RegisterEvent;

import java.util.function.Function;
import java.util.function.Supplier;

import static com.tom.storagemod.TomsStorage.RegistryEvents.resource;

public class RegisterBlocks {

    /*
    * Note: Please use the constructor passing (settings) instead of configuring everything this bad.
    * This allows other developers to easily create addons without having to clone classes just because of that
    */

    //A default property, used many times then. If you dont like it, select "DEFAULT" and press "inline variable"
    private static final BlockBehaviour.Properties DEFAULT = BlockBehaviour.Properties.of(Material.WOOD).strength(3);

    public static final InventoryConnectorBlock INVENTORY_CONNECTOR = new InventoryConnectorBlock(DEFAULT);
    public static final StorageTerminalBlock STORAGE_TERMINAL = new StorageTerminalBlock(DEFAULT.lightLevel(s -> 6));
    public static final TrimBlock INVENTORY_TRIM = new TrimBlock(DEFAULT);
    public static final OpenCrateBlock OPEN_CRATE = new OpenCrateBlock(DEFAULT);
    public static final PaintedTrimBlock PAINTED_TRIM = new PaintedTrimBlock(DEFAULT);
    public static final InventoryCableBlock INVENTORY_CABLE = new InventoryCableBlock(0.125f, Block.Properties.of(Material.WOOD).strength(2));
    public static final InventoryCableFramedBlock INVENTORY_CABLE_FRAMED = new InventoryCableFramedBlock(Block.Properties.of(Material.WOOD).strength(2).noOcclusion());
    public static final InventoryCableConnectorBlock INVENTORY_CABLE_CONNECTOR = new InventoryCableConnectorBlock(DEFAULT.noOcclusion());
    public static final InventoryCableConnectorFilteredBlock INVENTORY_CABLE_CONNECTOR_FILTERED = new InventoryCableConnectorFilteredBlock(DEFAULT.noOcclusion());
    public static final InventoryCableConnectorFramedBlock INVENTORY_CABLE_CONNECTOR_FRAMED = new InventoryCableConnectorFramedBlock(DEFAULT.noOcclusion());
    public static final InventoryProxyBlock INVENTORY_PROXY = new InventoryProxyBlock(DEFAULT);
    public static final CraftingTerminalBlock CRAFTING_TERMINAL = new CraftingTerminalBlock(DEFAULT.lightLevel(s -> 6));
    public static final InventoryHopperBasicBlock INVENTORY_HOPPER_BASIC = new InventoryHopperBasicBlock(DEFAULT.noOcclusion());
    public static final LevelEmitterBlock LEVEL_EMITTER = new LevelEmitterBlock(DEFAULT.noOcclusion());

    public static void register(RegisterEvent.RegisterHelper<Block> helper) {
        withItem(helper, resource("inventory_connector"), INVENTORY_CONNECTOR);
        withItem(helper, resource("storage_terminal"), STORAGE_TERMINAL);
        withItem(helper, resource("trim"), INVENTORY_TRIM);
        withItem(helper, resource("open_crate"), OPEN_CRATE);
        withItem(helper, resource("painted_trim"), PAINTED_TRIM, BlockPaintedItem::new);
        withItem(helper, resource("inventory_cable"), INVENTORY_CABLE);
        withItem(helper, resource("inventory_cable_framed"), INVENTORY_CABLE_FRAMED, BlockPaintedItem::new);
        withItem(helper, resource("inventory_cable_connector"), INVENTORY_CABLE_CONNECTOR);
        withItem(helper, resource("inventory_cable_connector_filtered"), INVENTORY_CABLE_CONNECTOR_FILTERED);
        withItem(helper, resource("inventory_cable_connector_framed"), INVENTORY_CABLE_CONNECTOR_FRAMED);
        withItem(helper, resource("inventory_proxy"), INVENTORY_PROXY, BlockPaintedItem::new);
        withItem(helper, resource("crafting_terminal"), CRAFTING_TERMINAL);
        withItem(helper, resource("inventory_hopper_basic"), INVENTORY_HOPPER_BASIC);
        helper.register(resource("level_emitter"), LEVEL_EMITTER);
    }

    // Default Property for BlockItems
    private static final Item.Properties TEMP = new Item.Properties().tab(TomsStorage.STORAGE_MOD_TAB);


    public static void withItem(RegisterEvent.RegisterHelper<Block> helper, String id, Block block, Supplier<BlockItem> supplier) {
        helper.register(id, block);
        RegisterItems.ASYNC_BLOCK_ITEMS.put(id, supplier.get());
    }

    public static void withItem(RegisterEvent.RegisterHelper<Block> helper, String id, Block block, Function<Item.Properties, BlockItem> supplier) {
        helper.register(id, block);
        RegisterItems.ASYNC_BLOCK_ITEMS.put(id, supplier.apply(TEMP));
    }

    public static void withItem(RegisterEvent.RegisterHelper<Block> helper, String id, Block block, Item.Properties settings) {
        withItem(helper, id, block, () -> new BlockItem(block, settings));
    }

    public static void withItem(RegisterEvent.RegisterHelper<Block> helper, String id, Block block) {
        withItem(helper, id, block, TEMP);
    }

    private interface Executor {
        BlockItem supply(Block block, Item.Properties settings);
    }

    public static void withItem(RegisterEvent.RegisterHelper<Block> helper, String id, Block block, Executor executor) {
        withItem(helper, id, block, () -> executor.supply(block, TEMP));
    }

}
