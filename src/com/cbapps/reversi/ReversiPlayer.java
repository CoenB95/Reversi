package com.cbapps.reversi;

import java.io.*;
import java.net.Socket;

/**
 * @author Coen Boelhouwers
 */
public class ReversiPlayer extends Player {

	private Socket socket;
	private DataInputStream inputStream;
	private DataOutputStream outputStream;
	private int SessionId;

	public ReversiPlayer(Socket socket) throws IOException {
		this.socket = socket;
		this.inputStream = new DataInputStream(socket.getInputStream());
		this.outputStream = new DataOutputStream(socket.getOutputStream());
	}

	public String getMessage() throws IOException {
		return inputStream.readUTF();
	}

	public void setSessionId(int sessionId) {
		SessionId = sessionId;
	}

	public int getSessionId() {
		return SessionId;
	}


}
