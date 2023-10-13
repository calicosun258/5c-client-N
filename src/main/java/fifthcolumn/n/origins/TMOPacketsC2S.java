package fifthcolumn.n.origins;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;

public class TMOPacketsC2S {
   public static void register() {
      ServerLoginConnectionEvents.QUERY_START.register(TMOPacketsC2S::handshake);
      ServerLoginNetworking.registerGlobalReceiver(TMOPackets.HANDSHAKE, (server, handler, understood, buf, synchronizer, responseSender) -> {
      });
   }

   private static void handshake(ServerLoginNetworkHandler serverLoginNetworkHandler, MinecraftServer minecraftServer, PacketSender packetSender, ServerLoginNetworking.LoginSynchronizer loginSynchronizer) {
      packetSender.sendPacket(TMOPackets.HANDSHAKE, PacketByteBufs.empty());
   }
}
