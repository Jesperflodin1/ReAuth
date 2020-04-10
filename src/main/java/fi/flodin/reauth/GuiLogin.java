package fi.flodin.reauth;

import java.awt.Color;
import java.io.IOException;

import org.lwjgl.input.Keyboard;

import com.mojang.authlib.exceptions.AuthenticationException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.client.config.GuiCheckBox;

final class GuiLogin extends GuiScreen {

	private GuiTextField username;
	private GuiPasswordField pw;
	private GuiButton login;
	private GuiButton cancel;
	private GuiButton offline;
	private GuiCheckBox save;
	private GuiButton config;

	private final GuiScreen prev;

	private int basey;

	private String message = "";

	GuiLogin(final GuiScreen prev) {
		mc = Minecraft.getMinecraft();
		fontRenderer = mc.fontRenderer;
		this.prev = prev;
	}

	@Override
	protected void actionPerformed(final GuiButton b) {
		switch (b.id) {
		case 0:
			if (login()) {
				mc.displayGuiScreen(prev);
			}
			break;
		case 3:
			if (playOffline()) {
				mc.displayGuiScreen(prev);
			}
			break;
		case 1:
			mc.displayGuiScreen(prev);
			break;
		case 4:
			mc.displayGuiScreen(new ConfigGUI(this));
			break;
		}

	}

	@Override
	public void drawScreen(final int p_73863_1_, final int p_73863_2_, final float p_73863_3_) {
		drawDefaultBackground();

		drawCenteredString(fontRenderer, "Username/E-Mail:", width / 2, basey, Color.WHITE.getRGB());
		drawCenteredString(fontRenderer, "Password:", width / 2, basey + 45, Color.WHITE.getRGB());
		if (((message != null) && !message.isEmpty())) {
			drawCenteredString(fontRenderer, message, width / 2, basey - 15, 0xFFFFFF);
		}
		username.drawTextBox();
		pw.drawTextBox();

		super.drawScreen(p_73863_1_, p_73863_2_, p_73863_3_);
	}

	@Override
	public void initGui() {
		super.initGui();
		Keyboard.enableRepeatEvents(true);

		basey = height / 2 - 110 / 2;

		username = new GuiTextField(0, fontRenderer, width / 2 - 155, basey + 15, 2 * 155, 20);
		username.setMaxStringLength(512);
		username.setText(Secure.username);
		username.setFocused(true);

		pw = new GuiPasswordField(fontRenderer, width / 2 - 155, basey + 60, 2 * 155, 20);
		pw.setPassword(Secure.password);

		save = new GuiCheckBox(2, width / 2 - 155, basey + 85, "Save Password to Config (WARNING: SECURITY RISK!)",
				false);
		buttonList.add(save);

		if (!Main.OfflineModeEnabled) {
			login = new GuiButton(0, width / 2 - 155, basey + 105, 153, 20, "Login");
			cancel = new GuiButton(1, width / 2 + 2, basey + 105, 155, 20, "Cancel");
			buttonList.add(login);
			buttonList.add(cancel);
		} else {
			login = new GuiButton(0, width / 2 - 155, basey + 105, 100, 20, "Login");
			offline = new GuiButton(3, width / 2 - 50, basey + 105, 100, 20, "Play Offline");
			cancel = new GuiButton(1, width / 2 + 55, basey + 105, 100, 20, "Cancel");
			buttonList.add(login);
			buttonList.add(cancel);
			buttonList.add(offline);
		}

		config = new GuiButton(4, width - 80, height - 25, 75, 20, "Config");
		buttonList.add(config);

		if (!VersionChecker.isLatestVersion()) {
			message = VersionChecker.getUpdateMessage();
		}
		if (!VersionChecker.isVersionAllowed()) {
			message = VersionChecker.getUpdateMessage();
			login.enabled = false;
		}
	}

	@Override
	protected void keyTyped(final char c, final int k) throws IOException {
		super.keyTyped(c, k);
		username.textboxKeyTyped(c, k);
		pw.textboxKeyTyped(c, k);
		if (k == Keyboard.KEY_TAB) {
			username.setFocused(!username.isFocused());
			pw.setFocused(!pw.isFocused());
		} else if (k == Keyboard.KEY_RETURN) {
			if (username.isFocused()) {
				username.setFocused(false);
				pw.setFocused(true);
			} else if (pw.isFocused()) {
				actionPerformed(login);
			}
		}
	}

	/**
	 * used as an interface between this and the secure class
	 * <p>
	 * returns whether the login was successful
	 */
	private boolean login() {
		try {
			Secure.login(username.getText(), pw.getPW(), save.isChecked());
			message = (char) 167 + "aLogin successful!";
			return true;
		} catch (final AuthenticationException e) {
			message = (char) 167 + "4Login failed: " + e.getMessage();
			Main.log.error("Login failed:", e);
			return false;
		} catch (final Exception e) {
			message = (char) 167 + "4Error: Something went wrong!";
			Main.log.error("Error:", e);
			return false;
		}
	}

	@Override
	protected void mouseClicked(final int x, final int y, final int b) throws IOException {
		super.mouseClicked(x, y, b);
		username.mouseClicked(x, y, b);
		pw.mouseClicked(x, y, b);
	}

	@Override
	public void onGuiClosed() {
		super.onGuiClosed();
		pw.setPassword(new char[0]);
		Keyboard.enableRepeatEvents(false);
	}

	/**
	 * sets the name for playing offline
	 */
	private boolean playOffline() {
		final String username = this.username.getText();
		if (((username.length() < 2) || (username.length() > 16))) {
			message = (char) 167 + "4Error: Username needs a length between 2 and 16";
			return false;
		}
		if (!username.matches("[A-Za-z0-9_]{2,16}")) {
			message = (char) 167 + "4Error: Username has to be alphanumerical";
			return false;
		}
		try {
			Secure.offlineMode(username);
			return true;
		} catch (final Exception e) {
			message = (char) 167 + "4Error: Something went wrong!";
			Main.log.error("Error:", e);
			return false;
		}
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		username.drawTextBox();
		pw.drawTextBox();
	}
}
