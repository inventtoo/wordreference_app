package com.example.wordreferenceapp;

public class CombinationNotAvailableException extends Exception {
	public CombinationNotAvailableException() {
		this("");
	}
	
	public CombinationNotAvailableException(String msg) {
		super(msg);
	}
};
