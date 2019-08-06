package view;

import lib.AbsEventEmitter;

/**
 * An abstract class that represents the required interface (and basic functionality) from an email UI.
 * @author Janty Azmat
 */
public abstract class AbsEmailDisplay extends AbsEventEmitter<AbsEmailDisplay.EmailDisplayEvents> {

	public static enum EmailDisplayEvents {
		EMAIL_LIST_REQ,
		EMAIL_DATA_REQ
	}

	// Fields

	public AbsEmailDisplay() {
		super(EmailDisplayEvents.class);
	}

	public abstract void displayEamilList();

	public abstract void displayEmailData();
}
