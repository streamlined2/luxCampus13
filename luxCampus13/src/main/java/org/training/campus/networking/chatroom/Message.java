package org.training.campus.networking.chatroom;

import java.time.LocalDateTime;

public record Message(String author, LocalDateTime stamp, String note) {

}
