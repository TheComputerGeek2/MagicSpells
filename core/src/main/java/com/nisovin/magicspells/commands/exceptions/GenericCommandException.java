package com.nisovin.magicspells.commands.exceptions;

public class GenericCommandException extends RuntimeException {

	public GenericCommandException(String message) {
		super(message);
	}

	public GenericCommandException(String message, Throwable cause) {
		super(message, cause);
	}

}
