package com.sjl.async;

import java.util.concurrent.*;

public class ResponseTimeSLA implements ServiceLevelAgreement {

	public ResponseTimeSLA nanosFromNow(long aNanos) {
		return new ResponseTimeSLA(aNanos).start();
	}
	
	public ResponseTimeSLA nanosFromStart(long aNanos) {
		return new ResponseTimeSLA(aNanos);
	}
	
	private long slaTime;
	private long startTime;
	
	private ResponseTimeSLA(long aNanos) {
		slaTime = aNanos;
	}
	
	@Override
	public boolean isExceeded() {
		return timeRemaining() > 0;
	}

	@Override
	public <T> T get(Future<T> aFuture) 
	throws ExecutionException, InterruptedException, TimeoutException {
		return aFuture.get(timeRemaining(), TimeUnit.NANOSECONDS);
	}

	public ResponseTimeSLA start() {
		startTime = System.nanoTime();
		return this;
	}
	
	private long timeRemaining() {
		return slaTime - (System.nanoTime() - startTime);
	}
}
