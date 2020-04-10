package fi.flodin.reauth;

import java.util.Arrays;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class GuiPasswordField extends GuiTextField {

	private char[] password = new char[0];

	GuiPasswordField(final FontRenderer renderer, final int posx, final int posy, final int x, final int y) {
		super(1, renderer, posx, posy, x, y);
		setMaxStringLength(512);
	}

	@Override
	public void deleteFromCursor(final int num) {
		if (password.length == 0)
			return;
		if (getSelectionEnd() != getCursorPosition()) {
			writeText("");
		} else {
			final boolean direction = num < 0;
			final int start = direction ? Math.max(getCursorPosition() + num, 0) : getCursorPosition();
			final int end = direction ? getCursorPosition() : Math.min(getCursorPosition() + num, password.length);

			final char[] newPW = new char[start + password.length - end];

			if (start >= 0) {
				System.arraycopy(password, 0, newPW, 0, start);
			}

			if (end < password.length) {
				System.arraycopy(password, end, newPW, start, password.length - end);
			}

			setPassword(newPW);
			Arrays.fill(newPW, 'f');
			if (direction) {
				moveCursorBy(num);
			}
		}
	}

	/**
	 * Modified version of
	 * {@link ChatAllowedCharacters#filterAllowedCharacters(String)}
	 */
	private String filterAllowedCharacters(final String input) {
		final StringBuilder stringbuilder = new StringBuilder();
		input.chars().filter(this::isAllowedCharacter).forEach(i -> stringbuilder.append((char) i));
		return stringbuilder.toString();
	}

	char[] getPW() {
		final char[] pw = new char[password.length];
		System.arraycopy(password, 0, pw, 0, password.length);
		return pw;
	}

	/**
	 * Allow SectionSign to be input into the field
	 */
	private boolean isAllowedCharacter(final int character) {
		return character == 0xa7 || ChatAllowedCharacters.isAllowedCharacter((char) character);
	}

	void setPassword(final char[] password) {
		Arrays.fill(this.password, 'f');
		this.password = new char[password.length];
		System.arraycopy(password, 0, this.password, 0, password.length);
		updateText();
	}

	@Override
	public void setText(final String textIn) {
		setPassword(textIn.toCharArray());
		updateText();
	}

	@Override
	public boolean textboxKeyTyped(final char typedChar, final int keyCode) {
		if (!isFocused() || GuiScreen.isKeyComboCtrlC(keyCode) || GuiScreen.isKeyComboCtrlX(keyCode))
			return false; // Prevent Cut/Copy
		if (GuiScreen.isKeyComboCtrlA(keyCode) || GuiScreen.isKeyComboCtrlV(keyCode))
			return super.textboxKeyTyped(typedChar, keyCode); // combos handled by super

		switch (keyCode) {
		case Keyboard.KEY_BACK: // backspace
		case Keyboard.KEY_DELETE:
		case Keyboard.KEY_HOME: // jump keys?
		case Keyboard.KEY_END:
		case Keyboard.KEY_LEFT: // arrowkey
		case Keyboard.KEY_RIGHT:
			return super.textboxKeyTyped(typedChar, keyCode); // special keys handled by super
		default:
			if (isAllowedCharacter(typedChar)) {
				writeText(Character.toString(typedChar));
				return true;
			}
			return false;
		}
	}

	private void updateText() {
		final char[] chars = new char[password.length];
		Arrays.fill(chars, '\u25CF');
		super.setText(new String(chars));
	}

	@Override
	public void writeText(final String rawInput) {
		final int selStart = getCursorPosition() < getSelectionEnd() ? getCursorPosition() : getSelectionEnd();
		final int selEnd = getCursorPosition() < getSelectionEnd() ? getSelectionEnd() : getCursorPosition();

		final char[] input = filterAllowedCharacters(rawInput).toCharArray();
		final char[] newPW = new char[selStart + password.length - selEnd + input.length];

		if (password.length != 0 && selStart > 0) {
			System.arraycopy(password, 0, newPW, 0, Math.min(selStart, password.length));
		}

		System.arraycopy(input, 0, newPW, selStart, input.length);
		final int l = input.length;

		if (password.length != 0 && selEnd < password.length) {
			System.arraycopy(password, selEnd, newPW, selStart + input.length, password.length - selEnd);
		}

		setPassword(newPW);
		Arrays.fill(newPW, 'f');
		moveCursorBy(selStart - getSelectionEnd() + l);
	}
}
