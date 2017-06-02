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
        this.boardHeight = columns;
        this.boardWidth = rows;
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
    public boolean isCellEmpty(int column, int row) {
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
		int centerLeft = Math.floorDiv(boardHeight, 2);
		int centerTop = Math.floorDiv(boardWidth, 2);
		//Setup start field
		changeCell(centerLeft, centerTop, 1);
		changeCell(centerLeft + 1, centerTop, 2);
		changeCell(centerLeft, centerTop + 1, 2);
		changeCell(centerLeft + 1, centerTop + 1, 1);
	}

    public static void setupPlayerColors(List<SimplePlayer> players) {
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
    	for (int i = 0; i < board.length; i++) {
			for (int j = 0; j < board[0].length; j++) {
				if (board[i][j] == EMPTY_CELL) return false;
			}
		}
		return true;
	}

    public boolean isOver() {
        boolean gameOver = false;
        int isOver = 0;
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                if (isCellEmpty(i, j) == false) {
                    isOver++;
                }
            }
        }
        if (isOver == 64) {
            gameOver = true;
        }
        return gameOver;
    }

    public int getWinner(){
        int Winner=-1;
        if(isOver()==true){
            if(getScoreOfPlayer(0)>getScoreOfPlayer(1)){
                Winner = 0;
            }
            else if(getScoreOfPlayer(0)<getScoreOfPlayer(1)){
                Winner = 1;
            }
            else{
                Winner = -1;
            }
        }
        return Winner;
    }

    public boolean changeAllValidCells(int row, int column, int playerId) {
        System.out.println("\n=== Check those cells! ===");
        int otherCellCount = 0;
        //Northwards
        System.out.println("> North");
        otherCellCount += changeNeighboorCell(row - 1, column, -1, 0, playerId);
        //Eastwards
        System.out.println("> East");
        otherCellCount += changeNeighboorCell(row, column + 1, 0, 1, playerId);
        //Southwards
        System.out.println("> South");
        otherCellCount += changeNeighboorCell(row + 1, column, 1, 0, playerId);
        //Westwards
        System.out.println("> West");
        otherCellCount += changeNeighboorCell(row, column - 1, 0, -1, playerId);

        //Diagonals
        //NorthEastwards
        System.out.println("> NorthEast");
        otherCellCount += changeNeighboorCell(row - 1, column + 1, -1, 1, playerId);
        //Eastwards
        System.out.println("> SouthEast");
        otherCellCount += changeNeighboorCell(row + 1, column + 1, 1, 1, playerId);
        //Eastwards
        System.out.println("> SouthWest");
        otherCellCount += changeNeighboorCell(row + 1, column - 1, 1, -1, playerId);
        //Eastwards
        System.out.println("> NorthWest");
        otherCellCount += changeNeighboorCell(row - 1, column - 1, -1, -1, playerId);

        if (otherCellCount > 1) {
            System.out.println("We are allowed to turn this stone [" + row + "," + column + "]");
            changeCell(row, column, playerId);
            return true;
        } else {
			System.out.println("There were not enough other stones in between.");
			return false;
		}
    }

    private int changeNeighboorCell(int row, int column, int rowChange, int columnChange, int playerId) {
        System.out.println("changeNeighboorCell called for cell at [" + row + "," + column + "]");
        //Bounds check; return false on hit
        if (row < 0 || row >= board.length) {
            System.out.println("row out of bounds");
            return 0;
        }
        if (column < 0 || column >= board[0].length) {
            System.out.println("column out of bounds");
            return 0;
        }
        //else if is empty cell return false;
        if (board[row][column] == EMPTY_CELL) {
            System.out.println("cell empty");
            return 0;
        }
        //else if cell == playerID; return true;
        if (board[row][column] == playerId) {
            System.out.println("found own type of stone. Allow turning of tiles.");
            return 1;
        }
        //else if (changeAllValidCells()) {turn stone; return true;};
        int next = changeNeighboorCell(row + rowChange, column + columnChange, rowChange, columnChange, playerId);
        if (next >= 1) {
            System.out.println("We are allowed to turn this stone [" + row + "," + column + "]");
            changeCell(row, column, playerId);
            return next + 1;
        }
        return 0;
    }



}
