package com.sjl.async;

public class DynamicProxyAsyncificationService implements AsyncificationService {

	private PromissoryService promissory;
	
	public DynamicProxyAsyncificationService(PromissoryService aPromissory) {
		promissory = aPromissory;
	}
	
	@Override
	public <T> T makeAsync(T aT) {
		return aT; // no-op, currently
	}
	
}
