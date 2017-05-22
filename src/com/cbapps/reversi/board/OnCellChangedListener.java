package com.cbapps.reversi.board;

/**
 * @author Coen Boelhouwers
 */
public interface OnCellChangedListener {
	void onCellChanged(int column, int row, int playerId);
}
