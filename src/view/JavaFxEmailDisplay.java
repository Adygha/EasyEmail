package view;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;
import lib.Constraint;
import lib.ConstraintData;
import lib.EventEmitter;
import lib.IEventEmitter;

/**
 * @author Janty Azmat
 */
public class JavaFxEmailDisplay extends SplitPane implements IEmailDisplay {
	// Fields
	private EventEmitter<EmailDisplayEvent> meEmit;
	private Stage meStage;
	@FXML private SplitPane meEmailSplit;
	@FXML private TreeView<String> meSideBar;
	@FXML private TableView<String[]> meEmailList;
	@FXML private AnchorPane meEmailBoard;

	/**
	 * Default constructor for the user interface.
	 * @throws IOException	when building the interface from its files fails.
	 */
	public JavaFxEmailDisplay(Stage theStage) throws IOException {
		this.meEmit = new EventEmitter<>(EmailDisplayEvent.class);
		this.meStage = theStage;
		FXMLLoader tmpLoad = new FXMLLoader(this.getClass().getResource("JavaFxEmailDisplay.fxml"));
		tmpLoad.setController(this);
		tmpLoad.setRoot(this);
		tmpLoad.load();
//		meEmailSplit.setOrientation(Orientation.HORIZONTAL);
	}

	private void displayMesssage(String theMsg, String msgTitle, AlertType msgType) {
		Alert tmpAlert = new Alert(msgType);
		((Label)tmpAlert.getDialogPane().getChildren().get(1)).setMinWidth(600.0);
		String tmpStyle = "-fx-font-size: 16px; -fx-font-weight: bold;";
		switch (msgType) {
			case ERROR:
				tmpStyle += "-fx-text-fill: red;";
				break;
			case WARNING:
				tmpStyle += "-fx-text-fill: orangered;";
				break;
			default:
				tmpStyle += "-fx-text-fill: mediumblue;";
		}
		tmpAlert.getDialogPane().getChildren().get(1).setStyle(tmpStyle);
		tmpAlert.initModality(Modality.APPLICATION_MODAL);
		tmpAlert.initOwner(this.getScene().getWindow());
		tmpAlert.setHeaderText(null);
		tmpAlert.setTitle(msgTitle);
		tmpAlert.setContentText(theMsg);
		tmpAlert.showAndWait();
	}

	@Override
	public void startDisplay() {
		this.meStage.setTitle("Email Client");
		this.meStage.setScene(new Scene(this));
		this.meStage.centerOnScreen();
		this.meStage.show();

		// TODO: Remove the rest down.
		TreeItem<String> tmpTreeRoot = new TreeItem<>();
		tmpTreeRoot.getChildren().add(new TreeItem<String>("One"));
		tmpTreeRoot.getChildren().add(new TreeItem<String>("Two"));
		tmpTreeRoot.getChildren().add(new TreeItem<String>("Three"));
		this.meSideBar.setRoot(tmpTreeRoot);
		this.meSideBar.setShowRoot(false);
		for (int i = 0; i < 50; i++) {
			this.meEmailList.getItems().add(new String[] {"cell1", "cell2"});
		}
	}

	@Override
	public boolean addEventListener(EmailDisplayEvent theEvent, BiConsumer<IEventEmitter<EmailDisplayEvent>, Object> eventListener) {
		return this.meEmit.addEventListener(theEvent, eventListener);
	}

	@Override
	public boolean removeEventListener(EmailDisplayEvent theEvent, BiConsumer<IEventEmitter<EmailDisplayEvent>, Object> eventListener) {
		return this.meEmit.removeEventListener(theEvent, eventListener);
	}

	@Override
	public void emitEvent(EmailDisplayEvent theEvent, Object eventData) {
		this.meEmit.emitEvent(theEvent, eventData);
	}

	@Override
	public void emitEvent(EmailDisplayEvent theEvent) {
		this.meEmit.emitEvent(theEvent);
	}

	@Override
	public ConstraintData requestData(String dataTitle, String dataMsg, ConstraintData requestedData) throws IllegalArgumentException {
		JavaFxDialog tmpDlg = null;
		try {
			tmpDlg = new JavaFxDialog(this.getScene().getWindow(), dataTitle, dataMsg, requestedData);
		} catch (IOException e) {}
		var tmpVal = tmpDlg.showAndWait();
		this.displayInformation(tmpVal.toString());
//		this.displayInformation(String.valueOf(tmpVal.isEmpty()));
//		this.displayInformation(String.valueOf(tmpVal.isPresent()));
		return requestedData;
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
	public void displayInformation(String infoMsg) {
		this.displayMesssage(infoMsg, "Information..", AlertType.INFORMATION);
	}

	@Override
	public void displayWarning(String warningMsg) {
		this.displayMesssage(warningMsg, "Warning..", AlertType.WARNING);
	}

	@Override
	public void displayError(String errorMsg) {
		this.displayMesssage(errorMsg, "Error..", AlertType.ERROR);
	}

	@Override
	public void exit() {
		// TODO Auto-generated method stub

	}
}
