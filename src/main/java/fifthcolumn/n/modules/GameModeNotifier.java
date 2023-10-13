package fifthcolumn.n.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;
import net.minecraft.world.GameMode;

public class GameModeNotifier extends Module {
   public GameModeNotifier() {
      super(NAddOn.FIFTH_COLUMN_CATEGORY, "Gamemode Notifier", "Alerts you when someone changes gamemode");
   }

   @EventHandler
   public void onPacket(PacketEvent.Receive event) {
      if (this.mc.getNetworkHandler() != null) {
         if (event.packet instanceof PlayerListS2CPacket packet) {
            for (PlayerListS2CPacket.Entry entry : packet.getEntries()) {
               for (Action action : packet.getActions()) {
                  if (action.equals(Action.UPDATE_GAME_MODE) && !packet.getPlayerAdditionEntries().contains(entry)) {
                     GameMode newGameMode = entry.gameMode();
                     String player = this.mc.getNetworkHandler().getPlayerListEntry(entry.profileId()).getProfile().getName();
                     super.info(player + " has switched to " + newGameMode.getName() + " mode!");
                  }
               }
            }
         }

      }
   }
}
