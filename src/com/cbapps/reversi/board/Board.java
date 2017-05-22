package com.cbapps.reversi.board;

/**
 * @author Coen Boelhouwers
 */
public class Board {
	public static final int EMPTY_CELL = 0;

	private int[][] board;
	private OnCellChangedListener listener;

	public Board(int columns, int rows) {
		this.board = new int[columns][rows];
		int centerLeft = Math.floorDiv(columns, 2);
		int centerTop = Math.floorDiv(rows, 2);
		//Setup start field
		changeCell(centerLeft, centerTop, 1);
		changeCell(centerLeft + 1, centerTop, 2);
		changeCell(centerLeft, centerTop + 1, 2);
		changeCell(centerLeft + 1, centerTop + 1, 1);
	}


	public boolean canPlace(int column, int row) {
		if (isCellEmpty(column, row)) {
			return true;
		} else return false;
	}

	/**
	 * @param column the column index.
	 * @param row the row index.
	 * @return true if this cell is empty, false otherwise.
	 */
	public boolean isCellEmpty(int column, int row) {
		return board[row][column] == EMPTY_CELL;
	}

	public void changeCell(int column, int row, int playerId) {
		board[column][row] = playerId;
		if (listener != null) listener.onCellChanged(column, row, playerId);
	}

	public void setOnCellChangedListener(OnCellChangedListener l) {
		listener = l;
	}
}
