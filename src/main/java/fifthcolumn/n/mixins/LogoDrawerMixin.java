package fifthcolumn.n.mixins;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.LogoDrawer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({LogoDrawer.class})
public class LogoDrawerMixin {
   private static final Identifier N_LOGO = new Identifier("nc:title.png");

   @Redirect(
      method = {"draw(Lnet/minecraft/client/gui/DrawContext;IFI)V"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIFFIIII)V",
   ordinal = 0
)
   )
   public void redirectDrawLogo(DrawContext instance, Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
      instance.drawTexture(N_LOGO, x, y, u, v, width, 250, textureWidth, textureHeight);
   }
}
