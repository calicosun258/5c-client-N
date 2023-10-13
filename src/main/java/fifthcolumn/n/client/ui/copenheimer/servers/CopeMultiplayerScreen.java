package fifthcolumn.n.client.ui.copenheimer.servers;

import fifthcolumn.n.NMod;
import fifthcolumn.n.client.ui.copenheimer.search.SearchParametersScreen;
import fifthcolumn.n.copenheimer.CopeService;
import fifthcolumn.n.modules.StreamerMode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.stream.Stream;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiThemes;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.AllowedAddressResolver;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

public class CopeMultiplayerScreen extends Screen {
   private static final int BUTTON_WIDTH_TOP = 80;
   private static final int BUTTON_WIDTH_BOTTOM = 80;
   private static final int BUTTON_HEIGHT = 20;
   private final MultiplayerServerListPinger serverListPinger = new MultiplayerServerListPinger();
   private final Screen parent;
   private final CopeService copeService;
   protected CopeServerListWidget serverListWidget;
   private ServerList serverList;
   private ButtonWidget buttonModded;
   private ButtonWidget buttonJoin;
   private ButtonWidget protectedButton;
   private ButtonWidget whiteListedButton;
   private ButtonWidget griefedButton;
   private ButtonWidget newAltButton;
   private boolean initialized;

   public CopeMultiplayerScreen(Screen screen, CopeService copeService) {
      super(Text.of("Copenheimer"));
      this.parent = screen;
      this.copeService = copeService;
   }

