package yalter.mousetweaks.handlers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.crash.CrashReport;
import net.minecraft.inventory.*;
import net.minecraft.util.ReportedException;
import org.lwjgl.input.Mouse;
import yalter.mousetweaks.*;
import yalter.mousetweaks.api.MouseTweaksDisableWheelTweak;
import yalter.mousetweaks.api.MouseTweaksIgnore;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class GuiContainerHandler implements IGuiScreenHandler {
	protected Minecraft mc;
	protected GuiContainer guiContainer;
	protected Method handleMouseClick;

	public GuiContainerHandler(GuiContainer guiContainer) {
		this.mc = Minecraft.getMinecraft();
		this.guiContainer = guiContainer;
		this.handleMouseClick = Reflection.getHMCMethod(guiContainer);
	}

	private int getDisplayWidth() {
		return mc.displayWidth;
	}

	private int getDisplayHeight() {
		return mc.displayHeight;
	}

	private int getRequiredMouseX() {
		return (Mouse.getX() * guiContainer.width) / getDisplayWidth();
	}

	private int getRequiredMouseY() {
		return guiContainer.height - ((Mouse.getY() * guiContainer.height) / getDisplayHeight()) - 1;
	}

	@Override
	public boolean isMouseTweaksDisabled() {
		return guiContainer.getClass().isAnnotationPresent(MouseTweaksIgnore.class)
			|| (Reflection.guiContainerClass == null);
	}

	@Override
	public boolean isWheelTweakDisabled() {
		return guiContainer.getClass().isAnnotationPresent(MouseTweaksDisableWheelTweak.class);
	}

	@Override
	public List<Slot> getSlots() {
		return guiContainer.inventorySlots.inventorySlots;
	}

	@Override
	public Slot getSlotUnderMouse() {
		try {
			return (Slot)Reflection.guiContainerClass.invokeMethod(guiContainer, Constants.GETSLOTATPOSITION_NAME.forgeName, getRequiredMouseX(), getRequiredMouseY());
		} catch (InvocationTargetException e) {
			CrashReport crashreport = CrashReport.makeCrashReport(e, "GuiContainer.getSlotAtPosition() threw an exception when called from MouseTweaks.");
			throw new ReportedException(crashreport);
		}
	}

	@Override
	public boolean disableRMBDraggingFunctionality() {
		Reflection.guiContainerClass.setFieldValue(guiContainer, Constants.IGNOREMOUSEUP_NAME.forgeName, true);

		if ((Boolean)Reflection.guiContainerClass.getFieldValue(guiContainer, Constants.DRAGSPLITTING_NAME.forgeName)) {
			if ((Integer)Reflection.guiContainerClass.getFieldValue(guiContainer, Constants.DRAGSPLITTINGBUTTON_NAME.forgeName) == 1) {
				Reflection.guiContainerClass.setFieldValue(guiContainer, Constants.DRAGSPLITTING_NAME.forgeName, false);
				return true;
			}
		}

		return false;
	}

	@Override
	public void clickSlot(Slot slot, MouseButton mouseButton, boolean shiftPressed) {
		try {
			handleMouseClick.invoke(guiContainer,
			                        slot,
			                        slot.slotNumber,
			                        mouseButton.getValue(),
			                        shiftPressed ? ClickType.QUICK_MOVE : ClickType.PICKUP);
		} catch (InvocationTargetException e) {
			CrashReport crashreport = CrashReport.makeCrashReport(e, "handleMouseClick() threw an exception when called from MouseTweaks.");
			throw new ReportedException(crashreport);
		} catch (IllegalAccessException e) {
			CrashReport crashreport = CrashReport.makeCrashReport(e, "Calling handleMouseClick() from MouseTweaks.");
			throw new ReportedException(crashreport);
		}
	}

	@Override
	public boolean isCraftingOutput(Slot slot) {
		return (slot instanceof SlotCrafting
			|| slot instanceof SlotFurnaceOutput
			|| slot instanceof SlotMerchantResult
			|| (guiContainer.inventorySlots instanceof ContainerRepair && slot.slotNumber == 2));
	}

	@Override
	public boolean isIgnored(Slot slot) {
		return false;
	}
}
