# Implicit Futures in Java

Java has [_explicit_ futures](http://en.wikipedia.org/wiki/Futures_and_promises) courtesy of Doug Lea's java.util.concurrent.Future&lt;T>. Future is a very cool piece of work, but usually you don't really want to scatter Future&lt;T> references through all of the client code that actually only cares about the &lt;T> part. 

In other words what you want is an _implicit_ future, where the client code doesn't know or care whether it is dealing with a fully realised &lt;T> or an implementation that may actually be working in the background to fulfill the promise of &lt;T>.

	class Undesirable {
	    public void doSomething(SomeService aService) {
	        Future&lt;MyResult> _future = aService.provideResult();
	       
	        // .. do some other stuff
	       
	        MyResult _actual = _r.get();
	       
	        // .. use the actual MyResult
	    }
	}
	
    class Desirable {
        public void doSomething(SomeService aService) {
	        MyResult _r = aService.provideResult();
	       
	        // .. do some other stuff
	       
	        // .. use MyResult
	    }
    }

The full impact of the difference doesn't really show until you want to pass around MyResult to other collaborators of the above classes. The further MyResult travels from the original call site, the more desirable implicit future's become.

You can achieve this manually by implementing the interface T with a proxy that delegates to the Future via its `T get()` method. This is tedious and error-prone, so it would be nice if you could instead have some machinery that automatically did that proxying for you. Perhaps something like this:

	interface ProxyingService {
	    public <T> T promise(Future<T> aFuture);
	}
	
Now we could rewrite `Undesirable` as:

	class Undesirable {
	    ProxyingService proxyingService = ..;
	
	    public void doSomething(SomeService aService) {
	        MyResult _r = promissoryService.promise(aService.provideResult());
	       
	        // .. do some other stuff
	       
	        // .. use MyResult
	    }
	}
	
Even better, we can implement `SomeService` to use the `ProxyingService`, so that `Undesirable` doesn't know anything about it, at which point it will look _exactly_ like `Desirable`.

We actually need to be able to do a little more housekeeping, for example defining how to handle exceptions raised during the realisation of the Implicit Future. To do that we need to be pass a more prescriptive type to the proxying service which adds some handler methods. That might start to look something like this:

	interface PromissoryService {
    	public <T> T promise(Fulfilment<T> aPromise);
	}
	
	interface Fulfilment<T> {
	    public Class<T> getResultType();
    	public T execute();
    	public T createDefaultResult();
    	public void onException(Exception anExc);
	}
	
Now when we want to issue an Implicit Future in response to a method call (lets use the SomeService example again), the code to calculate and return that promise would look something like:

    class SomeService {
        PromissoryService promissoryService = ..;
    
        public MyResult provideResult() {
            return promissoryService.promise(new Fulfilment<T> aPromise) {
                // .. 
            }
        }
    }
    
Not bad, we can return an object of the desired type which is a transparent Implicit Future, and we can deal with exceptions raised during the realisation of the promised result.

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

## Asyncification

Still thinking about this bit ..

	interface AsyncificationService {
	    public <T> T makeAsync(T aT);
	}
	
.. pushing an object through makeAsync would wrap it in a proxy which automagically fulfils some or all of its methods via the PromissoryService (and hence with implicit futures).

Deciding which methods to asyncify might be guided by annotations - @Async/NotAsync? @Promissable? @ComputationallyIntensive?

## Cautionary Note

Java is not Erlang. Java threads are pretty heavy-weight. There is overhead entailed by context-switching. You do not want to go making every single method of every single class return implicit futures just because you can - exercise judgement and return futures only when appropriate.

## Current Implementation

The only current implementation of PromissoryService uses dynamic proxying. This has some downsides:

1. Only interfaces can be proxied, so you can only promise to fulfill return-types which are interfaces.
2. Creating the first proxy for any class entails some overhead.
3. Invocation of proxied methods is by reflection, hence some (small) overhead per invocation.

At least (1) and (3) can be overcome with an implementation that uses runtime class generation in favour of java's built-in dynamic proxying - for example using CGLib or Javassist.