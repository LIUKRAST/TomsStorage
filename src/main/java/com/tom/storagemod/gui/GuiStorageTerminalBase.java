package com.tom.storagemod.gui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.tom.storagemod.Config;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import net.minecraftforge.fml.ModList;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import com.tom.storagemod.StoredItemStack;
import com.tom.storagemod.StoredItemStack.ComparatorAmount;
import com.tom.storagemod.StoredItemStack.IStoredItemStackComparator;
import com.tom.storagemod.StoredItemStack.SortingTypes;
import com.tom.storagemod.gui.StorageTerminalMenu.SlotAction;
import com.tom.storagemod.gui.StorageTerminalMenu.SlotStorage;
import com.tom.storagemod.jei.JEIHandler;
import com.tom.storagemod.network.IDataReceiver;

public abstract class GuiStorageTerminalBase<T extends StorageTerminalMenu> extends AbstractContainerScreen<T> implements IDataReceiver {
	private static final LoadingCache<StoredItemStack, List<String>> tooltipCache = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.SECONDS).build(new CacheLoader<StoredItemStack, List<String>>() {

		@Override
		public List<String> load(StoredItemStack key) throws Exception {
			return key.getStack().getTooltipLines(Minecraft.getInstance().player, getTooltipFlag()).stream().map(Component::getString).collect(Collectors.toList());
		}

	});
	protected Minecraft mc = Minecraft.getInstance();

	/** Amount scrolled in Creative mode inventory (0 = top, 1 = bottom) */
	protected float currentScroll;
	/** True if the scrollbar is being dragged */
	protected boolean isScrolling;
	/**
	 * True if the left mouse button was held down last time drawScreen was
	 * called.
	 */
	private boolean refreshItemList;
	protected boolean wasClicking;
	protected EditBox searchField;
	protected int slotIDUnderMouse = -1, controllMode, rowCount, searchType;
	private String searchLast = "";
	protected boolean loadedSearch = false;
	private IStoredItemStackComparator comparator = new ComparatorAmount(false);
	protected static final ResourceLocation creativeInventoryTabs = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");
	protected GuiButton buttonSortingType, buttonDirection, buttonSearchType, buttonCtrlMode;

	private int storedEnergy = 0;
	private int consumtion = 0;

	public GuiStorageTerminalBase(T screenContainer, Inventory inv, Component titleIn) {
		super(screenContainer, inv, titleIn);
		screenContainer.onPacket = this::onPacket;
	}

	protected void onPacket() {
		int s = menu.terminalData;
		controllMode = (s & 0b000_00_0_11) % ControllMode.VALUES.length;
		boolean rev = (s & 0b000_00_1_00) > 0;
		int type = (s & 0b000_11_0_00) >> 3;
		comparator = SortingTypes.VALUES[type % SortingTypes.VALUES.length].create(rev);
		searchType = (s & 0b111_00_0_00) >> 5;
		if(!searchField.isFocused() && (searchType & 1) > 0) {
			searchField.setFocus(true);
		}
		buttonSortingType.state = type;
		buttonDirection.state = rev ? 1 : 0;
		buttonSearchType.state = searchType;
		buttonCtrlMode.state = controllMode;

		if(!loadedSearch && menu.search != null) {
			loadedSearch = true;
			if((searchType & 2) > 0)
				searchField.setValue(menu.search);
		}
	}

	protected void sendUpdate() {
		CompoundTag c = new CompoundTag();
		c.putInt("d", updateData());
		CompoundTag msg = new CompoundTag();
		msg.put("c", c);
		menu.sendMessage(msg);
	}

	protected int updateData() {
		int d = 0;
		d |= (controllMode & 0b000_0_11);
		d |= ((comparator.isReversed() ? 1 : 0) << 2);
		d |= (comparator.type() << 3);
		d |= ((searchType & 0b111) << 5);
		return d;
	}

	@Override
	protected void init() {
		clearWidgets();
		inventoryLabelY = imageHeight - 92;
		super.init();
		this.searchField = new EditBox(getFont(), this.leftPos + 82, this.topPos + 6, 89, this.getFont().lineHeight, Component.translatable("narrator.toms_storage.terminal_search"));
		this.searchField.setMaxLength(100);
		this.searchField.setBordered(false);
		this.searchField.setVisible(true);
		this.searchField.setTextColor(16777215);
		this.searchField.setValue(searchLast);
		searchLast = "";
		addRenderableWidget(searchField);
		buttonSortingType = addRenderableWidget(new GuiButton(leftPos - 18, topPos + 5, 0, b -> {
			comparator = SortingTypes.VALUES[(comparator.type() + 1) % SortingTypes.VALUES.length].create(comparator.isReversed());
			buttonSortingType.state = comparator.type();
			sendUpdate();
			refreshItemList = true;
		}));
		buttonDirection = addRenderableWidget(new GuiButton(leftPos - 18, topPos + 5 + 18, 1, b -> {
			comparator.setReversed(!comparator.isReversed());
			buttonDirection.state = comparator.isReversed() ? 1 : 0;
			sendUpdate();
			refreshItemList = true;
		}));
		buttonSearchType = addRenderableWidget(new GuiButton(leftPos - 18, topPos + 5 + 18*2, 2, b -> {
			searchType = (searchType + 1) & ((ModList.get().isLoaded("jei") || this instanceof GuiCraftingTerminal) ? 0b111 : 0b011);
			buttonSearchType.state = searchType;
			sendUpdate();
		}) {
			@Override
			public void renderButton(PoseStack st, int mouseX, int mouseY, float pt) {
				if (this.visible) {
					RenderSystem.setShader(GameRenderer::getPositionTexShader);
					RenderSystem.setShaderTexture(0, getGui());
					this.isHovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
					//int i = this.getYImage(this.isHovered);
					RenderSystem.enableBlend();
					RenderSystem.defaultBlendFunc();
					RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
					this.blit(st, this.x, this.y, texX, texY + tile * 16, this.width, this.height);
					if((state & 1) > 0)this.blit(st, this.x+1, this.y+1, texX + 16, texY + tile * 16, this.width-2, this.height-2);
					if((state & 2) > 0)this.blit(st, this.x+1, this.y+1, texX + 16+14, texY + tile * 16, this.width-2, this.height-2);
					if((state & 4) > 0)this.blit(st, this.x+1, this.y+1, texX + 16+14*2, texY + tile * 16, this.width-2, this.height-2);
				}
			}
		});
		buttonCtrlMode = addRenderableWidget(new GuiButton(leftPos - 18, topPos + 5 + 18*3, 3, b -> {
			controllMode = (controllMode + 1) % ControllMode.VALUES.length;
			buttonCtrlMode.state = controllMode;
			sendUpdate();
		}));
		updateSearch();
	}

	protected void updateSearch() {
		String searchString = searchField.getValue();
		if (refreshItemList || !searchLast.equals(searchString)) {
			getMenu().itemListClientSorted.clear();
			boolean searchMod = false;
			String search = searchString;
			if (searchString.startsWith("@")) {
				searchMod = true;
				search = searchString.substring(1);
			}
			Pattern m;
			try {
				m = Pattern.compile(search.toLowerCase(), Pattern.CASE_INSENSITIVE);
			} catch (Throwable ignore) {
				try {
					m = Pattern.compile(Pattern.quote(search.toLowerCase()), Pattern.CASE_INSENSITIVE);
				} catch (Throwable __) {
					return;
				}
			}
			boolean notDone;
			try {
				for (int i = 0;i < getMenu().itemListClient.size();i++) {
					StoredItemStack is = getMenu().itemListClient.get(i);
					if (is != null && is.getStack() != null) {
						//
						String dspName = searchMod ? ForgeRegistries.ITEMS.getKey(is.getStack().getItem()).getNamespace() : is.getStack().getHoverName().getString();
						notDone = true;
						if (m.matcher(dspName.toLowerCase()).find()) {
							addStackToClientList(is);
							notDone = false;
						}
						if (notDone) {
							for (String lp : tooltipCache.get(is)) {
								if (m.matcher(lp).find()) {
									addStackToClientList(is);
									notDone = false;
									break;
								}
							}
						}
					}
				}
			} catch (Exception ignored) {}
			getMenu().itemListClientSorted.sort(comparator);
			if(!searchLast.equals(searchString)) {
				getMenu().scrollTo(0);
				this.currentScroll = 0;
				if ((searchType & 4) > 0) {
					if(ModList.get().isLoaded("jei"))
						JEIHandler.setJeiSearchText(searchString);
				}
				if ((searchType & 2) > 0) {
					CompoundTag nbt = new CompoundTag();
					nbt.putString("s", searchString);
					menu.sendMessage(nbt);
				}
				onUpdateSearch(searchString);
			} else {
				getMenu().scrollTo(this.currentScroll);
			}
			refreshItemList = false;
			this.searchLast = searchString;
		}
	}

	private void addStackToClientList(StoredItemStack is) {
		getMenu().itemListClientSorted.add(is);
	}

	public static TooltipFlag getTooltipFlag(){
		return Minecraft.getInstance().options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL;
	}

	@Override
	protected void containerTick() {
		updateSearch();
	}

	@Override
	public void render(PoseStack st, int mouseX, int mouseY, float partialTicks) {
		boolean flag = GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_RELEASE;
		int i = this.leftPos;
		int j = this.topPos;
		int k = i + 174;
		int l = j + 18;
		int i1 = k + 14;
		int j1 = l + rowCount * 18;

		if (!this.wasClicking && flag && mouseX >= k && mouseY >= l && mouseX < i1 && mouseY < j1) {
			this.isScrolling = this.needsScrollBars();
		}

		if (!flag) {
			this.isScrolling = false;
		}
		this.wasClicking = flag;

		if (this.isScrolling) {
			this.currentScroll = (mouseY - l - 7.5F) / (j1 - l - 15.0F);
			this.currentScroll = Mth.clamp(this.currentScroll, 0.0F, 1.0F);
			getMenu().scrollTo(this.currentScroll);
		}
		super.render(st, mouseX, mouseY, partialTicks);

		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		//DiffuseLighting.disableGuiDepthLighting();
		RenderSystem.setShaderTexture(0, creativeInventoryTabs);
		i = k;
		j = l;
		k = j1;
		this.blit(st, i, j + (int) ((k - j - 17) * this.currentScroll), 232 + (this.needsScrollBars() ? 0 : 12), 0, 12, 15);
		st.pushPose();
		//RenderHelper.turnBackOn();
		slotIDUnderMouse = drawSlots(st, mouseX, mouseY);
		st.popPose();
		this.renderTooltip(st, mouseX, mouseY);

		if (buttonSortingType.isHoveredOrFocused()) {
			renderTooltip(st, Component.translatable("tooltip.toms_storage.sorting_" + buttonSortingType.state), mouseX, mouseY);
		}
		if (buttonSearchType.isHoveredOrFocused()) {
			renderTooltip(st, Component.translatable("tooltip.toms_storage.search_" + buttonSearchType.state, "JEI"), mouseX, mouseY);
		}
		if (buttonCtrlMode.isHoveredOrFocused()) {
			renderComponentTooltip(st, Arrays.stream(I18n.get("tooltip.toms_storage.ctrlMode_" + buttonCtrlMode.state).split("\\\\")).map(Component::literal).collect(Collectors.toList()), mouseX, mouseY);
		}

		int h = this instanceof GuiCraftingTerminal ? 5 : 4;
		Component text1 = Component.translatable("toms_storage.tooltip.stored_energy", storedEnergy);
		font.draw(st, text1, leftPos - 20 - font.width(text1), topPos + 18*h + 20, 0xFFFFFF);
		Component text2 = Component.translatable("toms_storage.tooltip.consume_energy", consumtion);
		font.draw(st, text2, leftPos - 20 - font.width(text2), topPos + 18*h + 34, 0xFFFFFF);

		RenderSystem.setShaderTexture(0, getGui());
		this.blit(st, leftPos - 17, topPos + 18*h + 5, 227, 10, 14, 52);
		int y = getEnergyStoredScaled(50);
		this.blit(st, leftPos - 16, topPos + 18*h + 56 - y, 242, 11, 14, y);
	}

	protected int drawSlots(PoseStack st, int mouseX, int mouseY) {
		StorageTerminalMenu term = getMenu();
		for (int i = 0;i < term.storageSlotList.size();i++) {
			drawSlot(st, term.storageSlotList.get(i), mouseX, mouseY);
		}
		RenderSystem.disableDepthTest();
		RenderSystem.disableBlend();
		st.pushPose();
		st.translate(0, 0, 100);
		for (int i = 0;i < term.storageSlotList.size();i++) {
			if (drawTooltip(st, term.storageSlotList.get(i), mouseX, mouseY)) {
				st.popPose();
				return i;
			}
		}
		st.popPose();
		return -1;
	}

	protected void drawSlot(PoseStack st, SlotStorage slot, int mouseX, int mouseY) {
		if (mouseX >= getGuiLeft() + slot.xDisplayPosition - 1 && mouseY >= getGuiTop() + slot.yDisplayPosition - 1 && mouseX < getGuiLeft() + slot.xDisplayPosition + 17 && mouseY < getGuiTop() + slot.yDisplayPosition + 17) {
			int l = getGuiLeft() + slot.xDisplayPosition;
			int t = getGuiTop() + slot.yDisplayPosition;
			fill(st, l, t, l+16, t+16, 0x80FFFFFF);

		}
		if (slot.stack != null) {
			st.pushPose();
			renderItemInGui(st, slot.stack.getStack().copy().split(1), getGuiLeft() + slot.xDisplayPosition, getGuiTop() + slot.yDisplayPosition, 0, 0, false, 0xFFFFFF, false);
			Font r = getFont();
			drawStackSize(st, r, slot.stack.getQuantity(), getGuiLeft() + slot.xDisplayPosition, getGuiTop() + slot.yDisplayPosition);
			st.popPose();
		}
	}

	protected boolean drawTooltip(PoseStack st, SlotStorage slot, int mouseX, int mouseY) {
		if (slot.stack != null) {
			if (slot.stack.getQuantity() > 9999) {
				renderItemInGui(st, slot.stack.getStack(), getGuiLeft() + slot.xDisplayPosition, getGuiTop() + slot.yDisplayPosition, mouseX, mouseY, false, 0, true, I18n.get("tooltip.toms_storage.amount", slot.stack.getQuantity()));
			} else {
				renderItemInGui(st, slot.stack.getStack(), getGuiLeft() + slot.xDisplayPosition, getGuiTop() + slot.yDisplayPosition, mouseX, mouseY, false, 0, true);
			}
		}
		return mouseX >= (getGuiLeft() + slot.xDisplayPosition) - 1 && mouseY >= (getGuiTop() + slot.yDisplayPosition) - 1 && mouseX < (getGuiLeft() + slot.xDisplayPosition) + 17 && mouseY < (getGuiTop() + slot.yDisplayPosition) + 17;
	}

	private void drawStackSize(PoseStack st, Font fr, long size, int x, int y) {
		float scaleFactor = 0.6f;
		RenderSystem.disableDepthTest();
		RenderSystem.disableBlend();
		String stackSize = StorageTerminalMenu.formatNumber(size);
		st.pushPose();
		st.scale(scaleFactor, scaleFactor, scaleFactor);
		st.translate(0, 0, 450);
		float inverseScaleFactor = 1.0f / scaleFactor;
		int X = (int) (((float) x + 0 + 16.0f - fr.width(stackSize) * scaleFactor) * inverseScaleFactor);
		int Y = (int) (((float) y + 0 + 16.0f - 7.0f * scaleFactor) * inverseScaleFactor);
		fr.drawShadow(st, stackSize, X, Y, 16777215);
		st.popPose();
		RenderSystem.enableDepthTest();
	}

	protected boolean needsScrollBars() {
		return this.getMenu().itemListClientSorted.size() > rowCount * 9;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		if (slotIDUnderMouse > -1) {
			if (isPullOne(mouseButton)) {
				if (getMenu().getSlotByID(slotIDUnderMouse).stack != null && getMenu().getSlotByID(slotIDUnderMouse).stack.getQuantity() > 0) {
					for (int i = 0;i < getMenu().itemList.size();i++) {
						if (getMenu().getSlotByID(slotIDUnderMouse).stack.equals(getMenu().itemList.get(i))) {
							storageSlotClick(getMenu().getSlotByID(slotIDUnderMouse).stack.getStack(), SlotAction.PULL_ONE, isTransferOne(mouseButton) ? 1 : 0);
							return true;
						}
					}
				}
				return true;
			} else if (pullHalf(mouseButton)) {
				if (!menu.getCarried().isEmpty()) {
					storageSlotClick(ItemStack.EMPTY, hasControlDown() ? SlotAction.GET_QUARTER : SlotAction.GET_HALF, 0);
				} else {
					if (getMenu().getSlotByID(slotIDUnderMouse).stack != null && getMenu().getSlotByID(slotIDUnderMouse).stack.getQuantity() > 0) {
						for (int i = 0;i < getMenu().itemList.size();i++) {
							if (getMenu().getSlotByID(slotIDUnderMouse).stack.equals(getMenu().itemList.get(i))) {
								storageSlotClick(getMenu().getSlotByID(slotIDUnderMouse).stack.getStack(), hasControlDown() ? SlotAction.GET_QUARTER : SlotAction.GET_HALF, 0);
								return true;
							}
						}
					}
				}
			} else if (pullNormal(mouseButton)) {
				if (!menu.getCarried().isEmpty()) {
					storageSlotClick(ItemStack.EMPTY, SlotAction.PULL_OR_PUSH_STACK, 0);
				} else {
					if (getMenu().getSlotByID(slotIDUnderMouse).stack != null) {
						if (getMenu().getSlotByID(slotIDUnderMouse).stack.getQuantity() > 0) {
							for (int i = 0;i < getMenu().itemList.size();i++) {
								if (getMenu().getSlotByID(slotIDUnderMouse).stack.equals(getMenu().itemList.get(i))) {
									storageSlotClick(getMenu().getSlotByID(slotIDUnderMouse).stack.getStack(), hasShiftDown() ? SlotAction.SHIFT_PULL : SlotAction.PULL_OR_PUSH_STACK, 0);
									return true;
								}
							}
						}
					}
				}
			}
		} else if (GLFW.glfwGetKey(mc.getWindow().getWindow(), GLFW.GLFW_KEY_SPACE) != GLFW.GLFW_RELEASE) {
			storageSlotClick(ItemStack.EMPTY, SlotAction.SPACE_CLICK, 0);
		} else {
			if (mouseButton == 1 && isHovering(searchField.x - leftPos, searchField.y - topPos, 89, this.getFont().lineHeight, mouseX, mouseY))
				searchField.setValue("");
			else if(this.searchField.mouseClicked(mouseX, mouseY, mouseButton))return true;
			else
				return super.mouseClicked(mouseX, mouseY, mouseButton);
		}
		return true;
	}

	protected void storageSlotClick(ItemStack slotStack, SlotAction act, int mod) {
		CompoundTag c = new CompoundTag();
		c.put("s", slotStack.save(new CompoundTag()));
		c.putInt("a", act.ordinal());
		c.putByte("m", (byte) mod);
		CompoundTag msg = new CompoundTag();
		msg.put("a", c);
		menu.sendMessage(msg);
	}

	public boolean isPullOne(int mouseButton) {
		switch (ctrlm()) {
		case AE:
			return mouseButton == 1 && hasShiftDown();
		case RS:
			return mouseButton == 2;
		case DEF:
			return mouseButton == 1 && !menu.getCarried().isEmpty();
		default:
			return false;
		}
	}

	public boolean isTransferOne(int mouseButton) {
		switch (ctrlm()) {
		case AE:
			return hasShiftDown() && hasControlDown();//not in AE
		case RS:
			return hasShiftDown() && mouseButton == 2;
		case DEF:
			return mouseButton == 1 && hasShiftDown();
		default:
			return false;
		}
	}

	public boolean pullHalf(int mouseButton) {
		switch (ctrlm()) {
		case AE:
			return mouseButton == 1;
		case RS:
			return mouseButton == 1;
		case DEF:
			return mouseButton == 1 && menu.getCarried().isEmpty();
		default:
			return false;
		}
	}

	public boolean pullNormal(int mouseButton) {
		switch (ctrlm()) {
		case AE:
		case RS:
		case DEF:
			return mouseButton == 0;
		default:
			return false;
		}
	}

	private ControllMode ctrlm() {
		return ControllMode.VALUES[controllMode];
	}

	public final void renderItemInGui(PoseStack st, ItemStack stack, int x, int y, int mouseX, int mouseY, boolean hasBg, int color, boolean tooltip, String... extraInfo) {
		if (stack != null) {
			if (!tooltip) {
				if (hasBg) {
					fill(st, x, y, 16, 16, color | 0x80000000);
				}
				st.translate(0.0F, 0.0F, 32.0F);
				//this.setBlitOffset(100);
				//this.itemRenderer.zLevel = 100.0F;
				Font font = null;
				/*if (stack != null)
					font = stack.getItem().getFontRenderer(stack);*/
				if (font == null)
					font = this.getFont();
				RenderSystem.enableDepthTest();
				this.itemRenderer.renderAndDecorateItem(stack, x, y);
				this.itemRenderer.renderGuiItemDecorations(font, stack, x, y, null);
				//this.setBlitOffset(0);
				//this.itemRenderer.zLevel = 0.0F;
			} else if (mouseX >= x - 1 && mouseY >= y - 1 && mouseX < x + 17 && mouseY < y + 17) {
				List<Component> list = getTooltipFromItem(stack);
				// list.add(I18n.format("tomsmod.gui.amount", stack.stackSize));
				if (extraInfo != null && extraInfo.length > 0) {
					for (int i = 0; i < extraInfo.length; i++) {
						list.add(Component.literal(extraInfo[i]));
					}
				}
				for (int i = 0;i < list.size();++i) {
					Component t = list.get(i);
					MutableComponent t2 = t instanceof MutableComponent ? (MutableComponent) t : t.copy();
					if (i == 0) {
						list.set(i, t2.withStyle(stack.getRarity().color));
					} else {
						list.set(i, t2.withStyle(ChatFormatting.GRAY));
					}
				}
				this.renderComponentTooltip(st, list, mouseX, mouseY);
			}
		}
	}

	public Font getFont() {
		return font;
	}

	@Override
	public boolean keyPressed(int p_keyPressed_1_, int p_keyPressed_2_, int p_keyPressed_3_) {
		if (p_keyPressed_1_ == 256) {
			this.onClose();
			return true;
		}
		return !this.searchField.keyPressed(p_keyPressed_1_, p_keyPressed_2_, p_keyPressed_3_) && !this.searchField.canConsumeInput() ? super.keyPressed(p_keyPressed_1_, p_keyPressed_2_, p_keyPressed_3_) : true;
	}

	@Override
	public boolean charTyped(char p_charTyped_1_, int p_charTyped_2_) {
		if(searchField.charTyped(p_charTyped_1_, p_charTyped_2_))return true;
		return super.charTyped(p_charTyped_1_, p_charTyped_2_);
	}

	@Override
	public boolean mouseScrolled(double p_mouseScrolled_1_, double p_mouseScrolled_3_, double p_mouseScrolled_5_) {
		if (!this.needsScrollBars()) {
			return false;
		} else {
			int i = ((this.menu).itemListClientSorted.size() + 9 - 1) / 9 - 5;
			this.currentScroll = (float)(this.currentScroll - p_mouseScrolled_5_ / i);
			this.currentScroll = Mth.clamp(this.currentScroll, 0.0F, 1.0F);
			this.menu.scrollTo(this.currentScroll);
			return true;
		}
	}

	public abstract ResourceLocation getGui();

	@Override
	protected void renderBg(PoseStack st, float partialTicks, int mouseX, int mouseY) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, getGui());
		this.blit(st, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
	}

	public class GuiButton extends Button {
		protected int tile;
		protected int state;
		protected int texX = 194;
		protected int texY = 30;
		public GuiButton(int x, int y, int tile, OnPress pressable) {
			super(x, y, 16, 16, null, pressable);
			this.tile = tile;
		}

		public void setX(int i) {
			x = i;
		}

		/**
		 * Draws this button to the screen.
		 */
		@Override
		public void renderButton(PoseStack st, int mouseX, int mouseY, float pt) {
			if (this.visible) {
				RenderSystem.setShader(GameRenderer::getPositionTexShader);
				RenderSystem.setShaderTexture(0, getGui());
				this.isHovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
				//int i = this.getYImage(this.isHovered);
				RenderSystem.enableBlend();
				RenderSystem.defaultBlendFunc();
				RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
				this.blit(st, this.x, this.y, texX + state * 16, texY + tile * 16, this.width, this.height);
			}
		}
	}

	protected void onUpdateSearch(String text) {}

	@Override
	public void receive(CompoundTag tag) {
		if (tag.contains("e")) {
			storedEnergy = tag.getInt("e");
			consumtion = tag.getInt("c");
		} else {
			menu.receiveClientNBTPacket(tag);
			refreshItemList = true;
		}
	}

	private FakeSlot fakeSlotUnderMouse = new FakeSlot();
	private static class FakeSlot extends Slot {
		private static final Container DUMMY = new SimpleContainer(1);

		public FakeSlot() {
			super(DUMMY, 0, Integer.MIN_VALUE, Integer.MIN_VALUE);
		}

		@Override
		public boolean allowModification(Player p_150652_) {
			return false;
		}

		@Override
		public void set(ItemStack p_40240_) {}

		@Override
		public ItemStack remove(int p_40227_) {
			return ItemStack.EMPTY;
		}
	}

	@Override
	public Slot getSlotUnderMouse() {
		Slot s = super.getSlotUnderMouse();
		if(s != null)return s;
		if(slotIDUnderMouse > -1 && getMenu().getSlotByID(slotIDUnderMouse).stack != null) {
			fakeSlotUnderMouse.container.setItem(0, getMenu().getSlotByID(slotIDUnderMouse).stack.getStack());
			return fakeSlotUnderMouse;
		}
		return null;
	}

	private int getEnergyStoredScaled(int pixels) {
		int energyStored = storedEnergy;
		int maxEnergy = Config.maxEnergyCapacity;
		if (energyStored != 0 && maxEnergy != 0) {
			return energyStored * pixels / maxEnergy;
		} else {
			return 0;
		}
	}
}
