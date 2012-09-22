package com.sjl.async;

public interface Fulfilment<T>
{
    public Class<T> getResultType();
    
    public T execute() throws Exception;
    
    public T createDefaultResult();
    
    public void onException(Throwable anExc);
    
    public void onBreachSLA(ServiceLevelAgreement anSLA);
}
