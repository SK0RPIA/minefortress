package org.minefortress.renderer.gui.blueprints;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.remmintan.mods.minefortress.core.interfaces.blueprints.BlueprintGroup;
import net.remmintan.mods.minefortress.core.interfaces.blueprints.IBlueprintMetadata;
import net.remmintan.mods.minefortress.core.interfaces.resources.IItemInfo;
import net.remmintan.panama.renderer.BlueprintRenderer;
import org.minefortress.renderer.gui.blueprints.handler.BlueprintScreenHandler;
import org.minefortress.renderer.gui.blueprints.handler.BlueprintSlot;
import org.minefortress.utils.ModUtils;

import java.util.List;

public final class BlueprintsScreen extends Screen {

    private static final Identifier SCROLLER_TEXTURE = new Identifier("container/creative_inventory/scroller");
    private static final Identifier SCROLLER_DISABLED_TEXTURE = new Identifier("container/creative_inventory/scroller_disabled");
    private static final Identifier[] TAB_TOP_UNSELECTED_TEXTURES = new Identifier[]{new Identifier("container/creative_inventory/tab_top_unselected_1"), new Identifier("container/creative_inventory/tab_top_unselected_2"), new Identifier("container/creative_inventory/tab_top_unselected_3"), new Identifier("container/creative_inventory/tab_top_unselected_4"), new Identifier("container/creative_inventory/tab_top_unselected_5"), new Identifier("container/creative_inventory/tab_top_unselected_6"), new Identifier("container/creative_inventory/tab_top_unselected_7")};
    private static final Identifier[] TAB_TOP_SELECTED_TEXTURES = new Identifier[]{new Identifier("container/creative_inventory/tab_top_selected_1"), new Identifier("container/creative_inventory/tab_top_selected_2"), new Identifier("container/creative_inventory/tab_top_selected_3"), new Identifier("container/creative_inventory/tab_top_selected_4"), new Identifier("container/creative_inventory/tab_top_selected_5"), new Identifier("container/creative_inventory/tab_top_selected_6"), new Identifier("container/creative_inventory/tab_top_selected_7")};
    private static final Identifier[] TAB_BOTTOM_UNSELECTED_TEXTURES = new Identifier[]{new Identifier("container/creative_inventory/tab_bottom_unselected_1"), new Identifier("container/creative_inventory/tab_bottom_unselected_2"), new Identifier("container/creative_inventory/tab_bottom_unselected_3"), new Identifier("container/creative_inventory/tab_bottom_unselected_4"), new Identifier("container/creative_inventory/tab_bottom_unselected_5"), new Identifier("container/creative_inventory/tab_bottom_unselected_6"), new Identifier("container/creative_inventory/tab_bottom_unselected_7")};
    private static final Identifier[] TAB_BOTTOM_SELECTED_TEXTURES = new Identifier[]{new Identifier("container/creative_inventory/tab_bottom_selected_1"), new Identifier("container/creative_inventory/tab_bottom_selected_2"), new Identifier("container/creative_inventory/tab_bottom_selected_3"), new Identifier("container/creative_inventory/tab_bottom_selected_4"), new Identifier("container/creative_inventory/tab_bottom_selected_5"), new Identifier("container/creative_inventory/tab_bottom_selected_6"), new Identifier("container/creative_inventory/tab_bottom_selected_7")};
    private static final String BACKGROUND_TEXTURE = "textures/gui/container/creative_inventory/tab_items.png";
    private static final Identifier BLUEPRINT_PREVIEW_BACKGROUND_TEXTURE = new Identifier("textures/gui/recipe_book.png");
    private static final Text EDIT_BLUEPRINT_TEXT = Text.literal("right click to edit");
    private static final Text ADD_BLUEPRINT_TEXT = Text.literal("click to add blueprint");
    private static final Text DELETE_BLUEPRINT_TEXT = Text.literal("right click to delete");

    private final int backgroundWidth = 195;
    private final int backgroundHeight = 136;

    private final int previewWidth = 120;
    private final int previewOffset = 4;
    private boolean sneakButtonDown = false;

    private BlueprintRenderer blueprintRenderer;

    private int x;
    private int y;

    private boolean isScrolling = false;
    private float scrollPosition = 0;

    private BlueprintScreenHandler handler;

    private ButtonWidget importExportButton;

    public BlueprintsScreen() {
        super(Text.literal("Blueprints"));
    }

