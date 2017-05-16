package client;

import javafx.application.Application;
import javafx.stage.Stage;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Coen Boelhouwers
 */
public class ClientMain extends Application {

	private ExecutorService service;

	@Override
	public void start(Stage primaryStage) throws Exception {
		service = Executors.newCachedThreadPool();
		service.submit(() -> {
			try {
				byte[] bytes = new byte[256];
				InetAddress address = InetAddress.getByName("192.168.1.65");
				int port = 4445;
				DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);
				DatagramSocket socket = new DatagramSocket();
				socket.send(packet);
				System.out.println("send packet");
				//Socket socket = new Socket("192.168.1.65", 8000);
				//DataOutputStream stream = new DataOutputStream(socket.getOutputStream());
				//stream.writeUTF("Hello!");
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	@Override
	public void stop() throws Exception {
		super.stop();
		service.shutdown();
	}

	public static void main(String[] args) {
		launch(ClientMain.class, args);
	}
}
