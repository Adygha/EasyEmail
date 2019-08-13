package view;

import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import lib.Constraint;
import lib.ConstraintList;

/**
 * @author Janty Azmat
 */
public class JavaFxDialog extends Dialog<ConstraintList> {

	private static class JavaFxConstraint<X> {
		// Fields
		private static final String STYLE_VALID = "-fx-border-color: transparent; -fx-border-width: 2;";
		private static final String STYLE_INVALID = "-fx-border-color: red; -fx-border-width: 2; -fx-border-radius: 3; -fx-background-color: mistyrose;";
//		private static final String STYLE_INVALID = "-fx-background-color: mistyrose,linear-gradient(to bottom, derive(red,60%) 5%,derive(red,90%) 5%);";
//		private static final String STYLE_INVALID = "-fx-foreground-color: red; -fx-background-color: linear-gradient(red 0%, mistyrose 5%, mistyrose 95%, red 100%), linear-gradient(to right, red 0%, mistyrose 2%, mistyrose 98%, red 100%);";
		private Constraint<X> meConstr;
		private Tooltip meTip;
		private Label meLbl;
		private Control meCtrl;
		private Property<X> meCtrlValProp;

		@SuppressWarnings("unchecked")
		public JavaFxConstraint(Constraint<X> valueConstraint) throws IllegalArgumentException {
			this.meConstr = valueConstraint;
			this.meTip = new Tooltip(valueConstraint.getInvalidMessage());
			this.meLbl = new Label(this.meConstr.getName() + (this.meConstr.isMandatory() ? " (required)" : ""));
			if (this.meConstr.getConstraintTypeClass() == Boolean.class) {
				CheckBox tmpChk = new CheckBox();
				tmpChk.setSelected(this.meConstr.isValuePresent() ? (Boolean)this.meConstr.getValue() : false);
				this.meCtrlValProp = (Property<X>)tmpChk.selectedProperty();
				this.meCtrl = tmpChk;
			} else if (this.meConstr.getConstraintTypeClass() == String.class) {
				TextField tmpTxt = this.meConstr.isConfidential() ? new PasswordField() : new TextField();
				tmpTxt.setText(this.meConstr.isValuePresent() ? (String)this.meConstr.getValue() : "");
				this.meCtrlValProp = (Property<X>)tmpTxt.textProperty();
				this.meCtrl = tmpTxt;
			} else {
				throw new IllegalArgumentException("The provided Constraint holds unsupported data type.");
			}
			this.meCtrl.setStyle(JavaFxConstraint.STYLE_VALID);
//			this.meTip.setAutoFix(false);
			this.meLbl.setLabelFor(this.meCtrl);
			GridPane.setHgrow(this.meCtrl, Priority.ALWAYS);
			this.meCtrlValProp.addListener(this::handleValueChanged);
		}

		private void handleControlBoundsChange(ObservableValue<? extends Number> obsVal, Number oldVal, Number newVal) {
			Point2D tmpPnt = this.meCtrl.localToScreen(this.meCtrl.getWidth() + 3.0, 0);
			this.meTip.setAnchorX(tmpPnt.getX());
			this.meTip.setAnchorY(tmpPnt.getY());
		}

		private void handleValueChanged(ObservableValue<? extends X> obsVal, X oldVal, X newVal) {
			Window tmpWin = JavaFxConstraint.this.meCtrl.getScene().getWindow();
			if (this.meConstr.validate(newVal)) {
				if (this.meTip.isShowing()) {
					this.meTip.hide();
					this.meCtrl.setStyle(JavaFxConstraint.STYLE_VALID);
					tmpWin.xProperty().removeListener(this::handleControlBoundsChange);
					tmpWin.yProperty().removeListener(this::handleControlBoundsChange);
					tmpWin.widthProperty().removeListener(this::handleControlBoundsChange);
				}
			} else {
				if (!this.meTip.isShowing()) {
					Point2D tmpPnt = this.meCtrl.localToScreen(this.meCtrl.getWidth() + 3.0, 0);
					this.meCtrl.setStyle(JavaFxConstraint.STYLE_INVALID);
					this.meTip.show(this.meCtrl, tmpPnt.getX(), tmpPnt.getY());
					tmpWin.xProperty().addListener(this::handleControlBoundsChange);
					tmpWin.yProperty().addListener(this::handleControlBoundsChange);
					tmpWin.widthProperty().addListener(this::handleControlBoundsChange);
				}
			}
		}

