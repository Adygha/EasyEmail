package loader;

import view.IEmailDisplay.MsgType;
import view.JavaFxEmailDisplay;

/**
 * Just a loader class.
 * @author Janty Azmat
 */
public class Loader {

	/**
	 * Main entry.
	 * @param args
	 */
	public static void main(String[] args) {
		JavaFxEmailDisplay tmpView = new JavaFxEmailDisplay();
		tmpView.displayMesssage("Hello", MsgType.INFORMATION);
	}
}
