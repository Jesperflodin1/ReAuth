package technicianlp.reauth;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class Secure {

    /**
     * Username/email
     */
    static String username = "";
    /**
     * password if saved to config else empty
     */
    static char[] password = new char[0];

    /**
     * Mojang authentificationservice
     */
    private static final YggdrasilAuthenticationService yas;
    private static final YggdrasilUserAuthentication yua;
    private static final YggdrasilMinecraftSessionService ymss;
    
    static boolean autoReauth = true;

    /**
     * currently used to load the class
     */
    static void init() {
        String base = "technicianlp.reauth.";
        List<String> classes = ImmutableList.of(base + "ConfigGUI", base + "GuiFactory", base + "GuiHandler",
                base + "GuiLogin", base + "GuiPasswordField", base + "Main",
                base + "Secure", base + "VersionChecker", base + "CachedProperty");
        try {
            Set<ClassInfo> set = ClassPath.from(Secure.class.getClassLoader()).getTopLevelClassesRecursive("technicianlp.reauth");
            for (ClassInfo info : set)
                if (!classes.contains(info.getName())) {
                    throw new RuntimeException("Detected unexpected class in ReAuth package! Offender: " + "(" + info.getName() + ")" + info.url().getPath());
                }
        } catch (IOException e) {
            throw new RuntimeException("Classnames could not be fetched!", e);
        }

        VersionChecker.update();
    }

    static {
        /* initialize the authservices */
        yas = new YggdrasilAuthenticationService(Minecraft.getMinecraft().getProxy(), UUID.randomUUID().toString());
        yua = (YggdrasilUserAuthentication) yas.createUserAuthentication(Agent.MINECRAFT);
        ymss = (YggdrasilMinecraftSessionService) yas.createMinecraftSessionService();
    }

    /**
     * Logs you in; replaces the Session in your client; and saves to config
     */
    static void login(String user, char[] pw, boolean savePassToConfig) throws AuthenticationException, IllegalArgumentException, IllegalAccessException {
        if (!VersionChecker.isVersionAllowed())
            throw new AuthenticationException("ReAuth has a critical update!");

        /* set credentials */
        Secure.yua.setUsername(user);
        Secure.yua.setPassword(new String(pw));

        /* login */
        Secure.yua.logIn();

        Main.log.info("Login successful!");

        /* put together the new Session with the auth-data */
        String username = Secure.yua.getSelectedProfile().getName();
        String uuid = UUIDTypeAdapter.fromUUID(Secure.yua.getSelectedProfile().getId());
        String access = Secure.yua.getAuthenticatedToken();
        String type = Secure.yua.getUserType().getName();
        Sessionutil.set(new Session(username, uuid, access, type));

        /* logout to discard the credentials in the object */
        Secure.yua.logOut();

        /* save username to config */
        Secure.username = user;
        Main.config.get(Configuration.CATEGORY_GENERAL, "username", "", "Your Username").set(Secure.username);
        /* save password to config if desired */
        if (savePassToConfig) {
            Secure.password = pw;
            Main.config.get(Configuration.CATEGORY_GENERAL, "password", "",
                    "Your Password in plaintext if chosen to save to disk").set(new String(Secure.password));
        }
        Main.config.save();
    }
    /*
     * Logs in using saved credentials. Does nothing if password is not saved.
     */
    static void login() throws AuthenticationException, IllegalArgumentException, IllegalAccessException {
    	if (Secure.hasSavedCredentials()) {
    		Main.log.info("I have creds. Trying to login");
    		Secure.login(Secure.username, Secure.password, false);
    	}
    	Main.log.error("I dont have any credentials. I give up. No connection was made.");
    }
    
    static boolean hasSavedCredentials() {
    	return (Secure.username.length() > 0 && Secure.password.length > 0);
    }

    static void offlineMode(String username) throws IllegalArgumentException, IllegalAccessException {
        /* Create offline uuid */
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(Charsets.UTF_8));
        Sessionutil.set(new Session(username, uuid.toString(), "invalid", "legacy"));
        Main.log.info("Offline Username set!");
        Secure.username = username;
    }

    /**
     * checks online if the session is valid
     * @param forceDisableReauth TODO
     */
    static boolean SessionValid(boolean forceDisableReauth) {
        try {
            GameProfile gp = Sessionutil.get().getProfile();
            String token = Sessionutil.get().getToken();
            String id = UUID.randomUUID().toString();

            Secure.ymss.joinServer(gp, token, id);
            if (Secure.ymss.hasJoinedServer(gp, id, null).isComplete()) {
                Main.log.info("Session validation successful");
                return true;
            }
        } catch (Exception e) {
            Main.log.info("Session validation failed: " + e.getMessage());
        }
        Main.log.info("Session validation failed!");
        if (Secure.autoReauth && !forceDisableReauth) {
        	try {
				Secure.login();
				if (!Secure.SessionValid(true)) {
					Main.log.info("Reauth failed :(");
					return false;
				}
				Main.log.info("Reauth success!");
				return true;
			} catch (IllegalArgumentException | IllegalAccessException | AuthenticationException e) {
				Main.log.info("Reauth failed :(");
				return false;
			}
        }
        return false;
    }

    static final class Sessionutil {
        /**
         * as the Session field in Minecraft.class is final we have to access it
         * via reflection
         */
        private static Field sessionField = ReflectionHelper.findField(Minecraft.class, "session", "S", "field_71449_j");

        static Session get() {
            return Minecraft.getMinecraft().getSession();
        }

        static void set(Session s) throws IllegalArgumentException, IllegalAccessException {
            Sessionutil.sessionField.set(Minecraft.getMinecraft(), s);
            GuiHandler.invalidateStatus();
        }
    }

}
