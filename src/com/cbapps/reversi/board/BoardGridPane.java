package com.cbapps.reversi.board;

import com.cbapps.reversi.ReversiConstants;
import com.cbapps.reversi.ReversiPlayer;
import com.cbapps.reversi.client.CellPane;
import javafx.application.Platform;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.io.IOException;

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
	}

	public interface OnCellClickedListener {
		void onCellClicked(CellPane cellPane);
	}
}
