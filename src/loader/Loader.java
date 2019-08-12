package loader;

import java.io.IOException;
import controller.Hive;
import javafx.application.Application;
import javafx.stage.Stage;
import view.IEmailDisplay;
import view.JavaFxEmailDisplay;

/**
 * Just a loader class.
 * @author Janty Azmat
 */
public class Loader extends Application {
	// Fields
	private IEmailDisplay meView;

	@Override
	public void start(Stage primaryStage) {
		try {
			this.meView = new JavaFxEmailDisplay(primaryStage);
		} catch (IOException e) {
			System.out.println("There was an error constructing the user interface. Please check that all the application files are provided.");
			e.printStackTrace();
		}
		Thread.currentThread().setUncaughtExceptionHandler((thrd, thrw) -> { // To handle any un-handled exception encountered by the JavaFX thread
			this.meView.displayError("Error: An unhandled exception was thrown with message: " + thrw.getMessage() + "\nExitting application.");
			primaryStage.close();
		});
		Hive tmpController = new Hive(this.meView);
		tmpController.start();
	}

	/**
	 * Main entry.
	 * @param args	passed program command-line arguments.
	 */
	public static void main(String[] args) {
		Application.launch(args);
	}
}
