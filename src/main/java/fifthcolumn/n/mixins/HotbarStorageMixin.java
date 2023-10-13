package fifthcolumn.n.mixins;

import com.mojang.datafixers.DataFixer;
import java.io.DataInputStream;
import java.io.InputStream;
import net.minecraft.client.option.HotbarStorage;
import net.minecraft.client.option.HotbarStorageEntry;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({HotbarStorage.class})
public abstract class HotbarStorageMixin {
   @Final
   @Shadow
   private DataFixer dataFixer;
   @Final
   @Shadow
   private HotbarStorageEntry[] entries;

   @Inject(
      method = {"load"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void n$loadBuiltInCreativeHotbar(CallbackInfo cb) {
      String HOTBAR_HOTBAR_NBT = "hotbar/hotbar.nbt";
      Logger LOGGER = LoggerFactory.getLogger(HotbarStorageMixin.class);

      try {
         InputStream in = HotbarStorageMixin.class.getClassLoader().getResourceAsStream("hotbar/hotbar.nbt");
         if (in == null) {
            LOGGER.error("Could not find hotbar hotbar/hotbar.nbt");
            return;
         }

         NbtCompound nbtComp = NbtIo.readCompressed(new DataInputStream(in));
         if (nbtComp != null) {
            if (!nbtComp.contains("DataVersion", NbtElement.NUMBER_TYPE)) {
               nbtComp.putInt("DataVersion", 1343);
            }

            nbtComp = DataFixTypes.HOTBAR.update(this.dataFixer, nbtComp, nbtComp.getInt("DataVersion"));

            for(int i = 0; i < 9; ++i) {
               this.entries[i].readNbtList(nbtComp.getList(String.valueOf(i), 10));
            }
         }
      } catch (Exception var7) {
         LOGGER.error("Failed to load creative mode options", var7);
      }

      cb.cancel();
   }
}
