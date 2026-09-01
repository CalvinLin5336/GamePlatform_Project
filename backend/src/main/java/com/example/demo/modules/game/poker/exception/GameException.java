package com.example.demo.modules.game.poker.exception;

public class GameException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final String code;
    public GameException(String code, String message) { super(message); this.code = code; }
    public String getCode() { return code; }
}
