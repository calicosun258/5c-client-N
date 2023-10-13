package fifthcolumn.n.collar;

import com.collarmc.api.authentication.AuthenticationService.LoginRequest;
import com.collarmc.rest.RESTClient;
import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CollarLogin {
   private static final UUID COPE_GROUP_ID = UUID.fromString("fe2b0ae3-8984-414b-8a5f-e972736bb77c");
   private static final Logger LOGGER = LoggerFactory.getLogger(CollarLogin.class);
   private static final Gson GSON = new Gson();
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public static String getMembershipToken() {
      try {
         return CollarSettings.read().membershipToken;
      } catch (IOException var1) {
         LOGGER.error("Unable to read Collar group membership token", var1);
         throw new IllegalStateException(var1);
      }
   }

   public static boolean refreshSession() {
      RESTClient client = createClient();

      CollarSettings settings;
      try {
         settings = CollarSettings.read();
      } catch (Throwable var3) {
         LOGGER.error("Unable to read Collar settings", var3);
         return false;
      }

      LoginResult loginResult = loginAndSave(settings.email, settings.password);
      if (loginResult.success) {
         return client.validateGroupMembershipToken(settings.membershipToken, COPE_GROUP_ID).isPresent();
      } else {
         LOGGER.error("Collar group membership validation unsuccessful");
         return false;
      }
   }

   public static LoginResult loginAndSave(String email, String password) {
      RESTClient client = createClient();
      return client.login(LoginRequest.emailAndPassword(email, password)).map((loginResponse) -> {
         return loginResponse.token;
      }).map((token) -> {
         return client.createGroupMembershipToken(token, COPE_GROUP_ID).map((resp) -> {
            CollarSettings settings = new CollarSettings();
            settings.email = email;
            settings.password = password;
            settings.membershipToken = resp.token;

            try {
               settings.save();
            } catch (IOException var5) {
               LOGGER.error("Could not save collar settings");
               return new LoginResult(false, var5.getMessage());
            }

            return new LoginResult(true, (String)null);
         }).orElse(new LoginResult(false, "Login failed"));
      }).orElse(new LoginResult(false, "Login failed"));
   }

   private static RESTClient createClient() {
      return new RESTClient("https://api.collarmc.com");
   }

   public static final class CollarSettings {
      public String email;
      public String password;
      public String membershipToken;

      public void save() throws IOException {
         File file = new File(CollarLogin.mc.runDirectory, "collar.json");
         String contents = CollarLogin.GSON.toJson(this);
         Files.writeString(file.toPath(), contents);
      }

      public static CollarSettings read() throws IOException {
         File file = new File(CollarLogin.mc.runDirectory, "collar.json");
         String contents = Files.readString(file.toPath());
         return CollarLogin.GSON.fromJson(contents, CollarSettings.class);
      }
   }

   public static final class LoginResult {
      public final boolean success;
      public final String reason;

      public LoginResult(boolean success, String reason) {
         this.success = success;
         this.reason = reason;
      }
   }
}
