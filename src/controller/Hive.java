package controller;

import model.SystemCredentialsManager;
import model.SystemCredentialsManager.WindowsCredential;
import view.IEmailDisplay;

/**
 * A class that represents the central application controller.
 * @author Janty Azmat
 */
public class Hive {
	// Fields
//	private static final String SECURE_SAVE_DIR = "secu_config/";
//	private static final String DBASE_FILE = "EasyEmail.db";
//	private static final int SECURE_ITER_COUNT = 40000;
	private IEmailDisplay meView;

	public Hive(IEmailDisplay theView) {
		this.meView = theView;
	}

	public void start() {
		this.meView.startDisplay();
		SystemCredentialsManager tmpCred = new SystemCredentialsManager("MyApp/");

		WindowsCredential[] tmpCreds = tmpCred.getWindowsCredentials("azm*");
		for (var cred : tmpCreds) {
			this.meView.displayInformation(cred.toString());
		}
	}
}
