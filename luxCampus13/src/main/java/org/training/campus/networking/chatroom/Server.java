package org.training.campus.networking.chatroom;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Server implements RunnableFuture<Void> {
	private static final String CLIENT_GREETING = "Привіт!";
	private static final int ACCEPT_TIMEOUT = 1000;
	private static final int BUFFER_SIZE = 1024;
	private static final long JOIN_TIMEOUT = 100;
	private static final int JOIN_COUNT = 5;

	private final int ordinal;
	private final int port;
	private final Charset charset;
	private volatile boolean proceed = true;
	private int handlerCount = 0;
	private ServerSocket serverSocket = null;
	private ThreadGroup threadGroup;

	public Server(int ordinal, int port, Charset charset) {
		this.ordinal = ordinal;
		this.port = port;
		this.charset = charset;
		threadGroup = new ThreadGroup(String.valueOf(ordinal));
	}

	private void closeSocket() {
		if (serverSocket != null) {
			try {
				serverSocket.close();
				serverSocket = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void run() {
		System.out.printf("server #%d started.%n", ordinal);
		try {
			serverSocket = new ServerSocket(port);
			serverSocket.setSoTimeout(ACCEPT_TIMEOUT);
			while (!(isDone() || Thread.interrupted())) {
				handleRequest();
			}
		} catch (IOException e) {
			if (!isDone()) {
				e.printStackTrace();
				throw new CommunicationException(e);
			}
		} finally {
			closeSocket();
		}
		System.out.printf("server #%d shutdown.%n", ordinal);
	}

	private void handleRequest() throws IOException {
		try {
			Socket socket = serverSocket.accept();
			new RequestHandler(socket, handlerCount++).start();
		} catch (SocketTimeoutException e) {
			// let server check if thread should be interrupted and then continue waiting
			// for incoming connection
		}
	}

	private class RequestHandler extends Thread {
		private final Socket socket;
		private final int no;

		private RequestHandler(Socket socket, int no) {
			super(threadGroup, String.valueOf(no));
			this.socket = socket;
			this.no = no;
		}

		@Override
		public void run() {
			System.out.printf("handler #%d of server #%d started.%n", no, ordinal);
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), charset),
					BUFFER_SIZE);
					PrintWriter writer = new PrintWriter(
							new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), charset), BUFFER_SIZE),
							true)) {
				while (!(isDone() || Thread.interrupted())) {
					String reply = String.format("server #%d, handler #%d (%s): %s%n", ordinal, no,
							DateTimeFormatter.ISO_LOCAL_TIME.format(LocalTime.now()), reader.readLine());
					writer.println(reply);
				}
			} catch (IOException e) {
				e.printStackTrace();
				throw new CommunicationException(e);
			} finally {
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			System.out.printf("handler #%d of server #%d shutdown.%n", no, ordinal);
		}
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		proceed = false;
		closeSocket();
		threadGroup.interrupt();
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
