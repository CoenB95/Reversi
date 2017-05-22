package com.cbapps.reversi;

import javafx.application.Platform;
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
	private String sessionName;
	private int sessionNr;
	private TextInputControl logArea;
	private int activePlayer = 0;
	private List<ReversiPlayer> players;

	public ReversiSession(String sessionName, int boardWidth, int boardHeight, TextInputControl logArea) {
		this.sessionName = sessionName;
		board = new int[boardWidth][boardHeight];
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

	@Override
	public void run() {
		while (true) {
			try {
				String s = players.get(activePlayer).getMessage();
				Platform.runLater(() -> logArea.appendText("Session " + sessionNr + "|ReversiPlayer " + activePlayer +
						": " + s + "\n"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
