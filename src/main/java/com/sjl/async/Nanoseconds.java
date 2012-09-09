package com.sjl.async;

public class Nanoseconds {

	public static final long fromMicroseconds(long aMicros) {
		return aMicros * 1000L;
	}
	
	public static final long fromMilliseconds(long aMillis) {
		return aMillis * 1000000L;
	}
	
	public static final long fromSeconds(long aSeconds) {
		return aSeconds * 1000000000L;
	}
	
	public static final long fromMinutes(long aMinutes) {
		return fromSeconds(aMinutes * 60);
	}
	
	public static final long fromHours(long anHours) {
		return fromMinutes(anHours * 60);
	}
	
}
