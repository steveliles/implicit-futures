package com.sjl.async;

public class DynamicProxyAsyncificationService implements AsyncificationService {

	@Override
	public <T> T makeAsync(T aT) {
		return aT; // no-op, currently
	}
	
}
