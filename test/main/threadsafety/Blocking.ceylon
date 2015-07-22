
blocking void block() {	}

class Blocking() {
	blocking void sleep(Integer millis) {
		//Impl not actually needed.
	}
	
	interface BlockingMixin {
		blocking shared void invokeSleep() => block();
		blocking shared void forEach(Array<Integer> ints) {
			for (Integer i in ints) {
				print(number); //This is calling a synthetic getter, which is safe.
				invokeSleep();
				doSoemthing(i);
			}
		}
		shared formal void doSoemthing(Integer i);
		shared formal Integer number;
	}
	mutable class MutableImpl() satisfies BlockingMixin {
		shared variable actual Integer number = 0;
		shared actual void doSoemthing(Integer i) {
			number = i; //Cannot call invokeSleep() here, but in mixin above is OK.
		}
	}
	
	mutable class BlockingInnerclassClass() {
		variable Integer i = 0;
		shared object blockingInner {
			shared blocking void doSoemthing(Integer i) {
				sleep(i); //OK to invoke sleep here
				//referring to i is using a synthetic getter, which is safe.
			}	
		}
	}
	
	class InnerObjectClass() {
		mutable object foo {
			shared variable Integer i = 0;
		}	
		blocking Integer getInnerIAfter(Integer millis) {
			sleep(millis);
			return foo.i;
		}
		Object getFoo() {
			return foo;
		}
	}
	
	class CallableMemberClass() {
		@error variable Integer() j = () => 0; //Error: Not Mutable
	}
	
	mutable class VariableCallableMemberClass() {
		variable Integer() j = () => 0;
		void invokeJ() {
			j();
		}
		void assignJ() {
			j = () { //Error: Not annotated blocking
				@error sleep(100);
				return 1;
			};
		}
		object inner {
			blocking shared Integer foo() {
				sleep(100);
				return 1;
			}
		}
		void assignJ2() {
			@error j = inner.foo; //Error: J is of type callable foo is of type blockingCallable
		}
	}
	
	mutable class BadMetaprogrammingClass() {
		variable Array<Integer> ints = Array({ 1, 2, 3, 4 });
		shared void changeInts() {
			value stealthyBlockingCall = `function block`;
			if (exists i = ints.get(1)) {
				@error stealthyBlockingCall(outer); //Error: Blocking call in method not annotated blocking.
				ints = Array({ i });
			}
		}
	}
	
	class TrickInterfaceClass() {
		interface Trick {
			shared formal void totallyNotBlocking();
		}
		class TrickImpl() satisfies Trick {
			@error blocking shared actual void totallyNotBlocking() => block();	
		}
	}

}

