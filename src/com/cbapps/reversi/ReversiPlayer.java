package com.cbapps.reversi;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.io.*;
import java.net.Socket;

/**
 * @author Coen Boelhouwers
 */
public class ReversiPlayer extends SimplePlayer {

	private transient Socket socket;
	private transient ObjectInputStream inputStream;
	private transient ObjectOutputStream outputStream;

	public ReversiPlayer(Socket socket) throws IOException {
		this("Unnamed", Color.BLACK, socket);
	}

	public ReversiPlayer(String name, Socket socket) throws IOException {
		this(name, Color.BLACK, socket);
	}

	public ReversiPlayer(String name, Color color, Socket socket) throws IOException {
		super(name, color);
		this.socket = socket;
	}

	public ObjectInputStream getInputStream() throws IOException {
		if (inputStream == null) inputStream = new ObjectInputStream(socket.getInputStream());
		return inputStream;
	}

	public ObjectOutputStream getOutputStream() throws IOException {
		if (outputStream == null) outputStream = new ObjectOutputStream(socket.getOutputStream());
		return outputStream;
	}
}
