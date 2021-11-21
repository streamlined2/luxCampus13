package org.training.campus.networking.chatroom;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class Worker implements RunnableFuture<Void> {
	private static final long JOIN_TIMEOUT = 100;
	private static final int JOIN_COUNT = 5;

	private final long joinTimeout;
	private final int joinCount;
	private volatile boolean proceed = true;

	protected Worker() {
		this(JOIN_TIMEOUT, JOIN_COUNT);
	}

	protected Worker(long joinTimeout, int joinCount) {
		this.joinTimeout = joinTimeout;
		this.joinCount = joinCount;
	}

	protected void send(PrintWriter writer, String message) {
		writer.println(message);
	}

	protected Queue<String> receiveAvailable(BufferedReader reader) throws IOException {
		Queue<String> replies = new LinkedList<>();
		while (reader.ready()) {
			final String line = reader.readLine();
			if (line != null) {
				replies.add(line);
			} else {
				break;
			}
		}
		return replies;
	}

	protected Queue<String> receiveAtLeast(BufferedReader reader, int atLeast) throws IOException {
		Queue<String> replies = new LinkedList<>();
		for (int count = 0; count < atLeast; count++) {
			final String line = reader.readLine();
			if (line != null) {
				replies.add(line);
			} else {
				throw new CommunicationException(String.format("no more data: expected %d lines, actually received %d",
						atLeast, replies.size()));
			}
		}
		return replies;
	}

	protected String receiveOne(BufferedReader reader) throws IOException {
		return receiveAtLeast(reader, 1).poll();
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
			return get(joinTimeout * joinCount, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		for (int k = 0; k < joinCount && !isDone(); k++) {
			Thread.sleep(joinTimeout);
		}
		return null;
	}

}
