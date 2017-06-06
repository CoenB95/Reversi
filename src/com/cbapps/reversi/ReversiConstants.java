package com.cbapps.reversi;

/**
 * @author Coen Boelhouwers
 */
public interface ReversiConstants {
	int PORT = 8081;

	/**Usage: [PLAYER_ADDED][{@link SimplePlayer} player]*/
	int SERVER_SEND_PLAYER_ADDED = 1000;

	/**Usage: [START]*/
	int SERVER_RECEIVE_START_GAME = 2000;

	/**Usage: [START][int amountOfPlayers]*/
	int SERVER_SEND_START_GAME = 2001;

	/**Usage: [START_MOVE]*/
	int SERVER_SEND_START_MOVE = 3000;

	/**Usage: [MOVE][int row][int column][boolean freeMove]*/
	int SERVER_RECEIVE_MOVE = 3001;

	/**Usage: [YOU_WON]*/
	int SERVER_SEND_YOU_WON = 4000;

	/**Usage: [OTHER_WON][int player_id]*/
	int SERVER_SEND_OTHER_WON = 4500;

	/**Usage: [START_MOVE][int playerId]*/
	int SERVER_SEND_OTHER_START_MOVE = 5000;

	/**Usage: [OTHER_DID_MOVE][int playerId][int row][int column][boolean freeMove]*/
	int SERVER_SEND_OTHER_DID_MOVE = 6000;

	/**Usage: [SUCCESS]*/
	int SERVER_SEND_SUCCESS = 9000;
	int SERVER_RECEIVE_SUCCESS = 9001;

	/**Usage: [ERROR][utfString message]*/
	int SERVER_SEND_ERROR = 9500;
	int SERVER_RECEIVE_ERROR = 9600;

	int CLIENT_RECEIVE_PLAYER_ADDED = SERVER_SEND_PLAYER_ADDED;
	int CLIENT_SEND_MOVE = SERVER_RECEIVE_MOVE;
	int CLIENT_SEND_START_GAME = SERVER_RECEIVE_START_GAME;
	int CLIENT_RECEIVE_START_GAME = SERVER_SEND_START_GAME;
	int CLIENT_RECEIVE_START_MOVE = SERVER_SEND_START_MOVE;
	int CLIENT_RECEIVE_YOU_WON = SERVER_SEND_YOU_WON;
	int CLIENT_RECEIVE_OTHER_WON = SERVER_SEND_OTHER_WON;
	int CLIENT_RECEIVE_OTHER_START_MOVE = SERVER_SEND_OTHER_START_MOVE;
	int CLIENT_RECEIVE_OTHER_DID_MOVE = SERVER_SEND_OTHER_DID_MOVE;
	int CLIENT_RECEIVE_SUCCESS = SERVER_SEND_SUCCESS;
	int CLIENT_SEND_SUCCESS = SERVER_RECEIVE_SUCCESS;
	int CLIENT_RECEIVE_ERROR = SERVER_SEND_ERROR;
	int CLIENT_SEND_ERROR = SERVER_RECEIVE_ERROR;
}
