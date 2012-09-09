package com.sjl.async;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@SuppressWarnings("unchecked")
public class DynamicProxyAsyncificationService implements AsyncificationService {

	private PromissoryService promissory;
	
	public DynamicProxyAsyncificationService(PromissoryService aPromissory) {
		promissory = aPromissory;
	}
	
	@Override
	public <T> T makeAsync(final T aT) {
		return (T) Proxy.newProxyInstance(
	            aT.getClass().getClassLoader(),
	            aT.getClass().getInterfaces(),
	            new InvocationHandler()
	        {
	            @Override
	            public Object invoke(Object aProxy, final Method aMethod, final Object[] anArgs) throws Throwable
	            {
	            	if (aMethod.isAnnotationPresent(ComputationallyIntensive.class)) {
	            		return promise(aT, aMethod, anArgs);
	            	} else {
	            		return DynamicProxyAsyncificationService.this.invoke(aT, aMethod, anArgs);
	            	}
	            }
	        });
	}
	
	private <T> T promise(final T aT, final Method aMethod, final Object[] anArgs) {
		return promissory.promise(new FulfilmentAdapter<T>((Class<T>)aT.getClass()) {
			@Override
			public T execute() {
				return invoke(aT, aMethod, anArgs);
			}
		});
	}
	
	private <T> T invoke(T aT, Method aMethod, Object[] anArgs) {
		try {
			return (T) aMethod.invoke(aT, anArgs);
		} catch (RuntimeException anExc) {
			throw anExc;
		} catch (IllegalAccessException anExc) {
			throw new RuntimeException(anExc);
		} catch (InvocationTargetException anExc) {
			throw new RuntimeException(anExc.getCause());
		}
	}
}
