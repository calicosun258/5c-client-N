package fifthcolumn.n.mixins;

import fifthcolumn.n.NMod;
import fifthcolumn.n.client.ui.collar.CollarLoginScreen;
import fifthcolumn.n.client.ui.copenheimer.servers.CopeMultiplayerScreen;
import fifthcolumn.n.collar.CollarLogin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import meteordevelopment.meteorclient.MeteorClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(
   value = {TitleScreen.class},
   priority = 1001
)
public abstract class TitleScreenMixin extends Screen {
   private static final List<SplashTextRenderer> SPLASHES = new ArrayList<>();

   private static final Logger LOGGER = LoggerFactory.getLogger(TitleScreenMixin.class);
   private static final int BG_AMT = 25;
   @Shadow
   private @Nullable SplashTextRenderer splashText;
   private final Identifier backgroundId = new Identifier("nc:" + ThreadLocalRandom.current().nextInt(1, BG_AMT+1) + ".jpg");

   protected TitleScreenMixin(Text title) {
      super(title);
   }

   @Inject(
      method = {"init"},
      at = {@At("HEAD")}
   )
   private void n$modifySplashText(CallbackInfo ci) {
      if (this.splashText == null) {
         if(SPLASHES.size() == 0){
            SPLASHES.add(new SplashTextRenderer("Bribe. Grief. Larp. Clout. Repeat."));
            SPLASHES.add(new SplashTextRenderer("Decompiled by househousehouse1"));
            SPLASHES.add(new SplashTextRenderer("Brought to you by The BackStreetBoys of 2b2t"));
         }
         this.splashText = SPLASHES.get(ThreadLocalRandom.current().nextInt(0, SPLASHES.size()));
      }

   }

   @Inject(
      method = {"initWidgetsNormal"},
      at = {@At("TAIL")}
   )
   private void n$addCopenheimerButton(int y, int spacingY, CallbackInfo ci) {
      this.addDrawableChild(ButtonWidget.builder(Text.of("Copenheimer"), (button) -> {
         CopeMultiplayerScreen multiplayerScreen = NMod.getOrCreateMultiplayerScreen(this);
         ForkJoinPool.commonPool().submit(() -> {
            if(NMod.COPE_OFFLINE_MODE) {
               button.active = false;
               button.setMessage(Text.of("Cope is disabled"));
               return;
            }
            Text originalTitle = button.getMessage();
            button.active = false;
            button.setMessage(Text.of("Logging in..."));
            Screen screen = CollarLogin.refreshSession() ? multiplayerScreen : new CollarLoginScreen(multiplayerScreen, this);
            MeteorClient.mc.execute(() -> {
               try {
                  button.active = true;
                  button.setMessage(originalTitle);
                  this.client.setScreen(screen);
               } catch (Exception var5) {
                  LOGGER.error("Could not set screen", var5);
               }

            });
         });
      }).width(200).dimensions(this.width / 2 - 100, y + spacingY * 5, 200, 20).build());
   }

   @Redirect(
      method = {"render"},
      at = @At(
   value = "FIELD",
   target = "Lnet/minecraft/client/gui/screen/TitleScreen;PANORAMA_OVERLAY:Lnet/minecraft/util/Identifier;"
)
   )
   private Identifier n$modifyPanoramaOverlay() {
      return this.backgroundId;
   }
}
