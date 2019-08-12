/**
 * @author Janty Azmat
 */
module easyEmail {
	requires javafx.controls;
	requires javafx.fxml;
	requires transitive javafx.graphics;
//	requires javafx.graphics;
	opens loader to javafx.graphics;
	opens view to javafx.fxml;
//	opens view to javafx.fxml;
}
