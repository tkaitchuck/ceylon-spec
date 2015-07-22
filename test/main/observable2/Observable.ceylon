mutable class CopyingList<opaque T>() extends Object() satisfies List<T> {
	variable List<T> impl = [];
	shared actual T? getFromFirst(Integer index) {
		return impl.getFromFirst(index);
	}
	shared void add(T element) {
		impl = [element, *impl];
	}
	shared actual Integer? lastIndex => impl.lastIndex;
}

shared class Observable<Event>() 
        given Event satisfies Object {
    value listeners = CopyingList<Anything(Nothing)>();
    
    shared void addObserver<ObservedEvent>
            (void handle(ObservedEvent event))
            given ObservedEvent satisfies Event
            => listeners.add(handle);
    
    shared void raise<RaisedEvent>(RaisedEvent event)
            given RaisedEvent satisfies Event
            => listeners.narrow<Anything(RaisedEvent)>()
            .each((handle) => handle(event));
    
}

object observable
        extends Observable<String|Integer|Float>() {
    shared void hello() {
        raise("hello world");
    }
    shared void foo() {
        raise(10);
    }
    shared void bar() {
        raise(0.0);
    }
}

shared void runit() {
    observable.addObserver((String string) => print(string));
    observable.addObserver((Integer integer) => print(integer*integer + integer));
    observable.addObserver((Integer|Float event) => print("NUMBER"));
    
    observable.hello();
    observable.foo();
    observable.bar();
}