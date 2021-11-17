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

public class Client extends Worker {
	private static final long SLEEP_TIME = 100;
	private static final int BUFFER_SIZE = 1024;

	private final int ordinal;
	private String message;
	private final InetAddress serverAddress;
	private final int port;
	private final Charset charset;

	public Client(int ordinal, InetAddress serverAddress, int port, Charset charset) {
		this.ordinal = ordinal;
		this.message = "hello!";
		this.serverAddress = serverAddress;
		this.port = port;
		this.charset = charset;
	}

	@Override
	public void run() {
		System.out.printf("client #%d (%s:%d) started.%n", ordinal, serverAddress.toString(), port);
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
		System.out.printf("client #%d shutdown.%n", ordinal);
	}

	private void doWork(BufferedReader reader, PrintWriter writer) throws IOException, InterruptedException {
		while (!(isDone() || Thread.interrupted())) {
			String stimulus = String.format("client #%d (%s): %s", ordinal,
					DateTimeFormatter.ISO_LOCAL_TIME.format(LocalTime.now()), message);
			System.out.println(stimulus);
			writer.println(stimulus);
			while (reader.ready()) {
				System.out.println(reader.readLine());
			}
			Thread.sleep(SLEEP_TIME);
		}
	}

}
