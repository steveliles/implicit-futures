package com.sjl.async;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

public interface ServiceLevelAgreement {
	 
	public static final ServiceLevelAgreement NULL_OBJECT = new ServiceLevelAgreement() {
        @Override
        public boolean isExceeded()
        {
            return false;
        }

		@Override
		public <T> T get(Future<T> aFuture)
		throws ExecutionException, InterruptedException {
			return aFuture.get();
		}        
    };
	
    public boolean isExceeded();
    
    public <T> T get(Future<T> aFuture)
    throws ExecutionException, InterruptedException, TimeoutException; 
}
