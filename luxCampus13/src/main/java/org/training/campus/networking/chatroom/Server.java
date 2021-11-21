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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;

public class Server extends Worker {
	private static final String GREETING = "Привіт, %s!";
	private static final int ACCEPT_TIMEOUT = 1000;
	private static final int BUFFER_SIZE = 1024;
	private static final String EMPTY_AUTHOR = "";

	private final int port;
	private final Charset charset;
	private int handlerCount = 0;
	private ServerSocket serverSocket;
	private ThreadGroup threadGroup;
	private NavigableSet<Message> messageSet;

	public Server(int ordinal, int port, Charset charset) {
		this.port = port;
		this.charset = charset;
		threadGroup = new ThreadGroup(String.valueOf(ordinal));
		messageSet = new ConcurrentSkipListSet<>();
	}

	@Override
	public void run() {
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

	private synchronized void closeSocket() {
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
	public boolean cancel(boolean mayInterruptIfRunning) {
		super.cancel(mayInterruptIfRunning);
		closeSocket();
		threadGroup.interrupt();
		return true;
	}

	private class RequestHandler extends Thread {
		private final Socket socket;
		private String author;
		private LocalDateTime startStamp;

		private RequestHandler(Socket socket, int no) {
			super(threadGroup, String.valueOf(no));
			this.socket = socket;
			this.startStamp = LocalDateTime.now();
		}

		@Override
		public void run() {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), charset),
					BUFFER_SIZE);
					PrintWriter writer = new PrintWriter(
							new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), charset), BUFFER_SIZE),
							true)) {

				greetNewcomer(reader, writer);
				while (!(isDone() || Thread.interrupted())) {
					receiveBroadcastMessages(reader, writer);
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
		}

		private void greetNewcomer(BufferedReader reader, PrintWriter writer) throws IOException {
			author = receiveOne(reader);
			if (author != null && !author.isBlank()) {
				send(writer, String.format(GREETING, author));
			} else {
				throw new CommunicationException("client hasn't responded with any meaningful name");
			}
		}

		private void receiveBroadcastMessages(BufferedReader reader, PrintWriter writer) throws IOException {
			receiveStoreMessages(reader);
			fetchBroadcastMessages(writer);
		}

		private void receiveStoreMessages(BufferedReader reader) throws IOException {
			for (String note : receiveAvailable(reader)) {
				Message message = new Message(author, LocalDateTime.now(), note);
				messageSet.add(message);
			}
		}

		private void fetchBroadcastMessages(PrintWriter writer) {
			NavigableSet<Message> messages = messageSet.tailSet(new Message(EMPTY_AUTHOR, startStamp, null), true);
			for (Message message : messages) {
				if (!author.equals(message.author())) {
					send(writer, String.format("%s received message from %s on %tr", author, message.toString(),
							LocalDateTime.now()));
				}
				if (startStamp.isBefore(message.stamp()) || startStamp.isEqual(message.stamp())) {
					startStamp = message.stamp().plus(1, ChronoUnit.NANOS);
				}
			}
		}

	}

}