    @Override
    protected void init() {
        if(this.client != null) {
            if(ModUtils.isClientInFortressGamemode()) {
                super.init();
                this.x = (this.width - backgroundWidth - previewWidth - previewOffset) / 2;
                this.y = (this.height - backgroundHeight) / 2;

                this.handler = new BlueprintScreenHandler(this.client);
                this.blueprintRenderer = ModUtils.getFortressClient().get_BlueprintRenderer();
                final var connectedToTheServer = ModUtils.getFortressClientManager().isConnectedToTheServer();
                if(!connectedToTheServer) {
                    importExportButton = ButtonWidget
                            .builder(Text.literal("Import / Export"), btn -> client.setScreen(new ImportExportBlueprintsScreen()))
                            .dimensions(this.x + backgroundWidth + previewOffset, this.y - 22, 120, 20)
                            .build();
                    this.addDrawableChild(importExportButton);
                }
            } else {
                this.client.setScreen(null);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(button == 0) {
            for(BlueprintGroup group : BlueprintGroup.values()) {
                if(isClickInTab(group, mouseX, mouseY)) return true;
            }

            if(this.isClickInScrollbar(mouseX, mouseY)) {
                this.isScrolling = this.handler.isNeedScrollbar();
                return true;
            }
        }

        if(super.mouseClicked(mouseX, mouseY, button)) return true;

        if(button == 0 || button == 1) {
            if(this.handler.hasFocusedSlot()) {
                if(this.handler.getFocusedSlot() == BlueprintSlot.EMPTY) {
                    if(this.client != null)
                        this.client.setScreen(new AddBlueprintScreen(handler.getSelectedGroup()));
                    return true;
                }

                if(button == 1) return true;

                if(this.client != null){
                    this.client.setScreen(null);
                }
                this.handler.clickOnFocusedSlot();
            }
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.isScrolling) {
            int minScroll = this.y + 18;
            int maxScroll = minScroll + 112;
            this.scrollPosition = ((float)mouseY - (float)minScroll - 7.5f) / ((float)(maxScroll - minScroll) - 15.0f);
            this.scrollPosition = MathHelper.clamp(this.scrollPosition, 0.0f, 1.0f);
            this.handler.scroll(scrollPosition);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isScrolling = false;
            for (BlueprintGroup blueprintGroup: BlueprintGroup.values()) {
                if (this.isClickInTab(blueprintGroup, mouseX, mouseY)) {
                    this.handler.selectGroup(blueprintGroup);
                    this.scrollPosition = 0;
                    return true;
                }
            }
        }

        if(button == 1) {
            if(this.handler.hasFocusedSlot()) {
                if(sneakButtonDown) {
                    this.handler.sendRemovePacket();
                } else {
                    this.handler.sendEditPacket();
                    if(client != null) {
                        this.client.setScreen(null);
                    }
                }
            }
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!this.handler.isNeedScrollbar()) {
            return false;
        }
        int i = (this.handler.getSelectedGroupSize() + 9 - 1) / 9 - 5;
        this.scrollPosition = (float)((double)this.scrollPosition - verticalAmount / (double)i);
        this.scrollPosition = MathHelper.clamp(this.scrollPosition, 0.0f, 1.0f);
        this.handler.scroll(scrollPosition);
        return true;
    }

    public void updateSlots() {
        this.handler.scroll(scrollPosition);
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        this.renderBackground(drawContext, mouseX, mouseY, delta);
        this.drawBackground(drawContext, mouseX, mouseY);
        RenderSystem.disableDepthTest();
        if(importExportButton!=null)
            importExportButton.render(drawContext, mouseX, mouseY, delta);
//        super.render(drawContext, mouseX, mouseY, delta);

        int screenX = this.x;
        int screenY = this.y;

        MatrixStack matrixStack = RenderSystem.getModelViewStack();
        matrixStack.push();
        matrixStack.translate(screenX, screenY, 0.0);
        RenderSystem.applyModelViewMatrix();
        this.handler.focusOnSlot(null);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);

        final List<BlueprintSlot> currentSlots = this.handler.getCurrentSlots();
        final int currentSlotsSize = currentSlots.size();
        for (int i = 0; i < 45; i++) {
            int slotColumn = i % 9;
            int slotRow = i / 9;

            int slotX = slotColumn * 18 + 9;
            int slotY = slotRow * 18 + 18;

            final BlueprintSlot blueprintSlot = i<currentSlotsSize ? currentSlots.get(i) : BlueprintSlot.EMPTY;
            this.drawSlot(drawContext, blueprintSlot, slotColumn, slotRow);

            if (!this.isPointOverSlot(slotX, slotY, mouseX, mouseY)) continue;
            final var fortressClientManager = ModUtils.getFortressClientManager();
            final var resourceManager = fortressClientManager.getResourceManager();
            this.handler.focusOnSlot(blueprintSlot);
            HandledScreen.drawSlotHighlight(drawContext, slotX, slotY, 10);
            final var x = this.x - this.backgroundWidth / 2;
            if(blueprintSlot != BlueprintSlot.EMPTY) {
                if(fortressClientManager.isSurvival()) {
                    final var stacks = blueprintSlot.getBlockData().getStacks();
                    for (int i1 = 0; i1 < stacks.size(); i1++) {
                        final var stack = stacks.get(i1);
                        final var hasItem = resourceManager.hasItem(stack, stacks);
                        final var itemX = x + 25 + i1%10 * 30;
                        final var itemY = i1/10 * 20 + this.backgroundHeight;
                        final var convertedItem = convertItemIconInTheGUI(stack);
                        drawContext.drawItem(new ItemStack(convertedItem), itemX, itemY);
                        drawContext.drawText(this.textRenderer, String.valueOf(stack.amount()), itemX + 17, itemY + 7, hasItem?0xFFFFFF:0xFF0000, false);
                    }
                }

                this.blueprintRenderer.renderBlueprintPreview(drawContext.getMatrices(), blueprintSlot.getMetadata().getId(), BlockRotation.NONE);
            }
        }

        this.drawForeground(drawContext);
        if(this.handler.hasFocusedSlot()){
            if(this.sneakButtonDown && handler.getFocusedSlot() != BlueprintSlot.EMPTY) {
                drawContext.drawText(this.textRenderer,
                        DELETE_BLUEPRINT_TEXT,
                        this.backgroundWidth + this.previewOffset + 3,
                        this.backgroundHeight - this.textRenderer.fontHeight - 3,
                        0xFF0000,
                        false);
            } else {
                final var editText = handler.getFocusedSlot() != BlueprintSlot.EMPTY ? EDIT_BLUEPRINT_TEXT : ADD_BLUEPRINT_TEXT;
                drawContext.drawText(this.textRenderer,
                        editText,
                        this.backgroundWidth + this.previewOffset + 3,
                        this.backgroundHeight - this.textRenderer.fontHeight - 3,
                        0xFFFFFF,
                        false);
            }

        }

        matrixStack.pop();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.enableDepthTest();

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        this.drawMouseoverTooltip(drawContext, mouseX, mouseY);
    }

    public static Item convertItemIconInTheGUI(IItemInfo stack) {
        final var originalItem = stack.item();
        if(Items.FARMLAND.equals(originalItem)) {
            return Items.DIRT;
        }
        return originalItem;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(isSneak(keyCode, scanCode)) {
            this.sneakButtonDown = true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if(isSneak(keyCode,scanCode)) {
            this.sneakButtonDown = false;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    private boolean isSneak(int keyCode, int scanCode) {
        return MinecraftClient.getInstance().options.sneakKey.matchesKey(keyCode, scanCode);
    }

    private boolean isClickInTab(BlueprintGroup group, double mouseX, double mouseY) {
        mouseX -= this.x;
        mouseY -= this.y;

        int columnNumber = group.ordinal();
        int x = 28 * columnNumber;
        int y = 0;
        if (columnNumber > 0) x += columnNumber;

        if (group.isTopRow()) {
            y -= 32;
        } else {
            y += this.backgroundHeight;
        }
        return mouseX >= (double)x && mouseX <= (double)(x + 28) && mouseY >= (double)y && mouseY <= (double)(y + 32);
    }

    private boolean isClickInScrollbar(double mouseX, double mouseY) {
        int i = this.x;
        int j = this.y;
        int k = i + 175;
        int l = j + 18;
        int m = k + 14;
        int n = l + 112;
        return mouseX >= (double)k && mouseY >= (double)l && mouseX < (double)m && mouseY < (double)n;
    }

    private boolean isPointOverSlot(int slotX, int slotY, int mouseX, int mouseY) {
        int screenX = this.x;
        int screenY = this.y;

        return mouseX >= screenX + slotX && mouseX < screenX + slotX + 18 && mouseY >= screenY + slotY && mouseY < screenY + slotY + 18;
    }

    private void drawForeground(DrawContext drawContext) {
        final BlueprintGroup selectedGroup = this.handler.getSelectedGroup();
        if (selectedGroup != null) {
            RenderSystem.disableBlend();
            drawContext.drawText(this.textRenderer, selectedGroup.getNameText(), 8, 6, 0x404040, false);
        }
    }

    private void drawSlot(DrawContext drawContext, BlueprintSlot slot, int slotColumn, int slotRow) {
        RenderSystem.enableDepthTest();
        int slotX = slotColumn * 18 + 9 + 5;
        int slotY = slotRow * 18 + 18 + 5;
        if(slot == BlueprintSlot.EMPTY){
            final var scaleFactor = 0.5f;
            final var matrices = drawContext.getMatrices();
            matrices.push();
            matrices.scale(scaleFactor, scaleFactor, scaleFactor);
            drawContext.drawItem(new ItemStack(Items.BRICK), (int)(slotX/scaleFactor), (int)(slotY/scaleFactor));
            matrices.pop();
        } else {
            final IBlueprintMetadata metadata = slot.getMetadata();
            final var enoughResources = !ModUtils.getFortressClientManager().isSurvival() || slot.isEnoughResources();
            if(this.client != null){
                this.blueprintRenderer.renderBlueprintInGui(drawContext.getMatrices(), metadata.getId(), BlockRotation.NONE, slotColumn, slotRow, enoughResources);
            }
        }

    }

    private void drawBackground(DrawContext drawContext, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        final BlueprintGroup selectedGroup = this.handler.getSelectedGroup();
        for (BlueprintGroup bg : BlueprintGroup.values()) {
            if (selectedGroup == bg) continue;
            this.renderTabIcon(drawContext, bg);
            this.renderTabTooltipIfHovered(drawContext, bg, mouseX, mouseY);
        }

        drawContext.drawTexture(new Identifier(BACKGROUND_TEXTURE), this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        final var scrollbarX = this.x + 175;
        final var scrollbarY = this.y + 18 + (int) ((float) (112 - 17) * this.scrollPosition);
        final var scrollBarTexture = this.handler.isNeedScrollbar() ? SCROLLER_TEXTURE : SCROLLER_DISABLED_TEXTURE;

        drawContext.drawGuiTexture(scrollBarTexture, scrollbarX, scrollbarY, 12, 15);

        if(selectedGroup != null)
            this.renderTabIcon(drawContext, selectedGroup);


        drawContext.drawTexture(BLUEPRINT_PREVIEW_BACKGROUND_TEXTURE, this.x + this.backgroundWidth + this.previewOffset, this.y, 250, 230, this.previewWidth, this.backgroundHeight, 1000, 1000);
    }


    private void renderTabIcon(DrawContext drawContext, BlueprintGroup group) {
        boolean isSelectedGroup = group == this.handler.getSelectedGroup();
        int columnNumber = group.ordinal() % 7;

        int buttonWidth = 26;


        int x = this.x + buttonWidth * columnNumber;
        int y = this.y;
        if (columnNumber > 0) {
            x += columnNumber;
        }
        boolean topRow = group.isTopRow();
        if (topRow) {
            y -= 28;
        } else {
            y += this.backgroundHeight - 4;
        }

        final var tabTexture = getTabTexture(topRow, isSelectedGroup, columnNumber);

        drawContext.drawGuiTexture(tabTexture, x, y, buttonWidth, 32);
        int yIconDelta = topRow ? 1 : -1;
        ItemStack icon = group.getIcon();

        drawContext.drawItem(icon, x + 5, y + 8 + yIconDelta);
    }

    private Identifier getTabTexture(boolean topRow, boolean selected, int groupIndex) {
        final Identifier[] identifiers;
        if(topRow) {
            identifiers = selected ? TAB_TOP_SELECTED_TEXTURES : TAB_TOP_UNSELECTED_TEXTURES;
        } else {
            identifiers = selected ? TAB_BOTTOM_SELECTED_TEXTURES : TAB_BOTTOM_UNSELECTED_TEXTURES;
        }

        return identifiers[MathHelper.clamp(groupIndex, 0, identifiers.length)];
    }

    private void renderTabTooltipIfHovered(DrawContext drawContext, BlueprintGroup group, int mouseX, int mouseY) {
        int columnNumber = group.ordinal();
        int x = 26 * columnNumber + this.x;
        int y = this.y;
        if (columnNumber > 0) {
            x += columnNumber;
        }
        if (group.isTopRow()) {
            y -= 32;
        } else {
            y += this.backgroundHeight;
        }
        if (this.isPointWithinBounds(x + 3, y + 3, mouseX, mouseY)) {
            drawContext.drawTooltip(this.textRenderer, group.getNameText(), mouseX, mouseY);
        }
    }

    private boolean isPointWithinBounds(int x, int y, int mouseX, int mouseY) {
        return mouseX >= x && mouseY >= y && mouseX < x + 23 && mouseY < y + 27;
    }

    private void drawMouseoverTooltip(DrawContext drawContext, int x, int y) {
        if (this.handler.hasFocusedSlot() && this.handler.getFocusedSlot() != BlueprintSlot.EMPTY) {
            drawContext.drawTooltip(this.textRenderer, this.handler.getFocusedSlotName(), x, y);
        }
    }

}
