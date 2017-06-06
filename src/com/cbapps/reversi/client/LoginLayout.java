package com.cbapps.reversi.client;

import com.cbapps.reversi.ReversiConstants;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * @author Coen Boelhouwers
 * @version 1.0
 */
public class LoginLayout extends Pane {

	private TextField ipField;
	private TextField usernameField;
	private Button startGameButton;

	public LoginLayout() {
		Label reversiLabel = new Label("Reversi");
		reversiLabel.setFont(Font.font("Roboto Thin", 100));
		reversiLabel.setTextFill(Color.WHITE);
		reversiLabel.layoutXProperty().bind(widthProperty().subtract(reversiLabel.widthProperty()).divide(2));
		reversiLabel.layoutYProperty().bind(heightProperty().divide(3).subtract(reversiLabel.heightProperty()));

		Label welcomeLabel = new Label("Welcome. Insert your name:");
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

		startGameButton = new Button("Start");
		startGameButton.setFont(Font.font("Roboto"));
		startGameButton.setMinWidth(100);
		startGameButton.layoutXProperty().bind(widthProperty().subtract(startGameButton.widthProperty()).divide(2));
		startGameButton.layoutYProperty().bind(usernameField.layoutYProperty().add(usernameField.heightProperty()).add(10));
		startGameButton.setDisable(true);

		getChildren().addAll(reversiLabel, welcomeLabel, usernameField, ipField, startGameButton);
		setBackground(new Background(new BackgroundFill(Color.DARKGREEN, null, null)));

		try {
			ipField.setText(InetAddress.getLocalHost().getHostAddress());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public InetAddress getIpAddress() throws UnknownHostException {
		return InetAddress.getByName(ipField.getText());
	}

	public TextField getIpField() {
		return ipField;
	}

	public Button getStartGameButton() {
		return startGameButton;
	}

	public TextField getUsernameField() {
		return usernameField;
	}
}
