package com.sjl.async;

public interface AsyncificationService {

	/**
	 * @param a type whose apparently synchronous methods should be made asynchronous 
	 * @return a T which may or may not return Implicit Future's from its methods. Neither T nor the
	 * call site need to be aware of the use of futures, threads or any other special constructs. 
	 */
	public <T> T makeAsync(T aT);
	
}
