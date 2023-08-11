package com.tom.storagemod.registry;

import com.tom.storagemod.gui.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.RegisterEvent;

public class RegisterMenuTypes {

    public static final MenuType<StorageTerminalMenu> STORAGE_TERMINAL = new MenuType<>(StorageTerminalMenu::new);
    public static final MenuType<CraftingTerminalMenu> CRAFTING_TERMINAL = new MenuType<>(CraftingTerminalMenu::new);
    public static final MenuType<FilteredMenu> FILTERED = new MenuType<>(FilteredMenu::new);
    public static final MenuType<LevelEmitterMenu> LEVEL_EMITTER = new MenuType<>(LevelEmitterMenu::new);
    public static final MenuType<InventoryLinkMenu> INVENTORY_LINK = new MenuType<>(InventoryLinkMenu::new);

    public static void register(RegisterEvent.RegisterHelper<MenuType<?>> helper) {
        helper.register("storage_terminal.container", STORAGE_TERMINAL);
        helper.register("crafting_terminal.container", CRAFTING_TERMINAL);
        helper.register("filtered.container", FILTERED);
        helper.register("level_emitter.container", LEVEL_EMITTER);
        helper.register("inventory_link.container", INVENTORY_LINK);
    }
}
