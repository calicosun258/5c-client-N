package fifthcolumn.n.modules;

import com.google.common.net.InetAddresses;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.AbstractSignEditScreenAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.network.packet.s2c.play.SignEditorOpenS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.world.chunk.WorldChunk;
import org.apache.commons.lang3.StringUtils;

public class AutoSign extends Module {
   private final SettingGroup sgGeneral;
   private final Setting<TextPreset> textPreset;
   private final Setting<String> signTextLine1;
   private final Setting<String> signTextLine2;
   private final Setting<String> signTextLine3;
   private final Setting<String> signTextLine4;
   private static final String ticketNumberReplace = "<ticketNumber>";
   private final Setting<Boolean> editSignAura;
   private final Setting<Double> distance;
   private final Setting<Integer> delay;
   private Instant lastEdit;
   private boolean interacting;
   private Instant interactTime;
   private final Duration interactTimeout;

   public AutoSign() {
      super(NAddOn.FIFTH_COLUMN_CATEGORY, "AutoSign", "Places bait signs to the 5c discord");
      this.sgGeneral = this.settings.getDefaultGroup();
      this.textPreset = this.sgGeneral.add(new EnumSetting.Builder<TextPreset>()
              .name("Text Preset")
              .description("What do sign say?")
              .defaultValue(TextPreset.FifthColumn)
              .build());
      this.signTextLine1 = this.sgGeneral.add(new StringSetting.Builder()
              .name("Line 1")
              .description("The text to put on the sign line 1.")
              .defaultValue("Rekt by")
              .visible(() -> this.textPreset.get() == TextPreset.Custom)
              .build());
      this.signTextLine2 = this.sgGeneral.add(new StringSetting.Builder().name("Line 2")
              .description("The text to put on the sign line 2.")
              .defaultValue("discord.gg/")
              .visible(() -> this.textPreset.get() == TextPreset.Custom)
              .build());
      this.signTextLine3 = this.sgGeneral.add(new StringSetting.Builder()
              .name("Line 3")
              .description("The text to put on the sign line 3.")
              .defaultValue("thefifthcolumn")
              .visible(() -> this.textPreset.get() == TextPreset.Custom)
              .build());
      this.signTextLine4 = this.sgGeneral.add(new StringSetting.Builder()
              .name("Line 4")
              .description("The text to put on the sign line 4.")
              .defaultValue(ticketNumberReplace)
              .visible(() -> this.textPreset.get() == TextPreset.Custom)
              .build());
      this.editSignAura = this.sgGeneral.add(new BoolSetting.Builder()
              .name("Edit Sign Aura")
              .description("Automatically edits signs around you.")
              .defaultValue(false)
              .build());
      this.distance = this.sgGeneral.add(new DoubleSetting.Builder()
              .name("Distance")
              .description("The distance to search for signs.")
              .defaultValue(5.0D)
              .min(0.1D)
              .sliderMax(10.0D)
              .visible(this.editSignAura::get)
              .build());
      this.delay = this.sgGeneral.add((new IntSetting.Builder()).name("Delay").description("The delay between editing signs.").defaultValue(100).min(0).sliderMax(1000).visible(this.editSignAura::get)
              .build());

      this.lastEdit = Instant.EPOCH;
      this.interacting = false;
      this.interactTime = Instant.EPOCH;
      this.interactTimeout = Duration.ofMillis(500L);
   }

   @EventHandler
   public void onOpenScreen(OpenScreenEvent event) {
      if (this.isActive()) {
         if (event.screen instanceof AbstractSignEditScreenAccessor) {
            event.cancel();
         }

      }
   }

   private UpdateSignC2SPacket getUpdateSignPacket(BlockPos pos, boolean front) {
      UpdateSignC2SPacket packet;
      switch (this.textPreset.get()) {
         case FifthColumn:
            packet = new UpdateSignC2SPacket(pos, front, "Rekt by", "discord.gg/", "thefifthcolumn", "#" + this.getTicketNumber());
            break;
         case Astral:
            packet = new UpdateSignC2SPacket(pos, front, "Rekt by Astral", "discord.gg/", "e58M9R5TDA", "#<" + this.getTicketNumber() + ">");
            break;
         default:
            packet = new UpdateSignC2SPacket(pos, front, this.replaceText(this.signTextLine1.get()), this.replaceText(this.signTextLine2.get()), this.replaceText(this.signTextLine3.get()), this.replaceText(this.signTextLine4.get()));
      }

      return packet;
   }

