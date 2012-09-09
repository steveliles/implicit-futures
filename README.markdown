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

