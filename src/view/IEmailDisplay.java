/**
 *
 */
package view;

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

	static enum MsgType {
		INFORMATION,
		WARNING,
		ERROR
	}

	void askForCredentials();

	void displayEamilList();

	void displayEmailData();

	void displayMesssage(String theMsg, MsgType msgType);

	void exit();
}
