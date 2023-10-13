package fifthcolumn.n.mixins;

import fifthcolumn.n.modules.InventoryDupe;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({InventoryScreen.class})
public abstract class InventoryScreenMixin extends AbstractInventoryScreen<PlayerScreenHandler> {
   public InventoryScreenMixin(PlayerScreenHandler container, PlayerInventory playerInventory, Text name) {
      super(container, playerInventory, name);
   }

   @Inject(
      method = {"init"},
      at = {@At("TAIL")}
   )
   private void n$addInventoryDupeButton(CallbackInfo ci) {
      if (Modules.get().get(InventoryDupe.class).isActive()) {
         this.addDrawableChild(ButtonWidget.builder(Text.of("Dupe"), (button) -> {
            this.dupe();
         }).dimensions(this.x + 130, this.height / 2 - 24, 40, 20).build());
      }

   }

   private void dupe() {
      if (Modules.get().get(InventoryDupe.class).isActive()) {
         Slot outputSlot = this.handler.slots.get(0);
         this.onMouseClick(outputSlot, outputSlot.id, 0, SlotActionType.THROW);
      }

   }
}
