package org.training.campus.networking.chatroom;

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
