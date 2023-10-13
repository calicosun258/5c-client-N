package fifthcolumn.n.client.ui.copenheimer.servers;

import fifthcolumn.n.copenheimer.CopeService;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class CopeServerInfo extends ServerInfo {
   public static final Identifier TNT_BLOCK_TEXTURE = new Identifier("minecraft:textures/block/tnt_side.png");
   private final CopeService.Server server;

   public CopeServerInfo(String name, CopeService.Server server) {
      super(name, server.serverAddress, false);
      this.server = server;
      this.online = true;
      this.playerCountLabel = Text.of("Updating...");
      this.playerListSummary = new ArrayList();
      this.label = Text.of(server.displayDescription());
      server.iconData().ifPresent((s) -> {
         this.setFavicon(this.toString().getBytes(StandardCharsets.UTF_8));
      });
   }

   public boolean isGriefing() {
      return !this.server.griefers.isEmpty();
   }
}
