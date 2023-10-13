package fifthcolumn.n.copenheimer;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fifthcolumn.n.NMod;
import fifthcolumn.n.collar.CollarLogin;
import fifthcolumn.n.events.GrieferUpdateEvent;
import fifthcolumn.n.modules.BanEvasion;
import fifthcolumn.n.modules.GrieferTracer;
import fifthcolumn.n.modules.LarpModule;
import fifthcolumn.n.modules.StreamerMode;
import fifthcolumn.n.modules.WaypointSync;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixin.MinecraftClientAccessor;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.waypoints.Waypoints;
import meteordevelopment.meteorclient.utils.world.Dimension;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.Session;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CopeService {
   private static final Logger LOGGER = LoggerFactory.getLogger(CopeService.class);
   private static final String BASE_URL = "http://cope.fifthcolumnmc.com/";
   private static final Long backgroundRefreshIntervalSeconds = 5L;
   private static final Gson GSON = new Gson();
   private final HttpClient clientDelegate = HttpClient.newBuilder().build();
   public final FindServersRequest currentFindRequest = defaultFindRequest();
   public final Executor executor = Executors.newFixedThreadPool(3, (r) -> {
      Thread thread = new Thread(r);
      thread.setName("CopeService");
      return thread;
   });
   public AtomicBoolean loading = new AtomicBoolean(false);
   public final ScheduledExecutorService backgroundActiveExecutorService = new ScheduledThreadPoolExecutor(1);
   private ServerInfo currentServer;
   private ServerInfo serverInfo;
   private Session defaultSession;
   private ScheduledFuture<?> backgroundActiveRefresh;
   private final ConcurrentHashMap<String, Griefer> griefers = new ConcurrentHashMap();
   private final LoadingCache<TranslateRequest, CompletableFuture<Optional<TranslateResponse>>> translationCache;

   public CopeService() {
      this.translationCache = CacheBuilder.newBuilder().maximumSize(1000L).expireAfterAccess(1L, TimeUnit.HOURS).build(new CacheLoader<>() {
         public CompletableFuture<Optional<TranslateResponse>> load(TranslateRequest req) throws Exception {
            CompletableFuture<Optional<TranslateResponse>> resp = new CompletableFuture();
            CopeService.this.executor.execute(() -> {
               try {
                  String content = CopeService.this.post("http://cope.fifthcolumnmc.com/api/text/translate", req);
                  Optional<TranslateResponse> translateResponse = Optional.ofNullable(CopeService.GSON.fromJson(content, TranslateResponse.class));
                  resp.complete(translateResponse);
               } catch (Throwable var5) {
                  CopeService.LOGGER.error("CopeService Translate Error", var5);
                  resp.complete(Optional.empty());
               }

            });
            return resp;
         }
      });
   }

   public List<Griefer> griefers() {
      return new ArrayList(this.griefers.values());
   }

   public void find(BiConsumer<List<Server>, List<Server>> resultConsumer) {
      this.currentFindRequest.skip = 0;
      this.doFind(resultConsumer);
   }

   public void findMore(BiConsumer<List<Server>, List<Server>> resultConsumer) {
      FindServersRequest var2 = this.currentFindRequest;
      var2.skip = var2.skip + this.currentFindRequest.limit;
      this.doFind(resultConsumer);
   }

   private void doFind(BiConsumer<List<Server>, List<Server>> resultConsumer) {
      this.loading.set(true);
      this.executor.execute(() -> {
         try {
            String content = this.post("http://cope.fifthcolumnmc.com/api/servers/find", this.currentFindRequest);
            FindServersResponse servers = GSON.fromJson(content, FindServersResponse.class);
            this.loading.set(false);
            resultConsumer.accept(servers.searchResult, servers.activeServers);
         } catch (Throwable var4) {
            LOGGER.error("CopeService Find Error", var4);
            this.loading.set(false);
         }

      });
   }

   public void update(UpdateServerRequest req, Consumer<Server> serverConsumer) {
      this.executor.execute(() -> {
         try {
            String content = this.post("http://cope.fifthcolumnmc.com/api/servers/update", req);
            serverConsumer.accept(GSON.fromJson(content, Server.class));
         } catch (Throwable var4) {
            LOGGER.error("CopeService Update Error", var4);
         }

      });
   }

   public void findHistoricalPlayers(Consumer<List<ServerPlayer>> playersConsumer) {
      this.createFindPlayersRequest().ifPresent((req) -> {
         this.executor.execute(() -> {
            try {
               String content = this.post("http://cope.fifthcolumnmc.com/api/servers/findPlayers", req);
               playersConsumer.accept(GSON.fromJson(content, (new TypeToken<ArrayList<ServerPlayer>>() {
               }).getType()));
            } catch (Throwable var4) {
               LOGGER.error("CopeService FindPlayers Error", var4);
            }

         });
      });
   }

   public CompletableFuture<Optional<TranslateResponse>> translate(TranslateRequest request) {
      return this.translationCache.getUnchecked(request);
   }

   public void getAccount(Consumer<GetAccountResponse> consumer) {
      this.executor.execute(() -> {
         String response = this.httpGet("http://cope.fifthcolumnmc.com/api/accounts/alt");
         consumer.accept(GSON.fromJson(response, GetAccountResponse.class));
      });
   }

   public void useNewAlternateAccount(Consumer<Session> sessionConsumer) {
      this.getAccount((resp) -> {
         String username = BanEvasion.isSpacesToNameEnabled() ? "  " + resp.username + "  " : resp.username;
         Session session = new Session(username, resp.uuid, resp.token, Optional.empty(), Optional.empty(), Session.AccountType.MSA);
         ((MinecraftClientAccessor)MeteorClient.mc).setSession(session);
         MeteorClient.mc.getSessionProperties().clear();
         MeteorClient.mc.execute(() -> {
            sessionConsumer.accept(session);
         });
      });
   }

   private void setActive(ActiveServerRequest req) {
      try {
         String body = this.post("http://cope.fifthcolumnmc.com/api/servers/active", req);
         ActiveServerResponse resp = GSON.fromJson(body, ActiveServerResponse.class);
         resp.griefers.forEach((griefer) -> {
            this.griefers.put(griefer.profileName, griefer);
         });
         MeteorClient.EVENT_BUS.post(new GrieferUpdateEvent(resp.griefers));
      } catch (Throwable var4) {
         LOGGER.error("CopeService Active Server Error", var4);
      }

   }

   private String post(String url, Object req) {
      String body = GSON.toJson(req);
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(20L)).header("x-collar-membership", CollarLogin.getMembershipToken()).headers(new String[]{"x-player-name", MeteorClient.mc.getSession().getUsername()}).POST(BodyPublishers.ofString(body)).build();
      return this.execute(request);
   }

   private String httpGet(String url) {
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(20L)).header("x-collar-membership", CollarLogin.getMembershipToken()).headers(new String[]{"x-player-name", MeteorClient.mc.getSession().getUsername()}).GET().build();
      return this.execute(request);
   }

   private String execute(HttpRequest request) {
      HttpResponse response;
      try {
         response = this.clientDelegate.send(request, BodyHandlers.ofString());
      } catch (InterruptedException | IOException var4) {
         throw new IllegalStateException(var4);
      }

      int code = response.statusCode();
      if (code != 200) {
         throw new RuntimeException("status: " + code + " body: " + response.body());
      } else {
         return (String)response.body();
      }
   }

   public void backgroundActiveServerUpdate() {
      if (MeteorClient.mc != null) {
         ServerInfo serverEntry = MeteorClient.mc.getCurrentServerEntry();
         if (serverEntry != null) {
            ClientPlayerEntity player = MeteorClient.mc.player;
            if (player != null) {
               Position location = null;
               Modules modules = Modules.get();
               if (modules.isActive(GrieferTracer.class) && !NMod.is2b2t()) {
                  location = Position.from(player);
               }

               List<Waypoint> waypoints = Collections.emptyList();
               if (modules.isActive(WaypointSync.class) && !NMod.is2b2t()) {
                  waypoints = Waypoints.get().waypoints.values().stream().map((waypoint) -> {
                     Waypoint newWaypoint = new Waypoint();
                     newWaypoint.name = waypoint.name.get();
                     newWaypoint.position = new Position();
                     newWaypoint.position.x = waypoint.getPos().getX();
                     newWaypoint.position.y = waypoint.getPos().getY();
                     newWaypoint.position.z = waypoint.getPos().getZ();
                     if (waypoint.dimension.get() == Dimension.Overworld) {
                        newWaypoint.position.dimension = "OVERWORLD";
                     } else if (waypoint.dimension.get() == Dimension.End) {
                        newWaypoint.position.dimension = "END";
                     } else if (waypoint.dimension.get() == Dimension.Nether) {
                        newWaypoint.position.dimension = "NETHER";
                     }

                     return newWaypoint;
                  }).collect(Collectors.toList());
               }

               String playerNameAlias = modules.isActive(LarpModule.class) ? modules.get(LarpModule.class).aliasName.get() : null;

               try {
                  this.setActive(new ActiveServerRequest(serverEntry.address, player.getEntityName(), MeteorClient.mc.player.getUuid(), playerNameAlias, location, waypoints));
               } catch (Exception var8) {
                  LOGGER.error("Active server request failed.", var8);
               }

            }
         }
      }
   }

   private static FindServersRequest defaultFindRequest() {
      FindServersRequest request = new FindServersRequest();
      request.hasName = null;
      request.hasVersion = SharedConstants.getGameVersion().getName();
      request.playersOnline = true;
      request.isModded = false;
      request.isProtected = false;
      request.isWhitelisted = false;
      request.isGriefed = false;
      request.isCracked = false;
      request.skip = 0;
      request.limit = 100;
      request.lang = null;
      request.multiProtocol = false;
      request.orderBy = "ONLINE_PLAYERS";
      return request;
   }

   public synchronized void startUpdating() {
      if (this.backgroundActiveRefresh == null) {
         this.backgroundActiveRefresh = this.backgroundActiveExecutorService.scheduleAtFixedRate(this::backgroundActiveServerUpdate, 0L, backgroundRefreshIntervalSeconds, TimeUnit.SECONDS);
      }

   }

   public synchronized void stopUpdating() {
      if (this.backgroundActiveRefresh != null) {
         this.backgroundActiveRefresh.cancel(true);
         this.backgroundActiveRefresh = null;
      }
   }

   public Optional<FindPlayersRequest> createFindPlayersRequest() {
      ServerInfo currentServerEntry = MeteorClient.mc.getCurrentServerEntry();
      if (currentServerEntry == null) {
         return Optional.empty();
      } else if (currentServerEntry.equals(this.currentServer)) {
         return Optional.empty();
      } else {
         this.currentServer = currentServerEntry;
         FindPlayersRequest request = new FindPlayersRequest();
         request.serverAddress = currentServerEntry.address;
         return Optional.of(request);
      }
   }

   public void clearTranslations() {
      this.translationCache.invalidateAll();
   }

   public void setLastServerInfo(ServerInfo currentServerEntry) {
      this.serverInfo = currentServerEntry;
   }

   public ServerInfo getLastServerInfo() {
      return this.serverInfo;
   }

   public void setDefaultSession(Session session) {
      this.defaultSession = session;
   }

   public void setDefaultSession() {
      if (this.defaultSession != null) {
         MinecraftClient mc = MinecraftClient.getInstance();
         ((MinecraftClientAccessor)mc).setSession(this.defaultSession);
      }
   }

   public Session getDefaultSession() {
      return this.defaultSession;
   }

   public static final class FindServersRequest {
      public String hasName;
      public String hasVersion;
      public Boolean playersOnline;
      public Boolean isCracked;
      public Boolean isWhitelisted;
      public Boolean isModded;
      public Boolean isProtected;
      public Boolean isGriefed;
      public Integer skip;
      public Integer limit;
      public String lang;
      public Boolean multiProtocol;
      public String orderBy;
   }

   public static final class UpdateServerRequest {
      public String server;
      public Boolean isWhitelisted;
      public Boolean isProtected;
      public Boolean isModded;
      public Boolean isGriefed;
   }

   public static class ActiveServerResponse {
      public List<Griefer> griefers;
   }

   public static final class Position {
      public static final String OVERWORLD = "OVERWORLD";
      public static final String END = "END";
      public static final String NETHER = "NETHER";
      public double x;
      public double y;
      public double z;
      public String dimension;

      public static Position from(PlayerEntity player) {
         Position location = new Position();
         location.x = player.getPos().x;
         location.y = player.getPos().y;
         location.z = player.getPos().z;
         RegistryKey<World> world = player.getWorld().getRegistryKey();
         String dimension;
         if (world == World.OVERWORLD) {
            dimension = "OVERWORLD";
         } else if (world == World.NETHER) {
            dimension = "END";
         } else if (world == World.END) {
            dimension = "NETHER";
         } else {
            dimension = "OVERWORLD";
         }

         location.dimension = dimension;
         return location;
      }

      public String toString() {
         return "Position{x=" + this.x + ", y=" + this.y + ", z=" + this.z + ", dimension='" + this.dimension + "'}";
      }
   }

   public static class ActiveServerRequest {
      public final String server;
      public final String playerName;
      public final UUID playerId;
      public final String playerNameAlias;
      public final Position location;
      public final List<Waypoint> waypoints;

      public ActiveServerRequest(String server, String playerName, UUID playerId, String playerNameAlias, Position position, List<Waypoint> waypoints) {
         this.server = server;
         this.playerName = playerName;
         this.playerId = playerId;
         this.playerNameAlias = playerNameAlias;
         this.location = position;
         this.waypoints = waypoints;
      }
   }

   public static final class FindPlayersRequest {
      public String serverAddress;
   }

   public static final class Waypoint {
      public String name;
      public Position position;
   }

   public static final class Griefer {
      public String profileName;
      public String playerName;
      public UUID playerId;
      public String playerNameAlias;
      public String serverAddress;
      public Position location;
      public List<Waypoint> waypoints;
   }

   public static final class GetAccountResponse {
      public String username;
      public String uuid;
      public String token;
   }

   public static final class Server {
      public String serverAddress;
      public String description;
      public String icon;
      public Set<Griefer> griefers;

      public String displayServerAddress() {
         return displayForServerAddress(this.serverAddress);
      }

      public static String displayForServerAddress(String serverAddress) {
         StreamerMode streamerMode = Modules.get().get(StreamerMode.class);
         if (streamerMode != null && streamerMode.isActive()) {
            try {
               int ipOffset = streamerMode.useRandomIpOffset.get() ? (new Random()).nextInt(1, 254) : 0;
               int ipHeader = (Integer.parseInt(serverAddress.substring(0, serverAddress.indexOf("."))) + ipOffset) % 255;
               return ipHeader + ".xxx.xxx.xxx";
            } catch (StringIndexOutOfBoundsException | NumberFormatException var4) {
               return "Server";
            }
         } else {
            return serverAddress;
         }
      }

      public String displayDescription() {
         if (StreamerMode.isHideServerInfoEnabled()) {
            return "No peeking ;)";
         } else {
            return this.description == null ? "" : this.description.replaceAll("[\ud83c\udf00-\ud83d\ude4f]|[\ud83d\ude80-\ud83d\udeff]", "");
         }
      }

      public Optional<String> iconData() {
         return this.icon != null && this.icon.startsWith("data:image/png;base64,") ? Optional.of(this.icon.substring("data:image/png;base64,".length())) : Optional.empty();
      }
   }

   public static class FindServersResponse {
      public List<Server> activeServers;
      public List<Server> searchResult;
   }

   public static final class TranslateResponse {
      public String text;
      public String lang;
   }

   public static final class TranslateRequest {
      public long uniqueId;
      public String text;
      public String sourceLang;
      public String targetLang;

      public TranslateRequest(long uniqueId, String text, String sourceLang, String targetLang) {
         this.uniqueId = uniqueId;
         this.text = text;
         this.sourceLang = sourceLang;
         this.targetLang = targetLang;
      }

      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else if (o != null && this.getClass() == o.getClass()) {
            TranslateRequest that = (TranslateRequest)o;
            return this.uniqueId == that.uniqueId && Objects.equals(this.text, that.text) && Objects.equals(this.sourceLang, that.sourceLang) && Objects.equals(this.targetLang, that.targetLang);
         } else {
            return false;
         }
      }

      public int hashCode() {
         return Objects.hash(this.uniqueId, this.text, this.sourceLang, this.targetLang);
      }
   }

   public static final class ServerPlayer {
      public String name;
      public Boolean isValid;
   }
}
