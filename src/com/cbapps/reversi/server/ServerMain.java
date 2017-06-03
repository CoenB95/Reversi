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

import java.io.DataInputStream;
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

import static com.cbapps.reversi.ReversiConstants.SERVER_RECEIVE_START_GAME;

/**
 * @author Coen Boelhouwers
 */
public class ServerMain extends Application {

	private static String TAG = "Server";

	private List<ReversiSession> sessions;
	private int port = 8081;
	private ServerSocketChannel server;
	private ExecutorService service;

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
							ReversiPlayer player = new ReversiPlayer(channel.socket());

							//Request player's name
							ObjectInputStream ois = player.getInputStream();
							player.setName(ois.readUTF());

							log(TAG, "New player found (IP: " + channel.socket().getInetAddress().getHostAddress() +
									", Player name: '" + player.getName() + "').");

							//Send available sessions
							ObjectOutputStream oos = player.getOutputStream();
							List<String> sessionNames = new ArrayList<>();
							sessions.forEach(s -> sessionNames.add(s.getSessionName()));
							log(TAG, "Available sessions for '" + player.getName() + "': " + sessionNames);
							oos.writeObject(sessionNames);
							oos.flush();

							//Receive chosen session
							System.out.println("Waiting for session choice...");
							String chosenSessionName = ois.readUTF();
							System.out.println("Received session choice: '" + chosenSessionName + "'");
							log(TAG, "Player '" + player.getName() + "' chose for session '" + chosenSessionName + "'.");

							//Match the sessionName with an actual session, create turnStones new one with the
							//name if there is no match.
							ReversiSession chosenSession = sessions.stream()
									.filter(s -> s.getSessionName().equals(chosenSessionName))
									.findFirst().orElse(null);
							System.out.println("Searching session...");

							int sessionId;
							if (chosenSession != null) {
								System.out.println("Session exists. Add player...");
								sessionId = chosenSession.addPlayer(player);
								System.out.println("Player added.");
							} else {
								System.out.println("Session doesn't exist. Creating one with same name...");
								chosenSession = new ReversiSession(chosenSessionName,
										8, 8, service)
										.setSessionNr(sessions.size());
								System.out.println("Add player...");
								sessionId = chosenSession.addPlayer(player);
								System.out.println("Player added.");
								sessions.add(chosenSession);
								log(TAG, "Started turnStones new session named '" + chosenSession.getSessionName() + "'");
							}
							System.out.println("Player sessionId=" + sessionId);
							//Acknowledge by sending back the sessionID.
							player.setSessionId(sessionId);
							log(TAG, "Added player '" + player + "' to session '" + chosenSession.getSessionName() +
									"' (sessionID = " + sessionId + ")");
							System.out.println("Send sessionID...");
							oos.writeInt(sessionId);
							oos.flush();
							System.out.println("SessionID send, start session if needed.");
							chosenSession.startSession();
						}
						Thread.sleep(100);
					} catch (Exception e) {
						e.printStackTrace();
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
		service.shutdown();
	}

	public static void main(String[] args) {
		launch(ServerMain.class, args);
	}
}
