package controller;

import javafx.beans.property.SimpleBooleanProperty;
import view.IEmailDisplay;

/**
 * @author Janty Azmat
 */
public class Hive {
	// Fields
	private static final String SECURE_SAVE_DIR = "./secu_config/";
	private static final int SECURE_ITER_COUNT = 40000;
	private IEmailDisplay meView;

	public Hive(IEmailDisplay theView) {
		this.meView = theView;
	}

	public void start() {
		this.meView.startDisplay();
		Boolean tmpBool = Boolean.FALSE;
		SimpleBooleanProperty tmpProp = new SimpleBooleanProperty(tmpBool);
		tmpProp.addListener((obs, oldVal, newVal) -> this.meView.displayInformation("Bool changed"));
		this.meView.displayInformation("Before trigger");
		tmpBool = Boolean.TRUE;
		this.meView.displayInformation("Second trigger");
		tmpProp.set(true);
//		this.meView.requestData(dataTitle, dataMsg, requestedData)
	}
}
