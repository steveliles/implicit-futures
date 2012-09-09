package com.sjl.async;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

public class DynamicProxyPromissoryService implements PromissoryService
{
    private ExecutorService executor;
    
    public DynamicProxyPromissoryService(ExecutorService anExecutor)
    {
        executor = anExecutor;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T promise(final Fulfilment<T> aPromise)
    throws RejectedExecutionException
    {
        checkSatisfiable(aPromise);
        
        final FutureWithSLA<T> _f = new FutureWithSLA<T>(
            executor.submit(newCallableTask(aPromise)), ServiceLevelAgreement.NULL_OBJECT)
        {
            @Override
            protected void whenExecutionException(Exception anExc)
            {
                aPromise.onException(anExc);
            }

            @Override
            protected T createDefaultResult()
            {
                return aPromise.createDefaultResult();
            }            
        };
        
        return (T) Proxy.newProxyInstance(
            getClassLoader(aPromise.getResultType()), 
            new Class<?>[]{ aPromise.getResultType() }, 
            new InvocationHandler()
        {
            @Override
            public Object invoke(Object aProxy, Method aMethod, Object[] aArgs) throws Throwable
            {
                return _f.get();
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T attempt(final Fulfilment<T> aPromise, final ServiceLevelAgreement anSLA)
    throws RejectedExecutionException
    {
        checkSatisfiable(aPromise);
        
        final FutureWithSLA<T> _f = new FutureWithSLA<T>(executor.submit(newCallableTask(aPromise)), anSLA)
        {
            @Override
            protected void whenSLAExceeded()
            {
                aPromise.onBreachSLA(anSLA);
            }

            @Override
            protected void whenExecutionException(Exception anExc)
            {
                aPromise.onException(anExc);
            }

            @Override
            protected T createDefaultResult()
            {
                return aPromise.createDefaultResult();
            }            
        };
    
        return (T) Proxy.newProxyInstance(
            getClassLoader(aPromise.getResultType()), 
            new Class<?>[]{ aPromise.getResultType() }, 
            new InvocationHandler()
        {
            @Override
            public Object invoke(Object aProxy, Method aMethod, Object[] aArgs) throws Throwable
            {
                return aMethod.invoke(_f.get(), aArgs);
            }            
        });
    }

    private <T> void checkSatisfiable(final Fulfilment<T> aTask)
    throws RejectedExecutionException
    {
        if (!aTask.getResultType().isInterface())
            throw new RejectedExecutionException(
                "this imlementation is only able to satisfy interfaces as return-types");
    }    

    private ClassLoader getClassLoader(Class<?> aClass)
    {
        return (aClass != null) ? aClass.getClassLoader() : getClass().getClassLoader();
    }
    
    private <T> Callable<T> newCallableTask(final Fulfilment<T> aTask)
    {
        return new Callable<T>() 
        {
            @Override
            public T call() throws Exception
            {
                return aTask.execute();
            }            
        };
    }
}
