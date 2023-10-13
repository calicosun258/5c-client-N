package fifthcolumn.n.client.ui.copenheimer.servers;

import com.google.common.base.MoreObjects;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import fifthcolumn.n.modules.StreamerMode;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
import meteordevelopment.meteorclient.MeteorClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.logging.UncaughtExceptionLogger;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CopeServerListWidget extends AlwaysSelectedEntryListWidget<CopeServerListWidget.Entry> {
   static final Logger LOGGER = LoggerFactory.getLogger(CopeServerListWidget.class);
   static final ThreadPoolExecutor SERVER_PINGER_THREAD_POOL;
   static final Identifier UNKNOWN_SERVER_TEXTURE;
   static final Identifier SERVER_SELECTION_TEXTURE;
   static final Identifier ICONS_TEXTURE;
   static final Text LAN_SCANNING_TEXT;
   static final Text CANNOT_RESOLVE_TEXT;
   static final Text CANNOT_CONNECT_TEXT;
   static final Text INCOMPATIBLE_TEXT;
   static final Text NO_CONNECTION_TEXT;
   static final Text PINGING_TEXT;
   private final CopeMultiplayerScreen screen;
   private static final List<ServerEntry> SERVERS = new CopyOnWriteArrayList<>();
   private static final List<String> hideServersAddress = new CopyOnWriteArrayList<>();
   private ForkJoinPool serverListPingPool;

   public CopeServerListWidget(CopeMultiplayerScreen multiplayerScreen, MinecraftClient minecraftClient, int i, int j, int k, int l, int m) {
      super(minecraftClient, i, j, k, l, m);
      this.screen = multiplayerScreen;
      this.serverListPingPool = new ForkJoinPool(1);
   }

   public List<ServerEntry> getServers() {
      synchronized(SERVERS) {
         return SERVERS;
      }
   }

   private void updateEntries() {
      synchronized(SERVERS) {
         this.replaceEntries(Collections.unmodifiableList(SERVERS.stream().filter((s) -> !hideServersAddress.contains(s.server.address)).collect(Collectors.toList())));
      }
   }

   public void setSelected(@Nullable Entry entry) {
      super.setSelected(entry);
      this.screen.updateButtonActivationStates();
   }

   public void removeSelectedServerEntry() {
      Optional<Entry> entry = Optional.ofNullable(this.getSelectedOrNull());
      entry.ifPresent((e) -> {
         hideServersAddress.add(((ServerEntry)e).server.address);
         SERVERS.remove(e);
         this.replaceEntries(Collections.unmodifiableList(SERVERS));
      });
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      Entry entry = (Entry)this.getSelectedOrNull();
      return entry != null ? entry.keyPressed(keyCode, scanCode, modifiers) : super.keyPressed(keyCode, scanCode, modifiers);
   }

   public void setServers(ServerList servers) {
      synchronized(SERVERS) {
         SERVERS.clear();
         this.screen.getServerListPinger().cancel();
         this.serverListPingPool.shutdownNow();
         this.serverListPingPool = new ForkJoinPool(100);

         for(int i = 0; i < servers.size(); ++i) {
            SERVERS.add(new ServerEntry(this.screen, servers.get(i)));
         }

         this.updateEntries();
         (new ArrayList<>(SERVERS)).parallelStream().forEach((serverEntry) -> {
            this.serverListPingPool.submit(() -> {
               try {
                  this.screen.getServerListPinger().add(serverEntry.server, () -> {
                     this.serverListPingPool.submit(this::updateEntries);
                  });
               } catch (UnknownHostException var3) {
                  serverEntry.server.ping = -1L;
                  serverEntry.server.label = CANNOT_RESOLVE_TEXT;
               } catch (Exception var4) {
                  serverEntry.server.ping = -1L;
                  serverEntry.server.label = CANNOT_CONNECT_TEXT;
               }

            });
         });
      }
   }

   protected int getScrollbarPositionX() {
      return super.getScrollbarPositionX() + 30;
   }

   public int getRowWidth() {
      return super.getRowWidth() + 85;
   }

   public boolean isFocused() {
      return this.screen.getFocused() == this;
   }

   public void method_25325(DrawContext context) {
      float vOffset = 0.0F;
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder bufferBuilder = tessellator.getBuffer();
      RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
      RenderSystem.setShaderTexture(0, CopeServerInfo.TNT_BLOCK_TEXTURE);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      bufferBuilder.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      bufferBuilder.vertex(0.0, this.height, 0.0).texture(0.0F, (float)this.height / 32.0F + vOffset).color(64, 64, 64, 255).next();
      bufferBuilder.vertex(this.width, this.height, 0.0).texture((float)this.width / 32.0F, (float)this.height / 32.0F + vOffset).color(64, 64, 64, 255).next();
      bufferBuilder.vertex(this.width, 0.0, 0.0).texture((float)this.width / 32.0F, vOffset).color(64, 64, 64, 255).next();
      bufferBuilder.vertex(0.0, 0.0, 0.0).texture(0.0F, vOffset).color(64, 64, 64, 255).next();
      tessellator.draw();
   }

   static {
      SERVER_PINGER_THREAD_POOL = new ScheduledThreadPoolExecutor(25, (new ThreadFactoryBuilder()).setNameFormat("Server Pinger #%d").setDaemon(true).setUncaughtExceptionHandler(new UncaughtExceptionLogger(LOGGER)).build());
      UNKNOWN_SERVER_TEXTURE = new Identifier("textures/misc/unknown_server.png");
      SERVER_SELECTION_TEXTURE = new Identifier("textures/gui/server_selection.png");
      ICONS_TEXTURE = new Identifier("textures/gui/icons.png");
      LAN_SCANNING_TEXT = Text.translatable("lanServer.scanning");
      CANNOT_RESOLVE_TEXT = Text.translatable("multiplayer.status.cannot_resolve").formatted(Formatting.DARK_RED);
      CANNOT_CONNECT_TEXT = Text.translatable("multiplayer.status.cannot_connect").formatted(Formatting.DARK_RED);
      INCOMPATIBLE_TEXT = Text.translatable("multiplayer.status.incompatible");
      NO_CONNECTION_TEXT = Text.translatable("multiplayer.status.no_connection");
      PINGING_TEXT = Text.translatable("multiplayer.status.pinging");
   }

   @Environment(EnvType.CLIENT)
   public abstract static class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> {
   }

   @Environment(EnvType.CLIENT)
   public class ServerEntry extends Entry {
      private final CopeMultiplayerScreen screen;
      private final ServerInfo server;
      private final Identifier iconTextureId;
      private @Nullable NativeImageBackedTexture icon;
      private long time;

      protected ServerEntry(CopeMultiplayerScreen multiplayerScreen, ServerInfo serverInfo) {
         this.screen = multiplayerScreen;
         this.server = serverInfo;
         this.iconTextureId = new Identifier("servers/" + Hashing.sha256().hashUnencodedChars(serverInfo.address) + "/icon");
         AbstractTexture abstractTexture = MeteorClient.mc.getTextureManager().getOrDefault(this.iconTextureId, MissingSprite.getMissingSpriteTexture());
         if (abstractTexture != MissingSprite.getMissingSpriteTexture() && abstractTexture instanceof NativeImageBackedTexture) {
            this.icon = (NativeImageBackedTexture)abstractTexture;
         }

      }

      public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
         synchronized(CopeServerListWidget.SERVERS) {
            if (!this.server.online) {
               this.server.online = true;
               this.server.ping = -2L;
               this.server.label = Text.empty();
               this.server.playerCountLabel = Text.empty();
            }

            boolean bl = this.server.protocolVersion != SharedConstants.getGameVersion().getProtocolVersion();
            TextRenderer textRenderer = MeteorClient.mc.textRenderer;
            context.drawText(textRenderer, this.server.name, x + 32 + 3, y + 1, 16777215, false);
            Text labelText = StreamerMode.isHideServerInfoEnabled() ? Text.of("No peeking ;)") : MoreObjects.firstNonNull(this.server.label, Text.empty());
            List<OrderedText> list = MeteorClient.mc.textRenderer.wrapLines(labelText, entryWidth - 32 - 2);

            int s;
            for(int i = 0; i < Math.min(list.size(), 2); ++i) {
               OrderedText orderedText = list.get(i);
               int textX = x + 32 + 3;
               s = y + 12 + 9 * i;
               context.drawText(textRenderer, orderedText, textX, s, 8421504, false);
            }

            Object text;
            int j;
            if (bl) {
               text = this.server.version.copy().formatted(Formatting.RED);
            } else if (StreamerMode.isHideServerInfoEnabled() && this.server.players != null) {
               j = this.server.players.online() > 0 ? this.server.players.online() + StreamerMode.addFakePlayers() : this.server.players.max() % (StreamerMode.addFakePlayers() + 1);
               text = Text.of(j + " online");
            } else {
               text = this.server.playerCountLabel;
            }

            j = MeteorClient.mc.textRenderer.getWidth((StringVisitable)text);
            context.drawText(textRenderer, (Text)text, x + entryWidth - j - 15 - 2, y + 1, 8421504, false);
            int k = 0;
            List list5;
            Text text5;
            if (bl) {
               s = 5;
               text5 = CopeServerListWidget.INCOMPATIBLE_TEXT;
               list5 = this.server.playerListSummary != null ? this.server.playerListSummary.stream().map(Text::asOrderedText).toList() : List.of();
            } else if (this.server.online && this.server.ping != -2L) {
               if (this.server.ping < 0L) {
                  s = 5;
               } else if (this.server.ping < 150L) {
                  s = 0;
               } else if (this.server.ping < 300L) {
                  s = 1;
               } else if (this.server.ping < 600L) {
                  s = 2;
               } else if (this.server.ping < 1000L) {
                  s = 3;
               } else {
                  s = 4;
               }

               if (this.server.ping < 0L) {
                  text5 = CopeServerListWidget.NO_CONNECTION_TEXT;
                  list5 = Collections.emptyList();
               } else {
                  text5 = Text.translatable("multiplayer.status.ping", this.server.ping);
                  list5 = this.server.playerListSummary != null ? this.server.playerListSummary.stream().map(Text::asOrderedText).toList() : List.of();
               }
            } else {
               k = 1;
               s = (int)(Util.getMeasuringTimeMs() / 100L + (long)index * 2L & 7L);
               if (s > 4) {
                  s = 8 - s;
               }

               text5 = CopeServerListWidget.PINGING_TEXT;
               list5 = Collections.emptyList();
            }

            if (StreamerMode.isHideServerInfoEnabled()) {
               list5 = Collections.emptyList();
            }

            label127: {
               RenderSystem.setShader(GameRenderer::getPositionTexProgram);
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               RenderSystem.setShaderTexture(0, CopeServerListWidget.ICONS_TEXTURE);
               context.drawTexture(CopeServerListWidget.ICONS_TEXTURE, x + entryWidth - 15, y, (float)(k * 10), (float)(176 + s * 8), 10, 8, 256, 256);
               byte[] bytes = this.server.getFavicon();
               this.server.setFavicon(bytes);
               if (this.server instanceof CopeServerInfo copeServer) {
                  if (copeServer.isGriefing()) {
                     this.draw(context, x, y, CopeServerInfo.TNT_BLOCK_TEXTURE);
                     break label127;
                  }
               }

               if (this.icon == null) {
                  this.draw(context, x, y, CopeServerListWidget.UNKNOWN_SERVER_TEXTURE);
               } else {
                  this.draw(context, x, y, this.iconTextureId);
               }
            }

            int t = mouseX - x;
            int u = mouseY - y;
            if (t >= entryWidth - 15 && t <= entryWidth - 5 && u >= 0 && u <= 8) {
               this.screen.setTooltip(List.of(text5.asOrderedText()));
            } else if (t >= entryWidth - j - 15 - 2 && t <= entryWidth - 15 - 2 && u >= 0 && u <= 8) {
               this.screen.setTooltip(list5);
            }

            if (hovered) {
               RenderSystem.setShaderTexture(0, CopeServerListWidget.SERVER_SELECTION_TEXTURE);
               context.fill(x, y, x + 32, y + 32, -1601138544);
               RenderSystem.setShader(GameRenderer::getPositionTexProgram);
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               int v = mouseX - x;
               if (this.canConnect()) {
                  if (v < 32 && v > 16) {
                     context.drawTexture(CopeServerListWidget.SERVER_SELECTION_TEXTURE, x, y, 0.0F, 32.0F, 32, 32, 256, 256);
                  } else {
                     context.drawTexture(CopeServerListWidget.SERVER_SELECTION_TEXTURE, x, y, 0.0F, 0.0F, 32, 32, 256, 256);
                  }
               }
            }

         }
      }

      protected void draw(DrawContext context, int x, int y, Identifier textureId) {
         RenderSystem.setShaderTexture(0, textureId);
         RenderSystem.enableBlend();
         context.drawTexture(textureId, x, y, 0.0F, 0.0F, 32, 32, 32, 32);
         RenderSystem.disableBlend();
      }

      private boolean canConnect() {
         return true;
      }

      public boolean mouseClicked(double mouseX, double mouseY, int button) {
         double d = mouseX - (double)CopeServerListWidget.this.getRowLeft();
         if (d <= 32.0 && d < 32.0 && d > 16.0 && this.canConnect()) {
            this.screen.select(this);
            this.screen.connect();
            return true;
         } else {
            this.screen.select(this);
            if (Util.getMeasuringTimeMs() - this.time < 250L) {
               this.screen.connect();
            }

            this.time = Util.getMeasuringTimeMs();
            return false;
         }
      }

      public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
         if (Screen.isCopy(keyCode)) {
            MeteorClient.mc.keyboard.setClipboard(this.server.address);
            return true;
         } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
         }
      }

      public ServerInfo getServer() {
         return this.server;
      }

      public Text getNarration() {
         return Text.translatable("narrator.select", this.server.name);
      }
   }
}
