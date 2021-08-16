package it.unimi.twitter.tagger.configuration;

import java.util.concurrent.ArrayBlockingQueue;

import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class TwitterStreamedData<T> extends ArrayBlockingQueue<T> {

	private static final int RING_BUFFER_SIZE = 10;
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int capacity;

	public TwitterStreamedData() {
		this(RING_BUFFER_SIZE);
	}

	public TwitterStreamedData(int capacity) {
		super(capacity);
		this.setCapacity(capacity);
	}
	
	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}
	
}
