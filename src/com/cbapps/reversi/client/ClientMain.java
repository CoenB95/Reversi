package com.cbapps.reversi.client;

import com.cbapps.reversi.ReversiConstants;
import com.cbapps.reversi.ReversiPlayer;
import com.cbapps.reversi.SimplePlayer;
import com.cbapps.reversi.board.Board;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Coen Boelhouwers
 */
public class ClientMain extends Application implements ReversiConstants {

	private ReversiPlayer player;
	private List<SimplePlayer> otherPlayers;
	private ExecutorService service;
	private Label lblStatus = new Label();
	private Board board;
	private CellPane[][] cell = new CellPane[8][8];
	private TextField username;
	private Button startGameButton;
	private Scene boardScene;
	private Stage primaryStage;

	private int playerIndex;

	@Override
	public void start(Stage primaryStage) throws Exception {
		this.primaryStage = primaryStage;
		service = Executors.newCachedThreadPool();
		otherPlayers = new ArrayList<>();

		player = new ReversiPlayer("Player 1", Color.WHITE, null);
		player.setSessionId(1);
		otherPlayers = new ArrayList<>();
		otherPlayers.add(player);
		otherPlayers.add(new SimplePlayer("Player 2", Color.WHITE).setSessionId(2));

		//Layout 1
		VBox layout1 = new VBox(40);
		Label welcomelabel = new Label("Welcome, Please insert name here.");
		startGameButton = new Button("Start");
		startGameButton.setDisable(true);
		username = new TextField();
		username.setOnAction(event -> {
			username.setDisable(true);
			service.submit(() -> {
				connectToServer(username.getText());
				playGame();
			});
		});

		layout1.getChildren().addAll(welcomelabel, username, startGameButton);
		Scene loginScene = new Scene(layout1, 300, 300);
		startGameButton.setOnAction(e -> {
			System.out.println("Button pressed");
			service.submit(() -> {
				try {
					System.out.println("Send start game");
					ObjectOutputStream dos = player.getOutputStream();
					System.out.println("- writeInt");
					dos.writeInt(CLIENT_SEND_START_GAME);
					System.out.println("- flush");
					dos.flush();
					System.out.println("- done");
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			});
		});

		//Layout 2
		GridPane gridpane = new GridPane();
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				CellPane c = new CellPane(i, j);
				c.setOnMouseClicked(event -> {
					if (board.changeAllValidCells(c.getRow(), c.getColumn(),
							otherPlayers.get(playerIndex).getSessionId())) {
						playerIndex = (playerIndex + 1) % 2;
						Platform.runLater(() -> lblStatus.setText("It's " + otherPlayers.get(playerIndex).getName() +
								"'s turn!"));
					}
				});
				gridpane.add(cell[i][j] = c, j, i);
			}
		}

		board = new Board(8, 8, (row, column, playerId) -> {
			cell[row][column].changePossession(playerId == 1 ? Color.BLACK : Color.WHITE);
		});

		BorderPane borderPane = new BorderPane();
		borderPane.setCenter(gridpane);
		borderPane.setBottom(lblStatus);
		boardScene = new Scene(borderPane, 400, 400);

		primaryStage.setScene(loginScene);
		primaryStage.setTitle("ReversiClient");
		primaryStage.show();
	}

	private void connectToServer(String playerName) {
		try {
			Socket socket = new Socket(InetAddress.getLocalHost(), 8081);

			player = new ReversiPlayer(playerName, socket);

			//Introduce yourself
			ObjectOutputStream dos = player.getOutputStream();
			dos.writeUTF(playerName);
			dos.flush();

			//Receive available sessions
			ObjectInputStream ois = player.getInputStream();
			List<String> availableSessions = (List<String>) ois.readObject();

			String chosenSessionName;
			if (availableSessions.isEmpty()) {
				chosenSessionName = "Een of andere nieuwe sessie";
			} else {
				chosenSessionName = availableSessions.get(0);
			}

			//Send session choice
			dos.writeUTF(chosenSessionName);
			dos.flush();

			//Receive the sessionID
			player.setSessionId(ois.readInt());
			System.out.println("Received sessionID: " + player.getSessionId());

			//If we are the first player, we may start the game
			if (player.getSessionId() == 1) startGameButton.setDisable(false);

			//Now, the server will send info about which other players will contest.
			int avail;
			int command = 0;// = ois.readInt();
			while ((command = ois.readInt()) != CLIENT_RECEIVE_START_GAME) {
				System.out.println("a Received '" + command + "'");
				if (command == CLIENT_RECEIVE_PLAYER_ADDED) {
					System.out.println("Received player addition notification, expecting object");
					SimplePlayer otherPlayer = (SimplePlayer) ois.readObject();
					System.out.print("We'll be playing against " + otherPlayer);
					otherPlayers.add(otherPlayer);
				}
				command = 0;
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				//command = ois.readInt();
			}
			System.out.println("Read start game signal");
			int playerAmount = ois.readInt();
			System.out.println("Total amount of player in this session: " + playerAmount);
			for (int i = 0; i < playerAmount; i++) otherPlayers.add(
					new SimplePlayer("Player " + (i + 1), Color.BLACK));
			Board.setupPlayerColors(otherPlayers);

			//Go to board layout.
			Platform.runLater(() -> primaryStage.setScene(boardScene));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void playGame() {

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