   private String replaceText(String text) {
      return StringUtils.replace(text, "<ticketNumber>", this.getTicketNumber());
   }

   @EventHandler
   public void onTick(TickEvent.Post event) {
      if (this.mc.player != null) {
         if (this.mc.world != null) {
            if (this.editSignAura.get()) {
               if (!Instant.now().isBefore(this.lastEdit.plusMillis((long) this.delay.get()))) {
                  this.lastEdit = Instant.now();
                  if (!this.interacting || !Instant.now().isBefore(this.interactTime.plus(this.interactTimeout))) {
                     ChunkPos playerChunkPos = this.mc.player.getChunkPos();

                     for(int x = -1; x <= 1; ++x) {
                        for(int z = -1; z <= 1; ++z) {
                           WorldChunk chunk = this.mc.world.getChunk(playerChunkPos.x + x, playerChunkPos.z + z);
                           Map<BlockPos, BlockEntity> blockEntities = chunk.getBlockEntities();

                           for (Map.Entry<BlockPos, BlockEntity> blockPosBlockEntityEntry : blockEntities.entrySet()) {
                              Map.Entry<BlockPos, BlockEntity> entry = blockPosBlockEntityEntry;
                              BlockPos blockPos = entry.getKey();
                              BlockEntity blockEntity = entry.getValue();
                              if (blockEntity instanceof SignBlockEntity signBlockEntity) {
                                 if (!signBlockEntity.isWaxed() && blockPos.isWithinDistance(this.mc.player.getEyePos(), this.distance.get()) && this.shouldUpdateText(signBlockEntity)) {
                                    RaycastContext raycastContext = new RaycastContext(new Vec3d(this.mc.player.getX(), this.mc.player.getEyePos().y, this.mc.player.getZ()), new Vec3d((double) blockPos.getX() + 0.5, (double) blockPos.getY() + 0.5, (double) blockPos.getZ() + 0.5), ShapeType.OUTLINE, FluidHandling.NONE, this.mc.player);
                                    BlockHitResult raycast = this.mc.world.raycast(raycastContext);
                                    boolean playerFacingFront = signBlockEntity.isPlayerFacingFront(this.mc.player);
                                    if (this.isSignUnedited(signBlockEntity.getText(playerFacingFront))) {
                                       this.mc.interactionManager.interactBlock(this.mc.player, Hand.MAIN_HAND, raycast);
                                       this.interacting = false;
                                       this.interactTime = Instant.now();
                                       return;
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onPacketReceived(PacketEvent.Receive event) {
      if (event.packet instanceof SignEditorOpenS2CPacket) {
         this.mc.player.networkHandler.sendPacket(this.getUpdateSignPacket(((SignEditorOpenS2CPacket)event.packet).getPos(), ((SignEditorOpenS2CPacket)event.packet).isFront()));
         this.interacting = false;
         event.cancel();
      }

   }

   private boolean shouldUpdateText(SignBlockEntity blockEntity) {
      return this.isSignUnedited(blockEntity.getText(true)) || this.isSignUnedited(blockEntity.getText(false));
   }

   private boolean isSignUnedited(SignText signText) {
      return !signText.getMessage(0, false).getString().equals(this.replaceText(this.signTextLine1.get())) || !signText.getMessage(1, false).getString().equals(this.replaceText(this.signTextLine2.get())) || !signText.getMessage(2, false).getString().equals(this.replaceText(this.signTextLine3.get())) || !signText.getMessage(3, false).getString().equals(this.replaceText(this.signTextLine4.get()));
   }

   private String getTicketNumber() {
      ServerInfo entry = this.mc.getCurrentServerEntry();
      if (entry != null && entry.address != null) {
         String ip = entry.address.split(":")[0];

         Inet4Address address;
         try {
            address = (Inet4Address)Inet4Address.getByName(ip);
         } catch (UnknownHostException var5) {
            return StringUtils.abbreviate(this.mc.player.getEntityName(), 15);
         }

         return String.valueOf(InetAddresses.coerceToInteger(address));
      } else {
         return this.textPreset.get() == TextPreset.Astral ? "Astral on top!" : "5C ON TOP";
      }
   }

   public enum TextPreset {
      FifthColumn,
      Astral,
      Custom;

      // $FF: synthetic method
      private static TextPreset[] $values() {
         return new TextPreset[]{FifthColumn, Astral, Custom};
      }
   }
}
