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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Client implements RunnableFuture<Void> {
	private static final int MESSAGE_COUNT = 100;
	private static final long SLEEP_TIME = 100;
	private static final int BUFFER_SIZE = 1024;
	private static final long JOIN_TIMEOUT = 100;
	private static final int JOIN_COUNT = 5;

	private final int ordinal;
	private String message;
	private final InetAddress serverAddress;
	private final int port;
	private final Charset charset;
	private volatile boolean proceed = true;

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
		try {
			try (Socket socket = new Socket(serverAddress, port);
					PrintWriter writer = new PrintWriter(
							new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), charset), BUFFER_SIZE),
							true);
					BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), charset),
							BUFFER_SIZE)) {
				for (int msgCount = 0; msgCount < MESSAGE_COUNT && !isDone() && !Thread.interrupted(); msgCount++) {
					String stimulus = String.format("client #%d (%s): %s (%d)", ordinal,
							DateTimeFormatter.ISO_LOCAL_TIME.format(LocalTime.now()), message, msgCount);
					System.out.println(stimulus);
					writer.println(stimulus);
					while (reader.ready()) {
						System.out.println(reader.readLine());
					}
					Thread.sleep(SLEEP_TIME);
				}
			} catch (IOException e) {
				e.printStackTrace();
				throw new CommunicationException(e);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		System.out.printf("client #%d shutdown.%n", ordinal);
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		proceed = false;
		return true;
	}

	@Override
	public boolean isCancelled() {
		return true;
	}

	@Override
	public boolean isDone() {
		return !proceed;
	}

	@Override
	public Void get() throws InterruptedException, ExecutionException {
		try {
			return get(JOIN_TIMEOUT * JOIN_COUNT, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		for (int k = 0; k < JOIN_COUNT && !isDone(); k++) {
			Thread.sleep(JOIN_TIMEOUT);
		}
		return null;
	}

}
