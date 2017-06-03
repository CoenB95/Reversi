package com.cbapps.reversi.board;

import com.cbapps.reversi.SimplePlayer;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * @author Coen Boelhouwers
 */
public class Board {
    public static final int EMPTY_CELL = 0;

    private int[][] board;
    private int boardWidth;
    private int boardHeight;
    private OnBoardActivityListener listener;


    public Board(int rows, int columns) {
        this.board = new int[rows][columns];
        this.boardWidth = columns;
        this.boardHeight = rows;
    }

	/**
	 * Changes the possession of the cell at the specified coordinate and notifies listeners of this change.
	 *
	 * @param row the row index.
	 * @param column the column index.
	 * @param playerId the new cell owner's id.
	 */
	public void changeCell(int row, int column, int playerId) {
		board[row][column] = playerId;
		if (listener != null) listener.onCellChanged(row, column, playerId);
	}

	/**
	 * Checks if placing a stone of the given id on the passed coordinate would be a valid move.
	 * This method is a simplified version of {@link #turnStones(int, int, int)} that doesn't turn
	 * the stones and returns immediately if either direction would work, making it more efficient.
	 *
	 * @param row the row coordinate of the placed stone.
	 * @param column the column coordinate of the placed stone.
	 * @param playerId the player's id of the placed stone.
	 * @return true if the move is valid, false otherwise.
	 */
	public boolean checkValidStonePlacement(int row, int column, int playerId) {
		if (!isCellEmpty(row, column)) return false;
		else if (turnStonesFix(row, column, Direction.NORTH, playerId, false) > 0) return true;
		else if (turnStonesFix(row, column, Direction.NORTH_EAST, playerId, false) > 0) return true;
		else if (turnStonesFix(row, column, Direction.EAST, playerId, false) > 0) return true;
		else if (turnStonesFix(row, column, Direction.SOUTH_EAST, playerId, false) > 0) return true;
		else if (turnStonesFix(row, column, Direction.SOUTH, playerId, false) > 0) return true;
		else if (turnStonesFix(row, column, Direction.SOUTH_WEST, playerId, false) > 0) return true;
		else if (turnStonesFix(row, column, Direction.WEST, playerId, false) > 0) return true;
		else if (turnStonesFix(row, column, Direction.NORTH_WEST, playerId, false) > 0) return true;
		return false;
	}

	public int getBoardHeight() {
		return boardHeight;
	}

	public int getBoardWidth() {
		return boardWidth;
	}

	public int getScoreOfPlayer(int playerID) {
		int PlayerScore = 0;
		for (int i = 0; i < board.length; i++) {
			for (int j = 0; j < board[0].length; j++) {
				if (board[i][j] == playerID) {
					PlayerScore++;
				}
			}
		}
		return PlayerScore;
	}

	/**
	 * Checks the board to see if there are no empty cells left.
	 * @return true if the complete board is full, false otherwise.
	 */
	public boolean isBoardFull() {
		for (int i = 0; i < boardWidth; i++) {
			for (int j = 0; j < boardHeight; j++) {
				if (board[i][j] == EMPTY_CELL) return false;
			}
		}
		return true;
	}

    /**
	 * Checks if the cell at the specified coordinate is empty.
	 *
     * @param column the column index.
     * @param row    the row index.
     * @return true if this cell is empty, false otherwise.
	 * @throws IndexOutOfBoundsException if the index falls outside the board's size.
     */
    public boolean isCellEmpty(int row, int column) {
        return board[row][column] == EMPTY_CELL;
    }

    public void setOnCellChangedListener(OnBoardActivityListener l) {
        listener = l;
    }

	/**
	 * Setups the board, effectively placing four start-stones.
	 * Note that this method does NOT clear the board.
	 */
	public void setupBoard() {
		int centerLeft = Math.floorDiv(boardWidth, 2);
		int centerTop = Math.floorDiv(boardHeight, 2);
		//Setup start field
		changeCell(centerLeft, centerTop, 1);
		changeCell(centerLeft + 1, centerTop, 2);
		changeCell(centerLeft, centerTop + 1, 2);
		changeCell(centerLeft + 1, centerTop + 1, 1);
	}

