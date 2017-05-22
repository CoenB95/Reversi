package com.cbapps.reversi.client;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Cell;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Coen Boelhouwers
 */
public class ClientMain extends Application {

	private ExecutorService service;
	private Label lblStatus = new Label();
	private CellPane[][] cell = new CellPane[8][8];
	private TextField Username;
	Stage Window;
	Scene scene1;
	Scene scene2;

	@Override
	public void start(Stage primaryStage) throws Exception {
		Window = primaryStage;

		//Layout 1
		VBox layout1 = new VBox(40);
		Label Welcomelabel = new Label("Welcome, Please insert name here.");
		Button button = new Button("Start");
		Username = new TextField();
		layout1.getChildren().addAll(Welcomelabel,Username,button);
		scene1 = new Scene(layout1,300,300);
		button.setOnAction(e -> Window.setScene(scene2));

		//Layout 2
		GridPane gridpane = new GridPane();
		for (int i = 0; i < 8; i++)
		for (int j = 0; j < 8; j++)
		gridpane.add(cell[i][j] = new CellPane(), j, i);

		BorderPane borderPane = new BorderPane();
		borderPane.setCenter(gridpane);
		borderPane.setBottom(lblStatus);
		scene2 = new Scene(borderPane,400,400);

		Window.setScene(scene1);
		Window.setTitle("ReversiClient");
		Window.show();



		service = Executors.newCachedThreadPool();
		service.submit(() -> {
			try {
				Socket socket = new Socket(InetAddress.getLocalHost(), 8000);
				DataOutputStream stream = new DataOutputStream(socket.getOutputStream());
				stream.writeUTF("Hello!");
			} catch (IOException e) {
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
