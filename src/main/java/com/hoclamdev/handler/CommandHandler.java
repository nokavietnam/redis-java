package com.hoclamdev.handler;

import java.util.List;

public interface CommandHandler {
    String handle(List<String> command);
}
