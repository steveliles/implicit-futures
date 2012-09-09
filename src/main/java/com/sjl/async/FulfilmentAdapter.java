package com.sjl.async;

public abstract class FulfilmentAdapter<T> implements Fulfilment<T>
{
    private Class<T> resultType;
    private T defaultResult;

    public FulfilmentAdapter(Class<T> aResultType)
    {
        this(aResultType, null);
    }
    
    public FulfilmentAdapter(Class<T> aResultType, T aDefaultResult)
    {
        resultType = aResultType;
        defaultResult = aDefaultResult;
    }
     
    @Override
    public Class<T> getResultType()
    {
        return resultType;
    }
    
    @Override
    public T createDefaultResult()
    {
        return defaultResult;
    }

    @Override
    public void onException(Exception anExc)
    {       
    }

    @Override
    public void onBreachSLA(ServiceLevelAgreement anSLA)
    {
    }
}
