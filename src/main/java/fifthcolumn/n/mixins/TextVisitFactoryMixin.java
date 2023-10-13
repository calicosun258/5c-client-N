package fifthcolumn.n.mixins;

import fifthcolumn.n.modules.LarpModule;
import fifthcolumn.n.modules.StreamerMode;
import net.minecraft.text.TextVisitFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin({TextVisitFactory.class})
public class TextVisitFactoryMixin {
   @ModifyVariable(
      method = {"visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z"},
      at = @At("HEAD"),
      ordinal = 0,
      index = 0,
      argsOnly = true
   )
   private static String n$modifyAllPlayerNameInstances(String text) {
      text = LarpModule.modifyPlayerNameInstances(text);
      text = StreamerMode.anonymizePlayerNameInstances(text);
      return text;
   }
}
