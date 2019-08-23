package controller;

import model.SystemCredentialsManager;
import model.SystemCredentialsManager.WindowsCredential;
//import model.SystemCredentialsManager.WindowsCredential.CredentialFlag;
//import model.SystemCredentialsManager.WindowsCredential.CredentialPersist;
//import model.SystemCredentialsManager.WindowsCredential.CredentialType;
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
		SystemCredentialsManager tmpMan = new SystemCredentialsManager("MyApp/");

		WindowsCredential[] tmpCreds = tmpMan.getWindowsCredentials("*ALLAH");
		for (var cred : tmpCreds) {
			this.meView.displayInformation(cred.toString());
		}

//		WindowsCredential[] tmpCreds = tmpMan.getWindowsCredentialsAll();
//		for (var cred : tmpCreds) {
//			this.meView.displayInformation(cred.toString());
//		}

//		this.meView.displayInformation(
//				"Error Code: " +
//				tmpMan.newCredential(new WindowsCredential(
//							CredentialFlag.CRED_FLAGS_NONE,
//							CredentialType.CRED_TYPE_GENERIC,
//							"InTheNameOfALLAH",
//							"MyComment",
//							"ABCDEF",
//							CredentialPersist.CRED_PERSIST_LOCAL_MACHINE,
//							"SomeAlias",
//							"LoginUser"
//						)
//				)
//		);
	}
}
