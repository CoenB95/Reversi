import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Coen Boelhouwers
 */
public class Main extends Application {

	private long lastNanos;
	private int port = 8000;
	private ServerSocketChannel server;
	private ExecutorService service;
	private List<Session> sessions;

	private TextArea textArea;

	@Override
	public void start(Stage primaryStage) throws Exception {
		sessions = new ArrayList<>();

		textArea = new TextArea();
		service = Executors.newCachedThreadPool();
		service.submit(() -> {
			try {
				server = ServerSocketChannel.open();
				server.configureBlocking(false);
				server.socket().bind(new InetSocketAddress(port));
				log("Server started.\n" +
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
							log("New player found (IP=" + channel.socket().getInetAddress().getHostAddress() +
									").");
							Player newPlayer = new Player(channel.socket());
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
		new Session(10, 10);
		primaryStage.setScene(new Scene(root, 500, 500));
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
		launch(Main.class, args);
	}
}
