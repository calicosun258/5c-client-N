package fifthcolumn.n.events;

import java.util.UUID;
import net.minecraft.util.math.BlockPos;

public record SpawnPlayerEvent(UUID uuid, BlockPos blockPos) {
   public SpawnPlayerEvent(UUID uuid, BlockPos blockPos) {
      this.uuid = uuid;
      this.blockPos = blockPos;
   }

   public UUID uuid() {
      return this.uuid;
   }

   public BlockPos blockPos() {
      return this.blockPos;
   }
}