   protected void init() {
      super.init();
      if (this.client != null) {
         if (this.initialized) {
            this.serverListWidget.updateSize(this.width, this.height, 32, this.height - 64);
         } else {
            this.initialized = true;
            this.serverList = new ServerList();
            this.serverListWidget = new CopeServerListWidget(this, this.client, this.width, this.height, 32, this.height - 64, 36);
            this.serverListWidget.setServers(this.serverList);
            this.refreshList();
            AllowedAddressResolver allowedAddressResolver = AllowedAddressResolver.DEFAULT;
         }

         int buttonTopRowY = this.height - 52;
         int buttonBottomRowY = this.height - 28;
         int padding = 4;
         int farLeftXTopRow = this.width / 2 - 210;
         int farLeftXBottomRow = this.width / 2 - 210;
         this.addSelectableChild(this.serverListWidget);
         this.buttonJoin = this.addDrawableChild(ButtonWidget.builder(Text.translatable("selectServer.select"), (button) -> {
            this.connect();
         }).dimensions(farLeftXTopRow, buttonTopRowY, 80, 20).build());
         this.addDrawableChild(ButtonWidget.builder(Text.of("Update search"), (button) -> {
            this.client.setScreen(new SearchParametersScreen((CopeMultiplayerScreen)this.client.currentScreen, this.copeService));
         }).dimensions(farLeftXTopRow + padding + 80, buttonTopRowY, 80, 20).build());
         this.addDrawableChild(ButtonWidget.builder(Text.of("Hide Server"), (button) -> {
            this.serverListWidget.removeSelectedServerEntry();
         }).dimensions(farLeftXTopRow + 168, buttonTopRowY, 80, 20).build());
         this.addDrawableChild(ButtonWidget.builder(Text.of("Show More"), (button) -> {
            if (!this.copeService.loading.get()) {
               this.showMore();
            }

         }).dimensions(farLeftXTopRow + 252, buttonTopRowY, 80, 20).build());
         this.addDrawableChild(ButtonWidget.builder(Text.of("Refresh"), (button) -> {
            if (!this.copeService.loading.get()) {
               this.refreshList();
            }

         }).dimensions(farLeftXTopRow + 336, buttonTopRowY, 80, 20).build());
         this.protectedButton = this.addDrawableChild(ButtonWidget.builder(Text.of("Protected"), (button) -> {
            CopeService.UpdateServerRequest req = new CopeService.UpdateServerRequest();
            req.isProtected = true;
            this.serverListWidget.removeSelectedServerEntry();
            this.updateServer(req);
         }).dimensions(farLeftXBottomRow, buttonBottomRowY, 80, 20).build());
         this.whiteListedButton = this.addDrawableChild(ButtonWidget.builder(Text.of("Whitelisted"), (button) -> {
            CopeService.UpdateServerRequest req = new CopeService.UpdateServerRequest();
            req.isWhitelisted = true;
            this.serverListWidget.removeSelectedServerEntry();
            this.updateServer(req);
         }).dimensions(farLeftXBottomRow + 4 + 80, buttonBottomRowY, 80, 20).build());
         this.buttonModded = ButtonWidget.builder(Text.translatable("Modded"), (button) -> {
            CopeService.UpdateServerRequest req = new CopeService.UpdateServerRequest();
            req.isModded = true;
            this.serverListWidget.removeSelectedServerEntry();
            this.updateServer(req);
         }).dimensions(farLeftXBottomRow + 168, buttonBottomRowY, 80, 20).build();
         this.addDrawableChild(this.buttonModded);
         this.griefedButton = ButtonWidget.builder(Text.translatable("Griefed"), (button) -> {
            CopeService.UpdateServerRequest req = new CopeService.UpdateServerRequest();
            req.isGriefed = true;
            this.serverListWidget.removeSelectedServerEntry();
            this.updateServer(req);
         }).dimensions(farLeftXBottomRow + 252, buttonBottomRowY, 80, 20).build();
         this.addDrawableChild(this.griefedButton);
         this.addDrawableChild(ButtonWidget.builder(Text.of("Cancel"), (button) -> {
            NMod.setMultiplayerScreen(null);
            this.client.setScreen(this.parent);
         }).dimensions(farLeftXBottomRow + 336, buttonBottomRowY, 80, 20).build());
         this.updateButtonActivationStates();
         CopeService copeService = NMod.getCopeService();
         this.addDrawableChild(ButtonWidget.builder(Text.of("Proxies"), (button) -> {
            this.client.setScreen(GuiThemes.get().proxiesScreen());
         }).dimensions(this.width - 75 - 3, 3, 75, 20).build());
         this.addDrawableChild(ButtonWidget.builder(Text.of("Accounts"), (button) -> {
            this.client.setScreen(GuiThemes.get().accountsScreen());
         }).dimensions(this.width - 75 - 3 - 75 - 2, 3, 75, 20).build());
         this.newAltButton = ButtonWidget.builder(Text.of("New alt"), (button) -> {
            this.newAltButton.active = false;
            copeService.useNewAlternateAccount((session) -> {
               MeteorClient.mc.execute(() -> {
                  this.newAltButton.active = true;
               });
            });
         }).dimensions(this.width - 75 - 3 - 150 - 2, 3, 75, 20).build();
         this.addDrawableChild(this.newAltButton);
         this.addDrawableChild(ButtonWidget.builder(Text.of("Reset account"), (button) -> {
            copeService.setDefaultSession();
         }).dimensions(this.width - 75 - 3 - 230 - 2, 3, 80, 20).build());
      }
   }

   public CopeServerListWidget getServerListWidget() {
      return this.serverListWidget;
   }

   public void tick() {
      super.tick();
      this.serverListPinger.tick();
   }

   public void removed() {
      assert this.client != null;

      this.serverListPinger.cancel();
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (super.keyPressed(keyCode, scanCode, modifiers)) {
         return true;
      } else if (keyCode == 294) {
         this.refreshList();
         return true;
      } else if (this.serverListWidget.getSelectedOrNull() != null) {
         if (keyCode != 257 && keyCode != 335) {
            return this.serverListWidget.keyPressed(keyCode, scanCode, modifiers);
         } else {
            this.connect();
            return true;
         }
      } else {
         return false;
      }
   }

   private String getLoadingText() {
      long dotsCount = Instant.now().getEpochSecond() % 5L;
      StringBuilder text = new StringBuilder("Loading");

      for(int i = 0; (long)i <= dotsCount; ++i) {
         text.append(".");
      }

      return text.toString();
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.renderBackground(context);

      try {
         this.serverListWidget.render(context, mouseX, mouseY, delta);
      } catch (Exception var6) {
         var6.printStackTrace();
      }

      context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 16777215);
      if (this.copeService.loading.get() && this.serverListWidget.getServers().isEmpty()) {
         context.drawCenteredTextWithShadow(this.textRenderer, this.getLoadingText(), this.width / 2, 50, 16777215);
      }

