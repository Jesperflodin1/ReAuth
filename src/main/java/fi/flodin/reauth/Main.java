package fi.flodin.reauth;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.event.FMLFingerprintViolationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = Main.MOD_ID, name = Main.NAME, version = Main.VERSION, guiFactory = Main.CONFIG_GUI_FACTORY, canBeDeactivated = true, clientSideOnly = true, acceptedMinecraftVersions = Main.ACCEPTED_VERSIONS, certificateFingerprint = Main.CERTIFICATE_FINGERPRINT)
public final class Main {

	public static final String MOD_ID = "@ModId@";
	public static final String NAME = "@ModName@";
	public static final String VERSION = "@packageVersion@";
	public static final String CONFIG_GUI_FACTORY = "@GroupId@.@ModId@.GuiFactory";
	public static final String ACCEPTED_VERSIONS = "@acceptedVersions@";
	public static final String CERTIFICATE_FINGERPRINT = "@certSHA1@";

	static final Logger log = LogManager.getLogger(Main.MOD_ID);
	static Configuration config;

	static boolean OfflineModeEnabled;

	@Mod.Instance(Main.MOD_ID)
	static Main main;

	@Mod.Metadata
	static ModMetadata meta;

	/**
	 * (re-)loads config
	 */
	private static void loadConfig() {
		final Property username = config.get(Configuration.CATEGORY_GENERAL, "username", "", "Your Username");
		Secure.username = username.getString();

		final Property password = config.get(Configuration.CATEGORY_GENERAL, "password", "",
				"Your Password in plaintext if chosen to save to disk");
		Secure.password = password.getString().toCharArray();

		final Property offline = config.get(Configuration.CATEGORY_GENERAL, "offlineModeEnabled", false,
				"Enables play-offline button");
		Main.OfflineModeEnabled = offline.getBoolean();

		final Property validator = config.get(Configuration.CATEGORY_GENERAL, "validatorEnabled", true,
				"Disables the Session Validator");
		GuiHandler.enabled = validator.getBoolean();

		final Property bold = config.get(Configuration.CATEGORY_GENERAL, "validatorBold", true,
				"If the Session-Validator look weird disable this");
		GuiHandler.bold = bold.getBoolean();

		Main.config.save();
	}

	@SubscribeEvent
	public void onConfigChanged(final OnConfigChangedEvent evt) {
		if (evt.getModID().equals("reauth")) {
			Main.loadConfig();
		}
	}

	@Mod.EventHandler
	public void preInit(final FMLPreInitializationEvent evt) {
		MinecraftForge.EVENT_BUS.register(this);

		// Moved ReAuth config out of /config
		final File config = new File(Minecraft.getMinecraft().mcDataDir, ".ReAuth.cfg");
		// new one missing; old one there -> move the file
		if (evt.getSuggestedConfigurationFile().exists() && !config.exists()) {
			evt.getSuggestedConfigurationFile().renameTo(config);
		}
		// initialize config
		Main.config = new Configuration(config);
		Main.loadConfig();

		Secure.init();
	}

	@Mod.EventHandler
	public void securityError(final FMLFingerprintViolationEvent event) {
		// ignore the development environment
		if (event.isDirectory()) {
			Main.log.info("Dev environment, skipping signature check.");
			return;
		}

		log.fatal("+-------------------------------------------------------+");
		log.fatal("| The Version of ReAuth is not signed! It was modified! |");
		log.fatal("+-------------------------------------------------------+");
		System.out.println(String.format("Mod %s failed fingerprint check!", event.getSource().getAbsolutePath()));
		System.out.println(String.format("\tExpected fingerprint: %s", event.getExpectedFingerprint()));
		System.out.println(String.format("\tObserved %d fingerprints:", event.getFingerprints().size()));
		for (final String fingerprint : event.getFingerprints()) {
			System.out.println("\t\t" + fingerprint);
		}
		System.out.println("\n\n");
		throw new SecurityException("The Version of ReAuth is not signed! It is a modified version!");
	}

}