	/**
	 * Sets the colors of the passed players based on the player amount, such as
	 * black and white for 2 players or 4 distinct colors for 3-4 players.
	 *
	 * @param players the players that will compete each other and need coloring.
	 */
    public static void setupPlayerColors(List<? extends SimplePlayer> players) {
        if (players.size() == 2) {
            players.get(0).setColor(Color.BLACK);
            players.get(1).setColor(Color.WHITE);
        }
    }

	/**
	 * Turns the stones in all directions that are valid moves (animating from end to start) and
	 * places a stone on the base coordinate if more than zero stones got turned.
	 * Otherwise, the move was invalid and therefore the stone will not be placed.
	 *
	 * @param row the row coordinate of the placed stone.
	 * @param column the column coordinate of the placed stone.
	 * @param playerId the player's id of the placed stone.
	 * @return true if the move is valid and the stones have turned, false otherwise.
	 */
    public boolean turnStones(int row, int column, int playerId) {
        System.out.println("Move on [" + row + "," + column + "], checking cells...");
        if (!isCellEmpty(row, column)) {
        	System.out.println("Cell is already possessed. Move not allowed.");
        	return false;
		}
        int otherCellCount = 0;
        otherCellCount += turnStonesFix(row, column, Direction.NORTH, playerId, true);
		otherCellCount += turnStonesFix(row, column, Direction.NORTH_EAST, playerId, true);
		otherCellCount += turnStonesFix(row, column, Direction.EAST, playerId, true);
		otherCellCount += turnStonesFix(row, column, Direction.SOUTH_EAST, playerId, true);
		otherCellCount += turnStonesFix(row, column, Direction.SOUTH, playerId, true);
		otherCellCount += turnStonesFix(row, column, Direction.SOUTH_WEST, playerId, true);
		otherCellCount += turnStonesFix(row, column, Direction.WEST, playerId, true);
		otherCellCount += turnStonesFix(row, column, Direction.NORTH_WEST, playerId, true);

        if (otherCellCount > 0) {
            System.out.println(otherCellCount + " stones have turned. Valid move. Turn start stone.");
            changeCell(row, column, playerId);
            return true;
        } else {
			System.out.println("No stones have turned. Move not allowed.");
			return false;
		}
    }

    //Extra method handling -1 to 0, so turnStones() can count the result.
	private int turnStonesFix(int row, int column, Direction direction, int playerId, boolean turnStones) {
    	int change = turnStoneRecursive(row + direction.getRowChange(), column +
						direction.getColumnChange(), direction.getRowChange(), direction.getColumnChange(),
				playerId, turnStones);
    	return change >= 0 ? change : 0;
	}

    private int turnStoneRecursive(int row, int column, int rowChange, int columnChange, int playerId, boolean turnStones) {
        //If we hit the board's bounds, this move is invalid.
        if (row < 0 || row >= board.length) {
            return -1;
        }
        if (column < 0 || column >= board[0].length) {
            return -1;
        }
        //If we encounter a empty cell, this move is invalid.
        if (board[row][column] == EMPTY_CELL) {
            return -1;
        }
        //If we encounter a stone of our own, the move is valid and the stones in between may be turned (if any).
        if (board[row][column] == playerId) {
            return 0;
        }
        //The decision if this cell may be turned is not made and therefore depends on the next cell.
        //If cells ahead are allowed to turn, than this one may too.
        int next = turnStoneRecursive(row + rowChange, column + columnChange, rowChange, columnChange,
				playerId, turnStones);
        if (next >= 0) {
        	//Turning of stones is allowed.
            if (turnStones) {
				System.out.println("  Turning stone at [" + row + "," + column + "].");
				changeCell(row, column, playerId);
			}
            return next + 1;
        }
        return -1;
    }

    private enum Direction {
    	NORTH(-1, 0),
		NORTH_EAST(-1, 1),
		EAST(0, 1),
		SOUTH_EAST(1, 1),
    	SOUTH(1, 0),
		SOUTH_WEST(1, -1),
    	WEST(0, -1),
		NORTH_WEST(-1, -1);

    	private int rowChange;
    	private int columnChange;

		Direction(int rowChange, int columnChange) {
			this.rowChange = rowChange;
			this.columnChange = columnChange;
		}

		public int getColumnChange() {
			return columnChange;
		}

		public int getRowChange() {
			return rowChange;
		}
	}
}
