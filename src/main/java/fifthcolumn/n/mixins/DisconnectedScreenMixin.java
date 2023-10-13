package fifthcolumn.n.mixins;

import fifthcolumn.n.NMod;
import fifthcolumn.n.client.ui.copenheimer.servers.CopeMultiplayerScreen;
import fifthcolumn.n.copenheimer.CopeService;
import fifthcolumn.n.modules.BanEvasion;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin({DisconnectedScreen.class})
public abstract class DisconnectedScreenMixin extends Screen {
   @Unique
   private ButtonWidget switchAltButton;

   protected DisconnectedScreenMixin(Text title) {
      super(title);
   }

   @Inject(
      method = {"init"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/widget/GridWidget;refreshPositions()V"
)},
      locals = LocalCapture.CAPTURE_FAILHARD
   )
   private void n$banEvasion(CallbackInfo ci, GridWidget.Adder adder, ButtonWidget buttonWidget) {
      if (Modules.get().isActive(BanEvasion.class) && Modules.get().get(BanEvasion.class).evadeAndReconnect.get()) {
         ButtonWidget banEvadeButton = ButtonWidget.builder(Text.of("Evade ban and reconnect"), (button) -> {
            this.switchAltButton.active = false;
            CopeService copeService = NMod.getCopeService();
            copeService.useNewAlternateAccount((session) -> {
               ConnectScreen.connect(new CopeMultiplayerScreen(new TitleScreen(), copeService), this.client, ServerAddress.parse(Modules.get().get(BanEvasion.class).lastServer.address), Modules.get().get(BanEvasion.class).lastServer, false);
               this.switchAltButton.active = true;
            });
         }).build();
         adder.add(banEvadeButton);
      }

   }
}
