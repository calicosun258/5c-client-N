package fifthcolumn.n.events;

import net.minecraft.util.math.BlockPos;

public record PlayerSpawnPositionEvent(BlockPos blockPos) {
   public PlayerSpawnPositionEvent(BlockPos blockPos) {
      this.blockPos = blockPos;
   }

   public BlockPos blockPos() {
      return this.blockPos;
   }
}
