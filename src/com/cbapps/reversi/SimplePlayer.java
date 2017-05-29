package com.cbapps.reversi;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.io.*;
import java.net.Socket;

/**
 * @author Coen Boelhouwers
 */
public class SimplePlayer implements Serializable {

	private String name;
	private transient Color color;
	private int sessionId;

	public SimplePlayer(String name) {
		this(name, Color.BLACK);
	}

	public SimplePlayer(String name, Color color) {
		this.name = name;
		this.color = color;
	}

	public String getName() {
		return name;
	}

	public Paint getColor() {
		return color;
	}

	public int getSessionId() {
		return sessionId;
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		color = new Color(in.readDouble(), in.readDouble(), in.readDouble(), in.readDouble());
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public void setName(String name) {
		this.name = name;
	}

	public SimplePlayer setSessionId(int sessionId) {
		this.sessionId = sessionId;
		return this;
	}

	@Override
	public String toString() {
		return "Player{name=" + name + ", color=" + color + "}";
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		out.writeDouble(color.getRed());
		out.writeDouble(color.getGreen());
		out.writeDouble(color.getBlue());
		out.writeDouble(color.getOpacity());
	}
}
