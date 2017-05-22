package com.cbapps.reversi;

import com.cbapps.reversi.board.Board;
import javafx.scene.control.TextInputControl;

import java.io.IOException;
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
	private TextInputControl logArea;
	private int activePlayer = 0;
	private List<ReversiPlayer> players;

	public ReversiSession(String sessionName, int boardWidth, int boardHeight, TextInputControl logArea) {
		this.board = new Board(boardWidth, boardHeight);
		this.sessionName = sessionName;
		this.logArea = logArea;
		this.players = new ArrayList<>();
	}

	public void addPlayer(ReversiPlayer player){
		player.setSessionId(players.size()+1);
		players.add(player);
	}

	public String getSessionName() {
		return sessionName;
	}

	public ReversiSession setSessionNr(int nr) {
		this.sessionNr = nr;
		return this;
	}

	public void begin(ExecutorService service) {
		this.service = service;
		service.submit(this);
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
