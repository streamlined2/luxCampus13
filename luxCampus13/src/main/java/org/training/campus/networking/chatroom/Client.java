package org.training.campus.networking.chatroom;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Queue;
import java.util.function.Consumer;

public class Client extends Worker {
	private static final long SLEEP_TIME = 100;
	private static final int BUFFER_SIZE = 1024;

	private final InetAddress serverAddress;
	private final int port;
	private final String name;
	private final Charset charset;

	public Client(String name, InetAddress serverAddress, int port, Charset charset) {
		this.name = name;
		this.serverAddress = serverAddress;
		this.port = port;
		this.charset = charset;
	}

	@Override
	public void run() {
		// System.out.printf("client '%s' started.%n", name);
		try (Socket socket = new Socket(serverAddress, port);
				PrintWriter writer = new PrintWriter(
						new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), charset), BUFFER_SIZE),
						true);
				BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), charset),
						BUFFER_SIZE)) {

			doWork(reader, writer);
		} catch (IOException e) {
			e.printStackTrace();
			throw new CommunicationException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		// System.out.printf("client '%s' shutdown.%n", name);
	}

	private void doWork(BufferedReader reader, PrintWriter writer) throws IOException, InterruptedException {
		communicate(reader, writer, name, System.out::println);
		int count = 0;
		while (!(isDone() || Thread.interrupted())) {
			String stimulus = composeMessage(count++);
			communicate(reader, writer, stimulus, System.out::println);
			Thread.sleep(SLEEP_TIME);
		}
	}

	private String composeMessage(int count) {
		return String.format("%s #%d (%s)", name, count, DateTimeFormatter.ISO_LOCAL_TIME.format(LocalTime.now()));
	}

	protected void communicate(BufferedReader reader, PrintWriter writer, String message, Consumer<String> sink)
			throws IOException {
		send(writer, message);
		Queue<String> replies = receive(reader);
		replies.forEach(sink);
	}

}
