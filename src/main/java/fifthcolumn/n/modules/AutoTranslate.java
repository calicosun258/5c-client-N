package fifthcolumn.n.modules;

import fifthcolumn.n.events.ReceiveMsgEvent;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.BetterChat;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AutoTranslate extends Module {
   private final SettingGroup sgGeneral;
   public static final List<Module> CONFLICTING_MODULES = List.of(getModule(BetterChat.class));
   public final Setting<Boolean> translateOut;
   public final Setting<Boolean> translateIn;
   public final Setting<Lang> serverLang;
   public final Setting<Lang> localLang;

   public AutoTranslate() {
      super(NAddOn.FIFTH_COLUMN_CATEGORY, "AutoTranslate", "Google translate for block game");
      this.sgGeneral = this.settings.getDefaultGroup();
      this.translateOut = this.sgGeneral.add(new BoolSetting.Builder()
              .name("Translate outgoing")
              .description("messages you send")
              .defaultValue(false)
              .build());
      this.translateIn = this.sgGeneral.add(new BoolSetting.Builder()
              .name("Translate incoming")
              .description("messages you receive")
              .defaultValue(true)
              .build());
      this.serverLang = this.sgGeneral.add(new EnumSetting.Builder<Lang>()
              .name("Server Language")
              .description("Language the server speaks")
              .defaultValue(Lang.RUSSIAN).build());
      this.localLang = this.sgGeneral.add(new EnumSetting.Builder<Lang>()
              .name("Local Language")
              .description("Language you speak")
              .defaultValue(Lang.ENGLISH).build());
   }

   @EventHandler
   private void onMessageSend(SendMessageEvent event) {
      if (this.translateOut.get()) {
         String translated = this.translate(event.message, this.localLang.get().value, this.serverLang.get().value);
         if (!translated.isEmpty()) {
            event.message = translated;
         }

         reengageModules(disengageConflictingModules());
      }

   }

   @EventHandler
   private void onMessageReceive(ReceiveMsgEvent event) {
      if (this.translateIn.get()) {
         String message = event.getMessage().getString();
         Thread translate = new Thread(() -> {
            String translated = this.translate(message, this.serverLang.get().value, this.localLang.get().value);
            MinecraftClient.getInstance().execute(() -> {
               if (!translated.isEmpty()) {
                  this.mc.inGameHud.getChatHud().addMessage(Text.of("[TRANSLATED] <" + event.getSender() + "> " + translated));
               }

               reengageModules(disengageConflictingModules());
            });
         });
         translate.start();
      }

   }

   public void onActivate() {
      super.onActivate();
   }

   public void onDeactivate() {
      super.onDeactivate();
   }

   public String translate(String text, String from, String to) {
      String translated = this.parseHTML(this.getHTML(text, from, to));
      return text.equalsIgnoreCase(translated) ? "" : translated;
   }

   private String getHTML(String text, String langFrom, String langTo) {
      URL url = this.createURL(text, langFrom, langTo);

      try {
         URLConnection con = this.setupConnection(url);
         InputStreamReader streamReader = new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8);
         BufferedReader br = new BufferedReader(streamReader);

         String var10;
         try {
            StringBuilder html = new StringBuilder();

            while(true) {
               String line;
               if ((line = br.readLine()) == null) {
                  var10 = html.toString();
                  break;
               }

               html.append(line).append("\n");
            }
         } catch (Throwable var12) {
            try {
               br.close();
            } catch (Throwable var11) {
               var12.addSuppressed(var11);
            }

            throw var12;
         }

         br.close();
         return var10;
      } catch (IOException var13) {
         return null;
      }
   }

   private String parseHTML(String html) {
      String regex = "class=\"result-container\">([^<]*)<\\/div>";
      Pattern pattern = Pattern.compile(regex, 8);
      Matcher matcher = pattern.matcher(html);
      matcher.find();
      String match = matcher.group(1);
      return match != null && !match.isEmpty() ? StringEscapeUtils.unescapeHtml4(match) : null;
   }

   private URL createURL(String text, String langFrom, String langTo) {
      try {
         String encodedText = URLEncoder.encode(text.trim(), StandardCharsets.UTF_8);
         String urlString = String.format("https://translate.google.com/m?hl=en&sl=%s&tl=%s&ie=UTF-8&prev=_m&q=%s", langFrom, langTo, encodedText);
         return new URL(urlString);
      } catch (MalformedURLException var6) {
         throw new RuntimeException(var6);
      }
   }

   private URLConnection setupConnection(URL url) throws IOException {
      URLConnection connection = url.openConnection();
      connection.setConnectTimeout(5000);
      connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
      return connection;
   }

   private static List<Module> disengageConflictingModules() {
      List<Module> modules = CONFLICTING_MODULES.stream().filter(Module::isActive).collect(Collectors.toList());
      modules.forEach(Module::toggle);
      return modules;
   }

   private static void reengageModules(List<Module> modules) {
      modules.forEach(Module::toggle);
   }

   private static Module getModule(Class module) {
      return Modules.get().get(module);
   }

   public enum Lang {
      AFRIKAANS("Afrikaans", "af"),
      ARABIC("Arabic", "ar"),
      CZECH("Czech", "cs"),
      CHINESE_SIMPLIFIED("Chinese (simplified)", "zh-CN"),
      CHINESE_TRADITIONAL("Chinese (traditional)", "zh-TW"),
      DANISH("Danish", "da"),
      DUTCH("Dutch", "nl"),
      ENGLISH("English", "en"),
      FINNISH("Finnish", "fi"),
      FRENCH("French", "fr"),
      GERMAN("German", "de"),
      GREEK("Greek", "el"),
      HINDI("Hindi", "hi"),
      ITALIAN("Italian", "it"),
      JAPANESE("Japanese", "ja"),
      KOREAN("Korean", "ko"),
      NORWEGIAN("Norwegian", "no"),
      POLISH("Polish", "pl"),
      PORTUGUESE("Portuguese", "pt"),
      RUSSIAN("Russian", "ru"),
      SPANISH("Spanish", "es"),
      SWAHILI("Swahili", "sw"),
      SWEDISH("Swedish", "sv"),
      TURKISH("Turkish", "tr");

      private final String name;
      public final String value;

      Lang(String name, String value) {
         this.name = name;
         this.value = value;
      }

      public String toString() {
         return this.name;
      }
   }
}
