class Examples() {
	
	//Be opaque
	//Create inner class
	//Create interface with impl methods
	//Box
	
	class BadLinkedListNode<Element>() {
		@error shared variable Element? next = null;
		@error shared variable Element? prev = null;
	}
	
	mutable class GoodLinkedList<opaque Element>() {
		shared variable Element? next = null;
		shared variable Element? prev = null;
	}
	
	interface ExampleApi {
		shared formal void mutate(Integer something, Integer somethingElse);
		shared formal void longBlockingCall(Integer timeout);
	}
	
	class InnerObjectExample() {
		mutable object state {
			shared variable Integer foo = 0;
			shared variable Integer bar = 0;
		}	
		Integer blockAndUpdateState(ExampleApi api, Integer newFoo) {
			api.longBlockingCall(newFoo);
			Integer oldFoo = state.foo;
			state.foo = newFoo;
			api.mutate(state.foo, state.bar);
			return oldFoo;
		}
	}
	
	class ExampleApiImpl() satisfies ExampleApi {
		value current = Box<Integer>(0);
		shared actual void mutate(Integer something, Integer somethingElse) {
			//...
			current.set(something);
			//...
		}
		shared actual void longBlockingCall(Integer timeout) {
			//...
		}
	}
	
	class BadRectangle(width, height) satisfies Scalable<Float,BadRectangle> {
		@error shared variable Float width;
		@error shared variable Float height;
		string => "Rectangle ``width`` by ``height``";
		shared actual Boolean equals(Object other) {
			if (is BadRectangle other) {
				return other.width==width && other.height==height;
			}
			return false;
		}
		shared actual BadRectangle scale(Float d) => BadRectangle(width * d, height * d);
	}
	
	interface Rectangle {
		shared formal Float width;
		shared formal Float height;
		shared actual String string => "Rectangle ``width`` by ``height``";
		shared actual Integer hash => width.hash*31 + height.hash;
		shared actual Boolean equals(Object other) {
			if (is Rectangle other) {
				return other.width==width && other.height==height;
			}
			return false;
		}
	}
	mutable class ScalableRectangle(width, height) extends Object() satisfies Scalable<Float,ScalableRectangle> & Rectangle {
		shared actual variable Float width;
		shared actual variable Float height;
		shared actual ScalableRectangle scale(Float d) => ScalableRectangle(width * d, height * d);
	}

	class BadCopyingList<T>() satisfies List<T> {
		@error variable List<T> impl = [];
		shared actual Integer? lastIndex => impl.lastIndex;
		shared actual T? getFromFirst(Integer index) {
			return impl.getFromFirst(index);
		}
		shared void add(T element) {
			impl = [element, *impl];
		}
		shared actual Boolean equals(Object that) => (super of List).equals(that);
		shared actual Integer hash => impl.hash;
	}
	
	mutable class GoodCopyingList<opaque T>() extends Object() satisfies List<T> {
		variable List<T> impl = [];
		shared actual Integer? lastIndex => impl.lastIndex;
		shared actual T? getFromFirst(Integer index) {
			return impl.getFromFirst(index);
		}
		shared void add(T element) {
			impl = [element, *impl];
		}
	}
	
}
