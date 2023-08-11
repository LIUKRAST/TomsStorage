package com.tom.storagemod.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.recipebook.GhostRecipe;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;

public class GuiCraftingTerminal extends GuiStorageTerminalBase<CraftingTerminalMenu> implements RecipeUpdateListener {
	private static final ResourceLocation gui = new ResourceLocation("toms_storage", "textures/gui/crafting_terminal.png");
	private static Field stackedContentsField, searchBarField, ghostRecipeField;
	static {
		try {
			for (Field f : RecipeBookComponent.class.getDeclaredFields()) {
				if(f.getType() == StackedContents.class) {
					f.setAccessible(true);
					stackedContentsField = f;
				} else if(f.getType() == EditBox.class) {
					f.setAccessible(true);
					searchBarField = f;
				} else if(f.getType() == GhostRecipe.class) {
					f.setAccessible(true);
					ghostRecipeField = f;
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	private final RecipeBookComponent recipeBookGui;
	private boolean widthTooNarrow;
	private static final ResourceLocation RECIPE_BUTTON_TEXTURE = new ResourceLocation("textures/gui/recipe_button.png");
	private EditBox searchField;
	private GhostRecipe ghostRecipe;
	private GuiButton buttonPullFromInv;
	private boolean pullFromInv;

	public GuiCraftingTerminal(CraftingTerminalMenu screenContainer, Inventory inv, Component titleIn) {
		super(screenContainer, inv, titleIn);

		recipeBookGui = new RecipeBookComponent();
		try {
			stackedContentsField.set(recipeBookGui, getMenu().new TerminalRecipeItemHelper());
			ghostRecipe = (GhostRecipe) ghostRecipeField.get(recipeBookGui);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ResourceLocation getGui() {
		return gui;
	}

	@Override
	protected void onUpdateSearch(String text) {
		if(ModList.get().isLoaded("jei") || (searchType & 4) > 0) {
			if(searchField != null)searchField.setValue(text);
			recipeBookGui.recipesUpdated();
		}
	}

	@Override
	protected void init() {
		imageWidth = 194;
		imageHeight = 256;
		rowCount = 5;
		super.init();
		this.widthTooNarrow = this.width < 379;
		this.recipeBookGui.init(this.width, this.height, this.minecraft, this.widthTooNarrow, this.menu);
		this.leftPos = this.recipeBookGui.updateScreenPosition(this.width, this.imageWidth);
		addRenderableWidget(recipeBookGui);
		this.setInitialFocus(this.recipeBookGui);
		GuiButtonClear btnClr = new GuiButtonClear(leftPos + 80, topPos + 110, b -> clearGrid());
		addRenderableWidget(btnClr);
		buttonPullFromInv = addRenderableWidget(new GuiButton(leftPos - 18, topPos + 5 + 18*4, 4, b -> {
			pullFromInv = !pullFromInv;
			buttonPullFromInv.state = pullFromInv ? 1 : 0;
			sendUpdate();
		}));
		addRenderableWidget(new ImageButton(this.leftPos + 4, this.height / 2, 20, 18, 0, 0, 19, RECIPE_BUTTON_TEXTURE, (p_214076_1_) -> {
			this.recipeBookGui.initVisuals();
			try {
				searchField = (EditBox) searchBarField.get(recipeBookGui);
			} catch (Exception e) {
				searchField = null;
			}

			this.recipeBookGui.toggleVisibility();
			this.leftPos = this.recipeBookGui.updateScreenPosition(this.width, this.imageWidth);
			((ImageButton)p_214076_1_).setPosition(this.leftPos + 4, this.height / 2);
			super.searchField.setX(this.leftPos + 82);
			btnClr.setX(this.leftPos + 80);
			buttonSortingType.setX(leftPos - 18);
			buttonDirection.setX(leftPos - 18);
			if(recipeBookGui.isVisible()) {
				buttonSearchType.setX(leftPos - 36);
				buttonCtrlMode.setX(leftPos - 36);
				buttonPullFromInv.setX(leftPos - 54);
				buttonSearchType.y = topPos + 5;
				buttonCtrlMode.y = topPos + 5 + 18;
				buttonPullFromInv.y = topPos + 5 + 18;
			} else {
				buttonSearchType.setX(leftPos - 18);
				buttonCtrlMode.setX(leftPos - 18);
				buttonPullFromInv.setX(leftPos - 18);
				buttonSearchType.y = topPos + 5 + 18*2;
				buttonCtrlMode.y = topPos + 5 + 18*3;
				buttonPullFromInv.y = topPos + 5 + 18*4;
			}
		}));
		if(recipeBookGui.isVisible()) {
			buttonSortingType.setX(leftPos - 18);
			buttonDirection.setX(leftPos - 18);
			buttonSearchType.setX(leftPos - 36);
			buttonCtrlMode.setX(leftPos - 36);
			buttonPullFromInv.setX(leftPos - 54);
			buttonSearchType.y = topPos + 5;
			buttonCtrlMode.y = topPos + 5 + 18;
			buttonPullFromInv.y = topPos + 5 + 18;
			super.searchField.setX(this.leftPos + 82);
			try {
				searchField = (EditBox) searchBarField.get(recipeBookGui);
			} catch (Exception e) {
				searchField = null;
			}
		}
		onPacket();
	}

	@Override
	protected void onPacket() {
		super.onPacket();
		int s = menu.terminalData;
		pullFromInv = (s & (1 << 8)) != 0;
		buttonPullFromInv.state = pullFromInv ? 1 : 0;
	}

	@Override
	protected int updateData() {
		int d = super.updateData();
		d |= (pullFromInv ? 1 : 0) << 8;
		return d;
	}

	@Override
	public void containerTick() {
		super.containerTick();
		this.recipeBookGui.tick();
	}

	@Override
	public void render(PoseStack st, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(st);
		if (this.recipeBookGui.isVisible() && this.widthTooNarrow) {
			this.renderBg(st, partialTicks, mouseX, mouseY);
			this.recipeBookGui.render(st, mouseX, mouseY, partialTicks);
		} else {
			this.recipeBookGui.render(st, mouseX, mouseY, partialTicks);
			super.render(st, mouseX, mouseY, partialTicks);
			this.recipeBookGui.renderGhostRecipe(st, this.leftPos, this.topPos, true, partialTicks);
		}

		this.renderTooltip(st, mouseX, mouseY);
		this.recipeBookGui.renderTooltip(st, this.leftPos, this.topPos, mouseX, mouseY);
		this.setInitialFocus(this.recipeBookGui);

		if (buttonPullFromInv.isHoveredOrFocused()) {
			renderTooltip(st, Component.translatable("tooltip.toms_storage.pull_" + buttonPullFromInv.state), mouseX, mouseY);
		}
	}

	@Override
	protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
		return (!this.widthTooNarrow || !this.recipeBookGui.isVisible()) && super.isHovering(x, y, width, height, mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(double p_mouseClicked_1_, double p_mouseClicked_3_, int p_mouseClicked_5_) {
		if (this.recipeBookGui.mouseClicked(p_mouseClicked_1_, p_mouseClicked_3_, p_mouseClicked_5_)) {
			return true;
		} else {
			return this.widthTooNarrow && this.recipeBookGui.isVisible() ? true : super.mouseClicked(p_mouseClicked_1_, p_mouseClicked_3_, p_mouseClicked_5_);
		}
	}

	@Override
	protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeftIn, int guiTopIn, int mouseButton) {
		boolean flag = mouseX < guiLeftIn || mouseY < guiTopIn || mouseX >= guiLeftIn + this.imageWidth || mouseY >= guiTopIn + this.imageHeight;
		return this.recipeBookGui.hasClickedOutside(mouseX, mouseY, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, mouseButton) && flag;
	}

	/**
	 * Called when the mouse is clicked over a slot or outside the gui.
	 */
	@Override
	protected void slotClicked(Slot slotIn, int slotId, int mouseButton, ClickType type) {
		super.slotClicked(slotIn, slotId, mouseButton, type);
		this.recipeBookGui.slotClicked(slotIn);
	}

	@Override
	public void recipesUpdated() {
		this.recipeBookGui.recipesUpdated();
	}

	@Override
	public void onClose() {
		this.recipeBookGui.removed();
		super.onClose();
	}

	@Override
	public RecipeBookComponent getRecipeBookComponent() {
		return this.recipeBookGui;
	}

	private void clearGrid() {
		this.minecraft.gameMode.handleInventoryButtonClick((this.menu).containerId, 0);
	}

	@Override
	public boolean keyPressed(int code, int p_231046_2_, int p_231046_3_) {
		if(code == GLFW.GLFW_KEY_S && hoveredSlot != null) {
			ItemStack itemstack = null;

			for (int i = 0; i < this.ghostRecipe.size(); ++i) {
				GhostRecipe.GhostIngredient ghostrecipe$ghostingredient = this.ghostRecipe.get(i);
				int j = ghostrecipe$ghostingredient.getX();
				int k = ghostrecipe$ghostingredient.getY();
				if (j == hoveredSlot.x && k == hoveredSlot.y) {
					itemstack = ghostrecipe$ghostingredient.getItem();
				}
			}
			if(itemstack != null) {
				super.searchField.setValue(itemstack.getHoverName().getString());
				super.searchField.setFocus(false);
				return true;
			}
		}
		return super.keyPressed(code, p_231046_2_, p_231046_3_);
	}

	public class GuiButtonClear extends Button {

		public GuiButtonClear(int x, int y, OnPress pressable) {
			super(x, y, 11, 11, null, pressable);
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
				int i = this.getYImage(this.isHovered);
				RenderSystem.enableBlend();
				RenderSystem.defaultBlendFunc();
				RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
				this.blit(st, this.x, this.y, 194 + i * 11, 10, this.width, this.height);
			}
		}
	}
}
