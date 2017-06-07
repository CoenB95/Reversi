package com.cbapps.reversi.client;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author Coen Boelhouwers
 * @version 1.0
 */
public class LoginLayout extends Pane {

	private static final String ITEM_NEW_SESSION = "New Session ->";

	private TextField ipField;
	private TextField usernameField;
	private Button startGameButton;
	private ComboBox<String> sessionComboBox;
	private TextField newSessionField;
	private Label welcomeLabel;

	private StringProperty chosenSession;

	public LoginLayout() {
		chosenSession = new SimpleStringProperty();

		Label reversiLabel = new Label("Reversi");
		reversiLabel.setFont(Font.font("Roboto Thin", 100));
		reversiLabel.setTextFill(Color.WHITE);
		reversiLabel.layoutXProperty().bind(widthProperty().subtract(reversiLabel.widthProperty()).divide(2));
		reversiLabel.layoutYProperty().bind(heightProperty().divide(3).subtract(reversiLabel.heightProperty()));

		welcomeLabel = new Label("Welcome. Insert your name:");
		welcomeLabel.setFont(Font.font("Roboto", 20));
		welcomeLabel.setTextFill(Color.WHITE);
		welcomeLabel.setWrapText(true);
		welcomeLabel.maxWidthProperty().bind(widthProperty());
		welcomeLabel.layoutXProperty().bind(widthProperty().subtract(welcomeLabel.widthProperty()).divide(2));
		welcomeLabel.layoutYProperty().bind(heightProperty().divide(3));

		ipField = new TextField();
		ipField.setPromptText("Server IP");
		ipField.layoutXProperty().bind(widthProperty().subtract(ipField.widthProperty()).divide(2));
		ipField.layoutYProperty().bind(welcomeLabel.layoutYProperty().add(welcomeLabel.heightProperty()));

		usernameField = new TextField();
		usernameField.setPromptText("Username");
		usernameField.layoutXProperty().bind(widthProperty().subtract(usernameField.widthProperty()).divide(2));
		usernameField.layoutYProperty().bind(ipField.layoutYProperty().add(ipField.heightProperty()));

		sessionComboBox = new ComboBox<>();
		sessionComboBox.layoutXProperty().bind(widthProperty().divide(2).subtract(sessionComboBox.widthProperty()));
		sessionComboBox.layoutYProperty().bind(usernameField.layoutYProperty().add(usernameField.heightProperty()).add(10));

		newSessionField = new TextField();
		newSessionField.setPromptText("Session name");
		newSessionField.layoutXProperty().bind(widthProperty().divide(2));
		newSessionField.layoutYProperty().bind(sessionComboBox.layoutYProperty());

		startGameButton = new Button("Start");
		startGameButton.setFont(Font.font("Roboto"));
		startGameButton.setMinWidth(100);
		startGameButton.layoutXProperty().bind(widthProperty().subtract(startGameButton.widthProperty()).divide(2));
		startGameButton.layoutYProperty().bind(newSessionField.layoutYProperty().add(newSessionField.heightProperty()).add(10));
		startGameButton.setDisable(true);

		getChildren().addAll(reversiLabel, welcomeLabel, usernameField, ipField, startGameButton,
				sessionComboBox, newSessionField);
		setBackground(new Background(new BackgroundFill(Color.DARKGREEN, null, null)));

		try {
			ipField.setText(InetAddress.getLocalHost().getHostAddress());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		sessionComboBox.getItems().add(ITEM_NEW_SESSION);
		sessionComboBox.setOnAction(event -> {
			if (sessionComboBox.getValue().equals(ITEM_NEW_SESSION)) {
				newSessionField.setDisable(false);
			} else {
				newSessionField.setDisable(true);
				chosenSession.set(sessionComboBox.getValue());
			}
		});
		newSessionField.setOnAction(event -> {
			if (sessionComboBox.getValue().equals(ITEM_NEW_SESSION) &&
					!getSessionOptions().contains(newSessionField.getText())) {
				chosenSession.set(newSessionField.getText());
			}
		});
		disableSessionChosing(true);
	}

	public void disableSessionChosing(boolean value) {
		sessionComboBox.setDisable(value);
		newSessionField.setDisable(value || sessionComboBox.getValue() == null ||
				!sessionComboBox.getValue().equals(ITEM_NEW_SESSION));
	}

	public InetAddress getIpAddress() throws UnknownHostException {
		return InetAddress.getByName(ipField.getText());
	}

	public TextField getIpField() {
		return ipField;
	}

	public ObservableList<String> getSessionOptions() {
		return sessionComboBox.getItems();
	}

	public Button getStartGameButton() {
		return startGameButton;
	}

	public Label getStatusLabel() {
		return welcomeLabel;
	}

	public TextField getUsernameField() {
		return usernameField;
	}

	public void setOnSessionChosen(OnSessionChosenListener l) {
		chosenSession.addListener((v1, v2, v3) -> {
			if (!sessionComboBox.isDisabled() && l != null) l.onSessionChosen(v3);
		});
	}

	public void reset() {
		welcomeLabel.setText("Connection lost");
		usernameField.setDisable(false);
		ipField.setDisable(false);
		startGameButton.setDisable(true);
		getSessionOptions().setAll(ITEM_NEW_SESSION);
		chosenSession.set("");
		sessionComboBox.setDisable(true);
		newSessionField.setDisable(true);
		newSessionField.setText("");
	}

	public interface OnSessionChosenListener {
		void onSessionChosen(String sessionName);
	}
}
