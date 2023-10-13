package fifthcolumn.n.client.ui.copenheimer.search;

import com.mojang.blaze3d.systems.RenderSystem;
import fifthcolumn.n.client.ui.copenheimer.servers.CopeMultiplayerScreen;
import fifthcolumn.n.copenheimer.CopeService;
import java.util.Objects;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.StringUtils;

public class SearchParametersScreen extends Screen {
   public static final Identifier OPTIONS_BACKGROUND_TEXTURE = new Identifier("minecraft:textures/block/tnt_side.png");
   private final CopeMultiplayerScreen parent;
   private TextFieldWidget serverNameWidget;
   private TextFieldWidget serverVersionWidget;
   private TextFieldWidget serverLangWidget;
   private CheckboxWidget onlineWidget;
   private CheckboxWidget crackedWidget;
   private final CopeService copeService;

   public SearchParametersScreen(CopeMultiplayerScreen parent, CopeService copeService) {
      super(Text.of("Search Parameters"));
      this.parent = parent;
      this.copeService = copeService;
   }

   public void tick() {
      super.tick();
      this.serverNameWidget.tick();
      this.serverVersionWidget.tick();
      this.serverLangWidget.tick();
   }

   protected void init() {
      this.addDrawableChild(ButtonWidget.builder(Text.of("Search"), (button) -> {
         this.saveAndClose();
      }).dimensions(this.width / 2 - 100, this.height - 65, 200, 20).build());
      this.addDrawableChild(ButtonWidget.builder(Text.of("Cancel"), (button) -> {
         this.method_25419();
      }).dimensions(this.width / 2 - 100, this.height - 40, 200, 20).build());
      CopeService.FindServersRequest currentFindRequest = this.copeService.currentFindRequest;
      this.serverNameWidget = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 116, 200, 20, Text.of("Name like"));
      this.serverNameWidget.setMaxLength(128);
      this.serverNameWidget.setText(currentFindRequest.hasName);
      this.serverNameWidget.setChangedListener((text) -> {
         this.onChange();
      });
      this.addSelectableChild(this.serverNameWidget);
      this.serverLangWidget = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 160, 200, 20, Text.of("Server lang"));
      this.serverLangWidget.setMaxLength(2);
      this.serverLangWidget.setText(currentFindRequest.lang);
      this.serverLangWidget.setChangedListener((text) -> {
         this.onChange();
      });
      this.addSelectableChild(this.serverLangWidget);
      this.serverVersionWidget = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 204, 200, 20, this.serverVersionWidget, Text.of("Version like"));
      this.serverVersionWidget.setMaxLength(128);
      this.serverVersionWidget.setText(currentFindRequest.hasVersion);
      this.serverVersionWidget.setChangedListener((text) -> {
         this.onChange();
      });
      this.addSelectableChild(this.serverVersionWidget);
      this.onlineWidget = new CheckboxWidget(this.width / 2 - 100, 248, 200, 20, Text.of("Players online"), checkboxState(currentFindRequest.playersOnline));
      this.addSelectableChild(this.onlineWidget);
      this.crackedWidget = new CheckboxWidget(this.width / 2 - 100, 292, 200, 20, Text.of("Cracked"), checkboxState(currentFindRequest.isCracked));
      this.addSelectableChild(this.crackedWidget);
      this.setInitialFocus(this.serverVersionWidget);
      this.onChange();
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.renderBackground(context);
      context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 16777215);
      context.drawTextWithShadow(this.textRenderer, Text.of("Name"), this.width / 2 - 100, 100, 10526880);
      context.drawTextWithShadow(this.textRenderer, Text.of("Language (en, ja, es, etc)"), this.width / 2 - 100, 145, 10526880);
      context.drawTextWithShadow(this.textRenderer, Text.of("Version"), this.width / 2 - 100, 190, 10526880);
      this.serverNameWidget.render(context, mouseX, mouseY, delta);
      this.serverVersionWidget.render(context, mouseX, mouseY, delta);
      this.serverLangWidget.render(context, mouseX, mouseY, delta);
      this.onlineWidget.render(context, mouseX, mouseY, delta);
      this.crackedWidget.render(context, mouseX, mouseY, delta);
      super.render(context, mouseX, mouseY, delta);
   }

   public void renderBackground(DrawContext context) {
      float vOffset = 0.0F;
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder bufferBuilder = tessellator.getBuffer();
      RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
      RenderSystem.setShaderTexture(0, OPTIONS_BACKGROUND_TEXTURE);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      float f = 32.0F;
      bufferBuilder.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      bufferBuilder.vertex(0.0, this.height, 0.0).texture(0.0F, (float)this.height / 32.0F + vOffset).color(64, 64, 64, 255).next();
      bufferBuilder.vertex(this.width, this.height, 0.0).texture((float)this.width / 32.0F, (float)this.height / 32.0F + vOffset).color(64, 64, 64, 255).next();
      bufferBuilder.vertex(this.width, 0.0, 0.0).texture((float)this.width / 32.0F, vOffset).color(64, 64, 64, 255).next();
      bufferBuilder.vertex(0.0, 0.0, 0.0).texture(0.0F, vOffset).color(64, 64, 64, 255).next();
      tessellator.draw();
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      boolean focusedKeyPress = this.getFocused() != null && this.getFocused().keyPressed(keyCode, scanCode, modifiers);
      return super.keyPressed(keyCode, scanCode, modifiers) || focusedKeyPress;
   }

   public boolean charTyped(char chr, int modifiers) {
      Element focused = this.getFocused();
      return focused != null && focused.charTyped(chr, modifiers);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.clickedWidget(mouseX, mouseY, this.serverNameWidget)) {
         this.serverVersionWidget.setFocused(false);
         this.serverLangWidget.setFocused(false);
         this.setFocused(this.serverNameWidget);
      } else if (this.clickedWidget(mouseX, mouseY, this.serverVersionWidget)) {
         this.serverNameWidget.setFocused(false);
         this.serverLangWidget.setFocused(false);
         this.setFocused(this.serverVersionWidget);
      } else if (this.clickedWidget(mouseX, mouseY, this.serverLangWidget)) {
         this.serverVersionWidget.setFocused(false);
         this.serverNameWidget.setFocused(false);
         this.setFocused(this.serverLangWidget);
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   private boolean clickedWidget(double mouseX, double mouseY, ClickableWidget widget) {
      return mouseX >= (double)widget.getX() && mouseY >= (double)widget.getY() && mouseX < (double)(widget.getX() + widget.getWidth()) && mouseY < (double)(widget.getY() + widget.getHeight());
   }

   public void method_25419() {
      if (this.client != null) {
         this.client.setScreen(this.parent);
      }
   }

   private void saveAndClose() {
      if (this.client != null) {
         this.onChange();
         this.copeService.currentFindRequest.skip = 0;
         this.parent.refreshList();
         this.client.setScreen(this.parent);
      }
   }

   private void onChange() {
      this.copeService.currentFindRequest.hasName = StringUtils.trimToNull(this.serverNameWidget.getText());
      this.copeService.currentFindRequest.hasVersion = StringUtils.trimToEmpty(this.serverVersionWidget.getText());
      this.copeService.currentFindRequest.playersOnline = this.onlineWidget.isChecked();
      this.copeService.currentFindRequest.isCracked = this.crackedWidget.isChecked();
      this.copeService.currentFindRequest.isWhitelisted = false;
      this.copeService.currentFindRequest.isModded = false;
      this.copeService.currentFindRequest.isProtected = false;
      if (!Objects.equals(this.serverLangWidget.getText(), "")) {
         this.copeService.currentFindRequest.lang = this.serverLangWidget.getText();
      } else {
         this.copeService.currentFindRequest.lang = null;
      }

   }

   private static boolean checkboxState(Boolean property) {
      return property != null && property;
   }
}
