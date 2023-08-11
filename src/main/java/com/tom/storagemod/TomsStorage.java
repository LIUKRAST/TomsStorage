package com.tom.storagemod;

import com.tom.storagemod.gui.*;
import com.tom.storagemod.network.NetworkHandler;
import com.tom.storagemod.registry.RegisterBlockEntityTypes;
import com.tom.storagemod.registry.RegisterBlocks;
import com.tom.storagemod.registry.RegisterMenuTypes;
import com.tom.storagemod.registry.RegisterItems;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(TomsStorage.MOD_ID)
public class TomsStorage {
	public static final String MOD_ID = "toms_storage";
	public static final Logger LOGGER = LogManager.getLogger();

	public TomsStorage() {
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.commonSpec);
		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.serverSpec);
		FMLJavaModLoadingContext.get().getModEventBus().register(Config.class);
		MinecraftForge.EVENT_BUS.register(this);
	}

	private void setup(final FMLCommonSetupEvent event) {
		LOGGER.info("Tom's Storage Setup starting");
		NetworkHandler.init();
	}

	private void doClientStuff(final FMLClientSetupEvent event) {
		StorageModClient.clientSetup();
	}

	public static final CreativeModeTab STORAGE_MOD_TAB = new CreativeModeTab("toms_storage.tab") {

		@Override
		@OnlyIn(Dist.CLIENT)
		public ItemStack makeIcon() {
			return new ItemStack(RegisterBlocks.STORAGE_TERMINAL);
		}
	};


	@Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
	public static class RegistryEvents {
		@SubscribeEvent
		public static void register(final RegisterEvent event) {
			//Calls several classes to register stuff instead of PUTTING EVERYTHING HERE LIKE WTF?
			event.register(ForgeRegistries.Keys.BLOCKS, RegisterBlocks::register);
			event.register(ForgeRegistries.Keys.ITEMS, RegisterItems::register);
			event.register(ForgeRegistries.Keys.BLOCK_ENTITY_TYPES, RegisterBlockEntityTypes::register);
			event.register(ForgeRegistries.Keys.MENU_TYPES, RegisterMenuTypes::register);
		}

		public static String resource(String resource) {
			return "ts." + resource;
		}
	}
}
