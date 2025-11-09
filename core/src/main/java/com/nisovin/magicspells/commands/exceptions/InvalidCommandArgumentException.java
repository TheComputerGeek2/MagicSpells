package com.nisovin.magicspells.commands.exceptions;

public class InvalidCommandArgumentException extends RuntimeException {

	public InvalidCommandArgumentException(String message) {
		super(message);
	}

	public InvalidCommandArgumentException(String message, Throwable cause) {
		super(message, cause);
	}

}