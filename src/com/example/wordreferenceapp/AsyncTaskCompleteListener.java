package com.example.wordreferenceapp;

public interface AsyncTaskCompleteListener<T> {
	public void onTaskComplete(T result, ApplicationError error);
}
