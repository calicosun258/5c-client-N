package fifthcolumn.n.mixins;

import fifthcolumn.n.NMod;
import fifthcolumn.n.copenheimer.CopeService;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GameMenuScreen.class})
public abstract class GameMenuScreenMixin extends Screen {
   public GameMenuScreenMixin(Text text) {
      super(text);
   }

   @Inject(
      method = {"initWidgets"},
      at = {@At("TAIL")}
   )
   private void n$addCopeBookmark(CallbackInfo info) {
      ServerInfo entry = MinecraftClient.getInstance().getCurrentServerEntry();
      if (entry != null) {
         String serverAddress = CopeService.Server.displayForServerAddress(entry.address);
         this.addDrawableChild(ButtonWidget.builder(Text.of("Bookmark " + serverAddress), (button) -> {
            ServerList serverList = new ServerList(MeteorClient.mc);
            serverList.loadFile();
            serverList.add(entry, false);
            serverList.saveFile();
            MinecraftClient.getInstance().keyboard.setClipboard(entry.address);
         }).dimensions(this.width / 2 - 102, this.height / 4 + 144 - 16, 204, 20).build());
      }

   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (Screen.isCopy(keyCode)) {
         ServerInfo serverEntry = MeteorClient.mc.getCurrentServerEntry();
         if (serverEntry != null) {
            MeteorClient.mc.keyboard.setClipboard(serverEntry.address);
            return true;
         }
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Inject(
      method = {"disconnect"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/MinecraftClient;setScreen(Lnet/minecraft/client/gui/screen/Screen;)V",
   shift = Shift.AFTER,
   ordinal = 2
)},
      cancellable = true
   )
   private void n$changeMultiplayerScreen(CallbackInfo ci) {
      this.client.setScreen(NMod.getMultiplayerScreen());
      ci.cancel();
   }
}
