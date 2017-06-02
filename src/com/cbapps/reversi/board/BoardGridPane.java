package com.cbapps.reversi.board;

import com.cbapps.reversi.ReversiConstants;
import com.cbapps.reversi.client.CellPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

/**
 * @author Coen Boelhouwers
 */
public class BoardGridPane extends GridPane implements ReversiConstants {

	private CellPane[][] cells;

	public BoardGridPane(Board board, OnCellClickedListener listener) {
		super();
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
			cells[row][column].changePossession(playerId == 1 ? Color.BLACK : Color.WHITE);
		});
		board.setupBoard();
		setBackground(new Background(new BackgroundFill(Color.GREEN, null, null)));
	}

	public interface OnCellClickedListener {
		void onCellClicked(CellPane cellPane);
	}
}
