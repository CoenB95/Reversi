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

	public ReversiSession(String sessionName, int boardWidth, int boardHeight) {
		this.board = new Board(boardWidth, boardHeight, null);
		this.sessionName = sessionName;
		this.players = new ArrayList<>();
	}

	public int addPlayer(ReversiPlayer player){
		int id = players.size()+1;
		players.forEach(p -> {
			try {
				ObjectOutputStream oos = p.getOutputStream();
				oos.writeInt(SERVER_SEND_PLAYER_ADDED);
				oos.writeObject(player);
				oos.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		players.add(player);
		return id;
	}

	public String getSessionName() {
		return sessionName;
	}

	public ReversiSession setSessionNr(int nr) {
		this.sessionNr = nr;
		return this;
	}

	public void startGame() {
		ServerMain.log("[" + sessionName + "] Started game");
		//service.submit(this);
	}

	public void startSession(ExecutorService service) {
		if (setup) return;
		setup = true;
		if (players.isEmpty()) throw new IllegalStateException("Can't start session without any players");
		this.service = service;
		this.service.submit(() -> {
			try {
				ObjectInputStream dis = players.get(0).getInputStream();
				ServerMain.log("[" + sessionName + "] Player '" + players.get(0).getName() +
						"' can start the game.\n");
				while (dis.readInt() != SERVER_RECEIVE_START_GAME) {
					Thread.yield();
				}
				System.out.println("Yes! Let's start this session!");
				for (ReversiPlayer p : players) {
					ObjectOutputStream dos = p.getOutputStream();
					dos.writeInt(SERVER_SEND_START_GAME);
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
		while (!service.isShutdown()) {
			try {
				ReversiPlayer currentPlayer = players.get(activePlayer);
				currentPlayer.getOutputStream().writeInt(SERVER_SEND_START_MOVE);
				checkState(currentPlayer, SERVER_RECEIVE_MOVE);
				int column = currentPlayer.getInputStream().readInt();
				int row = currentPlayer.getInputStream().readInt();

				if (board.canPlace(column, row)) {

				} else throwIllegalStateException("");
			} catch (IOException e) {
				e.printStackTrace();
			}
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
