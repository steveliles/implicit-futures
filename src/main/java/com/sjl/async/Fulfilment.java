package com.sjl.async;

public interface Fulfilment<T>
{
    public Class<T> getResultType();
    
    public T execute();
    
    public T createDefaultResult();
    
    public void onException(Exception anExc);
    
    public void onBreachSLA(ServiceLevelAgreement anSLA);
}
