package controller;

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
//		model.SystemCredentialsManager tmpMan = new model.SystemCredentialsManager("MyApp/MyCred");

//		int tmpErrCode = model.SystemCredentialsManager.deleteCredential("InTheNameOfALLAH", model.SystemCredentialsManager.WindowsCredential.CredentialType.CRED_TYPE_DOMAIN_PASSWORD);


//		model.SystemCredentialsManager.WindowsCredential[] tmpCreds = model.SystemCredentialsManager.getWindowsCredentials("*ALLAH");
//		for (var cred : tmpCreds) {
//			this.meView.displayInformation(cred.toString());
//		}

//		model.SystemCredentialsManager.WindowsCredential[] tmpCreds = model.SystemCredentialsManager.getWindowsCredentialsAll();
//		for (var cred : tmpCreds) {
//			this.meView.displayInformation(cred.toString());
//		}

		this.meView.displayInformation("" + model.SystemCredentialsManager.getWindowsCredential("InTheNameOfALLAH", model.SystemCredentialsManager.WindowsCredential.CredentialType.CRED_TYPE_GENERIC));

//		for (var attr : model.SystemCredentialsManager.getWindowsCredential("MicrosoftAccount:target=SSO_POP_Device", model.SystemCredentialsManager.WindowsCredential.CredentialType.CRED_TYPE_GENERIC).getByteAttributes()) {
//			this.meView.displayInformation(attr.getKey() + ":\n" + java.util.Arrays.toString(attr.getValue()));
//		}

//		for (var attr : model.SystemCredentialsManager.getWindowsCredential("WindowsLive:target=virtualapp/didlogical", model.SystemCredentialsManager.WindowsCredential.CredentialType.CRED_TYPE_GENERIC).getByteAttributes()) {
//			this.meView.displayInformation(attr.getKey() + ":\n" + java.util.Arrays.toString(attr.getValue()));
//		}


//		var tmpCred = new model.SystemCredentialsManager.WindowsCredential(
//			model.SystemCredentialsManager.WindowsCredential.CredentialFlag.CRED_FLAGS_NONE,
//			model.SystemCredentialsManager.WindowsCredential.CredentialType.CRED_TYPE_DOMAIN_PASSWORD,
//			"InTheNameOfALLAH",
//			"MyComment",
//			"ABCDEF",
//			model.SystemCredentialsManager.WindowsCredential.CredentialPersist.CRED_PERSIST_LOCAL_MACHINE,
//			"SomeAlias",
//			"LoginUser"
//		);
//		int tmpErrCode = model.SystemCredentialsManager.newCredential(tmpCred);


//		if (tmpErrCode == 0) {
//			this.meView.displayInformation("Success..");
//		} else {
//			this.meView.displayWarning(model.SystemCredentialsManager.getErrorMessage(tmpErrCode));
//		}
	}
}
