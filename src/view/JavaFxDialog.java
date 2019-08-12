package view;

import java.io.IOException;
import java.util.List;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Pair;
import lib.Constraint;
import lib.ConstraintData;

/**
 * @author Janty Azmat
 */
public class JavaFxDialog extends Dialog<ConstraintData> {

	private static class JavaFxConstraint<X> {
		// Fields
		private static final String STYLE_INVALID = "-fx-border-color: red;";
		private Constraint<X> meConstr;
		private Tooltip meTip;
		private Label meLbl;
		private Control meCtrl;

		public JavaFxConstraint(Constraint<X> valueConstraint) throws IllegalArgumentException {
			this.meConstr = valueConstraint;
			this.meTip = new Tooltip(valueConstraint.getInvalidMessage());
			this.meLbl = new Label(this.meConstr.getName());
			if (this.meConstr.getConstraintTypeClass() == Boolean.class) {
				CheckBox tmpChk = new CheckBox();
				tmpChk.setSelected((Boolean)this.meConstr.getValue());
				tmpChk.selectedProperty().addListener(this::handleValueChanged);
				this.meCtrl = tmpChk;
			} else if (this.meConstr.getConstraintTypeClass() == String.class) {
				this.meCtrl = new TextField(this.meConstr.getValue() == null ? "" : (String)this.meConstr.getValue());
			} else {
				throw new IllegalArgumentException("The provided Constraint holds unsupported data type.");
			}
		}

		private void handleValueChanged(ObservableValue<? extends Object> obsVal, Object oldVal, Object newVal) {
			@SuppressWarnings("unchecked")
			X tmpVal = (X)newVal;
			if (this.meConstr.validate(tmpVal)) {
				Tooltip.uninstall(this.meCtrl, this.meTip);
				this.meCtrl.setStyle(null);
			} else {
				Tooltip.install(this.meCtrl, this.meTip);
				this.meCtrl.setStyle(JavaFxConstraint.STYLE_INVALID);
			}
		}

		public Constraint<X> getOriginalConstraint() {
			return this.meConstr;
		}

		public Control getConstraintControl() {
			return this.meCtrl;
		}

		public Label getConstraintLabel() {
			return this.meLbl;
		}
	}

	// Fields
//	@FXML private TextField meTxtUser;
//	@FXML private PasswordField meTxtPass;
//	@FXML private CheckBox meChkNoPass;
	private Node meLoginBut;
	private ConstraintData meConstr;

	public JavaFxDialog(Window ownerWindow, String dialogTitle, String dialogMsg, ConstraintData contentConstraints) throws IOException, IllegalArgumentException {
		this.meConstr = contentConstraints;
		this.initOwner(ownerWindow);
		this.setResizable(true);
		this.setTitle(dialogTitle);
		this.setHeaderText(dialogMsg);
		this.setResultConverter(this::convertResults);
		this.setDialogPane(this.dialogPaneFactory());
		this.meLoginBut = this.getDialogPane().lookupButton(this.getDialogPane().getButtonTypes().get(0));
		this.meLoginBut.setDisable(true);
		this.getDialogPane().getScene().getWindow().showingProperty().addListener(this::handleShowing); // Added to avoid resizing dialog too small
//		this.meTxtPass.disableProperty().bind(this.meChkNoPass.selectedProperty());
//		this.meLoginBut.disableProperty().bind(this.meTxtUser.textProperty().isEmpty());
	}

	private ConstraintData convertResults(ButtonType butType) {
		this.meConstr.setFilled(butType.getButtonData() == ButtonData.OK_DONE);
		return this.meConstr;
	}

	private void handleShowing(ObservableValue<? extends Boolean> obsVal, boolean oldVal, boolean newVal) {
		if (newVal) {
			double tmpDifW = this.getDialogPane().getScene().getWindow().getWidth() - this.getDialogPane().getWidth();
			double tmpDifH = this.getDialogPane().getScene().getWindow().getHeight() - this.getDialogPane().getHeight();
			((Stage)this.getDialogPane().getScene().getWindow()).setMinWidth(this.getDialogPane().getWidth() + tmpDifW);
			((Stage)this.getDialogPane().getScene().getWindow()).setMinHeight(this.getDialogPane().getHeight() + tmpDifH);
		}
	}

	private static Pair<Label, Control> controlFactory(Constraint<?> controlData) throws IllegalArgumentException {
		Label tmpLbl = new Label(controlData.getName());
		Pair<Label, Control> outPair = null;
		if (controlData.getConstraintTypeClass() == Boolean.class) {
			CheckBox tmpChk = new CheckBox();
			tmpChk.setSelected((Boolean)controlData.getValue());
			outPair = new Pair<>(tmpLbl, tmpChk);
		} else if (controlData.getConstraintTypeClass() == String.class) {
			outPair = new Pair<>(tmpLbl, new TextField(controlData.getValue() == null ? "" : (String)controlData.getValue()));
		} else {
			throw new IllegalArgumentException("The provided Constraint holds unsupported data type.");
		}
		return outPair;
	}

	private DialogPane dialogPaneFactory() {
		DialogPane outPane = new DialogPane();

		return outPane;
	}
}
