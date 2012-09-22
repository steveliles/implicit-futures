package com.sjl.async;

import java.util.concurrent.*;

/**
 * Wrap a Future<T> such that attempts to get() the result will be
 * made within a Service-Level Agreement. 
 * 
 * If the first call to get() fails to return within the SLA the future
 * will be cancelled and ensuing calls to get() will return immediately
 * with the "otherwise" result.
 * 
 * There are 3 template methods you can override:
 * 
 * whenSLAExceeded() allows the creator to perform one-time operations if
 * the SLA is not met (for example logging said failure).
 * 
 * whenExecutionException(anExc) allows the creator to perform one-time
 * operations if an exception occurs during processing of the nested Future.
 * 
 * createDefaultResult() allows the creator to specify a return value 
 * that should be used if the Future does not complete within the given SLA.
 * 
 * @author steve
 *
 * @param <T> the return type of the future
 */
public class FutureWithSLA<T>
{
    private Future<T> future;    
    private ServiceLevelAgreement sla;
    
    private T result;    
    
    public FutureWithSLA(Future<T> aFuture, ServiceLevelAgreement anSLA)
    {
        future = aFuture;
        sla = anSLA;
    }
    
    public T get()
    {
        try
        {
            if (result != null)
                return result;
            
            if (future.isCancelled())
                return getDefaultResult();
            
            if (sla.isExceeded())            
                timeout();      
            else            
                result = sla.get(future);
            
            return (result != null) ? 
                result : getDefaultResult();
        }
        catch (TimeoutException anExc)
        {
            timeout();
            return result;
        }
        catch (InterruptedException anExc)
        {
            Thread.interrupted(); // clear interrupt
            return getDefaultResult();
        }
        catch (CancellationException anExc)
        {            
            return getDefaultResult();
        }
        catch (ExecutionException anExc)
        {            
            whenExecutionException(anExc.getCause());
            return getDefaultResult();
        }        
    }
    
    private T getDefaultResult()
    {
        if (result == null)
            result = createDefaultResult();
        
        return result;
    }
    
    private void timeout()
    {
        future.cancel(true);
        whenSLAExceeded();
        result = getDefaultResult();
    }
    
    protected void whenSLAExceeded()
    {        
    }
    
    protected void whenExecutionException(Throwable anExc)
    {        
    }       
    
    protected T createDefaultResult()
    {
        return null;
    }
}
