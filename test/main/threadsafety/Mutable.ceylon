class Mutable() {

	class BadVarMemberClass() {
		@error variable Integer i = 0;
	}
	class BadVarParClass(@error variable Integer i) {
	}
	
	mutable class VarParClass(variable Integer i) {
	}
	
	mutable class BadMutableParClass1(@error variable List<Integer> ints) {
	}//Error: ints may be mutable 
	
	mutable class BadMutableParClass2() {
		@error shared variable List<Integer> ints = []; 
	}//Error: ints may be mutated by another class 
	
	class MutableParClass1(List<Integer> ints) { 
	} 
	
	mutable class MutableParClass2() {
		variable List<Integer> ints1 = [];
	}
	
	mutable class VarMemberClass() {
		variable Integer i = 0;
	}
	
	object badVarParObject {
		@error variable Integer i;
	}
	
	mutable object varParObject {
		variable Integer i = 10;
	}
	
	class ParClass(List<Integer> ints) {
	}
	
	class MemberClass() {
		List<Integer> ints = Array({ 12, 3, 4 });
	}
	
	mutable class BadMethodArgs() {
		variable Integer i = 0;
		@error shared void increment(List<Integer> ints) {
			for (Integer j in ints) {
				i+=j;
			}
		}
	}
	mutable class MethodArgs() {
		variable Integer i = 0;
		shared void increment(List<Integer>&Immutable ints) {
			for (Integer j in ints) {
				i+=j;
			}
		}
	}
	
	interface BadMutableInterface {
		@error variable Integer i;
	}
	
	interface MixInInterface {
		shared formal Integer num;
		Integer plus(Integer other) => num + other;
	}
	
	mutable class SimpleMixin() satisfies MixInInterface {
		shared actual Integer num = 0;
	}
	
	class BadIncrementer() satisfies MixInInterface {
		@error shared variable actual Integer num = 0;
	}
	
	mutable class VariableIncrementer() satisfies MixInInterface {
		shared variable actual Integer num = 0;
	}
	
	class GenericParameterClass<T>(T toHold) {
	}
	
	mutable class BadGenericVariableParameterClass<T>(@error variable T toHold)
			given T satisfies Object { //Error: T may be mutable
	}
	
	mutable class GenericVariableParameterClass<T>(variable T toHold)
			given T satisfies Immutable {
	}
	
	mutable class BadUnionVariableParameterClass<T>(@error variable T|{Integer*} toHold)
			given T satisfies Immutable {//Error: {Integer*} may be mutable
	}
	
	mutable class UnionVariableParameterClass<T>(variable T|Integer toHold)
			given T satisfies Immutable {
	}
	
	interface BasicInterface {}
	
	mutable class IntersectionVariableParameterClass<T>(variable T&BasicInterface toHold)
			given T satisfies Immutable & Comparable<T> {
	}
	
	alias Simple<T> given T satisfies Immutable & Comparable<T> => IntersectionVariableParameterClass<T>;
	
	mutable class MetaprogrammingClass() {
		variable Array<Integer> ints = Array({ 1, 2, 3, 4 });
		shared void changeInts() {
			value get = `Array<Integer>.get`;
			if (exists i = get(ints)(1)) {
				ints = Array({i});
			}
		}
	}
	
	class ChildMutableClass() {
		class ParentClass() satisfies Immutable {
			Integer i = 0;
		}
		mutable class ChildClass() extends ParentClass() {
			@error variable Integer j = 0; //Not Immutable
		}
	}
	class ParentMutableClass() {
		mutable class ParentClass()  {
			variable Integer j = 0; 
		}
		@error class ChildClass() extends ParentClass() satisfies Immutable {
			//Not Immutable
		}
	}
	
	class GrandParentClass() {
		Integer x = 0;
	}
	mutable class ParentClass() extends GrandParentClass(){
		variable Integer i = 0;
	}
	mutable class ChildClass() extends ParentClass() {
		variable Integer j = 0;
	}
	class GrandChildClass() extends ChildClass() {
		Integer y = 0;
	}
	
	class CallableMemberClass() {
		Integer() j = () => 0;
	}
	
	mutable class VariableCallableMemberClass() {
		variable Integer() j = () => 0;
		void invoke() {
			j();
		}
	}

	mutable class FunctionAsArgumentClass() {
		variable Integer i = 0;
		@error void applyFunction(Integer(Integer) transform) { //Error: transform may be mutable.
			i = transform(i);
		}
	}
	class InnerObjectClass() {
		mutable object my {
			shared variable Integer i = 0;
		}	
		void applyFunction(Integer(Integer) transform) { 
			my.i = transform(my.i);
		}
	}
	
	mutable class ReturnAnonymousClass() {
		variable Integer i = 0;
		Anything(Integer) getMutator() {
			return (Integer newI) => i = newI; 
		}
		void getMutator2()(Integer newI) {
			i = newI; 
		}
		void getMutator3(Integer newI)() {
			i = newI; 
		}
		interface Change {
			shared formal void setI(Integer i);
		}
		Change getMutator4() {
			return object satisfies Change {
				shared actual void setI(Integer newI) {
					i = newI; 
				}
			};
		}
	}
	
}

