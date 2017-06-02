package com.cbapps.reversi.client;

import com.cbapps.reversi.ReversiConstants;
import com.cbapps.reversi.ReversiPlayer;
import com.cbapps.reversi.SimplePlayer;
import com.cbapps.reversi.board.Board;
import com.cbapps.reversi.board.BoardGridPane;
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
 * The client main layout.
 *
 * @author Coen Boelhouwers, Gijs van Fessem
 */
public class ClientMain extends Application implements ReversiConstants {

	private ReversiPlayer player;
	private List<SimplePlayer> otherPlayers;
	private Board board;
	private ExecutorService service;

	//Login Layout
	private Scene 		loginScene;
	private TextField 	loginUsernameField;
	private Button 		loginStartGameButton;

	//Board Layout
	private Scene 		boardScene;
	private GridPane	boardGridPane;
	private Label 		boardStatusLabel;

	private Stage primaryStage;

	@Override
	public void start(Stage primaryStage) throws Exception {
		this.primaryStage = primaryStage;
		service = Executors.newCachedThreadPool();

		player = new ReversiPlayer("Player 1", Color.WHITE, null);
		player.setSessionId(1);

		//Login Scene
		VBox layout1 = new VBox(40);
		Label welcomelabel = new Label("Welcome, Please insert name here.");
		loginStartGameButton = new Button("Start");
		loginStartGameButton.setDisable(true);
		loginUsernameField = new TextField();
		loginUsernameField.setOnAction(event -> {
			loginUsernameField.setDisable(true);
			service.submit(() -> {
				if (connectToServer(loginUsernameField.getText()))
					playGame();
			});
		});

		layout1.getChildren().addAll(welcomelabel, loginUsernameField, loginStartGameButton);
		loginScene = new Scene(layout1, 300, 300);
		loginStartGameButton.setOnAction(e -> service.submit(() -> {
			try {
				System.out.println("Send start game");
				ObjectOutputStream dos = player.getOutputStream();
				dos.writeInt(CLIENT_SEND_START_GAME);
				dos.flush();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}));

		//Board Scene
		board = new Board(8, 8);
		boardGridPane = new BoardGridPane(board, cell -> {
			if (board.changeAllValidCells(cell.getRow(), cell.getColumn(), player.getSessionId())) {
				boardGridPane.setDisable(true);
				service.submit(() -> {
					try {
						player.getOutputStream().writeInt(CLIENT_SEND_MOVE);
						player.getOutputStream().writeInt(cell.getRow());
						player.getOutputStream().writeInt(cell.getColumn());
						player.getOutputStream().flush();
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
			}
		});
		boardStatusLabel = new Label();
		boardGridPane.setDisable(true);

		BorderPane borderPane = new BorderPane();
		borderPane.setCenter(boardGridPane);
		borderPane.setBottom(boardStatusLabel);
		boardScene = new Scene(borderPane, 400, 400);

		primaryStage.setScene(loginScene);
		primaryStage.setTitle("ReversiClient");
		primaryStage.show();
	}

	private boolean connectToServer(String playerName) {
		try {
			Socket socket = new Socket(InetAddress.getLocalHost(), 8081);

			player = new ReversiPlayer(playerName, socket);
			otherPlayers = new ArrayList<>();

			//Introduce yourself
			//Send player name
			ObjectOutputStream dos = player.getOutputStream();
			dos.writeUTF(playerName);
			dos.flush();

			//Receive available sessions
			ObjectInputStream ois = player.getInputStream();
			List<String> availableSessions = (List<String>) ois.readObject();

			String chosenSessionName;
			if (availableSessions.isEmpty()) {
				chosenSessionName = "Sessie1";
			} else {
				chosenSessionName = availableSessions.get(0);
			}

			//Send chosen session
			dos.writeUTF(chosenSessionName);
			dos.flush();

			//Receive the sessionID
			player.setSessionId(ois.readInt());
			System.out.println("Received sessionID: " + player.getSessionId());

			//If we are the first player, we may start the game
			if (player.getSessionId() == 1) loginStartGameButton.setDisable(false);

			//Now, the server will send info about which other players will contest.
			int command;
			while ((command = ois.readInt()) != CLIENT_RECEIVE_START_GAME) {
				System.out.println("a Received '" + command + "'");
				if (command == CLIENT_RECEIVE_PLAYER_ADDED) {
					System.out.println("Received player addition notification, expecting object");
					SimplePlayer otherPlayer = (SimplePlayer) ois.readObject();
					System.out.print("We'll be playing against " + otherPlayer);
					otherPlayers.add(otherPlayer);
				}
			}

			//The start-game signal was send
			System.out.println("Read start game signal");
			int playerAmount = ois.readInt();
			System.out.println("Total amount of player in this session: " + playerAmount);
//			for (int i = 0; i < playerAmount; i++) {
//				if (i != player.getSessionId())
//					otherPlayers.add(new SimplePlayer("Player " + (i + 1), Color.BLACK).setSessionId(i));
//			}

			//Setup the colors of the players (currently determined by id)
			//Board.setupPlayerColors(otherPlayers);

			//Go to board scene
			Platform.runLater(this::goToBoardScene);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private SimplePlayer getPlayerById(int playerId) {
		return otherPlayers.stream().filter(p -> p.getSessionId() == playerId).findFirst().orElse(null);
	}

	private void goToBoardScene() {
		primaryStage.setScene(boardScene);
	}

	private void goToLoginScene() {
		primaryStage.setScene(loginScene);
	}

	/**
	 * Gameplay logic. This method returns when the game has finished.
	 */
	private void playGame() {
		try {
			ObjectInputStream ois = player.getInputStream();
			while (true) {
				int com = ois.readInt();
				if (com == CLIENT_RECEIVE_OTHER_WON) {
					int playerId = ois.readInt();
					Platform.runLater(() -> boardStatusLabel.setText("Ahw... '" +
							getPlayerById(playerId).getName() + "' won. Next time better."));
					break;
				} else if (com == CLIENT_RECEIVE_YOU_WON) {
					Platform.runLater(() -> boardStatusLabel.setText("You won! Congrats!"));
					break;
				} else if (com == CLIENT_RECEIVE_OTHER_START_MOVE) {
					SimplePlayer p = getPlayerById(ois.readInt());
					Platform.runLater(() -> boardStatusLabel.setText("Waiting for " + p.getName() + "'s move..."));
				} else if (com == CLIENT_RECEIVE_OTHER_DID_MOVE) {
					int playerId = ois.readInt();
					int row = ois.readInt();
					int column = ois.readInt();
					board.changeAllValidCells(row, column, playerId);
				} else if (com == CLIENT_RECEIVE_START_MOVE) {
					boardGridPane.setDisable(false);
					Platform.runLater(() -> boardStatusLabel.setText("It's your turn"));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Game ended.");
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
