package fi.flodin.reauth;

import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;

public final class GuiFactory implements IModGuiFactory {

	@Override
	public GuiScreen createConfigGui(final GuiScreen parentScreen) {
		return new ConfigGUI(parentScreen);
	}

	@Override
	public boolean hasConfigGui() {
		return true;
	}

	@Override
	public void initialize(final Minecraft minecraftInstance) {
	}

	@Override
	public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
		return null;
	}

}
