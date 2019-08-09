package view;

import java.util.function.BiConsumer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lib.EventEmitter;
import lib.IEventEmitter;
import view.IEmailDisplay.EmailDisplayEvent;

/**
 * @author Janty Azmat
 */
public class JavaFxEmailDisplay extends EventEmitter<EmailDisplayEvent> implements IEmailDisplay {

	// Fields
//	private EventEmitter<EmailDisplayEvent> meEmit;

	public JavaFxEmailDisplay() {
		super(EmailDisplayEvent.class);
	}

//	@Override
//	public boolean addEventListener(EmailDisplayEvent theEvent, BiConsumer<IEventEmitter<EmailDisplayEvent>, Object> eventListener) {
//		return this.meEmit.addEventListener(theEvent, eventListener);
//	}
//
//	@Override
//	public boolean removeEventListener(EmailDisplayEvent theEvent, BiConsumer<IEventEmitter<EmailDisplayEvent>, Object> eventListener) {
//		return this.meEmit.removeEventListener(theEvent, eventListener);
//	}
//
//	@Override
//	public void emitEvent(EmailDisplayEvent theEvent, Object eventData) {
//		this.meEmit.emitEvent(theEvent, eventData);
//	}
//
//	@Override
//	public void emitEvent(EmailDisplayEvent theEvent) {
//		this.meEmit.emitEvent(theEvent);
//	}

	@Override
	public void askForCredentials() {
		// TODO Auto-generated method stub

	}

	@Override
	public void displayEamilList() {
		// TODO Auto-generated method stub

	}

	@Override
	public void displayEmailData() {
		// TODO Auto-generated method stub

	}

	@Override
	public void displayMesssage(String theMsg, MsgType msgType) {
		// TODO Auto-generated method stub

	}

	@Override
	public void exit() {
		// TODO Auto-generated method stub

	}
}
