package view;

import lib.ConstraintList;
import lib.IEventEmitter;

/**
 * The interface required (and basic functionality) from an email UI.
 * @author Janty Azmat
 */
public interface IEmailDisplay extends IEventEmitter<IEmailDisplay.EmailDisplayEvent> {

	static enum EmailDisplayEvent {
		DISPLAY_STARTED,
//		CREDENTIALS_PROVIDED,
		EMAIL_LIST_REQUESTED,
		EMAIL_DATA_REQUESTED,
		EXIT_REQUESTED
	}

	void startDisplay();

	ConstraintList requestData(String dataTitle, String dataMsg, ConstraintList requestedData);

	void displayEamilList();

	void displayEmailData();

	/**
	 * Used to display an information message to the user.
	 * @param infoMsg	the information message.
	 */
	void displayInformation(String infoMsg);

	/**
	 * Used to display a warning message to the user
	 * @param warningMsg	the warning message.
	 */
	void displayWarning(String warningMsg);

	/**
	 * Used to display an error message to the user.
	 * @param errorMsg	the error message.
	 */
	void displayError(String errorMsg);

	void exit();
}
