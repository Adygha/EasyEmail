package controller;

import lib.Constraint;
import lib.ConstraintList;
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
		Constraint<String> tmpUserCons = new Constraint<>(String.class, "User Name", false, true, "User name must not be empty or white-space.", str -> str != null && !str.isBlank());
		Constraint<String> tmpPass = new Constraint<>(String.class, "Password", true);
		tmpPass.setValue("OldPass");
		Constraint<Boolean> tmpMandBool = new Constraint<Boolean>(Boolean.class, "Mandatory Bool", false, true, "This must be true.", bol -> bol);
		Constraint<Boolean> tmpBool = new Constraint<Boolean>(Boolean.class, "This does not matter.");
		ConstraintList tmpData = new ConstraintList();
		tmpData.add(tmpUserCons);
		tmpData.add(tmpPass);
		tmpData.add(tmpMandBool);
		tmpData.add(tmpBool);
		this.meView.displayInformation(this.meView.requestData("Login", "Please login.", tmpData).toString());
	}
}
