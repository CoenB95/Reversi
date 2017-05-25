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

/**
 * @author Coen Boelhouwers
 */
public class ServerMain extends Application {

	private List<ReversiSession> sessions;
	private int port = 8000;
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
				log("Server started.\n" +
						"IP: " + InetAddress.getLocalHost().getHostAddress() + "\n" +
						"Port: " + port + "\n");

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

							log("New player found (IP: " + channel.socket().getInetAddress().getHostAddress() +
									", Player name: '" + player.getName() + "').\n");

							//Send available sessions
							ObjectOutputStream oos = player.getOutputStream();
							List<String> sessionNames = new ArrayList<>();
							sessions.forEach(s -> sessionNames.add(s.getSessionName()));
							log("Available sessions for '" + player.getName() + "':\n" + sessionNames + "\n");
							oos.writeObject(sessionNames);
							oos.flush();

							//Receive chosen session
							String chosenSessionName = ois.readUTF();
							log("Player '" + player.getName() + "' chose for session '" + chosenSessionName + "'.\n");

							//Match the sessionName with an actual session, create a new one with the
							//name if there is no match.
							ReversiSession chosenSession = sessions.stream()
									.filter(s -> s.getSessionName().equals(chosenSessionName))
									.findFirst().orElse(null);

							int sessionId;
							if (chosenSession != null) {
								sessionId = chosenSession.addPlayer(player);
							} else {
								chosenSession = new ReversiSession(chosenSessionName,
										5, 5)
										.setSessionNr(sessions.size());
								sessionId = chosenSession.addPlayer(player);
								chosenSession.startSession(service);
								sessions.add(chosenSession);
								log("Started a new session named '" + chosenSession.getSessionName() + "'\n");
							}
							//Acknowledge by sending back the sessionID.
							player.setSessionId(sessionId);
							log("Added player '" + player + "' to session '" + chosenSession.getSessionName() +
									"' (sessionID = " + sessionId + ")\n");
							oos.writeInt(sessionId);
							oos.flush();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

				}
				System.out.println("Service shutdown");
			} catch (IOException e) {
				e.printStackTrace();
			}

		});
		/*new AnimationTimer() {
			@Override
			public void handle(long now) {
				if (lastNanos == 0) lastNanos = now;
				frame((now - lastNanos) / 1_000_000_000.0);
				lastNanos = now;
			}
		}.start();*/
		ScrollPane root = new ScrollPane(textArea);
		BorderPane borderPane = new BorderPane(root);
		borderPane.setBottom(new CellPane());
		//new ReversiSession(10, 10);
		primaryStage.setScene(new Scene(borderPane, 500, 500));
		primaryStage.show();
	}

	public static void log(String text) {
		Platform.runLater(() -> textArea.appendText(text));
	}

	@Override
	public void stop() throws Exception {
		super.stop();
		service.shutdown();
	}

	/*public void frame(double elapsedTime) {
		System.out.println("Passed time: " + elapsedTime);
	}*/

	public static void main(String[] args) {
		launch(ServerMain.class, args);
	}
}
