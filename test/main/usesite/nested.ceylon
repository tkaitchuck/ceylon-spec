void nested() {
    mutable class Outer<T>(variable T t) given T satisfies Immutable {
        shared class Inner() {
            shared T get() => t;
            shared void set(T t) => outer.t = t;
        }
    }
    Outer<out Immutable>.Inner o = Outer("").Inner();
    Immutable obj = o.get();
    @error o.set("");
}