		public void addValueChangedListener(ChangeListener<X> changeListener) {
			this.meCtrlValProp.addListener(changeListener);
		}

		public Control getConstraintControl() {
			return this.meCtrl;
		}

		public Label getConstraintLabel() {
			return this.meLbl;
		}

		public Constraint<X> getOriginalConstraint() {
			return this.meConstr;
		}

		public boolean validateInput() {
			return this.meConstr.validate(this.meCtrlValProp.getValue());
		}

		public void commitInput() {
			this.meConstr.setValue(this.meCtrlValProp.getValue());
		}
	}

	// Fields
	private Stage meStage;
	private Button meLoginBut;
	private ConstraintList meConstrList;
	private List<JavaFxConstraint<? extends Object>> meFxConstrList;

	public JavaFxDialog(Window ownerWindow, String dialogTitle, String dialogMsg, ConstraintList contentConstraints) throws IllegalArgumentException {
		this.meConstrList = contentConstraints;
		this.initOwner(ownerWindow);
		this.setResizable(true);
		this.setTitle(dialogTitle);
		this.setResultConverter(this::convertResults);
		this.initModality(Modality.APPLICATION_MODAL);
		this.setDialogPane(this.dialogPaneFactory());
		this.meStage = (Stage)this.getDialogPane().getScene().getWindow();
		this.getDialogPane().setHeaderText(dialogMsg);
		this.meStage.showingProperty().addListener(this::handleShowing); // Added to avoid resizing dialog too small
		this.meStage.initStyle(StageStyle.UTILITY);
//		this.meStage.onShownProperty().addListener(obs -> this.meStage.centerOnScreen());
	}

	private ConstraintList convertResults(ButtonType butType) {
		if (butType.getButtonData() == ButtonData.OK_DONE) {
			this.meFxConstrList.stream().forEach(JavaFxConstraint::commitInput);
			this.meConstrList.setFilled(true);
		} else {
			this.meConstrList.setFilled(false);
		}
		return this.meConstrList;
	}

	private void handleShowing(ObservableValue<? extends Boolean> obsVal, boolean oldVal, boolean newVal) {
		if (newVal) {
			this.meStage.showingProperty().removeListener(this::handleShowing);
			this.meStage.setMinWidth(this.meStage.getWidth());
			this.meStage.setMinHeight(this.meStage.getHeight());
			this.meStage.setMaxWidth(this.meStage.getWidth() * 2.0);
			this.meStage.setMaxHeight(this.meStage.getHeight() * 2.0);
//			this.meStage.centerOnScreen();
		}
	}

	private DialogPane dialogPaneFactory() throws IllegalArgumentException {
		DialogPane outPane = new DialogPane();
		GridPane tmpGrid = new GridPane();
		this.meFxConstrList = this.meConstrList.stream().map(cons -> new JavaFxConstraint<>(cons)).collect(Collectors.toList());
		outPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		this.meLoginBut = (Button)outPane.lookupButton(ButtonType.OK);
		this.meLoginBut.setDisable(true);
		tmpGrid.setHgap(10.0);
		tmpGrid.setVgap(5.0);
		tmpGrid.setPadding(new Insets(5.0, 5.0, 5.0, 10.0));
		tmpGrid.setMinWidth(Screen.getPrimary().getVisualBounds().getWidth() / 4.0);
		for (int i = 0; i < this.meFxConstrList.size(); i++) {
			tmpGrid.add(this.meFxConstrList.get(i).getConstraintLabel(), 0, i);
			tmpGrid.add(this.meFxConstrList.get(i).getConstraintControl(), 1, i);
//			GridPane.setHgrow(tmpFxCons.get(i).getConstraintControl(), Priority.ALWAYS);
			if (this.meFxConstrList.get(i).getOriginalConstraint().isMandatory()) {
				this.meFxConstrList.get(i).addValueChangedListener((obsVal, oldVal, newVal) -> {
					this.meLoginBut.setDisable(this.meFxConstrList.stream().anyMatch(cons -> cons.getOriginalConstraint().isMandatory() && !cons.validateInput()));
				});
			}
		}
		this.meLoginBut.setOnAction(acEv -> this.meFxConstrList.stream().forEach(JavaFxConstraint::commitInput));
		outPane.setContent(tmpGrid);
		return outPane;
	}
}
