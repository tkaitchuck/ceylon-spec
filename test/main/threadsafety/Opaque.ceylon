
interface OpaqueInterface<opaque X> satisfies Immutable {	
}

interface NonOpaqueInterface<X> satisfies Immutable {	
}

mutable class GenericClass<opaque X>(variable shared X x) {
	
}

mutable class MultipleGenericClass<opaque X,opaque Y>(variable Y y) {
	variable shared X? x = null;
}

mutable class GenericParameterClass<opaque X>(OpaqueInterface<X> x1) {
	shared OpaqueInterface<X>? x2 = null;
	void unbox(OpaqueInterface<X> x3) {
		
	}
}

mutable class BadGenericParameterClass<opaque X>(@error NonOpaqueInterface<X> x1) {
	@error shared variable NonOpaqueInterface<X>? x2 = null;
	void unbox(@error NonOpaqueInterface<X> x3) {
		
	}
}

mutable class BadCombinationClass<@error opaque X>(shared X x) given X satisfies Object {
}

mutable class BadMethodArgs<opaque X>(X x) {
	shared void unbox(@error NonOpaqueInterface<X> unboxer) {
		//Error: unboxer could call into X because it knows its origional type
	}
}
mutable class MethodArgs<opaque X>(variable X x) {
	shared void setX(X x) {
		this.x = x;
	}
	shared void box(OpaqueInterface<X> box) {
		
	}
}

interface OpaqueInterface2<opaque X> {
	shared formal X x;
	shared X getX() {
		return x;
	}
}

abstract mutable class Parent<opaque X>() {
	variable String s = "";
	shared formal String foo(X x);
	shared void bar(X x) { s = foo(x); }
}

@error class BadChild<X>() extends Parent<X>() given X satisfies Object {
	shared actual String foo(X x) { return x.string; }
}

class NonOpaqueImpl<X>() satisfies OpaqueInterface<X> given X satisfies Object {
}

class NonOpaqueImpl2() satisfies OpaqueInterface<String> {
}

class BadImpl<@error opaque X>() satisfies OpaqueInterface<X> given X satisfies Object {
}

interface ExtendingBlindingInterface<opaque X> satisfies NonOpaqueInterface<X> {
}

interface BaseInterface<opaque X>  {
}

interface ExtendedInterface<opaque X> satisfies BaseInterface<X> {
}

@error mutable class BadExtends<X>() extends MethodArgs<X?>(null) given X satisfies Object {
}

@error mutable class BadExtends2(variable String x) extends MethodArgs<String>(x) {
}

mutable class GoodExtends<opaque X>(variable X x) extends MethodArgs<X>(x) satisfies ExtendedInterface<X> {
	void foo(OpaqueInterface<X> bar) {
		
	}
}

mutable class MetaprogrammingClass<opaque X>(variable X x) {
	shared void badThings(X x) {
		/*@error*/ value get = `X`;
		/*@error*/ if (x is String) {
			
		}
		/*@error*/ switch (x) 
		case (is String) {
			
		} else {
		}
	}
}


