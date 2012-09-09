package com.sjl.async;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class DynamicProxyAsyncificationService implements AsyncificationService {

	private PromissoryService promissory;
	
	public DynamicProxyAsyncificationService(PromissoryService aPromissory) {
		promissory = aPromissory;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T makeAsync(final T aT) {
		return (T) Proxy.newProxyInstance(
	            aT.getClass().getClassLoader(),
	            aT.getClass().getInterfaces(),
	            new InvocationHandler()
	        {
	            @Override
	            public Object invoke(Object aProxy, final Method aMethod, final Object[] aArgs) throws Throwable
	            {
	            	return promissory.promise(new FulfilmentAdapter<T>((Class<T>)aT.getClass()) {
						@Override
						public T execute() {
							try {
								return (T) aMethod.invoke(aT, aArgs);
							} catch (RuntimeException anExc) {
								throw anExc;
							} catch (IllegalAccessException anExc) {
								throw new RuntimeException(anExc);
							} catch (InvocationTargetException anExc) {
								throw new RuntimeException(anExc.getCause());
							}
						}
					});
	            }
	        });
	}
	
}
