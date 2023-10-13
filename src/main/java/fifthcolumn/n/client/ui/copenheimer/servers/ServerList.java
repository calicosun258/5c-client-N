package fifthcolumn.n.client.ui.copenheimer.servers;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.client.network.ServerInfo;

public class ServerList {
   private final List<ServerInfo> servers = Lists.newArrayList();

   public ServerInfo get(int index) {
      return (ServerInfo)this.servers.get(index);
   }

   public void remove(ServerInfo serverInfo) {
      this.servers.remove(serverInfo);
   }

   public void add(ServerInfo serverInfo) {
      this.servers.add(serverInfo);
   }

   public int size() {
      return this.servers.size();
   }

   public void set(int index, ServerInfo serverInfo) {
      this.servers.set(index, serverInfo);
   }
}
