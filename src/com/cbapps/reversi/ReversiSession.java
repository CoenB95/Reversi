package com.cbapps.reversi;

import com.cbapps.reversi.board.Board;
import com.cbapps.reversi.server.ServerMain;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author Coen Boelhouwers
 */
public class ReversiSession implements Runnable, ReversiConstants {

	private Board board;
	private ExecutorService service;
	private String sessionName;
	private int sessionNr;
	private int activePlayer = 0;
	private List<ReversiPlayer> players;
	private boolean setup = false;

	public ReversiSession(String sessionName, int boardWidth, int boardHeight, ExecutorService service) {
		this.board = new Board(boardWidth, boardHeight);
		this.board.setupBoard();
		this.sessionName = sessionName;
		this.players = new ArrayList<>();
		this.service = service;
	}

	public int addPlayer(ReversiPlayer player){
		int id = players.size()+1;
		players.add(player);
		return id;
	}

	public String getSessionName() {
		return sessionName;
	}

	private void notifyOtherPlayers(ReversiPlayer player) {
		for (ReversiPlayer p : players) {
			if (!p.equals(player)) {
				try {
					System.out.println("Notify '" + p.getName() + "' of '" + player.getName() + "'");
					ObjectOutputStream oos = p.getOutputStream();
					oos.writeInt(SERVER_SEND_PLAYER_ADDED);
					oos.writeObject(player);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public ReversiSession setSessionNr(int nr) {
		this.sessionNr = nr;
		return this;
	}

	public void startGame() {
		service.submit(this);
	}

	public void startSession() {
		if (setup) return;
		setup = true;
		if (players.isEmpty()) throw new IllegalStateException("Can't start session without any players");
		this.service.submit(() -> {
			try {
				ObjectInputStream dis = players.get(0).getInputStream();
				ServerMain.log(sessionName, "Player '" + players.get(0).getName() +
						"' can start the game.");
				int command = dis.readInt();
				if (command == SERVER_RECEIVE_START_GAME) {
					ServerMain.log(sessionName, "Received start signal. Setup colors.");
					Board.setupPlayerColors(players);
					ServerMain.log(sessionName, "Notifying players of each other...");
					for (ReversiPlayer p : players) notifyOtherPlayers(p);
					ServerMain.log(sessionName, "Done. Broadcast start signal.");
					for (ReversiPlayer p : players) {
						ObjectOutputStream dos = p.getOutputStream();
						dos.writeInt(SERVER_SEND_START_GAME);
						dos.writeInt(players.size());
						dos.flush();
					}
					startGame();
				} else {
					ServerMain.log(sessionName, "Unexpected command '" + command + "' received.");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	private void checkState(ReversiPlayer player, int expected)
			throws IOException, IllegalStateException {
		int value = player.getInputStream().readInt();
		if (value != expected) {
			String exception;
			if (value == SERVER_RECEIVE_ERROR) exception = "Client-side error: " +
					player.getInputStream().readUTF();
			else exception = "Expected " + expected + " but received " + value;
			throwIllegalStateException(exception);
		}
	}

	@Override
	public void run() {
		try {
			ServerMain.log(sessionName, "Game started");
			while (!service.isShutdown() && !board.isBoardFull()) {
				ReversiPlayer currentPlayer = players.get(activePlayer);
				ServerMain.log(sessionName, "It's " + currentPlayer.getName() + "'s turn.");
				currentPlayer.getOutputStream().writeInt(SERVER_SEND_START_MOVE);
				currentPlayer.getOutputStream().flush();
				for (ReversiPlayer p : players) {
					if (!p.equals(currentPlayer)) {
						p.getOutputStream().writeInt(SERVER_SEND_OTHER_START_MOVE);
						p.getOutputStream().writeInt(currentPlayer.getSessionId());
						p.getOutputStream().flush();
					}
				}
				checkState(currentPlayer, SERVER_RECEIVE_MOVE);
				int row = currentPlayer.getInputStream().readInt();
				int column = currentPlayer.getInputStream().readInt();

				if (board.checkStoneTurns(row, column, currentPlayer.getSessionId(), true)) {
					ServerMain.log(sessionName, "Player did valid move.");
					for (ReversiPlayer pl : players) {
						if (!pl.equals(currentPlayer)) {
							pl.getOutputStream().writeInt(SERVER_SEND_OTHER_DID_MOVE);
							pl.getOutputStream().writeInt(currentPlayer.getSessionId());
							pl.getOutputStream().writeInt(row);
							pl.getOutputStream().writeInt(column);
							pl.getOutputStream().flush();
						}
					}
					activePlayer = (activePlayer + 1) % players.size();
				} else {
					ServerMain.log(sessionName, "Wrong move from player " + currentPlayer +
							". That should NOT have happened!");
				}
			}
			ServerMain.log(sessionName, "Board is full. Determining winner...");
			ReversiPlayer winner = updateScores();
			logScores();
			ServerMain.log(sessionName, winner + " won!");
			for (ReversiPlayer p : players) {
				if (p.equals(winner)) {
					p.getOutputStream().writeInt(SERVER_SEND_YOU_WON);
				} else {
					p.getOutputStream().writeInt(SERVER_SEND_OTHER_WON);
					p.getOutputStream().writeInt(winner.getSessionId());
				}
				p.getOutputStream().flush();
			}
			ServerMain.log(sessionName, "Game ended normally");
		} catch (IOException e) {
			e.printStackTrace();
			ServerMain.log(sessionName, "An error happened. Gameplay is disrupted");
		}
	}

	private void logScores() {
		ServerMain.log(sessionName, "Scores:");
		players.stream().sorted(Comparator.comparingInt(SimplePlayer::getScore).reversed())
				.forEach(p -> {
					p.setScore(board.getScoreOfPlayer(p.getSessionId()));
					ServerMain.log(sessionName, String.format("%-30s %d", p.getName(), p.getScore()));
				});
	}

	private void throwIllegalStateException(String message) throws IOException, IllegalStateException {
		for (ReversiPlayer p : players) {
			p.getOutputStream().writeInt(SERVER_SEND_ERROR);
			p.getOutputStream().writeUTF(message);
		}
		throw new IllegalStateException(message);
	}

	private ReversiPlayer updateScores() {
		ReversiPlayer topScore = null;
		for (ReversiPlayer p : players) {
			p.setScore(board.getScoreOfPlayer(p.getSessionId()));
			if (topScore == null || p.getScore() > topScore.getScore())
				topScore = p;
		}
		return topScore;
	}
}
