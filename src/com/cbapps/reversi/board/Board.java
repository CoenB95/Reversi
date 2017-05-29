package com.cbapps.reversi.board;

import com.cbapps.reversi.ReversiPlayer;
import com.cbapps.reversi.SimplePlayer;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Coen Boelhouwers
 */
public class Board {
    public static final int EMPTY_CELL = 0;

    private int[][] board;
    private OnCellChangedListener listener;


    public Board(int columns, int rows, OnCellChangedListener l) {
        this.listener = l;
        this.board = new int[rows][columns];
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

    public void setOnCellChangedListener(OnCellChangedListener l) {
        listener = l;
    }

    public static void setupPlayerColors(List<SimplePlayer> players) {
        if (players.size() <= 2) {
            players.get(0).setColor(Color.BLACK);
            players.get(1).setColor(Color.WHITE);
        }
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

    public void a(int row, int column, int rowChange, int columnChange, int playerId) {
        if (b(row + rowChange, column + columnChange, rowChange, columnChange, playerId)) {
            System.out.println("We are allowed to turn this stone [" + row + "," + column + "]");
            changeCell(row, column, playerId);
        }
    }

    private boolean b(int row, int column, int rowChange, int columnChange, int playerId) {
        System.out.println("b called for cell at [" + row + "," + column + "]");
        //Bounds check; return false on hit
        if (row < 0 || row >= board.length) {
            System.out.println("row out of bounds");
            return false;
        }
        if (column < 0 || column >= board[0].length) {
            System.out.println("column out of bounds");
            return false;
        }
        //else if is empty cell return false;
        if (board[row][column] == EMPTY_CELL) {
            System.out.println("cell empty");
            return false;
        }
        //else if cell == playerID; return true;
        if (board[row][column] == playerId) {
            System.out.println("found own type of stone. Allow turning of tiles.");
            return true;
        }
        //else if (a()) {turn stone; return true;};
        if (b(row + rowChange, column + columnChange, rowChange, columnChange, playerId)) {
            System.out.println("We are allowed to turn this stone [" + row + "," + column + "]");
            changeCell(row, column, playerId);
            return true;
        }
        return false;
    }



}
