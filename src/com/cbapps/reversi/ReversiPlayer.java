package com.cbapps.reversi;

import javafx.scene.paint.Paint;

import java.io.*;
import java.net.Socket;

/**
 * @author Coen Boelhouwers
 */
public class ReversiPlayer {

	private String name;
	private Paint color;
	private Socket socket;
	private DataInputStream inputStream;
	private DataOutputStream outputStream;
	private int SessionId;

	public ReversiPlayer(String name, Paint color, Socket socket) throws IOException {
		this.name = name;
		this.color = color;
		this.socket = socket;
		this.inputStream = new DataInputStream(socket.getInputStream());
		this.outputStream = new DataOutputStream(socket.getOutputStream());
	}

	public ReversiPlayer(PlayerInfo p, Socket s) throws IOException {
		this(p.getName(), p.getColor(), s);
	}

	public String getName() {
		return name;
	}

	public Paint getColor() {
		return color;
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
