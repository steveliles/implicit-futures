package com.sjl.async;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FutureWithSLATest {
	private Mockery ctx;

	private Future<String> future;
	private ServiceLevelAgreement sla;

	interface Pokeable {
		public void poke();
	}

	@SuppressWarnings("unchecked")
	@Before
	public void setup() {
		ctx = new Mockery();

		future = ctx.mock(Future.class);
		sla = ctx.mock(ServiceLevelAgreement.class);
	}

	@After
	public void teardown() {
		ctx.assertIsSatisfied();
	}

	@Test
	public void invokesFutureGetWithAppropriateTimeout() throws Exception {
		ctx.checking(new Expectations() {{
			allowing(future).isCancelled(); will(returnValue(false));
			allowing(sla).isExceeded(); will(returnValue(false));
			oneOf(sla).get(future); will(invokeGet(future));

			oneOf(future).get(1000L, TimeUnit.MILLISECONDS); will(returnValue("hello"));
		}});

		FutureWithSLA<String> _fws = new FutureWithSLA<String>(future, sla);

		Assert.assertEquals("hello", _fws.get());
	}

	@Test
	public void cancelsFutureIfTimeoutExceededDuringExecution()
	throws Exception {
		ctx.checking(new Expectations() {{
			oneOf(future).isCancelled(); will(returnValue(false));
			allowing(sla).isExceeded(); will(returnValue(false));
			oneOf(sla).get(future); will(invokeGet(future));

			oneOf(future).get(1000L, TimeUnit.MILLISECONDS);
			will(throwException(new TimeoutException()));
			
			oneOf(future).cancel(true);
		}});

		FutureWithSLA<String> _fws = new FutureWithSLA<String>(future, sla);

		Assert.assertEquals(null, _fws.get());
	}

	@Test
	public void returnsDefaultValueIfTimeoutExceededDuringExecution()
	throws Exception {
		ctx.checking(new Expectations() {{
			oneOf(future).isCancelled(); will(returnValue(false));
			allowing(sla).isExceeded(); will(returnValue(false));
			oneOf(sla).get(future); will(invokeGet(future));

			oneOf(future).get(1000L, TimeUnit.MILLISECONDS);
			will(throwException(new TimeoutException()));
			oneOf(future).cancel(true);
		}});

		FutureWithSLA<String> _fws = new FutureWithSLA<String>(future, sla) {
			@Override
			protected String createDefaultResult() {
				return "hello";
			}
		};

		Assert.assertEquals("hello", _fws.get());
	}

	@Test
	public void defaultValueIsCreatedOnlyOnceAndReturnedManyTimes()
	throws Exception {
		ctx.checking(new Expectations() {{
			oneOf(future).isCancelled(); will(returnValue(false));
			allowing(sla).isExceeded(); will(returnValue(false));
			oneOf(sla).get(future); will(invokeGet(future));

			oneOf(future).get(1000L, TimeUnit.MILLISECONDS);
			will(throwException(new TimeoutException()));
			oneOf(future).cancel(true);
		}});

		FutureWithSLA<String> _fws = new FutureWithSLA<String>(future, sla) {
			@Override
			protected String createDefaultResult() {
				return new String(new Object().hashCode() + "");
			}
		};

		String _defaultResult = _fws.get();

		Assert.assertEquals(_defaultResult, _fws.get());
		Assert.assertEquals(_defaultResult, _fws.get());
	}

	@Test
	public void callsAfterInitialTimeoutExceededDoNotReWait() throws Exception {
		ctx.checking(new Expectations() {{
			oneOf(future).isCancelled(); will(returnValue(false));
			allowing(sla).isExceeded(); will(returnValue(false));
			oneOf(sla).get(future); will(invokeGet(future));

			oneOf(future).get(1000L, TimeUnit.MILLISECONDS);
			will(throwException(new TimeoutException()));
			oneOf(future).cancel(true);

			oneOf(future).isCancelled();
			will(returnValue(true));
		}});

		FutureWithSLA<String> _fws = new FutureWithSLA<String>(future, sla);

		Assert.assertEquals(null, _fws.get());
		Assert.assertEquals(null, _fws.get());
	}

	@Test
	public void allowsCallerToSpecifyErrorHandling() throws Exception {
		final Pokeable _pokeable = ctx.mock(Pokeable.class);

		ctx.checking(new Expectations() {{
			oneOf(future).isCancelled(); will(returnValue(false));
			allowing(sla).isExceeded(); will(returnValue(false));
			oneOf(sla).get(future); will(invokeGet(future));

			oneOf(future).get(1000L, TimeUnit.MILLISECONDS);
			will(throwException(new Exception()));
			oneOf(_pokeable).poke();
		}});

		FutureWithSLA<String> _fws = new FutureWithSLA<String>(future, sla) {
			@Override
			protected void whenExecutionException(Exception anExc) {
				_pokeable.poke();
			}
		};

		Assert.assertEquals(null, _fws.get());
	}

	@Test
	public void returnsDefaultValueIfFutureCalculationReturnsNull()
	throws Exception {
		ctx.checking(new Expectations() {{
			oneOf(future).isCancelled(); will(returnValue(false));
			allowing(sla).isExceeded(); will(returnValue(false));
			oneOf(sla).get(future); will(invokeGet(future));

			oneOf(future).get(1000L, TimeUnit.MILLISECONDS);
			will(returnValue(null));
		}});

		FutureWithSLA<String> _fws = new FutureWithSLA<String>(future, sla) {
			@Override
			protected String createDefaultResult() {
				return "hello";
			}
		};

		Assert.assertEquals("hello", _fws.get());
	}

	private <T> Action invokeGet(final Future<T> aFuture) {
		return new Action() {
			@Override
			public Object invoke(Invocation anInvocation) throws Throwable {
				return aFuture.get(1000L, TimeUnit.MILLISECONDS);
			}
			
			@Override
			public void describeTo(Description aDescription) {
			}
		};
	}
}
