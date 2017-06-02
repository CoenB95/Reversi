package com.cbapps.reversi.board;

import com.cbapps.reversi.SimplePlayer;
import com.cbapps.reversi.client.CellPane;
import javafx.application.Platform;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

import java.io.IOException;
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

    public boolean changeAllValidCells(int row, int column, int playerId) {
        System.out.println("Move on [" + row + "," + column + "], checking cells...");
        if (!isCellEmpty(row, column)) {
        	System.out.println("Cell is already possessed. Move not allowed.");
        	return false;
		}
        int otherCellCount = 0;
        //Northwards
        otherCellCount += changeNeighboorCells(row - 1, column, -1, 0, playerId);
        //Eastwards
        otherCellCount += changeNeighboorCells(row, column + 1, 0, 1, playerId);
        //Southwards
        otherCellCount += changeNeighboorCells(row + 1, column, 1, 0, playerId);
        //Westwards
        otherCellCount += changeNeighboorCells(row, column - 1, 0, -1, playerId);

        //Diagonals
        //NorthEastwards
        otherCellCount += changeNeighboorCells(row - 1, column + 1, -1, 1, playerId);
        //Eastwards
        otherCellCount += changeNeighboorCells(row + 1, column + 1, 1, 1, playerId);
        //Eastwards
        otherCellCount += changeNeighboorCells(row + 1, column - 1, 1, -1, playerId);
        //Eastwards
        otherCellCount += changeNeighboorCells(row - 1, column - 1, -1, -1, playerId);

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
	private int changeNeighboorCells(int row, int column, int rowChange, int columnChange, int playerId) {
    	int change = changeNeighboorCell(row, column, rowChange, columnChange, playerId);
    	return change >= 0 ? change : 0;
	}

    private int changeNeighboorCell(int row, int column, int rowChange, int columnChange, int playerId) {
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
        int next = changeNeighboorCell(row + rowChange, column + columnChange, rowChange, columnChange, playerId);
        if (next >= 0) {
            System.out.println("  Turning stone at [" + row + "," + column + "].");
            changeCell(row, column, playerId);
            return next + 1;
        }
        return -1;
    }



}
