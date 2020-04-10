package fi.flodin.reauth;

import java.awt.Color;
import java.lang.reflect.Field;

import org.apache.logging.log4j.core.appender.rolling.action.Duration;

import com.mojang.realmsclient.gui.ChatFormatting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent;
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = "reauth", value = Side.CLIENT)
public final class GuiHandler {

	static final class reflectionHelper {

		private static Field messageField = ReflectionHelper.findField(GuiDisconnected.class, "message", "f",
				"field_146304_f");

		static ITextComponent getDCMessage(final GuiDisconnected dcgui) {
			try {
				return (ITextComponent) reflectionHelper.messageField.get(dcgui);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return new TextComponentString("");
			}
		}
	}

	private enum ValidationStatus {
		Unknown("?", Color.GRAY.getRGB()), Valid("\u2714", Color.GREEN.getRGB()), Invalid("\u2718", Color.RED.getRGB());

		private final String text;
		private final int color;

		ValidationStatus(final String text, final int color) {
			this.text = text;
			this.color = color;
		}
	}

	/**
	 * Cache the Status for 3 Minutes
	 */
	private static final CachedProperty<ValidationStatus> status = new CachedProperty<>(1000 * 60 * 3,
			ValidationStatus.Unknown);
	private static Thread validator;
	static boolean enabled = true;

	static boolean bold = true;

	static boolean reauthRunning = false;

	@SubscribeEvent
	public static void action(final ActionPerformedEvent.Post e) {
		if ((e.getGui() instanceof GuiMainMenu || e.getGui() instanceof GuiMultiplayer) && e.getButton().id == 17325) {
			Minecraft.getMinecraft().displayGuiScreen(new GuiLogin(Minecraft.getMinecraft().currentScreen));
		}
	}

	@SubscribeEvent
	public static void action(final ActionPerformedEvent.Pre e) {
		if (enabled && e.getGui() instanceof GuiMultiplayer && e.getButton().id == 8 && GuiScreen.isShiftKeyDown()) {
			status.invalidate();
		}
	}

	@SubscribeEvent
	public static void draw(final DrawScreenEvent.Post e) {
		if (enabled && (e.getGui() instanceof GuiMultiplayer || e.getGui() instanceof GuiDisconnected)) {
			e.getGui().drawString(e.getGui().mc.fontRenderer, "Online:", 110, 10, 0xFFFFFFFF);
			final ValidationStatus state = status.get();
			final long timeleft = System.currentTimeMillis() - status.timestamp();
			final Duration d = null;

			e.getGui().drawString(e.getGui().mc.fontRenderer, (bold ? ChatFormatting.BOLD : "") + state.text, 145, 10,
					state.color);
		}
	}

	static void invalidateStatus() {
		status.invalidate();
	}

	@SubscribeEvent
	public static void open(final InitGuiEvent.Post e) {
		boolean run = false;
		if (e.getGui() instanceof GuiMultiplayer) {
			e.getButtonList().add(new GuiButton(17325, 5, 5, 100, 20, "Re-Login"));
			run = true;

			runValidator();
		} else if (e.getGui() instanceof GuiMainMenu) {
			run = true;
			// Support for Custom Main Menu (add button outside of viewport)
			e.getButtonList().add(new GuiButton(17325, -50, -50, 20, 20, "ReAuth"));
		} else if (enabled && e.getGui() instanceof GuiDisconnected) {
			final GuiScreen screen = e.getGui();
			final GuiDisconnected dcgui = (GuiDisconnected) screen;
			TextComponentTranslation dcreason = null;
			try {
				dcreason = (TextComponentTranslation) reflectionHelper.getDCMessage(dcgui);
				if (dcreason.getKey().contentEquals("disconnect.loginFailedInfo")) {
					Main.log.info("Disconnected. Let's find out why." + dcreason.getFormatArgs());
					for (final Object sibling : dcreason.getFormatArgs()) {
						if (((TextComponentTranslation) sibling).getKey()
								.contentEquals("disconnect.loginFailedInfo.invalidSession")) {
							Main.log.info("Session is invalid!");
							status.invalidate();
							Main.log.info("Running login from disconnect screen");
							Secure.login();
						}
					}
				}
			} catch (final Exception e1) {
				Main.log.info(
						"Disconnected. I don't really care why but i'll try to run a session validation just to be nice. error:"
								+ e1.getLocalizedMessage());
				runValidator();
			}

		}

		if (run && VersionChecker.shouldRun()) {
			VersionChecker.update();
		}
	}

	private static void runValidator() {
		if (enabled && !status.check()) {
			if (validator != null) {
				validator.interrupt();
			}
			validator = new Thread(
					() -> status.set(Secure.SessionValid(false) ? ValidationStatus.Valid : ValidationStatus.Invalid),
					"Session-Validator");
			validator.setDaemon(true);
			validator.start();
		}
	}

}
