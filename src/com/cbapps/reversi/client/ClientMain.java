package com.cbapps.reversi.client;

import com.cbapps.reversi.ReversiConstants;
import com.cbapps.reversi.ReversiPlayer;
import com.cbapps.reversi.ReversiSession;
import com.cbapps.reversi.SimplePlayer;
import com.cbapps.reversi.board.Board;
import com.cbapps.reversi.board.BoardGridPane;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
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
	private Board board;
	private ExecutorService service;
	private boolean placeFree;

	private Scene loginScene;
	private LoginLayout loginLayout;

	//Board Layout
	private Scene 			boardScene;
	private BoardGridPane	boardGridPane;
	private Label 			boardStatusLabel;
	private CellPane		boardStatusCell;

	private Stage primaryStage;

	@Override
	public void start(Stage primaryStage) throws Exception {
		this.primaryStage = primaryStage;
		service = Executors.newCachedThreadPool();
		//Load in fonts for use across the app.
		Font.loadFont(getClass().getResourceAsStream("/Roboto-Regular.ttf"), 100);
		Font.loadFont(getClass().getResourceAsStream("/Roboto-Thin.ttf"), 100);

		player = new ReversiPlayer("Player 1", Color.WHITE, null);
		player.setSessionId(1);

		loginLayout = new LoginLayout();
		loginLayout.getStartGameButton().setOnAction(event -> {
			service.submit(() -> {
				try {
					System.out.println("Send start game");
					ObjectOutputStream dos = player.getOutputStream();
					dos.writeInt(CLIENT_SEND_START_GAME);
					dos.flush();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			});
		});
		loginLayout.getUsernameField().setOnAction(event -> {
			loginLayout.getUsernameField().setDisable(true);
			loginLayout.getIpField().setDisable(true);
			service.submit(() -> {
				Platform.runLater(() -> loginLayout.getStatusLabel().setText("Connecting..." +
						" This may take a while."));
				List<String> sessionNames = connectToServer(loginLayout.getUsernameField().getText());
				if (sessionNames == null) {
					loginLayout.getUsernameField().setDisable(false);
					loginLayout.getIpField().setDisable(false);
					Platform.runLater(() -> loginLayout.getStatusLabel().setText("Something went wrong." +
							" Try again."));
				} else {
					loginLayout.getSessionOptions().addAll(sessionNames);
					loginLayout.disableSessionChosing(false);
					Platform.runLater(() -> loginLayout.getStatusLabel().setText("Choose a session to join" +
							" or create a new one:"));
				}
			});
		});
		loginLayout.setOnSessionChosen(v1 -> {
			loginLayout.disableSessionChosing(true);
			service.submit(() -> {
				sendSessionChoice(v1);
			});
		});
		loginScene = new Scene(loginLayout, 400, 400);

		//Board Scene
		board = new Board(8, 8);
		boardGridPane = new BoardGridPane(board, cell -> {
			if (board.turnStones(cell.getRow(), cell.getColumn(), player.getSessionId(), placeFree)) {
				boardGridPane.setDisable(true);
				boardGridPane.markAllCells(boardGridPane.getAdvisedBorderColor());
				service.submit(() -> {
					try {
						player.getOutputStream().writeInt(CLIENT_SEND_MOVE);
						player.getOutputStream().writeInt(cell.getRow());
						player.getOutputStream().writeInt(cell.getColumn());
						player.getOutputStream().writeBoolean(placeFree);
						player.getOutputStream().flush();
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
			}
		});
		boardStatusCell = new CellPane(0, 0);
		boardStatusCell.setPrefSize(20, 20);
		boardStatusLabel = new Label();
		boardStatusLabel.setFont(Font.font("monospaced"));
		boardGridPane.setDisable(true);

		HBox boardStatusBar = new HBox(10, boardStatusCell, boardStatusLabel);
		boardStatusBar.setAlignment(Pos.CENTER_LEFT);

		BorderPane borderPane = new BorderPane();
		borderPane.setCenter(boardGridPane);
		borderPane.setBottom(boardStatusBar);
		boardScene = new Scene(borderPane, 400, 400);

		primaryStage.setScene(loginScene);
		primaryStage.setTitle("ReversiClient");
		primaryStage.show();
	}

	private List<String> connectToServer(String playerName) {
		try {
			Socket socket = new Socket(loginLayout.getIpAddress(), PORT);

			player = new ReversiPlayer(playerName, socket);

			//Introduce yourself
			//Send player name
			ObjectOutputStream dos = player.getOutputStream();
			dos.writeUTF(playerName);
			dos.flush();

			//Receive available sessions
			ObjectInputStream ois = player.getInputStream();
			return (List<String>) ois.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void sendSessionChoice(String chosenSessionName) {
		try {
			/*if (availableSessions.isEmpty()) {
				chosenSessionName = "Sessie1";
			} else {
				chosenSessionName = availableSessions.get(0);
			}*/

			//Send chosen session
			ObjectOutputStream oos = player.getOutputStream();
			oos.writeInt(CLIENT_SEND_SUCCESS);
			oos.writeUTF(chosenSessionName);
			oos.flush();

			ObjectInputStream ois = player.getInputStream();
			try {
				ReversiSession.requireState(player, null, CLIENT_RECEIVE_SUCCESS);
			} catch (IllegalStateException e) {
				Platform.runLater(() -> {
					loginLayout.getStatusLabel().setText(e.getMessage());
					loginLayout.disableSessionChosing(false);
				});
				return;
			}
			Platform.runLater(() -> loginLayout.getStatusLabel().setText("Joined session. Waiting for start signal..."));

			//Receive the sessionID
			player.setSessionId(ois.readInt());
			System.out.println("Received sessionID: " + player.getSessionId());

			//If we are the first player, we may start the game
			if (player.getSessionId() == 1) {
				loginLayout.getStartGameButton().setDisable(false);
				Platform.runLater(() -> loginLayout.getStatusLabel().setText("Session opened, other players may join." +
						" When ready, click start."));
			}

			//Now, the server will send info about which other players will contest.
			int command;
			while ((command = ReversiSession.requireState(player, null, CLIENT_RECEIVE_PLAYER_ADDED,
					CLIENT_RECEIVE_START_GAME)) != CLIENT_RECEIVE_START_GAME) {
				System.out.println("a Received '" + command + "'");
				if (command == CLIENT_RECEIVE_PLAYER_ADDED) {
					SimplePlayer otherPlayer = (SimplePlayer) ois.readObject();
					if (player.equals(otherPlayer)) {
						System.out.print("Received our color: " + otherPlayer.getColor());
						player.setColor((Color) otherPlayer.getColor());
					} else {
						System.out.print("We'll be playing against " + otherPlayer);
					}
					boardGridPane.addPlayer(otherPlayer);
				}
			}

			//The start-game signal was send
			System.out.println("Read start game signal");
			int playerAmount = ois.readInt();
			System.out.println("Total amount of player in this session: " + playerAmount);

			//Go to board scene
			Platform.runLater(this::goToBoardScene);
			playGame();
		} catch (SocketException | EOFException | IllegalStateException e) {
			Platform.runLater(this::goToLoginScene);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void goToBoardScene() {
		primaryStage.setScene(boardScene);
	}

	private void goToLoginScene() {
		primaryStage.setScene(loginScene);
		loginLayout.reset();
		boardGridPane.getPlayers().clear();
	}

	/**
	 * Gameplay logic. This method returns when the game has finished.
	 */
	private void playGame() {
		board.setupBoard(boardGridPane.getPlayers().size());
		try {
			ObjectInputStream ois = player.getInputStream();
			while (true) {
				int com = ois.readInt();
				if (com == CLIENT_RECEIVE_OTHER_WON) {
					int playerId = ois.readInt();
					showWinner(playerId);
					Platform.runLater(() -> boardStatusLabel.setText("Ahw... '" +
							boardGridPane.getPlayerById(playerId).getName() + "' won. Next time better."));
					break;
				} else if (com == CLIENT_RECEIVE_YOU_WON) {
				    showWinner(player.getSessionId());
					Platform.runLater(() -> boardStatusLabel.setText("You won! Congrats!"));
					break;
				} else if (com == CLIENT_RECEIVE_OTHER_START_MOVE) {
					SimplePlayer p = boardGridPane.getPlayerById(ois.readInt());
					Platform.runLater(() -> {
						boardStatusLabel.setText("Waiting for " + p.getName() + "'s move...");
						boardStatusCell.changePossession(p.getColor(), 200);
					});
				} else if (com == CLIENT_RECEIVE_OTHER_DID_MOVE) {
					int playerId = ois.readInt();
					int row = ois.readInt();
					int column = ois.readInt();
					boolean free = ois.readBoolean();
					board.turnStones(row, column, playerId, free);
				} else if (com == CLIENT_RECEIVE_START_MOVE) {
					Platform.runLater(() -> boardStatusLabel.setText("Marking options..."));
					List<CellPane> options = updatePlacementOptions(player.getSessionId(), false);
					if (options.isEmpty()) {
						options = updatePlacementOptions(player.getSessionId(), true);
						boardGridPane.markCells(options, Color.ORANGE);
						Platform.runLater(() -> boardStatusLabel.setText("Ai! Your whipped of the board." +
								" Place anywhere adjacent, there will be no points."));
						placeFree = true;
					} else {
						boardGridPane.markCells(options, Color.WHITE);
						Platform.runLater(() -> {
							boardStatusLabel.setText("It's your turn");
							boardStatusCell.changePossession(player.getColor(), 200);
						});
						placeFree = false;
					}
					boardGridPane.setDisable(false);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			Platform.runLater(this::goToLoginScene);
		}
		System.out.println("Game ended.");
	}

	@Override
	public void stop() throws Exception {
		super.stop();
		if (player != null) {
			player.getOutputStream().close();
		}
		service.shutdown();
	}

	private List<CellPane> updatePlacementOptions(int playerId, boolean freeMove) {
		List<CellPane> validPlacements = new ArrayList<>();
		for (int row = 0; row < board.getBoardHeight(); row++) {
			for (int column = 0; column < board.getBoardWidth(); column++) {
				if (board.checkValidStonePlacement(row, column, playerId, freeMove))
					validPlacements.add(boardGridPane.getCell(row, column));
			}
		}
		return validPlacements;
	}

	public static void main(String[] args) {
		launch(ClientMain.class, args);
	}

	public void showWinner(int playerID){
        Label wonLabel = new Label();
        wonLabel.setFont(Font.font("Roboto Thin", 100));
        wonLabel.layoutXProperty().bind(boardGridPane.widthProperty().divide(2).subtract(wonLabel
                .widthProperty().divide(2)));
        wonLabel.layoutYProperty().bind(boardGridPane.heightProperty().divide(2).subtract(wonLabel
                .heightProperty().divide(2)));

        Button backButton = new Button("Back");
        backButton.layoutXProperty().bind(boardGridPane.widthProperty().divide(2).subtract(backButton
                .widthProperty().divide(2)));
        backButton.layoutYProperty().bind(wonLabel.layoutYProperty().add(wonLabel.heightProperty()));
//        backButton.setOnAction(event -> {
//            goToLoginScene();
//            boardGridPane.getChildren().removeAll(wonLabel, backButton);
//        });

	    if (player.getSessionId() == playerID){
	        wonLabel.setText("YOU WON!");
	        wonLabel.setTextFill(Color.GREEN);
        } else {
	        wonLabel.setText("YOU LOST!");
	        wonLabel.setTextFill(Color.RED);
        }
        boardGridPane.getChildren().addAll(wonLabel, backButton);
    }

}
