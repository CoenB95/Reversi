package com.cbapps.reversi.server;

import com.cbapps.reversi.PlayerInfo;
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

/**
 * @author Coen Boelhouwers
 */
public class ServerMain extends Application {

	private List<ReversiSession> sessions;
	private int port = 8000;
	private ServerSocketChannel server;
	private ExecutorService service;

	private TextArea textArea;

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
							//Request player's name and color
							ObjectInputStream ois = new ObjectInputStream(channel.socket().getInputStream());
							PlayerInfo playerInfoInfo = (PlayerInfo) ois.readObject();

							log("New player found (IP=" + channel.socket().getInetAddress().getHostAddress() +
									", PlayerInfo=" + playerInfoInfo + ").\n");

							//Send available sessions
							ObjectOutputStream oos = new ObjectOutputStream(channel.socket().getOutputStream());
							List<String> sessionNames = new ArrayList<>();
							sessions.forEach(s -> sessionNames.add(s.getSessionName()));
							log("Available sessions for " + playerInfoInfo + ":\n" + sessionNames);
							oos.writeObject(sessionNames);
							oos.flush();

							//Receive chosen session
							String chosenSession = (String) ois.readObject();
							log("PlayerInfo " + playerInfoInfo + " chose for session '" + chosenSession + "'.\n");

							ReversiPlayer player = new ReversiPlayer(playerInfoInfo, channel.socket());

							if (chosenSession != null) {
								ReversiSession ses = sessions.stream()
										.filter(s -> s.getSessionName().equals(chosenSession))
										.findFirst().orElse(null);

								if (ses != null) {
									ses.addPlayer(player);
									log("Added player '" + player + "' to session '" + ses.getSessionName() +
											"'.\n");
								} else {
									ReversiSession newSession = new ReversiSession(chosenSession,
											5, 5, textArea)
											.setSessionNr(sessions.size());
									newSession.addPlayer(player);
									sessions.add(newSession);
									newSession.begin(service);
									log("Started a new session named '" + newSession.getSessionName() +
											"' and added player '" + player.getName() + "'.\n");
								}
							} else {
								log("The chosen session should not be null. Ignored request.\n");
							}
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

	private void log(String text) {
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
