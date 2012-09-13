# Implicit Futures in Java

Java has [_explicit_ futures](http://en.wikipedia.org/wiki/Futures_and_promises) courtesy of Doug Lea's java.util.concurrent.Future&lt;T>. Future is a very cool piece of work, but usually you don't really want to scatter Future&lt;T> references through all of the client code that actually only cares about the &lt;T> part. 

In other words what you want is an _implicit_ future, where the client code doesn't know or care whether it is dealing with a fully realised &lt;T> or an implementation that may actually be working in the background to fulfill the promise of &lt;T>.

Just as with explicit futures, an _implicit_ future should block the calling thread if its value is not yet realised, and return when the value is available. The calling thread doesn't need to know anything about this.

You can create implicit futures manually by implementing the interface T with a proxy that delegates to the explicit future via its `T get()` method. This is tedious and error-prone, so it would be nice if you could instead have some machinery that automatically did that proxying for you. 

At its most basic, the following code is actually all you need. It is included in the implicit-futures library, but if this is the _only_ part you want you can avoid the additional dependency by adding this code to your code-base directly:

	package com.sjl.async;

	import java.lang.reflect.*;
	import java.util.concurrent.*;

	public class ImplicitFuture {

		@SuppressWarnings("unchecked")
		public static <T> T create(final Future<T> anExplicit, Class<T> aClass) {
			return (T) Proxy.newProxyInstance(
				aClass.getClassLoader(), 
				new Class<?>[]{aClass}, 
				new InvocationHandler() {
					public Object invoke(
						Object aProxy, final Method aMethod, final Object[] anArgs) 
						throws Throwable {
						try {
							return aMethod.invoke(anExplicit.get(), anArgs);
						} catch (InvocationTargetException anExc) {
							throw anExc.getCause();
						}
					}
				});
		}
	}

Now to convert an explicit future to an implicit, simply pass it through the `create` method like this:

	ExecutorService _pool = Executors.newFixedThreadPool(5);
	Future<MyResult> _f = _pool.submit(new Callable<MyResult>(){ .. });
	MyResult _implicit = ImplicitFuture.create(_f);
	
Now we have an object of type `MyResult` which we can pass to collaborators while its actual value is still being realised by the explicit future it wraps.

### House-keeping

Sometimes its helpful to be able to do a little more housekeeping, for example defining how to handle exceptions raised during the realisation of the Implicit Future. 

To do that we need to be pass a more prescriptive type when creating the implicit-future, which adds some handler methods. That might start to look something like this:

	interface PromissoryService {
    	public <T> T promise(Fulfilment<T> aPromise);
	}
	
	interface Fulfilment<T> {
	    public Class<T> getResultType();
    	public T execute();
    	public T createDefaultResult();
    	public void onException(Exception anExc);
	}
	
Now when we want to issue an implicit future in response to a method call, the code to calculate and return that future would look something like:

    class SomeService {
        PromissoryService promissoryService = ..;
    
        public MyResult provideResult() {
            return promissoryService.promise(new Fulfilment<T> aPromise) {
                // .. 
            }
        }
    }
    
With this we can return an object of the desired type which is a transparent implicit future, and we can deal with exceptions raised during the realisation of the promised result.

When you create a PromissoryService you will pass it an ExecutorService to which it can submit work to realise its implicit futures. This allows you to control the way in which threads are created and managed.

## Service Level Agreements

Java's explicit Future class has realisation methods that allow the caller to give up after waiting a specified length of time for the result to be realised. We can use this to implement SLA's for our implicit futures and realise them with default values if the SLA is breached.

We could do this with an additional method on `PromissoryService` that accepts an SLA parameter:

	interface PromissoryService {
    	public <T> T promise(Fulfilment<T> aPromise);
    	public <T> T attempt(Fulfilment<T> aPromise, ServiceLevelAgreement anSLA);
	}
	
	interface ServiceLevelAgreement {
	    public boolean isBreached();
	    public <T> T get(Future<T> aFuture);
	}
	
A simple response-time SLA implementation is provided - `ResponseTimeSLA`. SLA's can be used for multiple `attempt` invocations, which allows you to coordinate several concurrent activities.

Some or all of the activities may complete within the SLA response time. Those that do not complete within the SLA will "breach" and return default results.

    interface A {  
        // ..
    }

    interface B {  
        // ..
    }
    
    interface C {  
        // ..
    }

	class Calculator {
	    PromissoryService promissory = ..;
	
	    public CombinedResult calculate() {
	        ServiceLevelAgreement _sla = 
	        	ResponseTimeSLA.nanosFromNow(Nanoseconds.fromSeconds(2L));
	        	
	        A _a = promissory.attempt(calculateA(), _sla);
	        B _b = promissory.attempt(calculateB(), _sla);
	        C _c = promissory.attempt(calculateC(), _sla);
	        	
	        return new CombinedResult(_a, _b, _c);
	    }
	    
	    private Fulfilment<A> calculateA() {
	        return new FulfilmentAdapter<A>() { 
	            public A execute() {
	                // expensive calculations to realise A here..
	                return _a;
	            }
            };
	    }
	    
	    private Fulfilment<B> calculateB() {
	        return new FulfilmentAdapter<B>() {
	            public B execute() {
	                // expensive calculations to realise B here..
	                return _b;
	            }
            };
	    }
	    
	    private Fulfilment<C> calculateC() {
	        return new FulfilmentAdapter<C>() { 
	            public C execute() {
	                // expensive calculations here..
	                return _c;
	            }
            };
	    }
	}

## Asyncification

This is still experimental. We want to be able to remove even more of the boiler plate from converting a synchronous method to return implicit futures. Imagine if we had another service:

	interface AsyncificationService {
	    public <T> T makeAsync(T aT);
	}
	
Pushing an object through makeAsync would wrap it in a proxy which automagically fulfils some or all of its methods via the PromissoryService (and hence with implicit futures).

Deciding which methods to asyncify can be guided by use of the annotation `@ComputationallyIntensive`.

Rewriting the `Calculator` example from above in terms of AsyncificationService:

	interface Calculator {
	    public CombinedResult calculate();
	    @ComputationallyIntensive public A calculateA();
	    @ComputationallyIntensive public B calculateB();
	    @ComputationallyIntensive public C calculateC();
	}
	
	class SynchronousCalculator implements Calculator {
	
	    public CombinedResult calculate() {
	        return new CombinedResult(
	        	calculateA(), calculateB(), calculateC());
	    }
	    
	    public A calculateA() {
	        // expensive computations here..
	        return _a;
	    }
	    
	    public B calculateB() {
	    	// expensive computations here..
	        return _b;
	    }
	    
	    public C calculateC() {
	        // expensive computations here..
	        return _c;
	    }
    }
    
.. and to get hold of, and invoke, an asynchronised version of Calculator:

    AsyncificationService _service = ..;
    Calculator _sync = new SynchronousCalculator();
    Calculator _async = _async.makeAsync(_sync);
    
    // calculations all occur synchronously, all results
    // fully realised before this method call returns.
    // Returned object is a "normal" pojo.
    CombinedResult _r1 = _sync.calculate(); 
                       
    // calculations occur asynchronously, this method
    // call returns almost immediately and probably
    // before any of the calculations are completed.
    // Returned object is a pojo containing 3 implicit
    // futures (A, B and C)
    CombinedResult _r2 = _async.calculate(); 

## Cautionary Note

Java is not Erlang. Java threads are pretty heavy-weight. There is overhead entailed by context-switching. You do not want to go making every single method of every single class return implicit futures just because you can - exercise judgement and return futures only when appropriate.

## Current Implementation

The current implementations of `ImplicitFuture`, `PromissoryService` and `AsyncificationService` use dynamic proxying. This has some downsides:

1. Only interfaces can be proxied, so you can only promise to fulfill return-types which are interfaces, and you can only asyncify methods which implement methods of an interface.
2. Creating the first proxy for any class entails some overhead.
3. Invocation of proxied methods is by reflection, hence some (small) overhead per invocation.

At least (1) and (3) can be overcome with an implementation that uses runtime class generation in favour of java's built-in dynamic proxying - for example using CGLib or Javassist.

## Maven

Maven repository:

	<repository>
    	<id>sjl-github</id>
    	<name>steveliles github repo</name>
    	<url>http://steveliles.github.com/repository</url>
	</repository>
	
Maven dependency (latest):

	<dependency>
    	<groupId>com.sjl</groupId>
    	<artifactId>implicit-futures</artifactId>
    	<version>1.0-SNAPSHOT</artifactId>
	</dependency>