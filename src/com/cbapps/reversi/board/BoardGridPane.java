package com.cbapps.reversi.board;

import com.cbapps.reversi.ReversiConstants;
import com.cbapps.reversi.SimplePlayer;
import com.cbapps.reversi.client.CellPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Coen Boelhouwers
 */
public class BoardGridPane extends GridPane implements ReversiConstants {

	private Board board;
	private List<SimplePlayer> otherPlayers;
	private CellPane[][] cells;

	public BoardGridPane(Board board, OnCellClickedListener listener) {
		super();
		this.board = board;
		this.otherPlayers = new ArrayList<>();
		cells = new CellPane[board.getBoardWidth()][board.getBoardHeight()];
		for (int i = 0; i < board.getBoardWidth(); i++) {
			for (int j = 0; j < board.getBoardHeight(); j++) {
				CellPane c = new CellPane(i, j);
				c.setOnMouseClicked(event -> {
					if (listener != null) listener.onCellClicked(c);
				});
				cells[i][j] = c;
				add(c, j, i);
			}
		}
		board.setOnCellChangedListener((row, column, playerId) -> {
			cells[row][column].changePossession(getPlayerById(playerId).getColor());
		});
		setBackground(new Background(new BackgroundFill(Color.GREEN, null, null)));
	}

	public void addPlayer(SimplePlayer player) {
		otherPlayers.add(player);
		//In case of multicolor use grey background.
		if (otherPlayers.size() > 2) {
			setBackground(new Background(new BackgroundFill(Color.GRAY, null, null)));
			for (int i = 0; i < board.getBoardWidth(); i++) {
				for (int j = 0; j < board.getBoardHeight(); j++) {
					cells[i][j].setBorderColor(getAdvisedBorderColor());
				}
			}
		}
	}

	public Color getAdvisedBorderColor() {
		return otherPlayers.size() > 2 ? Color.DIMGRAY : Color.DARKGREEN;
	}

	public CellPane getCell(int row, int column) {
		return cells[row][column];
	}

	public SimplePlayer getPlayerById(int playerId) {
		return otherPlayers.stream().filter(p -> p.getSessionId() == playerId).findFirst().orElse(null);
	}

	public List<SimplePlayer> getPlayers() {
		return otherPlayers;
	}

	public void markAllCells(Color color) {
		for (int i = 0; i < board.getBoardWidth(); i++) {
			for (int j = 0; j < board.getBoardHeight(); j++) {
				cells[i][j].setBorderColor(color);
			}
		}
	}
	public void markCells(Collection<CellPane> cells, Color color) {
		for (CellPane c : cells) c.setBorderColor(color);
	}

	public interface OnCellClickedListener {
		void onCellClicked(CellPane cellPane);
	}
}
