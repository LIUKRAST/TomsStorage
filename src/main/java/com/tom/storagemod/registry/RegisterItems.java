package com.tom.storagemod.registry;

import com.tom.storagemod.TomsStorage;
import com.tom.storagemod.item.AdvWirelessTerminalItem;
import com.tom.storagemod.item.PaintKitItem;
import com.tom.storagemod.item.WirelessTerminalItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegisterEvent;

import java.util.HashMap;
import java.util.Map;

import static com.tom.storagemod.TomsStorage.RegistryEvents.resource;

public class RegisterItems {

    //Check com.tom.storagemod.registry.RegisterBlocks for info

    public static final PaintKitItem PAINTING_KIT = new PaintKitItem(new Item.Properties().durability(100).tab(TomsStorage.STORAGE_MOD_TAB));
    public static final WirelessTerminalItem WIRELESS_TERMINAL = new WirelessTerminalItem(new Item.Properties().tab(TomsStorage.STORAGE_MOD_TAB).stacksTo(1));
    public static final AdvWirelessTerminalItem ADV_WIRELESS_TERMINAL = new AdvWirelessTerminalItem(new Item.Properties().tab(TomsStorage.STORAGE_MOD_TAB).stacksTo(1));

    //Automatically subscribe block items for later
    public static final Map<String, BlockItem> ASYNC_BLOCK_ITEMS = new HashMap<>();

    public static void register(RegisterEvent.RegisterHelper<Item> helper) {
        helper.register(resource("paint_kit"), PAINTING_KIT);
        helper.register(resource("wireless_terminal"), WIRELESS_TERMINAL);
        helper.register(resource("adv_wireless_terminal"), ADV_WIRELESS_TERMINAL);

        //Block items will be registered after all. It could be configurable with an index order, but im too lazy to do it rn
        ASYNC_BLOCK_ITEMS.forEach(helper::register);
    }
}
