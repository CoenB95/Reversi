import java.net.Socket;

/**
 * @author Coen Boelhouwers
 */
public class Player {
	String name;
	Socket socket;

	public Player(Socket socket) {
		this.socket = socket;
	}

	public Socket getSocket() {
		return socket;
	}
}
