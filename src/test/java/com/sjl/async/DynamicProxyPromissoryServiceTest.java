package com.sjl.async;

import java.util.concurrent.*;

import org.jmock.*;
import org.junit.*;

public class DynamicProxyPromissoryServiceTest {

	private Mockery ctx;

	private ExecutorService executorService;
	private ReturnType syncResult;
	private Fulfilment<ReturnType> fulfilment;

	private PromissoryService promissory;

	@SuppressWarnings("unchecked")
	@Before
	public void setup() {
		ctx = new Mockery();

		executorService = Executors.newSingleThreadExecutor();
		
		fulfilment = ctx.mock(Fulfilment.class, "fulfilment");
		syncResult = ctx.mock(ReturnType.class, "sync");

		promissory = new DynamicProxyPromissoryService(executorService);
	}
	
	@After
	public void teardown(){
		ctx.assertIsSatisfied();
	}
	
	@Test
	public void defaultValueReturnedWhenExecutionException() throws Exception {
		final Exception exception = new Exception();
		
		ctx.checking(new Expectations() {{
			allowing(fulfilment).getResultType(); will(returnValue(ReturnType.class));
			oneOf(fulfilment).execute(); will(throwException(exception));
			oneOf(fulfilment).onException(with(exception));
			
			oneOf(fulfilment).createDefaultResult(); will(returnValue(syncResult));
			oneOf(syncResult).getValue1(); will(returnValue("hello"));
		}});
		
		ReturnType _result = promissory.promise(fulfilment);
		_result.getValue1();
	}
	
	interface ReturnType {
		String getValue1();
		String getValue2();
	}

	interface Service {
		ReturnType doSomething();
	}
}
