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

    public boolean canPlace(int column, int row) {
        if (isCellEmpty(column, row)) {
            return true;
        } else return false;
    }

    /**
     * @param column the column index.
     * @param row    the row index.
     * @return true if this cell is empty, false otherwise.
     */
    public boolean isCellEmpty(int row, int column) {
        return board[row][column] == EMPTY_CELL;
    }

    public void changeCell(int row, int column, int playerId) {
        board[row][column] = playerId;
        if (listener != null) listener.onCellChanged(row, column, playerId);
    }

    public void setOnCellChangedListener(OnBoardActivityListener l) {
        listener = l;
    }

    public void setupBoard() {
		int centerLeft = Math.floorDiv(boardWidth, 2);
		int centerTop = Math.floorDiv(boardHeight, 2);
		//Setup start field
		changeCell(centerLeft, centerTop, 1);
		changeCell(centerLeft + 1, centerTop, 2);
		changeCell(centerLeft, centerTop + 1, 2);
		changeCell(centerLeft + 1, centerTop + 1, 1);
	}

    public static void setupPlayerColors(List<? extends SimplePlayer> players) {
        if (players.size() == 2) {
            players.get(0).setColor(Color.BLACK);
            players.get(1).setColor(Color.WHITE);
        }
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

    public boolean isBoardFull() {
    	for (int i = 0; i < boardWidth; i++) {
			for (int j = 0; j < boardHeight; j++) {
				if (board[i][j] == EMPTY_CELL) return false;
			}
		}
		return true;
	}

	public boolean checkValidStonePlacement(int row, int column, int playerId) {
    	if (!isCellEmpty(row, column)) return false;
		else if (checkLineairStoneTurns(row, column, Direction.NORTH, playerId, false) > 0) return true;
		else if (checkLineairStoneTurns(row, column, Direction.NORTH_EAST, playerId, false) > 0) return true;
		else if (checkLineairStoneTurns(row, column, Direction.EAST, playerId, false) > 0) return true;
		else if (checkLineairStoneTurns(row, column, Direction.SOUTH_EAST, playerId, false) > 0) return true;
		else if (checkLineairStoneTurns(row, column, Direction.SOUTH, playerId, false) > 0) return true;
		else if (checkLineairStoneTurns(row, column, Direction.SOUTH_WEST, playerId, false) > 0) return true;
		else if (checkLineairStoneTurns(row, column, Direction.WEST, playerId, false) > 0) return true;
		else if (checkLineairStoneTurns(row, column, Direction.NORTH_WEST, playerId, false) > 0) return true;
		return false;
	}

    public boolean checkStoneTurns(int row, int column, int playerId, boolean turnStones) {
        System.out.println("Move on [" + row + "," + column + "], checking cells...");
        if (!isCellEmpty(row, column)) {
        	System.out.println("Cell is already possessed. Move not allowed.");
        	return false;
		}
        int otherCellCount = 0;
        otherCellCount += checkLineairStoneTurns(row, column, Direction.NORTH, playerId, turnStones);
		otherCellCount += checkLineairStoneTurns(row, column, Direction.NORTH_EAST, playerId, turnStones);
		otherCellCount += checkLineairStoneTurns(row, column, Direction.EAST, playerId, turnStones);
		otherCellCount += checkLineairStoneTurns(row, column, Direction.SOUTH_EAST, playerId, turnStones);
		otherCellCount += checkLineairStoneTurns(row, column, Direction.SOUTH, playerId, turnStones);
		otherCellCount += checkLineairStoneTurns(row, column, Direction.SOUTH_WEST, playerId, turnStones);
		otherCellCount += checkLineairStoneTurns(row, column, Direction.WEST, playerId, turnStones);
		otherCellCount += checkLineairStoneTurns(row, column, Direction.NORTH_WEST, playerId, turnStones);

        if (otherCellCount > 0) {
            System.out.println(otherCellCount + " stones have turned. Valid move. Turn start stone.");
            changeCell(row, column, playerId);
            return true;
        } else {
			System.out.println("No stones have turned. Move not allowed.");
			return false;
		}
    }

    //Bugfix method.
	private int checkLineairStoneTurns(int row, int column, Direction direction, int playerId, boolean turnStones) {
    	int change = turnStoneRecursive(row + direction.getRowChange(), column +
						direction.getColumnChange(), direction.getRowChange(), direction.getColumnChange(),
				playerId, turnStones);
    	return change >= 0 ? change : 0;
	}

    private int turnStoneRecursive(int row, int column, int rowChange, int columnChange, int playerId, boolean turnStones) {
        //Bounds check; return false on hit
        if (row < 0 || row >= board.length) {
			System.out.println("  Board edge hit.");
            return -1;
        }
        if (column < 0 || column >= board[0].length) {
			System.out.println("  Board edge hit.");
            return -1;
        }
        //else if is empty cell return false;
        if (board[row][column] == EMPTY_CELL) {
			System.out.println("  Empty cell hit.");
            return -1;
        }
        //else if cell == playerID; return true;
        if (board[row][column] == playerId) {
			System.out.println("  Same stone type hit. Allow turning of stones.");
            return 0;
        }
        int next = turnStoneRecursive(row + rowChange, column + columnChange, rowChange, columnChange,
				playerId, turnStones);
        if (next >= 0) {
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
