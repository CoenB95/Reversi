/**
 * @author Coen Boelhouwers
 */
public class Session {

	public static final int EMPTY_FIELD = 0;

	private int[][] board;

	public Session(int boardWidth, int boardHeight) {
		board = new int[boardWidth][boardHeight];
	}
}
