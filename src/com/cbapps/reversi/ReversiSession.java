package com.cbapps.reversi;

import com.cbapps.reversi.board.Board;
import com.cbapps.reversi.server.ServerMain;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
	private Lock lock;

	public ReversiSession(String sessionName, int boardWidth, int boardHeight, ExecutorService service) {
		this.board = new Board(boardWidth, boardHeight);
		this.board.setupBoard();
		this.sessionName = sessionName;
		this.players = new ArrayList<>();
		lock = new ReentrantLock();
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

	/**
	 * @deprecated this method somehow doesn't want to work and blocks the application.
	 * Avoid using this method until an other solution is found.
	 * @param player the new player.
	 */
	@Deprecated
	public void notifyOtherPlayers(ReversiPlayer player) {
		lock.lock();
		for (ReversiPlayer p : players) {
			if (!p.equals(player)) {
				try {
					System.out.println("addPlayer - send player addition to " + p + "; getOutput");
					ObjectOutputStream oos = p.getOutputStream();
					//oos.flush();
					System.out.println("addPlayer - writeInt");
					oos.writeInt(SERVER_SEND_PLAYER_ADDED);
					System.out.println("addPlayer - flush");
					//oos.flush();
					System.out.println("addplayer - writeObject");
					oos.writeObject(player);
					System.out.println("addPlayer - flush");
					//oos.flush();
					System.out.println("addPlayer - send.");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		lock.unlock();
	}

	public ReversiSession setSessionNr(int nr) {
		this.sessionNr = nr;
		return this;
	}

	public void startGame() {
		ServerMain.log("[" + sessionName + "] Started game");
		service.submit(this);
	}

	public void startSession() {
		if (setup) return;
		setup = true;
		if (players.isEmpty()) throw new IllegalStateException("Can't start session without any players");
		this.service = service;
		this.service.submit(() -> {
			try {
				ObjectInputStream dis = players.get(0).getInputStream();
				ServerMain.log("[" + sessionName + "] Player '" + players.get(0).getName() +
						"' can start the game.\n");
				int command = dis.readInt();
				System.out.println("command=" + command);
				System.out.println("Yes! Let's start this session!");
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
			while (!service.isShutdown() && !board.isBoardFull()) {
				ReversiPlayer currentPlayer = players.get(activePlayer);
				System.out.println("It's player " + currentPlayer + "'s turn.");
				currentPlayer.getOutputStream().writeInt(SERVER_SEND_START_MOVE);
				currentPlayer.getOutputStream().flush();
				checkState(currentPlayer, SERVER_RECEIVE_MOVE);
				int row = currentPlayer.getInputStream().readInt();
				int column = currentPlayer.getInputStream().readInt();

				if (board.changeAllValidCells(row, column, currentPlayer.getSessionId())) {
					System.out.println("Player did valid move.");
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
					System.out.println("Wrong move from player " + currentPlayer);
				}
			}
			System.out.println("Board is full. Determining winner...");
			int highestScore = 0;
			ReversiPlayer winner = null;
			for (ReversiPlayer p : players) {
				int score = board.getScoreOfPlayer(p.getSessionId());
				if (winner == null || score > highestScore) {
					winner = p;
					highestScore = score;
				}
			}
			System.out.println(winner + " won!");
			for (ReversiPlayer p : players) {
				if (p.equals(winner)) {
					p.getOutputStream().writeInt(SERVER_SEND_YOU_WON);
				} else {
					p.getOutputStream().writeInt(SERVER_SEND_OTHER_WON);
					p.getOutputStream().writeInt(p.getSessionId());
				}
				p.getOutputStream().flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void throwIllegalStateException(String message) throws IOException, IllegalStateException {
		for (ReversiPlayer p : players) {
			p.getOutputStream().writeInt(SERVER_SEND_ERROR);
			p.getOutputStream().writeUTF(message);
		}
		throw new IllegalStateException(message);
	}
}
