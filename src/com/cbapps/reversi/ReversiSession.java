package com.cbapps.reversi;

import com.cbapps.reversi.board.Board;
import com.cbapps.reversi.server.ServerMain;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
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
	private boolean opened;
	private int activePlayer = 0;
	private int unableToMoveCount = 0;
	private boolean freeMoveAllowed = true;
	private List<ReversiPlayer> players;
	private boolean setup = false;

	public ReversiSession(String sessionName, int boardWidth, int boardHeight, ExecutorService service) {
		this.board = new Board(boardWidth, boardHeight);
		this.sessionName = sessionName;
		this.players = new ArrayList<>();
		this.service = service;
		this.opened = true;
	}

	public int addPlayer(ReversiPlayer player) {
		if (!opened) throw new IllegalStateException("Try to add players while session is closed");
		int id = players.size()+1;
		players.add(player);
		return id;
	}

	public List<ReversiPlayer> getPlayers() {
		return players;
	}

	public void open(boolean value) {
		opened = value;
	}

	public boolean isOpened() {
		return opened;
	}

	public String getSessionName() {
		return sessionName;
	}

	private void notifyOtherPlayers(ReversiPlayer player) {
		for (ReversiPlayer p : players) {
			try {
				if (p.equals(player)) System.out.println("Notify '" + p.getName() + "' of itself (for color)");
				else System.out.println("Notify '" + p.getName() + "' of '" + player.getName() + "'");
				ObjectOutputStream oos = p.getOutputStream();
				oos.writeInt(SERVER_SEND_PLAYER_ADDED);
				oos.writeObject(player);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
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
				requireState(players.get(0), players, SERVER_RECEIVE_START_GAME);
				opened = false;
				ServerMain.log(sessionName, "Received start signal. Setup colors.");
				board.setupBoard(players.size());
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
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	private boolean checkValidMovesPossibleForPlayer(int playerId, boolean freeMove) {
		for (int r = 0; r < board.getBoardHeight(); r++) {
			for (int col = 0; col < board.getBoardWidth(); col++) {
				if (board.checkValidStonePlacement(r, col, playerId, freeMove)) return true;
			}
		}
		return false;
	}

	public static int requireState(ReversiPlayer player, Collection<ReversiPlayer> allPlayers, int... expected)
			throws IOException, IllegalStateException {
		try {
			int value = player.getInputStream().readInt();
			for (int accept : expected) {
				if (value == accept) return value;
			}
			String exception;
			if (value == SERVER_RECEIVE_ERROR) {
				exception = "Client-side error: " + player.getInputStream().readUTF();
				throwIllegalStateException(allPlayers, exception);
			} else if (value == CLIENT_RECEIVE_ERROR) {
				exception = "Server-side error: " + player.getInputStream().readUTF();
				throw new IllegalStateException(exception);
			} else throw new IllegalStateException("Unexpected " + value + " received");
			return -1;
		} catch (IOException e) {
			throwIllegalStateException(allPlayers, "Connection lost");
			return -1;
		}
	}

	@Override
	public void run() {
		try {
			ServerMain.log(sessionName, "Game started");
			freeMoveAllowed = players.size() >= 3;
			ServerMain.log(sessionName, "Free move allowed in case you'r stuck: " + freeMoveAllowed);
			while (!service.isShutdown() && !board.isBoardFull() && unableToMoveCount <= players.size()) {
				ReversiPlayer currentPlayer = players.get(activePlayer);
				ServerMain.log(sessionName, "It's " + currentPlayer.getName() + "'s turn.");
				if (!checkValidMovesPossibleForPlayer(currentPlayer.getSessionId(), false)) {
					if (!freeMoveAllowed || !checkValidMovesPossibleForPlayer(currentPlayer.getSessionId(),
							true)) {
						unableToMoveCount++;
						if (unableToMoveCount > players.size()) {
							ServerMain.log(sessionName, "None of the players can move. Game is at end.");
						} else {
							ServerMain.log(sessionName, currentPlayer.getName() +
									" can't make any valid move. Skip.");
							activePlayer = (activePlayer + 1) % players.size();
						}
						continue;
					}
					ServerMain.log(sessionName, currentPlayer.getName() + " can't make any valid move. " +
							"A free move is allowed to get back in the game.");
				}
				currentPlayer.getOutputStream().writeInt(SERVER_SEND_START_MOVE);
				currentPlayer.getOutputStream().flush();
				for (ReversiPlayer p : players) {
					if (!p.equals(currentPlayer)) {
						p.getOutputStream().writeInt(SERVER_SEND_OTHER_START_MOVE);
						p.getOutputStream().writeInt(currentPlayer.getSessionId());
						p.getOutputStream().flush();
					}
				}
				requireState(currentPlayer, players, SERVER_RECEIVE_MOVE);
				int row = currentPlayer.getInputStream().readInt();
				int column = currentPlayer.getInputStream().readInt();
				boolean free = currentPlayer.getInputStream().readBoolean();

				if (board.turnStones(row, column, currentPlayer.getSessionId(), free)) {
					ServerMain.log(sessionName, "Player did valid move.");
					for (ReversiPlayer pl : players) {
						if (!pl.equals(currentPlayer)) {
							pl.getOutputStream().writeInt(SERVER_SEND_OTHER_DID_MOVE);
							pl.getOutputStream().writeInt(currentPlayer.getSessionId());
							pl.getOutputStream().writeInt(row);
							pl.getOutputStream().writeInt(column);
							pl.getOutputStream().writeBoolean(free);
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
			try {
				throwIllegalStateException(players, "Connection lost");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} catch (IllegalStateException e) {
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

	public static void throwIllegalStateException(Collection<ReversiPlayer> players, String message)
			throws IOException, IllegalStateException {
		if (players != null) {
			for (ReversiPlayer p : players) {
				p.getOutputStream().close();
			}
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
