package fifthcolumn.n.modules;

import fifthcolumn.n.NMod;
import fifthcolumn.n.copenheimer.CopeService;
import fifthcolumn.n.events.GrieferUpdateEvent;
import fifthcolumn.n.events.PlayerSpawnPositionEvent;
import fifthcolumn.n.events.SpawnPlayerEvent;
import fifthcolumn.n.utils.BlockPosUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.waypoints.Waypoint;
import meteordevelopment.meteorclient.systems.waypoints.Waypoints;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;

public class WaypointSync extends Module {
   public WaypointSync() {
      super(NAddOn.FIFTH_COLUMN_CATEGORY, "Waypoint Sync", "Syncs your waypoints. Disabled on 2b2t.org.");
   }

   @EventHandler
   public void spawnPosition(PlayerSpawnPositionEvent event) {
      this.mc.execute(() -> {
         Waypoints waypoints = Waypoints.get();
         waypoints.add((new Waypoint.Builder()).name("Spawn").pos(event.blockPos()).build());
         waypoints.save();
      });
   }

   @EventHandler
   private void onPlayerSeen(SpawnPlayerEvent event) {
      this.mc.world.getPlayers().stream().filter((player) -> {
         return player.getGameProfile().getId().equals(event.uuid());
      }).findFirst().ifPresent((player) -> {
         long count = NMod.getCopeService().griefers().stream().filter((griefer) -> {
            return griefer.playerId.equals(event.uuid());
         }).count();
         if (count <= 0L) {
            Waypoints waypoints = Waypoints.get();
            String playerName = "Player " + player.getEntityName();
            Waypoint existingWaypoint = waypoints.get(playerName);
            if (existingWaypoint == null) {
               Waypoint.Builder waypointBuilder = new Waypoint.Builder();
               waypointBuilder.name(playerName);
               waypointBuilder.pos(event.blockPos());
               waypointBuilder.icon("5c");
               Waypoint waypoint = waypointBuilder.build();
               waypoints.add(waypoint);
               waypoints.save();
            }
         }
      });
   }

   @EventHandler
   public void griefersUpdated(GrieferUpdateEvent event) {
      if (Modules.get().get(WaypointSync.class).isActive()) {
         ServerInfo currentServer = this.mc.getCurrentServerEntry();
         if (currentServer != null && this.mc.player != null) {
            Map<String, CopeService.Waypoint> remoteWaypoints = event.griefers.stream().filter((griefer) -> {
               return !griefer.playerName.equalsIgnoreCase(this.mc.player.getEntityName());
            }).filter((griefer) -> {
               return griefer.serverAddress.equalsIgnoreCase(currentServer.address);
            }).flatMap((griefer) -> {
               return griefer.waypoints.stream();
            }).collect(Collectors.toMap((waypoint) -> {
               return waypoint.name;
            }, (waypoint) -> {
               return waypoint;
            }, (waypoint, waypoint2) -> {
               return waypoint;
            }));
            this.mc.execute(() -> {
               Waypoints waypoints = Waypoints.get();
               boolean waypointUpdated = false;
               Iterator var3 = remoteWaypoints.entrySet().iterator();

               while(true) {
                  CopeService.Waypoint remoteWaypoint;
                  Waypoint existingWaypoint;
                  do {
                     if (!var3.hasNext()) {
                        if (waypointUpdated) {
                           waypoints.save();
                        }

                        return;
                     }

                     Map.Entry<String, CopeService.Waypoint> entry = (Map.Entry)var3.next();
                     String s = (String)entry.getKey();
                     remoteWaypoint = (CopeService.Waypoint)entry.getValue();
                     existingWaypoint = waypoints.get(remoteWaypoint.name);
                  } while(existingWaypoint != null && ((String)existingWaypoint.name.get()).equals(remoteWaypoint.name));

                  Waypoint.Builder waypointBuilder = new Waypoint.Builder();
                  waypointBuilder.name(remoteWaypoint.name);
                  waypointBuilder.pos(BlockPosUtils.from(remoteWaypoint.position));
                  waypointBuilder.icon("5c");
                  if (remoteWaypoint.position.dimension.equals("OVERWORLD")) {
                     waypointBuilder = waypointBuilder.dimension(Dimension.Nether);
                  } else if (remoteWaypoint.position.dimension.equals("END")) {
                     waypointBuilder = waypointBuilder.dimension(Dimension.End);
                  } else {
                     waypointBuilder = waypointBuilder.dimension(Dimension.Overworld);
                  }

                  Waypoint waypoint = waypointBuilder.build();
                  waypoints.add(waypoint);
                  waypointUpdated = true;
               }
            });
         }
      }
   }

   static {
      try {
         InputStream inputStream = WaypointSync.class.getResourceAsStream("/assets/nc/5c.png");

         try {
            Waypoints.get().icons.put("5c", new NativeImageBackedTexture(NativeImage.read(Objects.requireNonNull(inputStream))));
         } catch (Throwable var4) {
            if (inputStream != null) {
               try {
                  inputStream.close();
               } catch (Throwable var3) {
                  var4.addSuppressed(var3);
               }
            }

            throw var4;
         }

         inputStream.close();

      } catch (IOException var5) {
         throw new RuntimeException("did not load 5c icon");
      }
   }
}
