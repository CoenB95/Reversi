package com.cbapps.reversi.board;

/**
 * @author Coen Boelhouwers
 */
public interface OnCellChangedListener {
	void onCellChanged(int row, int column, int playerId);
}
