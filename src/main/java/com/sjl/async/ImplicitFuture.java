package com.sjl.async;

import java.lang.reflect.*;
import java.util.concurrent.*;

public class ImplicitFuture {

	@SuppressWarnings("unchecked")
	public static <T> T create(final Future<T> anExplicit, Class<T> aClass) {
		return (T) Proxy.newProxyInstance(aClass.getClassLoader(),
			new Class<?>[] { aClass }, new InvocationHandler() {
				public Object invoke(Object aProxy, final Method aMethod, final Object[] anArgs) 
				throws Throwable {
					try {
						return aMethod.invoke(anExplicit.get(), anArgs);
					} catch (InvocationTargetException anExc) {
						throw anExc.getCause();
					}
				}
			});
	}
}