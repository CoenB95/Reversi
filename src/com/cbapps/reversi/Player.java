package com.cbapps.reversi;

import java.io.*;
import java.net.Socket;

/**
 * @author Coen Boelhouwers
 */
public class Player {
	private String name;
	private Socket socket;
	private DataInputStream inputStream;
	private DataOutputStream outputStream;

	public Player(Socket socket) throws IOException {
		this.socket = socket;
		this.inputStream = new DataInputStream(socket.getInputStream());
		this.outputStream = new DataOutputStream(socket.getOutputStream());
	}

	public String getMessage() throws IOException {
		return inputStream.readUTF();
	}
}
