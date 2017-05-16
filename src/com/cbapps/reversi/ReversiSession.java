package com.cbapps.reversi;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Coen Boelhouwers
 */
public class ReversiSession implements Runnable {

	public static final int EMPTY_FIELD = 0;

	private int[][] board;
	private int sessionNr;
	private TextInputControl logArea;
	private int activePlayer = 0;
	private List<Player> players;

	public ReversiSession(int boardWidth, int boardHeight, Collection<Player> players,
						  TextInputControl logArea) {
		board = new int[boardWidth][boardHeight];
		this.logArea = logArea;
		this.players = new ArrayList<>();
		this.players.addAll(players);
	}

	public ReversiSession setSessionNr(int nr) {
		this.sessionNr = nr;
		return this;
	}

	@Override
	public void run() {
		while (true) {
			try {
				String s = players.get(activePlayer).getMessage();
				Platform.runLater(() -> logArea.appendText("Session " + sessionNr + "|Player " + activePlayer +
						": " + s + "\n"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
