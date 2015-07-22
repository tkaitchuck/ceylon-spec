
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

mutable class BadGenericParameterClass<opaque X>(/*@error*/ NonOpaqueInterface<X> x1) {
	/*@error*/ shared NonOpaqueInterface<X>? x2 = null;
	void unbox(/*@error*/ NonOpaqueInterface<X> x3) {
		
	}
}

/*@error*/ mutable class BadCombinationClass<opaque X>(shared X x)
		given X satisfies Object {
}

mutable class BadMethodArgs<opaque X>(X x) {
	/*@error*/ shared void unbox(NonOpaqueInterface<X> unboxer) {
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

/*@error*/ class BadImpl<X>() satisfies OpaqueInterface<X> given X satisfies Object {
}

/*@error*/ class BadImpl2<opaque X>() satisfies OpaqueInterface<X> given X satisfies Object {
}

/*@error*/ class BadImpl3() satisfies OpaqueInterface<String> {
}

/*@error*/ interface BadInterface<opaque X> satisfies NonOpaqueInterface<X> {
}

interface BaseInterface<opaque X>  {
}

interface ExtendedInterface<opaque X> satisfies BaseInterface<X> {
}

/*@error*/ mutable class BadExtends<X>() extends MethodArgs<X?>(null) given X satisfies Object {
}

/*@error*/ mutable class BadExtends2(variable String x) extends MethodArgs<String>(x) {
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


