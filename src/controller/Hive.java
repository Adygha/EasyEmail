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
		model.CredentialManager tmpCredMan = new model.CredentialManager("MyApp");

		try {
			var tmpCred = tmpCredMan.getCredential("TestCred");

//			model.CredentialManager.WindowsCredential tmpCred = new model.CredentialManager.WindowsCredential(true, "TestCred", "CredUserName", "CredPassWord", "Some comment.", "CredTargetAlias");
//			tmpCred.addAttribute("Attrib1", "Attrib1 content.").addAttribute("Attrib2", "Attrib2 content.").addAttribute("Attrib3", "Attrib3 content.");
//			tmpCredMan.saveCredential(tmpCred);

			this.meView.displayInformation(tmpCred.toString());
			this.meView.displayInformation(tmpCred.toString(true, false));
			this.meView.displayInformation(tmpCred.toString(true, true));

//			this.meView.displayInformation(java.util.Arrays.toString(tmpCredMan.getCredentials()));
//			this.meView.displayInformation("" + tmpCredMan.getCredential("TestCred"));

//			tmpCredMan.deleteCredential(tmpCred);
//			tmpCredMan.deleteCredential("TestCred");

//			byte[] tmpBuf = tmpCredMan.protect("CredPassWord", false);
//			this.meView.displayInformation(java.util.Arrays.toString(tmpBuf) + "\n\n" + tmpBuf.length);
//			this.meView.displayInformation(tmpCredMan.unprotect(tmpBuf, false));
		} catch (model.CredentialManager.NativeException e) {
			this.meView.displayError("A NativeException was thrown with number: [" + e.getErrorCode() + "], and message: [" + e.getMessage() + "].");
		}
	}
}
