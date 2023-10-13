package fifthcolumn.n.mixins;

import com.google.common.collect.Lists;
import fifthcolumn.n.modules.LecternCrash;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Iterator;
import java.util.List;
import meteordevelopment.meteorclient.mixin.ClientConnectionAccessor;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.LecternScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LecternScreen.class})
public class LecternScreenMixin extends Screen {
   protected LecternScreenMixin(Text title) {
      super(title);
   }

   @Inject(
      method = {"init"},
      at = {@At("TAIL")}
   )
   public void n$addServerCrashButton(CallbackInfo ci) {
      if (Modules.get().get(LecternCrash.class).isActive()) {
         this.addDrawableChild(ButtonWidget.builder(Text.of("Crash Server"), (button) -> {
            ScreenHandler screenHandler = this.client.player.currentScreenHandler;
            DefaultedList<Slot> defaultedList = screenHandler.slots;
            int i = defaultedList.size();
            List<ItemStack> list = Lists.newArrayListWithCapacity(i);

            for (Slot slotx : defaultedList) {
               list.add(slotx.getStack().copy());
            }

            Int2ObjectMap<ItemStack> int2ObjectMap = new Int2ObjectOpenHashMap();

            for(int slot = 0; slot < i; ++slot) {
               ItemStack itemStack = list.get(slot);
               ItemStack itemStack2 = defaultedList.get(slot).getStack();
               if (!ItemStack.areEqual(itemStack, itemStack2)) {
                  int2ObjectMap.put(slot, itemStack2.copy());
               }
            }

            ((ClientConnectionAccessor)this.client.getNetworkHandler().getConnection()).getChannel().writeAndFlush(new ClickSlotC2SPacket(this.client.player.currentScreenHandler.syncId, this.client.player.currentScreenHandler.getRevision(), 0, 0, SlotActionType.QUICK_MOVE, this.client.player.currentScreenHandler.getCursorStack().copy(), int2ObjectMap));
            this.client.player.sendMessage(Text.of("Crashing Server..."), false);
            button.active = false;
         }).dimensions(10, 10, 160, 20).build());
      }

   }
}
