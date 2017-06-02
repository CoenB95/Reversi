package com.cbapps.reversi.board;

/**
 * @author Coen Boelhouwers
 */
public interface OnBoardActivityListener {
	void onCellChanged(int row, int column, int playerId);
}
