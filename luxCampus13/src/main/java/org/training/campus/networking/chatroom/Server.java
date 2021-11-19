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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Server extends Worker {
	private static final String GREETING = "Привіт, %s!";
	private static final int ACCEPT_TIMEOUT = 1000;
	private static final int BUFFER_SIZE = 1024;
	private static final String ANONYMOUS_USER_NAME = "Anonymous";

	private final int port;
	private final Charset charset;
	private int handlerCount = 0;
	private ServerSocket serverSocket;
	private ThreadGroup threadGroup;
	private Queue<Message> messageQueue;

	public Server(int ordinal, int port, Charset charset) {
		this.port = port;
		this.charset = charset;
		threadGroup = new ThreadGroup(String.valueOf(ordinal));
		messageQueue = new ConcurrentLinkedQueue<>();
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
		private String author = ANONYMOUS_USER_NAME;
		private LocalDateTime browseStart;

		private RequestHandler(Socket socket, int no) {
			super(threadGroup, String.valueOf(no));
			this.socket = socket;
			browseStart = LocalDateTime.now();
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
					receiveBroadcast(reader, writer);
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
			Queue<String> replies = receiveAtLeast(reader, 1);
			if (!replies.isEmpty()) {
				author = replies.poll();
				send(writer, String.format(GREETING, author));
			} else {
				throw new CommunicationException("client hasn't responded, no name provided");
			}
		}

		private void receiveBroadcast(BufferedReader reader, PrintWriter writer) throws IOException {
			receiveStore(reader);
			fetchBroadcast(writer);
		}

		private void receiveStore(BufferedReader reader) throws IOException {
			for (String note : receiveAvailable(reader)) {
				messageQueue.add(new Message(author, LocalDateTime.now(), note));
			}
		}

		private void fetchBroadcast(PrintWriter writer) {
			for (Message message : messageQueue) {
				if (!author.equals(message.author()) && browseStart.isBefore(message.stamp())) {
					send(writer, String.format("%s received message on %tr from %s", author, LocalDateTime.now(),
							message.toString()));
					browseStart = message.stamp();
				}
			}
		}

	}

}
