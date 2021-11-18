package org.training.campus.networking.chatroom;

import java.time.LocalDateTime;
import java.util.Objects;

public record Message(String author, LocalDateTime stamp, String note) {

	@Override
	public boolean equals(Object o) {
		if (o instanceof Message m) {
			return Objects.equals(author, m.author) && Objects.equals(stamp, m.stamp);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(author, stamp);
	}

	@Override
	public String toString() {
		return String.format("%s(%tr): %s", author, stamp, note);
	}

}
