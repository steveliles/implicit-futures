package com.sjl.async;

import org.jmock.*;
import org.junit.*;

public class DynamicProxyAsyncificationServiceTest {

	private Mockery ctx;
	private PromissoryService promissory;
	private AsyncificationService async;
	
	private Service synchronous;
	private ReturnType1 firstResult;
	private ReturnType2 secondResult;
	private ReturnType3 thirdResult;
	
	@Before
	public void setup() {
		ctx = new Mockery();
		
		promissory = ctx.mock(PromissoryService.class);		
		synchronous = ctx.mock(Service.class);
		firstResult = ctx.mock(ReturnType1.class);
		secondResult = ctx.mock(ReturnType2.class);
		thirdResult = ctx.mock(ReturnType3.class);
		
		async = new DynamicProxyAsyncificationService(promissory);
	}
	
	@After
	public void tearDown() {
		ctx.assertIsSatisfied();
	}
		
	@SuppressWarnings("unchecked")
	@Test
	public void createsImplicitFuturesAroundAllMethodReturnsMarkedComputationallyExpensive() {
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
	
	@Test
	public void invokesUnmarkedMethodsSynchronously() {
		ctx.checking(new Expectations() {{
			oneOf(synchronous).third(); will(returnValue(thirdResult));
			oneOf(thirdResult).getValue3(); will(returnValue("third"));
		}});
		
		Service _asynchronised = async.makeAsync(synchronous);
		ReturnType3 _r3 = _asynchronised.third();
		Assert.assertEquals("third", _r3.getValue3());
	}
	
	@Test
	public void createsFulfilmentsThatCaptureExceptionsForDelayedPropagation() {
		// capture the fulfilment, invoke onException
		// then invoke createDefaultResult,
		// check that createDefaultResult throws our exception
		// wrapped in a RuntimeExc if necessary
	}
	
	interface ReturnType1 {
		public String getValue1();
	}
	
	interface ReturnType2 {
		public String getValue2();
	}
	
	interface ReturnType3 {
		public String getValue3();
	}
	
	interface Service {
		@ComputationallyIntensive
		public ReturnType1 first();
		
		@ComputationallyIntensive
		public ReturnType2 second();
		
		public ReturnType3 third();
	}
}
