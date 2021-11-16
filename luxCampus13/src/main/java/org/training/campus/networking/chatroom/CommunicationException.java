package org.training.campus.networking.chatroom;

public class CommunicationException extends RuntimeException {

	public CommunicationException(String message) {
		super(message);
	}

	public CommunicationException(Exception e) {
		super(e);
	}

}
