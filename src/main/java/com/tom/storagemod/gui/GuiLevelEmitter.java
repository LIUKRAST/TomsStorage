package com.tom.storagemod.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import com.tom.storagemod.network.IDataReceiver;
import com.tom.storagemod.network.NetworkHandler;

public class GuiLevelEmitter extends AbstractContainerScreen<LevelEmitterMenu> implements IDataReceiver {
	private static final ResourceLocation gui = new ResourceLocation("toms_storage", "textures/gui/level_emitter.png");
	private GuiButton lessThanBtn;
	private EditBox textF;
	private boolean lt;
	private int count;
	private final List<AmountBtn> amountBtns = new ArrayList<>();

	public GuiLevelEmitter(LevelEmitterMenu screenContainer, Inventory inv, Component titleIn) {
		super(screenContainer, inv, titleIn);
	}

	@Override
	public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(matrixStack);
		amountBtns.forEach(AmountBtn::update);
		super.render(matrixStack, mouseX, mouseY, partialTicks);
		this.renderTooltip(matrixStack, mouseX, mouseY);
		if(lessThanBtn.isHoveredOrFocused()) {
			renderComponentTooltip(matrixStack, Arrays.stream(I18n.get("tooltip.toms_storage.lvlEm_lt_" + lessThanBtn.state).split("\\\\")).map(Component::literal).collect(Collectors.toList()), mouseX, mouseY);
		}
	}

	@Override
	protected void renderBg(PoseStack matrixStack, float partialTicks, int x, int y) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, gui);
		int i = (this.width - this.imageWidth) / 2;
		int j = (this.height - this.imageHeight) / 2;
		this.blit(matrixStack, i, j, 0, 0, this.imageWidth, this.imageHeight);
	}

	@Override
	protected void init() {
		clearWidgets();
		amountBtns.clear();
		super.init();
		textF = new EditBox(font, leftPos + 70, topPos + 41, 89, font.lineHeight, Component.translatable("narrator.toms_storage.level_emitter_amount"));
		textF.setMaxLength(100);
		textF.setBordered(false);
		textF.setVisible(true);
		textF.setTextColor(16777215);
		textF.setValue("1");
		textF.setResponder(t -> {
			try {
				int c = Integer.parseInt(t);
				if(c >= 1) {
					count = c;
					send();
				}
			} catch (NumberFormatException e) {
			}
		});
		addRenderableWidget(textF);
		lessThanBtn = new GuiButton(leftPos - 18, topPos + 5, 0, b -> {
			lt = !lt;
			lessThanBtn.state = lt ? 1 : 0;
			send();
		});
		addRenderableWidget(lessThanBtn);
		amountBtns.add(new AmountBtn( 20, 0,    1,  1, 20));
		amountBtns.add(new AmountBtn( 45, 0,   10, 16, 25));
		amountBtns.add(new AmountBtn( 75, 0,  100, 32, 30));
		amountBtns.add(new AmountBtn(110, 0, 1000, 64, 35));

		amountBtns.add(new AmountBtn( 20, 40,    -1,  -1, 20));
		amountBtns.add(new AmountBtn( 45, 40,   -10, -16, 25));
		amountBtns.add(new AmountBtn( 75, 40,  -100, -32, 30));
		amountBtns.add(new AmountBtn(110, 40, -1000, -64, 35));
	}

	@Override
	public void receive(CompoundTag tag) {
		count = tag.getInt("count");
		boolean lt = tag.getBoolean("lessThan");
		lessThanBtn.state = lt ? 1 : 0;
		this.lt = lt;
		textF.setValue(Integer.toString(count));
	}

	public class GuiButton extends Button {
		protected int tile;
		protected int state;
		protected int texX = 176;
		protected int texY = 0;
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
				RenderSystem.setShaderTexture(0, gui);
				this.isHovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
				//int i = this.getYImage(this.isHovered);
				RenderSystem.enableBlend();
				RenderSystem.defaultBlendFunc();
				RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
				this.blit(st, this.x, this.y, texX + state * 16, texY + tile * 16, this.width, this.height);
			}
		}
	}

	private void send() {
		CompoundTag mainTag = new CompoundTag();
		mainTag.putInt("count", count);
		mainTag.putBoolean("lessThan", lt);
		NetworkHandler.sendDataToServer(mainTag);
	}

	private class AmountBtn {
		private Button btn;
		private int v, sv;
		public AmountBtn(int x, int y, int v, int sv, int len) {
			btn = new Button(leftPos + x, topPos + y + 16, len, 20, Component.literal((v > 0 ? "+" : "") + v), this::evt);
			addRenderableWidget(btn);
			this.v = v;
			this.sv = sv;
		}

		private void evt(Button b) {
			count += hasShiftDown() ? sv : v;
			if(count < 1)count = 1;
			textF.setValue(Integer.toString(count));
			send();
		}

		private void update() {
			if(hasShiftDown()) {
				btn.setMessage(Component.literal((sv > 0 ? "+" : "") + sv));
			} else {
				btn.setMessage(Component.literal((v > 0 ? "+" : "") + v));
			}
		}
	}
}
