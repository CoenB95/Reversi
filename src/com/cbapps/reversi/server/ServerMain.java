package com.cbapps.reversi.server;

import com.cbapps.reversi.ReversiPlayer;
import com.cbapps.reversi.ReversiSession;
import com.cbapps.reversi.client.CellPane;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.cbapps.reversi.ReversiConstants.*;

/**
 * @author Coen Boelhouwers
 */
public class ServerMain extends Application {

	private static String TAG = "Server";

	private List<ReversiSession> sessions;
	private int port = 8081;
	private ServerSocketChannel server;
	private ExecutorService service;
	private ReversiPlayer incomingPlayer;

	private static TextArea textArea;

	@Override
	public void start(Stage primaryStage) throws Exception {
		textArea = new TextArea();
		sessions = new ArrayList<>();
		service = Executors.newCachedThreadPool();
		service.submit(() -> {
			try {
				server = ServerSocketChannel.open();
				server.configureBlocking(false);
				server.socket().bind(new InetSocketAddress(InetAddress.getLocalHost(), port));
				log(TAG, "Server started.\n" +
						"IP: " + InetAddress.getLocalHost().getHostAddress() + "\n" +
						"Port: " + port);

				while (!service.isShutdown()) {
					if (!server.isOpen()) {
						System.out.println("Server not yet opened");
						continue;
					}
					try {
						SocketChannel channel = server.accept();
						if (channel != null) {
							incomingPlayer = new ReversiPlayer(channel.socket());

							//Request player's name
							ObjectInputStream ois = incomingPlayer.getInputStream();
							incomingPlayer.setName(ois.readUTF());

							log(TAG, "New player found (IP: " + channel.socket().getInetAddress().getHostAddress() +
									", Player name: '" + incomingPlayer.getName() + "').");

							//Good time to cleanup by removing finished sessions, so they can be reused.
							sessions.removeIf(ReversiSession::isFinished);

							//Send available sessions
							ObjectOutputStream oos = incomingPlayer.getOutputStream();
							List<String> sessionNames = new ArrayList<>();
							sessions.stream()
									.filter(ReversiSession::isOpened)
									.forEach(s -> sessionNames.add(s.getSessionName()));
							log(TAG, "Available sessions for '" + incomingPlayer.getName() + "': " + sessionNames);
							oos.writeObject(sessionNames);
							oos.flush();

							while (true) {
								//Receive chosen session
								log(TAG, "Waiting for '" + incomingPlayer.getName() + "' to choose session...");
								ReversiSession.requireState(incomingPlayer, null, SERVER_RECEIVE_SUCCESS);
								String chosenSessionName = ois.readUTF();
								log(TAG, "Player '" + incomingPlayer.getName() + "' chose for session '" + chosenSessionName + "'.");

								//Match the sessionName with an actual session, create turnStones new one with the
								//name if there is no match.
								ReversiSession chosenSession = sessions.stream()
										.filter(s -> s.getSessionName().equals(chosenSessionName))
										.findFirst().orElse(null);

								int sessionId;
								if (chosenSession != null) {
									if (chosenSession.isOpened()) {
										sessionId = chosenSession.addPlayer(incomingPlayer);
									} else {
										log(TAG, "Session '" + chosenSession.getSessionName() + "' is already " +
												"closed. Choose a different one.");
										oos.writeInt(SERVER_SEND_ERROR);
										oos.writeUTF("Session is already started. Try another");
										oos.flush();
										continue;
									}
								} else {
									System.out.println("Session doesn't exist. Creating one with same name...");
									chosenSession = new ReversiSession(chosenSessionName,
											8, 8, service);
									log(TAG, "Started new session named '" + chosenSession.getSessionName() + "'");
									sessionId = chosenSession.addPlayer(incomingPlayer);
									sessions.add(chosenSession);
								}

								System.out.println("Player sessionId=" + sessionId);
								//Acknowledge by sending back the sessionID.
								incomingPlayer.setSessionId(sessionId);
								log(TAG, "Added player '" + incomingPlayer + "' to session '" + chosenSession.getSessionName() +
										"' (sessionID = " + sessionId + ")");
								System.out.println("Send sessionID...");
								oos.writeInt(SERVER_SEND_SUCCESS);
								oos.writeInt(sessionId);
								oos.flush();
								System.out.println("SessionID send, start session if needed.");
								chosenSession.startSession();
								break;
							}
						}
						Thread.sleep(100);
					} catch (Exception e) {
						e.printStackTrace();
						if (incomingPlayer != null) {
							log(TAG, "Connection lost with '" + incomingPlayer.getName() + "'.");
							incomingPlayer.getOutputStream().close();
						} else log(TAG, "Connection lost.");
					}
				}
				System.out.println("Service shutdown");
			} catch (IOException e) {
				e.printStackTrace();
			}

		});
		ScrollPane root = new ScrollPane(textArea);
		BorderPane borderPane = new BorderPane(root);
		borderPane.setBottom(new CellPane(0, 0));
		primaryStage.setScene(new Scene(borderPane, 500, 500));
		primaryStage.show();
	}

	public static void log(String tag, String text) {
		Platform.runLater(() -> textArea.appendText(String.format("%-20s %s%n", "[" + tag + "]", text)));
	}

	@Override
	public void stop() throws Exception {
		super.stop();
		if (incomingPlayer != null) {
			System.err.println("Force close connection with " + incomingPlayer.getName());
			incomingPlayer.getOutputStream().close();
		}
		for (ReversiSession session : sessions) {
			for (ReversiPlayer p : session.getPlayers()) {
				System.err.println("Force close connection with " + p.getName());
				p.getOutputStream().close();
			}
		}
		service.shutdown();
	}

	public static void main(String[] args) {
		launch(ServerMain.class, args);
	}
}
