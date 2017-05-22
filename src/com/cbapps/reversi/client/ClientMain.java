package com.cbapps.reversi.client;

import com.cbapps.reversi.PlayerInfo;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Coen Boelhouwers
 */
public class ClientMain extends Application {

	private ExecutorService service;
	private Label lblStatus = new Label();
	private CellPane[][] cell = new CellPane[8][8];
	private TextField username;
	Stage window;
	Scene scene1;
	Scene scene2;

	@Override
	public void start(Stage primaryStage) throws Exception {
		window = primaryStage;

		//Layout 1
		VBox layout1 = new VBox(40);
		Label welcomelabel = new Label("Welcome, Please insert name here.");
		Button button = new Button("Start");
		username = new TextField();
		layout1.getChildren().addAll(welcomelabel, username,button);
		scene1 = new Scene(layout1,300,300);
		button.setOnAction(e -> window.setScene(scene2));

		//Layout 2
		GridPane gridpane = new GridPane();
		for (int i = 0; i < 8; i++)
		for (int j = 0; j < 8; j++)
		gridpane.add(cell[i][j] = new CellPane(), j, i);

		BorderPane borderPane = new BorderPane();
		borderPane.setCenter(gridpane);
		borderPane.setBottom(lblStatus);
		scene2 = new Scene(borderPane,400,400);

		window.setScene(scene1);
		window.setTitle("ReversiClient");
		window.show();



		service = Executors.newCachedThreadPool();
		service.submit(() -> {
			try {
				Socket socket = new Socket(InetAddress.getLocalHost(), 8000);

				//Introduce yourself
				ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
				oos.writeObject(new PlayerInfo("Coen", "white"));
				oos.flush();

				//Receive available sessions
				ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
				List<String> availableSessions = (List<String>) ois.readObject();

				String chosen;
				if (availableSessions.isEmpty()) {
					chosen = "Een of andere nieuwe sessie";
				} else {
					chosen = availableSessions.get(0);
				}

				//Send session choice
				oos.writeObject(chosen);

			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		});
	}

	@Override
	public void stop() throws Exception {
		super.stop();
		service.shutdown();
	}

	public static void main(String[] args) {
		launch(ClientMain.class, args);
	}
}