      super.render(context, mouseX, mouseY, delta);
      if (!StreamerMode.isHideAccountEnabled()) {
         String username = MeteorClient.mc.getSession().getUsername();
         context.drawTextWithShadow(this.textRenderer, Text.of("Logged in as: " + username), 10, 10, 10526880);
      }

   }

   public void connect() {
      CopeServerListWidget.Entry entry = (CopeServerListWidget.Entry)this.serverListWidget.getSelectedOrNull();
      if (entry instanceof CopeServerListWidget.ServerEntry) {
         this.connect(((CopeServerListWidget.ServerEntry)entry).getServer());
      }

   }

   private void connect(ServerInfo entry) {
      assert this.client != null;

      ConnectScreen.connect(this, this.client, ServerAddress.parse(entry.address), entry, false);
   }

   public void select(CopeServerListWidget.Entry entry) {
      this.serverListWidget.setSelected(entry);
      this.updateButtonActivationStates();
   }

   protected void updateButtonActivationStates() {
      this.buttonJoin.active = false;
      this.whiteListedButton.active = false;
      this.protectedButton.active = false;
      this.buttonModded.active = false;
      this.griefedButton.active = false;
      CopeServerListWidget.Entry entry = (CopeServerListWidget.Entry)this.serverListWidget.getSelectedOrNull();
      if (entry != null) {
         this.buttonJoin.active = true;
         if (entry instanceof CopeServerListWidget.ServerEntry) {
            this.whiteListedButton.active = true;
            this.protectedButton.active = true;
            this.buttonModded.active = true;
            this.griefedButton.active = true;
         }
      }

   }

   public void refreshList() {
      CopeServerListWidget serverListWidget = this.getServerListWidget();
      if (serverListWidget != null) {
         serverListWidget.setServers(new ServerList());
         this.copeService.currentFindRequest.skip = 0;
         this.copeService.find(this::setCopeServers);
      }

   }

   public void showMore() {
      CopeServerListWidget serverListWidget = this.getServerListWidget();
      if (serverListWidget != null) {
         this.copeService.findMore(this::addCopeServers);
      }

   }

   private void updateServer(CopeService.UpdateServerRequest req) {
      ForkJoinPool.commonPool().submit(() -> {
         CopeServerListWidget.ServerEntry entry = (CopeServerListWidget.ServerEntry)this.serverListWidget.getSelectedOrNull();
         if (entry != null) {
            req.server = entry.getServer().address;
            this.copeService.update(req, (server) -> {
            });
         }

      });
   }

   private void setCopeServers(List<CopeService.Server> servers, List<CopeService.Server> activeServers) {
      this.serverList = new ServerList();
      this.mapServers(servers, activeServers);
      this.serverListWidget.setSelected((CopeServerListWidget.Entry)null);
      this.serverListWidget.setServers(this.serverList);
   }

   private void addCopeServers(List<CopeService.Server> servers, List<CopeService.Server> activeServers) {
      this.mapServers(servers, activeServers);
   }

   private void mapServers(List<CopeService.Server> servers, List<CopeService.Server> activeServers) {
      this.addServers(activeServers, (server) -> {
         if (server.griefers != null && !server.griefers.isEmpty()) {
            StringJoiner joiner = new StringJoiner(",");
            Stream<String> var10000 = server.griefers.stream().map((griefer) -> StreamerMode.isStreaming() ? griefer.playerNameAlias : griefer.profileName);
            var10000.forEach(joiner::add);
            return server.displayServerAddress() + " with " + joiner;
         } else {
            return server.displayServerAddress();
         }
      });
      this.addServers(servers, CopeService.Server::displayServerAddress);
   }

   private void addServers(List<CopeService.Server> servers, Function<CopeService.Server, String> name) {
      servers.stream().map((found) -> new CopeServerInfo(name.apply(found), found)).forEach((serverInfo) -> {
         this.serverList.add(serverInfo);
      });
      this.serverListWidget.setServers(this.serverList);
   }

   public MultiplayerServerListPinger getServerListPinger() {
      return this.serverListPinger;
   }
}
