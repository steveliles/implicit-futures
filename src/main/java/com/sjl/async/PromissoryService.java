package com.sjl.async;

import java.util.concurrent.RejectedExecutionException;

public interface PromissoryService {
    /**
     * A means to asynchronously execute a promise with a given return-type, where the returned object may
     * be an Implicit Future hiding that execution is happening asynchronously.
     * 
     * @param a promise to fulfill
     * @return An instance of the return-type, which may be a disguised Future that hides the fact that 
     *         execution is happening asynchronously.
     * @throws RejectedExecutionException if the implementation is not able to execute this task.
     */
    public <T> T promise(Fulfilment<T> aPromise)
    throws RejectedExecutionException;
    
    /**
     * A means to asynchronously fulfill a promise with a given return-type, specifying a default result
     * to return under error conditions or failure to complete within the given SLA. The returned object
     * may be an implicit future hiding that execution is happening asynchronously.
     * 
     * @param a Service-Level Agreement to complete within
     * @param a promised to fulfill
     * @return An instance of the return-type, which may be a disguised Future that hides the fact that 
     *         execution is happening asynchronously.
     * @throws RejectedExecutionException if the implementation is not able to execute this task.
     */
    public <T> T attempt(Fulfilment<T> aPromise, ServiceLevelAgreement anSLA)
    throws RejectedExecutionException;
}
