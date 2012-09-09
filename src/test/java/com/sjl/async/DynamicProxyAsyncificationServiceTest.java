package com.sjl.async;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DynamicProxyAsyncificationServiceTest {

	private Mockery ctx;
	private PromissoryService promissory;
	private AsyncificationService async;
	
	private Service synchronous;
	private ReturnType1 firstResult;
	private ReturnType2 secondResult;
	
	@Before
	public void setup() {
		ctx = new Mockery();
		
		promissory = ctx.mock(PromissoryService.class);		
		synchronous = ctx.mock(Service.class);
		firstResult = ctx.mock(ReturnType1.class);
		secondResult = ctx.mock(ReturnType2.class);
		
		async = new DynamicProxyAsyncificationService(promissory);
	}
	
	@After
	public void tearDown() {
		ctx.assertIsSatisfied();
	}
		
	@SuppressWarnings("unchecked")
	@Test
	public void createsImplicitFuturesAroundAllMethodReturns() {
		ctx.checking(new Expectations() {{
			oneOf(promissory).promise(with(any(Fulfilment.class)));
			will(returnValue(firstResult));
			
			oneOf(promissory).promise(with(any(Fulfilment.class)));
			will(returnValue(secondResult));
			
			oneOf(firstResult).getValue1(); will(returnValue("first"));
			oneOf(secondResult).getValue2(); will(returnValue("second"));
		}});
		
		Service _asynchronised = async.makeAsync(synchronous);
		ReturnType1 _r1 = _asynchronised.first();
		ReturnType2 _r2 = _asynchronised.second();
		
		Assert.assertEquals("first", _r1.getValue1());
		Assert.assertEquals("second", _r2.getValue2());
	}
	
	interface ReturnType1 {
		public String getValue1();
	}
	
	interface ReturnType2 {
		public String getValue2();
	}
	
	interface Service {
		public ReturnType1 first();
		public ReturnType2 second();
	}
}
