package fifthcolumn.n.client.ui.collar;

import com.mojang.blaze3d.systems.RenderSystem;
import fifthcolumn.n.client.ui.copenheimer.servers.CopeMultiplayerScreen;
import fifthcolumn.n.collar.CollarLogin;
import java.util.concurrent.ForkJoinPool;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CollarLoginScreen extends Screen {
   public static final Identifier OPTIONS_BACKGROUND_TEXTURE = new Identifier("minecraft:textures/block/tnt_side.png");
   private final CopeMultiplayerScreen multiplayerScreen;
   private final Screen titleScreen;
   private TextFieldWidget collarEmailWidget;
   private PasswordTextFieldWidget collarPasswordWidget;
   private ButtonWidget doneButton;
   private String errorMessage;

   public CollarLoginScreen(CopeMultiplayerScreen copeMultiplayerScreen, Screen titleScreen) {
      super(Text.of("Login to collarmc.com"));
      this.multiplayerScreen = copeMultiplayerScreen;
      this.titleScreen = titleScreen;
   }

   public void tick() {
      super.tick();
      if (this.collarEmailWidget.isFocused()) {
         this.collarEmailWidget.tick();
      }

      if (this.collarPasswordWidget.isFocused()) {
         this.collarPasswordWidget.tick();
      }

   }

   protected void init() {
      this.doneButton = this.addDrawableChild(ButtonWidget.builder(Text.of("Login"), (button) -> {
         this.tryToSaveAndClose();
      }).dimensions(this.width / 2 - 100, this.height - 65, 200, 20).build());
      this.addDrawableChild(ButtonWidget.builder(Text.of("Cancel"), (button) -> {
         this.method_25419();
      }).dimensions(this.width / 2 - 100, this.height - 40, 200, 20).build());
      this.collarEmailWidget = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 116, 200, 20, Text.of("email"));
      this.collarEmailWidget.setMaxLength(128);
      this.collarEmailWidget.setFocused(true);
      this.collarEmailWidget.setChangedListener((s) -> {
         this.onChange();
      });
      this.addSelectableChild(this.collarEmailWidget);
      this.collarPasswordWidget = new PasswordTextFieldWidget(this.textRenderer, this.width / 2 - 100, 160, 200, 20, Text.of("password"));
      this.collarPasswordWidget.setMaxLength(128);
      this.collarPasswordWidget.setChangedListener((s) -> {
         this.onChange();
      });
      this.addSelectableChild(this.collarPasswordWidget);
      this.setInitialFocus(this.collarEmailWidget);
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.renderBackground(context);
      context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 16777215);
      context.drawTextWithShadow(this.textRenderer, Text.of("Email address"), this.width / 2 - 100, 100, 10526880);
      context.drawTextWithShadow(this.textRenderer, Text.of("Password"), this.width / 2 - 100, 145, 10526880);
      if (this.errorMessage != null) {
         context.drawTextWithShadow(this.textRenderer, Text.of("Could not login: " + this.errorMessage), this.width / 2 - 100, 200, 10526880);
      }

      this.collarEmailWidget.render(context, mouseX, mouseY, delta);
      this.collarPasswordWidget.render(context, mouseX, mouseY, delta);
      super.render(context, mouseX, mouseY, delta);
   }

   public void renderBackground(DrawContext context) {
      float vOffset = 0.0F;
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder bufferBuilder = tessellator.getBuffer();
      RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
      RenderSystem.setShaderTexture(0, OPTIONS_BACKGROUND_TEXTURE);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      bufferBuilder.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      bufferBuilder.vertex(0.0, this.height, 0.0).texture(0.0F, (float)this.height / 32.0F + vOffset).color(64, 64, 64, 255).next();
      bufferBuilder.vertex(this.width, this.height, 0.0).texture((float)this.width / 32.0F, (float)this.height / 32.0F + vOffset).color(64, 64, 64, 255).next();
      bufferBuilder.vertex(this.width, 0.0, 0.0).texture((float)this.width / 32.0F, vOffset).color(64, 64, 64, 255).next();
      bufferBuilder.vertex(0.0, 0.0, 0.0).texture(0.0F, vOffset).color(64, 64, 64, 255).next();
      tessellator.draw();
   }

   private void onChange() {
      this.errorMessage = null;
   }

   public void method_25419() {
      if (this.client != null) {
         this.client.setScreen(this.titleScreen);
      }
   }

   private void tryToSaveAndClose() {
      if (this.client != null) {
         Text originalText = this.doneButton.getMessage();
         this.doneButton.active = false;
         this.doneButton.setMessage(Text.of("Logging in..."));
         ForkJoinPool.commonPool().submit(() -> {
            CollarLogin.LoginResult result = CollarLogin.loginAndSave(this.collarEmailWidget.getText(), this.collarPasswordWidget.getText());
            MeteorClient.mc.execute(() -> {
               if (result.success) {
                  this.client.setScreen(this.multiplayerScreen);
               } else {
                  this.errorMessage = result.reason;
               }

               this.doneButton.setMessage(originalText);
               this.doneButton.active = true;
            });
         });
      }
   }
}
